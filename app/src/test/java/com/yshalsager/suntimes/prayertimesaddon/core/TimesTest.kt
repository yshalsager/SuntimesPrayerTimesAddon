package com.yshalsager.suntimes.prayertimesaddon.core

import android.content.Context
import android.content.pm.ProviderInfo
import com.yshalsager.suntimes.prayertimesaddon.FallbackOnlyHostEventProvider
import com.yshalsager.suntimes.prayertimesaddon.FakeHostCalcProvider
import com.yshalsager.suntimes.prayertimesaddon.FakeHostEventProvider
import com.yshalsager.suntimes.prayertimesaddon.OffdaySunHostCalcProvider
import com.yshalsager.suntimes.prayertimesaddon.fallback_host_calc_authority
import com.yshalsager.suntimes.prayertimesaddon.fallback_host_event_authority
import com.yshalsager.suntimes.prayertimesaddon.host_calc_authority
import com.yshalsager.suntimes.prayertimesaddon.host_event_authority
import com.yshalsager.suntimes.prayertimesaddon.offday_host_calc_authority
import com.yshalsager.suntimes.prayertimesaddon.offday_host_event_authority
import com.yshalsager.suntimes.prayertimesaddon.provider.PrayerTimesProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
class TimesTest {
    private lateinit var context: Context

    @Before
    fun set_up() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE).edit().clear().apply()
        HostConfigReader.clear_cache()
        Prefs.set_asr_factor(context, 1)
        Prefs.set_host_event_authority(context, host_event_authority)

        Robolectric.setupContentProvider(PrayerTimesProvider::class.java, PrayerTimesProvider.authority)
        Robolectric.setupContentProvider(FakeHostEventProvider::class.java, host_event_authority)
        Robolectric.setupContentProvider(FakeHostCalcProvider::class.java, host_calc_authority)
        Robolectric.setupContentProvider(FallbackOnlyHostEventProvider::class.java, fallback_host_event_authority)
        Robolectric.setupContentProvider(FakeHostCalcProvider::class.java, fallback_host_calc_authority)
        Robolectric.setupContentProvider(FakeHostEventProvider::class.java, offday_host_event_authority)
        Robolectric.setupContentProvider(OffdaySunHostCalcProvider::class.java, offday_host_calc_authority)

        shadowOf(context.packageManager).addOrUpdateProvider(
            ProviderInfo().apply {
                authority = PrayerTimesProvider.authority
                packageName = context.packageName
                name = PrayerTimesProvider::class.java.name
                applicationInfo = context.applicationInfo
            }
        )
        shadowOf(context.packageManager).addOrUpdateProvider(
            ProviderInfo().apply {
                authority = host_event_authority
                packageName = context.packageName
                name = FakeHostEventProvider::class.java.name
                applicationInfo = context.applicationInfo
            }
        )
        shadowOf(context.packageManager).addOrUpdateProvider(
            ProviderInfo().apply {
                authority = host_calc_authority
                packageName = context.packageName
                name = FakeHostCalcProvider::class.java.name
                applicationInfo = context.applicationInfo
            }
        )
        shadowOf(context.packageManager).addOrUpdateProvider(
            ProviderInfo().apply {
                authority = fallback_host_event_authority
                packageName = context.packageName
                name = FallbackOnlyHostEventProvider::class.java.name
                applicationInfo = context.applicationInfo
            }
        )
        shadowOf(context.packageManager).addOrUpdateProvider(
            ProviderInfo().apply {
                authority = fallback_host_calc_authority
                packageName = context.packageName
                name = FakeHostCalcProvider::class.java.name
                applicationInfo = context.applicationInfo
            }
        )
        shadowOf(context.packageManager).addOrUpdateProvider(
            ProviderInfo().apply {
                authority = offday_host_event_authority
                packageName = context.packageName
                name = FakeHostEventProvider::class.java.name
                applicationInfo = context.applicationInfo
            }
        )
        shadowOf(context.packageManager).addOrUpdateProvider(
            ProviderInfo().apply {
                authority = offday_host_calc_authority
                packageName = context.packageName
                name = OffdaySunHostCalcProvider::class.java.name
                applicationInfo = context.applicationInfo
            }
        )
    }

    @Test
    fun query_host_addon_time_uses_host_shadow_ratio_event_for_asr() {
        val day_start = 0L

        val asr = query_host_addon_time(context, host_event_authority, AddonEvent.prayer_asr, day_start)

        assertEquals(15 * 60 * 60 * 1000L + 30 * 60 * 1000L, asr)
    }

    @Test
    fun query_host_addon_time_returns_eid_start_and_end_on_eid_day() {
        val day_start = find_eid_day_start()

        val eid_start = query_host_addon_time(context, host_event_authority, AddonEvent.prayer_eid_fitr_start, day_start)
        val eid_end = query_host_addon_time(context, host_event_authority, AddonEvent.prayer_eid_fitr_end, day_start)

        assertEquals(day_start + 6 * 60 * 60 * 1000L + 15 * 60 * 1000L, eid_start)
        assertEquals(day_start + 12 * 60 * 60 * 1000L, eid_end)
    }

    @Test
    fun query_host_addon_time_returns_null_for_eid_events_on_non_eid_day() {
        val day_start = utc_day_start(2026, Calendar.MARCH, 12)

        val eid_start = query_host_addon_time(context, host_event_authority, AddonEvent.prayer_eid_fitr_start, day_start)
        val eid_end = query_host_addon_time(context, host_event_authority, AddonEvent.prayer_eid_fitr_end, day_start)

        assertNull(eid_start)
        assertNull(eid_end)
    }

    @Test
    fun query_host_addon_time_returns_extra_fajr_and_isha_when_enabled() {
        val day_start = utc_day_start(2026, Calendar.MARCH, 12)
        Prefs.set_extra_fajr_1_enabled(context, true)
        Prefs.set_extra_isha_1_enabled(context, true)

        val fajr_extra = query_host_addon_time(context, host_event_authority, AddonEvent.prayer_fajr_extra_1, day_start)
        val isha_extra = query_host_addon_time(context, host_event_authority, AddonEvent.prayer_isha_extra_1, day_start)

        assertEquals(day_start + 5 * 60 * 60 * 1000L, fajr_extra)
        assertEquals(day_start + 19 * 60 * 60 * 1000L + 30 * 60 * 1000L, isha_extra)
    }

    @Test
    fun query_host_addon_time_returns_null_for_extra_fajr_and_isha_when_disabled() {
        val day_start = utc_day_start(2026, Calendar.MARCH, 12)

        val fajr_extra = query_host_addon_time(context, host_event_authority, AddonEvent.prayer_fajr_extra_1, day_start)
        val isha_extra = query_host_addon_time(context, host_event_authority, AddonEvent.prayer_isha_extra_1, day_start)

        assertNull(fajr_extra)
        assertNull(isha_extra)
    }

    @Test
    fun query_host_addon_time_uses_runtime_profile_override_for_extra_slots() {
        val day_start = utc_day_start(2026, Calendar.MARCH, 12)
        val runtime =
            AddonRuntimeProfile(
                hijri_variant = Prefs.hijri_variant_umalqura,
                hijri_day_offset = 0,
                extra_fajr_1_enabled = true,
                extra_fajr_1_angle = 20.0,
                extra_fajr_1_label_raw = "",
                extra_isha_1_enabled = true,
                extra_isha_1_angle = 20.0,
                extra_isha_1_label_raw = ""
            )

        val fajr_extra =
            query_host_addon_time(
                context,
                host_event_authority,
                AddonEvent.prayer_fajr_extra_1,
                day_start,
                addon_runtime_profile_override = runtime
            )
        val isha_extra =
            query_host_addon_time(
                context,
                host_event_authority,
                AddonEvent.prayer_isha_extra_1,
                day_start,
                addon_runtime_profile_override = runtime
            )

        assertEquals(day_start + 5 * 60 * 60 * 1000L, fajr_extra)
        assertEquals(day_start + 19 * 60 * 60 * 1000L + 30 * 60 * 1000L, isha_extra)
    }

    @Test
    fun query_host_addon_time_uses_runtime_profile_override_for_eid_detection() {
        val eid_day = find_eid_day_start()
        val day_before = eid_day - 24L * 60L * 60L * 1000L
        val runtime =
            AddonRuntimeProfile(
                hijri_variant = Prefs.hijri_variant_umalqura,
                hijri_day_offset = 1,
                extra_fajr_1_enabled = false,
                extra_fajr_1_angle = 18.0,
                extra_fajr_1_label_raw = "",
                extra_isha_1_enabled = false,
                extra_isha_1_angle = 18.0,
                extra_isha_1_label_raw = ""
            )

        val without_override = query_host_addon_time(context, host_event_authority, AddonEvent.prayer_eid_fitr_start, day_before)
        val with_override =
            query_host_addon_time(
                context,
                host_event_authority,
                AddonEvent.prayer_eid_fitr_start,
                day_before,
                addon_runtime_profile_override = runtime
            )

        assertNull(without_override)
        assertEquals(day_before + 6 * 60 * 60 * 1000L + 15 * 60 * 1000L, with_override)
    }

    @Test
    fun query_host_addon_time_uses_location_selection_for_sun_based_events() {
        val day_start = find_eid_day_start()
        val selection =
            "${AlarmEventContract.extra_alarm_now}=? AND ${AlarmEventContract.extra_alarm_offset}=? AND ${AlarmEventContract.extra_alarm_repeat}=? AND ${AlarmEventContract.extra_alarm_repeat_days}=? AND latitude=? AND longitude=? AND altitude=?"
        val selection_args = arrayOf(day_start.toString(), "0", "false", "[]", "55.0", "37.0", "100.0")

        val duha = query_host_addon_time(
            context,
            host_event_authority,
            AddonEvent.prayer_duha,
            day_start,
            selection,
            selection_args
        )
        val eid_start = query_host_addon_time(
            context,
            host_event_authority,
            AddonEvent.prayer_eid_fitr_start,
            day_start,
            selection,
            selection_args
        )
        val eid_end = query_host_addon_time(
            context,
            host_event_authority,
            AddonEvent.prayer_eid_fitr_end,
            day_start,
            selection,
            selection_args
        )

        assertEquals(day_start + 6 * 60 * 60 * 1000L + 45 * 60 * 1000L, duha)
        assertEquals(day_start + 6 * 60 * 60 * 1000L + 45 * 60 * 1000L, eid_start)
        assertEquals(day_start + 12 * 60 * 60 * 1000L + 30 * 60 * 1000L, eid_end)
    }

    @Test
    fun query_host_addon_time_uses_latitude_override_for_asr_fallback() {
        val day_start = utc_day_start(2026, Calendar.MARCH, 12)
        val selection =
            "${AlarmEventContract.extra_alarm_now}=? AND ${AlarmEventContract.extra_alarm_offset}=? AND ${AlarmEventContract.extra_alarm_repeat}=? AND ${AlarmEventContract.extra_alarm_repeat_days}=? AND latitude=? AND longitude=?"
        val selection_args = arrayOf(day_start.toString(), "0", "false", "[]", "10.0", "31.0")

        val without_override = query_host_addon_time(
            context,
            fallback_host_event_authority,
            AddonEvent.prayer_asr,
            day_start,
            selection,
            selection_args
        )
        val with_override = query_host_addon_time(
            context,
            fallback_host_event_authority,
            AddonEvent.prayer_asr,
            day_start,
            selection,
            selection_args,
            null,
            10.0
        )

        assertNull(without_override)
        assertEquals(day_start + 16 * 60 * 60 * 1000L, with_override)
    }

    @Test
    fun query_host_addon_time_ignores_offday_sunrise_for_duha_and_sunrise_end() {
        val day_start = utc_day_start(2026, Calendar.MARCH, 12)
        val selection =
            "${AlarmEventContract.extra_alarm_now}=? AND ${AlarmEventContract.extra_alarm_offset}=? AND ${AlarmEventContract.extra_alarm_repeat}=? AND ${AlarmEventContract.extra_alarm_repeat_days}=? AND latitude=? AND longitude=? AND altitude=?"
        val selection_args = arrayOf(day_start.toString(), "0", "false", "[]", "21.4", "39.8", "0")

        val duha = query_host_addon_time(
            context,
            offday_host_event_authority,
            AddonEvent.prayer_duha,
            day_start,
            selection,
            selection_args
        )
        val sunrise_end = query_host_addon_time(
            context,
            offday_host_event_authority,
            AddonEvent.makruh_sunrise_end,
            day_start,
            selection,
            selection_args
        )

        assertEquals(day_start + 6 * 60 * 60 * 1000L + 15 * 60 * 1000L, duha)
        assertEquals(day_start + 6 * 60 * 60 * 1000L + 15 * 60 * 1000L, sunrise_end)
    }

    @Test
    fun query_host_addon_time_ignores_offday_sunrise_and_noon_for_eid_events() {
        val day_start = find_eid_day_start()
        val selection =
            "${AlarmEventContract.extra_alarm_now}=? AND ${AlarmEventContract.extra_alarm_offset}=? AND ${AlarmEventContract.extra_alarm_repeat}=? AND ${AlarmEventContract.extra_alarm_repeat_days}=? AND latitude=? AND longitude=? AND altitude=?"
        val selection_args = arrayOf(day_start.toString(), "0", "false", "[]", "21.4", "39.8", "0")

        val eid_start = query_host_addon_time(
            context,
            offday_host_event_authority,
            AddonEvent.prayer_eid_fitr_start,
            day_start,
            selection,
            selection_args
        )
        val eid_end = query_host_addon_time(
            context,
            offday_host_event_authority,
            AddonEvent.prayer_eid_fitr_end,
            day_start,
            selection,
            selection_args
        )

        assertEquals(day_start + 6 * 60 * 60 * 1000L + 15 * 60 * 1000L, eid_start)
        assertEquals(day_start + 12 * 60 * 60 * 1000L, eid_end)
    }

    @Test
    fun query_addon_time_uses_location_selection_end_to_end() {
        val eid_day_start = find_eid_day_start()
        val selection =
            "${AlarmEventContract.extra_alarm_now}=? AND ${AlarmEventContract.extra_alarm_offset}=? AND ${AlarmEventContract.extra_alarm_repeat}=? AND ${AlarmEventContract.extra_alarm_repeat_days}=? AND latitude=? AND longitude=? AND altitude=?"
        val eid_selection_args = arrayOf(eid_day_start.toString(), "0", "false", "[]", "55.0", "37.0", "100.0")

        val eid_start = query_addon_time(context, AddonEvent.prayer_eid_fitr_start, eid_day_start, selection, eid_selection_args)
        val eid_end = query_addon_time(context, AddonEvent.prayer_eid_fitr_end, eid_day_start, selection, eid_selection_args)

        assertEquals(eid_day_start + 6 * 60 * 60 * 1000L + 45 * 60 * 1000L, eid_start)
        assertEquals(eid_day_start + 12 * 60 * 60 * 1000L + 30 * 60 * 1000L, eid_end)

        val day_start = utc_day_start(2026, Calendar.MARCH, 12)
        val night_selection_args = arrayOf(day_start.toString(), "0", "false", "[]", "55.0", "37.0", "100.0")
        val night_midpoint = query_addon_time(context, AddonEvent.night_midpoint, day_start, selection, night_selection_args)

        assertEquals(day_start - 15 * 60 * 1000L, night_midpoint)
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
        throw AssertionError("No Eid al-Fitr day found in search range")
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
