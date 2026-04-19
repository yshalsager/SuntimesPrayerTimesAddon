package com.yshalsager.suntimes.prayertimesaddon.core

import android.content.Context
import com.yshalsager.suntimes.prayertimesaddon.R

fun format_method_summary(context: Context, method_config: MethodConfig? = null): String {
    val cfg = method_config ?: method_config_from_prefs(context)
    val preset = cfg.method_preset
    val name =
        when (preset) {
            "egypt" -> context.getString(R.string.method_egypt)
            "mwl" -> context.getString(R.string.method_mwl)
            "karachi" -> context.getString(R.string.method_karachi)
            "isna" -> context.getString(R.string.method_isna)
            "uaq" -> context.getString(R.string.method_uaq)
            "uiof" -> context.getString(R.string.method_uiof)
            else -> context.getString(R.string.method_custom)
        }

    val fajr = cfg.fajr_angle
    val isha =
        if (cfg.isha_mode == Prefs.isha_mode_fixed) {
            "+${cfg.isha_fixed_minutes}${context.getString(R.string.minute_abbrev)}"
        } else {
            trim_angle(cfg.isha_angle)
        }

    return "$name ${trim_angle(fajr)}/$isha"
}

private fun trim_angle(v: Double): String {
    val i = v.toInt()
    return if (v == i.toDouble()) i.toString() else v.toString()
}
