package com.yshalsager.suntimes.prayertimesaddon.provider

import android.content.Context
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.net.Uri
import com.yshalsager.suntimes.prayertimesaddon.FakeHostCalcProvider
import com.yshalsager.suntimes.prayertimesaddon.FakeHostEventProvider
import com.yshalsager.suntimes.prayertimesaddon.core.AlarmEventContract
import com.yshalsager.suntimes.prayertimesaddon.core.HostConfigReader
import com.yshalsager.suntimes.prayertimesaddon.core.Prefs
import com.yshalsager.suntimes.prayertimesaddon.core.SavedLocation
import com.yshalsager.suntimes.prayertimesaddon.core.SavedLocations
import com.yshalsager.suntimes.prayertimesaddon.host_calc_authority
import com.yshalsager.suntimes.prayertimesaddon.host_event_authority
import com.yshalsager.suntimes.prayertimesaddon.core.hijri_for_day
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PrayerTimesProviderTest {
    private lateinit var context: Context

    @Before
    fun set_up() {
        context = RuntimeEnvironment.getApplication()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

        context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE).edit().clear().apply()
        HostConfigReader.clear_cache()
        Prefs.set_host_event_authority(context, host_event_authority)

        Robolectric.setupContentProvider(PrayerTimesProvider::class.java, PrayerTimesProvider.authority)
        Robolectric.setupContentProvider(FakeHostEventProvider::class.java, host_event_authority)
        Robolectric.setupContentProvider(FakeHostCalcProvider::class.java, host_calc_authority)

        val shadow_package_manager = shadowOf(context.packageManager)
        shadow_package_manager.addOrUpdateProvider(
            ProviderInfo().apply {
                authority = host_event_authority
                packageName = context.packageName
                name = FakeHostEventProvider::class.java.name
                applicationInfo = context.applicationInfo
            }
        )
        shadow_package_manager.addOrUpdateProvider(
            ProviderInfo().apply {
                authority = host_calc_authority
                packageName = context.packageName
                name = FakeHostCalcProvider::class.java.name
                applicationInfo = context.applicationInfo
            }
        )
    }

    @Test
    fun eid_event_calc_has_rows_on_eid_day() {
        val day_start = find_eid_day_start()

        query_event_calc("PRAYER_EID_FITR_START", day_start).use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals(
                day_start + 6 * 60 * 60 * 1000L + 15 * 60 * 1000L,
                cursor.getLong(cursor.getColumnIndexOrThrow(AlarmEventContract.column_event_timemillis))
            )
        }

        query_event_calc("PRAYER_EID_FITR_END", day_start).use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals(
                day_start + 12 * 60 * 60 * 1000L,
                cursor.getLong(cursor.getColumnIndexOrThrow(AlarmEventContract.column_event_timemillis))
            )
        }
    }

    @Test
    fun eid_adha_event_calc_has_rows_on_adha_day() {
        val day_start = find_eid_adha_day_start()

        query_event_calc("PRAYER_EID_ADHA_START", day_start).use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals(
                day_start + 6 * 60 * 60 * 1000L + 15 * 60 * 1000L,
                cursor.getLong(cursor.getColumnIndexOrThrow(AlarmEventContract.column_event_timemillis))
            )
        }

        query_event_calc("PRAYER_EID_ADHA_END", day_start).use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals(
                day_start + 12 * 60 * 60 * 1000L,
                cursor.getLong(cursor.getColumnIndexOrThrow(AlarmEventContract.column_event_timemillis))
            )
        }
    }

    @Test
    fun eid_event_calc_uses_location_selection_for_sun_query() {
        val day_start = find_eid_day_start()
        val selection = "${AlarmEventContract.extra_alarm_now}=? AND ${AlarmEventContract.extra_alarm_offset}=? AND ${AlarmEventContract.extra_alarm_repeat}=? AND ${AlarmEventContract.extra_alarm_repeat_days}=? AND latitude=? AND longitude=? AND altitude=?"
        val selection_args = arrayOf(day_start.toString(), "0", "false", "[]", "55.0", "37.0", "100.0")

        query_event_calc("PRAYER_EID_FITR_START", selection, selection_args).use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals(
                day_start + 6 * 60 * 60 * 1000L + 45 * 60 * 1000L,
                cursor.getLong(cursor.getColumnIndexOrThrow(AlarmEventContract.column_event_timemillis))
            )
        }

        query_event_calc("PRAYER_EID_FITR_END", selection, selection_args).use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals(
                day_start + 12 * 60 * 60 * 1000L + 30 * 60 * 1000L,
                cursor.getLong(cursor.getColumnIndexOrThrow(AlarmEventContract.column_event_timemillis))
            )
        }
    }

    @Test
    fun eid_event_calc_returns_next_occurrence_on_non_eid_day() {
        val day_start = utc_day_start(2026, Calendar.MARCH, 12)
        val next_eid_day_start = find_next_eid_day_start(day_start)

        query_event_calc("PRAYER_EID_FITR_START", day_start).use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals(
                next_eid_day_start + 6 * 60 * 60 * 1000L + 15 * 60 * 1000L,
                cursor.getLong(cursor.getColumnIndexOrThrow(AlarmEventContract.column_event_timemillis))
            )
        }
        query_event_calc("PRAYER_EID_FITR_END", day_start).use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals(
                next_eid_day_start + 12 * 60 * 60 * 1000L,
                cursor.getLong(cursor.getColumnIndexOrThrow(AlarmEventContract.column_event_timemillis))
            )
        }
    }

    @Test
    fun event_calc_uses_saved_location_id_scope_without_coordinate_selection() {
        val day_start = find_eid_day_start()
        SavedLocations.save(
            context,
            listOf(
                SavedLocation(
                    id = "loc-shifted",
                    label = "Shifted",
                    latitude = "55.0",
                    longitude = "37.0",
                    altitude = "100.0",
                    timezone_id = "UTC"
                )
            )
        )

        query_event_calc(
            event_id = "PRAYER_EID_FITR_START",
            selection = null,
            selection_args = arrayOf(day_start.toString(), "0", "false", "[]"),
            saved_location_id = "loc-shifted"
        ).use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals(
                day_start + 6 * 60 * 60 * 1000L + 45 * 60 * 1000L,
                cursor.getLong(cursor.getColumnIndexOrThrow(AlarmEventContract.column_event_timemillis))
            )
        }
    }

    @Test
    fun event_calc_unknown_saved_location_id_falls_back_to_host() {
        val day_start = find_eid_day_start()

        query_event_calc(
            event_id = "PRAYER_EID_FITR_START",
            selection = null,
            selection_args = arrayOf(day_start.toString(), "0", "false", "[]"),
            saved_location_id = "missing-id"
        ).use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals(
                day_start + 6 * 60 * 60 * 1000L + 15 * 60 * 1000L,
                cursor.getLong(cursor.getColumnIndexOrThrow(AlarmEventContract.column_event_timemillis))
            )
        }
    }

    @Test
    fun event_calc_saved_location_id_takes_precedence_over_coordinate_match() {
        val day_start = find_eid_day_start()
        SavedLocations.save(
            context,
            listOf(
                SavedLocation(
                    id = "loc-shifted",
                    label = "Shifted",
                    latitude = "55.0",
                    longitude = "37.0",
                    altitude = "100.0",
                    timezone_id = "UTC"
                )
            )
        )
        val selection =
            "${AlarmEventContract.extra_alarm_now}=? AND ${AlarmEventContract.extra_alarm_offset}=? AND ${AlarmEventContract.extra_alarm_repeat}=? AND ${AlarmEventContract.extra_alarm_repeat_days}=? AND latitude=? AND longitude=?"
        val selection_args = arrayOf(day_start.toString(), "0", "false", "[]", "30.0", "31.0")

        query_event_calc("PRAYER_EID_FITR_START", selection, selection_args, saved_location_id = "loc-shifted").use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals(
                day_start + 6 * 60 * 60 * 1000L + 45 * 60 * 1000L,
                cursor.getLong(cursor.getColumnIndexOrThrow(AlarmEventContract.column_event_timemillis))
            )
        }
    }

    @Test
    fun event_info_lists_eid_events() {
        val uri = Uri.parse("content://${PrayerTimesProvider.authority}/${AlarmEventContract.query_event_info}")
        context.contentResolver.query(uri, arrayOf(AlarmEventContract.column_event_name), null, null, null)!!.use { cursor ->
            val names = ArrayList<String>()
            while (cursor.moveToNext()) names.add(cursor.getString(cursor.getColumnIndexOrThrow(AlarmEventContract.column_event_name)))
            assertTrue(names.contains("PRAYER_EID_FITR_START"))
            assertTrue(names.contains("PRAYER_EID_FITR_END"))
            assertTrue(names.contains("PRAYER_EID_ADHA_START"))
            assertTrue(names.contains("PRAYER_EID_ADHA_END"))
        }
    }

    @Test
    fun event_info_hides_extra_fajr_and_isha_when_disabled() {
        val names = event_info_names()

        assertFalse(names.contains("PRAYER_FAJR_EXTRA_1"))
        assertFalse(names.contains("PRAYER_ISHA_EXTRA_1"))
    }

    @Test
    fun event_info_includes_extra_fajr_and_isha_when_enabled() {
        Prefs.set_extra_fajr_1_enabled(context, true)
        Prefs.set_extra_isha_1_enabled(context, true)

        val names = event_info_names()

        assertTrue(names.contains("PRAYER_FAJR_EXTRA_1"))
        assertTrue(names.contains("PRAYER_ISHA_EXTRA_1"))
    }

    @Test
    fun event_info_scoped_saved_location_uses_custom_extra_visibility_and_labels() {
        SavedLocations.save(
            context,
            listOf(
                SavedLocation(
                    id = "loc-custom",
                    label = "Custom",
                    latitude = "30.0",
                    longitude = "31.0",
                    altitude = null,
                    timezone_id = "UTC",
                    calc_mode = SavedLocations.calc_mode_custom,
                    extra_fajr_1_enabled = true,
                    extra_fajr_1_label_raw = "Local Fajr+",
                    extra_isha_1_enabled = false
                )
            )
        )

        val host_names = event_info_names()
        assertFalse(host_names.contains("PRAYER_FAJR_EXTRA_1"))
        assertFalse(host_names.contains("PRAYER_ISHA_EXTRA_1"))

        query_event_info(saved_location_id = "loc-custom").use { cursor ->
            val names = ArrayList<String>()
            var fajr_extra_title: String? = null
            while (cursor.moveToNext()) {
                val name = cursor.getString(cursor.getColumnIndexOrThrow(AlarmEventContract.column_event_name))
                names.add(name)
                if (name == "PRAYER_FAJR_EXTRA_1") {
                    fajr_extra_title = cursor.getString(cursor.getColumnIndexOrThrow(AlarmEventContract.column_event_title))
                }
            }
            assertTrue(names.contains("PRAYER_FAJR_EXTRA_1"))
            assertFalse(names.contains("PRAYER_ISHA_EXTRA_1"))
            assertEquals("Local Fajr+", fajr_extra_title)
        }
    }

    @Test
    fun extra_fajr_and_isha_calc_rows_require_enable_flag() {
        val day_start = utc_day_start(2026, Calendar.MARCH, 12)

        query_event_calc("PRAYER_FAJR_EXTRA_1", day_start).use { cursor -> assertEquals(0, cursor.count) }
        query_event_calc("PRAYER_ISHA_EXTRA_1", day_start).use { cursor -> assertEquals(0, cursor.count) }

        Prefs.set_extra_fajr_1_enabled(context, true)
        Prefs.set_extra_isha_1_enabled(context, true)

        query_event_calc("PRAYER_FAJR_EXTRA_1", day_start).use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals(day_start + 5 * 60 * 60 * 1000L, cursor.getLong(cursor.getColumnIndexOrThrow(AlarmEventContract.column_event_timemillis)))
        }
        query_event_calc("PRAYER_ISHA_EXTRA_1", day_start).use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals(day_start + 19 * 60 * 60 * 1000L + 30 * 60 * 1000L, cursor.getLong(cursor.getColumnIndexOrThrow(AlarmEventContract.column_event_timemillis)))
        }
    }

    @Test
    fun event_calc_saved_location_custom_profile_can_enable_extras_when_global_disabled() {
        val day_start = utc_day_start(2026, Calendar.MARCH, 12)
        SavedLocations.save(
            context,
            listOf(
                SavedLocation(
                    id = "loc-custom",
                    label = "Custom",
                    latitude = "30.0",
                    longitude = "31.0",
                    altitude = null,
                    timezone_id = "UTC",
                    calc_mode = SavedLocations.calc_mode_custom,
                    extra_fajr_1_enabled = true,
                    extra_isha_1_enabled = true
                )
            )
        )

        query_event_calc(
            event_id = "PRAYER_FAJR_EXTRA_1",
            selection = null,
            selection_args = arrayOf(day_start.toString(), "0", "false", "[]"),
            saved_location_id = "loc-custom"
        ).use { cursor -> assertEquals(1, cursor.count) }
        query_event_calc(
            event_id = "PRAYER_ISHA_EXTRA_1",
            selection = null,
            selection_args = arrayOf(day_start.toString(), "0", "false", "[]"),
            saved_location_id = "loc-custom"
        ).use { cursor -> assertEquals(1, cursor.count) }
    }

    @Test
    fun event_calc_saved_location_custom_profile_can_disable_extras_when_global_enabled() {
        val day_start = utc_day_start(2026, Calendar.MARCH, 12)
        Prefs.set_extra_fajr_1_enabled(context, true)
        Prefs.set_extra_isha_1_enabled(context, true)
        SavedLocations.save(
            context,
            listOf(
                SavedLocation(
                    id = "loc-custom",
                    label = "Custom",
                    latitude = "30.0",
                    longitude = "31.0",
                    altitude = null,
                    timezone_id = "UTC",
                    calc_mode = SavedLocations.calc_mode_custom,
                    extra_fajr_1_enabled = false,
                    extra_isha_1_enabled = false
                )
            )
        )

        query_event_calc(
            event_id = "PRAYER_FAJR_EXTRA_1",
            selection = null,
            selection_args = arrayOf(day_start.toString(), "0", "false", "[]"),
            saved_location_id = "loc-custom"
        ).use { cursor -> assertEquals(0, cursor.count) }
        query_event_calc(
            event_id = "PRAYER_ISHA_EXTRA_1",
            selection = null,
            selection_args = arrayOf(day_start.toString(), "0", "false", "[]"),
            saved_location_id = "loc-custom"
        ).use { cursor -> assertEquals(0, cursor.count) }
    }

    @Test
    fun event_calc_saved_location_custom_hijri_offset_controls_eid_detection() {
        val eid_day_start = find_eid_day_start()
        val day_before = eid_day_start - 24L * 60L * 60L * 1000L
        SavedLocations.save(
            context,
            listOf(
                SavedLocation(
                    id = "loc-custom",
                    label = "Custom",
                    latitude = "30.0",
                    longitude = "31.0",
                    altitude = null,
                    timezone_id = "UTC",
                    calc_mode = SavedLocations.calc_mode_custom,
                    hijri_day_offset = 1
                )
            )
        )

        var host_time: Long? = null
        query_event_calc("PRAYER_EID_FITR_START", day_before).use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            host_time = cursor.getLong(cursor.getColumnIndexOrThrow(AlarmEventContract.column_event_timemillis))
        }
        var custom_time: Long? = null
        query_event_calc(
            event_id = "PRAYER_EID_FITR_START",
            selection = null,
            selection_args = arrayOf(day_before.toString(), "0", "false", "[]"),
            saved_location_id = "loc-custom"
        ).use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            custom_time = cursor.getLong(cursor.getColumnIndexOrThrow(AlarmEventContract.column_event_timemillis))
        }
        assertEquals(day_before + 6 * 60 * 60 * 1000L + 15 * 60 * 1000L, custom_time)
        assertTrue(host_time!! > day_before)
    }

    @Test
    fun event_calc_uses_saved_location_custom_method_override() {
        val day_start = utc_day_start(2026, Calendar.MARCH, 12)
        SavedLocations.save(
            context,
            listOf(
                SavedLocation(
                    id = "loc-1",
                    label = "Custom",
                    latitude = "55.0",
                    longitude = "37.0",
                    altitude = "100.0",
                    timezone_id = "UTC",
                    calc_mode = SavedLocations.calc_mode_custom,
                    method_asr_factor = 2
                )
            )
        )
        val selection =
            "${AlarmEventContract.extra_alarm_now}=? AND ${AlarmEventContract.extra_alarm_offset}=? AND ${AlarmEventContract.extra_alarm_repeat}=? AND ${AlarmEventContract.extra_alarm_repeat_days}=? AND latitude=? AND longitude=? AND altitude=?"
        val selection_args = arrayOf(day_start.toString(), "0", "false", "[]", "55.0", "37.0", "100.0")

        query_event_calc("PRAYER_ASR", selection, selection_args).use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals(
                day_start + 16 * 60 * 60 * 1000L + 30 * 60 * 1000L,
                cursor.getLong(cursor.getColumnIndexOrThrow(AlarmEventContract.column_event_timemillis))
            )
        }
    }

    @Test
    fun event_calc_supports_selection_args_with_timezone_before_altitude() {
        val day_start = utc_day_start(2026, Calendar.MARCH, 12)
        val default_location =
            SavedLocation(
                id = "loc-default",
                label = "Default",
                latitude = "55.0",
                longitude = "37.0",
                altitude = null,
                timezone_id = "UTC"
            )
        val custom_location =
            SavedLocation(
                id = "loc-custom",
                label = "Custom",
                latitude = "55.0",
                longitude = "37.0",
                altitude = "100.0",
                timezone_id = "UTC",
                calc_mode = SavedLocations.calc_mode_custom,
                method_asr_factor = 2
            )
        SavedLocations.save(context, listOf(default_location, custom_location))
        val (selection, selection_args) = SavedLocations.build_selection(day_start, custom_location)

        query_event_calc("PRAYER_ASR", selection, selection_args).use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals(
                day_start + 16 * 60 * 60 * 1000L + 30 * 60 * 1000L,
                cursor.getLong(cursor.getColumnIndexOrThrow(AlarmEventContract.column_event_timemillis))
            )
        }
    }

    @Test
    fun night_event_calc_uses_location_selection_for_sun_query() {
        val day_start = utc_day_start(2026, Calendar.MARCH, 12)
        val selection = "${AlarmEventContract.extra_alarm_now}=? AND ${AlarmEventContract.extra_alarm_offset}=? AND ${AlarmEventContract.extra_alarm_repeat}=? AND ${AlarmEventContract.extra_alarm_repeat_days}=? AND latitude=? AND longitude=? AND altitude=?"
        val selection_args = arrayOf(day_start.toString(), "0", "false", "[]", "55.0", "37.0", "100.0")

        query_event_calc("NIGHT_MIDPOINT", selection, selection_args).use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals(
                day_start - 15 * 60 * 1000L,
                cursor.getLong(cursor.getColumnIndexOrThrow(AlarmEventContract.column_event_timemillis))
            )
        }
    }

    @Test
    fun eid_event_calc_differs_between_two_location_overrides() {
        val day_start = find_eid_day_start()
        val selection = "${AlarmEventContract.extra_alarm_now}=? AND ${AlarmEventContract.extra_alarm_offset}=? AND ${AlarmEventContract.extra_alarm_repeat}=? AND ${AlarmEventContract.extra_alarm_repeat_days}=? AND latitude=? AND longitude=? AND altitude=?"
        val default_location = arrayOf(day_start.toString(), "0", "false", "[]", "30.0", "31.0", "0.0")
        val shifted_location = arrayOf(day_start.toString(), "0", "false", "[]", "55.0", "37.0", "100.0")

        var t_default: Long? = null
        query_event_calc("PRAYER_EID_FITR_START", selection, default_location).use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            t_default = cursor.getLong(cursor.getColumnIndexOrThrow(AlarmEventContract.column_event_timemillis))
        }

        var t_shifted: Long? = null
        query_event_calc("PRAYER_EID_FITR_START", selection, shifted_location).use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            t_shifted = cursor.getLong(cursor.getColumnIndexOrThrow(AlarmEventContract.column_event_timemillis))
        }

        assertEquals(day_start + 6 * 60 * 60 * 1000L + 15 * 60 * 1000L, t_default)
        assertEquals(day_start + 6 * 60 * 60 * 1000L + 45 * 60 * 1000L, t_shifted)
    }

    @Test
    fun night_event_calc_differs_between_two_location_overrides() {
        val day_start = utc_day_start(2026, Calendar.MARCH, 12)
        val selection = "${AlarmEventContract.extra_alarm_now}=? AND ${AlarmEventContract.extra_alarm_offset}=? AND ${AlarmEventContract.extra_alarm_repeat}=? AND ${AlarmEventContract.extra_alarm_repeat_days}=? AND latitude=? AND longitude=? AND altitude=?"
        val default_location = arrayOf(day_start.toString(), "0", "false", "[]", "30.0", "31.0", "0.0")
        val shifted_location = arrayOf(day_start.toString(), "0", "false", "[]", "55.0", "37.0", "100.0")

        var t_default: Long? = null
        query_event_calc("NIGHT_MIDPOINT", selection, default_location).use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            t_default = cursor.getLong(cursor.getColumnIndexOrThrow(AlarmEventContract.column_event_timemillis))
        }

        var t_shifted: Long? = null
        query_event_calc("NIGHT_MIDPOINT", selection, shifted_location).use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            t_shifted = cursor.getLong(cursor.getColumnIndexOrThrow(AlarmEventContract.column_event_timemillis))
        }

        assertEquals(day_start - 30 * 60 * 1000L, t_default)
        assertEquals(day_start - 15 * 60 * 1000L, t_shifted)
    }

    @Test
    fun night_event_calc_uses_saved_location_timezone_for_day_boundary() {
        val selected_tz = TimeZone.getTimeZone("Pacific/Kiritimati")
        val alarm_now = find_eid_mismatch_alarm_now(selected_tz)
        val selection =
            "${AlarmEventContract.extra_alarm_now}=? AND ${AlarmEventContract.extra_alarm_offset}=? AND ${AlarmEventContract.extra_alarm_repeat}=? AND ${AlarmEventContract.extra_alarm_repeat_days}=? AND latitude=? AND longitude=? AND altitude=?"
        val selection_args = arrayOf(alarm_now.toString(), "0", "false", "[]", "55.0", "37.0", "100.0")

        var without_saved_timezone: Long? = null
        query_event_calc("NIGHT_MIDPOINT", selection, selection_args).use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            without_saved_timezone = cursor.getLong(cursor.getColumnIndexOrThrow(AlarmEventContract.column_event_timemillis))
        }

        SavedLocations.save(
            context,
            listOf(
                SavedLocation(
                    id = "loc-kiritimati",
                    label = "Kiritimati",
                    latitude = "55.0",
                    longitude = "37.0",
                    altitude = "100.0",
                    timezone_id = selected_tz.id
                )
            )
        )

        var with_saved_timezone: Long? = null
        query_event_calc("NIGHT_MIDPOINT", selection, selection_args).use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            with_saved_timezone = cursor.getLong(cursor.getColumnIndexOrThrow(AlarmEventContract.column_event_timemillis))
        }

        assertNotEquals(without_saved_timezone, with_saved_timezone)
    }

    @Test
    fun night_event_calc_supports_saved_location_dst_timezone() {
        val alarm_now = utc_day_start(2026, Calendar.MARCH, 8)
        SavedLocations.save(
            context,
            listOf(
                SavedLocation(
                    id = "loc-ny",
                    label = "New York",
                    latitude = "30.0",
                    longitude = "31.0",
                    altitude = null,
                    timezone_id = "America/New_York"
                )
            )
        )

        query_event_calc(
            event_id = "NIGHT_MIDPOINT",
            selection = null,
            selection_args = arrayOf(alarm_now.toString(), "0", "false", "[]"),
            saved_location_id = "loc-ny"
        ).use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            cursor.getLong(cursor.getColumnIndexOrThrow(AlarmEventContract.column_event_timemillis))
        }
    }

    private fun query_event_calc(event_id: String, day_start: Long): Cursor {
        return query_event_calc(event_id, null, arrayOf(day_start.toString(), "0", "false", "[]"))
    }

    private fun query_event_calc(
        event_id: String,
        selection: String?,
        selection_args: Array<String>,
        saved_location_id: String? = null
    ): Cursor {
        val uri_builder = Uri.parse("content://${PrayerTimesProvider.authority}/${AlarmEventContract.query_event_calc}/$event_id").buildUpon()
        saved_location_id?.let { uri_builder.appendQueryParameter(AlarmEventContract.extra_saved_location_id, it) }
        val uri = uri_builder.build()
        return context.contentResolver.query(uri, null, selection, selection_args, null)!!
    }

    private fun event_info_names(): List<String> {
        query_event_info().use { cursor ->
            val names = ArrayList<String>()
            while (cursor.moveToNext()) names.add(cursor.getString(cursor.getColumnIndexOrThrow(AlarmEventContract.column_event_name)))
            return names
        }
    }

    private fun query_event_info(saved_location_id: String? = null): Cursor {
        val builder = Uri.parse("content://${PrayerTimesProvider.authority}/${AlarmEventContract.query_event_info}").buildUpon()
        saved_location_id?.let { builder.appendQueryParameter(AlarmEventContract.extra_saved_location_id, it) }
        return context.contentResolver.query(
            builder.build(),
            arrayOf(AlarmEventContract.column_event_name, AlarmEventContract.column_event_title),
            null,
            null,
            null
        )!!
    }

    private fun find_eid_day_start(): Long {
        val tz = TimeZone.getTimeZone("UTC")
        val from = utc_day_start(2026, Calendar.JANUARY, 1)
        repeat(730) { idx ->
            val day_start = from + idx * 24L * 60L * 60L * 1000L
            val hijri = hijri_for_day(day_start, tz, Locale.getDefault(), Prefs.get_hijri_variant(context), Prefs.get_hijri_day_offset(context))
            if (hijri.month == 10 && hijri.day == 1) {
                return day_start
            }
        }
        throw AssertionError("No Eid day found in search range")
    }

    private fun find_eid_adha_day_start(): Long {
        val tz = TimeZone.getTimeZone("UTC")
        val from = utc_day_start(2026, Calendar.JANUARY, 1)
        repeat(730) { idx ->
            val day_start = from + idx * 24L * 60L * 60L * 1000L
            val hijri = hijri_for_day(day_start, tz, Locale.getDefault(), Prefs.get_hijri_variant(context), Prefs.get_hijri_day_offset(context))
            if (hijri.month == 12 && hijri.day == 10) return day_start
        }
        throw AssertionError("No Eid al-Adha day found in search range")
    }

    private fun find_next_eid_day_start(from_day_start: Long): Long {
        val tz = TimeZone.getTimeZone("UTC")
        val from = from_day_start + 24L * 60L * 60L * 1000L
        repeat(730) { idx ->
            val day_start = from + idx * 24L * 60L * 60L * 1000L
            val hijri = hijri_for_day(day_start, tz, Locale.getDefault(), Prefs.get_hijri_variant(context), Prefs.get_hijri_day_offset(context))
            if (hijri.month == 10 && hijri.day == 1) {
                return day_start
            }
        }
        throw AssertionError("No next Eid al-Fitr day found in search range")
    }

    private fun find_eid_mismatch_alarm_now(selected_tz: TimeZone): Long {
        val utc = TimeZone.getTimeZone("UTC")
        val from = utc_day_start(2026, Calendar.JANUARY, 1)
        repeat(24 * 730) { hour ->
            val alarm_now = from + hour * 60L * 60L * 1000L
            if (is_eid_day_for(alarm_now, selected_tz) && !is_eid_day_for(alarm_now, utc)) {
                return alarm_now
            }
        }
        throw AssertionError("No timezone mismatch Eid timestamp found")
    }

    private fun is_eid_day_for(alarm_now: Long, tz: TimeZone): Boolean {
        val day_start =
            Calendar.getInstance(tz).run {
                timeInMillis = alarm_now
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                timeInMillis
            }
        val hijri = hijri_for_day(day_start, tz, Locale.getDefault(), Prefs.get_hijri_variant(context), Prefs.get_hijri_day_offset(context))
        return (hijri.month == 10 && hijri.day == 1) || (hijri.month == 12 && hijri.day == 10)
    }

    private fun utc_day_start(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).run {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            timeInMillis
        }
}
