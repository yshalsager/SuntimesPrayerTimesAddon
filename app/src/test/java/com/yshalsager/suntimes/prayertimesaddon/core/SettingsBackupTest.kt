package com.yshalsager.suntimes.prayertimesaddon.core

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SettingsBackupTest {
    private lateinit var context: Context

    @Before
    fun set_up() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE).edit().clear().apply()
    }

    @Test
    fun encode_parse_roundtrip_preserves_values() {
        val values = linkedMapOf<String, Any>(
            "theme" to Prefs.theme_dark,
            "language" to "ar",
            "days_show_hijri" to false,
            "hijri_day_offset" to 2,
            "method_preset" to "uiof",
            "fajr_angle" to 16.2,
            "extra_fajr_1_enabled" to true,
            "extra_fajr_1_angle" to 18.3,
            "extra_fajr_1_label" to "Fajr Secondary",
            "asr_factor" to 2,
            "extra_isha_1_enabled" to true,
            "extra_isha_1_angle" to 17.8,
            "extra_isha_1_label" to "Isha Secondary",
            "makruh_sunrise_minutes" to 20,
            "host_event_authority" to "com.forrestguice.suntimeswidget.event.provider",
            "saved_locations_json" to """[{"id":"1","label":"Cairo","latitude":"30.0","longitude":"31.0","timezone_id":"Africa/Cairo"}]""",
            "home_location_source" to SavedLocations.home_source_saved,
            "home_location_id" to "1"
        )

        val raw = SettingsBackup.encode_json(values)
        val parsed = SettingsBackup.parse_json(raw)

        assertNotNull(parsed)
        val restored = parsed!!.values
        assertEquals(Prefs.theme_dark, restored["theme"])
        assertEquals("ar", restored["language"])
        assertEquals(false, restored["days_show_hijri"])
        assertEquals(2, restored["hijri_day_offset"])
        assertEquals("uiof", restored["method_preset"])
        assertEquals(16.2, restored["fajr_angle"])
        assertEquals(true, restored["extra_fajr_1_enabled"])
        assertEquals(18.3, restored["extra_fajr_1_angle"])
        assertEquals("Fajr Secondary", restored["extra_fajr_1_label"])
        assertEquals(2, restored["asr_factor"])
        assertEquals(true, restored["extra_isha_1_enabled"])
        assertEquals(17.8, restored["extra_isha_1_angle"])
        assertEquals("Isha Secondary", restored["extra_isha_1_label"])
        assertEquals(20, restored["makruh_sunrise_minutes"])
        assertEquals("com.forrestguice.suntimeswidget.event.provider", restored["host_event_authority"])
        assertEquals("""[{"id":"1","label":"Cairo","latitude":"30.0","longitude":"31.0","timezone_id":"Africa/Cairo"}]""", restored["saved_locations_json"])
        assertEquals(SavedLocations.home_source_saved, restored["home_location_source"])
        assertEquals("1", restored["home_location_id"])
        assertEquals(0, parsed.skipped_count)
    }

    @Test
    fun parse_rejects_invalid_schema_version() {
        val raw = """
            {
              "schema_version": 2,
              "prefs": { "theme": "dark" }
            }
        """.trimIndent()

        val parsed = SettingsBackup.parse_json(raw)
        assertNull(parsed)
    }

    @Test
    fun parse_merge_subset_contains_only_present_keys() {
        val raw = """
            {
              "schema_version": 1,
              "prefs": { "theme": "dark" }
            }
        """.trimIndent()

        val parsed = SettingsBackup.parse_json(raw)

        assertNotNull(parsed)
        assertEquals(1, parsed!!.values.size)
        assertEquals(Prefs.theme_dark, parsed.values["theme"])
    }

    @Test
    fun parse_ignores_unknown_keys() {
        val raw = """
            {
              "schema_version": 1,
              "prefs": {
                "theme": "dark",
                "unknown_key": 123
              }
            }
        """.trimIndent()

        val parsed = SettingsBackup.parse_json(raw)

        assertNotNull(parsed)
        assertEquals(Prefs.theme_dark, parsed!!.values["theme"])
        assertEquals(false, parsed.values.containsKey("unknown_key"))
        assertEquals(0, parsed.skipped_count)
    }

    @Test
    fun parse_skips_invalid_values_and_keeps_valid_values() {
        val raw = """
            {
              "schema_version": 1,
              "prefs": {
                "asr_factor": "bad",
                "theme": "dark",
                "hijri_day_offset": 9,
                "fajr_angle": "NaN",
                "home_location_source": "invalid"
              }
            }
        """.trimIndent()

        val parsed = SettingsBackup.parse_json(raw)

        assertNotNull(parsed)
        assertEquals(Prefs.theme_dark, parsed!!.values["theme"])
        assertEquals(false, parsed.values.containsKey("asr_factor"))
        assertEquals(false, parsed.values.containsKey("hijri_day_offset"))
        assertEquals(false, parsed.values.containsKey("fajr_angle"))
        assertEquals(false, parsed.values.containsKey("home_location_source"))
        assertEquals(4, parsed.skipped_count)
    }

    @Test
    fun parse_rejects_malformed_json() {
        val parsed = SettingsBackup.parse_json("{not-valid-json")
        assertNull(parsed)
    }

    @Test
    fun import_applies_all_valid_values() {
        val raw = """
            {
              "schema_version": 1,
              "prefs": {
                "theme": "dark",
                "language": "ar",
                "days_show_hijri": false,
                "asr_factor": 2
              }
            }
        """.trimIndent()

        val applied = linkedMapOf<String, Any>()
        val result = SettingsBackup.import_json(raw) { values ->
            applied.putAll(values)
            values.size
        }

        assertTrue(result.ok)
        assertEquals(4, result.applied_count)
        assertEquals(0, result.skipped_count)
        assertEquals(Prefs.theme_dark, applied["theme"])
        assertEquals("ar", applied["language"])
        assertEquals(false, applied["days_show_hijri"])
        assertEquals(2, applied["asr_factor"])
    }

    @Test
    fun import_reports_skipped_values_and_applier_count() {
        val raw = """
            {
              "schema_version": 1,
              "prefs": {
                "theme": "dark",
                "asr_factor": 3
              }
            }
        """.trimIndent()

        val result = SettingsBackup.import_json(raw) { 0 }

        assertTrue(result.ok)
        assertEquals(0, result.applied_count)
        assertEquals(1, result.skipped_count)
    }

    @Test
    fun import_fails_when_file_has_no_valid_supported_values() {
        val raw = """
            {
              "schema_version": 1,
              "prefs": {
                "asr_factor": "bad",
                "unknown_key": 1
              }
            }
        """.trimIndent()

        val result = SettingsBackup.import_json(raw) { values -> values.size }

        assertEquals(false, result.ok)
        assertEquals(0, result.applied_count)
        assertEquals(1, result.skipped_count)
    }

    @Test
    fun parse_normalizes_boolean_strings_and_skips_invalid_entries() {
        val raw = """
            {
              "schema_version": 1,
              "prefs": {
                "theme": "dark",
                "days_show_hijri": "false",
                "asr_factor": 3,
                "unknown_key": 1
              }
            }
        """.trimIndent()

        val parsed = SettingsBackup.parse_json(raw)

        assertNotNull(parsed)
        assertEquals(2, parsed!!.values.size)
        assertEquals(Prefs.theme_dark, parsed.values["theme"])
        assertEquals(false, parsed.values["days_show_hijri"])
        assertEquals(false, parsed.values.containsKey("asr_factor"))
        assertEquals(1, parsed.skipped_count)
    }

    @Test
    fun parse_skips_out_of_range_integer_values() {
        val raw = """
            {
              "schema_version": 1,
              "prefs": {
                "theme": "dark",
                "isha_fixed_minutes": 3000000000
              }
            }
        """.trimIndent()

        val parsed = SettingsBackup.parse_json(raw)

        assertNotNull(parsed)
        assertEquals(Prefs.theme_dark, parsed!!.values["theme"])
        assertEquals(false, parsed.values.containsKey("isha_fixed_minutes"))
        assertEquals(1, parsed.skipped_count)
    }

    @Test
    fun export_preserves_blank_extra_labels_as_blank() {
        Prefs.set_extra_fajr_1_label(context, "")
        Prefs.set_extra_isha_1_label(context, "")

        val raw = SettingsBackup.export_json(context)
        val parsed = SettingsBackup.parse_json(raw)

        assertNotNull(parsed)
        assertEquals("", parsed!!.values["extra_fajr_1_label"])
        assertEquals("", parsed.values["extra_isha_1_label"])
    }
}
