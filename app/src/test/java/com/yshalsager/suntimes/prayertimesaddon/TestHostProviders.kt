package com.yshalsager.suntimes.prayertimesaddon

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import com.yshalsager.suntimes.prayertimesaddon.core.AlarmEventContract
import com.yshalsager.suntimes.prayertimesaddon.core.CalculatorConfigContract

const val host_event_authority = "com.test.host.event.provider"
const val host_calc_authority = "com.test.host.calculator.provider"
const val denied_host_event_authority = "com.test.denied.event.provider"
const val denied_host_calc_authority = "com.test.denied.calculator.provider"
const val day_millis = 24 * 60 * 60 * 1000L

class FakeHostEventProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selection_args: Array<String>?,
        sort_order: String?
    ): Cursor? {
        val path = uri.pathSegments
        if (path.isEmpty()) return null
        return when (path[0]) {
            AlarmEventContract.query_event_info -> query_event_info(path.getOrNull(1), projection)
            AlarmEventContract.query_event_calc -> query_event_calc(path.getOrNull(1), projection, selection_args)
            else -> null
        }
    }

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selection_args: Array<String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selection_args: Array<String>?): Int = 0

    private fun query_event_info(event_id: String?, projection: Array<String>?): Cursor {
        val cols = projection ?: arrayOf(AlarmEventContract.column_event_name)
        val c = MatrixCursor(cols)
        if (event_id == null) return c
        if (time_for_event(event_id, System.currentTimeMillis()) != null) {
            val row = arrayOfNulls<Any>(cols.size)
            cols.indices.forEach { i ->
                row[i] = if (cols[i] == AlarmEventContract.column_event_name) event_id else null
            }
            c.addRow(row)
        }
        return c
    }

    private fun query_event_calc(event_id: String?, projection: Array<String>?, selection_args: Array<String>?): Cursor {
        val cols = projection ?: AlarmEventContract.query_event_calc_projection
        val c = MatrixCursor(cols)
        if (event_id == null) return c

        val alarm_now = selection_args?.getOrNull(0)?.toLongOrNull() ?: System.currentTimeMillis()
        val time = time_for_event(event_id, alarm_now) ?: return c

        val row = arrayOfNulls<Any>(cols.size)
        cols.indices.forEach { i ->
            row[i] =
                when (cols[i]) {
                    AlarmEventContract.column_event_name -> event_id
                    AlarmEventContract.column_event_timemillis -> time
                    else -> null
                }
        }
        c.addRow(row)
        return c
    }

    private fun time_for_event(event_id: String, alarm_now: Long): Long? {
        val day_start = alarm_now - Math.floorMod(alarm_now, day_millis)
        val offset =
            when {
                event_id == "SUNRISE" -> 6 * 60 * 60 * 1000L
                event_id == "NOON" -> 12 * 60 * 60 * 1000L
                event_id == "SUNSET" -> 18 * 60 * 60 * 1000L
                event_id == "SHADOWRATIO_X:1.0" -> 15 * 60 * 60 * 1000L + 30 * 60 * 1000L
                event_id == "SHADOWRATIO_X:2.0" -> 16 * 60 * 60 * 1000L + 30 * 60 * 1000L
                event_id == "SUN_5.0s" -> 17 * 60 * 60 * 1000L + 45 * 60 * 1000L
                event_id.startsWith("SUN_-") && event_id.endsWith("r") -> 5 * 60 * 60 * 1000L
                event_id.startsWith("SUN_-") && event_id.endsWith("s") -> 19 * 60 * 60 * 1000L + 30 * 60 * 1000L
                else -> null
            }
        return offset?.let { day_start + it }
    }
}

class FakeHostCalcProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selection_args: Array<String>?,
        sort_order: String?
    ): Cursor? {
        val path = uri.pathSegments
        if (path.isEmpty()) return null
        return when (path[0]) {
            CalculatorConfigContract.query_config -> query_config(projection)
            CalculatorConfigContract.query_sun -> query_sun(path.getOrNull(1), projection, selection_args)
            CalculatorConfigContract.query_sunpos -> query_sunpos(projection)
            else -> null
        }
    }

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selection_args: Array<String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selection_args: Array<String>?): Int = 0

    private fun query_config(projection: Array<String>?): Cursor {
        val cols = projection ?: CalculatorConfigContract.projection_basic
        val c = MatrixCursor(cols)
        val row = arrayOfNulls<Any>(cols.size)
        cols.indices.forEach { i ->
            row[i] =
                when (cols[i]) {
                    CalculatorConfigContract.column_location -> "Test Location"
                    CalculatorConfigContract.column_latitude -> "30.0"
                    CalculatorConfigContract.column_longitude -> "31.0"
                    CalculatorConfigContract.column_timezone -> "UTC"
                    else -> null
                }
        }
        c.addRow(row)
        return c
    }

    private fun query_sun(at_millis_segment: String?, projection: Array<String>?, selection_args: Array<String>?): Cursor {
        val cols = projection ?: CalculatorConfigContract.projection_sun_basic
        val c = MatrixCursor(cols)
        val at_millis = at_millis_segment?.toLongOrNull() ?: System.currentTimeMillis()
        val day_start = at_millis - Math.floorMod(at_millis, day_millis)
        val location_shift = if (selection_args?.getOrNull(4) == "55.0") 30L * 60L * 1000L else 0L
        val row = arrayOfNulls<Any>(cols.size)
        cols.indices.forEach { i ->
            row[i] =
                when (cols[i]) {
                    CalculatorConfigContract.column_sun_noon -> day_start + 12 * 60 * 60 * 1000L + location_shift
                    CalculatorConfigContract.column_sunrise -> day_start + 6 * 60 * 60 * 1000L + location_shift
                    CalculatorConfigContract.column_sunset -> day_start + 18 * 60 * 60 * 1000L + location_shift
                    else -> null
                }
        }
        c.addRow(row)
        return c
    }

    private fun query_sunpos(projection: Array<String>?): Cursor {
        val cols = projection ?: CalculatorConfigContract.projection_sunpos_dec
        val c = MatrixCursor(cols)
        val row = arrayOfNulls<Any>(cols.size)
        cols.indices.forEach { i ->
            row[i] = if (cols[i] == CalculatorConfigContract.column_sunpos_dec) 10.0 else null
        }
        c.addRow(row)
        return c
    }
}

class DeniedHostCalcProvider : ContentProvider() {
    override fun onCreate(): Boolean = true
    override fun query(uri: Uri, projection: Array<String>?, selection: String?, selection_args: Array<String>?, sort_order: String?): Cursor? {
        throw SecurityException("denied")
    }
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selection_args: Array<String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selection_args: Array<String>?): Int = 0
}
