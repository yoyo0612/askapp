package com.example.healthapp.nutrition

import org.junit.Assert.assertEquals
import org.junit.Test

class NutritionTest {

    private val rice = Nutrients(
        kcal = 168.0,
        proteinG = 2.5,
        fatG = 0.3,
        carbsG = 37.1,
        fiberG = 0.3,
        sodiumMg = 1.0,
    )

    @Test
    fun `scales per 100g values by grams`() {
        val result = rice.forGrams(150.0)
        assertEquals(252.0, result.kcal, 0.001)
        assertEquals(3.8, result.proteinG, 0.001)
        assertEquals(55.7, result.carbsG, 0.001)
    }

    @Test
    fun `100g equals the per 100g values`() {
        val result = rice.forGrams(100.0)
        assertEquals(168.0, result.kcal, 0.001)
        assertEquals(1.0, result.sodiumMg, 0.001)
    }

    @Test
    fun `total sums each nutrient`() {
        val total = listOf(rice.forGrams(100.0), rice.forGrams(200.0)).total()
        assertEquals(504.0, total.kcal, 0.001)
        assertEquals(0.9, total.fiberG, 0.001)
    }

    @Test
    fun `total of nothing is zero`() {
        assertEquals(Nutrients.ZERO, emptyList<Nutrients>().total())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects non positive grams`() {
        rice.forGrams(0.0)
    }
}
