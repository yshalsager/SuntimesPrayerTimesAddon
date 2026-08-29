package com.yshalsager.suntimes.prayertimesaddon.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.LocaleList
import android.text.format.DateFormat
import android.view.View
import android.widget.RemoteViews
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.ConfigurationCompat
import androidx.core.text.layoutDirection
import com.yshalsager.suntimes.prayertimesaddon.R
import com.yshalsager.suntimes.prayertimesaddon.core.AddonEvent
import com.yshalsager.suntimes.prayertimesaddon.core.AppClock
import com.yshalsager.suntimes.prayertimesaddon.core.AppIds
import com.yshalsager.suntimes.prayertimesaddon.core.HostConfigReader
import com.yshalsager.suntimes.prayertimesaddon.core.HostResolver
import com.yshalsager.suntimes.prayertimesaddon.core.ObligatoryPrayerWindowInput
import com.yshalsager.suntimes.prayertimesaddon.core.Prefs
import com.yshalsager.suntimes.prayertimesaddon.core.ReceiverWork
import com.yshalsager.suntimes.prayertimesaddon.core.calc_night
import com.yshalsager.suntimes.prayertimesaddon.core.format_method_summary
import com.yshalsager.suntimes.prayertimesaddon.core.hijri_for_day
import com.yshalsager.suntimes.prayertimesaddon.core.home_location_key
import com.yshalsager.suntimes.prayertimesaddon.core.query_addon_time
import com.yshalsager.suntimes.prayertimesaddon.core.format_gregorian_day_title
import com.yshalsager.suntimes.prayertimesaddon.core.resolve_location_query_context
import com.yshalsager.suntimes.prayertimesaddon.core.select_next_and_prev_obligatory_prayer
import com.yshalsager.suntimes.prayertimesaddon.ui.DaysActivity
import com.yshalsager.suntimes.prayertimesaddon.ui.MainActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import android.os.Build
import android.os.SystemClock

class PrayerTimesWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        enqueue_update(context)
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        enqueue_update(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        if (action == action_alarm && !is_valid_alarm_intent(context, intent)) return
        if (action == action_alarm || action == Intent.ACTION_TIME_CHANGED || action == Intent.ACTION_TIMEZONE_CHANGED || action == Intent.ACTION_DATE_CHANGED || action == Intent.ACTION_LOCALE_CHANGED) enqueue_update(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        WidgetPrefs.clear_saved_location_ids(context, appWidgetIds)
    }

    override fun onRestored(context: Context, oldWidgetIds: IntArray, newWidgetIds: IntArray) {
        val bindings = oldWidgetIds.zip(newWidgetIds).map { (old_id, new_id) -> new_id to WidgetPrefs.get_saved_location_id(context, old_id) }
        WidgetPrefs.clear_saved_location_ids(context, oldWidgetIds)
        bindings.forEach { (new_id, saved_location_id) -> WidgetPrefs.set_saved_location_id(context, new_id, saved_location_id) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val manager = AppWidgetManager.getInstance(context)
            newWidgetIds.forEach { id ->
                manager.updateAppWidgetOptions(
                    id,
                    Bundle().apply { putBoolean(AppWidgetManager.OPTION_APPWIDGET_RESTORE_COMPLETED, true) }
                )
            }
        }
    }

    private fun enqueue_update(context: Context) {
        val app_context = context.applicationContext
        ReceiverWork.submit(this, update_work) {
            val mgr = AppWidgetManager.getInstance(app_context)
            val ids = mgr.getAppWidgetIds(ComponentName(app_context, PrayerTimesWidgetProvider::class.java))
            if (ids.isNotEmpty()) update_all(app_context, mgr, ids)
        }
    }

    private fun update_all(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        val text_context = app_localized_context(context)

        fun scoped_intent(id: Int, target: Class<*>, location_key: String) =
            Intent(context, target)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                .putExtra(MainActivity.extra_location_scope, location_key)

        fun show_unavailable(id: Int, message: String, root_intent: Intent, header_intent: Intent = root_intent) {
            val rv = RemoteViews(context.packageName, R.layout.widget_prayer_times)
            rv.setTextViewText(R.id.widget_hijri, message)
            rv.setTextViewText(R.id.widget_gregorian, "")
            set_static_summary(rv, R.id.widget_summary, "")
            rv.setViewVisibility(R.id.widget_prayer_row, View.GONE)
            rv.setViewVisibility(R.id.widget_prohibited_row, View.GONE)
            rv.setViewVisibility(R.id.widget_night_row, View.GONE)
            val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            rv.setOnClickPendingIntent(R.id.widget_root, PendingIntent.getActivity(context, id, root_intent, flags))
            rv.setOnClickPendingIntent(R.id.widget_header, PendingIntent.getActivity(context, id, header_intent, flags))
            mgr.updateAppWidget(id, rv)
        }

        val location_contexts = ids.associateWith { resolve_location_query_context(context, WidgetPrefs.get_saved_location_id(context, it), null, null, null) }
        fun show_scoped_unavailable(id: Int, message: String) {
            val location_key = home_location_key(location_contexts.getValue(id).resolved_saved_location_id)
            show_unavailable(
                id,
                message,
                scoped_intent(id, MainActivity::class.java, location_key),
                scoped_intent(id, DaysActivity::class.java, location_key)
            )
        }
        location_contexts.filterValues { it.saved_location_missing }.forEach { (id, _) ->
            show_unavailable(
                id,
                text_context.getString(R.string.saved_location_missing_widget),
                Intent(context, PrayerTimesWidgetConfigureActivity::class.java)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            )
        }
        val active_ids = ids.filter { !location_contexts.getValue(it).saved_location_missing }
        if (active_ids.isEmpty()) return

        val host = HostResolver.ensure_default_selected(context)
        if (host == null) {
            active_ids.forEach { show_scoped_unavailable(it, text_context.getString(R.string.no_host_found)) }
            schedule_next(context, AppClock.now_millis(), emptyList())
            return
        }

        val required_permission = HostResolver.get_required_permission(context, host)
        if (required_permission != null && ContextCompat.checkSelfPermission(context, required_permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            active_ids.forEach { show_scoped_unavailable(it, text_context.getString(R.string.widget_host_permission_missing)) }
            schedule_next(context, AppClock.now_millis(), emptyList())
            return
        }

        val cfg = HostConfigReader.read_config(context, host)
        val host_tz = cfg?.timezone?.let(TimeZone::getTimeZone) ?: TimeZone.getDefault()
        val host_location_label = cfg?.display_label() ?: text_context.getString(R.string.unknown_location)
        val now = AppClock.now_millis()

        val month_basis = Prefs.get_days_month_basis(context)
        val show_hijri = Prefs.get_days_show_hijri(context) || month_basis == Prefs.days_month_basis_hijri
        val locale = ConfigurationCompat.getLocales(text_context.resources.configuration).get(0) ?: Locale.getDefault()
        val row_dir = locale.layoutDirection

        val widget_show_prohibited = Prefs.get_widget_show_prohibited(context)
        val widget_show_night = Prefs.get_widget_show_night_portions(context)
        val colors = widget_colors(context)
        val all_candidates = ArrayList<Long>()

        active_ids.forEach { id ->
            val location_context = location_contexts.getValue(id)
            val scoped_saved_location_id = location_context.resolved_saved_location_id
            val tz = location_context.timezone_override ?: host_tz
            val day_start = day_start(now, tz)
            val time_format = DateFormat.getTimeFormat(text_context).apply { timeZone = tz }
            val time_only_format =
                SimpleDateFormat(if (DateFormat.is24HourFormat(text_context)) "HH:mm" else "h:mm", locale).apply { timeZone = tz }

            fun time_short(v: Long?): String = v?.let { time_only_format.format(Date(it)) } ?: "--"
            fun time_str(v: Long?): String = v?.let { time_format.format(Date(it)) } ?: "--"
            fun range(a: Long?, b: Long?): String = "${time_short(a)}-${time_short(b)}"

            val fajr = query_addon_time(context, AddonEvent.prayer_fajr, day_start, saved_location_id = scoped_saved_location_id)
            val duha = query_addon_time(context, AddonEvent.prayer_duha, day_start, saved_location_id = scoped_saved_location_id)
            val dhuhr = query_addon_time(context, AddonEvent.prayer_dhuhr, day_start, saved_location_id = scoped_saved_location_id)
            val asr = query_addon_time(context, AddonEvent.prayer_asr, day_start, saved_location_id = scoped_saved_location_id)
            val maghrib = query_addon_time(context, AddonEvent.prayer_maghrib, day_start, saved_location_id = scoped_saved_location_id)
            val isha = query_addon_time(context, AddonEvent.prayer_isha, day_start, saved_location_id = scoped_saved_location_id)

            if (listOf(fajr, dhuhr, asr, maghrib, isha).all { it == null }) {
                show_scoped_unavailable(id, text_context.getString(R.string.widget_host_unavailable))
                return@forEach
            }

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
            val greg = format_gregorian_day_title(text_context, day_start, tz, locale)
            val location_label = location_context.saved_location?.display_label() ?: host_location_label
            val method_summary = format_method_summary(text_context, location_context.method_config_override)

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

            val yesterday_start = prev_day_start(day_start, tz)
            val tomorrow_start = next_day_start(day_start, tz)
            val isha_yesterday = query_addon_time(context, AddonEvent.prayer_isha, yesterday_start, saved_location_id = scoped_saved_location_id)
            val fajr_tomorrow = query_addon_time(context, AddonEvent.prayer_fajr, tomorrow_start, saved_location_id = scoped_saved_location_id)
            val obligatory_selection =
                select_next_and_prev_obligatory_prayer(
                    now = now,
                    input =
                        ObligatoryPrayerWindowInput(
                            fajr = fajr,
                            dhuhr = dhuhr,
                            asr = asr,
                            maghrib = maghrib,
                            isha = isha,
                            prev_day_isha = isha_yesterday,
                            next_day_fajr = fajr_tomorrow
                        )
                )
            val next_obligatory_prayer = obligatory_selection.next
            val next_obligatory_time = next_obligatory_prayer?.second
            val summary =
                next_obligatory_prayer?.let { (event, time) ->
                    val next_obligatory_label = obligatory_prayer_label(text_context, event, is_friday)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        WidgetSummary.Countdown(
                            base = SystemClock.elapsedRealtime() + (time - now).coerceAtLeast(0L),
                            format = "$next_obligatory_label ${text_context.getString(R.string.in_countdown, "%s")} · $location_label · $method_summary"
                        )
                    } else {
                        val countdown = text_context.getString(R.string.in_countdown, format_countdown(time - now, text_context))
                        WidgetSummary.Static("$next_obligatory_label $countdown \u00b7 $location_label \u00b7 $method_summary")
                    }
                } ?: WidgetSummary.Static("$location_label \u00b7 $method_summary")
            val night = if (!widget_show_night) null else calc_night(maghrib, fajr_tomorrow)
            val night_times =
                if (!widget_show_night) emptyList()
                else listOfNotNull(night?.first_third, night?.midpoint, night?.last_third, night?.last_sixth)
            val night_ok = widget_show_night && night_times.size == 4
            val night_first_third = night_times.getOrNull(0)
            val night_midpoint = night_times.getOrNull(1)
            val night_last_third = night_times.getOrNull(2)
            val night_last_sixth = night_times.getOrNull(3)

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
            when (summary) {
                is WidgetSummary.Countdown -> {
                    rv.setChronometer(R.id.widget_summary, summary.base, summary.format, layout_profile.show_summary)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        rv.setChronometerCountDown(R.id.widget_summary, true)
                    }
                    rv.setTextColor(R.id.widget_summary, colors.accent)
                }

                is WidgetSummary.Static -> {
                    set_static_summary(rv, R.id.widget_summary, summary.text)
                    rv.setTextColor(R.id.widget_summary, colors.text_muted)
                }
            }
            rv.setViewVisibility(R.id.widget_summary, if (layout_profile.show_summary) View.VISIBLE else View.GONE)

            rv.setTextColor(R.id.widget_hijri, colors.text_primary)
            rv.setTextColor(R.id.widget_gregorian, colors.text_muted)

            // Keep prayer/prohibited/night column ordering consistent in RTL:
            // first column (Fajr/Dawn/First third) stays on the Start side.
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
            rv.setTextColor(R.id.widget_prayer_duha, colors.text_primary)
            rv.setTextColor(R.id.widget_prayer_dhuhr, colors.text_primary)
            rv.setTextColor(R.id.widget_prayer_asr, colors.text_primary)
            rv.setTextColor(R.id.widget_prayer_maghrib, colors.text_primary)
            rv.setTextColor(R.id.widget_prayer_isha, colors.text_primary)

            rv.setTextViewText(R.id.widget_label_fajr, text_context.getString(R.string.event_prayer_fajr))
            rv.setTextViewText(R.id.widget_label_duha, text_context.getString(R.string.event_prayer_duha))
            rv.setTextViewText(
                R.id.widget_label_dhuhr,
                if (is_friday) text_context.getString(R.string.event_prayer_jummah) else text_context.getString(R.string.event_prayer_dhuhr)
            )
            rv.setTextViewText(R.id.widget_label_asr, text_context.getString(R.string.event_prayer_asr))
            rv.setTextViewText(R.id.widget_label_maghrib, text_context.getString(R.string.event_prayer_maghrib))
            rv.setTextViewText(R.id.widget_label_isha, text_context.getString(R.string.event_prayer_isha))

            listOf(
                R.id.widget_label_fajr,
                R.id.widget_label_duha,
                R.id.widget_label_dhuhr,
                R.id.widget_label_asr,
                R.id.widget_label_maghrib,
                R.id.widget_label_isha
            ).forEach { rv.setTextColor(it, colors.text_muted) }

            next_obligatory_prayer?.let { (event, _) ->
                listOf(
                    AddonEvent.prayer_fajr to (R.id.widget_label_fajr to R.id.widget_prayer_fajr),
                    AddonEvent.prayer_dhuhr to (R.id.widget_label_dhuhr to R.id.widget_prayer_dhuhr),
                    AddonEvent.prayer_asr to (R.id.widget_label_asr to R.id.widget_prayer_asr),
                    AddonEvent.prayer_maghrib to (R.id.widget_label_maghrib to R.id.widget_prayer_maghrib),
                    AddonEvent.prayer_isha to (R.id.widget_label_isha to R.id.widget_prayer_isha)
                ).firstOrNull { it.first == event }?.second?.let { (label_id, time_id) ->
                    rv.setTextColor(label_id, colors.accent)
                    rv.setTextColor(time_id, colors.accent)
                }
            }

            val prohibited_ok =
                layout_profile.show_optional_rows &&
                    widget_show_prohibited &&
                    listOf(prohibited_dawn, prohibited_sunrise, prohibited_zawal, prohibited_after_asr, prohibited_sunset).any { it != null }
            rv.setViewVisibility(R.id.widget_prohibited_row, if (prohibited_ok) View.VISIBLE else View.GONE)
            if (prohibited_ok) {
                fun labeled(label_res: Int, v: String?): String = "${text_context.getString(label_res)}\n${v ?: "--"}"

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
                fun labeled(label_res: Int, v: Long?): String = "${text_context.getString(label_res)}\n${time_short(v)}"

                rv.setTextViewText(R.id.widget_night_first_third, labeled(R.string.night_first_third, night_first_third))
                rv.setTextViewText(R.id.widget_night_midpoint, labeled(R.string.night_midpoint, night_midpoint))
                rv.setTextViewText(R.id.widget_night_last_third, labeled(R.string.night_last_third, night_last_third))
                rv.setTextViewText(R.id.widget_night_last_sixth, labeled(R.string.night_last_sixth, night_last_sixth))

                rv.setTextColor(R.id.widget_night_first_third, colors.accent)
                rv.setTextColor(R.id.widget_night_midpoint, colors.accent)
                rv.setTextColor(R.id.widget_night_last_third, colors.accent)
                rv.setTextColor(R.id.widget_night_last_sixth, colors.accent)
            }

            val location_key = home_location_key(scoped_saved_location_id)
            val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            val open_main = PendingIntent.getActivity(context, id, scoped_intent(id, MainActivity::class.java, location_key), flags)
            rv.setOnClickPendingIntent(R.id.widget_root, open_main)

            val open_days = PendingIntent.getActivity(context, id, scoped_intent(id, DaysActivity::class.java, location_key), flags)
            rv.setOnClickPendingIntent(R.id.widget_header, open_days)

            mgr.updateAppWidget(id, rv)

            next_obligatory_time?.let {
                all_candidates += it
            }
            all_candidates += listOfNotNull(fajr, duha, dhuhr, asr, maghrib, isha)
            all_candidates += listOfNotNull(sunrise, sunrise_end, zawal_start, dhuhr, sunset_start, sunset)
            if (night_ok) all_candidates += night_times
            all_candidates += (tomorrow_start + 120_000L)
        }
        schedule_next(context, now, all_candidates)
    }

    private fun app_localized_context(context: Context): Context {
        val app_locales = AppCompatDelegate.getApplicationLocales()
        if (app_locales.isEmpty) return context

        val cfg = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            cfg.setLocales(LocaleList.forLanguageTags(app_locales.toLanguageTags()))
        } else {
            cfg.setLocale(app_locales[0] ?: Locale.getDefault())
        }
        return context.createConfigurationContext(cfg)
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

    private fun prev_day_start(day_start: Long, tz: TimeZone): Long =
        Calendar.getInstance(tz).run {
            timeInMillis = day_start
            add(Calendar.DAY_OF_YEAR, -1)
            timeInMillis
        }

    private fun obligatory_prayer_label(context: Context, event: AddonEvent, is_friday: Boolean): String =
        if (event == AddonEvent.prayer_dhuhr && is_friday) context.getString(R.string.event_prayer_jummah)
        else context.getString(event.title_res)

    private fun set_static_summary(rv: RemoteViews, view_id: Int, text: CharSequence) {
        rv.setChronometer(view_id, 0L, null, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            rv.setChronometerCountDown(view_id, false)
        }
        rv.setTextViewText(view_id, text)
    }

    private fun format_countdown(delta_ms: Long, context: Context): String {
        val mins = (delta_ms.coerceAtLeast(0L) + 30_000L) / 60_000L
        val h = mins / 60
        val m = mins % 60
        return if (h > 0) String.format(Locale.getDefault(), "%d:%02d", h, m)
        else String.format(Locale.getDefault(), "%d%s", m, context.getString(R.string.minute_abbrev))
    }

    private fun schedule_next(context: Context, now: Long, candidates: List<Long>) {
        val mgr = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val next = candidates.filter { it >= now }.minOrNull() ?: (now + 6 * 60 * 60 * 1000L)
        val when_ms = (next + 5_000L).coerceAtLeast(now + 5_000L)

        val intent = Intent(context, PrayerTimesWidgetProvider::class.java).apply {
            action = action_alarm
            putExtra(extra_alarm_token, WidgetPrefs.alarm_token(context))
        }
        val pi = PendingIntent.getBroadcast(context, 2, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        mgr.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when_ms, pi)
    }

    private fun is_valid_alarm_intent(context: Context, intent: Intent): Boolean {
        val token = intent.getStringExtra(extra_alarm_token) ?: return false
        return token == WidgetPrefs.alarm_token(context)
    }

    private sealed interface WidgetSummary {
        data class Countdown(val base: Long, val format: String) : WidgetSummary
        data class Static(val text: String) : WidgetSummary
    }

    companion object {
        val action_alarm = AppIds.action_widget_alarm
        private const val extra_alarm_token = "alarm_token"
        private val update_work = ReceiverWork.State()
    }
}
