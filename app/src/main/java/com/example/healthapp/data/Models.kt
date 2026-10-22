package com.example.healthapp.data

import com.example.healthapp.nutrition.Nutrients
import java.time.LocalDate

enum class MealType(val label: String, val dbValue: String) {
    BREAKFAST("朝食", "breakfast"),
    LUNCH("昼食", "lunch"),
    DINNER("夕食", "dinner"),
    SNACK("間食", "snack");

    companion object {
        fun fromDb(value: String): MealType = entries.first { it.dbValue == value }
    }
}

/** 食品マスタ。栄養素は100gあたり。 */
data class Food(val id: Long, val name: String, val per100g: Nutrients)

data class MealItemInput(val foodId: Long, val grams: Double)

data class MealItemDetail(
    val foodId: Long,
    val name: String,
    val grams: Double,
    val nutrients: Nutrients,
)

data class MealDetail(
    val id: Long,
    val mealType: MealType,
    val items: List<MealItemDetail>,
    val total: Nutrients,
)

data class DaySummary(
    val date: LocalDate,
    val meals: List<MealDetail>,
    val total: Nutrients,
)
