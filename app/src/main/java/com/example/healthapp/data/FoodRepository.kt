package com.example.healthapp.data

import android.content.ContentValues
import android.database.Cursor
import com.example.healthapp.nutrition.Nutrients
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FoodRepository(private val database: HealthDatabase) {

    suspend fun list(): List<Food> = withContext(Dispatchers.IO) {
        database.readableDatabase.rawQuery(
            "SELECT id, name, kcal, protein_g, fat_g, carbs_g, fiber_g, sodium_mg FROM foods ORDER BY name",
            null,
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.toFood())
            }
        }
    }

    /** 食品を追加し、採番された id を返す。同名があると例外になる。 */
    suspend fun add(food: Food): Long = withContext(Dispatchers.IO) {
        database.writableDatabase.insertOrThrow("foods", null, food.toContentValues())
    }

    suspend fun update(food: Food) {
        withContext(Dispatchers.IO) {
            database.writableDatabase.update(
                "foods",
                food.toContentValues(),
                "id = ?",
                arrayOf(food.id.toString()),
            )
        }
    }

    /** 食事の記録で使われている食品は、外部キー制約により削除できず例外になる。 */
    suspend fun delete(foodId: Long) {
        withContext(Dispatchers.IO) {
            database.writableDatabase.delete("foods", "id = ?", arrayOf(foodId.toString()))
        }
    }
}

internal fun Food.toContentValues(): ContentValues = ContentValues().apply {
    put("name", name)
    put("kcal", per100g.kcal)
    put("protein_g", per100g.proteinG)
    put("fat_g", per100g.fatG)
    put("carbs_g", per100g.carbsG)
    put("fiber_g", per100g.fiberG)
    put("sodium_mg", per100g.sodiumMg)
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
