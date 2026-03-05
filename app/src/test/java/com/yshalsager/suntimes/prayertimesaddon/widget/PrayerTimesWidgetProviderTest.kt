package com.yshalsager.suntimes.prayertimesaddon.widget

import android.app.AlarmManager
import android.appwidget.AppWidgetManager
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.yshalsager.suntimes.prayertimesaddon.R
import com.yshalsager.suntimes.prayertimesaddon.core.AlarmEventContract
import com.yshalsager.suntimes.prayertimesaddon.core.CalculatorConfigContract
import com.yshalsager.suntimes.prayertimesaddon.core.HostConfigReader
import com.yshalsager.suntimes.prayertimesaddon.core.Prefs
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

private const val host_event_authority = "com.test.host.event.provider"
private const val host_calc_authority = "com.test.host.calculator.provider"
private const val alarm_token_key = "alarm_token"
private const val alarm_token_pref_key = "widget_alarm_token"
private const val day_millis = 24 * 60 * 60 * 1000L

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

class FakeHostEventProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selection_args: Array<String>?,
        sort_order: String?
    ): Cursor? {
        val path = uri.pathSegments
        if (path.isEmpty()) return null
        return when (path[0]) {
            AlarmEventContract.query_event_info -> query_event_info(path.getOrNull(1), projection)
            AlarmEventContract.query_event_calc -> query_event_calc(path.getOrNull(1), projection, selection_args)
            else -> null
        }
    }

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selection_args: Array<String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selection_args: Array<String>?): Int = 0

    private fun query_event_info(event_id: String?, projection: Array<String>?): Cursor {
        val cols = projection ?: arrayOf(AlarmEventContract.column_event_name)
        val c = MatrixCursor(cols)
        if (event_id == null) return c
        if (time_for_event(event_id, System.currentTimeMillis()) != null) {
            val row = arrayOfNulls<Any>(cols.size)
            cols.indices.forEach { i ->
                row[i] = if (cols[i] == AlarmEventContract.column_event_name) event_id else null
            }
            c.addRow(row)
        }
        return c
    }

    private fun query_event_calc(event_id: String?, projection: Array<String>?, selection_args: Array<String>?): Cursor {
        val cols = projection ?: AlarmEventContract.query_event_calc_projection
        val c = MatrixCursor(cols)
        if (event_id == null) return c

        val alarm_now = selection_args?.getOrNull(0)?.toLongOrNull() ?: System.currentTimeMillis()
        val time = time_for_event(event_id, alarm_now) ?: return c

        val row = arrayOfNulls<Any>(cols.size)
        cols.indices.forEach { i ->
            row[i] =
                when (cols[i]) {
                    AlarmEventContract.column_event_name -> event_id
                    AlarmEventContract.column_event_timemillis -> time
                    else -> null
                }
        }
        c.addRow(row)
        return c
    }

    private fun time_for_event(event_id: String, alarm_now: Long): Long? {
        val day_start = alarm_now - Math.floorMod(alarm_now, day_millis)
        val offset =
            when {
                event_id == "SUNRISE" -> 6 * 60 * 60 * 1000L
                event_id == "NOON" -> 12 * 60 * 60 * 1000L
                event_id == "SUNSET" -> 18 * 60 * 60 * 1000L
                event_id == "SHADOWRATIO_X:1.0" -> 15 * 60 * 60 * 1000L + 30 * 60 * 1000L
                event_id == "SHADOWRATIO_X:2.0" -> 16 * 60 * 60 * 1000L + 30 * 60 * 1000L
                event_id == "SUN_5.0s" -> 17 * 60 * 60 * 1000L + 45 * 60 * 1000L
                event_id.startsWith("SUN_-") && event_id.endsWith("r") -> 5 * 60 * 60 * 1000L
                event_id.startsWith("SUN_-") && event_id.endsWith("s") -> 19 * 60 * 60 * 1000L + 30 * 60 * 1000L
                else -> null
            }
        return offset?.let { day_start + it }
    }
}

class FakeHostCalcProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selection_args: Array<String>?,
        sort_order: String?
    ): Cursor? {
        val path = uri.pathSegments
        if (path.isEmpty()) return null
        return when (path[0]) {
            CalculatorConfigContract.query_config -> query_config(projection)
            CalculatorConfigContract.query_sun -> query_sun(path.getOrNull(1), projection)
            CalculatorConfigContract.query_sunpos -> query_sunpos(projection)
            else -> null
        }
    }

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selection_args: Array<String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selection_args: Array<String>?): Int = 0

    private fun query_config(projection: Array<String>?): Cursor {
        val cols = projection ?: CalculatorConfigContract.projection_basic
        val c = MatrixCursor(cols)
        val row = arrayOfNulls<Any>(cols.size)
        cols.indices.forEach { i ->
            row[i] =
                when (cols[i]) {
                    CalculatorConfigContract.column_location -> "Test Location"
                    CalculatorConfigContract.column_latitude -> "30.0"
                    CalculatorConfigContract.column_longitude -> "31.0"
                    CalculatorConfigContract.column_timezone -> "UTC"
                    else -> null
                }
        }
        c.addRow(row)
        return c
    }

    private fun query_sun(at_millis_segment: String?, projection: Array<String>?): Cursor {
        val cols = projection ?: CalculatorConfigContract.projection_sun_basic
        val c = MatrixCursor(cols)
        val at_millis = at_millis_segment?.toLongOrNull() ?: System.currentTimeMillis()
        val day_start = at_millis - Math.floorMod(at_millis, day_millis)
        val row = arrayOfNulls<Any>(cols.size)
        cols.indices.forEach { i ->
            row[i] =
                when (cols[i]) {
                    CalculatorConfigContract.column_sun_noon -> day_start + 12 * 60 * 60 * 1000L
                    CalculatorConfigContract.column_sunrise -> day_start + 6 * 60 * 60 * 1000L
                    CalculatorConfigContract.column_sunset -> day_start + 18 * 60 * 60 * 1000L
                    else -> null
                }
        }
        c.addRow(row)
        return c
    }

    private fun query_sunpos(projection: Array<String>?): Cursor {
        val cols = projection ?: CalculatorConfigContract.projection_sunpos_dec
        val c = MatrixCursor(cols)
        val row = arrayOfNulls<Any>(cols.size)
        cols.indices.forEach { i ->
            row[i] = if (cols[i] == CalculatorConfigContract.column_sunpos_dec) 10.0 else null
        }
        c.addRow(row)
        return c
    }
}
