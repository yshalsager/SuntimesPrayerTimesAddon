package com.yshalsager.suntimes.prayertimesaddon.core

import android.content.Context
import androidx.core.content.ContextCompat
import com.yshalsager.suntimes.prayertimesaddon.R
import java.util.Calendar
import java.util.TimeZone

data class PrayerTimesCalendarMeta(
    val host: String,
    val tz: TimeZone,
    val summary: String,
    val event_location: String,
    val color: Int
)

data class PrayerTimesCalendarEvent(
    val title: String,
    val description: String,
    val event_timezone: String,
    val dtstart: Long,
    val dtend: Long,
    val event_location: String
)

private data class PrayerTimesCalendarDay(
    val is_friday: Boolean,
    val fajr: Long?,
    val fajr_extra_1: Long?,
    val duha: Long?,
    val eid_start: Long?,
    val eid_end: Long?,
    val dhuhr: Long?,
    val asr: Long?,
    val maghrib: Long?,
    val isha: Long?,
    val isha_extra_1: Long?,
    val sunrise: Long?,
    val sunrise_end: Long?,
    val zawal_start: Long?,
    val sunset_start: Long?,
    val sunset: Long?,
    val night_midpoint: Long?,
    val night_last_third: Long?,
    val night_last_sixth: Long?
)

fun resolve_prayer_times_calendar_meta(context: Context, source: PrayerTimesCalendarSource): PrayerTimesCalendarMeta? {
    val host = HostResolver.ensure_default_selected(context) ?: return null
    val config = HostConfigReader.read_config(context, host) ?: return null
    val tz = config.timezone?.let(TimeZone::getTimeZone) ?: TimeZone.getDefault()
    val location = config.display_label().orEmpty()
    val summary_location = location.ifBlank { context.getString(R.string.unknown_location) }
    val summary = "$summary_location · ${format_method_summary(context)}"
    return PrayerTimesCalendarMeta(
        host = host,
        tz = tz,
        summary = summary,
        event_location = location,
        color = ContextCompat.getColor(context, source.color_res)
    )
}

fun query_prayer_times_calendar_events(
    context: Context,
    source: PrayerTimesCalendarSource,
    window_start: Long,
    window_end: Long
): List<PrayerTimesCalendarEvent> {
    if (window_end <= window_start) return emptyList()
    val meta = resolve_prayer_times_calendar_meta(context, source) ?: return emptyList()
    val first_day =
        if (source == PrayerTimesCalendarSource.night) add_days(day_start_at(window_start, meta.tz), -1, meta.tz)
        else day_start_at(window_start, meta.tz)
    val last_anchor = if (window_end > window_start) window_end - 1 else window_start
    val last_day = day_start_at(last_anchor, meta.tz)

    val out = ArrayList<PrayerTimesCalendarEvent>()
    var day_start = first_day
    while (day_start <= last_day) {
        out.addAll(day_events_for_source(context, meta, day_start, source))
        day_start = add_days(day_start, 1, meta.tz)
    }

    return out.filter { overlaps(it, window_start, window_end) }
}

private fun day_start_at(at_millis: Long, tz: TimeZone): Long =
    Calendar.getInstance(tz).run {
        timeInMillis = at_millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        timeInMillis
    }

private fun overlaps(event: PrayerTimesCalendarEvent, window_start: Long, window_end: Long): Boolean {
    if (event.dtstart == event.dtend) return event.dtstart in window_start until window_end
    return event.dtstart < window_end && event.dtend > window_start
}

private fun point_event(meta: PrayerTimesCalendarMeta, title: String, at: Long): PrayerTimesCalendarEvent =
    PrayerTimesCalendarEvent(
        title = title,
        description = meta.summary,
        event_timezone = meta.tz.id,
        dtstart = at,
        dtend = at,
        event_location = meta.event_location
    )

private fun range_event(meta: PrayerTimesCalendarMeta, title: String, start: Long, end: Long): PrayerTimesCalendarEvent =
    PrayerTimesCalendarEvent(
        title = title,
        description = meta.summary,
        event_timezone = meta.tz.id,
        dtstart = start,
        dtend = end,
        event_location = meta.event_location
    )

private fun day_events_for_source(
    context: Context,
    meta: PrayerTimesCalendarMeta,
    day_start: Long,
    source: PrayerTimesCalendarSource
): List<PrayerTimesCalendarEvent> {
    val day = build_calendar_day(context, meta.host, meta.tz, day_start)
    return when (source) {
        PrayerTimesCalendarSource.prayers ->
            buildList {
                day.fajr?.let { add(point_event(meta, addon_event_title(context, AddonEvent.prayer_fajr), it)) }
                day.fajr_extra_1?.let { add(point_event(meta, addon_event_title(context, AddonEvent.prayer_fajr_extra_1), it)) }
                day.duha?.let { add(point_event(meta, addon_event_title(context, AddonEvent.prayer_duha), it)) }
                day.eid_start?.let { add(point_event(meta, addon_event_title(context, AddonEvent.prayer_eid_start), it)) }
                day.eid_end?.let { add(point_event(meta, addon_event_title(context, AddonEvent.prayer_eid_end), it)) }
                day.dhuhr?.let {
                    val title = if (day.is_friday) context.getString(R.string.event_prayer_jummah) else context.getString(R.string.event_prayer_dhuhr)
                    add(point_event(meta, title, it))
                }
                day.asr?.let { add(point_event(meta, addon_event_title(context, AddonEvent.prayer_asr), it)) }
                day.maghrib?.let { add(point_event(meta, addon_event_title(context, AddonEvent.prayer_maghrib), it)) }
                day.isha?.let { add(point_event(meta, addon_event_title(context, AddonEvent.prayer_isha), it)) }
                day.isha_extra_1?.let { add(point_event(meta, addon_event_title(context, AddonEvent.prayer_isha_extra_1), it)) }
            }

        PrayerTimesCalendarSource.makruh ->
            buildList {
                if (day.fajr != null && day.sunrise != null) add(range_event(meta, context.getString(R.string.prohibited_dawn), day.fajr, day.sunrise))
                if (day.sunrise != null && day.sunrise_end != null) add(range_event(meta, context.getString(R.string.prohibited_sunrise), day.sunrise, day.sunrise_end))
                if (day.zawal_start != null && day.dhuhr != null) add(range_event(meta, context.getString(R.string.prohibited_zawal), day.zawal_start, day.dhuhr))
                if (day.asr != null && day.sunset_start != null) add(range_event(meta, context.getString(R.string.prohibited_after_asr), day.asr, day.sunset_start))
                if (day.sunset_start != null && day.sunset != null) add(range_event(meta, context.getString(R.string.prohibited_sunset), day.sunset_start, day.sunset))
            }

        PrayerTimesCalendarSource.night ->
            buildList {
                day.night_midpoint?.let { add(point_event(meta, context.getString(R.string.night_midpoint), it)) }
                day.night_last_third?.let { add(point_event(meta, context.getString(R.string.night_last_third), it)) }
                day.night_last_sixth?.let { add(point_event(meta, context.getString(R.string.night_last_sixth), it)) }
            }
    }
}

private fun build_calendar_day(context: Context, host: String, tz: TimeZone, day_start: Long): PrayerTimesCalendarDay {
    val is_friday =
        Calendar.getInstance(tz).run {
            timeInMillis = day_start
            get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
        }

    val sun = query_host_sun(context, host, day_start)
    val maghrib_offset_ms = Prefs.get_maghrib_offset_minutes(context) * 60_000L

    fun q(event: AddonEvent) = query_host_addon_time(context, host, event, day_start)

    val fajr = q(AddonEvent.prayer_fajr)
    val fajr_extra_1 = q(AddonEvent.prayer_fajr_extra_1)
    val duha = q(AddonEvent.prayer_duha)
    val eid_start = q(AddonEvent.prayer_eid_start)
    val eid_end = q(AddonEvent.prayer_eid_end)
    val dhuhr = sun?.noon ?: q(AddonEvent.prayer_dhuhr)
    val asr = q(AddonEvent.prayer_asr)
    val maghrib = sun?.sunset?.plus(maghrib_offset_ms) ?: q(AddonEvent.prayer_maghrib)
    val isha = q(AddonEvent.prayer_isha)
    val isha_extra_1 = q(AddonEvent.prayer_isha_extra_1)

    val sunrise = sun?.sunrise ?: q(AddonEvent.makruh_sunrise_start)
    val sunrise_end = q(AddonEvent.makruh_sunrise_end)
    val zawal_start = if (sun?.noon != null) sun.noon - Prefs.get_zawal_minutes(context) * 60_000L else q(AddonEvent.makruh_zawal_start)
    val sunset_start = q(AddonEvent.makruh_sunset_start)
    val sunset = sun?.sunset ?: q(AddonEvent.makruh_sunset_end)

    val fajr_next = query_host_addon_time(context, host, AddonEvent.prayer_fajr, add_days(day_start, 1, tz))
    val night = calc_night(maghrib, fajr_next)

    return PrayerTimesCalendarDay(
        is_friday = is_friday,
        fajr = fajr,
        fajr_extra_1 = fajr_extra_1,
        duha = duha,
        eid_start = eid_start,
        eid_end = eid_end,
        dhuhr = dhuhr,
        asr = asr,
        maghrib = maghrib,
        isha = isha,
        isha_extra_1 = isha_extra_1,
        sunrise = sunrise,
        sunrise_end = sunrise_end,
        zawal_start = zawal_start,
        sunset_start = sunset_start,
        sunset = sunset,
        night_midpoint = night?.midpoint,
        night_last_third = night?.last_third,
        night_last_sixth = night?.last_sixth
    )
}
