package com.yshalsager.suntimes.prayertimesaddon.widget

import android.content.Context
import androidx.core.content.edit

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
}
