package com.monetracka.shared.ui.home.components

import com.monetracka.shared.domain.model.CategorySpend
import kotlin.test.Test
import kotlin.test.assertEquals

class CategoryDonutChartTest {
    @Test
    fun testTotalDegreesSumsTo360() {
        val list = listOf(
            CategorySpend("Food", 60.0, 100.0, 0),
            CategorySpend("Transport", 40.0, 100.0, 1)
        )
        val totalDegrees = list.sumOf { (it.percentage / 100.0) * 360.0 }
        assertEquals(360.0, totalDegrees, 0.01)
    }
}
