package com.doitsh.app.data.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The theme setting's one piece of real logic: which colour scheme a stored mode means.
 *
 * The decode is tested because it is what a SharedPreferences value goes through, and the
 * fallback is the part that keeps a missing key (every existing install) or an unknown
 * value from throwing at launch.
 */
class ThemeModeTest {

    @Test
    fun darkAndLightIgnoreTheSystem() {
        assertTrue(ThemeMode.DARK.isDark(systemInDarkTheme = false))
        assertTrue(ThemeMode.DARK.isDark(systemInDarkTheme = true))
        assertFalse(ThemeMode.LIGHT.isDark(systemInDarkTheme = false))
        assertFalse(ThemeMode.LIGHT.isDark(systemInDarkTheme = true))
    }

    @Test
    fun systemFollowsTheDevice() {
        assertTrue(ThemeMode.SYSTEM.isDark(systemInDarkTheme = true))
        assertFalse(ThemeMode.SYSTEM.isDark(systemInDarkTheme = false))
    }

    @Test
    fun missingOrUnknownValuesFallBackToSystem() {
        // The key was never written on installs that predate this setting.
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromName(null))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromName("SEPIA"))
    }

    @Test
    fun everyStoredNameRoundTrips() {
        ThemeMode.entries.forEach { mode ->
            assertEquals(mode, ThemeMode.fromName(mode.name))
        }
    }
}
