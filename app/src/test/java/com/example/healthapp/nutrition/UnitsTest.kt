package com.example.healthapp.nutrition

import org.junit.Assert.assertEquals
import org.junit.Test

class UnitsTest {

    @Test
    fun `kilojoules convert to kcal`() {
        // 2252 kJ（Nutella 100g の値）÷ 4.184 = 538.24… → 538.2
        assertEquals(538.2, kilojouleToKcal(2252.0), 0.001)
    }

    @Test
    fun `zero stays zero`() {
        assertEquals(0.0, kilojouleToKcal(0.0), 0.001)
    }
}
