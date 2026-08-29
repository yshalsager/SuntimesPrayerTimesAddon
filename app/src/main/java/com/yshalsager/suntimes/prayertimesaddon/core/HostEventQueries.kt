package com.yshalsager.suntimes.prayertimesaddon.core

import android.content.Context
import androidx.core.net.toUri
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.tan

object HostEventQueries {
    fun host_event_exists(context: Context, host_event_authority: String, event_id: String): Boolean {
        val uri = "content://$host_event_authority/${AlarmEventContract.query_event_info}/$event_id".toUri()
        return query_host_provider(
            authority = host_event_authority,
            operation = AlarmEventContract.query_event_info,
            event_id = event_id,
            query = { context.contentResolver.query(uri, arrayOf(AlarmEventContract.column_event_name), null, null, null) }
        ) { cur ->
            if (cur.moveToFirst()) cur.host_string(AlarmEventContract.column_event_name)?.let { true } else null
        }.value_or_null == true
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
    ): Long? = query_host_event_time_result(
        context,
        host_event_authority,
        base_event_id,
        delta_millis,
        selection,
        selectionArgs
    ).value_or_null

    internal fun query_host_event_time_result(
        context: Context,
        host_event_authority: String,
        base_event_id: String,
        delta_millis: Long,
        selection: String?,
        selection_args: Array<String>?
    ): HostProviderResult<Long> {
        val host_uri = "content://$host_event_authority/${AlarmEventContract.query_event_calc}/$base_event_id".toUri()
        fun query_time(query_selection: String?, query_args: Array<String>?): HostProviderResult<Long> =
            query_host_provider(
                authority = host_event_authority,
                operation = AlarmEventContract.query_event_calc,
                event_id = base_event_id,
                query = {
                    context.contentResolver.query(
                        host_uri,
                        AlarmEventContract.query_event_calc_projection,
                        query_selection,
                        query_args,
                        null
                    )
                }
            ) { cur ->
                if (cur.moveToFirst()) cur.host_long(AlarmEventContract.column_event_timemillis) else null
            }

        val base_result = query_time(selection, selection_args)
        val base = base_result.value_or_null ?: return base_result
        if (delta_millis == 0L) return base_result

        val parsed_selection = parse_host_selection(selection, selection_args)
        val alarm_now = parsed_selection[AlarmEventContract.extra_alarm_now]?.toLongOrNull()
        val adjusted = base + delta_millis
        if (alarm_now != null && adjusted < alarm_now) {
            val retry = parsed_selection.with_values(mapOf(AlarmEventContract.extra_alarm_now to (base + 60_000L).toString()))
            val retry_result = query_time(retry.selection, retry.selection_args)
            val retry_base = retry_result.value_or_null ?: return retry_result
            return HostProviderResult.Success(retry_base + delta_millis)
        }

        return HostProviderResult.Success(adjusted)
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
        if (host_event_id != null) {
            val host_time = query_host_event_time(context, host_event_authority, host_event_id, 0, selection, selectionArgs)
            val alarm_now = parse_host_selection(selection, selectionArgs)[AlarmEventContract.extra_alarm_now]?.toLongOrNull()
            if (host_time != null && (alarm_now == null || host_time - alarm_now in 0L..48L * 60L * 60L * 1000L)) return host_time
        }
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
        return query_host_provider(
            authority = calc_authority,
            operation = CalculatorConfigContract.query_sunpos,
            query = { context.contentResolver.query(uri, CalculatorConfigContract.projection_sunpos_dec, null, null, null) }
        ) { cur ->
            if (cur.moveToFirst()) cur.host_double(CalculatorConfigContract.column_sunpos_dec) else null
        }.value_or_null
    }
}
