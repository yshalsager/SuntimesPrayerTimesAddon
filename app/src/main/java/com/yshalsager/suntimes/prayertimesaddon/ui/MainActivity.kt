package com.yshalsager.suntimes.prayertimesaddon.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.format.DateFormat
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.net.Uri
import android.provider.AlarmClock
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
import com.yshalsager.suntimes.prayertimesaddon.core.hijri_for_day
import com.yshalsager.suntimes.prayertimesaddon.core.query_host_addon_time
import com.yshalsager.suntimes.prayertimesaddon.core.query_host_sun
import com.yshalsager.suntimes.prayertimesaddon.ui.compose.HomeItemUi
import com.yshalsager.suntimes.prayertimesaddon.ui.compose.HomeScreen
import com.yshalsager.suntimes.prayertimesaddon.ui.compose.HomeDayUiState
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
import com.yshalsager.suntimes.prayertimesaddon.core.AlarmEventContract
import com.yshalsager.suntimes.prayertimesaddon.provider.PrayerTimesProvider

class MainActivity : ThemedActivity() {
    private val request_code_permissions = 1001
    private val ui = Handler(Looper.getMainLooper())
    private var tick: Runnable? = null

    private var center_day_start: Long? = null
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
            days = listOf(
                HomeDayUiState(0L, "--", null, emptyList()),
                HomeDayUiState(0L, "--", null, emptyList()),
                HomeDayUiState(0L, "--", null, emptyList())
            )
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PrayerTimesTheme {
                HomeScreen(
                    state = state,
                    on_open_days = { startActivity(Intent(this@MainActivity, DaysActivity::class.java)) },
                    on_open_settings = { startActivity(Intent(this@MainActivity, SettingsActivity::class.java)) },
                    on_open_alarm = { event_id -> open_host_alarm(event_id) },
                    on_shift_day = { delta -> shift_day(delta) }
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
        val center_day_start: Long,
        val tz: TimeZone,
        val time_format: java.text.DateFormat,
        val day_format: SimpleDateFormat,
        val loc: String
    )

    private fun compute_home(host: String): Computed {
        val host_config = HostConfigReader.read_config(this, host)
        val tz = host_config?.timezone?.let(TimeZone::getTimeZone) ?: TimeZone.getDefault()
        val now = System.currentTimeMillis()
        val maghrib_offset_ms = Prefs.get_maghrib_offset_minutes(this) * 60_000L

        fun same_day_start(millis: Long): Long =
            Calendar.getInstance(tz).run {
                timeInMillis = millis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                timeInMillis
            }

        fun shift_day_start(day_start: Long, delta: Int): Long =
            Calendar.getInstance(tz).run {
                timeInMillis = day_start
                add(Calendar.DAY_OF_YEAR, delta)
                timeInMillis
            }

        val today_start = same_day_start(now)
        val center = same_day_start(center_day_start ?: today_start)
        center_day_start = center

        val time_format = DateFormat.getTimeFormat(this).apply { timeZone = tz }
        val locale = Locale.getDefault()
        val day_format = SimpleDateFormat("EEE", locale).apply { timeZone = tz }
        val date_format: java.text.DateFormat =
            when (Prefs.get_gregorian_date_format(this)) {
                Prefs.gregorian_date_format_long -> java.text.DateFormat.getDateInstance(java.text.DateFormat.LONG, locale)
                Prefs.gregorian_date_format_card -> SimpleDateFormat("MMM d", locale)
                else -> java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM, locale)
            }.apply { timeZone = tz }
        val loc = host_config?.display_label() ?: "--"

        fun day_time(v: Long?): String = v?.let { "${day_format.format(Date(it))} ${time_format.format(Date(it))}" } ?: "--"
        fun time_only(v: Long?): String = v?.let { time_format.format(Date(it)) } ?: "--"

        fun is_friday(millis: Long): Boolean =
            Calendar.getInstance(tz).run {
                timeInMillis = millis
                get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
            }

        fun prayer_label(event: AddonEvent, time: Long?): String {
            if (event != AddonEvent.prayer_dhuhr || time == null) return getString(event.title_res)
            return if (is_friday(time)) getString(R.string.event_prayer_jummah) else getString(R.string.event_prayer_dhuhr)
        }

        data class DayComputed(
            val day: HomeDayUiState,
            val next_time_millis: Long?,
            val prev_time_millis: Long?,
            val next_boundary_millis: Long?
        )

        val month_basis = Prefs.get_days_month_basis(this)
        val show_hijri_effective = Prefs.get_days_show_hijri(this) || month_basis == Prefs.days_month_basis_hijri
        val hijri_variant = Prefs.get_hijri_variant(this)
        val hijri_offset = Prefs.get_hijri_day_offset(this)

        fun build_day(day_start: Long): DayComputed {
            val is_today = day_start == today_start
            val yesterday_start = shift_day_start(day_start, -1)
            val tomorrow_start = shift_day_start(day_start, 1)

            fun timeline_time(v: Long?): String {
                if (v == null) return "--"
                return if (same_day_start(v) == day_start) time_only(v) else day_time(v)
            }

            fun window_range_str(start: Long?, end: Long?): String {
                if (start == null || end == null) return "${timeline_time(start)} - ${timeline_time(end)}"
                val ds = same_day_start(start)
                val de = same_day_start(end)
                val ts = time_only(start)
                val te = time_only(end)
                return when {
                    ds == de && ds == day_start -> "$ts - $te"
                    ds == de -> "${day_format.format(Date(start))} $ts - $te"
                    else -> "${day_format.format(Date(start))} $ts - ${day_format.format(Date(end))} $te"
                }
            }

            fun duration_minutes_str(start: Long?, end: Long?): String? {
                if (start == null || end == null || end <= start) return null
                val mins = ((end - start) + 30_000L) / 60_000L
                return getString(R.string.duration_minutes, mins.toInt())
            }

            fun q(event: AddonEvent, alarm_now: Long = day_start) = query_host_addon_time(this, host, event, alarm_now)

            val sun_today = query_host_sun(this, host, day_start)

            val fajr = q(AddonEvent.prayer_fajr)
            val asr = q(AddonEvent.prayer_asr)
            val isha = q(AddonEvent.prayer_isha)

            val dhuhr = sun_today?.noon ?: q(AddonEvent.prayer_dhuhr)
            val maghrib = sun_today?.sunset?.plus(maghrib_offset_ms) ?: q(AddonEvent.prayer_maghrib)

            val sunrise = sun_today?.sunrise ?: q(AddonEvent.makruh_sunrise_start)
            val sunrise_end = q(AddonEvent.makruh_sunrise_end)
            val zawal_end = dhuhr
            val zawal_start = if (sun_today?.noon != null) sun_today.noon - Prefs.get_zawal_minutes(this) * 60_000L else q(AddonEvent.makruh_zawal_start)
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

            val next_prayer = if (!is_today) null else prayers.mapNotNull { (event, t) -> if (t != null && t >= now) event to t else null }.minByOrNull { it.second }
            val prev_prayer_time = if (!is_today) null else prayers.mapNotNull { it.second }.filter { it < now }.maxOrNull() ?: q(AddonEvent.prayer_isha, yesterday_start)

            val items = ArrayList<HomeItemUi>(24)
            prayers.forEach { (event, t) ->
                if (t == null) return@forEach
                val is_next = is_today && next_prayer != null && event == next_prayer.first && t == next_prayer.second
                val is_passed = is_today && t < now && !is_next
                items.add(
                    HomeItemUi.Prayer(
                        sort_time = t,
                        event_id = event.event_id,
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

            if (is_today) items.add(HomeItemUi.Now(now, time_format.format(Date(now))))

            listOf(
                Triple(R.string.prohibited_dawn, fajr, sunrise),
                Triple(R.string.prohibited_sunrise, sunrise, sunrise_end),
                Triple(R.string.prohibited_zawal, zawal_start, zawal_end),
                Triple(R.string.prohibited_after_asr, asr, sunset_start),
                Triple(R.string.prohibited_sunset, sunset_start, sunset_end)
            ).forEach { (label, start, end) ->
                if (start == null) return@forEach
                items.add(HomeItemUi.Window(sort_time = start, label = getString(label), range = window_range_str(start, end), duration = duration_minutes_str(start, end)))
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

            add_night_items(night_prev, day_start, (tomorrow_start - 1L).coerceAtLeast(day_start))
            add_night_items(night_next, day_start, scope_end)

            if (fajr_tomorrow != null) {
                items.add(
                    HomeItemUi.Prayer(
                        sort_time = fajr_tomorrow,
                        event_id = AddonEvent.prayer_fajr.event_id,
                        label = prayer_label(AddonEvent.prayer_fajr, fajr_tomorrow),
                        time = timeline_time(fajr_tomorrow),
                        countdown = null,
                        is_next = false,
                        is_passed = false,
                        dot_icon = R.drawable.ic_prayer_dawn
                    )
                )
            }

            val ordered = items.filter { it.sort_time in day_start..scope_end }.sortedBy { it.sort_time }
            val next_boundary_millis = if (!is_today) null else ordered.map { it.sort_time }.filter { it >= now }.minOrNull()

            val next_ui = if (!is_today || next_prayer == null) null else {
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

            val day_parts = ArrayList<String>(5)
            if (is_today) day_parts.add(getString(R.string.today))
            day_parts.add(day_format.format(Date(day_start)))

            val greg = date_format.format(Date(day_start))
            val hijri =
                if (!show_hijri_effective) null
                else
                    hijri_for_day(
                            day_start_millis = day_start,
                            tz = tz,
                            locale = locale,
                            variant = hijri_variant,
                            offset_days = hijri_offset
                        )
                        .formatted

            if (month_basis == Prefs.days_month_basis_hijri) {
                hijri?.let(day_parts::add)
                day_parts.add(greg)
            } else {
                day_parts.add(greg)
                hijri?.let(day_parts::add)
            }
            val day_label = day_parts.joinToString(" \u00b7 ")
            val day_state = HomeDayUiState(day_start = day_start, day_label = day_label, next_prayer = next_ui, items = ordered)
            return DayComputed(day_state, next_prayer?.second, prev_prayer_time, next_boundary_millis)
        }

        val day_prev = build_day(shift_day_start(center, -1))
        val day_center = build_day(center)
        val day_next = build_day(shift_day_start(center, 1))

        val host_now = "${day_format.format(Date(now))} ${time_format.format(Date(now))}"
        val ui_state = HomeUiState(
            method_summary = format_method_summary(this),
            location_summary = "$loc | $host_now",
            host_footer = "HOST: $host",
            error = null,
            days = listOf(day_prev.day, day_center.day, day_next.day)
        )

        return Computed(ui_state, day_center.next_time_millis, day_center.prev_time_millis, day_center.next_boundary_millis, today_start, center, tz, time_format, day_format, loc)
    }

    private fun open_host_alarm(event_id: String) {
        val host_event_authority = HostResolver.ensure_default_selected(this) ?: return
        val host_package = packageManager.resolveContentProvider(host_event_authority, 0)?.packageName ?: return
        val component = ComponentName(host_package, "com.forrestguice.suntimeswidget.alarmclock.ui.AlarmClockActivity")
        val event_uri = Uri.parse("content://${PrayerTimesProvider.authority}/${AlarmEventContract.query_event_info}/$event_id").toString()

        val intent = Intent(AlarmClock.ACTION_SET_ALARM)
            .setComponent(component)
            .putExtra("solarevent", event_uri)
            .putExtra("alarmtype", "ALARM")

        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
        }
    }

    private fun apply_computed(c: Computed) {
        state = c.state
        next_time_millis = c.next_time_millis
        prev_time_millis = c.prev_time_millis
        next_boundary_millis = c.next_boundary_millis
        today_start = c.today_start
        center_day_start = c.center_day_start
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
            if (ts != null) {
                state = state.copy(
                    days =
                        state.days.map { day ->
                            if (day.day_start != ts) day
                            else {
                                val np = day.next_prayer
                                val next_updated =
                                    if (nt != null && np != null) {
                                        val countdown = getString(R.string.in_countdown, format_countdown(nt - n))
                                        val progress = if (pt != null && nt > pt) ((n - pt).toFloat() / (nt - pt).toFloat()).coerceIn(0f, 1f) else 0f
                                        np.copy(countdown = countdown, progress = progress)
                                    } else {
                                        np
                                    }
                                day.copy(
                                    next_prayer = next_updated,
                                    items =
                                        day.items.map { item ->
                                            when (item) {
                                                is HomeItemUi.Prayer ->
                                                    if (item.is_next) item.copy(countdown = getString(R.string.in_countdown, format_countdown(item.sort_time - n))) else item
                                                is HomeItemUi.Now -> item.copy(time = now_str)
                                                else -> item
                                            }
                                        }
                                )
                            }
                        }
                )
            }

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

    private fun shift_day(delta: Int) {
        val zone = tz ?: return
        val center = center_day_start ?: return
        center_day_start =
            Calendar.getInstance(zone).run {
                timeInMillis = center
                add(Calendar.DAY_OF_YEAR, delta)
                timeInMillis
            }
        refresh_home()
    }
}
