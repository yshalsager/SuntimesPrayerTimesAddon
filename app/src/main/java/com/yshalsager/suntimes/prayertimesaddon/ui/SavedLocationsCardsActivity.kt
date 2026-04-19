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
import com.yshalsager.suntimes.prayertimesaddon.core.AppClock
import com.yshalsager.suntimes.prayertimesaddon.core.HostResolver
import com.yshalsager.suntimes.prayertimesaddon.core.Prefs
import com.yshalsager.suntimes.prayertimesaddon.core.SavedLocations
import com.yshalsager.suntimes.prayertimesaddon.core.calc_night
import com.yshalsager.suntimes.prayertimesaddon.core.day_start_at
import com.yshalsager.suntimes.prayertimesaddon.core.format_method_summary
import com.yshalsager.suntimes.prayertimesaddon.core.query_host_addon_time
import com.yshalsager.suntimes.prayertimesaddon.core.query_inputs
import com.yshalsager.suntimes.prayertimesaddon.core.query_host_sun
import com.yshalsager.suntimes.prayertimesaddon.core.open_url
import com.yshalsager.suntimes.prayertimesaddon.core.resolve_location_query_context
import com.yshalsager.suntimes.prayertimesaddon.core.valid_timezone_id
import com.yshalsager.suntimes.prayertimesaddon.ui.compose.PrayerTimesTheme
import com.yshalsager.suntimes.prayertimesaddon.ui.compose.SavedLocationCardUi
import com.yshalsager.suntimes.prayertimesaddon.ui.compose.SavedLocationsCardsScreen
import com.yshalsager.suntimes.prayertimesaddon.ui.compose.SavedLocationsCardsUiState
import com.yshalsager.suntimes.prayertimesaddon.core.add_days
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class SavedLocationsCardsActivity : ThemedActivity() {
    companion object {
        private const val request_code_permissions = 1101
    }

    private var state by mutableStateOf(
        SavedLocationsCardsUiState(
            title = "",
            subtitle = null,
            cards = emptyList(),
            host_footer = "--",
            error = null,
            show_reinstall_addon = false
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PrayerTimesTheme {
                SavedLocationsCardsScreen(
                    state = state,
                    on_back = { finish() },
                    on_open_settings = { startActivity(Intent(this@SavedLocationsCardsActivity, SettingsActivity::class.java)) },
                    on_install_host = { open_url(this@SavedLocationsCardsActivity, "https://github.com/forrestguice/SuntimesWidget") },
                    on_reinstall_addon = { open_url(this@SavedLocationsCardsActivity, "https://github.com/yshalsager/SuntimesPrayerTimesAddon/releases/latest") }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refresh_cards()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == request_code_permissions) refresh_cards()
    }

    private fun refresh_cards() {
        val host = HostResolver.ensure_default_selected(this)
        val title = getString(R.string.saved_locations_cards_title)
        val subtitle = getString(R.string.saved_locations_cards_subtitle)
        if (host == null) {
            state = state.copy(title = title, subtitle = subtitle, error = getString(R.string.no_host_found), show_reinstall_addon = false, cards = emptyList())
            return
        }

        val required_perm = HostResolver.get_required_permission(this, host)
        if (required_perm != null && ContextCompat.checkSelfPermission(this, required_perm) != PackageManager.PERMISSION_GRANTED) {
            val declared =
                try {
                    @Suppress("DEPRECATION")
                    packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS).requestedPermissions?.contains(required_perm) == true
                } catch (_: Exception) {
                    false
                }
            val requestable = HostResolver.is_runtime_permission(this, required_perm) && declared
            if (requestable) {
                ActivityCompat.requestPermissions(this, arrayOf(required_perm), request_code_permissions)
            }
            val message =
                if (requestable) getString(R.string.missing_permission, required_perm)
                else getString(R.string.missing_permission_reinstall, required_perm)
            state = state.copy(title = title, subtitle = subtitle, error = message, show_reinstall_addon = !requestable, cards = emptyList(), host_footer = "HOST: $host")
            return
        }

        Thread {
            val computed_cards = compute_cards(host)
            runOnUiThread {
                state =
                    SavedLocationsCardsUiState(
                        title = title,
                        subtitle = subtitle,
                        cards = computed_cards,
                        host_footer = "HOST: $host",
                        error = null,
                        show_reinstall_addon = false
                    )
            }
        }.start()
    }

    private fun compute_cards(host: String): List<SavedLocationCardUi> {
        val saved_locations = SavedLocations.load(this)
        if (saved_locations.isEmpty()) return emptyList()

        val now = AppClock.now_millis()
        return saved_locations.map { location ->
            val location_context = resolve_location_query_context(this, location.id, null, null, null, saved_locations)
            val timezone_id = location_context.timezone_override?.id ?: valid_timezone_id(location.timezone_id) ?: TimeZone.getDefault().id
            val tz = TimeZone.getTimeZone(timezone_id)
            val day_start = day_start_at(now, tz)
            val time_format = DateFormat.getTimeFormat(this).apply { timeZone = tz }
            val date_format = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).apply { timeZone = tz }
            val inputs = location_context.query_inputs(day_start)
            val method_override = inputs.method_config_override
            val maghrib_offset_millis = (method_override?.maghrib_offset_minutes ?: Prefs.get_maghrib_offset_minutes(this)) * 60_000L

            fun q(event: AddonEvent, alarm_now: Long = day_start): Long? =
                location_context.query_inputs(alarm_now).let { query ->
                    query_host_addon_time(
                        context = this,
                        host_event_authority = host,
                        event = event,
                        alarm_now = alarm_now,
                        selection = query.selection,
                        selection_args = query.selection_args,
                        timezone_override = query.timezone_override ?: tz,
                        latitude_override = query.latitude_override,
                        method_config_override = query.method_config_override,
                        addon_runtime_profile_override = query.addon_runtime_profile_override
                    )
                }

            val sun_today = query_host_sun(this, host, day_start, inputs.selection, inputs.selection_args)
            fun same_selected_day(v: Long?): Boolean = v != null && day_start_at(v, tz) == day_start
            val sun_today_noon = sun_today?.noon?.takeIf(::same_selected_day)
            val sun_today_sunrise = sun_today?.sunrise?.takeIf(::same_selected_day)
            val sun_today_sunset = sun_today?.sunset?.takeIf(::same_selected_day)
            val fajr = q(AddonEvent.prayer_fajr)
            val fajr_extra_1 = q(AddonEvent.prayer_fajr_extra_1)
            val duha = q(AddonEvent.prayer_duha)
            val eid_start = q(AddonEvent.prayer_eid_start)
            val eid_end = q(AddonEvent.prayer_eid_end)
            val dhuhr = sun_today_noon ?: q(AddonEvent.prayer_dhuhr)
            val asr = q(AddonEvent.prayer_asr)
            val maghrib = sun_today_sunset?.plus(maghrib_offset_millis) ?: q(AddonEvent.prayer_maghrib)
            val isha = q(AddonEvent.prayer_isha)
            val isha_extra_1 = q(AddonEvent.prayer_isha_extra_1)
            val sunrise = sun_today_sunrise ?: q(AddonEvent.makruh_sunrise_start)
            val sunrise_end = q(AddonEvent.makruh_sunrise_end)
            val zawal_start =
                if (sun_today_noon != null) {
                    sun_today_noon - (method_override?.zawal_minutes ?: Prefs.get_zawal_minutes(this)) * 60_000L
                } else {
                    q(AddonEvent.makruh_zawal_start)
                }
            val sunset_start = q(AddonEvent.makruh_sunset_start)
            val sunset_end = sun_today_sunset ?: q(AddonEvent.makruh_sunset_end)
            val is_friday = Calendar.getInstance(tz).run {
                timeInMillis = day_start
                get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
            }

            val tomorrow_start = add_days(day_start, 1, tz)
            val tomorrow_inputs = location_context.query_inputs(tomorrow_start)
            val fajr_next =
                query_host_addon_time(
                    context = this,
                    host_event_authority = host,
                    event = AddonEvent.prayer_fajr,
                    alarm_now = tomorrow_start,
                    selection = tomorrow_inputs.selection,
                    selection_args = tomorrow_inputs.selection_args,
                    timezone_override = tomorrow_inputs.timezone_override ?: tz,
                    latitude_override = tomorrow_inputs.latitude_override,
                    method_config_override = tomorrow_inputs.method_config_override,
                    addon_runtime_profile_override = tomorrow_inputs.addon_runtime_profile_override
                )
            val night = calc_night(maghrib, fajr_next)
            val method_summary = format_method_summary(this, method_override)
            val date_label = date_format.format(Date(day_start))
            val time_only_format =
                SimpleDateFormat(
                    if (DateFormat.is24HourFormat(this)) "HH:mm" else "h:mm",
                    Locale.getDefault()
                ).apply { timeZone = tz }

            fun range_short(start: Long?, end: Long?): String = "${format_or_dash(time_only_format, start)}-${format_or_dash(time_only_format, end)}"

            SavedLocationCardUi(
                id = location.id,
                title = location.display_label(),
                subtitle = "$date_label · $timezone_id · $method_summary",
                is_friday = is_friday,
                fajr = format_or_dash(time_format, fajr),
                fajr_extra_1 = fajr_extra_1?.let { format_or_dash(time_format, fajr_extra_1) },
                duha = duha?.let { format_or_dash(time_format, duha) },
                eid_start = eid_start?.let { format_or_dash(time_format, eid_start) },
                eid_end = eid_end?.let { format_or_dash(time_format, eid_end) },
                dhuhr = format_or_dash(time_format, dhuhr),
                asr = format_or_dash(time_format, asr),
                maghrib = format_or_dash(time_format, maghrib),
                isha = format_or_dash(time_format, isha),
                isha_extra_1 = isha_extra_1?.let { format_or_dash(time_format, isha_extra_1) },
                prohibited_dawn = range_short(fajr, sunrise),
                prohibited_sunrise = range_short(sunrise, sunrise_end),
                prohibited_zawal = range_short(zawal_start, dhuhr),
                prohibited_after_asr = range_short(asr, sunset_start),
                prohibited_sunset = range_short(sunset_start, sunset_end),
                night_midpoint = night?.midpoint?.let { format_or_dash(time_only_format, it) },
                night_last_third = night?.last_third?.let { format_or_dash(time_only_format, it) },
                night_last_sixth = night?.last_sixth?.let { format_or_dash(time_only_format, it) }
            )
        }
    }

    private fun format_or_dash(formatter: java.text.DateFormat, value: Long?): String =
        value?.let { formatter.format(Date(it)) } ?: "--"
}
