package com.yshalsager.suntimes.prayertimesaddon.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import com.yshalsager.suntimes.prayertimesaddon.core.AddonEvent
import com.yshalsager.suntimes.prayertimesaddon.core.AddonEventMapper
import com.yshalsager.suntimes.prayertimesaddon.core.AddonEventType
import com.yshalsager.suntimes.prayertimesaddon.core.AlarmEventContract
import com.yshalsager.suntimes.prayertimesaddon.core.HostConfigReader
import com.yshalsager.suntimes.prayertimesaddon.core.HostEventQueries
import com.yshalsager.suntimes.prayertimesaddon.core.HostResolver
import com.yshalsager.suntimes.prayertimesaddon.core.Prefs
import com.yshalsager.suntimes.prayertimesaddon.core.calc_night
import com.yshalsager.suntimes.prayertimesaddon.core.query_host_sun
import java.util.Calendar

class PrayerTimesProvider : ContentProvider() {
    companion object {
        const val authority = "com.yshalsager.suntimes.prayertimesaddon.event.provider"

        private const val match_events = 1
        private const val match_event = 2
        private const val match_calc = 3
        private const val match_types = 4

        private val matcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(authority, AlarmEventContract.query_event_info, match_events)
            addURI(authority, AlarmEventContract.query_event_info + "/*", match_event)
            addURI(authority, AlarmEventContract.query_event_calc + "/*", match_calc)
            addURI(authority, AlarmEventContract.query_event_types, match_types)
        }
    }

    override fun onCreate(): Boolean = true

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0
    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int = 0

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        val ctx = context ?: return null
        return when (matcher.match(uri)) {
            match_types -> query_types(ctx, projection)
            match_events -> query_event_info(ctx, null, projection)
            match_event -> query_event_info(ctx, uri.lastPathSegment, projection)
            match_calc -> query_event_calc(ctx, uri.lastPathSegment, projection, selection, selectionArgs)

            else -> null
        }
    }

    private fun query_types(context: Context, projection: Array<String>?): Cursor {
        val cols = projection ?: AlarmEventContract.query_event_types_projection
        val c = MatrixCursor(cols)
        for (t in AddonEventType.values()) {
            val row = arrayOfNulls<Any>(cols.size)
            cols.indices.forEach { i ->
                row[i] = when (cols[i]) {
                    AlarmEventContract.column_event_type -> t.type_id
                    AlarmEventContract.column_event_type_label -> context.getString(t.label_res)
                    else -> null
                }
            }
            c.addRow(row)
        }
        return c
    }

    private fun query_event_info(
        context: Context,
        event_id: String?,
        projection: Array<String>?
    ): Cursor {
        val cols = projection ?: AlarmEventContract.query_event_info_projection
        val c = MatrixCursor(cols)
        for (e in AddonEvent.values()) {
            if (event_id == null || e.event_id == event_id) c.addRow(event_info_row(context, cols, e))
        }
        return c
    }

    private fun event_info_row(context: Context, cols: Array<String>, e: AddonEvent): Array<Any?> {
        val title = context.getString(e.title_res)
        val row = arrayOfNulls<Any>(cols.size)
        for (i in cols.indices) {
            row[i] = when (cols[i]) {
                AlarmEventContract.column_config_provider -> authority
                AlarmEventContract.column_event_name -> e.event_id
                AlarmEventContract.column_event_title -> title
                AlarmEventContract.column_event_summary -> null
                AlarmEventContract.column_event_phrase -> title
                AlarmEventContract.column_event_phrase_gender -> "other"
                AlarmEventContract.column_event_phrase_quantity -> 1
                AlarmEventContract.column_event_supports_repeating -> AlarmEventContract.repeat_support_daily
                AlarmEventContract.column_event_supports_offsetdays -> "false"
                AlarmEventContract.column_event_requires_location -> "true"
                AlarmEventContract.column_event_type -> e.type.type_id
                AlarmEventContract.column_event_type_label -> context.getString(e.type.label_res)
                else -> null
            }
        }
        return row
    }

    private fun query_event_calc(
        context: Context,
        addon_event_id: String?,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?
    ): Cursor {
        val cols = projection ?: AlarmEventContract.query_event_calc_projection
        val c = MatrixCursor(cols)

        val selected = HostResolver.ensure_default_selected(context) ?: return c
        val addon_event = AddonEvent.values().firstOrNull { it.event_id == addon_event_id } ?: return c

        if (addon_event.type == AddonEventType.night) {
            val t = calc_night_event_time(context, selected, addon_event, selection, selectionArgs)
            if (t != null) c.addRow(calc_row(cols, addon_event, t))
            return c
        }

        val host_query = AddonEventMapper.map_event(context, addon_event) ?: return c

        var t: Long? = null
        if (addon_event == AddonEvent.prayer_asr || addon_event == AddonEvent.makruh_after_asr_start) {
            t = HostEventQueries.query_asr_time(context, selected, selection, selectionArgs)
        } else {
            t = HostEventQueries.query_host_event_time(context, selected, host_query.base_event_id, host_query.delta_millis, selection, selectionArgs)
        }

        if (t != null) c.addRow(calc_row(cols, addon_event, t))

        return c
    }

    private fun calc_row(cols: Array<String>, addon_event: AddonEvent, event_time: Long): Array<Any?> {
        val row = arrayOfNulls<Any>(cols.size)
        for (i in cols.indices) {
            row[i] = when (cols[i]) {
                AlarmEventContract.column_event_name -> addon_event.event_id
                AlarmEventContract.column_event_timemillis -> event_time
                else -> null
            }
        }
        return row
    }

    private fun calc_night_event_time(
        context: Context,
        host_event_authority: String,
        addon_event: AddonEvent,
        selection: String?,
        selectionArgs: Array<String>?
    ): Long? {
        val now = selectionArgs?.getOrNull(0)?.toLongOrNull() ?: System.currentTimeMillis()
        val fajr_query = AddonEventMapper.map_event(context, AddonEvent.prayer_fajr) ?: return null
        val tz =
            HostConfigReader.read_config(context, host_event_authority)?.timezone?.let(java.util.TimeZone::getTimeZone)
                ?: java.util.TimeZone.getDefault()

        fun with_now(v: Long): Array<String>? {
            val a = selectionArgs?.clone() ?: return null
            if (a.isEmpty()) return null
            a[0] = v.toString()
            return a
        }

        var fajr_alarm_now = now
        repeat(2) { attempt ->
            val fajr =
                HostEventQueries.query_host_event_time(
                    context,
                    host_event_authority,
                    fajr_query.base_event_id,
                    fajr_query.delta_millis,
                    selection,
                    with_now(fajr_alarm_now) ?: selectionArgs
                ) ?: return null
            val maghrib_day_start = Calendar.getInstance(tz).run {
                timeInMillis = fajr
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.DAY_OF_YEAR, -1)
                timeInMillis
            }
            val sunset = query_host_sun(context, host_event_authority, maghrib_day_start)?.sunset ?: return null
            val maghrib = sunset + Prefs.get_maghrib_offset_minutes(context) * 60_000L
            val night = calc_night(maghrib, fajr) ?: return null

            val t = when (addon_event) {
                AddonEvent.night_midpoint -> night.midpoint
                AddonEvent.night_last_third -> night.last_third
                AddonEvent.night_last_sixth -> night.last_sixth
                else -> return null
            }

            if (t >= now || attempt == 1) return t
            fajr_alarm_now = fajr + 60_000L
        }

        return null
    }
}
