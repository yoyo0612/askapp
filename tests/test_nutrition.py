import unittest

from health_app.nutrition import calc_nutrients, sum_nutrients

RICE = {"kcal": 168, "protein_g": 2.5, "fat_g": 0.3, "carbs_g": 37.1, "fiber_g": 0.3, "sodium_mg": 1}


class CalcNutrientsTest(unittest.TestCase):
    def test_scales_by_grams(self):
        result = calc_nutrients(RICE, 150)
        self.assertEqual(result["kcal"], 252.0)
        self.assertEqual(result["protein_g"], 3.8)
        self.assertEqual(result["carbs_g"], 55.7)

    def test_100g_equals_per_100g_values(self):
        result = calc_nutrients(RICE, 100)
        self.assertEqual(result["kcal"], 168)
        self.assertEqual(result["sodium_mg"], 1)


class SumNutrientsTest(unittest.TestCase):
    def test_sums_each_nutrient(self):
        a = calc_nutrients(RICE, 100)
        b = calc_nutrients(RICE, 200)
        total = sum_nutrients([a, b])
        self.assertEqual(total["kcal"], 504)
        self.assertEqual(total["fiber_g"], 0.9)

    def test_empty_is_zero(self):
        total = sum_nutrients([])
        self.assertEqual(total["kcal"], 0.0)


if __name__ == "__main__":
    unittest.main()
