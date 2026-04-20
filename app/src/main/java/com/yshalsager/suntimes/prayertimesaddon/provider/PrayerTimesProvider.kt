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
import com.yshalsager.suntimes.prayertimesaddon.core.AddonRuntimeProfile
import com.yshalsager.suntimes.prayertimesaddon.core.AddonEventType
import com.yshalsager.suntimes.prayertimesaddon.core.AlarmEventContract
import com.yshalsager.suntimes.prayertimesaddon.core.AppIds
import com.yshalsager.suntimes.prayertimesaddon.core.HostConfigReader
import com.yshalsager.suntimes.prayertimesaddon.core.HostEventQueries
import com.yshalsager.suntimes.prayertimesaddon.core.HostResolver
import com.yshalsager.suntimes.prayertimesaddon.core.MethodConfig
import com.yshalsager.suntimes.prayertimesaddon.core.Prefs
import com.yshalsager.suntimes.prayertimesaddon.core.addon_event_title
import com.yshalsager.suntimes.prayertimesaddon.core.calc_night
import com.yshalsager.suntimes.prayertimesaddon.core.day_start_at
import com.yshalsager.suntimes.prayertimesaddon.core.find_next_eid_day_start
import com.yshalsager.suntimes.prayertimesaddon.core.is_eid_event
import com.yshalsager.suntimes.prayertimesaddon.core.is_addon_event_enabled
import com.yshalsager.suntimes.prayertimesaddon.core.query_host_eid_time
import com.yshalsager.suntimes.prayertimesaddon.core.query_host_sun
import com.yshalsager.suntimes.prayertimesaddon.core.resolve_location_query_context
import com.yshalsager.suntimes.prayertimesaddon.core.visible_addon_events
import java.util.Calendar
import java.util.TimeZone

class PrayerTimesProvider : ContentProvider() {
    private data class SelectionLocationArgs(
        val latitude: String?,
        val longitude: String?,
        val altitude: String?
    )

    companion object {
        val authority = AppIds.event_provider_authority

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
            match_events -> query_event_info(ctx, uri, null, projection)
            match_event -> query_event_info(ctx, uri, uri.lastPathSegment, projection)
            match_calc -> query_event_calc(ctx, uri, uri.lastPathSegment, projection, selection, selectionArgs)

            else -> null
        }
    }

    private fun query_types(context: Context, projection: Array<String>?): Cursor {
        val cols = projection ?: AlarmEventContract.query_event_types_projection
        val c = MatrixCursor(cols)
        for (t in AddonEventType.entries) {
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
        uri: Uri,
        event_id: String?,
        projection: Array<String>?
    ): Cursor {
        val cols = projection ?: AlarmEventContract.query_event_info_projection
        val c = MatrixCursor(cols)
        val location_context =
            resolve_location_query_context(
                context = context,
                saved_location_id = uri.getQueryParameter(AlarmEventContract.extra_saved_location_id),
                latitude = null,
                longitude = null,
                altitude = null
            )
        val runtime_profile = location_context.addon_runtime_profile_override
        for (e in visible_addon_events(context, runtime_profile)) {
            if (event_id == null || e.event_id == event_id) c.addRow(event_info_row(context, cols, e, runtime_profile))
        }
        return c
    }

    private fun event_info_row(
        context: Context,
        cols: Array<String>,
        e: AddonEvent,
        runtime_profile: AddonRuntimeProfile?
    ): Array<Any?> {
        val title = addon_event_title(context, e, runtime_profile)
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
        uri: Uri,
        addon_event_id: String?,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?
    ): Cursor {
        val cols = projection ?: AlarmEventContract.query_event_calc_projection
        val c = MatrixCursor(cols)

        val selected = HostResolver.ensure_default_selected(context) ?: return c
        val addon_event = AddonEvent.entries.firstOrNull { it.event_id == addon_event_id } ?: return c
        val selection_location = selection_location_from_args(selectionArgs)
        val location_context =
            resolve_location_query_context(
                context = context,
                saved_location_id = uri.getQueryParameter(AlarmEventContract.extra_saved_location_id),
                latitude = selection_location.latitude,
                longitude = selection_location.longitude,
                altitude = selection_location.altitude
            )
        val method_override = location_context.method_config_override
        val runtime_profile = location_context.addon_runtime_profile_override
        val timezone_override = location_context.timezone_override
        val alarm_now = selectionArgs?.getOrNull(0)?.toLongOrNull() ?: System.currentTimeMillis()
        val resolved_selection = location_context.selection_for_alarm_now(alarm_now, selection, selectionArgs)
        val effective_selection = resolved_selection.first
        val effective_selection_args = resolved_selection.second
        if (!is_addon_event_enabled(context, addon_event, runtime_profile)) return c

        if (addon_event.type == AddonEventType.night) {
            val t =
                calc_night_event_time(
                    context,
                    selected,
                    addon_event,
                    effective_selection,
                    effective_selection_args,
                    method_override,
                    timezone_override
                )
            if (t != null) c.addRow(calc_row(cols, addon_event, t))
            return c
        }

        if (addon_event.is_eid_event()) {
            val t =
                calc_eid_event_time(
                    context,
                    selected,
                    addon_event,
                    alarm_now,
                    effective_selection,
                    effective_selection_args,
                    timezone_override,
                    runtime_profile
                )
            if (t != null) c.addRow(calc_row(cols, addon_event, t))
            return c
        }

        val t =
            if (addon_event == AddonEvent.prayer_asr || addon_event == AddonEvent.makruh_after_asr_start) {
                HostEventQueries.query_asr_time(
                    context,
                    selected,
                    effective_selection,
                    effective_selection_args,
                    latitude_override = location_context.latitude_override,
                    asr_factor_override = method_override?.asr_factor
                )
            } else {
                val host_query = AddonEventMapper.map_event(context, addon_event, method_override, runtime_profile) ?: return c
                HostEventQueries.query_host_event_time(
                    context,
                    selected,
                    host_query.base_event_id,
                    host_query.delta_millis,
                    effective_selection,
                    effective_selection_args
                )
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
        selectionArgs: Array<String>?,
        method_override: MethodConfig?,
        timezone_override: TimeZone?
    ): Long? {
        val now = selectionArgs?.getOrNull(0)?.toLongOrNull() ?: System.currentTimeMillis()
        val alarm_offset = selectionArgs?.getOrNull(1)?.toLongOrNull() ?: 0L
        val fajr_query = AddonEventMapper.map_event(context, AddonEvent.prayer_fajr, method_override) ?: return null
        val tz =
            timezone_override
                ?: HostConfigReader.read_config(context, host_event_authority)?.timezone?.let(java.util.TimeZone::getTimeZone)
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
            val sunset =
                query_host_sun(
                    context,
                    host_event_authority,
                    maghrib_day_start,
                    selection,
                    with_now(maghrib_day_start) ?: selectionArgs
                )?.sunset ?: return null
            val maghrib = sunset + (method_override?.maghrib_offset_minutes ?: Prefs.get_maghrib_offset_minutes(context)) * 60_000L
            val night = calc_night(maghrib, fajr) ?: return null

            val t = when (addon_event) {
                AddonEvent.night_midpoint -> night.midpoint
                AddonEvent.night_last_third -> night.last_third
                AddonEvent.night_last_sixth -> night.last_sixth
                else -> return null
            }

            if (t + alarm_offset >= now || attempt == 1) return t
            fajr_alarm_now = fajr + 60_000L
        }

        return null
    }

    private fun calc_eid_event_time(
        context: Context,
        host_event_authority: String,
        addon_event: AddonEvent,
        alarm_now: Long,
        selection: String?,
        selectionArgs: Array<String>?,
        timezone_override: TimeZone?,
        runtime_profile: AddonRuntimeProfile?
    ): Long? {
        val alarm_offset = selectionArgs?.getOrNull(1)?.toLongOrNull() ?: 0L
        val tz =
            timezone_override
                ?: HostConfigReader.read_config(context, host_event_authority)?.timezone?.let(java.util.TimeZone::getTimeZone)
                ?: java.util.TimeZone.getDefault()
        val current_day_start = day_start_at(alarm_now, tz)

        fun args_for(day_start: Long): Array<String>? {
            val args = selectionArgs?.clone() ?: return null
            if (args.isNotEmpty()) args[0] = day_start.toString()
            return args
        }

        fun time_for_day(day_start: Long): Long? =
            query_host_eid_time(
                context = context,
                host_event_authority = host_event_authority,
                event = addon_event,
                alarm_now = day_start,
                selection = selection,
                selection_args = args_for(day_start) ?: selectionArgs,
                timezone_override = timezone_override,
                addon_runtime_profile_override = runtime_profile
            )

        val today_time = time_for_day(current_day_start)
        if (today_time != null && today_time + alarm_offset >= alarm_now) return today_time

        val next_day_start =
            find_next_eid_day_start(
                context = context,
                host_event_authority = host_event_authority,
                event = addon_event,
                from_day_start = current_day_start,
                timezone_override = timezone_override,
                addon_runtime_profile_override = runtime_profile
            ) ?: return null

        val next_time = time_for_day(next_day_start) ?: return null
        return if (next_time + alarm_offset >= alarm_now) next_time else null
    }

    private fun selection_location_from_args(selectionArgs: Array<String>?): SelectionLocationArgs =
        SelectionLocationArgs(
            latitude = selectionArgs?.getOrNull(4),
            longitude = selectionArgs?.getOrNull(5),
            altitude = selectionArgs?.getOrNull(6)
        )
}
