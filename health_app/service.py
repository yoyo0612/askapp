"""業務ロジック。DB 接続を受け取り、API が返す辞書を作る。"""

import re
import sqlite3
from datetime import date

from .nutrition import NUTRIENT_KEYS, calc_nutrients, sum_nutrients

MEAL_TYPES = ("breakfast", "lunch", "dinner", "snack")
FOOD_FIELDS = NUTRIENT_KEYS
DATE_PATTERN = re.compile(r"^\d{4}-\d{2}-\d{2}$")


class ValidationError(ValueError):
    pass


def _require_name(data):
    name = data.get("name")
    if not isinstance(name, str) or not name.strip():
        raise ValidationError("name は空でない文字列で指定してください")
    return name.strip()


def _require_number(value, label, minimum_exclusive=None, minimum=None):
    if isinstance(value, bool) or not isinstance(value, (int, float)):
        raise ValidationError(f"{label} は数値で指定してください")
    if minimum_exclusive is not None and value <= minimum_exclusive:
        raise ValidationError(f"{label} は {minimum_exclusive} より大きい値にしてください")
    if minimum is not None and value < minimum:
        raise ValidationError(f"{label} は {minimum} 以上にしてください")
    return float(value)


def _require_date(value):
    if not isinstance(value, str) or not DATE_PATTERN.match(value):
        raise ValidationError("日付は YYYY-MM-DD 形式で指定してください")
    try:
        date.fromisoformat(value)
    except ValueError:
        raise ValidationError("存在しない日付です") from None
    return value


def list_foods(conn):
    rows = conn.execute("SELECT id, name, " + ", ".join(FOOD_FIELDS) + " FROM foods ORDER BY name")
    return [dict(row) for row in rows]


def add_food(conn, data):
    name = _require_name(data)
    values = [_require_number(data.get(field), field, minimum=0) for field in FOOD_FIELDS]
    cursor = conn.execute(
        "INSERT INTO foods (name, " + ", ".join(FOOD_FIELDS) + ") VALUES (?, "
        + ", ".join("?" * len(FOOD_FIELDS)) + ")",
        [name, *values],
    )
    conn.commit()
    return cursor.lastrowid


def add_meal(conn, data):
    eaten_on = _require_date(data.get("eaten_on"))
    meal_type = data.get("meal_type")
    if meal_type not in MEAL_TYPES:
        raise ValidationError(f"meal_type は {', '.join(MEAL_TYPES)} のいずれかにしてください")

    items = data.get("items")
    if not isinstance(items, list) or not items:
        raise ValidationError("items は1件以上の配列で指定してください")

    parsed = []
    for item in items:
        if not isinstance(item, dict):
            raise ValidationError("items の各要素はオブジェクトで指定してください")
        food_id = item.get("food_id")
        if isinstance(food_id, bool) or not isinstance(food_id, int):
            raise ValidationError("food_id は整数で指定してください")
        grams = _require_number(item.get("grams"), "grams", minimum_exclusive=0)
        parsed.append((food_id, grams))

    # with conn: は成功時 commit、例外時 rollback になる
    with conn:
        cursor = conn.execute(
            "INSERT INTO meals (eaten_on, meal_type) VALUES (?, ?)", (eaten_on, meal_type)
        )
        meal_id = cursor.lastrowid
        for food_id, grams in parsed:
            try:
                conn.execute(
                    "INSERT INTO meal_items (meal_id, food_id, grams) VALUES (?, ?, ?)",
                    (meal_id, food_id, grams),
                )
            except sqlite3.IntegrityError:
                raise ValidationError(f"food_id={food_id} の食品が存在しません") from None
    return meal_id


def _meal_detail(conn, meal_row):
    rows = conn.execute(
        "SELECT mi.grams, f.id AS food_id, f.name, " + ", ".join("f." + f for f in FOOD_FIELDS)
        + " FROM meal_items mi JOIN foods f ON f.id = mi.food_id"
        " WHERE mi.meal_id = ? ORDER BY mi.id",
        (meal_row["id"],),
    ).fetchall()

    items = [
        {
            "food_id": row["food_id"],
            "name": row["name"],
            "grams": row["grams"],
            "nutrients": calc_nutrients(row, row["grams"]),
        }
        for row in rows
    ]
    return {
        "id": meal_row["id"],
        "eaten_on": meal_row["eaten_on"],
        "meal_type": meal_row["meal_type"],
        "items": items,
        "total": sum_nutrients(item["nutrients"] for item in items),
    }


def day_summary(conn, day):
    _require_date(day)
    meal_rows = conn.execute(
        "SELECT id, eaten_on, meal_type FROM meals WHERE eaten_on = ? ORDER BY id", (day,)
    ).fetchall()
    meals = [_meal_detail(conn, row) for row in meal_rows]
    return {
        "date": day,
        "meals": meals,
        "total": sum_nutrients(meal["total"] for meal in meals),
    }
