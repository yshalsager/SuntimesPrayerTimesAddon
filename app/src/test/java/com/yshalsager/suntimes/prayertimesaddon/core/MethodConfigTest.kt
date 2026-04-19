package com.yshalsager.suntimes.prayertimesaddon.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MethodConfigTest {
    @Test
    fun supported_presets_includes_uiof() {
        assertTrue(MethodConfig.supported_presets.contains("uiof"))
    }

    @Test
    fun method_config_with_uiof_sets_expected_angles() {
        val base = MethodConfig.defaults()

        val cfg = method_config_with_preset(base, "uiof")

        assertEquals("uiof", cfg.method_preset)
        assertEquals(12.0, cfg.fajr_angle, 0.0001)
        assertEquals(Prefs.isha_mode_angle, cfg.isha_mode)
        assertEquals(12.0, cfg.isha_angle, 0.0001)
    }
}
