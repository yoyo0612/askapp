package com.example.healthapp.nutrition

import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseTest {

    @Test
    fun `one hour of walking at 3_5 MET for 60kg burns 210 kcal`() {
        assertEquals(210.0, exerciseKcal(met = 3.5, weightKg = 60.0, minutes = 60.0), 0.001)
    }

    @Test
    fun `half an hour of jogging for 70kg burns 245 kcal`() {
        assertEquals(245.0, exerciseKcal(met = 7.0, weightKg = 70.0, minutes = 30.0), 0.001)
    }

    @Test
    fun `result is rounded to one decimal`() {
        // 3.5 × 61.3 × 10 / 60 = 35.758… → 35.8
        assertEquals(35.8, exerciseKcal(met = 3.5, weightKg = 61.3, minutes = 10.0), 0.001)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects zero minutes`() {
        exerciseKcal(met = 3.5, weightKg = 60.0, minutes = 0.0)
    }
}
