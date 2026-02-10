package com.yshalsager.suntimes.prayertimesaddon.core

import android.content.Context
import android.net.Uri
import com.yshalsager.suntimes.prayertimesaddon.provider.PrayerTimesProvider

data class NightPortions(val midpoint: Long, val last_third: Long, val last_sixth: Long)

data class SunTimes(val noon: Long?, val sunrise: Long?, val sunset: Long?)

private const val event_calc_selection =
    AlarmEventContract.extra_alarm_now + "=? AND " +
        AlarmEventContract.extra_alarm_offset + "=? AND " +
        AlarmEventContract.extra_alarm_repeat + "=? AND " +
        AlarmEventContract.extra_alarm_repeat_days + "=?"

private fun event_calc_args(alarm_now: Long): Array<String> =
    arrayOf(alarm_now.toString(), "0", "false", "[]")

fun calc_night(maghrib_prev: Long?, fajr: Long?): NightPortions? {
    if (maghrib_prev == null || fajr == null || fajr <= maghrib_prev) return null
    val night_len = fajr - maghrib_prev
    return NightPortions(maghrib_prev + night_len / 2, fajr - night_len / 3, fajr - night_len / 6)
}

fun query_host_sun(context: Context, host_event_authority: String, at_millis: Long): SunTimes? {
    val calc_authority = HostConfigReader.calc_authority_from_event_authority(host_event_authority) ?: return null
    val uri = Uri.parse("content://$calc_authority/${CalculatorConfigContract.query_sun}/$at_millis")
    return try {
        context.contentResolver.query(uri, CalculatorConfigContract.projection_sun_basic, null, null, null)
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

fun query_host_addon_time(context: Context, host_event_authority: String, event: AddonEvent, alarm_now: Long): Long? {
    val selection = event_calc_selection
    val selection_args = event_calc_args(alarm_now)

    if (event.type == AddonEventType.night) return null

    if (event == AddonEvent.prayer_asr) {
        return HostEventQueries.query_asr_time(context, host_event_authority, selection, selection_args)
    }

    val host_query = AddonEventMapper.map_event(context, event) ?: return null
    return HostEventQueries.query_host_event_time(context, host_event_authority, host_query.base_event_id, host_query.delta_millis, selection, selection_args)
}

fun query_addon_time(context: Context, event: AddonEvent, alarm_now: Long): Long? {
    val selection = event_calc_selection
    val selection_args = event_calc_args(alarm_now)
    val uri = Uri.parse("content://${PrayerTimesProvider.authority}/${AlarmEventContract.query_event_calc}/${event.event_id}")
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
