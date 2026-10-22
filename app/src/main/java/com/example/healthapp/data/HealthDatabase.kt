package com.example.healthapp.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class HealthDatabase(context: Context) : SQLiteOpenHelper(context, "health.db", null, 3) {

    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        createFoodTables(db)
        createMealTables(db)
        createBodyTables(db)
        createExerciseTables(db)
        SEED_FOODS.forEach { food -> db.insertOrThrow("foods", null, food.toContentValues()) }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // v1 → v2: 目標値と体重の表を追加
        if (oldVersion < 2) createBodyTables(db)
        // v2 → v3: 運動記録の表を追加
        if (oldVersion < 3) createExerciseTables(db)
    }

    private fun createExerciseTables(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS exercises (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                performed_on TEXT NOT NULL,
                name TEXT NOT NULL,
                minutes REAL NOT NULL CHECK (minutes > 0),
                kcal REAL NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_exercises_performed_on ON exercises(performed_on)")
    }

    private fun createFoodTables(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE foods (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL UNIQUE,
                kcal REAL NOT NULL,
                protein_g REAL NOT NULL,
                fat_g REAL NOT NULL,
                carbs_g REAL NOT NULL,
                fiber_g REAL NOT NULL,
                sodium_mg REAL NOT NULL
            )
            """.trimIndent(),
        )
    }

    private fun createMealTables(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE meals (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                eaten_on TEXT NOT NULL,
                meal_type TEXT NOT NULL CHECK (meal_type IN ('breakfast', 'lunch', 'dinner', 'snack'))
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX idx_meals_eaten_on ON meals(eaten_on)")
        db.execSQL(
            """
            CREATE TABLE meal_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                meal_id INTEGER NOT NULL REFERENCES meals(id) ON DELETE CASCADE,
                food_id INTEGER NOT NULL REFERENCES foods(id),
                grams REAL NOT NULL CHECK (grams > 0)
            )
            """.trimIndent(),
        )
    }

    private fun createBodyTables(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS goals (
                id INTEGER PRIMARY KEY CHECK (id = 1),
                kcal REAL NOT NULL,
                protein_g REAL NOT NULL,
                fat_g REAL NOT NULL,
                carbs_g REAL NOT NULL,
                fiber_g REAL NOT NULL,
                sodium_mg REAL NOT NULL
            )
            """.trimIndent(),
        )
        // 既定値は一般的な成人の目安。画面から変更できる。
        db.execSQL(
            "INSERT OR IGNORE INTO goals (id, kcal, protein_g, fat_g, carbs_g, fiber_g, sodium_mg)" +
                " VALUES (1, 2000, 60, 60, 300, 20, 2300)",
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS weights (
                measured_on TEXT PRIMARY KEY,
                kg REAL NOT NULL
            )
            """.trimIndent(),
        )
    }
}
