package com.yshalsager.suntimes.prayertimesaddon.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import androidx.core.content.ContextCompat
import com.yshalsager.suntimes.prayertimesaddon.R
import com.yshalsager.suntimes.prayertimesaddon.core.LocationQueryContext
import com.yshalsager.suntimes.prayertimesaddon.core.resolve_location_query_context
import com.yshalsager.suntimes.prayertimesaddon.core.PrayerTimesCalendarContract
import com.yshalsager.suntimes.prayertimesaddon.core.PrayerTimesCalendarSource
import com.yshalsager.suntimes.prayertimesaddon.core.query_prayer_times_calendar_events
import com.yshalsager.suntimes.prayertimesaddon.core.resolve_prayer_times_calendar_meta

class PrayerTimesCalendarProvider : ContentProvider() {
    companion object {
        val authority = PrayerTimesCalendarContract.authority
    }

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        val ctx = context ?: return null
        val source = uri.pathSegments.getOrNull(0)?.let(PrayerTimesCalendarSource::from_id) ?: return null
        val location_context =
            resolve_location_query_context(
                context = ctx,
                saved_location_id = uri.getQueryParameter(PrayerTimesCalendarContract.param_saved_location_id),
                latitude = selectionArgs?.getOrNull(4),
                longitude = selectionArgs?.getOrNull(5),
                altitude = selectionArgs?.getOrNull(6)
            )
        return when (uri.pathSegments.getOrNull(1)) {
            PrayerTimesCalendarContract.query_calendar_info -> query_calendar_info(ctx, source, projection, location_context)
            PrayerTimesCalendarContract.query_calendar_template_strings -> MatrixCursor(projection ?: PrayerTimesCalendarContract.query_calendar_template_strings_projection)
            PrayerTimesCalendarContract.query_calendar_template_flags -> MatrixCursor(projection ?: PrayerTimesCalendarContract.query_calendar_template_flags_projection)
            PrayerTimesCalendarContract.query_calendar_content -> query_calendar_content(ctx, source, projection, uri.pathSegments.getOrNull(2), location_context)
            else -> null
        }
    }

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int = 0

    private fun query_calendar_info(
        context: android.content.Context,
        source: PrayerTimesCalendarSource,
        projection: Array<String>?,
        location_context: LocationQueryContext
    ): Cursor {
        val cols = projection ?: PrayerTimesCalendarContract.query_calendar_info_projection
        val c = MatrixCursor(cols)
        val meta = resolve_prayer_times_calendar_meta(context, source, location_context)
        val summary = meta?.summary ?: context.getString(R.string.no_host_found)
        val color = meta?.color ?: ContextCompat.getColor(context, source.color_res)

        val row = arrayOfNulls<Any>(cols.size)
        cols.indices.forEach { i ->
            row[i] =
                when (cols[i]) {
                    PrayerTimesCalendarContract.column_calendar_name -> source.id
                    PrayerTimesCalendarContract.column_calendar_title -> context.getString(source.title_res)
                    PrayerTimesCalendarContract.column_calendar_summary -> summary
                    PrayerTimesCalendarContract.column_calendar_color -> color
                    PrayerTimesCalendarContract.column_calendar_template_title -> null
                    PrayerTimesCalendarContract.column_calendar_template_description -> null
                    PrayerTimesCalendarContract.column_calendar_template_location -> null
                    else -> null
                }
        }
        c.addRow(row)
        return c
    }

    private fun query_calendar_content(
        context: android.content.Context,
        source: PrayerTimesCalendarSource,
        projection: Array<String>?,
        range_segment: String?,
        location_context: LocationQueryContext
    ): Cursor {
        val cols = projection ?: PrayerTimesCalendarContract.query_calendar_content_projection
        val c = MatrixCursor(cols)
        val (window_start, window_end) = parse_window(range_segment) ?: return c

        query_prayer_times_calendar_events(context, source, window_start, window_end, location_context).forEach { event ->
            val row = arrayOfNulls<Any>(cols.size)
            cols.indices.forEach { i ->
                row[i] =
                    when (cols[i]) {
                        android.provider.CalendarContract.Events.TITLE -> event.title
                        android.provider.CalendarContract.Events.DESCRIPTION -> event.description
                        android.provider.CalendarContract.Events.EVENT_TIMEZONE -> event.event_timezone
                        android.provider.CalendarContract.Events.DTSTART -> event.dtstart
                        android.provider.CalendarContract.Events.DTEND -> event.dtend
                        android.provider.CalendarContract.Events.EVENT_LOCATION -> event.event_location
                        else -> null
                    }
            }
            c.addRow(row)
        }

        return c
    }

    private fun parse_window(range_segment: String?): Pair<Long, Long>? {
        val (start, end) = range_segment?.split('-', limit = 2)?.takeIf { it.size == 2 } ?: return null
        val window_start = start.toLongOrNull() ?: return null
        val window_end = end.toLongOrNull() ?: return null
        return if (window_end > window_start) window_start to window_end else null
    }
}
