"""栄養計算の純粋関数。DB や HTTP に依存しない。"""

from collections.abc import Iterable, Mapping

NUTRIENT_KEYS = ("kcal", "protein_g", "fat_g", "carbs_g", "fiber_g", "sodium_mg")


def calc_nutrients(food_per_100g: Mapping[str, float], grams: float) -> dict:
    """食品の100gあたりの栄養素から、指定グラム数分の栄養素を求める。"""
    factor = grams / 100
    return {key: round(food_per_100g[key] * factor, 1) for key in NUTRIENT_KEYS}


def sum_nutrients(rows: Iterable[Mapping[str, float]]) -> dict:
    """複数の栄養素データを合計する。"""
    total = dict.fromkeys(NUTRIENT_KEYS, 0.0)
    for row in rows:
        for key in NUTRIENT_KEYS:
            total[key] += row[key]
    return {key: round(value, 1) for key, value in total.items()}
