package com.yshalsager.suntimes.prayertimesaddon.core

import android.content.Context
import com.yshalsager.suntimes.prayertimesaddon.R

fun format_method_summary(context: Context): String {
    val preset = Prefs.get_method_preset(context)
    val name =
        when (preset) {
            "egypt" -> context.getString(R.string.method_egypt)
            "mwl" -> context.getString(R.string.method_mwl)
            "karachi" -> context.getString(R.string.method_karachi)
            "isna" -> context.getString(R.string.method_isna)
            "uaq" -> context.getString(R.string.method_uaq)
            else -> context.getString(R.string.method_custom)
        }

    val fajr = Prefs.get_fajr_angle(context)
    val isha =
        if (Prefs.get_isha_mode(context) == Prefs.isha_mode_fixed) {
            "+${Prefs.get_isha_fixed_minutes(context)}${context.getString(R.string.minute_abbrev)}"
        } else {
            trim_angle(Prefs.get_isha_angle(context))
        }

    return "$name ${trim_angle(fajr)}/$isha"
}

private fun trim_angle(v: Double): String {
    val i = v.toInt()
    return if (v == i.toDouble()) i.toString() else v.toString()
}

