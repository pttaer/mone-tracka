package com.monetracka.shared.ui.home.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SparklineMathTest {
    @Test
    fun testNormalizePointsProducesValidFractions() {
        val points = listOf(10f, 20f, 50f, 30f)
        val normalized = points.map { (it - 10f) / (50f - 10f) }
        assertEquals(0f, normalized.first())
        assertEquals(1f, normalized[2])
        assertTrue(normalized.all { it in 0f..1f })
    }
}
