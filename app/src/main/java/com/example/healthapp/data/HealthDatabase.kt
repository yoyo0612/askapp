package com.example.healthapp.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class HealthDatabase(context: Context) : SQLiteOpenHelper(context, "health.db", null, 1) {

    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
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
        db.execSQL(
            """
            CREATE TABLE meals (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                eaten_on TEXT NOT NULL,
                meal_type TEXT NOT NULL CHECK (meal_type IN ('breakfast', 'lunch', 'dinner', 'snack'))
            )
            """.trimIndent(),
        )
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
        db.execSQL("CREATE INDEX idx_meals_eaten_on ON meals(eaten_on)")

        SEED_FOODS.forEach { food ->
            val values = ContentValues().apply {
                put("name", food.name)
                put("kcal", food.per100g.kcal)
                put("protein_g", food.per100g.proteinG)
                put("fat_g", food.per100g.fatG)
                put("carbs_g", food.per100g.carbsG)
                put("fiber_g", food.per100g.fiberG)
                put("sodium_mg", food.per100g.sodiumMg)
            }
            db.insertOrThrow("foods", null, values)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // バージョン1のみのため、マイグレーションは未実装
    }
}
