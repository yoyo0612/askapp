package com.example.healthapp.data

import com.example.healthapp.nutrition.Nutrients

/**
 * 初期登録する食品データ（100gあたり）。
 *
 * ※ 日本食品標準成分表を参考にした概算値です。正確な値が必要な場合は
 *   公式の「日本食品標準成分表（八訂）」で確認し、必要に応じて修正してください。
 */
val SEED_FOODS: List<Food> = listOf(
    food("白米ごはん", 168.0, 2.5, 0.3, 37.1, 0.3, 1.0),
    food("食パン", 264.0, 9.4, 4.2, 46.7, 2.3, 491.0),
    food("鶏むね肉（皮なし・生）", 108.0, 22.3, 1.5, 0.0, 0.0, 42.0),
    food("鶏卵（全卵・生）", 151.0, 12.3, 10.3, 0.3, 0.0, 140.0),
    food("牛乳", 67.0, 3.3, 3.8, 4.8, 0.0, 41.0),
    food("無糖ヨーグルト", 62.0, 3.6, 3.0, 4.9, 0.0, 60.0),
    food("納豆", 200.0, 16.5, 10.0, 12.1, 6.7, 1.0),
    food("鮭（生）", 139.0, 22.1, 4.1, 0.1, 0.0, 58.0),
    food("ほうれんそう（生）", 20.0, 2.2, 0.4, 3.1, 2.8, 16.0),
    food("トマト（生）", 19.0, 0.7, 0.1, 4.7, 1.0, 2.0),
    food("バナナ", 86.0, 1.1, 0.2, 22.5, 1.1, 0.0),
    food("りんご（生）", 54.0, 0.1, 0.1, 14.6, 1.3, 0.0),
)

/** id は 0 のダミー。実際の id は DB 挿入時に採番される。 */
private fun food(
    name: String,
    kcal: Double,
    protein: Double,
    fat: Double,
    carbs: Double,
    fiber: Double,
    sodium: Double,
) = Food(
    id = 0,
    name = name,
    per100g = Nutrients(kcal, protein, fat, carbs, fiber, sodium),
)
