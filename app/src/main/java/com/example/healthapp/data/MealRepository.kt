package com.example.healthapp.data

import android.content.ContentValues
import android.database.Cursor
import com.example.healthapp.nutrition.Nutrients
import com.example.healthapp.nutrition.forGrams
import com.example.healthapp.nutrition.total
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MealRepository(private val database: HealthDatabase) {

    suspend fun listFoods(): List<Food> = withContext(Dispatchers.IO) {
        database.readableDatabase.rawQuery(
            "SELECT id, name, kcal, protein_g, fat_g, carbs_g, fiber_g, sodium_mg FROM foods ORDER BY name",
            null,
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.toFood())
            }
        }
    }

    /** 食事を1件記録し、その id を返す。途中で失敗した場合は全体をロールバックする。 */
    suspend fun addMeal(date: LocalDate, mealType: MealType, items: List<MealItemInput>): Long =
        withContext(Dispatchers.IO) {
            require(items.isNotEmpty()) { "items は1件以上必要です" }
            val db = database.writableDatabase
            db.beginTransaction()
            try {
                val mealId = db.insertOrThrow(
                    "meals",
                    null,
                    ContentValues().apply {
                        put("eaten_on", date.toString())
                        put("meal_type", mealType.dbValue)
                    },
                )
                items.forEach { item ->
                    require(item.grams > 0) { "grams は 0 より大きい値にしてください" }
                    db.insertOrThrow(
                        "meal_items",
                        null,
                        ContentValues().apply {
                            put("meal_id", mealId)
                            put("food_id", item.foodId)
                            put("grams", item.grams)
                        },
                    )
                }
                db.setTransactionSuccessful()
                mealId
            } finally {
                db.endTransaction()
            }
        }

    /** 指定日の食事一覧と、1日の栄養素合計を返す。 */
    suspend fun daySummary(date: LocalDate): DaySummary = withContext(Dispatchers.IO) {
        val db = database.readableDatabase
        val dateText = date.toString()

        val mealRows = db.rawQuery(
            "SELECT id, meal_type FROM meals WHERE eaten_on = ? ORDER BY id",
            arrayOf(dateText),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(cursor.getLong(0) to MealType.fromDb(cursor.getString(1)))
                }
            }
        }

        val itemsByMeal = db.rawQuery(
            """
            SELECT mi.meal_id, mi.food_id, f.name, mi.grams,
                   f.kcal, f.protein_g, f.fat_g, f.carbs_g, f.fiber_g, f.sodium_mg
            FROM meal_items mi
            JOIN meals m ON m.id = mi.meal_id
            JOIN foods f ON f.id = mi.food_id
            WHERE m.eaten_on = ?
            ORDER BY mi.id
            """.trimIndent(),
            arrayOf(dateText),
        ).use { cursor ->
            val map = mutableMapOf<Long, MutableList<MealItemDetail>>()
            while (cursor.moveToNext()) {
                val grams = cursor.getDouble(3)
                val per100g = Nutrients(
                    kcal = cursor.getDouble(4),
                    proteinG = cursor.getDouble(5),
                    fatG = cursor.getDouble(6),
                    carbsG = cursor.getDouble(7),
                    fiberG = cursor.getDouble(8),
                    sodiumMg = cursor.getDouble(9),
                )
                map.getOrPut(cursor.getLong(0)) { mutableListOf() }.add(
                    MealItemDetail(
                        foodId = cursor.getLong(1),
                        name = cursor.getString(2),
                        grams = grams,
                        nutrients = per100g.forGrams(grams),
                    ),
                )
            }
            map
        }

        val meals = mealRows.map { (mealId, mealType) ->
            val items = itemsByMeal[mealId].orEmpty()
            MealDetail(
                id = mealId,
                mealType = mealType,
                items = items,
                total = items.map { it.nutrients }.total(),
            )
        }

        DaySummary(
            date = date,
            meals = meals,
            total = meals.map { it.total }.total(),
        )
    }

    private fun Cursor.toFood(): Food = Food(
        id = getLong(0),
        name = getString(1),
        per100g = Nutrients(
            kcal = getDouble(2),
            proteinG = getDouble(3),
            fatG = getDouble(4),
            carbsG = getDouble(5),
            fiberG = getDouble(6),
            sodiumMg = getDouble(7),
        ),
    )
}
