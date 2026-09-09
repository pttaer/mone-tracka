package com.monetracka.shared.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ThemeTokensTest {
    @Test
    fun testCoreDesignTokensAreDefined() {
        assertEquals(Color(0xFF060B11), MoneTrackaColors.BackgroundOled)
        assertEquals(Color(0xFF00D09C), MoneTrackaColors.MintPrimary)
        assertEquals(Color(0xFF00B2FF), MoneTrackaColors.CyanAccent)
        assertEquals(Color(0xFFFF5A79), MoneTrackaColors.CoralDanger)
        assertEquals(Color(0xFFFFB300), MoneTrackaColors.AmberWarning)
        assertEquals(Color(0xFF9D65FF), MoneTrackaColors.VioletInsight)
        assertTrue(CategoryColors.size >= 10)
    }
}
