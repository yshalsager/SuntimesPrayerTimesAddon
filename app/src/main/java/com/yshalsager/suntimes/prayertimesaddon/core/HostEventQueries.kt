package com.yshalsager.suntimes.prayertimesaddon.core

import android.content.Context
import androidx.core.net.toUri
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.tan

object HostEventQueries {
    fun host_event_exists(context: Context, host_event_authority: String, event_id: String): Boolean {
        val uri = "content://$host_event_authority/${AlarmEventContract.query_event_info}/$event_id".toUri()
        return try {
            context.contentResolver.query(uri, arrayOf(AlarmEventContract.column_event_name), null, null, null)
        } catch (_: SecurityException) {
            null
        }?.use { it.moveToFirst() } == true
    }

    fun resolve_shadow_ratio_event_id(context: Context, host_event_authority: String, factor: Int): String? {
        val v = format_angle_id(factor.toDouble())
        return listOf(
            HostEventIds.shadow_ratio(factor),
            "SHADOWRATIO_${v}",
            "SHADOWRATIO_${factor}"
        ).firstOrNull { host_event_exists(context, host_event_authority, it) }
    }

    fun query_host_event_time(
        context: Context,
        host_event_authority: String,
        base_event_id: String,
        delta_millis: Long,
        selection: String?,
        selectionArgs: Array<String>?
    ): Long? {
        val host_uri = "content://$host_event_authority/${AlarmEventContract.query_event_calc}/$base_event_id".toUri()
        val base = try {
            context.contentResolver.query(host_uri, AlarmEventContract.query_event_calc_projection, selection, selectionArgs, null)
        } catch (_: SecurityException) {
            null
        }?.use { cur ->
            if (!cur.moveToFirst()) return@use null
            val i_time = cur.getColumnIndex(AlarmEventContract.column_event_timemillis)
            if (i_time < 0 || cur.isNull(i_time)) return@use null
            cur.getLong(i_time)
        }

        if (base == null) return null
        if (delta_millis == 0L) return base

        val parsed_selection = parse_host_selection(selection, selectionArgs)
        val alarm_now = parsed_selection[AlarmEventContract.extra_alarm_now]?.toLongOrNull()
        val adjusted = base + delta_millis
        if (alarm_now != null && adjusted < alarm_now) {
            val retry = parsed_selection.with_values(mapOf(AlarmEventContract.extra_alarm_now to (base + 60_000L).toString()))
            val retry_base = try {
                context.contentResolver.query(host_uri, AlarmEventContract.query_event_calc_projection, retry.selection, retry.selection_args, null)
            } catch (_: SecurityException) {
                null
            }?.use { cur ->
                if (!cur.moveToFirst()) return@use null
                val i_time = cur.getColumnIndex(AlarmEventContract.column_event_timemillis)
                if (i_time < 0 || cur.isNull(i_time)) return@use null
                cur.getLong(i_time)
            }

            return (retry_base ?: base) + delta_millis
        }

        return adjusted
    }

    fun query_asr_time(
        context: Context,
        host_event_authority: String,
        selection: String?,
        selectionArgs: Array<String>?,
        latitude_override: Double? = null,
        asr_factor_override: Int? = null
    ): Long? {
        val factor = asr_factor_override ?: Prefs.get_asr_factor(context)
        val host_event_id = resolve_shadow_ratio_event_id(context, host_event_authority, factor)
        if (host_event_id != null) return query_host_event_time(context, host_event_authority, host_event_id, 0, selection, selectionArgs)
        return calc_asr_fallback_time(context, host_event_authority, selection, selectionArgs, latitude_override, factor)
    }

    fun calc_asr_fallback_time(
        context: Context,
        host_event_authority: String,
        selection: String?,
        selectionArgs: Array<String>?,
        latitude_override: Double? = null,
        asr_factor_override: Int? = null
    ): Long? {
        val factor = asr_factor_override ?: Prefs.get_asr_factor(context)
        val noon = query_host_event_time(context, host_event_authority, "NOON", 0, selection, selectionArgs) ?: return null

        val lat = latitude_override ?: HostConfigReader.read_config(context, host_event_authority)?.latitude?.toDoubleOrNull() ?: return null
        val dec = query_host_declination(context, host_event_authority, noon) ?: return null

        val ratio = factor + tan(Math.toRadians(abs(lat - dec)))
        val angle = Math.toDegrees(atan(1.0 / ratio))
        val sun_event_id = HostEventIds.sun_elevation(angle, rising = false)

        return query_host_event_time(context, host_event_authority, sun_event_id, 0, selection, selectionArgs)
    }

    fun query_host_declination(context: Context, host_event_authority: String, at_millis: Long): Double? {
        val calc_authority = HostConfigReader.calc_authority_from_event_authority(host_event_authority) ?: return null
        val uri = "content://$calc_authority/${CalculatorConfigContract.query_sunpos}/$at_millis".toUri()
        return try {
            context.contentResolver.query(uri, CalculatorConfigContract.projection_sunpos_dec, null, null, null)
        } catch (_: SecurityException) {
            null
        }?.use { c ->
            if (!c.moveToFirst()) return@use null
            val i = c.getColumnIndex(CalculatorConfigContract.column_sunpos_dec)
            if (i < 0 || c.isNull(i)) return@use null
            c.getDouble(i)
        }
    }
}
