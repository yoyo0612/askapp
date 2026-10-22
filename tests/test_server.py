import json
import threading
import unittest
import urllib.error
import urllib.request

from health_app.db import connect
from health_app.server import create_server


class ServerTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        # サーバーは別スレッドで動くため、接続をそのスレッドから使えるようにする
        cls.conn = connect(":memory:", check_same_thread=False)
        cls.server = create_server(cls.conn, "127.0.0.1", 0)
        cls.base = f"http://127.0.0.1:{cls.server.server_address[1]}"
        cls.thread = threading.Thread(target=cls.server.serve_forever, daemon=True)
        cls.thread.start()

    @classmethod
    def tearDownClass(cls):
        cls.server.shutdown()
        cls.server.server_close()
        cls.conn.close()

    def request(self, method, path, payload=None):
        data = json.dumps(payload).encode("utf-8") if payload is not None else None
        req = urllib.request.Request(self.base + path, data=data, method=method,
                                     headers={"Content-Type": "application/json"})
        try:
            with urllib.request.urlopen(req) as res:
                return res.status, json.loads(res.read())
        except urllib.error.HTTPError as error:
            return error.code, json.loads(error.read())

    def test_index_page(self):
        with urllib.request.urlopen(self.base + "/") as res:
            self.assertEqual(res.status, 200)
            self.assertIn("食事記録", res.read().decode("utf-8"))

    def test_list_foods(self):
        status, foods = self.request("GET", "/api/foods")
        self.assertEqual(status, 200)
        self.assertGreater(len(foods), 0)

    def test_post_meal_returns_calculated_nutrients(self):
        status, foods = self.request("GET", "/api/foods")
        rice = next(f for f in foods if f["name"] == "白米ごはん")

        status, meal = self.request("POST", "/api/meals", {
            "eaten_on": "2026-10-22",
            "meal_type": "lunch",
            "items": [{"food_id": rice["id"], "grams": 200}],
        })
        self.assertEqual(status, 201)
        self.assertEqual(meal["items"][0]["nutrients"]["kcal"], 336.0)

        status, day = self.request("GET", "/api/meals?date=2026-10-22")
        self.assertEqual(status, 200)
        self.assertEqual(day["total"]["kcal"], 336.0)

    def test_invalid_meal_returns_400(self):
        status, body = self.request("POST", "/api/meals", {
            "eaten_on": "2026-10-22", "meal_type": "lunch", "items": [],
        })
        self.assertEqual(status, 400)
        self.assertIn("error", body)

    def test_unknown_path_returns_404(self):
        status, _ = self.request("GET", "/api/nothing")
        self.assertEqual(status, 404)


if __name__ == "__main__":
    unittest.main()
