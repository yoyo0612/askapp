import unittest

from health_app import service
from health_app.db import connect
from health_app.service import ValidationError


def food_id(conn, name):
    return next(f["id"] for f in service.list_foods(conn) if f["name"] == name)


class ServiceTest(unittest.TestCase):
    def setUp(self):
        self.conn = connect(":memory:")

    def tearDown(self):
        self.conn.close()

    def test_day_summary_totals_meals(self):
        rice = food_id(self.conn, "白米ごはん")
        egg = food_id(self.conn, "鶏卵（全卵・生）")
        service.add_meal(self.conn, {
            "eaten_on": "2026-10-22",
            "meal_type": "breakfast",
            "items": [{"food_id": rice, "grams": 150}, {"food_id": egg, "grams": 50}],
        })

        day = service.day_summary(self.conn, "2026-10-22")

        self.assertEqual(len(day["meals"]), 1)
        meal = day["meals"][0]
        self.assertEqual(meal["items"][0]["nutrients"]["kcal"], 252.0)
        self.assertEqual(meal["items"][1]["nutrients"]["kcal"], 75.5)
        self.assertEqual(meal["total"]["kcal"], 327.5)
        self.assertEqual(day["total"]["kcal"], 327.5)

    def test_other_dates_are_excluded(self):
        rice = food_id(self.conn, "白米ごはん")
        service.add_meal(self.conn, {
            "eaten_on": "2026-10-21",
            "meal_type": "dinner",
            "items": [{"food_id": rice, "grams": 100}],
        })
        day = service.day_summary(self.conn, "2026-10-22")
        self.assertEqual(day["meals"], [])
        self.assertEqual(day["total"]["kcal"], 0.0)

    def test_rejects_invalid_meal_type(self):
        with self.assertRaises(ValidationError):
            service.add_meal(self.conn, {
                "eaten_on": "2026-10-22",
                "meal_type": "brunch",
                "items": [{"food_id": 1, "grams": 100}],
            })

    def test_rejects_nonpositive_grams(self):
        with self.assertRaises(ValidationError):
            service.add_meal(self.conn, {
                "eaten_on": "2026-10-22",
                "meal_type": "lunch",
                "items": [{"food_id": 1, "grams": 0}],
            })

    def test_rejects_unknown_food_and_rolls_back(self):
        with self.assertRaises(ValidationError):
            service.add_meal(self.conn, {
                "eaten_on": "2026-10-22",
                "meal_type": "lunch",
                "items": [{"food_id": 99999, "grams": 100}],
            })
        self.assertEqual(service.day_summary(self.conn, "2026-10-22")["meals"], [])

    def test_rejects_invalid_date(self):
        with self.assertRaises(ValidationError):
            service.day_summary(self.conn, "2026-02-30")

    def test_add_food(self):
        new_id = service.add_food(self.conn, {
            "name": "テスト食品", "kcal": 100, "protein_g": 1, "fat_g": 1,
            "carbs_g": 10, "fiber_g": 0, "sodium_mg": 5,
        })
        self.assertIn(new_id, [f["id"] for f in service.list_foods(self.conn)])

    def test_add_food_rejects_missing_field(self):
        with self.assertRaises(ValidationError):
            service.add_food(self.conn, {"name": "欠損", "kcal": 100})


if __name__ == "__main__":
    unittest.main()
