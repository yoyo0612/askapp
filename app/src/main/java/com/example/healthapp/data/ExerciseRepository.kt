package com.example.healthapp.data

import android.content.ContentValues
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ExerciseRepository(private val database: HealthDatabase) {

    suspend fun listFor(date: LocalDate): List<ExerciseEntry> = withContext(Dispatchers.IO) {
        database.readableDatabase.rawQuery(
            "SELECT id, performed_on, name, minutes, kcal FROM exercises WHERE performed_on = ? ORDER BY id",
            arrayOf(date.toString()),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        ExerciseEntry(
                            id = cursor.getLong(0),
                            date = LocalDate.parse(cursor.getString(1)),
                            name = cursor.getString(2),
                            minutes = cursor.getDouble(3),
                            kcal = cursor.getDouble(4),
                        ),
                    )
                }
            }
        }
    }

    suspend fun add(entry: ExerciseEntry) {
        withContext(Dispatchers.IO) {
            val values = ContentValues().apply {
                put("performed_on", entry.date.toString())
                put("name", entry.name)
                put("minutes", entry.minutes)
                put("kcal", entry.kcal)
            }
            database.writableDatabase.insertOrThrow("exercises", null, values)
        }
    }

    suspend fun delete(id: Long) {
        withContext(Dispatchers.IO) {
            database.writableDatabase.delete("exercises", "id = ?", arrayOf(id.toString()))
        }
    }
}
