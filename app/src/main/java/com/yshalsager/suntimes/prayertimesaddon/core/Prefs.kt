package com.yshalsager.suntimes.prayertimesaddon.core

import android.content.Context
import androidx.core.content.edit
import com.yshalsager.suntimes.prayertimesaddon.R

object Prefs {
    const val isha_mode_angle = "angle"
    const val isha_mode_fixed = "fixed"

    const val theme_system = "system"
    const val theme_light = "light"
    const val theme_dark = "dark"

    const val palette_parchment = "parchment"
    const val palette_dynamic = "dynamic"
    const val palette_sapphire = "sapphire"
    const val palette_rose = "rose"

    const val hijri_variant_umalqura = "umalqura"
    const val hijri_variant_diyanet = "diyanet"
    const val days_month_basis_gregorian = "gregorian"
    const val days_month_basis_hijri = "hijri"
    const val gregorian_date_format_card = "card"
    const val gregorian_date_format_medium = "medium"
    const val gregorian_date_format_long = "long"

    private const val k_days_show_prohibited = "days_show_prohibited"
    private const val k_days_show_night_portions = "days_show_night_portions"
    private const val k_days_show_hijri = "days_show_hijri"
    private const val k_days_month_basis = "days_month_basis"
    private const val k_widget_show_prohibited = "widget_show_prohibited"
    private const val k_widget_show_night_portions = "widget_show_night_portions"
    private const val k_gregorian_date_format = "gregorian_date_format"
    private const val k_hijri_variant = "hijri_variant"
    private const val k_hijri_day_offset = "hijri_day_offset"
    private const val k_theme = "theme"
    private const val k_palette = "palette"
    private const val k_host_event_authority = "host_event_authority"
    private const val k_method_preset = "method_preset"
    private const val k_fajr_angle = "fajr_angle"
    private const val k_extra_fajr_1_enabled = "extra_fajr_1_enabled"
    private const val k_extra_fajr_1_angle = "extra_fajr_1_angle"
    private const val k_extra_fajr_1_label = "extra_fajr_1_label"
    private const val k_isha_mode = "isha_mode"
    private const val k_isha_angle = "isha_angle"
    private const val k_isha_fixed_minutes = "isha_fixed_minutes"
    private const val k_extra_isha_1_enabled = "extra_isha_1_enabled"
    private const val k_extra_isha_1_angle = "extra_isha_1_angle"
    private const val k_extra_isha_1_label = "extra_isha_1_label"
    private const val k_asr_factor = "asr_factor"
    private const val k_maghrib_offset_minutes = "maghrib_offset_minutes"
    private const val k_makruh_preset = "makruh_preset"
    private const val k_makruh_angle = "makruh_angle"
    private const val k_makruh_sunrise_minutes = "makruh_sunrise_minutes"
    private const val k_zawal_minutes = "zawal_minutes"
    private const val k_saved_locations_json = "saved_locations_json"
    private const val k_home_location_source = "home_location_source"
    private const val k_home_location_id = "home_location_id"

    private fun sp(context: Context) = context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)

    private fun get_str(context: Context, key: String, def: String): String = sp(context).getString(key, def) ?: def
    private fun put_str(context: Context, key: String, v: String) = sp(context).edit {putString(key, v)}
    private fun put_bool(context: Context, key: String, v: Boolean) = sp(context).edit {
        putBoolean(
            key,
            v
        )
    }
    private fun get_double(context: Context, key: String, def: Double): Double = get_str(context, key, def.toString()).toDoubleOrNull() ?: def
    private fun get_int(context: Context, key: String, def: Int): Int = get_str(context, key, def.toString()).toIntOrNull() ?: def

    fun get_days_show_prohibited(context: Context): Boolean =
        sp(context).getBoolean(k_days_show_prohibited, true)

    fun set_days_show_prohibited(context: Context, v: Boolean) =
        put_bool(context, k_days_show_prohibited, v)

    fun get_days_show_night_portions(context: Context): Boolean =
        sp(context).getBoolean(k_days_show_night_portions, true)

    fun set_days_show_night_portions(context: Context, v: Boolean) =
        put_bool(context, k_days_show_night_portions, v)

    fun get_days_show_hijri(context: Context): Boolean =
        sp(context).getBoolean(k_days_show_hijri, true)

    fun set_days_show_hijri(context: Context, v: Boolean) =
        put_bool(context, k_days_show_hijri, v)

    fun get_days_month_basis(context: Context): String =
        get_str(context, k_days_month_basis, days_month_basis_hijri)

    fun set_days_month_basis(context: Context, v: String) =
        put_str(context, k_days_month_basis, v)

    fun get_widget_show_prohibited(context: Context): Boolean =
        sp(context).getBoolean(k_widget_show_prohibited, true)

    fun set_widget_show_prohibited(context: Context, v: Boolean) =
        put_bool(context, k_widget_show_prohibited, v)

    fun get_widget_show_night_portions(context: Context): Boolean =
        sp(context).getBoolean(k_widget_show_night_portions, true)

    fun set_widget_show_night_portions(context: Context, v: Boolean) =
        put_bool(context, k_widget_show_night_portions, v)

    fun get_gregorian_date_format(context: Context): String =
        get_str(context, k_gregorian_date_format, gregorian_date_format_card)

    fun set_gregorian_date_format(context: Context, v: String) =
        put_str(context, k_gregorian_date_format, v)

    fun get_hijri_variant(context: Context): String =
        get_str(context, k_hijri_variant, hijri_variant_umalqura)

    fun set_hijri_variant(context: Context, v: String) =
        put_str(context, k_hijri_variant, v)

    fun get_hijri_day_offset(context: Context): Int =
        get_int(context, k_hijri_day_offset, 0).coerceIn(-2, 2)

    fun set_hijri_day_offset(context: Context, v: Int) =
        put_str(context, k_hijri_day_offset, v.coerceIn(-2, 2).toString())

    fun get_theme(context: Context): String =
        get_str(context, k_theme, theme_system)

    fun set_theme(context: Context, v: String) =
        put_str(context, k_theme, v)

    fun get_palette(context: Context): String =
        get_str(context, k_palette, palette_parchment)

    fun set_palette(context: Context, v: String) =
        put_str(context, k_palette, v)

    fun get_host_event_authority(context: Context): String? =
        sp(context).getString(k_host_event_authority, null)

    fun set_host_event_authority(context: Context, authority: String) =
        put_str(context, k_host_event_authority, authority)

    fun get_method_preset(context: Context): String =
        get_str(context, k_method_preset, "egypt")

    fun set_method_preset(context: Context, preset: String) =
        put_str(context, k_method_preset, preset)

    fun get_fajr_angle(context: Context): Double =
        get_double(context, k_fajr_angle, 19.5)

    fun set_fajr_angle(context: Context, angle: Double) =
        put_str(context, k_fajr_angle, angle.toString())

    fun get_extra_fajr_1_enabled(context: Context): Boolean =
        sp(context).getBoolean(k_extra_fajr_1_enabled, false)

    fun set_extra_fajr_1_enabled(context: Context, enabled: Boolean) =
        put_bool(context, k_extra_fajr_1_enabled, enabled)

    fun get_extra_fajr_1_angle(context: Context): Double =
        get_double(context, k_extra_fajr_1_angle, 18.0)

    fun set_extra_fajr_1_angle(context: Context, angle: Double) =
        put_str(context, k_extra_fajr_1_angle, angle.toString())

    fun get_extra_fajr_1_label_raw(context: Context): String =
        sp(context).getString(k_extra_fajr_1_label, null)?.trim().orEmpty()

    fun get_extra_fajr_1_label(context: Context): String {
        val stored = get_extra_fajr_1_label_raw(context)
        return if (stored.isBlank()) context.getString(R.string.event_prayer_fajr_extra_1) else stored
    }

    fun set_extra_fajr_1_label(context: Context, label: String) =
        put_str(context, k_extra_fajr_1_label, label.trim())

    fun get_isha_mode(context: Context): String =
        get_str(context, k_isha_mode, isha_mode_angle)

    fun set_isha_mode(context: Context, mode: String) =
        put_str(context, k_isha_mode, mode)

    fun get_isha_angle(context: Context): Double =
        get_double(context, k_isha_angle, 17.5)

    fun set_isha_angle(context: Context, angle: Double) =
        put_str(context, k_isha_angle, angle.toString())

    fun get_isha_fixed_minutes(context: Context): Int =
        get_int(context, k_isha_fixed_minutes, 90)

    fun set_isha_fixed_minutes(context: Context, minutes: Int) =
        put_str(context, k_isha_fixed_minutes, minutes.toString())

    fun get_extra_isha_1_enabled(context: Context): Boolean =
        sp(context).getBoolean(k_extra_isha_1_enabled, false)

    fun set_extra_isha_1_enabled(context: Context, enabled: Boolean) =
        put_bool(context, k_extra_isha_1_enabled, enabled)

    fun get_extra_isha_1_angle(context: Context): Double =
        get_double(context, k_extra_isha_1_angle, 18.0)

    fun set_extra_isha_1_angle(context: Context, angle: Double) =
        put_str(context, k_extra_isha_1_angle, angle.toString())

    fun get_extra_isha_1_label_raw(context: Context): String =
        sp(context).getString(k_extra_isha_1_label, null)?.trim().orEmpty()

    fun get_extra_isha_1_label(context: Context): String {
        val stored = get_extra_isha_1_label_raw(context)
        return if (stored.isBlank()) context.getString(R.string.event_prayer_isha_extra_1) else stored
    }

    fun set_extra_isha_1_label(context: Context, label: String) =
        put_str(context, k_extra_isha_1_label, label.trim())

    fun get_asr_factor(context: Context): Int =
        get_int(context, k_asr_factor, 1)

    fun set_asr_factor(context: Context, factor: Int) =
        put_str(context, k_asr_factor, factor.toString())

    fun get_maghrib_offset_minutes(context: Context): Int =
        get_int(context, k_maghrib_offset_minutes, 0)

    fun set_maghrib_offset_minutes(context: Context, minutes: Int) =
        put_str(context, k_maghrib_offset_minutes, minutes.toString())

    fun get_makruh_preset(context: Context): String =
        get_str(context, k_makruh_preset, "shafi")

    fun set_makruh_preset(context: Context, preset: String) =
        put_str(context, k_makruh_preset, preset)

    fun get_makruh_angle(context: Context): Double =
        get_double(context, k_makruh_angle, 5.0)

    fun set_makruh_angle(context: Context, angle: Double) =
        put_str(context, k_makruh_angle, angle.toString())

    fun get_makruh_sunrise_minutes(context: Context): Int {
        return when (val minutes = get_int(context, k_makruh_sunrise_minutes, 15)) {
            10, 15, 20 -> minutes
            else -> 15
        }
    }

    fun set_makruh_sunrise_minutes(context: Context, minutes: Int) {
        val v = when (minutes) {
            10, 15, 20 -> minutes
            else -> 15
        }
        put_str(context, k_makruh_sunrise_minutes, v.toString())
    }

    fun get_zawal_minutes(context: Context): Int =
        get_int(context, k_zawal_minutes, 10)

    fun set_zawal_minutes(context: Context, minutes: Int) =
        put_str(context, k_zawal_minutes, minutes.toString())

    fun get_saved_locations_json(context: Context): String =
        get_str(context, k_saved_locations_json, "[]")

    fun set_saved_locations_json(context: Context, raw: String) =
        put_str(context, k_saved_locations_json, raw)

    fun get_home_location_source(context: Context): String {
        val stored = get_str(context, k_home_location_source, SavedLocations.home_source_host)
        return if (stored == SavedLocations.home_source_saved) stored else SavedLocations.home_source_host
    }

    fun set_home_location_source(context: Context, source: String) =
        put_str(
            context,
            k_home_location_source,
            if (source == SavedLocations.home_source_saved) SavedLocations.home_source_saved else SavedLocations.home_source_host
        )

    fun get_home_location_id(context: Context): String =
        get_str(context, k_home_location_id, "").trim()

    fun set_home_location_id(context: Context, id: String?) =
        put_str(context, k_home_location_id, id?.trim().orEmpty())

    fun apply_method_preset(context: Context, preset: String) {
        when (preset) {
            "egypt" -> {
                set_fajr_angle(context, 19.5)
                set_isha_mode(context, isha_mode_angle)
                set_isha_angle(context, 17.5)
            }

            "mwl" -> {
                set_fajr_angle(context, 18.0)
                set_isha_mode(context, isha_mode_angle)
                set_isha_angle(context, 17.0)
            }

            "karachi" -> {
                set_fajr_angle(context, 18.0)
                set_isha_mode(context, isha_mode_angle)
                set_isha_angle(context, 18.0)
            }

            "isna" -> {
                set_fajr_angle(context, 15.0)
                set_isha_mode(context, isha_mode_angle)
                set_isha_angle(context, 15.0)
            }

            "uaq" -> {
                set_fajr_angle(context, 18.5)
                set_isha_mode(context, isha_mode_fixed)
                set_isha_fixed_minutes(context, 90)
            }

            "uiof" -> {
                set_fajr_angle(context, 12.0)
                set_isha_mode(context, isha_mode_angle)
                set_isha_angle(context, 12.0)
            }
        }
    }

    fun apply_makruh_preset(context: Context, preset: String) {
        when (preset) {
            "shafi" -> {
                set_makruh_angle(context, 5.0)
                set_zawal_minutes(context, 10)
            }

            "hanafi" -> {
                set_makruh_angle(context, 5.0)
                set_zawal_minutes(context, 5)
            }
        }
    }
}
