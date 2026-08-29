package com.yshalsager.suntimes.prayertimesaddon.core

import android.content.Context
import java.math.BigDecimal

internal fun parse_exact_int(value: Any?): Int? =
    when (value) {
        is String -> value.toIntOrNull()
        is Float, is Double -> null
        is Number -> try {
            BigDecimal(value.toString()).intValueExact()
        } catch (_: ArithmeticException) {
            null
        } catch (_: NumberFormatException) {
            null
        }
        else -> null
    }

data class MethodConfig(
    val method_preset: String,
    val fajr_angle: Double,
    val isha_mode: String,
    val isha_angle: Double,
    val isha_fixed_minutes: Int,
    val asr_factor: Int,
    val maghrib_offset_minutes: Int,
    val makruh_angle: Double,
    val makruh_sunrise_minutes: Int,
    val zawal_minutes: Int
) {
    companion object {
        val supported_presets = listOf("egypt", "mwl", "karachi", "isna", "uaq", "uiof", "custom")
        val supported_makruh_presets = listOf("shafi", "hanafi", "custom")
        fun is_valid_angle(value: Double): Boolean = value.isFinite() && value in 0.0..90.0
        fun is_valid_minutes(value: Int): Boolean = value in 0..1440
        fun is_valid_offset_minutes(value: Int): Boolean = value in -1440..1440

        fun defaults(): MethodConfig =
            MethodConfig(
                method_preset = "egypt",
                fajr_angle = 19.5,
                isha_mode = Prefs.isha_mode_angle,
                isha_angle = 17.5,
                isha_fixed_minutes = 90,
                asr_factor = 1,
                maghrib_offset_minutes = 0,
                makruh_angle = 5.0,
                makruh_sunrise_minutes = 15,
                zawal_minutes = 10
            )
    }

    fun normalized(): MethodConfig {
        val fallback = defaults()
        return copy(
            method_preset = method_preset.takeIf { it in supported_presets } ?: fallback.method_preset,
            fajr_angle = fajr_angle.takeIf(::is_valid_angle) ?: fallback.fajr_angle,
            isha_mode = isha_mode.takeIf { it == Prefs.isha_mode_angle || it == Prefs.isha_mode_fixed } ?: fallback.isha_mode,
            isha_angle = isha_angle.takeIf(::is_valid_angle) ?: fallback.isha_angle,
            isha_fixed_minutes = isha_fixed_minutes.takeIf(::is_valid_minutes) ?: fallback.isha_fixed_minutes,
            asr_factor = asr_factor.takeIf { it == 1 || it == 2 } ?: fallback.asr_factor,
            maghrib_offset_minutes = maghrib_offset_minutes.takeIf(::is_valid_offset_minutes) ?: fallback.maghrib_offset_minutes,
            makruh_angle = makruh_angle.takeIf(::is_valid_angle) ?: fallback.makruh_angle,
            makruh_sunrise_minutes = makruh_sunrise_minutes.takeIf { it == 10 || it == 15 || it == 20 } ?: fallback.makruh_sunrise_minutes,
            zawal_minutes = zawal_minutes.takeIf(::is_valid_minutes) ?: fallback.zawal_minutes
        )
    }
}

fun method_config_from_prefs(context: Context): MethodConfig =
    MethodConfig(
        method_preset = Prefs.get_method_preset(context),
        fajr_angle = Prefs.get_fajr_angle(context),
        isha_mode = Prefs.get_isha_mode(context),
        isha_angle = Prefs.get_isha_angle(context),
        isha_fixed_minutes = Prefs.get_isha_fixed_minutes(context),
        asr_factor = Prefs.get_asr_factor(context),
        maghrib_offset_minutes = Prefs.get_maghrib_offset_minutes(context),
        makruh_angle = Prefs.get_makruh_angle(context),
        makruh_sunrise_minutes = Prefs.get_makruh_sunrise_minutes(context),
        zawal_minutes = Prefs.get_zawal_minutes(context)
    )

fun method_config_with_preset(base: MethodConfig, preset: String): MethodConfig {
    val normalized = preset.takeIf { it in MethodConfig.supported_presets } ?: "custom"
    return when (normalized) {
        "egypt" ->
            base.copy(
                method_preset = "egypt",
                fajr_angle = 19.5,
                isha_mode = Prefs.isha_mode_angle,
                isha_angle = 17.5
            )

        "mwl" ->
            base.copy(
                method_preset = "mwl",
                fajr_angle = 18.0,
                isha_mode = Prefs.isha_mode_angle,
                isha_angle = 17.0
            )

        "karachi" ->
            base.copy(
                method_preset = "karachi",
                fajr_angle = 18.0,
                isha_mode = Prefs.isha_mode_angle,
                isha_angle = 18.0
            )

        "isna" ->
            base.copy(
                method_preset = "isna",
                fajr_angle = 15.0,
                isha_mode = Prefs.isha_mode_angle,
                isha_angle = 15.0
            )

        "uaq" ->
            base.copy(
                method_preset = "uaq",
                fajr_angle = 18.5,
                isha_mode = Prefs.isha_mode_fixed,
                isha_fixed_minutes = 90
            )

        "uiof" ->
            base.copy(
                method_preset = "uiof",
                fajr_angle = 12.0,
                isha_mode = Prefs.isha_mode_angle,
                isha_angle = 12.0
            )

        else -> base.copy(method_preset = "custom")
    }
}

fun method_config_for_preset(context: Context, preset: String): MethodConfig =
    method_config_with_preset(method_config_from_prefs(context), preset)
