package com.yshalsager.suntimes.prayertimesaddon.core

import android.content.Context
import androidx.core.content.edit
import androidx.appcompat.app.AppCompatDelegate
import org.json.JSONObject

object SettingsBackup {
    data class ImportResult(
        val ok: Boolean,
        val applied_count: Int,
        val skipped_count: Int
    )

    internal data class ParsedSettings(
        val values: Map<String, Any>,
        val skipped_count: Int
    )

    private const val schema_version = 1

    private val supported_keys = setOf(
        "days_show_prohibited",
        "days_show_night_portions",
        "days_show_hijri",
        "days_month_basis",
        "widget_show_prohibited",
        "widget_show_night_portions",
        "gregorian_date_format",
        "hijri_variant",
        "hijri_day_offset",
        "language",
        "theme",
        "palette",
        "host_event_authority",
        "method_preset",
        "fajr_angle",
        "isha_mode",
        "isha_angle",
        "isha_fixed_minutes",
        "asr_factor",
        "maghrib_offset_minutes",
        "makruh_preset",
        "makruh_angle",
        "makruh_sunrise_minutes",
        "zawal_minutes"
    )

    fun export_json(context: Context): String = encode_json(export_values(context))

    fun import_json(context: Context, raw: String): ImportResult =
        import_json(raw) { apply_values(context, it) }

    internal fun import_json(raw: String, apply_values: (Map<String, Any>) -> Int): ImportResult {
        val parsed = parse_json(raw) ?: return ImportResult(ok = false, applied_count = 0, skipped_count = 0)
        if (parsed.values.isEmpty()) return ImportResult(ok = false, applied_count = 0, skipped_count = parsed.skipped_count)
        val applied_count = apply_values(parsed.values)
        return ImportResult(ok = true, applied_count = applied_count, skipped_count = parsed.skipped_count)
    }

    internal fun encode_json(values: Map<String, Any>): String {
        val prefs = JSONObject()
        values.forEach { (key, value) -> prefs.put(key, value) }
        return JSONObject()
            .put("schema_version", schema_version)
            .put("prefs", prefs)
            .toString()
    }

    internal fun parse_json(raw: String): ParsedSettings? {
        val root = try {
            JSONObject(raw)
        } catch (_: Exception) {
            return null
        }
        if (root.optInt("schema_version", -1) != schema_version) return null
        val prefs = root.optJSONObject("prefs") ?: return null

        val values = linkedMapOf<String, Any>()
        var skipped_count = 0
        val keys = prefs.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            if (key !in supported_keys) continue
            if (prefs.isNull(key)) continue
            val parsed = parse_value(key, prefs.opt(key))
            if (parsed == null) skipped_count += 1 else values[key] = parsed
        }

        return ParsedSettings(values = values, skipped_count = skipped_count)
    }

    private fun export_values(context: Context): Map<String, Any> {
        val values = linkedMapOf<String, Any>(
            "days_show_prohibited" to Prefs.get_days_show_prohibited(context),
            "days_show_night_portions" to Prefs.get_days_show_night_portions(context),
            "days_show_hijri" to Prefs.get_days_show_hijri(context),
            "days_month_basis" to Prefs.get_days_month_basis(context),
            "widget_show_prohibited" to Prefs.get_widget_show_prohibited(context),
            "widget_show_night_portions" to Prefs.get_widget_show_night_portions(context),
            "gregorian_date_format" to Prefs.get_gregorian_date_format(context),
            "hijri_variant" to Prefs.get_hijri_variant(context),
            "hijri_day_offset" to Prefs.get_hijri_day_offset(context),
            "language" to current_app_language(),
            "theme" to Prefs.get_theme(context),
            "palette" to Prefs.get_palette(context),
            "method_preset" to Prefs.get_method_preset(context),
            "fajr_angle" to Prefs.get_fajr_angle(context),
            "isha_mode" to Prefs.get_isha_mode(context),
            "isha_angle" to Prefs.get_isha_angle(context),
            "isha_fixed_minutes" to Prefs.get_isha_fixed_minutes(context),
            "asr_factor" to Prefs.get_asr_factor(context),
            "maghrib_offset_minutes" to Prefs.get_maghrib_offset_minutes(context),
            "makruh_preset" to Prefs.get_makruh_preset(context),
            "makruh_angle" to Prefs.get_makruh_angle(context),
            "makruh_sunrise_minutes" to Prefs.get_makruh_sunrise_minutes(context),
            "zawal_minutes" to Prefs.get_zawal_minutes(context)
        )
        Prefs.get_host_event_authority(context)?.let { values["host_event_authority"] = it }
        return values
    }

    private fun apply_values(context: Context, values: Map<String, Any>): Int {
        context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE).edit {
            values.forEach { (key, value) ->
                if (key == "language") return@forEach
                when (value) {
                    is Boolean -> putBoolean(key, value)
                    else -> putString(key, value.toString())
                }
            }
        }
        (values["language"] as? String)?.let {
            AppCompatDelegate.setApplicationLocales(app_language_locales(it))
        }
        return values.size
    }

    private fun parse_value(key: String, raw_value: Any?): Any? {
        fun parse_bool(value: Any?): Boolean? =
            when (value) {
                is Boolean -> value
                is String -> when (value.lowercase()) {
                    "true" -> true
                    "false" -> false
                    else -> null
                }
                else -> null
            }

        fun parse_int(value: Any?, valid: (Int) -> Boolean = { true }): Int? {
            val parsed = when (value) {
                is Number -> value.toLong().takeIf {
                    it.toDouble() == value.toDouble() && it in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()
                }?.toInt()
                is String -> value.toIntOrNull()
                else -> null
            }
            return parsed?.takeIf(valid)
        }

        fun parse_double(value: Any?): Double? {
            val parsed = when (value) {
                is Number -> value.toDouble()
                is String -> value.toDoubleOrNull()
                else -> null
            }
            return parsed?.takeIf { !it.isNaN() && !it.isInfinite() }
        }

        fun parse_string(value: Any?, valid: (String) -> Boolean = { true }): String? =
            (value as? String)?.takeIf(valid)

        return when (key) {
            "days_show_prohibited",
            "days_show_night_portions",
            "days_show_hijri",
            "widget_show_prohibited",
            "widget_show_night_portions" -> parse_bool(raw_value)

            "days_month_basis" -> parse_string(raw_value) { it == "gregorian" || it == "hijri" }
            "gregorian_date_format" -> parse_string(raw_value) { it == "card" || it == "medium" || it == "long" }
            "hijri_variant" -> parse_string(raw_value) { it == "umalqura" || it == "diyanet" }
            "hijri_day_offset" -> parse_int(raw_value) { it in -2..2 }
            "language" -> parse_string(raw_value) { it == "system" || it == "en" || it == "ar" }
            "theme" -> parse_string(raw_value) { it == "system" || it == "light" || it == "dark" }
            "palette" -> parse_string(raw_value) { it == "parchment" || it == "dynamic" || it == "sapphire" || it == "rose" }
            "host_event_authority" -> parse_string(raw_value) { it.isNotBlank() }
            "method_preset" -> parse_string(raw_value) { it == "egypt" || it == "mwl" || it == "karachi" || it == "isna" || it == "uaq" || it == "custom" }
            "isha_mode" -> parse_string(raw_value) { it == "angle" || it == "fixed" }
            "makruh_preset" -> parse_string(raw_value) { it == "shafi" || it == "hanafi" || it == "custom" }
            "asr_factor" -> parse_int(raw_value) { it == 1 || it == 2 }
            "makruh_sunrise_minutes" -> parse_int(raw_value) { it == 10 || it == 15 || it == 20 }
            "fajr_angle",
            "isha_angle",
            "makruh_angle" -> parse_double(raw_value)

            "isha_fixed_minutes",
            "maghrib_offset_minutes",
            "zawal_minutes" -> parse_int(raw_value)

            else -> null
        }
    }
}
