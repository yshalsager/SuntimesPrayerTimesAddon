package com.yshalsager.suntimes.prayertimesaddon.core

import android.content.Context
import com.yshalsager.suntimes.prayertimesaddon.R

enum class AddonEventType(val type_id: String, val label_res: Int) {
    prayer("PRAYER", R.string.event_type_prayer),
    makruh("MAKRUH", R.string.event_type_makruh),
    night("NIGHT", R.string.event_type_night)
}

enum class AddonEvent(
    val event_id: String,
    val title_res: Int,
    val type: AddonEventType
) {
    prayer_fajr("PRAYER_FAJR", R.string.event_prayer_fajr, AddonEventType.prayer),
    prayer_fajr_extra_1("PRAYER_FAJR_EXTRA_1", R.string.event_prayer_fajr_extra_1, AddonEventType.prayer),
    prayer_duha("PRAYER_DUHA", R.string.event_prayer_duha, AddonEventType.prayer),
    prayer_eid_fitr_start("PRAYER_EID_FITR_START", R.string.event_prayer_eid_fitr_start, AddonEventType.prayer),
    prayer_eid_fitr_end("PRAYER_EID_FITR_END", R.string.event_prayer_eid_fitr_end, AddonEventType.prayer),
    prayer_eid_adha_start("PRAYER_EID_ADHA_START", R.string.event_prayer_eid_adha_start, AddonEventType.prayer),
    prayer_eid_adha_end("PRAYER_EID_ADHA_END", R.string.event_prayer_eid_adha_end, AddonEventType.prayer),
    prayer_dhuhr("PRAYER_DHUHR", R.string.event_prayer_dhuhr, AddonEventType.prayer),
    prayer_asr("PRAYER_ASR", R.string.event_prayer_asr, AddonEventType.prayer),
    prayer_maghrib("PRAYER_MAGHRIB", R.string.event_prayer_maghrib, AddonEventType.prayer),
    prayer_isha("PRAYER_ISHA", R.string.event_prayer_isha, AddonEventType.prayer),
    prayer_isha_extra_1("PRAYER_ISHA_EXTRA_1", R.string.event_prayer_isha_extra_1, AddonEventType.prayer),

    night_midpoint("NIGHT_MIDPOINT", R.string.night_midpoint, AddonEventType.night),
    night_last_third("NIGHT_LAST_THIRD", R.string.night_last_third, AddonEventType.night),
    night_last_sixth("NIGHT_LAST_SIXTH", R.string.night_last_sixth, AddonEventType.night),

    makruh_dawn_start("MAKRUH_DAWN_START", R.string.event_makruh_dawn_start, AddonEventType.makruh),
    makruh_dawn_end("MAKRUH_DAWN_END", R.string.event_makruh_dawn_end, AddonEventType.makruh),
    makruh_sunrise_start(
        "MAKRUH_SUNRISE_START",
        R.string.event_makruh_sunrise_start,
        AddonEventType.makruh
    ),
    makruh_sunrise_end(
        "MAKRUH_SUNRISE_END",
        R.string.event_makruh_sunrise_end,
        AddonEventType.makruh
    ),
    makruh_zawal_start(
        "MAKRUH_ZAWAL_START",
        R.string.event_makruh_zawal_start,
        AddonEventType.makruh
    ),
    makruh_zawal_end("MAKRUH_ZAWAL_END", R.string.event_makruh_zawal_end, AddonEventType.makruh),
    makruh_after_asr_start("MAKRUH_AFTER_ASR_START", R.string.event_makruh_after_asr_start, AddonEventType.makruh),
    makruh_after_asr_end("MAKRUH_AFTER_ASR_END", R.string.event_makruh_after_asr_end, AddonEventType.makruh),
    makruh_sunset_start(
        "MAKRUH_SUNSET_START",
        R.string.event_makruh_sunset_start,
        AddonEventType.makruh
    ),
    makruh_sunset_end("MAKRUH_SUNSET_END", R.string.event_makruh_sunset_end, AddonEventType.makruh)
}

fun AddonEvent.is_eid_event(): Boolean =
    when (this) {
        AddonEvent.prayer_eid_fitr_start,
        AddonEvent.prayer_eid_fitr_end,
        AddonEvent.prayer_eid_adha_start,
        AddonEvent.prayer_eid_adha_end -> true
        else -> false
    }

data class HostQuery(val base_event_id: String, val delta_millis: Long = 0L)

object HostEventIds {
    fun sun_elevation(angle: Double, rising: Boolean): String =
        "SUN_${format_angle_id(angle)}" + if (rising) "r" else "s"

    // Asr uses a ratio relative to the noon shadow.
    fun shadow_ratio(factor: Int): String = "SHADOWRATIO_X:${format_angle_id(factor.toDouble())}"
}

object AddonEventMapper {
    const val eid_start_offset_millis = 15L * 60_000L

    fun map_event(
        context: Context,
        addon_event: AddonEvent,
        method_config: MethodConfig? = null,
        addon_runtime_profile: AddonRuntimeProfile? = null
    ): HostQuery? {
        val cfg = method_config ?: method_config_from_prefs(context)
        val runtime = addon_runtime_profile ?: addon_runtime_profile_from_prefs(context)
        return when (addon_event) {
            AddonEvent.prayer_fajr ->
                HostQuery(HostEventIds.sun_elevation(-cfg.fajr_angle, rising = true))
            AddonEvent.prayer_fajr_extra_1 ->
                HostQuery(HostEventIds.sun_elevation(-runtime.extra_fajr_1_angle, rising = true))
            AddonEvent.prayer_duha -> {
                val delta = cfg.makruh_sunrise_minutes * 60_000L
                HostQuery("SUNRISE", delta_millis = delta)
            }
            AddonEvent.prayer_eid_fitr_start,
            AddonEvent.prayer_eid_adha_start -> HostQuery("SUNRISE", delta_millis = eid_start_offset_millis)
            AddonEvent.prayer_eid_fitr_end,
            AddonEvent.prayer_eid_adha_end -> HostQuery("NOON")

            AddonEvent.prayer_dhuhr -> HostQuery("NOON")
            AddonEvent.prayer_asr -> HostQuery(HostEventIds.shadow_ratio(cfg.asr_factor))

            AddonEvent.prayer_maghrib -> {
                val delta = cfg.maghrib_offset_minutes * 60_000L
                HostQuery("SUNSET", delta_millis = delta)
            }

            AddonEvent.prayer_isha -> when (cfg.isha_mode) {
                Prefs.isha_mode_fixed -> {
                    val delta = cfg.isha_fixed_minutes * 60_000L
                    HostQuery("SUNSET", delta_millis = delta)
                }

                else -> HostQuery(HostEventIds.sun_elevation(-cfg.isha_angle, rising = false))
            }
            AddonEvent.prayer_isha_extra_1 ->
                HostQuery(HostEventIds.sun_elevation(-runtime.extra_isha_1_angle, rising = false))

            AddonEvent.makruh_dawn_start ->
                HostQuery(HostEventIds.sun_elevation(-cfg.fajr_angle, rising = true))
            AddonEvent.makruh_dawn_end -> HostQuery("SUNRISE")
            AddonEvent.makruh_sunrise_start -> HostQuery("SUNRISE")
            AddonEvent.makruh_sunrise_end -> {
                val delta = cfg.makruh_sunrise_minutes * 60_000L
                HostQuery("SUNRISE", delta_millis = delta)
            }

            AddonEvent.makruh_zawal_end -> HostQuery("NOON")
            AddonEvent.makruh_zawal_start -> {
                val delta = -cfg.zawal_minutes * 60_000L
                HostQuery("NOON", delta_millis = delta)
            }

            AddonEvent.makruh_after_asr_start -> HostQuery(HostEventIds.shadow_ratio(cfg.asr_factor))
            AddonEvent.makruh_after_asr_end -> HostQuery(HostEventIds.sun_elevation(cfg.makruh_angle, rising = false))
            AddonEvent.makruh_sunset_start -> HostQuery(HostEventIds.sun_elevation(cfg.makruh_angle, rising = false))

            AddonEvent.makruh_sunset_end -> HostQuery("SUNSET")

            AddonEvent.night_midpoint,
            AddonEvent.night_last_third,
            AddonEvent.night_last_sixth -> null
        }
    }
}
