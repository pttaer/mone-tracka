package com.monetracka.shared.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class UserProfileTest {

    @Test
    fun testInitialsExtractionSingleWord() {
        val profile = UserProfile(userName = "Alice")
        assertEquals("A", profile.initials)
    }

    @Test
    fun testInitialsExtractionMultipleWords() {
        val profile = UserProfile(userName = "Thanh Pham")
        assertEquals("TP", profile.initials)
    }

    @Test
    fun testInitialsExtractionMoreThanTwoWords() {
        val profile = UserProfile(userName = "John Robert Doe")
        assertEquals("JR", profile.initials)
    }

    @Test
    fun testInitialsExtractionWithExtraSpaces() {
        val profile = UserProfile(userName = "  Bruce   Wayne  ")
        assertEquals("BW", profile.initials)
    }

    @Test
    fun testInitialsEmptyFallback() {
        val profile = UserProfile(userName = "   ")
        assertEquals("U", profile.initials)
    }

    @Test
    fun testUserProfileIncludesMonthlyBudgetLimit() {
        val defaultProfile = UserProfile(userName = "Thanh")
        assertEquals(2500.0, defaultProfile.monthlyBudgetLimit)

        val customProfile = UserProfile(userName = "Thanh", monthlyBudgetLimit = 3500.0)
        assertEquals(3500.0, customProfile.monthlyBudgetLimit)
    }
}
