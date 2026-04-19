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
import com.yshalsager.suntimes.prayertimesaddon.host_calc_authority
import com.yshalsager.suntimes.prayertimesaddon.host_event_authority
import com.yshalsager.suntimes.prayertimesaddon.core.hijri_for_day
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

        query_event_calc("PRAYER_EID_START", day_start).use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals(
                day_start + 6 * 60 * 60 * 1000L + 15 * 60 * 1000L,
                cursor.getLong(cursor.getColumnIndexOrThrow(AlarmEventContract.column_event_timemillis))
            )
        }

        query_event_calc("PRAYER_EID_END", day_start).use { cursor ->
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

        query_event_calc("PRAYER_EID_START", selection, selection_args).use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals(
                day_start + 6 * 60 * 60 * 1000L + 45 * 60 * 1000L,
                cursor.getLong(cursor.getColumnIndexOrThrow(AlarmEventContract.column_event_timemillis))
            )
        }

        query_event_calc("PRAYER_EID_END", selection, selection_args).use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals(
                day_start + 12 * 60 * 60 * 1000L + 30 * 60 * 1000L,
                cursor.getLong(cursor.getColumnIndexOrThrow(AlarmEventContract.column_event_timemillis))
            )
        }
    }

    @Test
    fun eid_event_calc_is_empty_on_non_eid_day() {
        val day_start = utc_day_start(2026, Calendar.MARCH, 12)

        query_event_calc("PRAYER_EID_START", day_start).use { cursor -> assertEquals(0, cursor.count) }
        query_event_calc("PRAYER_EID_END", day_start).use { cursor -> assertEquals(0, cursor.count) }
    }

    @Test
    fun event_info_lists_eid_events() {
        val uri = Uri.parse("content://${PrayerTimesProvider.authority}/${AlarmEventContract.query_event_info}")
        context.contentResolver.query(uri, arrayOf(AlarmEventContract.column_event_name), null, null, null)!!.use { cursor ->
            val names = ArrayList<String>()
            while (cursor.moveToNext()) names.add(cursor.getString(cursor.getColumnIndexOrThrow(AlarmEventContract.column_event_name)))
            assertTrue(names.contains("PRAYER_EID_START"))
            assertTrue(names.contains("PRAYER_EID_END"))
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
        query_event_calc("PRAYER_EID_START", selection, default_location).use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            t_default = cursor.getLong(cursor.getColumnIndexOrThrow(AlarmEventContract.column_event_timemillis))
        }

        var t_shifted: Long? = null
        query_event_calc("PRAYER_EID_START", selection, shifted_location).use { cursor ->
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

    private fun query_event_calc(event_id: String, day_start: Long): Cursor {
        return query_event_calc(event_id, null, arrayOf(day_start.toString(), "0", "false", "[]"))
    }

    private fun query_event_calc(event_id: String, selection: String?, selection_args: Array<String>): Cursor {
        val uri = Uri.parse("content://${PrayerTimesProvider.authority}/${AlarmEventContract.query_event_calc}/$event_id")
        return context.contentResolver.query(uri, null, selection, selection_args, null)!!
    }

    private fun event_info_names(): List<String> {
        val uri = Uri.parse("content://${PrayerTimesProvider.authority}/${AlarmEventContract.query_event_info}")
        context.contentResolver.query(uri, arrayOf(AlarmEventContract.column_event_name), null, null, null)!!.use { cursor ->
            val names = ArrayList<String>()
            while (cursor.moveToNext()) names.add(cursor.getString(cursor.getColumnIndexOrThrow(AlarmEventContract.column_event_name)))
            return names
        }
    }

    private fun find_eid_day_start(): Long {
        val tz = TimeZone.getTimeZone("UTC")
        val from = utc_day_start(2026, Calendar.JANUARY, 1)
        repeat(730) { idx ->
            val day_start = from + idx * 24L * 60L * 60L * 1000L
            val hijri = hijri_for_day(day_start, tz, Locale.getDefault(), Prefs.get_hijri_variant(context), Prefs.get_hijri_day_offset(context))
            if ((hijri.month == 10 && hijri.day == 1) || (hijri.month == 12 && hijri.day == 10)) {
                return day_start
            }
        }
        throw AssertionError("No Eid day found in search range")
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
