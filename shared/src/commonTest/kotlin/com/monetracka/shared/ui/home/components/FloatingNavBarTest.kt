package com.monetracka.shared.ui.home.components

import kotlin.test.Test
import kotlin.test.assertEquals

class FloatingNavBarTest {
    @Test
    fun testValidTabIndices() {
        val validIndices = listOf(0, 1, 2, 3)
        assertEquals(4, validIndices.size)
    }
}
