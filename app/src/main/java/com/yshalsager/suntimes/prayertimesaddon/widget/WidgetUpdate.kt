package com.yshalsager.suntimes.prayertimesaddon.widget

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.appwidget.AppWidgetManager

object WidgetUpdate {
    fun request(context: Context) {
        val mgr = AppWidgetManager.getInstance(context)
        val cn = ComponentName(context, PrayerTimesWidgetProvider::class.java)
        val ids = mgr.getAppWidgetIds(cn)
        if (ids.isEmpty()) return

        val intent =
            Intent(context, PrayerTimesWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }

        context.sendBroadcast(intent)
    }
}

