package com.yshalsager.suntimes.prayertimesaddon.core

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class SavedLocationsTest {
    private lateinit var context: Context

    @Before
    fun set_up() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE).edit().clear().apply()
    }

    @Test
    fun encode_parse_roundtrip_preserves_order() {
        val input =
            listOf(
                SavedLocation("1", "Cairo", "30.0444", "31.2357", null, "Africa/Cairo"),
                SavedLocation("2", "Makkah", "21.3891", "39.8579", "277", "Asia/Riyadh")
            )

        val encoded = SavedLocations.encode_json(input)
        val parsed = SavedLocations.parse_json(encoded)

        assertEquals(2, parsed.size)
        assertEquals("1", parsed[0].id)
        assertEquals("2", parsed[1].id)
        assertEquals("277", parsed[1].altitude)
    }

    @Test
    fun parse_skips_invalid_items() {
        val raw =
            """
            [
              {"id":"ok","label":"A","latitude":"10","longitude":"20","timezone_id":"UTC"},
              {"id":"","label":"B","latitude":"10","longitude":"20","timezone_id":"UTC"},
              {"id":"bad-lat","label":"C","latitude":"200","longitude":"20","timezone_id":"UTC"}
            ]
            """.trimIndent()

        val parsed = SavedLocations.parse_json(raw)

        assertEquals(1, parsed.size)
        assertEquals("ok", parsed.first().id)
    }

    @Test
    fun parse_skips_invalid_timezone_id() {
        val raw =
            """
            [
              {"id":"ok","label":"A","latitude":"10","longitude":"20","timezone_id":"UTC"},
              {"id":"bad-tz","label":"B","latitude":"10","longitude":"20","timezone_id":"Mars/Base"}
            ]
            """.trimIndent()

        val parsed = SavedLocations.parse_json(raw)

        assertEquals(1, parsed.size)
        assertEquals("ok", parsed.first().id)
    }

    @Test
    fun parse_invalid_altitude_normalizes_to_null() {
        val raw =
            """
            [
              {"id":"ok","label":"A","latitude":"10","longitude":"20","timezone_id":"UTC","altitude":"abc"}
            ]
            """.trimIndent()

        val parsed = SavedLocations.parse_json(raw)

        assertEquals(1, parsed.size)
        assertNull(parsed.first().altitude)
    }

    @Test
    fun save_enforces_max_count() {
        val many =
            (1..20).map {
                SavedLocation(
                    id = it.toString(),
                    label = "L$it",
                    latitude = "10.0",
                    longitude = "20.0",
                    altitude = null,
                    timezone_id = "UTC"
                )
            }

        SavedLocations.save(context, many)
        val loaded = SavedLocations.load(context)

        assertEquals(SavedLocations.max_count, loaded.size)
        assertEquals("1", loaded.first().id)
        assertEquals("10", loaded.last().id)
    }

    @Test
    fun build_selection_without_altitude_uses_base_args() {
        val location = SavedLocation("1", "Cairo", "30.0", "31.0", null, "Africa/Cairo")
        val (selection, args) = SavedLocations.build_selection(12345L, location)

        assertTrue(selection.contains("latitude=?"))
        assertTrue(selection.contains("longitude=?"))
        assertTrue(selection.contains("timezone=?"))
        assertFalse(selection.contains("altitude=?"))
        assertEquals(7, args.size)
        assertEquals("30.0", args[4])
        assertEquals("31.0", args[5])
        assertEquals("Africa/Cairo", args[6])
    }

    @Test
    fun build_selection_with_altitude_uses_alt_arg() {
        val location = SavedLocation("1", "Makkah", "21.4", "39.8", "277", "Asia/Riyadh")
        val (selection, args) = SavedLocations.build_selection(12345L, location)

        assertTrue(selection.contains("altitude=?"))
        assertTrue(selection.contains("timezone=?"))
        assertEquals(8, args.size)
        assertEquals("Asia/Riyadh", args[6])
        assertEquals("277", args[7])
    }

    @Test
    fun parse_roundtrip_preserves_custom_method_fields() {
        val input =
            listOf(
                SavedLocation(
                    id = "1",
                    label = "Cairo",
                    latitude = "30.0",
                    longitude = "31.0",
                    altitude = "12",
                    timezone_id = "Africa/Cairo",
                    calc_mode = SavedLocations.calc_mode_custom,
                    method_preset = "mwl",
                    method_fajr_angle = 18.0,
                    method_isha_mode = Prefs.isha_mode_angle,
                    method_isha_angle = 17.0,
                    method_isha_fixed_minutes = 90,
                    method_asr_factor = 2,
                    method_maghrib_offset_minutes = 2,
                    method_makruh_angle = 4.8,
                    method_makruh_sunrise_minutes = 20,
                    method_zawal_minutes = 11,
                    hijri_variant = Prefs.hijri_variant_diyanet,
                    hijri_day_offset = 1,
                    extra_fajr_1_enabled = true,
                    extra_fajr_1_angle = 17.2,
                    extra_fajr_1_label_raw = "City Fajr",
                    extra_isha_1_enabled = true,
                    extra_isha_1_angle = 16.8,
                    extra_isha_1_label_raw = "City Isha"
                )
            )

        val parsed = SavedLocations.parse_json(SavedLocations.encode_json(input))
        val saved = parsed.single()

        assertEquals(SavedLocations.calc_mode_custom, saved.calc_mode)
        assertEquals("mwl", saved.method_preset)
        assertEquals(18.0, saved.method_fajr_angle, 0.0001)
        assertEquals(Prefs.isha_mode_angle, saved.method_isha_mode)
        assertEquals(17.0, saved.method_isha_angle, 0.0001)
        assertEquals(2, saved.method_asr_factor)
        assertEquals(2, saved.method_maghrib_offset_minutes)
        assertEquals(20, saved.method_makruh_sunrise_minutes)
        assertEquals(11, saved.method_zawal_minutes)
        assertEquals(Prefs.hijri_variant_diyanet, saved.hijri_variant)
        assertEquals(1, saved.hijri_day_offset)
        assertTrue(saved.extra_fajr_1_enabled)
        assertEquals(17.2, saved.extra_fajr_1_angle, 0.0001)
        assertEquals("City Fajr", saved.extra_fajr_1_label_raw)
        assertTrue(saved.extra_isha_1_enabled)
        assertEquals(16.8, saved.extra_isha_1_angle, 0.0001)
        assertEquals("City Isha", saved.extra_isha_1_label_raw)
    }

    @Test
    fun find_matching_location_requires_altitude_match_when_present() {
        val locations =
            listOf(
                SavedLocation("1", "No Alt", "30.0", "31.0", null, "Africa/Cairo"),
                SavedLocation("2", "With Alt", "30.0", "31.0", "100", "Africa/Cairo", calc_mode = SavedLocations.calc_mode_custom, method_asr_factor = 2)
            )
        SavedLocations.save(context, locations)

        val matched = SavedLocations.find_matching_location(context, "30.0", "31.0", "100")

        assertEquals("2", matched?.id)
    }

    @Test
    fun find_matching_location_falls_back_to_coordinate_match_when_altitude_differs() {
        val locations =
            listOf(
                SavedLocation("1", "No Alt", "30.0", "31.0", null, "Africa/Cairo"),
                SavedLocation("2", "With Alt", "30.0", "31.0", "100", "Africa/Cairo")
            )
        SavedLocations.save(context, locations)

        val matched = SavedLocations.find_matching_location(context, "30.0", "31.0", "0.0")

        assertEquals("1", matched?.id)
    }
}
