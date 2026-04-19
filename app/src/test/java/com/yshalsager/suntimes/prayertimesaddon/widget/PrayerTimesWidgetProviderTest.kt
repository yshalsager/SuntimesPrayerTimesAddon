package com.yshalsager.suntimes.prayertimesaddon.widget

import android.app.AlarmManager
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.pm.ProviderInfo
import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.yshalsager.suntimes.prayertimesaddon.R
import com.yshalsager.suntimes.prayertimesaddon.FakeHostCalcProvider
import com.yshalsager.suntimes.prayertimesaddon.FakeHostEventProvider
import com.yshalsager.suntimes.prayertimesaddon.day_millis
import com.yshalsager.suntimes.prayertimesaddon.host_calc_authority
import com.yshalsager.suntimes.prayertimesaddon.host_event_authority
import com.yshalsager.suntimes.prayertimesaddon.core.HostConfigReader
import com.yshalsager.suntimes.prayertimesaddon.core.Prefs
import com.yshalsager.suntimes.prayertimesaddon.core.SavedLocation
import com.yshalsager.suntimes.prayertimesaddon.core.SavedLocations
import com.yshalsager.suntimes.prayertimesaddon.provider.PrayerTimesProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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

private const val alarm_token_key = "alarm_token"
private const val alarm_token_pref_key = "widget_alarm_token"

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PrayerTimesWidgetProviderTest {
    private lateinit var context: Context
    private lateinit var app_widget_manager: AppWidgetManager
    private lateinit var original_timezone: TimeZone

    @Before
    fun set_up() {
        context = RuntimeEnvironment.getApplication()
        original_timezone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

        context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences("${context.packageName}_widget", Context.MODE_PRIVATE).edit().clear().apply()
        HostConfigReader.clear_cache()

        Prefs.set_asr_factor(context, 1)
        Prefs.set_widget_show_prohibited(context, true)
        Prefs.set_widget_show_night_portions(context, true)

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

        app_widget_manager = AppWidgetManager.getInstance(context)

        ShadowAlarmManager.reset()
    }

    @After
    fun tear_down() {
        TimeZone.setDefault(original_timezone)
    }

    private fun create_widget_and_provider(): Pair<Int, PrayerTimesWidgetProvider> {
        val shadow_widget_manager = shadowOf(app_widget_manager)
        val widget_id = shadow_widget_manager.createWidget(PrayerTimesWidgetProvider::class.java, R.layout.widget_prayer_times)
        app_widget_manager.updateAppWidgetOptions(
            widget_id,
            Bundle().apply {
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 140)
            }
        )
        val provider = shadow_widget_manager.getAppWidgetProviderFor(widget_id) as PrayerTimesWidgetProvider
        return widget_id to provider
    }

    private fun widget_view(widget_id: Int) = shadowOf(app_widget_manager).getViewFor(widget_id)

    private fun update_widget_with_host(provider: PrayerTimesWidgetProvider, widget_id: Int) {
        Prefs.set_host_event_authority(context, host_event_authority)
        provider.onUpdate(context, app_widget_manager, intArrayOf(widget_id))
    }

    private fun clear_selected_host() {
        Prefs.set_host_event_authority(context, "")
        HostConfigReader.clear_cache()
    }

    private fun alarm_token_from_prefs() =
        context.getSharedPreferences("${context.packageName}_widget", Context.MODE_PRIVATE).getString(alarm_token_pref_key, null)

    @Test
    fun on_update_without_host_shows_no_host_state() {
        val (widget_id) = create_widget_and_provider()
        val view = widget_view(widget_id)

        assertEquals(context.getString(R.string.no_host_found), view.findViewById<TextView>(R.id.widget_hijri).text.toString())
        assertEquals(View.GONE, view.findViewById<View>(R.id.widget_prohibited_row).visibility)
        assertEquals(View.GONE, view.findViewById<View>(R.id.widget_night_row).visibility)
    }

    @Test
    fun on_update_with_host_renders_widget_values() {
        val (widget_id, provider) = create_widget_and_provider()

        update_widget_with_host(provider, widget_id)

        val view = widget_view(widget_id)

        val summary = view.findViewById<TextView>(R.id.widget_summary).text.toString()
        val fajr = view.findViewById<TextView>(R.id.widget_prayer_fajr).text.toString()

        assertTrue("summary=$summary", summary.contains("Test Location"))
        assertNotEquals("--", fajr)
        assertEquals(View.VISIBLE, view.findViewById<View>(R.id.widget_prohibited_row).visibility)
        assertEquals(View.VISIBLE, view.findViewById<View>(R.id.widget_night_row).visibility)
    }

    @Test
    fun on_receive_ignores_alarm_without_token() {
        val (widget_id, provider) = create_widget_and_provider()

        update_widget_with_host(provider, widget_id)

        val before = widget_view(widget_id).findViewById<TextView>(R.id.widget_summary).text.toString()

        clear_selected_host()

        provider.onReceive(context, android.content.Intent(PrayerTimesWidgetProvider.action_alarm))

        val after = widget_view(widget_id).findViewById<TextView>(R.id.widget_summary).text.toString()
        assertEquals(before, after)
    }

    @Test
    fun on_receive_alarm_with_valid_token_updates_widget() {
        val (widget_id, provider) = create_widget_and_provider()

        update_widget_with_host(provider, widget_id)

        val valid_token = alarm_token_from_prefs()
        assertNotNull(valid_token)

        clear_selected_host()

        val alarm_intent =
            android.content.Intent(PrayerTimesWidgetProvider.action_alarm).apply {
                putExtra(alarm_token_key, valid_token)
            }
        provider.onReceive(context, alarm_intent)

        val hijri = widget_view(widget_id).findViewById<TextView>(R.id.widget_hijri).text.toString()
        assertEquals(context.getString(R.string.no_host_found), hijri)
    }

    @Test
    fun on_receive_alarm_with_wrong_token_is_ignored() {
        val (widget_id, provider) = create_widget_and_provider()

        update_widget_with_host(provider, widget_id)

        val before = widget_view(widget_id).findViewById<TextView>(R.id.widget_summary).text.toString()

        clear_selected_host()

        val alarm_intent =
            android.content.Intent(PrayerTimesWidgetProvider.action_alarm).apply {
                putExtra(alarm_token_key, "wrong-token")
            }
        provider.onReceive(context, alarm_intent)

        val after = widget_view(widget_id).findViewById<TextView>(R.id.widget_summary).text.toString()
        assertEquals(before, after)
    }

    @Test
    fun on_update_with_widget_saved_location_shows_saved_location_summary() {
        val (widget_id, provider) = create_widget_and_provider()
        SavedLocations.save(
            context,
            listOf(
                SavedLocation(
                    id = "loc-widget",
                    label = "Mecca",
                    latitude = "21.4209",
                    longitude = "39.82562",
                    altitude = null,
                    timezone_id = "Etc/GMT-3",
                    calc_mode = SavedLocations.calc_mode_custom,
                    method_preset = "uaq"
                )
            )
        )
        WidgetPrefs.set_saved_location_id(context, widget_id, "loc-widget")

        update_widget_with_host(provider, widget_id)

        val summary = widget_view(widget_id).findViewById<TextView>(R.id.widget_summary).text.toString()
        assertTrue("summary=$summary", summary.contains("Mecca"))
        assertTrue("summary=$summary", summary.contains("Umm al-Qura"))
    }

    @Test
    fun on_deleted_clears_widget_saved_location_binding() {
        val (widget_id, provider) = create_widget_and_provider()
        WidgetPrefs.set_saved_location_id(context, widget_id, "loc-widget")

        provider.onDeleted(context, intArrayOf(widget_id))

        assertEquals(null, WidgetPrefs.get_saved_location_id(context, widget_id))
    }

    @Test
    fun update_schedules_alarm_with_token() {
        val (widget_id, provider) = create_widget_and_provider()

        val now_before = System.currentTimeMillis()
        update_widget_with_host(provider, widget_id)

        val alarm_manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadow_alarm_manager = shadowOf(alarm_manager)
        val alarms = shadow_alarm_manager.scheduledAlarms

        assertEquals("alarms=${alarms.size}", 1, alarms.size)
        val scheduled = alarms.first()
        assertTrue(scheduled.triggerAtMs >= now_before + 60_000L)
        assertNotNull(alarm_token_from_prefs())
    }
}
