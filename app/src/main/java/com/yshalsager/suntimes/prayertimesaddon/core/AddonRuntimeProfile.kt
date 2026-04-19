package com.yshalsager.suntimes.prayertimesaddon.core

import android.content.Context
import com.yshalsager.suntimes.prayertimesaddon.R

data class AddonRuntimeProfile(
    val hijri_variant: String,
    val hijri_day_offset: Int,
    val extra_fajr_1_enabled: Boolean,
    val extra_fajr_1_angle: Double,
    val extra_fajr_1_label_raw: String,
    val extra_isha_1_enabled: Boolean,
    val extra_isha_1_angle: Double,
    val extra_isha_1_label_raw: String
) {
    companion object {
        fun defaults(): AddonRuntimeProfile =
            AddonRuntimeProfile(
                hijri_variant = Prefs.hijri_variant_umalqura,
                hijri_day_offset = 0,
                extra_fajr_1_enabled = false,
                extra_fajr_1_angle = 18.0,
                extra_fajr_1_label_raw = "",
                extra_isha_1_enabled = false,
                extra_isha_1_angle = 18.0,
                extra_isha_1_label_raw = ""
            )
    }

    fun normalized(): AddonRuntimeProfile =
        copy(
            hijri_variant =
                when (hijri_variant) {
                    Prefs.hijri_variant_diyanet -> Prefs.hijri_variant_diyanet
                    else -> Prefs.hijri_variant_umalqura
                },
            hijri_day_offset = hijri_day_offset.coerceIn(-2, 2),
            extra_fajr_1_label_raw = extra_fajr_1_label_raw.trim(),
            extra_isha_1_label_raw = extra_isha_1_label_raw.trim()
        )

    fun extra_fajr_1_label(context: Context): String {
        val raw = extra_fajr_1_label_raw.trim()
        return if (raw.isBlank()) context.getString(R.string.event_prayer_fajr_extra_1) else raw
    }

    fun extra_isha_1_label(context: Context): String {
        val raw = extra_isha_1_label_raw.trim()
        return if (raw.isBlank()) context.getString(R.string.event_prayer_isha_extra_1) else raw
    }
}

fun addon_runtime_profile_from_prefs(context: Context): AddonRuntimeProfile =
    AddonRuntimeProfile(
        hijri_variant = Prefs.get_hijri_variant(context),
        hijri_day_offset = Prefs.get_hijri_day_offset(context),
        extra_fajr_1_enabled = Prefs.get_extra_fajr_1_enabled(context),
        extra_fajr_1_angle = Prefs.get_extra_fajr_1_angle(context),
        extra_fajr_1_label_raw = Prefs.get_extra_fajr_1_label_raw(context),
        extra_isha_1_enabled = Prefs.get_extra_isha_1_enabled(context),
        extra_isha_1_angle = Prefs.get_extra_isha_1_angle(context),
        extra_isha_1_label_raw = Prefs.get_extra_isha_1_label_raw(context)
    ).normalized()
