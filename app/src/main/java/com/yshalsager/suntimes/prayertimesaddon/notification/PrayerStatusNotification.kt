package com.yshalsager.suntimes.prayertimesaddon.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import android.text.format.DateFormat
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.yshalsager.suntimes.prayertimesaddon.R
import com.yshalsager.suntimes.prayertimesaddon.core.AddonEvent
import com.yshalsager.suntimes.prayertimesaddon.core.AppClock
import com.yshalsager.suntimes.prayertimesaddon.core.AppIds
import com.yshalsager.suntimes.prayertimesaddon.core.HostConfigReader
import com.yshalsager.suntimes.prayertimesaddon.core.HostResolver
import com.yshalsager.suntimes.prayertimesaddon.core.ObligatoryPrayerWindowInput
import com.yshalsager.suntimes.prayertimesaddon.core.Prefs
import com.yshalsager.suntimes.prayertimesaddon.core.addon_event_title
import com.yshalsager.suntimes.prayertimesaddon.core.day_start_at
import com.yshalsager.suntimes.prayertimesaddon.core.format_method_summary
import com.yshalsager.suntimes.prayertimesaddon.core.query_host_addon_time
import com.yshalsager.suntimes.prayertimesaddon.core.query_inputs
import com.yshalsager.suntimes.prayertimesaddon.core.resolve_selected_home_location
import com.yshalsager.suntimes.prayertimesaddon.core.select_next_and_prev_obligatory_prayer
import com.yshalsager.suntimes.prayertimesaddon.core.valid_timezone_id
import com.yshalsager.suntimes.prayertimesaddon.ui.MainActivity
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object PrayerStatusNotification {
    private const val channel_id = "prayer_status"
    private const val notification_id = 1001
    private const val request_open = 3001
    private const val request_refresh = 3002
    private const val request_disable = 3003
    private const val request_alarm = 3004

    fun set_enabled(context: Context, enabled: Boolean) {
        Prefs.set_prayer_status_notification_enabled(context, enabled)
        if (enabled) refresh(context) else cancel(context)
    }

    fun refresh(context: Context) {
        if (!Prefs.get_prayer_status_notification_enabled(context)) {
            cancel(context)
            return
        }
        if (!has_notification_permission(context)) {
            cancel(context)
            return
        }

        val text_context = app_localized_context(context)
        val data = build_status_data(text_context) ?: run {
            cancel(context)
            return
        }
        ensure_channel(text_context)
        notify(text_context, build_notification(text_context, data))
        schedule_next(text_context, data.next_refresh_millis)
    }

    fun cancel(context: Context) {
        notification_manager(context).cancel(notification_id)
        cancel_next(context)
    }

    fun has_notification_permission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    internal fun build_status_data(context: Context): PrayerStatusData? {
        val host = HostResolver.ensure_default_selected(context) ?: return null
        val required_perm = HostResolver.get_required_permission(context, host)
        if (required_perm != null && ContextCompat.checkSelfPermission(context, required_perm) != PackageManager.PERMISSION_GRANTED) return null

        val host_config = HostConfigReader.read_config(context, host)
        val host_label = host_config?.display_label() ?: context.getString(R.string.unknown_location)
        val host_timezone_id = valid_timezone_id(host_config?.timezone) ?: TimeZone.getDefault().id
        val selected_location = resolve_selected_home_location(context, host_label, host_timezone_id)
        val tz = selected_location.timezone
        val now = AppClock.now_millis()
        val today_start = day_start_at(now, tz)
        val tomorrow_start = add_days(today_start, 1, tz)
        val yesterday_start = add_days(today_start, -1, tz)

        fun q(event: AddonEvent, alarm_now: Long = today_start): Long? {
            val inputs = selected_location.query_inputs(alarm_now)
            return query_host_addon_time(
                context = context,
                host_event_authority = host,
                event = event,
                alarm_now = alarm_now,
                selection = inputs.selection,
                selection_args = inputs.selection_args,
                timezone_override = inputs.timezone_override,
                latitude_override = inputs.latitude_override,
                method_config_override = inputs.method_config_override,
                addon_runtime_profile_override = inputs.addon_runtime_profile_override
            )
        }

        val fajr = q(AddonEvent.prayer_fajr)
        val dhuhr = q(AddonEvent.prayer_dhuhr)
        val asr = q(AddonEvent.prayer_asr)
        val maghrib = q(AddonEvent.prayer_maghrib)
        val isha = q(AddonEvent.prayer_isha)
        val fajr_tomorrow = q(AddonEvent.prayer_fajr, tomorrow_start)
        val selection = select_next_and_prev_obligatory_prayer(
            now = now,
            input = ObligatoryPrayerWindowInput(
                fajr = fajr,
                dhuhr = dhuhr,
                asr = asr,
                maghrib = maghrib,
                isha = isha,
                prev_day_isha = q(AddonEvent.prayer_isha, yesterday_start),
                next_day_fajr = fajr_tomorrow
            )
        )
        val next = selection.next ?: return null
        val day_events = listOf(
            AddonEvent.prayer_fajr to fajr,
            AddonEvent.prayer_dhuhr to dhuhr,
            AddonEvent.prayer_asr to asr,
            AddonEvent.prayer_maghrib to maghrib,
            AddonEvent.prayer_isha to isha
        ).mapNotNull { (event, time) -> time?.let { event to it } }

        return PrayerStatusData(
            next_event = next.first,
            next_time_millis = next.second,
            location_label = selected_location.label,
            method_summary = format_method_summary(context, selected_location.method_config_override),
            timezone = tz,
            day_events = day_events,
            next_refresh_millis = next.second + 30_000L
        )
    }

    private fun build_notification(context: Context, data: PrayerStatusData): Notification {
        val next_label = prayer_label(context, data.next_event, data.next_time_millis, data.timezone)
        val times = DateFormat.getTimeFormat(context).apply { timeZone = data.timezone }
        val relative = context.getString(R.string.in_countdown, format_countdown(data.next_time_millis - AppClock.now_millis(), context))
        val content = "$relative · ${times.format(Date(data.next_time_millis))} · ${data.location_label} · ${data.method_summary}"
        val expanded = data.day_events.joinToString(" · ") { (event, time) ->
            "${prayer_label(context, event, time, data.timezone)} ${times.format(Date(time))}"
        }

        val builder = NotificationCompat.Builder(context, channel_id)
            .setSmallIcon(R.drawable.ic_prayer_sun)
            .setContentTitle(context.getString(R.string.prayer_status_notification_title, next_label))
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expanded))
            .setContentIntent(open_app_intent(context))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(true)
            .setWhen(data.next_time_millis)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .addAction(R.drawable.ic_prayer_sun, context.getString(R.string.prayer_status_refresh_action), refresh_intent(context))
            .addAction(R.drawable.ic_settings, context.getString(R.string.prayer_status_disable_action), disable_intent(context))
        return builder.build()
    }

    private fun prayer_label(context: Context, event: AddonEvent, time: Long, tz: TimeZone): String {
        if (event != AddonEvent.prayer_dhuhr) return addon_event_title(context, event)
        val is_friday = Calendar.getInstance(tz).run {
            timeInMillis = time
            get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
        }
        return if (is_friday) context.getString(R.string.event_prayer_jummah) else context.getString(R.string.event_prayer_dhuhr)
    }

    private fun format_countdown(delta_ms: Long, context: Context): String {
        val mins = (delta_ms.coerceAtLeast(0L) + 30_000L) / 60_000L
        val h = mins / 60
        val m = mins % 60
        return if (h > 0) String.format(Locale.getDefault(), "%d:%02d", h, m)
        else String.format(Locale.getDefault(), "%d%s", m, context.getString(R.string.minute_abbrev))
    }

    private fun ensure_channel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(channel_id, context.getString(R.string.prayer_status_channel_name), NotificationManager.IMPORTANCE_LOW).apply {
            description = context.getString(R.string.prayer_status_channel_description)
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }
        manager.createNotificationChannel(channel)
    }

    @SuppressLint("MissingPermission")
    private fun notify(context: Context, notification: Notification) {
        notification_manager(context).notify(notification_id, notification)
    }

    private fun notification_manager(context: Context): NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun open_app_intent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context,
            request_open,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    private fun refresh_intent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            request_refresh,
            Intent(context, PrayerStatusNotificationReceiver::class.java).setAction(AppIds.action_prayer_status_refresh),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    private fun disable_intent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            request_disable,
            Intent(context, PrayerStatusNotificationReceiver::class.java).setAction(AppIds.action_prayer_status_disable),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    private fun schedule_next(context: Context, when_millis: Long) {
        val alarm_manager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val now = AppClock.now_millis()
        val trigger = when_millis.coerceAtLeast(now + 60_000L)
        alarm_manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, alarm_intent(context))
    }

    private fun cancel_next(context: Context) {
        val alarm_manager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        alarm_manager.cancel(alarm_intent(context))
    }

    private fun alarm_intent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            request_alarm,
            Intent(context, PrayerStatusNotificationReceiver::class.java).setAction(AppIds.action_prayer_status_refresh),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    private fun add_days(day_start: Long, days: Int, tz: TimeZone): Long =
        Calendar.getInstance(tz).run {
            timeInMillis = day_start
            add(Calendar.DAY_OF_YEAR, days)
            timeInMillis
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
}

internal data class PrayerStatusData(
    val next_event: AddonEvent,
    val next_time_millis: Long,
    val location_label: String,
    val method_summary: String,
    val timezone: TimeZone,
    val day_events: List<Pair<AddonEvent, Long>>,
    val next_refresh_millis: Long
)

class PrayerStatusNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            AppIds.action_prayer_status_disable -> PrayerStatusNotification.set_enabled(context, false)
            AppIds.action_prayer_status_refresh -> PrayerStatusNotification.refresh(context)
        }
    }
}

class PrayerStatusSystemReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_LOCALE_CHANGED -> PrayerStatusNotification.refresh(context)
        }
    }
}
