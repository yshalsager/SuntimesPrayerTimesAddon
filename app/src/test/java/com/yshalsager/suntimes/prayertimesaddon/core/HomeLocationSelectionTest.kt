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
class HomeLocationSelectionTest {
    private lateinit var context: Context

    @Before
    fun set_up() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE).edit().clear().apply()
    }

    @Test
    fun resolve_selected_home_location_defaults_to_host() {
        val selected = resolve_selected_home_location(context, "Mecca", "Asia/Riyadh", emptyList())

        assertEquals(SavedLocations.home_source_host, selected.key)
        assertEquals("Mecca", selected.label)
        assertEquals("Asia/Riyadh", selected.timezone_id)
        assertNull(selected.saved_location)
        assertNull(selected.method_config_override)
    }

    @Test
    fun resolve_selected_home_location_normalizes_missing_saved_selection() {
        Prefs.set_home_location_source(context, SavedLocations.home_source_saved)
        Prefs.set_home_location_id(context, "missing-id")

        val selected = resolve_selected_home_location(context, "Host", "Africa/Cairo", emptyList())

        assertEquals(SavedLocations.home_source_host, selected.key)
        assertEquals(SavedLocations.home_source_host, Prefs.get_home_location_source(context))
        assertEquals("", Prefs.get_home_location_id(context))
    }

    @Test
    fun select_home_location_by_key_validates_saved_ids() {
        val saved = SavedLocation("loc-1", "Cairo", "30.0", "31.0", null, "Africa/Cairo")
        SavedLocations.save(context, listOf(saved))

        assertFalse(select_home_location_by_key(context, "saved:missing", SavedLocations.load(context)))
        assertTrue(select_home_location_by_key(context, home_location_key(saved.id), SavedLocations.load(context)))
        assertEquals(SavedLocations.home_source_saved, Prefs.get_home_location_source(context))
        assertEquals(saved.id, Prefs.get_home_location_id(context))
    }
}
