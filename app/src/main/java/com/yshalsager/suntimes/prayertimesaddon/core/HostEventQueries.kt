package com.yshalsager.suntimes.prayertimesaddon.core

import android.content.Context
import android.net.Uri
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.tan

object HostEventQueries {
    private data class ShadowRatioKey(val host: String, val factor: Int)
    private val shadow_ratio_event_id_cache = HashMap<ShadowRatioKey, String?>()

    private data class HostEventKey(
        val host: String,
        val base_event_id: String,
        val delta_millis: Long,
        val selection: String?,
        val args: String?
    )

    private class Lru<K, V>(private val max: Int) : LinkedHashMap<K, V>(max, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>): Boolean = size > max
    }

    private val host_event_time_cache = Lru<HostEventKey, Long?>(256)

    private data class SunPosKey(val calc: String, val at_millis: Long)
    private val sunpos_dec_cache = Lru<SunPosKey, Double?>(64)

    fun host_event_exists(context: Context, host_event_authority: String, event_id: String): Boolean {
        val uri = Uri.parse("content://$host_event_authority/${AlarmEventContract.query_event_info}/$event_id")
        return try {
            context.contentResolver.query(uri, arrayOf(AlarmEventContract.column_event_name), null, null, null)
        } catch (_: SecurityException) {
            null
        }?.use { it.moveToFirst() } == true
    }

    fun resolve_shadow_ratio_event_id(context: Context, host_event_authority: String, factor: Int): String? {
        val key = ShadowRatioKey(host_event_authority, factor)
        synchronized(shadow_ratio_event_id_cache) {
            if (shadow_ratio_event_id_cache.containsKey(key)) return shadow_ratio_event_id_cache[key]
        }

        val v = format_angle_id(factor.toDouble())
        val candidates = listOf(
            HostEventIds.shadow_ratio(factor), // SHADOWRATIO_X:<factor> (SuntimesWidget develop)
            "SHADOWRATIO_X:$v", // same, explicit
            "SHADOWRATIO_${v}", // legacy addon builds
            "SHADOWRATIO_${factor}"
        )
        val chosen = candidates.firstOrNull { host_event_exists(context, host_event_authority, it) }

        synchronized(shadow_ratio_event_id_cache) { shadow_ratio_event_id_cache[key] = chosen }
        return chosen
    }

    fun query_host_event_time(
        context: Context,
        host_event_authority: String,
        base_event_id: String,
        delta_millis: Long,
        selection: String?,
        selectionArgs: Array<String>?
    ): Long? {
        val key = HostEventKey(
            host = host_event_authority,
            base_event_id = base_event_id,
            delta_millis = delta_millis,
            selection = selection,
            args = selectionArgs?.joinToString("\u0001")
        )
        synchronized(host_event_time_cache) {
            if (host_event_time_cache.containsKey(key)) return host_event_time_cache[key]
        }

        val host_uri = Uri.parse("content://$host_event_authority/${AlarmEventContract.query_event_calc}/$base_event_id")
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

        if (base == null) {
            synchronized(host_event_time_cache) { host_event_time_cache[key] = null }
            return null
        }

        if (delta_millis == 0L) {
            synchronized(host_event_time_cache) { host_event_time_cache[key] = base }
            return base
        }

        val alarm_now = selectionArgs?.getOrNull(0)?.toLongOrNull()
        val adjusted = base + delta_millis
        if (alarm_now != null && adjusted < alarm_now) {
            val retry_args = selectionArgs.clone()
            if (retry_args.isNotEmpty()) retry_args[0] = (base + 60_000L).toString()
            val retry_base = try {
                context.contentResolver.query(host_uri, AlarmEventContract.query_event_calc_projection, selection, retry_args, null)
            } catch (_: SecurityException) {
                null
            }?.use { cur ->
                if (!cur.moveToFirst()) return@use null
                val i_time = cur.getColumnIndex(AlarmEventContract.column_event_timemillis)
                if (i_time < 0 || cur.isNull(i_time)) return@use null
                cur.getLong(i_time)
            }

            val v = (retry_base ?: base) + delta_millis
            synchronized(host_event_time_cache) { host_event_time_cache[key] = v }
            return v
        }

        synchronized(host_event_time_cache) { host_event_time_cache[key] = adjusted }
        return adjusted
    }

    fun query_asr_time(
        context: Context,
        host_event_authority: String,
        selection: String?,
        selectionArgs: Array<String>?
    ): Long? {
        val factor = Prefs.get_asr_factor(context)
        val host_event_id = resolve_shadow_ratio_event_id(context, host_event_authority, factor)
        if (host_event_id != null) return query_host_event_time(context, host_event_authority, host_event_id, 0, selection, selectionArgs)
        return calc_asr_fallback_time(context, host_event_authority, selection, selectionArgs)
    }

    fun calc_asr_fallback_time(
        context: Context,
        host_event_authority: String,
        selection: String?,
        selectionArgs: Array<String>?
    ): Long? {
        val factor = Prefs.get_asr_factor(context)
        val noon = query_host_event_time(context, host_event_authority, "NOON", 0, selection, selectionArgs) ?: return null

        val lat = HostConfigReader.read_config(context, host_event_authority)?.latitude?.toDoubleOrNull() ?: return null
        val dec = query_host_declination(context, host_event_authority, noon) ?: return null

        val ratio = factor + tan(Math.toRadians(abs(lat - dec)))
        val angle = Math.toDegrees(atan(1.0 / ratio))
        val sun_event_id = HostEventIds.sun_elevation(angle, rising = false)

        return query_host_event_time(context, host_event_authority, sun_event_id, 0, selection, selectionArgs)
    }

    fun query_host_declination(context: Context, host_event_authority: String, at_millis: Long): Double? {
        val calc_authority = HostConfigReader.calc_authority_from_event_authority(host_event_authority) ?: return null
        val key = SunPosKey(calc_authority, at_millis)
        synchronized(sunpos_dec_cache) {
            if (sunpos_dec_cache.containsKey(key)) return sunpos_dec_cache[key]
        }

        val uri = Uri.parse("content://$calc_authority/${CalculatorConfigContract.query_sunpos}/$at_millis")
        val v = try {
            context.contentResolver.query(uri, CalculatorConfigContract.projection_sunpos_dec, null, null, null)
        } catch (_: SecurityException) {
            null
        }?.use { c ->
            if (!c.moveToFirst()) return@use null
            val i = c.getColumnIndex(CalculatorConfigContract.column_sunpos_dec)
            if (i < 0 || c.isNull(i)) return@use null
            c.getDouble(i)
        }

        synchronized(sunpos_dec_cache) { sunpos_dec_cache[key] = v }
        return v
    }

    // Keep caches process-local for performance; they are safe to drop on process death.
}
