package com.focustube.app

import com.focustube.app.data.model.FocusPreferences
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReelsGatingTest {

    @Test
    fun testReels_disabledByDefault() {
        val defaultPrefs = FocusPreferences()
        assertFalse("Reels must be strictly disabled by default", defaultPrefs.reelsEnabled)
    }

    @Test
    fun testReels_canBeEnabledExplicitly() {
        val defaultPrefs = FocusPreferences()
        val enabledPrefs = defaultPrefs.copy(reelsEnabled = true)
        assertTrue(enabledPrefs.reelsEnabled)
    }

    @Test
    fun testShortsExclusion_enabledByDefault() {
        val defaultPrefs = FocusPreferences()
        assertTrue("Shorts must be excluded from Discover by default", defaultPrefs.excludeShortsFromDiscover)
        assertTrue("Shorts must be excluded from Search by default", defaultPrefs.excludeShortsFromSearch)
    }
}
