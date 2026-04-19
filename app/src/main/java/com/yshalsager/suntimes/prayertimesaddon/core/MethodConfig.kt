package com.yshalsager.suntimes.prayertimesaddon.core

import android.content.Context

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
