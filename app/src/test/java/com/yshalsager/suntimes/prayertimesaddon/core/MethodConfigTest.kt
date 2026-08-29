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
    fun normalized_repairs_out_of_domain_values() {
        val cfg =
            MethodConfig.defaults().copy(
                method_preset = "unknown",
                fajr_angle = Double.POSITIVE_INFINITY,
                isha_mode = "unknown",
                isha_angle = -1.0,
                isha_fixed_minutes = 1441,
                asr_factor = 3,
                maghrib_offset_minutes = -1441,
                makruh_angle = 91.0,
                makruh_sunrise_minutes = 11,
                zawal_minutes = -1
            ).normalized()

        assertEquals(MethodConfig.defaults(), cfg)
    }

    @Test
    fun addon_runtime_profile_repairs_out_of_domain_angles() {
        val profile =
            AddonRuntimeProfile.defaults().copy(
                extra_fajr_1_angle = -1.0,
                extra_isha_1_angle = Double.NaN
            ).normalized()

        assertEquals(18.0, profile.extra_fajr_1_angle, 0.0001)
        assertEquals(18.0, profile.extra_isha_1_angle, 0.0001)
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
