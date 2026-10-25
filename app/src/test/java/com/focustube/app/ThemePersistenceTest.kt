package com.focustube.app

import com.focustube.app.data.model.FocusPreferences
import com.focustube.app.data.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemePersistenceTest {

    @Test
    fun testDefaultTheme_isSystem() {
        val prefs = FocusPreferences()
        assertEquals(ThemeMode.SYSTEM, prefs.themeMode)
    }

    @Test
    fun testThemeModes_supportDarkAndLight() {
        val darkPrefs = FocusPreferences(themeMode = ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, darkPrefs.themeMode)

        val lightPrefs = FocusPreferences(themeMode = ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, lightPrefs.themeMode)
    }
}
