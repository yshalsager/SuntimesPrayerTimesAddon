package com.yshalsager.suntimes.prayertimesaddon.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.format.DateFormat
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.yshalsager.suntimes.prayertimesaddon.R
import com.yshalsager.suntimes.prayertimesaddon.core.AddonEvent
import com.yshalsager.suntimes.prayertimesaddon.core.HostConfigReader
import com.yshalsager.suntimes.prayertimesaddon.core.HostResolver
import com.yshalsager.suntimes.prayertimesaddon.core.Prefs
import com.yshalsager.suntimes.prayertimesaddon.core.calc_night
import com.yshalsager.suntimes.prayertimesaddon.core.query_host_addon_time
import com.yshalsager.suntimes.prayertimesaddon.core.query_host_sun
import com.yshalsager.suntimes.prayertimesaddon.ui.compose.HomeItemUi
import com.yshalsager.suntimes.prayertimesaddon.ui.compose.HomeScreen
import com.yshalsager.suntimes.prayertimesaddon.ui.compose.HomeUiState
import com.yshalsager.suntimes.prayertimesaddon.ui.compose.NextPrayerUi
import com.yshalsager.suntimes.prayertimesaddon.ui.compose.PrayerTimesTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import com.yshalsager.suntimes.prayertimesaddon.core.format_method_summary
import android.os.Handler
import android.os.Looper

class MainActivity : ThemedActivity() {
    private val request_code_permissions = 1001
    private val ui = Handler(Looper.getMainLooper())
    private var tick: Runnable? = null

    private var next_time_millis: Long? = null
    private var prev_time_millis: Long? = null
    private var next_boundary_millis: Long? = null
    private var today_start: Long? = null
    private var tz: TimeZone? = null
    private var time_format: java.text.DateFormat? = null
    private var day_format: SimpleDateFormat? = null
    private var loc: String? = null

    private var state by mutableStateOf(
        HomeUiState(
            method_summary = "--",
            location_summary = "--",
            host_footer = "--",
            error = null,
            next_prayer = null,
            items = emptyList()
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PrayerTimesTheme {
                HomeScreen(
                    state = state,
                    on_open_days = { startActivity(Intent(this@MainActivity, DaysActivity::class.java)) },
                    on_open_settings = { startActivity(Intent(this@MainActivity, SettingsActivity::class.java)) }
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        tick?.let(ui::removeCallbacks)
        tick = null
    }

    override fun onResume() {
        super.onResume()
        refresh_home()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == request_code_permissions) refresh_home()
    }

    private fun refresh_home() {
        tick?.let(ui::removeCallbacks)
        tick = null

        val host = HostResolver.ensure_default_selected(this)
        if (host == null) {
            state = state.copy(error = getString(R.string.no_host_found))
            return
        }

        val required_perm = HostResolver.get_required_permission(this, host)
        if (required_perm != null && ContextCompat.checkSelfPermission(this, required_perm) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(required_perm), request_code_permissions)
            state = state.copy(error = getString(R.string.missing_permission, required_perm))
            return
        }

        Thread {
            val computed = compute_home(host)
            ui.post {
                apply_computed(computed)
                start_tick(host)
            }
        }.start()
    }

    private data class Computed(
        val state: HomeUiState,
        val next_time_millis: Long?,
        val prev_time_millis: Long?,
        val next_boundary_millis: Long?,
        val today_start: Long,
        val tz: TimeZone,
        val time_format: java.text.DateFormat,
        val day_format: SimpleDateFormat,
        val loc: String
    )

    private fun compute_home(host: String): Computed {
        val host_config = HostConfigReader.read_config(this, host)
        val tz = host_config?.timezone?.let(TimeZone::getTimeZone) ?: TimeZone.getDefault()

        val today_start = Calendar.getInstance(tz).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val yesterday_start = Calendar.getInstance(tz).apply {
            timeInMillis = today_start
            add(Calendar.DAY_OF_YEAR, -1)
        }.timeInMillis
        val tomorrow_start = Calendar.getInstance(tz).apply {
            timeInMillis = today_start
            add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis

        val time_format = DateFormat.getTimeFormat(this).apply { timeZone = tz }
        val day_format = SimpleDateFormat("EEE", Locale.getDefault()).apply { timeZone = tz }
        val loc = host_config?.display_label() ?: "--"

        fun same_day_start(millis: Long): Long =
            Calendar.getInstance(tz).run {
                timeInMillis = millis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                timeInMillis
            }

        fun day_time(v: Long?): String = v?.let { "${day_format.format(Date(it))} ${time_format.format(Date(it))}" } ?: "--"
        fun time_only(v: Long?): String = v?.let { time_format.format(Date(it)) } ?: "--"
        fun timeline_time(v: Long?): String {
            if (v == null) return "--"
            return if (same_day_start(v) == today_start) time_only(v) else day_time(v)
        }

        fun window_range_str(start: Long?, end: Long?): String {
            if (start == null || end == null) return "${timeline_time(start)} - ${timeline_time(end)}"
            val ds = same_day_start(start)
            val de = same_day_start(end)
            val ts = time_only(start)
            val te = time_only(end)
            return when {
                ds == de && ds == today_start -> "$ts - $te"
                ds == de -> "${day_format.format(Date(start))} $ts - $te"
                else -> "${day_format.format(Date(start))} $ts - ${day_format.format(Date(end))} $te"
            }
        }

        fun duration_minutes_str(start: Long?, end: Long?): String? {
            if (start == null || end == null || end <= start) return null
            val mins = ((end - start) + 30_000L) / 60_000L
            return getString(R.string.duration_minutes, mins.toInt())
        }

        fun q(event: AddonEvent, alarm_now: Long = today_start) = query_host_addon_time(this, host, event, alarm_now)
        fun is_friday(millis: Long): Boolean =
            Calendar.getInstance(tz).run {
                timeInMillis = millis
                get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
            }
        fun prayer_label(event: AddonEvent, time: Long?): String {
            if (event != AddonEvent.prayer_dhuhr || time == null) return getString(event.title_res)
            return if (is_friday(time)) getString(R.string.event_prayer_jummah) else getString(R.string.event_prayer_dhuhr)
        }

        val now = System.currentTimeMillis()

        val sun_today = query_host_sun(this, host, today_start)

        val fajr = q(AddonEvent.prayer_fajr)
        val asr = q(AddonEvent.prayer_asr)
        val isha = q(AddonEvent.prayer_isha)

        val maghrib_offset_ms = Prefs.get_maghrib_offset_minutes(this) * 60_000L

        val dhuhr = sun_today?.noon ?: q(AddonEvent.prayer_dhuhr)
        val maghrib = sun_today?.sunset?.plus(maghrib_offset_ms) ?: q(AddonEvent.prayer_maghrib)

        val sunrise = sun_today?.sunrise ?: q(AddonEvent.makruh_sunrise_start)
        val sunrise_end = q(AddonEvent.makruh_sunrise_end)
        val zawal_end = dhuhr
        val zawal_start =
            if (sun_today?.noon != null) sun_today.noon - Prefs.get_zawal_minutes(this) * 60_000L
            else q(AddonEvent.makruh_zawal_start)
        val sunset_start = q(AddonEvent.makruh_sunset_start)
        val sunset_end = sun_today?.sunset ?: q(AddonEvent.makruh_sunset_end)

        val prayers = listOf(
            AddonEvent.prayer_fajr to fajr,
            AddonEvent.prayer_dhuhr to dhuhr,
            AddonEvent.prayer_asr to asr,
            AddonEvent.prayer_maghrib to maghrib,
            AddonEvent.prayer_isha to isha
        )

        val fajr_tomorrow = q(AddonEvent.prayer_fajr, tomorrow_start)

        val hour_ms = 60L * 60L * 1000L
        val sunset_yesterday = query_host_sun(this, host, yesterday_start)?.sunset
        val night_prev = calc_night(sunset_yesterday?.plus(maghrib_offset_ms), fajr)
        val night_next = calc_night(maghrib, fajr_tomorrow)

        val scope_end = fajr_tomorrow ?: (tomorrow_start + 6L * hour_ms)

        val next_prayer = prayers.mapNotNull { (event, t) -> if (t != null && t >= now) event to t else null }.minByOrNull { it.second }
        val prev_prayer_time = prayers.mapNotNull { it.second }.filter { it < now }.maxOrNull() ?: q(AddonEvent.prayer_isha, yesterday_start)

        val items = ArrayList<HomeItemUi>(24)
        prayers.forEach { (event, t) ->
            if (t == null) return@forEach
            val is_next = next_prayer != null && event == next_prayer.first && t == next_prayer.second
            val is_passed = t < now && !is_next
            items.add(
                HomeItemUi.Prayer(
                    sort_time = t,
                    label = prayer_label(event, t),
                    time = timeline_time(t),
                    countdown = if (is_next) getString(R.string.in_countdown, format_countdown(t - now)) else null,
                    is_next = is_next,
                    is_passed = is_passed,
                    dot_icon =
                        when (event) {
                            AddonEvent.prayer_fajr -> R.drawable.ic_prayer_dawn
                            AddonEvent.prayer_isha -> R.drawable.ic_prayer_moon
                            AddonEvent.prayer_maghrib -> R.drawable.ic_prayer_sunset
                            else -> R.drawable.ic_prayer_sun
                        }
                )
            )
        }

        items.add(HomeItemUi.Now(now, time_format.format(Date(now))))

        listOf(
            Triple(R.string.prohibited_dawn, fajr, sunrise),
            Triple(R.string.prohibited_sunrise, sunrise, sunrise_end),
            Triple(R.string.prohibited_zawal, zawal_start, zawal_end),
            Triple(R.string.prohibited_after_asr, asr, sunset_start),
            Triple(R.string.prohibited_sunset, sunset_start, sunset_end)
        ).forEach { (label, start, end) ->
            if (start == null) return@forEach
            items.add(
                HomeItemUi.Window(
                    sort_time = start,
                    label = getString(label),
                    range = window_range_str(start, end),
                    duration = duration_minutes_str(start, end)
                )
            )
        }

        fun add_night_items(n: com.yshalsager.suntimes.prayertimesaddon.core.NightPortions?, min_t: Long, max_t: Long) {
            if (n == null) return
            listOf(
                getString(R.string.night_midpoint) to n.midpoint,
                getString(R.string.night_last_third) to n.last_third,
                getString(R.string.night_last_sixth) to n.last_sixth
            ).forEach { (label, t) ->
                if (t in min_t..max_t) items.add(HomeItemUi.Night(t, label, timeline_time(t)))
            }
        }

        add_night_items(night_prev, today_start, (tomorrow_start - 1L).coerceAtLeast(today_start))
        add_night_items(night_next, today_start, scope_end)

        if (fajr_tomorrow != null) {
            items.add(
                HomeItemUi.Prayer(
                    sort_time = fajr_tomorrow,
                    label = prayer_label(AddonEvent.prayer_fajr, fajr_tomorrow),
                    time = timeline_time(fajr_tomorrow),
                    countdown = null,
                    is_next = false,
                    is_passed = false,
                    dot_icon = R.drawable.ic_prayer_dawn
                )
            )
        }

        val ordered = items.filter { it.sort_time in today_start..scope_end }.sortedBy { it.sort_time }
        val next_boundary_millis = ordered.map { it.sort_time }.filter { it >= now }.minOrNull()

        val next_ui = if (next_prayer == null) null else {
            val nt = next_prayer.second
            val pt = prev_prayer_time
            val progress = if (pt != null && nt > pt) ((now - pt).toFloat() / (nt - pt).toFloat()).coerceIn(0f, 1f) else 0f
            NextPrayerUi(
                label = getString(R.string.next_prayer_label, prayer_label(next_prayer.first, nt)),
                time = time_only(nt),
                countdown = getString(R.string.in_countdown, format_countdown(nt - now)),
                progress = progress
            )
        }

        val host_now = "${day_format.format(Date(now))} ${time_format.format(Date(now))}"
        val ui_state = HomeUiState(
            method_summary = format_method_summary(this),
            location_summary = "$loc | $host_now",
            host_footer = "HOST: $host",
            error = null,
            next_prayer = next_ui,
            items = ordered
        )

        return Computed(ui_state, next_prayer?.second, prev_prayer_time, next_boundary_millis, today_start, tz, time_format, day_format, loc)
    }

    private fun apply_computed(c: Computed) {
        state = c.state
        next_time_millis = c.next_time_millis
        prev_time_millis = c.prev_time_millis
        next_boundary_millis = c.next_boundary_millis
        today_start = c.today_start
        tz = c.tz
        time_format = c.time_format
        day_format = c.day_format
        loc = c.loc
    }

    private fun start_tick(host: String) {
        tick?.let(ui::removeCallbacks)

        fun same_day_start(millis: Long): Long {
            val zone = tz ?: return 0L
            return Calendar.getInstance(zone).run {
                timeInMillis = millis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                timeInMillis
            }
        }

        fun update() {
            val n = System.currentTimeMillis()
            val ts = today_start
            if (ts != null && same_day_start(n) != ts) {
                refresh_home()
                return
            }
            val boundary = next_boundary_millis
            if (boundary != null && n >= boundary) {
                refresh_home()
                return
            }

            val zone = tz
            val tf = time_format
            val df = day_format
            val location = loc
            val now_str =
                if (zone != null && tf != null) {
                    tf.timeZone = zone
                    tf.format(Date(n))
                } else {
                    DateFormat.getTimeFormat(this).format(Date(n))
                }
            if (zone != null && tf != null && df != null && location != null) {
                df.timeZone = zone
                val host_now = "${df.format(Date(n))} $now_str"
                state = state.copy(location_summary = "$location | $host_now")
            }

            val nt = next_time_millis
            val pt = prev_time_millis
            val next_prayer = state.next_prayer
            if (nt != null && next_prayer != null) {
                val countdown = getString(R.string.in_countdown, format_countdown(nt - n))
                val progress = if (pt != null && nt > pt) ((n - pt).toFloat() / (nt - pt).toFloat()).coerceIn(0f, 1f) else 0f
                state = state.copy(next_prayer = next_prayer.copy(countdown = countdown, progress = progress))
            }

            state = state.copy(
                items = state.items.map { item ->
                    when (item) {
                        is HomeItemUi.Prayer ->
                            if (item.is_next) item.copy(countdown = getString(R.string.in_countdown, format_countdown(item.sort_time - n))) else item
                        is HomeItemUi.Now -> item.copy(time = now_str)
                        else -> item
                    }
                }
            )

            tick?.let { ui.postDelayed(it, 30_000L) }
        }

        tick = Runnable { update() }
        ui.postDelayed(tick!!, 30_000L)
    }

    private fun format_countdown(delta_ms: Long): String {
        val mins = (delta_ms.coerceAtLeast(0L) + 30_000L) / 60_000L
        val h = mins / 60
        val m = mins % 60
        return if (h > 0) String.format(Locale.getDefault(), "%d:%02d", h, m)
        else String.format(Locale.getDefault(), "%d%s", m, getString(R.string.minute_abbrev))
    }

    // method summary formatting is shared with widget/settings
}
