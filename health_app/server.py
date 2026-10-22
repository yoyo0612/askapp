import json
from datetime import date
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, HTTPServer
from pathlib import Path
from urllib.parse import parse_qs, urlparse

from . import service
from .service import ValidationError

STATIC_DIR = Path(__file__).parent / "static"


def create_server(conn, host="127.0.0.1", port=8000):
    class Handler(BaseHTTPRequestHandler):
        def do_GET(self):
            url = urlparse(self.path)
            try:
                if url.path == "/":
                    self._send_file(STATIC_DIR / "index.html", "text/html; charset=utf-8")
                elif url.path == "/api/foods":
                    self._send_json(HTTPStatus.OK, service.list_foods(conn))
                elif url.path == "/api/meals":
                    day = parse_qs(url.query).get("date", [date.today().isoformat()])[0]
                    self._send_json(HTTPStatus.OK, service.day_summary(conn, day))
                else:
                    self._send_json(HTTPStatus.NOT_FOUND, {"error": "not found"})
            except ValidationError as error:
                self._send_json(HTTPStatus.BAD_REQUEST, {"error": str(error)})

        def do_POST(self):
            url = urlparse(self.path)
            try:
                data = self._read_json()
                if url.path == "/api/foods":
                    food_id = service.add_food(conn, data)
                    self._send_json(HTTPStatus.CREATED, {"id": food_id})
                elif url.path == "/api/meals":
                    meal_id = service.add_meal(conn, data)
                    day = service.day_summary(conn, data["eaten_on"])
                    meal = next(m for m in day["meals"] if m["id"] == meal_id)
                    self._send_json(HTTPStatus.CREATED, meal)
                else:
                    self._send_json(HTTPStatus.NOT_FOUND, {"error": "not found"})
            except ValidationError as error:
                self._send_json(HTTPStatus.BAD_REQUEST, {"error": str(error)})
            except json.JSONDecodeError:
                self._send_json(HTTPStatus.BAD_REQUEST, {"error": "JSON の形式が不正です"})

        def _read_json(self):
            length = int(self.headers.get("Content-Length", 0))
            body = self.rfile.read(length).decode("utf-8") if length else "{}"
            data = json.loads(body)
            if not isinstance(data, dict):
                raise ValidationError("リクエストはJSONオブジェクトで送ってください")
            return data

        def _send_json(self, status, payload):
            body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
            self._send_bytes(status, body, "application/json; charset=utf-8")

        def _send_file(self, path, content_type):
            self._send_bytes(HTTPStatus.OK, path.read_bytes(), content_type)

        def _send_bytes(self, status, body, content_type):
            self.send_response(status)
            self.send_header("Content-Type", content_type)
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)

        def log_message(self, format, *args):
            pass

    return HTTPServer((host, port), Handler)
