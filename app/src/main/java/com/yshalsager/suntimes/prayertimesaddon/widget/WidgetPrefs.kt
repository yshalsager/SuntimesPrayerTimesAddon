package com.yshalsager.suntimes.prayertimesaddon.widget

import android.content.Context
import androidx.core.content.edit
import java.util.UUID

object WidgetPrefs {
    private fun sp(context: Context) = context.getSharedPreferences("${context.packageName}_widget", Context.MODE_PRIVATE)
    private fun key_saved_location_id(app_widget_id: Int) = "widget_saved_location_id_$app_widget_id"

    fun get_saved_location_id(context: Context, app_widget_id: Int): String? =
        sp(context).getString(key_saved_location_id(app_widget_id), null)?.trim()?.ifBlank { null }

    fun set_saved_location_id(context: Context, app_widget_id: Int, saved_location_id: String?) {
        val normalized = saved_location_id?.trim()?.ifBlank { null }
        sp(context).edit {
            if (normalized == null) remove(key_saved_location_id(app_widget_id))
            else putString(key_saved_location_id(app_widget_id), normalized)
        }
    }

    fun clear_saved_location_ids(context: Context, app_widget_ids: IntArray) {
        if (app_widget_ids.isEmpty()) return
        sp(context).edit {
            app_widget_ids.forEach { remove(key_saved_location_id(it)) }
        }
    }

    fun alarm_token(context: Context): String {
        val prefs = context.getSharedPreferences(transient_prefs, Context.MODE_PRIVATE)
        val existing = prefs.getString(alarm_token_key, null)
        if (!existing.isNullOrBlank()) return existing
        val legacy = sp(context).getString(alarm_token_key, null)?.takeIf { it.isNotBlank() }
        if (legacy != null) {
            migrate_alarm_token(context)
            return legacy
        }
        return UUID.randomUUID().toString().also { prefs.edit { putString(alarm_token_key, it) } }
    }

    fun migrate_alarm_token(context: Context): Boolean {
        val legacy = sp(context)
        val token = legacy.getString(alarm_token_key, null)?.takeIf { it.isNotBlank() }
        if (token != null && !context.getSharedPreferences(transient_prefs, Context.MODE_PRIVATE).edit().putString(alarm_token_key, token).commit()) return false
        return legacy.edit().remove(alarm_token_key).commit()
    }

    private const val alarm_token_key = "widget_alarm_token"
    private const val transient_prefs = "widget_transient"
}
