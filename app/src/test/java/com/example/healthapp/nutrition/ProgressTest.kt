package com.example.healthapp.nutrition

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProgressTest {

    @Test
    fun `achievement percent is rounded`() {
        assertEquals(50, achievementPercent(1000.0, 2000.0))
        assertEquals(67, achievementPercent(2.0, 3.0))
        assertEquals(120, achievementPercent(2400.0, 2000.0))
    }

    @Test
    fun `achievement percent is zero when goal is not set`() {
        assertEquals(0, achievementPercent(500.0, 0.0))
    }

    @Test
    fun `weight change is last minus first`() {
        assertEquals(-1.0, weightChange(listOf(70.0, 68.5, 69.0))!!, 0.001)
    }

    @Test
    fun `weight change needs two entries`() {
        assertNull(weightChange(listOf(70.0)))
        assertNull(weightChange(emptyList()))
    }
}
