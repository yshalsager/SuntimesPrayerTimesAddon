package com.yshalsager.suntimes.prayertimesaddon.core

import android.content.Context
import org.junit.Assert.assertEquals
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
class LocationQueryContextTest {
    private lateinit var context: Context
    private lateinit var saved_locations: List<SavedLocation>

    @Before
    fun set_up() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE).edit().clear().apply()
        saved_locations =
            listOf(
                SavedLocation(id = "loc-a", label = "A", latitude = "30.0", longitude = "31.0", altitude = null, timezone_id = "UTC"),
                SavedLocation(
                    id = "loc-b",
                    label = "B",
                    latitude = "55.0",
                    longitude = "37.0",
                    altitude = "100.0",
                    timezone_id = "Africa/Cairo",
                    calc_mode = SavedLocations.calc_mode_custom,
                    extra_fajr_1_enabled = true,
                    hijri_day_offset = 1
                )
            )
    }

    @Test
    fun resolve_location_query_context_prioritizes_saved_location_id_over_coordinates() {
        val resolved =
            resolve_location_query_context(
                context = context,
                saved_location_id = "loc-b",
                latitude = "30.0",
                longitude = "31.0",
                altitude = null,
                saved_locations = saved_locations
            )

        assertEquals(SavedLocations.home_source_saved, resolved.source)
        assertEquals("loc-b", resolved.resolved_saved_location_id)
        assertEquals("55.0", resolved.latitude)
        assertEquals(55.0, resolved.latitude_override ?: -1.0, 0.0001)
        assertEquals(1, resolved.addon_runtime_profile_override?.hijri_day_offset)
        assertEquals(true, resolved.addon_runtime_profile_override?.extra_fajr_1_enabled)
    }

    @Test
    fun resolve_location_query_context_falls_back_to_coordinate_match_when_id_missing() {
        val resolved =
            resolve_location_query_context(
                context = context,
                saved_location_id = "missing",
                latitude = "30.0",
                longitude = "31.0",
                altitude = null,
                saved_locations = saved_locations
            )

        assertEquals(SavedLocations.home_source_saved, resolved.source)
        assertEquals("loc-a", resolved.resolved_saved_location_id)
        assertEquals("30.0", resolved.latitude)
    }

    @Test
    fun resolve_location_query_context_falls_back_to_host_when_unresolvable() {
        val resolved =
            resolve_location_query_context(
                context = context,
                saved_location_id = "missing",
                latitude = null,
                longitude = null,
                altitude = null,
                saved_locations = saved_locations
            )

        assertEquals(SavedLocations.home_source_host, resolved.source)
        assertNull(resolved.saved_location)
        assertNull(resolved.resolved_saved_location_id)
        assertNull(resolved.timezone_override)
        assertNull(resolved.method_config_override)
        assertNull(resolved.addon_runtime_profile_override)
    }
}
