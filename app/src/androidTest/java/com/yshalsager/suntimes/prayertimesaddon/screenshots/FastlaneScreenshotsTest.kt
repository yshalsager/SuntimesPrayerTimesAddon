package com.yshalsager.suntimes.prayertimesaddon.screenshots

import android.app.Activity
import android.app.LocaleManager
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.LocaleList
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.yshalsager.suntimes.prayertimesaddon.core.AddonEvent
import com.yshalsager.suntimes.prayertimesaddon.core.AddonRuntimeProfile
import com.yshalsager.suntimes.prayertimesaddon.core.AppClock
import com.yshalsager.suntimes.prayertimesaddon.core.HostResolver
import com.yshalsager.suntimes.prayertimesaddon.core.Prefs
import com.yshalsager.suntimes.prayertimesaddon.core.SavedLocation
import com.yshalsager.suntimes.prayertimesaddon.core.SavedLocations
import com.yshalsager.suntimes.prayertimesaddon.core.day_start_at
import com.yshalsager.suntimes.prayertimesaddon.core.query_host_addon_time
import com.yshalsager.suntimes.prayertimesaddon.core.query_host_sun
import com.yshalsager.suntimes.prayertimesaddon.ui.DaysActivity
import com.yshalsager.suntimes.prayertimesaddon.ui.MainActivity
import com.yshalsager.suntimes.prayertimesaddon.ui.SettingsActivity
import com.yshalsager.suntimes.prayertimesaddon.widget.PrayerTimesWidgetConfigureActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.cleanstatusbar.CleanStatusBar
import tools.fastlane.screengrab.cleanstatusbar.IconVisibility
import tools.fastlane.screengrab.locale.LocaleTestRule
import java.util.Calendar
import java.util.TimeZone

@RunWith(AndroidJUnit4::class)
class FastlaneScreenshotsTest {
    private val screenshot_location = SavedLocation(
        id = "screenshots-cairo",
        label = "Cairo",
        latitude = "30.0444",
        longitude = "31.2357",
        altitude = "23",
        timezone_id = "Africa/Cairo",
        calc_mode = SavedLocations.calc_mode_custom
    )

    @get:org.junit.Rule
    val locale_rule = LocaleTestRule()

    @Test
    fun capture_core_screens() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val locale_tag = InstrumentationRegistry.getArguments().getString("testLocale") ?: "en-US"
        AppClock.set_fixed_now_millis(fixed_now_millis())
        instrumentation.uiAutomation.executeShellCommand("pm grant ${instrumentation.targetContext.packageName} android.permission.DUMP").close()
        instrumentation.uiAutomation.executeShellCommand("settings put global sysui_demo_allowed 1").close()
        try {
            prepare_fixture()
            apply_locale(locale_tag)
            assert_valid_prayer_ranges()
            capture(MainActivity::class.java, "01_home", 4200L)
            capture(DaysActivity::class.java, "02_calendar", 1800L)
            capture(SettingsActivity::class.java, "03_settings", 1000L)
            capture(
                Intent(InstrumentationRegistry.getInstrumentation().targetContext, PrayerTimesWidgetConfigureActivity::class.java)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 42),
                "04_widget_configuration",
                1000L
            )
        } finally {
            CleanStatusBar.disable()
            AppClock.set_fixed_now_millis(null)
        }
    }

    private fun fixed_now_millis(): Long =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).run {
            set(2026, Calendar.FEBRUARY, 10, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
            timeInMillis
        }

    private fun prepare_fixture() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val host = checkNotNull(HostResolver.ensure_default_selected(context)) {
            "SuntimesWidget must be installed before capturing screenshots"
        }
        HostResolver.get_required_permission(context, host)?.let {
            instrumentation.uiAutomation.grantRuntimePermission(context.packageName, it)
        }
        SavedLocations.save(context, listOf(screenshot_location))
        Prefs.set_home_location_source(context, SavedLocations.home_source_saved)
        Prefs.set_home_location_id(context, screenshot_location.id)
        Prefs.set_days_show_prohibited(context, true)
        Prefs.set_days_show_night_portions(context, true)
        Prefs.set_days_show_hijri(context, true)
    }

    private fun assert_valid_prayer_ranges() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val host = HostResolver.ensure_default_selected(context)!!
        val timezone = TimeZone.getTimeZone(screenshot_location.timezone_id)
        val day_start = day_start_at(fixed_now_millis(), timezone)
        val selection = SavedLocations.build_selection(day_start, screenshot_location)
        val method = screenshot_location.method_config()
        val runtime = AddonRuntimeProfile.defaults()
        fun time(event: AddonEvent): Long? = query_host_addon_time(
            context,
            host,
            event,
            day_start,
            selection.first,
            selection.second,
            timezone,
            screenshot_location.latitude.toDouble(),
            method,
            runtime
        )

        val sun = query_host_sun(context, host, day_start, selection.first, selection.second)
        val fajr = time(AddonEvent.prayer_fajr)
        val dhuhr = sun?.noon ?: time(AddonEvent.prayer_dhuhr)
        val asr = time(AddonEvent.prayer_asr)
        val maghrib = sun?.sunset ?: time(AddonEvent.prayer_maghrib)
        val isha = time(AddonEvent.prayer_isha)
        val prayers = listOf("Fajr" to fajr, "Dhuhr" to dhuhr, "Asr" to asr, "Maghrib" to maghrib, "Isha" to isha)
        val prayer_times = prayers.map { (label, value) ->
            checkNotNull(value) { "$label must be available before screenshot capture" }
        }
        assertTrue("obligatory prayers must be ordered", prayer_times.zipWithNext().all { (start, end) -> end > start })
        assertTrue("obligatory prayers must span less than one day", prayer_times.last() - prayer_times.first() < 24L * 60L * 60L * 1000L)
        prayers.forEach { (label, value) ->
            assertEquals("$label must be on the captured day", day_start, day_start_at(value!!, timezone))
        }

        val sunrise = sun?.sunrise ?: time(AddonEvent.makruh_sunrise_start)
        val sunrise_end = time(AddonEvent.makruh_sunrise_end)
        val zawal_start = sun?.noon?.minus(method.zawal_minutes * 60_000L) ?: time(AddonEvent.makruh_zawal_start)
        val sunset_start = time(AddonEvent.makruh_sunset_start)
        val sunset = sun?.sunset ?: time(AddonEvent.makruh_sunset_end)
        listOf(
            "dawn" to (fajr to sunrise),
            "sunrise" to (sunrise to sunrise_end),
            "zawal" to (zawal_start to dhuhr),
            "after Asr" to (asr to sunset_start),
            "sunset" to (sunset_start to sunset)
        ).forEach { (label, range) ->
            val start = checkNotNull(range.first) { "$label start must be available before screenshot capture" }
            val end = checkNotNull(range.second) { "$label end must be available before screenshot capture" }
            assertTrue("$label must end after it starts ($start..$end)", end > start)
            assertTrue("$label must be shorter than one day (${(end - start) / 60_000L} minutes; $start..$end)", end - start < 24L * 60L * 60L * 1000L)
            assertEquals("$label start must be on the captured day", day_start, day_start_at(start, timezone))
            assertEquals("$label end must be on the captured day", day_start, day_start_at(end, timezone))
        }
    }

    private fun apply_locale(locale_tag: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val lang = if (locale_tag.startsWith("ar")) "ar" else "en"
        val intent = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ActivityScenario.launch<Activity>(intent).use {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                context.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags(lang)
            }
            SystemClock.sleep(400)
        }
    }

    private fun capture(activity_class: Class<out Activity>, name: String, wait_millis: Long) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        capture(Intent(context, activity_class), name, wait_millis)
    }

    private fun capture(intent: Intent, name: String, wait_millis: Long) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ActivityScenario.launch<Activity>(intent).use {
            SystemClock.sleep(wait_millis)
            clean_status_bar()
            Screengrab.screenshot(name)
        }
    }

    private fun clean_status_bar() {
        CleanStatusBar()
            .setClock("1200")
            .setWifiVisibility(IconVisibility.HIDE)
            .setMobileNetworkVisibility(IconVisibility.HIDE)
            .enable()
        SystemClock.sleep(100)
    }
}
