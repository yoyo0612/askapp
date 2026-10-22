package com.example.healthapp.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.example.healthapp.nutrition.Nutrients
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 目標値と体重の記録。 */
class BodyRepository(private val database: HealthDatabase) {

    suspend fun getGoal(): Nutrients = withContext(Dispatchers.IO) {
        database.readableDatabase.rawQuery(
            "SELECT kcal, protein_g, fat_g, carbs_g, fiber_g, sodium_mg FROM goals WHERE id = 1",
            null,
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                Nutrients(
                    kcal = cursor.getDouble(0),
                    proteinG = cursor.getDouble(1),
                    fatG = cursor.getDouble(2),
                    carbsG = cursor.getDouble(3),
                    fiberG = cursor.getDouble(4),
                    sodiumMg = cursor.getDouble(5),
                )
            } else {
                Nutrients.ZERO
            }
        }
    }

    suspend fun saveGoal(goal: Nutrients) {
        withContext(Dispatchers.IO) {
            val values = ContentValues().apply {
                put("kcal", goal.kcal)
                put("protein_g", goal.proteinG)
                put("fat_g", goal.fatG)
                put("carbs_g", goal.carbsG)
                put("fiber_g", goal.fiberG)
                put("sodium_mg", goal.sodiumMg)
            }
            database.writableDatabase.update("goals", values, "id = 1", null)
        }
    }

    /** 直近の記録を日付の昇順で返す。 */
    suspend fun listWeights(limit: Int = 60): List<WeightEntry> = withContext(Dispatchers.IO) {
        database.readableDatabase.rawQuery(
            "SELECT measured_on, kg FROM weights ORDER BY measured_on DESC LIMIT ?",
            arrayOf(limit.toString()),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(WeightEntry(LocalDate.parse(cursor.getString(0)), cursor.getDouble(1)))
                }
            }.reversed()
        }
    }

    /** 同じ日付の記録は上書きする。 */
    suspend fun saveWeight(entry: WeightEntry) {
        withContext(Dispatchers.IO) {
            val values = ContentValues().apply {
                put("measured_on", entry.date.toString())
                put("kg", entry.kg)
            }
            database.writableDatabase.insertWithOnConflict(
                "weights",
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE,
            )
        }
    }

    suspend fun deleteWeight(date: LocalDate) {
        withContext(Dispatchers.IO) {
            database.writableDatabase.delete("weights", "measured_on = ?", arrayOf(date.toString()))
        }
    }
}
