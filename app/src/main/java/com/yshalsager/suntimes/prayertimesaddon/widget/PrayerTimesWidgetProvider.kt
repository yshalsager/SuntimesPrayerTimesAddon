package com.yshalsager.suntimes.prayertimesaddon.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.edit
import androidx.core.content.ContextCompat
import androidx.core.os.ConfigurationCompat
import androidx.core.text.layoutDirection
import com.yshalsager.suntimes.prayertimesaddon.R
import com.yshalsager.suntimes.prayertimesaddon.core.AddonEvent
import com.yshalsager.suntimes.prayertimesaddon.core.AppIds
import com.yshalsager.suntimes.prayertimesaddon.core.HostConfigReader
import com.yshalsager.suntimes.prayertimesaddon.core.HostResolver
import com.yshalsager.suntimes.prayertimesaddon.core.Prefs
import com.yshalsager.suntimes.prayertimesaddon.core.calc_night
import com.yshalsager.suntimes.prayertimesaddon.core.format_method_summary
import com.yshalsager.suntimes.prayertimesaddon.core.hijri_for_day
import com.yshalsager.suntimes.prayertimesaddon.core.query_addon_time
import com.yshalsager.suntimes.prayertimesaddon.core.format_gregorian_day_title
import com.yshalsager.suntimes.prayertimesaddon.core.resolve_location_query_context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import android.os.Build
import java.util.UUID

class PrayerTimesWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        update_all(context, appWidgetManager, appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        update_all(context, appWidgetManager, intArrayOf(appWidgetId))
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        if (action == action_alarm && !is_valid_alarm_intent(context, intent)) return
        if (action == action_alarm || action == Intent.ACTION_TIME_CHANGED || action == Intent.ACTION_TIMEZONE_CHANGED || action == Intent.ACTION_DATE_CHANGED || action == Intent.ACTION_LOCALE_CHANGED) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, PrayerTimesWidgetProvider::class.java))
            if (ids.isNotEmpty()) update_all(context, mgr, ids)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        WidgetPrefs.clear_saved_location_ids(context, appWidgetIds)
    }

    private fun update_all(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        val host = HostResolver.ensure_default_selected(context)
        if (host == null) {
            ids.forEach { id ->
                val rv = RemoteViews(context.packageName, R.layout.widget_prayer_times)
                rv.setTextViewText(R.id.widget_hijri, context.getString(R.string.no_host_found))
                rv.setTextViewText(R.id.widget_gregorian, "")
                rv.setTextViewText(R.id.widget_summary, "")
                rv.setViewVisibility(R.id.widget_prohibited_row, View.GONE)
                rv.setViewVisibility(R.id.widget_night_row, View.GONE)
                val open_main = PendingIntent.getActivity(context, 0, Intent(context, com.yshalsager.suntimes.prayertimesaddon.ui.MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
                rv.setOnClickPendingIntent(R.id.widget_root, open_main)
                rv.setOnClickPendingIntent(R.id.widget_header, open_main)
                mgr.updateAppWidget(id, rv)
            }
            return
        }

        val cfg = HostConfigReader.read_config(context, host)
        val host_tz = cfg?.timezone?.let(TimeZone::getTimeZone) ?: TimeZone.getDefault()
        val host_location_label = cfg?.display_label() ?: context.getString(R.string.unknown_location)
        val now = System.currentTimeMillis()

        val month_basis = Prefs.get_days_month_basis(context)
        val show_hijri = Prefs.get_days_show_hijri(context) || month_basis == Prefs.days_month_basis_hijri
        val locale = ConfigurationCompat.getLocales(context.resources.configuration).get(0) ?: Locale.getDefault()
        val rtl = locale.layoutDirection == View.LAYOUT_DIRECTION_RTL
        val row_dir = if (rtl) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR

        val widget_show_prohibited = Prefs.get_widget_show_prohibited(context)
        val widget_show_night = Prefs.get_widget_show_night_portions(context)
        val colors = widget_colors(context)
        val all_candidates = ArrayList<Long>()

        ids.forEach { id ->
            val requested_saved_location_id = WidgetPrefs.get_saved_location_id(context, id)
            val location_context = resolve_location_query_context(context, requested_saved_location_id, null, null, null)
            if (requested_saved_location_id != null && location_context.saved_location == null) {
                WidgetPrefs.set_saved_location_id(context, id, null)
            }
            val scoped_saved_location_id = location_context.resolved_saved_location_id
            val tz = location_context.timezone_override ?: host_tz
            val day_start = day_start(now, tz)
            val time_format = DateFormat.getTimeFormat(context).apply { timeZone = tz }
            val time_only_format =
                SimpleDateFormat(if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm", Locale.getDefault()).apply { timeZone = tz }

            fun time_short(v: Long?): String = v?.let { time_only_format.format(Date(it)) } ?: "--"
            fun time_str(v: Long?): String = v?.let { time_format.format(Date(it)) } ?: "--"
            fun range(a: Long?, b: Long?): String = "${time_short(a)}-${time_short(b)}"

            val fajr = query_addon_time(context, AddonEvent.prayer_fajr, day_start, saved_location_id = scoped_saved_location_id)
            val duha = query_addon_time(context, AddonEvent.prayer_duha, day_start, saved_location_id = scoped_saved_location_id)
            val dhuhr = query_addon_time(context, AddonEvent.prayer_dhuhr, day_start, saved_location_id = scoped_saved_location_id)
            val asr = query_addon_time(context, AddonEvent.prayer_asr, day_start, saved_location_id = scoped_saved_location_id)
            val maghrib = query_addon_time(context, AddonEvent.prayer_maghrib, day_start, saved_location_id = scoped_saved_location_id)
            val isha = query_addon_time(context, AddonEvent.prayer_isha, day_start, saved_location_id = scoped_saved_location_id)

            val hijri_variant = location_context.addon_runtime_profile_override?.hijri_variant ?: Prefs.get_hijri_variant(context)
            val hijri_offset = location_context.addon_runtime_profile_override?.hijri_day_offset ?: Prefs.get_hijri_day_offset(context)
            val hijri =
                if (!show_hijri) null
                else
                    try {
                        hijri_for_day(day_start, tz, locale, hijri_variant, hijri_offset).formatted
                    } catch (_: ArithmeticException) {
                        null
                    }
            val greg = format_gregorian_day_title(context, day_start, tz, locale)
            val location_label = location_context.saved_location?.display_label() ?: host_location_label
            val method_summary = format_method_summary(context, location_context.method_config_override)
            val summary = "$location_label \u00b7 $method_summary"

            val is_friday =
                Calendar.getInstance(tz).run {
                    timeInMillis = day_start
                    get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
                }

            val sunrise =
                if (!widget_show_prohibited) null
                else query_addon_time(context, AddonEvent.makruh_sunrise_start, day_start, saved_location_id = scoped_saved_location_id)
            val sunrise_end = if (!widget_show_prohibited) null else duha
            val zawal_start =
                if (!widget_show_prohibited) null
                else query_addon_time(context, AddonEvent.makruh_zawal_start, day_start, saved_location_id = scoped_saved_location_id)
            val sunset_start =
                if (!widget_show_prohibited) null
                else query_addon_time(context, AddonEvent.makruh_sunset_start, day_start, saved_location_id = scoped_saved_location_id)
            val sunset =
                if (!widget_show_prohibited) null
                else query_addon_time(context, AddonEvent.makruh_sunset_end, day_start, saved_location_id = scoped_saved_location_id)

            val prohibited_dawn = if (!widget_show_prohibited) null else range(fajr, sunrise)
            val prohibited_sunrise = if (!widget_show_prohibited) null else range(sunrise, sunrise_end)
            val prohibited_zawal = if (!widget_show_prohibited) null else range(zawal_start, dhuhr)
            val prohibited_after_asr = if (!widget_show_prohibited) null else range(asr, sunset_start)
            val prohibited_sunset = if (!widget_show_prohibited) null else range(sunset_start, sunset)

            val tomorrow_start = next_day_start(day_start, tz)
            val fajr_next =
                if (!widget_show_night) null
                else query_addon_time(context, AddonEvent.prayer_fajr, tomorrow_start, saved_location_id = scoped_saved_location_id)
            val night = if (!widget_show_night) null else calc_night(maghrib, fajr_next)
            val night_times =
                if (!widget_show_night) emptyList()
                else listOfNotNull(night?.midpoint, night?.last_third, night?.last_sixth)
            val night_ok = widget_show_night && night_times.size == 3
            val night_midpoint = night_times.getOrNull(0)
            val night_last_third = night_times.getOrNull(1)
            val night_last_sixth = night_times.getOrNull(2)

            val rv = RemoteViews(context.packageName, R.layout.widget_prayer_times)
            val layout_profile = widget_layout_profile(mgr, id)

            rv.setInt(R.id.widget_root, "setBackgroundResource", colors.bg_res)
            rv.setInt(R.id.widget_accent, "setBackgroundColor", colors.accent)

            val primary = if (month_basis == Prefs.days_month_basis_hijri && hijri != null) hijri else greg
            val secondary = if (month_basis == Prefs.days_month_basis_hijri) greg else (hijri ?: "")
            val secondary_text = if (!layout_profile.show_secondary_date || secondary.isBlank() || secondary == primary) "" else secondary

            rv.setTextViewText(R.id.widget_hijri, primary)
            rv.setTextViewText(R.id.widget_gregorian, secondary_text)
            rv.setViewVisibility(R.id.widget_gregorian, if (secondary_text.isBlank()) View.GONE else View.VISIBLE)
            rv.setTextViewText(R.id.widget_summary, summary)
            rv.setViewVisibility(R.id.widget_summary, if (layout_profile.show_summary) View.VISIBLE else View.GONE)

            rv.setTextColor(R.id.widget_hijri, colors.text_primary)
            rv.setTextColor(R.id.widget_gregorian, colors.text_muted)
            rv.setTextColor(R.id.widget_summary, colors.text_muted)

            // Keep prayer/prohibited/night column ordering consistent in RTL:
            // first column (Fajr/Dawn/Midpoint) stays on the Start side.
            rv.setInt(R.id.widget_prayer_row, "setLayoutDirection", row_dir)
            rv.setInt(R.id.widget_prohibited_row, "setLayoutDirection", row_dir)
            rv.setInt(R.id.widget_night_row, "setLayoutDirection", row_dir)

            rv.setTextViewText(R.id.widget_prayer_fajr, time_str(fajr))
            rv.setTextViewText(R.id.widget_prayer_duha, time_str(duha))
            rv.setTextViewText(R.id.widget_prayer_dhuhr, time_str(dhuhr))
            rv.setTextViewText(R.id.widget_prayer_asr, time_str(asr))
            rv.setTextViewText(R.id.widget_prayer_maghrib, time_str(maghrib))
            rv.setTextViewText(R.id.widget_prayer_isha, time_str(isha))

            rv.setTextColor(R.id.widget_prayer_fajr, colors.text_primary)
            rv.setTextColor(R.id.widget_prayer_duha, colors.accent)
            rv.setTextColor(R.id.widget_prayer_dhuhr, colors.text_primary)
            rv.setTextColor(R.id.widget_prayer_asr, colors.text_primary)
            rv.setTextColor(R.id.widget_prayer_maghrib, colors.text_primary)
            rv.setTextColor(R.id.widget_prayer_isha, colors.text_primary)

            rv.setTextViewText(R.id.widget_label_fajr, context.getString(R.string.event_prayer_fajr))
            rv.setTextViewText(R.id.widget_label_duha, context.getString(R.string.event_prayer_duha))
            rv.setTextViewText(R.id.widget_label_dhuhr, if (is_friday) context.getString(R.string.event_prayer_jummah) else context.getString(R.string.event_prayer_dhuhr))
            rv.setTextViewText(R.id.widget_label_asr, context.getString(R.string.event_prayer_asr))
            rv.setTextViewText(R.id.widget_label_maghrib, context.getString(R.string.event_prayer_maghrib))
            rv.setTextViewText(R.id.widget_label_isha, context.getString(R.string.event_prayer_isha))

            listOf(
                R.id.widget_label_fajr,
                R.id.widget_label_duha,
                R.id.widget_label_dhuhr,
                R.id.widget_label_asr,
                R.id.widget_label_maghrib,
                R.id.widget_label_isha
            ).forEach { rv.setTextColor(it, colors.text_muted) }
            rv.setTextColor(R.id.widget_label_duha, colors.accent)

            val prohibited_ok =
                layout_profile.show_optional_rows &&
                    widget_show_prohibited &&
                    listOf(prohibited_dawn, prohibited_sunrise, prohibited_zawal, prohibited_after_asr, prohibited_sunset).any { it != null }
            rv.setViewVisibility(R.id.widget_prohibited_row, if (prohibited_ok) View.VISIBLE else View.GONE)
            if (prohibited_ok) {
                fun labeled(label_res: Int, v: String?): String = "${context.getString(label_res)}\n${v ?: "--"}"

                rv.setTextViewText(R.id.widget_prohibited_dawn, labeled(R.string.prohibited_dawn, prohibited_dawn))
                rv.setTextViewText(R.id.widget_prohibited_sunrise, labeled(R.string.prohibited_sunrise, prohibited_sunrise))
                rv.setTextViewText(R.id.widget_prohibited_zawal, labeled(R.string.prohibited_zawal, prohibited_zawal))
                rv.setTextViewText(R.id.widget_prohibited_after_asr, labeled(R.string.prohibited_after_asr, prohibited_after_asr))
                rv.setTextViewText(R.id.widget_prohibited_sunset, labeled(R.string.prohibited_sunset, prohibited_sunset))

                rv.setTextColor(R.id.widget_prohibited_dawn, colors.prohibited_light)
                rv.setTextColor(R.id.widget_prohibited_sunrise, colors.prohibited_heavy)
                rv.setTextColor(R.id.widget_prohibited_zawal, colors.prohibited_heavy)
                rv.setTextColor(R.id.widget_prohibited_after_asr, colors.prohibited_light)
                rv.setTextColor(R.id.widget_prohibited_sunset, colors.prohibited_heavy)
            }

            val show_night_row = layout_profile.show_optional_rows && night_ok
            rv.setViewVisibility(R.id.widget_night_row, if (show_night_row) View.VISIBLE else View.GONE)
            if (show_night_row) {
                fun labeled(label_res: Int, v: Long?): String = "${context.getString(label_res)}\n${time_short(v)}"

                rv.setTextViewText(R.id.widget_night_midpoint, labeled(R.string.night_midpoint, night_midpoint))
                rv.setTextViewText(R.id.widget_night_last_third, labeled(R.string.night_last_third, night_last_third))
                rv.setTextViewText(R.id.widget_night_last_sixth, labeled(R.string.night_last_sixth, night_last_sixth))

                rv.setTextColor(R.id.widget_night_midpoint, colors.accent)
                rv.setTextColor(R.id.widget_night_last_third, colors.accent)
                rv.setTextColor(R.id.widget_night_last_sixth, colors.accent)
            }

            val open_main = PendingIntent.getActivity(context, 0, Intent(context, com.yshalsager.suntimes.prayertimesaddon.ui.MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
            rv.setOnClickPendingIntent(R.id.widget_root, open_main)

            val open_days = PendingIntent.getActivity(context, 1, Intent(context, com.yshalsager.suntimes.prayertimesaddon.ui.DaysActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
            rv.setOnClickPendingIntent(R.id.widget_header, open_days)

            mgr.updateAppWidget(id, rv)

            all_candidates += listOfNotNull(fajr, duha, dhuhr, asr, maghrib, isha)
            all_candidates += listOfNotNull(sunrise, sunrise_end, zawal_start, dhuhr, sunset_start, sunset)
            if (night_ok) all_candidates += night_times
            all_candidates += (tomorrow_start + 120_000L)
        }
        schedule_next(context, now, all_candidates)
    }

    private data class WidgetColors(
        val bg_res: Int,
        val accent: Int,
        val text_primary: Int,
        val text_muted: Int,
        val prohibited_light: Int,
        val prohibited_heavy: Int
    )

    private data class WidgetLayoutProfile(
        val show_secondary_date: Boolean,
        val show_summary: Boolean,
        val show_optional_rows: Boolean
    )

    private fun widget_layout_profile(mgr: AppWidgetManager, id: Int): WidgetLayoutProfile {
        val options = mgr.getAppWidgetOptions(id)
        val min_width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
        val min_height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)
        return when {
            min_height < 110 -> WidgetLayoutProfile(show_secondary_date = false, show_summary = false, show_optional_rows = false)
            min_height < 140 -> WidgetLayoutProfile(show_secondary_date = min_width >= 220, show_summary = true, show_optional_rows = false)
            else -> WidgetLayoutProfile(show_secondary_date = min_width >= 220, show_summary = true, show_optional_rows = true)
        }
    }

    private fun widget_colors(context: Context): WidgetColors {
        val theme = Prefs.get_theme(context)
        val palette = Prefs.get_palette(context)

        val sys_dark =
            (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
        val dark = when (theme) {
            Prefs.theme_dark -> true
            Prefs.theme_light -> false
            else -> sys_dark
        }

        val bg_res =
            when (palette) {
                Prefs.palette_dynamic ->
                    if (Build.VERSION.SDK_INT >= 31) {
                        if (dark) R.drawable.widget_bg_dynamic_dark else R.drawable.widget_bg_dynamic_light
                    } else {
                        if (dark) R.drawable.widget_bg_parchment_dark else R.drawable.widget_bg_parchment_light
                    }
                Prefs.palette_sapphire -> if (dark) R.drawable.widget_bg_sapphire_dark else R.drawable.widget_bg_sapphire_light
                Prefs.palette_rose -> if (dark) R.drawable.widget_bg_rose_dark else R.drawable.widget_bg_rose_light
                else -> if (dark) R.drawable.widget_bg_parchment_dark else R.drawable.widget_bg_parchment_light
            }

        val accent =
            when (palette) {
                Prefs.palette_dynamic ->
                    if (Build.VERSION.SDK_INT >= 31) {
                        ContextCompat.getColor(
                            context,
                            if (dark) android.R.color.system_accent1_200 else android.R.color.system_accent1_600
                        )
                    } else {
                        0xFF00695C.toInt()
                    }
                Prefs.palette_sapphire -> 0xFF1C5D99.toInt()
                Prefs.palette_rose -> 0xFF8A3A57.toInt()
                else -> 0xFF00695C.toInt()
            }

        val text_primary =
            if (palette == Prefs.palette_dynamic && Build.VERSION.SDK_INT >= 31) {
                ContextCompat.getColor(context, if (dark) android.R.color.system_neutral1_50 else android.R.color.system_neutral1_900)
            } else {
                if (dark) 0xFFF1EEE5.toInt() else 0xFF263238.toInt()
            }

        val text_muted =
            if (palette == Prefs.palette_dynamic && Build.VERSION.SDK_INT >= 31) {
                ContextCompat.getColor(context, if (dark) android.R.color.system_neutral2_200 else android.R.color.system_neutral2_700)
            } else {
                if (dark) 0xFFB6B0A4.toInt() else 0xFF6B6B6B.toInt()
            }
        return WidgetColors(
            bg_res = bg_res,
            accent = accent,
            text_primary = text_primary,
            text_muted = text_muted,
            prohibited_light = accent,
            prohibited_heavy = text_muted
        )
    }

    private fun day_start(at_millis: Long, tz: TimeZone): Long =
        Calendar.getInstance(tz).run {
            timeInMillis = at_millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            timeInMillis
        }

    private fun next_day_start(day_start: Long, tz: TimeZone): Long =
        Calendar.getInstance(tz).run {
            timeInMillis = day_start
            add(Calendar.DAY_OF_YEAR, 1)
            timeInMillis
        }

    private fun schedule_next(context: Context, now: Long, candidates: List<Long>) {
        val mgr = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val next = candidates.filter { it >= now }.minOrNull() ?: (now + 6 * 60 * 60 * 1000L)
        val when_ms = (next + 30_000L).coerceAtLeast(now + 60_000L)

        val intent = Intent(context, PrayerTimesWidgetProvider::class.java).apply {
            action = action_alarm
            putExtra(extra_alarm_token, alarm_token(context))
        }
        val pi = PendingIntent.getBroadcast(context, 2, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        mgr.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when_ms, pi)
    }

    private fun is_valid_alarm_intent(context: Context, intent: Intent): Boolean {
        val token = intent.getStringExtra(extra_alarm_token) ?: return false
        return token == alarm_token(context)
    }

    private fun alarm_token(context: Context): String {
        val prefs = context.getSharedPreferences("${context.packageName}_widget", Context.MODE_PRIVATE)
        val existing = prefs.getString(pref_alarm_token, null)
        if (!existing.isNullOrBlank()) return existing
        val created = UUID.randomUUID().toString()
        prefs.edit { putString(pref_alarm_token, created) }
        return created
    }

    companion object {
        val action_alarm = AppIds.action_widget_alarm
        private const val extra_alarm_token = "alarm_token"
        private const val pref_alarm_token = "widget_alarm_token"
    }
}
