package com.example.healthapp.nutrition

import kotlin.math.roundToLong

data class Nutrients(
    val kcal: Double,
    val proteinG: Double,
    val fatG: Double,
    val carbsG: Double,
    val fiberG: Double,
    val sodiumMg: Double,
) {
    companion object {
        val ZERO = Nutrients(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    }
}

private fun Double.round1(): Double = (this * 10).roundToLong() / 10.0

/** 100gあたりの栄養素から、指定グラム数分の栄養素を求める。 */
fun Nutrients.forGrams(grams: Double): Nutrients {
    require(grams > 0) { "grams は 0 より大きい値にしてください" }
    val factor = grams / 100
    return Nutrients(
        kcal = (kcal * factor).round1(),
        proteinG = (proteinG * factor).round1(),
        fatG = (fatG * factor).round1(),
        carbsG = (carbsG * factor).round1(),
        fiberG = (fiberG * factor).round1(),
        sodiumMg = (sodiumMg * factor).round1(),
    )
}

/** 複数の栄養素を合計する。 */
fun Iterable<Nutrients>.total(): Nutrients = Nutrients(
    kcal = sumOf { it.kcal }.round1(),
    proteinG = sumOf { it.proteinG }.round1(),
    fatG = sumOf { it.fatG }.round1(),
    carbsG = sumOf { it.carbsG }.round1(),
    fiberG = sumOf { it.fiberG }.round1(),
    sodiumMg = sumOf { it.sodiumMg }.round1(),
)
