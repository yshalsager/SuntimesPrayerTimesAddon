package com.yshalsager.suntimes.prayertimesaddon.core

import android.content.Context
import androidx.core.net.toUri
import com.yshalsager.suntimes.prayertimesaddon.provider.PrayerTimesProvider
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

data class NightPortions(val midpoint: Long, val last_third: Long, val last_sixth: Long)

data class SunTimes(val noon: Long?, val sunrise: Long?, val sunset: Long?)

private const val event_calc_selection =
    AlarmEventContract.extra_alarm_now + "=? AND " +
        AlarmEventContract.extra_alarm_offset + "=? AND " +
        AlarmEventContract.extra_alarm_repeat + "=? AND " +
        AlarmEventContract.extra_alarm_repeat_days + "=?"

private fun event_calc_args(alarm_now: Long): Array<String> =
    arrayOf(alarm_now.toString(), "0", "false", "[]")

private fun day_start_at(at_millis: Long, tz: TimeZone): Long =
    Calendar.getInstance(tz).run {
        timeInMillis = at_millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        timeInMillis
    }

private fun is_eid_day(context: Context, host_event_authority: String, alarm_now: Long): Boolean {
    val tz =
        HostConfigReader.read_config(context, host_event_authority)?.timezone?.let(TimeZone::getTimeZone)
            ?: TimeZone.getDefault()
    val day_start = day_start_at(alarm_now, tz)
    val hijri =
        try {
            hijri_for_day(
                day_start_millis = day_start,
                tz = tz,
                locale = Locale.getDefault(),
                variant = Prefs.get_hijri_variant(context),
                offset_days = Prefs.get_hijri_day_offset(context)
            )
        } catch (_: ArithmeticException) {
            return false
        }
    return (hijri.month == 10 && hijri.day == 1) || (hijri.month == 12 && hijri.day == 10)
}

fun query_host_eid_time(
    context: Context,
    host_event_authority: String,
    event: AddonEvent,
    alarm_now: Long,
    selection: String?,
    selection_args: Array<String>?
): Long? {
    if (event != AddonEvent.prayer_eid_start && event != AddonEvent.prayer_eid_end) return null
    if (!is_eid_day(context, host_event_authority, alarm_now)) return null

    val tz =
        HostConfigReader.read_config(context, host_event_authority)?.timezone?.let(TimeZone::getTimeZone)
            ?: TimeZone.getDefault()
    val day_start = day_start_at(alarm_now, tz)
    val eid_selection_args =
        selection_args?.clone()?.also { args ->
            if (args.isNotEmpty()) args[0] = day_start.toString()
        } ?: selection_args
    val sun = query_host_sun(context, host_event_authority, day_start, selection, eid_selection_args)
    return when (event) {
        AddonEvent.prayer_eid_start -> {
            val sunrise =
                sun?.sunrise ?: HostEventQueries.query_host_event_time(
                    context,
                    host_event_authority,
                    "SUNRISE",
                    0L,
                    selection,
                    eid_selection_args
                )
            sunrise?.plus(AddonEventMapper.eid_start_offset_millis)
        }

        AddonEvent.prayer_eid_end ->
            sun?.noon ?: HostEventQueries.query_host_event_time(
                context,
                host_event_authority,
                "NOON",
                0L,
                selection,
                eid_selection_args
            )
    }
}

fun calc_night(maghrib_prev: Long?, fajr: Long?): NightPortions? {
    if (maghrib_prev == null || fajr == null || fajr <= maghrib_prev) return null
    val night_len = fajr - maghrib_prev
    return NightPortions(maghrib_prev + night_len / 2, fajr - night_len / 3, fajr - night_len / 6)
}

fun query_host_sun(
    context: Context,
    host_event_authority: String,
    at_millis: Long,
    selection: String? = null,
    selection_args: Array<String>? = null
): SunTimes? {
    val calc_authority = HostConfigReader.calc_authority_from_event_authority(host_event_authority) ?: return null
    val uri = "content://$calc_authority/${CalculatorConfigContract.query_sun}/$at_millis".toUri()
    return try {
        context.contentResolver.query(uri, CalculatorConfigContract.projection_sun_basic, selection, selection_args, null)
    } catch (_: SecurityException) {
        null
    }?.use { c ->
        if (!c.moveToFirst()) return@use null

        fun get(col: String): Long? {
            val i = c.getColumnIndex(col)
            return if (i >= 0 && !c.isNull(i)) c.getLong(i) else null
        }

        SunTimes(
            noon = get(CalculatorConfigContract.column_sun_noon),
            sunrise = get(CalculatorConfigContract.column_sunrise),
            sunset = get(CalculatorConfigContract.column_sunset)
        )
    }
}

fun query_host_addon_time(
    context: Context,
    host_event_authority: String,
    event: AddonEvent,
    alarm_now: Long,
    selection: String? = null,
    selection_args: Array<String>? = null
): Long? {
    val effective_selection = selection ?: event_calc_selection
    val effective_selection_args = selection_args ?: event_calc_args(alarm_now)

    if (event.type == AddonEventType.night) return null
    if (!is_addon_event_enabled(context, event)) return null

    if (event == AddonEvent.prayer_eid_start || event == AddonEvent.prayer_eid_end) {
        return query_host_eid_time(
            context,
            host_event_authority,
            event,
            alarm_now,
            effective_selection,
            effective_selection_args
        )
    }

    if (event == AddonEvent.prayer_duha || event == AddonEvent.makruh_sunrise_end) {
        val sunrise =
            query_host_sun(
                context,
                host_event_authority,
                alarm_now,
                effective_selection,
                effective_selection_args
            )?.sunrise ?: return null
        return sunrise + Prefs.get_makruh_sunrise_minutes(context) * 60_000L
    }

    if (event == AddonEvent.prayer_asr || event == AddonEvent.makruh_after_asr_start) {
        return HostEventQueries.query_asr_time(context, host_event_authority, effective_selection, effective_selection_args)
    }

    val host_query = AddonEventMapper.map_event(context, event) ?: return null
    return HostEventQueries.query_host_event_time(
        context,
        host_event_authority,
        host_query.base_event_id,
        host_query.delta_millis,
        effective_selection,
        effective_selection_args
    )
}

fun query_addon_time(context: Context, event: AddonEvent, alarm_now: Long): Long? {
    val selection = event_calc_selection
    val selection_args = event_calc_args(alarm_now)
    val uri = "content://${PrayerTimesProvider.authority}/${AlarmEventContract.query_event_calc}/${event.event_id}".toUri()
    return try {
        context.contentResolver.query(
            uri,
            AlarmEventContract.query_event_calc_projection,
            selection,
            selection_args,
            null
        )?.use { c ->
            val i_time = c.getColumnIndex(AlarmEventContract.column_event_timemillis)
            if (c.moveToFirst() && i_time >= 0 && !c.isNull(i_time)) c.getLong(i_time) else null
        }
    } catch (_: SecurityException) {
        null
    }
}
