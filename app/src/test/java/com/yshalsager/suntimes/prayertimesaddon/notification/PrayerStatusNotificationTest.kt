package com.yshalsager.suntimes.prayertimesaddon.notification

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.pm.ProviderInfo
import com.yshalsager.suntimes.prayertimesaddon.FakeHostCalcProvider
import com.yshalsager.suntimes.prayertimesaddon.FakeHostEventProvider
import com.yshalsager.suntimes.prayertimesaddon.core.AddonEvent
import com.yshalsager.suntimes.prayertimesaddon.core.AppClock
import com.yshalsager.suntimes.prayertimesaddon.core.HostConfigReader
import com.yshalsager.suntimes.prayertimesaddon.core.Prefs
import com.yshalsager.suntimes.prayertimesaddon.day_millis
import com.yshalsager.suntimes.prayertimesaddon.host_calc_authority
import com.yshalsager.suntimes.prayertimesaddon.host_event_authority
import com.yshalsager.suntimes.prayertimesaddon.provider.PrayerTimesProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PrayerStatusNotificationTest {
    private lateinit var context: Context
    private lateinit var original_timezone: TimeZone

    @Before
    fun set_up() {
        context = RuntimeEnvironment.getApplication()
        original_timezone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

        context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE).edit().clear().apply()
        HostConfigReader.clear_cache()
        AppClock.set_fixed_now_millis(null)
        ShadowAlarmManager.reset()

        Prefs.set_host_event_authority(context, host_event_authority)
        Prefs.set_asr_factor(context, 1)
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

    @After
    fun tear_down() {
        TimeZone.setDefault(original_timezone)
        AppClock.set_fixed_now_millis(null)
    }

    @Test
    fun status_data_uses_next_obligatory_prayer_for_selected_location() {
        val day_start = System.currentTimeMillis().let { it - Math.floorMod(it, day_millis) }
        AppClock.set_fixed_now_millis(day_start + 13L * 60L * 60L * 1000L)

        val data = PrayerStatusNotification.build_status_data(context)

        assertNotNull(data)
        assertEquals(AddonEvent.prayer_asr, data!!.next_event)
        assertEquals(day_start + 15L * 60L * 60L * 1000L + 30L * 60L * 1000L, data.next_time_millis)
        assertEquals(data.next_time_millis + 30_000L, data.next_refresh_millis)
        assertEquals("Test Location", data.location_label)
        assertEquals(5, data.day_events.size)
    }

    @Test
    fun refresh_schedules_next_boundary_only() {
        val day_start = System.currentTimeMillis().let { it - Math.floorMod(it, day_millis) }
        AppClock.set_fixed_now_millis(day_start + 13L * 60L * 60L * 1000L)
        shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        PrayerStatusNotification.set_enabled(context, true)

        val alarm_manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarms = shadowOf(alarm_manager).scheduledAlarms
        assertEquals("alarms=${alarms.size}", 1, alarms.size)
        assertEquals(day_start + 15L * 60L * 60L * 1000L + 30L * 60L * 1000L + 30_000L, alarms.first().triggerAtMs)
    }
}
