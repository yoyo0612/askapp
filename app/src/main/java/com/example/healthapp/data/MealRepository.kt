package com.example.healthapp.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.example.healthapp.nutrition.Nutrients
import com.example.healthapp.nutrition.forGrams
import com.example.healthapp.nutrition.round1
import com.example.healthapp.nutrition.total
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MealRepository(private val database: HealthDatabase) {

    /** 食事を1件記録し、その id を返す。途中で失敗した場合は全体をロールバックする。 */
    suspend fun addMeal(date: LocalDate, mealType: MealType, items: List<MealItemInput>): Long =
        withContext(Dispatchers.IO) {
            require(items.isNotEmpty()) { "items は1件以上必要です" }
            database.writableDatabase.inTransaction {
                val mealId = insertOrThrow("meals", null, mealValues(date, mealType))
                insertItems(this, mealId, items)
                mealId
            }
        }

    /** 既存の食事の日付・種類・食品を差し替える。 */
    suspend fun updateMeal(mealId: Long, date: LocalDate, mealType: MealType, items: List<MealItemInput>) {
        withContext(Dispatchers.IO) {
            require(items.isNotEmpty()) { "items は1件以上必要です" }
            database.writableDatabase.inTransaction {
                update("meals", mealValues(date, mealType), "id = ?", arrayOf(mealId.toString()))
                delete("meal_items", "meal_id = ?", arrayOf(mealId.toString()))
                insertItems(this, mealId, items)
            }
        }
    }

    /** meal_items は ON DELETE CASCADE で一緒に消える。 */
    suspend fun deleteMeal(mealId: Long) {
        withContext(Dispatchers.IO) {
            database.writableDatabase.delete("meals", "id = ?", arrayOf(mealId.toString()))
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

    /** 期間内で記録のある日ごとの合計（日付の昇順）。 */
    suspend fun dailyTotals(from: LocalDate, to: LocalDate): List<DailyTotal> = withContext(Dispatchers.IO) {
        database.readableDatabase.rawQuery(
            """
            SELECT m.eaten_on,
                   SUM(mi.grams * f.kcal) / 100.0,
                   SUM(mi.grams * f.protein_g) / 100.0,
                   SUM(mi.grams * f.fat_g) / 100.0,
                   SUM(mi.grams * f.carbs_g) / 100.0,
                   SUM(mi.grams * f.fiber_g) / 100.0,
                   SUM(mi.grams * f.sodium_mg) / 100.0
            FROM meal_items mi
            JOIN meals m ON m.id = mi.meal_id
            JOIN foods f ON f.id = mi.food_id
            WHERE m.eaten_on BETWEEN ? AND ?
            GROUP BY m.eaten_on
            ORDER BY m.eaten_on
            """.trimIndent(),
            arrayOf(from.toString(), to.toString()),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        DailyTotal(
                            date = LocalDate.parse(cursor.getString(0)),
                            total = Nutrients(
                                kcal = cursor.getDouble(1).round1(),
                                proteinG = cursor.getDouble(2).round1(),
                                fatG = cursor.getDouble(3).round1(),
                                carbsG = cursor.getDouble(4).round1(),
                                fiberG = cursor.getDouble(5).round1(),
                                sodiumMg = cursor.getDouble(6).round1(),
                            ),
                        ),
                    )
                }
            }
        }
    }

    private fun insertItems(db: SQLiteDatabase, mealId: Long, items: List<MealItemInput>) {
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
    }

    private fun mealValues(date: LocalDate, mealType: MealType): ContentValues = ContentValues().apply {
        put("eaten_on", date.toString())
        put("meal_type", mealType.dbValue)
    }

    private inline fun <T> SQLiteDatabase.inTransaction(block: SQLiteDatabase.() -> T): T {
        beginTransaction()
        try {
            val result = block()
            setTransactionSuccessful()
            return result
        } finally {
            endTransaction()
        }
    }
}
