package com.yshalsager.suntimes.prayertimesaddon.screenshots

import android.app.Activity
import android.content.Intent
import android.os.SystemClock
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.yshalsager.suntimes.prayertimesaddon.core.AppClock
import com.yshalsager.suntimes.prayertimesaddon.ui.DaysActivity
import com.yshalsager.suntimes.prayertimesaddon.ui.MainActivity
import com.yshalsager.suntimes.prayertimesaddon.ui.SettingsActivity
import org.junit.Test
import org.junit.runner.RunWith
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.locale.LocaleTestRule
import java.util.Calendar
import java.util.TimeZone

@RunWith(AndroidJUnit4::class)
class FastlaneScreenshotsTest {
    @get:org.junit.Rule
    val locale_rule = LocaleTestRule()

    @Test
    fun capture_core_screens() {
        val locale_tag = InstrumentationRegistry.getArguments().getString("testLocale") ?: "en-US"
        AppClock.set_fixed_now_millis(fixed_now_millis())
        try {
            apply_locale(locale_tag)
            capture(MainActivity::class.java, "01_home", 4200L)
            capture(DaysActivity::class.java, "02_calendar", 1800L)
            capture(SettingsActivity::class.java, "03_settings", 1000L)
        } finally {
            AppClock.set_fixed_now_millis(null)
        }
    }

    private fun fixed_now_millis(): Long =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).run {
            set(2026, Calendar.FEBRUARY, 10, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
            timeInMillis
        }

    private fun apply_locale(locale_tag: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val lang = if (locale_tag.startsWith("ar")) "ar" else "en"
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang))
        }
        val intent = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ActivityScenario.launch<Activity>(intent).use { SystemClock.sleep(400) }
    }

    private fun capture(activity_class: Class<out Activity>, name: String, wait_millis: Long) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(context, activity_class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ActivityScenario.launch<Activity>(intent).use {
            SystemClock.sleep(wait_millis)
            Screengrab.screenshot(name)
        }
    }
}
