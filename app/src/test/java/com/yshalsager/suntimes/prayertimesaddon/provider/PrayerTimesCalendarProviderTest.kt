package com.yshalsager.suntimes.prayertimesaddon.provider

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.yshalsager.suntimes.prayertimesaddon.DeniedHostCalcProvider
import com.yshalsager.suntimes.prayertimesaddon.FakeHostCalcProvider
import com.yshalsager.suntimes.prayertimesaddon.FakeHostEventProvider
import com.yshalsager.suntimes.prayertimesaddon.R
import com.yshalsager.suntimes.prayertimesaddon.day_millis
import com.yshalsager.suntimes.prayertimesaddon.denied_host_calc_authority
import com.yshalsager.suntimes.prayertimesaddon.denied_host_event_authority
import com.yshalsager.suntimes.prayertimesaddon.host_calc_authority
import com.yshalsager.suntimes.prayertimesaddon.host_event_authority
import com.yshalsager.suntimes.prayertimesaddon.core.HostConfigReader
import com.yshalsager.suntimes.prayertimesaddon.core.Prefs
import com.yshalsager.suntimes.prayertimesaddon.core.PrayerTimesCalendarContract
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
import org.robolectric.shadows.ShadowPackageManager
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PrayerTimesCalendarProviderTest {
    private lateinit var context: Context

    @Before
    fun set_up() {
        context = RuntimeEnvironment.getApplication()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

        context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE).edit().clear().apply()
        HostConfigReader.clear_cache()
        Prefs.set_asr_factor(context, 1)

        Robolectric.setupContentProvider(PrayerTimesCalendarProvider::class.java, PrayerTimesCalendarProvider.authority)
        Robolectric.setupContentProvider(FakeHostEventProvider::class.java, host_event_authority)
        Robolectric.setupContentProvider(FakeHostCalcProvider::class.java, host_calc_authority)
        Robolectric.setupContentProvider(DeniedHostCalcProvider::class.java, denied_host_calc_authority)

        val shadow_package_manager = shadowOf(context.packageManager)
        register_provider(shadow_package_manager, host_event_authority, FakeHostEventProvider::class.java.name)
        register_provider(shadow_package_manager, host_calc_authority, FakeHostCalcProvider::class.java.name)
        register_provider(shadow_package_manager, denied_host_event_authority, FakeHostEventProvider::class.java.name)
        register_provider(shadow_package_manager, denied_host_calc_authority, DeniedHostCalcProvider::class.java.name)
    }

    @Test
    fun discovery_alias_exposes_calendar_references() {
        val resolve_infos =
            context.packageManager.queryIntentActivities(
                Intent(PrayerTimesCalendarContract.action_add_calendar).addCategory(PrayerTimesCalendarContract.category_suntimes_calendar),
                PackageManager.GET_META_DATA
            )

        val match = resolve_infos.first { it.activityInfo.packageName == context.packageName }
        assertEquals(PrayerTimesCalendarContract.discovery_references, match.activityInfo.metaData.getString(PrayerTimesCalendarContract.metadata_reference))

        val package_info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        assertTrue(package_info.requestedPermissions?.contains("suntimes.permission.READ_CALCULATOR") == true)
    }

    @Test
    fun calendar_info_returns_row_for_valid_host() {
        Prefs.set_host_event_authority(context, host_event_authority)

        val cursor = query("content://${PrayerTimesCalendarProvider.authority}/prayers/calendarInfo")

        assertEquals(1, cursor.count)
        cursor.moveToFirst()
        assertEquals("prayers", cursor.getString(cursor.getColumnIndexOrThrow(PrayerTimesCalendarContract.column_calendar_name)))
        assertEquals(context.getString(R.string.calendar_prayers_title), cursor.getString(cursor.getColumnIndexOrThrow(PrayerTimesCalendarContract.column_calendar_title)))
        assertTrue(cursor.getString(cursor.getColumnIndexOrThrow(PrayerTimesCalendarContract.column_calendar_summary)).contains("Test Location"))
        assertEquals(ContextCompat.getColor(context, R.color.calendar_prayers), cursor.getInt(cursor.getColumnIndexOrThrow(PrayerTimesCalendarContract.column_calendar_color)))
    }

    @Test
    fun prayers_calendar_returns_point_events() {
        Prefs.set_host_event_authority(context, host_event_authority)
        val day_start = utc_day_start(2026, Calendar.MARCH, 12)

        val cursor = query("content://${PrayerTimesCalendarProvider.authority}/prayers/calendarContent/$day_start-${day_start + day_millis}")

        assertEquals(6, cursor.count)
        while (cursor.moveToNext()) {
            val start = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Events.DTSTART))
            val end = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Events.DTEND))
            assertEquals(start, end)
        }
    }

    @Test
    fun prayers_calendar_uses_jummah_title_on_friday() {
        Prefs.set_host_event_authority(context, host_event_authority)
        val friday_start = utc_day_start(2026, Calendar.MARCH, 13)

        val titles = query("content://${PrayerTimesCalendarProvider.authority}/prayers/calendarContent/$friday_start-${friday_start + day_millis}")
            .read_strings(CalendarContract.Events.TITLE)

        assertTrue(titles.contains(context.getString(R.string.event_prayer_jummah)))
        assertFalse(titles.contains(context.getString(R.string.event_prayer_dhuhr)))
    }

    @Test
    fun prayers_calendar_includes_eid_events_on_eid_day() {
        Prefs.set_host_event_authority(context, host_event_authority)
        val day_start = find_eid_day_start()

        val titles = query("content://${PrayerTimesCalendarProvider.authority}/prayers/calendarContent/$day_start-${day_start + day_millis}")
            .read_strings(CalendarContract.Events.TITLE)

        assertTrue(titles.contains(context.getString(R.string.event_prayer_eid_start)))
        assertTrue(titles.contains(context.getString(R.string.event_prayer_eid_end)))
    }

    @Test
    fun prayers_calendar_excludes_extra_fajr_and_isha_when_disabled() {
        Prefs.set_host_event_authority(context, host_event_authority)
        val day_start = utc_day_start(2026, Calendar.MARCH, 12)

        val titles = query("content://${PrayerTimesCalendarProvider.authority}/prayers/calendarContent/$day_start-${day_start + day_millis}")
            .read_strings(CalendarContract.Events.TITLE)

        assertFalse(titles.contains(context.getString(R.string.event_prayer_fajr_extra_1)))
        assertFalse(titles.contains(context.getString(R.string.event_prayer_isha_extra_1)))
    }

    @Test
    fun prayers_calendar_includes_custom_labeled_extra_fajr_and_isha_when_enabled() {
        Prefs.set_host_event_authority(context, host_event_authority)
        Prefs.set_extra_fajr_1_enabled(context, true)
        Prefs.set_extra_fajr_1_label(context, "Fajr Secondary")
        Prefs.set_extra_isha_1_enabled(context, true)
        Prefs.set_extra_isha_1_label(context, "Isha Secondary")
        val day_start = utc_day_start(2026, Calendar.MARCH, 12)

        val titles = query("content://${PrayerTimesCalendarProvider.authority}/prayers/calendarContent/$day_start-${day_start + day_millis}")
            .read_strings(CalendarContract.Events.TITLE)

        assertTrue(titles.contains("Fajr Secondary"))
        assertTrue(titles.contains("Isha Secondary"))
    }

    @Test
    fun makruh_calendar_returns_ranges() {
        Prefs.set_host_event_authority(context, host_event_authority)
        val day_start = utc_day_start(2026, Calendar.MARCH, 12)

        val cursor = query("content://${PrayerTimesCalendarProvider.authority}/makruh/calendarContent/$day_start-${day_start + day_millis}")

        assertEquals(5, cursor.count)
        while (cursor.moveToNext()) {
            val start = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Events.DTSTART))
            val end = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Events.DTEND))
            assertTrue(end > start)
        }
    }

    @Test
    fun night_calendar_includes_previous_day_events_that_overlap_window() {
        Prefs.set_host_event_authority(context, host_event_authority)
        val day_start = utc_day_start(2026, Calendar.MARCH, 12)
        val window_start = day_start + day_millis
        val window_end = window_start + 2 * 60 * 60 * 1000L

        val titles = query("content://${PrayerTimesCalendarProvider.authority}/night/calendarContent/$window_start-$window_end")
            .read_strings(CalendarContract.Events.TITLE)

        assertEquals(listOf(context.getString(R.string.night_last_third)), titles)
    }

    @Test
    fun calendar_info_returns_fallback_row_when_host_missing_or_denied() {
        Prefs.set_host_event_authority(context, "")
        query("content://${PrayerTimesCalendarProvider.authority}/prayers/calendarInfo").use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals(context.getString(R.string.no_host_found), cursor.getString(cursor.getColumnIndexOrThrow(PrayerTimesCalendarContract.column_calendar_summary)))
        }
        assertEquals(0, query("content://${PrayerTimesCalendarProvider.authority}/prayers/calendarContent/1-2").count)

        Prefs.set_host_event_authority(context, denied_host_event_authority)
        HostConfigReader.clear_cache()
        query("content://${PrayerTimesCalendarProvider.authority}/prayers/calendarInfo").use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals(context.getString(R.string.no_host_found), cursor.getString(cursor.getColumnIndexOrThrow(PrayerTimesCalendarContract.column_calendar_summary)))
        }
        assertEquals(0, query("content://${PrayerTimesCalendarProvider.authority}/prayers/calendarContent/1-2").count)
    }

    @Test
    fun malformed_window_returns_empty_cursor() {
        Prefs.set_host_event_authority(context, host_event_authority)
        assertEquals(0, query("content://${PrayerTimesCalendarProvider.authority}/prayers/calendarContent/not-a-window").count)
    }

    private fun register_provider(
        shadow_package_manager: ShadowPackageManager,
        authority: String,
        provider_name: String
    ) {
        shadow_package_manager.addOrUpdateProvider(
            ProviderInfo().apply {
                this.authority = authority
                packageName = context.packageName
                name = provider_name
                applicationInfo = context.applicationInfo
            }
        )
    }

    private fun query(uri: String): Cursor = context.contentResolver.query(Uri.parse(uri), null, null, null, null)!!

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

    private fun find_eid_day_start(): Long {
        val tz = TimeZone.getTimeZone("UTC")
        val from = utc_day_start(2026, Calendar.JANUARY, 1)
        repeat(730) { idx ->
            val day_start = from + idx * day_millis
            val hijri = hijri_for_day(day_start, tz, Locale.getDefault(), Prefs.get_hijri_variant(context), Prefs.get_hijri_day_offset(context))
            if ((hijri.month == 10 && hijri.day == 1) || (hijri.month == 12 && hijri.day == 10)) return day_start
        }
        throw AssertionError("No Eid day found in search range")
    }

    private fun Cursor.read_strings(column: String): List<String> {
        moveToPosition(-1)
        val out = ArrayList<String>()
        while (moveToNext()) out.add(getString(getColumnIndexOrThrow(column)))
        return out
    }
}
