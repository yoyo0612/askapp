package com.example.healthapp.ui

import com.example.healthapp.nutrition.Nutrients
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd (E)", Locale.JAPAN)

fun LocalDate.toDisplay(): String = format(DATE_FORMAT)

fun Double.oneDecimal(): String = String.format(Locale.US, "%.1f", this)

/** 整数なら小数点なしで表示する（150.0 → "150"）。 */
fun Double.plainText(): String = if (this % 1.0 == 0.0) toLong().toString() else toString()

fun Nutrients.summaryLine(): String =
    "${kcal.oneDecimal()} kcal ｜ P ${proteinG.oneDecimal()}g ｜ F ${fatG.oneDecimal()}g ｜ " +
        "C ${carbsG.oneDecimal()}g ｜ 食物繊維 ${fiberG.oneDecimal()}g ｜ Na ${sodiumMg.oneDecimal()}mg"
