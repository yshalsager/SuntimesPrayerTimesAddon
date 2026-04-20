package com.yshalsager.suntimes.prayertimesaddon.core

import android.content.Context
import androidx.core.net.toUri
import com.yshalsager.suntimes.prayertimesaddon.provider.PrayerTimesProvider
import net.time4j.PlainDate
import net.time4j.TemporalType
import net.time4j.calendar.HijriCalendar
import java.util.Locale
import java.util.TimeZone

data class NightPortions(val midpoint: Long, val last_third: Long, val last_sixth: Long)

data class SunTimes(val noon: Long?, val sunrise: Long?, val sunset: Long?)

private data class EidSpec(val month: Int, val day: Int, val is_start: Boolean)

private const val event_calc_selection =
    AlarmEventContract.extra_alarm_now + "=? AND " +
        AlarmEventContract.extra_alarm_offset + "=? AND " +
        AlarmEventContract.extra_alarm_repeat + "=? AND " +
        AlarmEventContract.extra_alarm_repeat_days + "=?"

private fun event_calc_args(alarm_now: Long): Array<String> =
    arrayOf(alarm_now.toString(), "0", "false", "[]")

private fun resolve_host_timezone(
    context: Context,
    host_event_authority: String,
    timezone_override: TimeZone?
): TimeZone =
    timezone_override
        ?: HostConfigReader.read_config(context, host_event_authority)?.timezone?.let(TimeZone::getTimeZone)
        ?: TimeZone.getDefault()

private fun eid_spec_for_event(event: AddonEvent): EidSpec? =
    when (event) {
        AddonEvent.prayer_eid_fitr_start -> EidSpec(month = 10, day = 1, is_start = true)
        AddonEvent.prayer_eid_fitr_end -> EidSpec(month = 10, day = 1, is_start = false)
        AddonEvent.prayer_eid_adha_start -> EidSpec(month = 12, day = 10, is_start = true)
        AddonEvent.prayer_eid_adha_end -> EidSpec(month = 12, day = 10, is_start = false)
        else -> null
    }

private fun time4j_hijri_variant(variant: String): String =
    if (variant == Prefs.hijri_variant_diyanet) HijriCalendar.VARIANT_DIYANET else HijriCalendar.VARIANT_UMALQURA

private fun is_eid_day(
    context: Context,
    host_event_authority: String,
    alarm_now: Long,
    timezone_override: TimeZone?,
    eid_spec: EidSpec,
    runtime_profile: AddonRuntimeProfile = addon_runtime_profile_from_prefs(context)
): Boolean {
    val tz = resolve_host_timezone(context, host_event_authority, timezone_override)
    val day_start = day_start_at(alarm_now, tz)
    val hijri =
        try {
            hijri_for_day(
                day_start_millis = day_start,
                tz = tz,
                locale = Locale.getDefault(),
                variant = runtime_profile.hijri_variant,
                offset_days = runtime_profile.hijri_day_offset
            )
        } catch (_: ArithmeticException) {
            return false
        }
    return hijri.month == eid_spec.month && hijri.day == eid_spec.day
}

fun query_host_eid_time(
    context: Context,
    host_event_authority: String,
    event: AddonEvent,
    alarm_now: Long,
    selection: String?,
    selection_args: Array<String>?,
    timezone_override: TimeZone? = null,
    addon_runtime_profile_override: AddonRuntimeProfile? = null
): Long? {
    val eid_spec = eid_spec_for_event(event) ?: return null
    val runtime = (addon_runtime_profile_override ?: addon_runtime_profile_from_prefs(context)).normalized()
    if (!is_eid_day(context, host_event_authority, alarm_now, timezone_override, eid_spec, runtime)) return null

    val tz = resolve_host_timezone(context, host_event_authority, timezone_override)
    val day_start = day_start_at(alarm_now, tz)
    val eid_selection_args =
        selection_args?.clone()?.also { args ->
            if (args.isNotEmpty()) args[0] = day_start.toString()
        } ?: selection_args
    val sun = query_host_sun(context, host_event_authority, day_start, selection, eid_selection_args)
    fun same_selected_day(v: Long?): Boolean = v != null && day_start_at(v, tz) == day_start
    val base_event_id = if (eid_spec.is_start) "SUNRISE" else "NOON"
    val delta = if (eid_spec.is_start) AddonEventMapper.eid_start_offset_millis else 0L
    val primary = if (eid_spec.is_start) sun?.sunrise else sun?.noon
    val resolved =
        primary?.takeIf(::same_selected_day) ?: HostEventQueries.query_host_event_time(
            context,
            host_event_authority,
            base_event_id,
            0L,
            selection,
            eid_selection_args
        )?.takeIf(::same_selected_day)
    return resolved?.plus(delta)
}

fun find_next_eid_day_start(
    context: Context,
    host_event_authority: String,
    event: AddonEvent,
    from_day_start: Long,
    timezone_override: TimeZone? = null,
    addon_runtime_profile_override: AddonRuntimeProfile? = null
): Long? {
    val eid_spec = eid_spec_for_event(event) ?: return null
    val runtime = (addon_runtime_profile_override ?: addon_runtime_profile_from_prefs(context)).normalized()
    val tz = resolve_host_timezone(context, host_event_authority, timezone_override)
    val offset_days = runtime.hijri_day_offset.coerceIn(-2, 2)

    fun candidate_day_start(hijri_year: Int): Long? {
        val base_day_start =
            try {
                val date = HijriCalendar.of(time4j_hijri_variant(runtime.hijri_variant), hijri_year, eid_spec.month, eid_spec.day)
                    .transform(PlainDate::class.java)
                TemporalType.MILLIS_SINCE_UNIX.from(date.atFirstMoment(tz.id))
            } catch (_: ArithmeticException) {
                return null
            }
        return add_days(base_day_start, -offset_days, tz)
    }

    val current_hijri =
        try {
            hijri_for_day(
                day_start_millis = from_day_start,
                tz = tz,
                locale = Locale.getDefault(),
                variant = runtime.hijri_variant,
                offset_days = runtime.hijri_day_offset
            )
        } catch (_: ArithmeticException) {
            return null
        }

    val target_year =
        when {
            current_hijri.month < eid_spec.month -> current_hijri.year
            current_hijri.month > eid_spec.month -> current_hijri.year + 1
            current_hijri.day < eid_spec.day -> current_hijri.year
            else -> current_hijri.year + 1
        }

    val first = candidate_day_start(target_year)
    if (first != null && first > from_day_start) return first
    return candidate_day_start(target_year + 1)
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
    selection_args: Array<String>? = null,
    timezone_override: TimeZone? = null,
    latitude_override: Double? = null,
    method_config_override: MethodConfig? = null,
    addon_runtime_profile_override: AddonRuntimeProfile? = null
): Long? {
    val effective_selection = selection ?: event_calc_selection
    val effective_selection_args = selection_args ?: event_calc_args(alarm_now)
    val method_config = method_config_override ?: method_config_from_prefs(context)
    val runtime_profile = (addon_runtime_profile_override ?: addon_runtime_profile_from_prefs(context)).normalized()

    if (event.type == AddonEventType.night) return null
    if (!is_addon_event_enabled(context, event, runtime_profile)) return null

    if (event.is_eid_event()) {
        return query_host_eid_time(
            context,
            host_event_authority,
            event,
            alarm_now,
            effective_selection,
            effective_selection_args,
            timezone_override,
            runtime_profile
        )
    }

    if (event == AddonEvent.prayer_duha || event == AddonEvent.makruh_sunrise_end) {
        val tz = resolve_host_timezone(context, host_event_authority, timezone_override)
        val expected_day_start = day_start_at(alarm_now, tz)
        val sunrise =
            query_host_sun(
                context,
                host_event_authority,
                alarm_now,
                effective_selection,
                effective_selection_args
            )?.sunrise?.takeIf { day_start_at(it, tz) == expected_day_start }
                ?: HostEventQueries.query_host_event_time(
                    context,
                    host_event_authority,
                    "SUNRISE",
                    0L,
                    effective_selection,
                    effective_selection_args
                )?.takeIf { day_start_at(it, tz) == expected_day_start }
                ?: return null
        return sunrise + method_config.makruh_sunrise_minutes * 60_000L
    }

    if (event == AddonEvent.prayer_asr || event == AddonEvent.makruh_after_asr_start) {
        return HostEventQueries.query_asr_time(
            context,
            host_event_authority,
            effective_selection,
            effective_selection_args,
            latitude_override,
            method_config.asr_factor
        )
    }

    val host_query = AddonEventMapper.map_event(context, event, method_config, runtime_profile) ?: return null
    return HostEventQueries.query_host_event_time(
        context,
        host_event_authority,
        host_query.base_event_id,
        host_query.delta_millis,
        effective_selection,
        effective_selection_args
    )
}

fun query_addon_time(
    context: Context,
    event: AddonEvent,
    alarm_now: Long,
    selection: String? = null,
    selection_args: Array<String>? = null,
    saved_location_id: String? = null
): Long? {
    val effective_selection = selection ?: event_calc_selection
    val effective_selection_args = selection_args ?: event_calc_args(alarm_now)
    val uri_builder =
        "content://${PrayerTimesProvider.authority}/${AlarmEventContract.query_event_calc}/${event.event_id}"
            .toUri()
            .buildUpon()
    saved_location_id?.trim()?.takeIf { it.isNotBlank() }?.let {
        uri_builder.appendQueryParameter(AlarmEventContract.extra_saved_location_id, it)
    }
    val uri = uri_builder.build()
    return try {
        context.contentResolver.query(
            uri,
            AlarmEventContract.query_event_calc_projection,
            effective_selection,
            effective_selection_args,
            null
        )?.use { c ->
            val i_time = c.getColumnIndex(AlarmEventContract.column_event_timemillis)
            if (c.moveToFirst() && i_time >= 0 && !c.isNull(i_time)) c.getLong(i_time) else null
        }
    } catch (_: SecurityException) {
        null
    }
}
