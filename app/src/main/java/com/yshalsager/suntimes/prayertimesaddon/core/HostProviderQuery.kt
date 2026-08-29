package com.yshalsager.suntimes.prayertimesaddon.core

import android.database.Cursor
import android.util.Log

internal sealed interface HostProviderResult<out T> {
    data class Success<T>(val value: T) : HostProviderResult<T>
    data object EventUnavailable : HostProviderResult<Nothing>
    data object PermissionDenied : HostProviderResult<Nothing>
    data object UnsupportedContract : HostProviderResult<Nothing>
    data object TemporaryFailure : HostProviderResult<Nothing>
}

internal val <T> HostProviderResult<T>.value_or_null: T?
    get() = (this as? HostProviderResult.Success)?.value

internal fun <T> query_host_provider(
    authority: String,
    operation: String,
    event_id: String? = null,
    query: () -> Cursor?,
    read: (Cursor) -> T?
): HostProviderResult<T> =
    try {
        val cursor = query()
        if (cursor == null) {
            log_host_failure(authority, operation, event_id, "NullCursor")
            HostProviderResult.TemporaryFailure
        } else {
            cursor.use { read(it)?.let { value -> HostProviderResult.Success(value) } ?: HostProviderResult.EventUnavailable }
        }
    } catch (_: SecurityException) {
        log_host_failure(authority, operation, event_id, "SecurityException")
        HostProviderResult.PermissionDenied
    } catch (_: IllegalArgumentException) {
        log_host_failure(authority, operation, event_id, "IllegalArgumentException")
        HostProviderResult.UnsupportedContract
    } catch (e: RuntimeException) {
        log_host_failure(authority, operation, event_id, e.javaClass.simpleName)
        HostProviderResult.TemporaryFailure
    }

private fun log_host_failure(authority: String, operation: String, event_id: String?, failure: String) {
    Log.w("HostProvider", "authority=$authority operation=$operation event=${event_id ?: "-"} failure=$failure")
}

internal fun Cursor.host_string(column: String): String? {
    val index = getColumnIndexOrThrow(column)
    if (isNull(index)) return null
    require(getType(index) == Cursor.FIELD_TYPE_STRING) { "Invalid host column type: $column" }
    return getString(index)
}

internal fun Cursor.host_long(column: String): Long? {
    val index = getColumnIndexOrThrow(column)
    if (isNull(index)) return null
    require(getType(index) == Cursor.FIELD_TYPE_INTEGER) { "Invalid host column type: $column" }
    return getLong(index)
}

internal fun Cursor.host_double(column: String): Double? {
    val index = getColumnIndexOrThrow(column)
    if (isNull(index)) return null
    require(getType(index) == Cursor.FIELD_TYPE_FLOAT || getType(index) == Cursor.FIELD_TYPE_INTEGER) { "Invalid host column type: $column" }
    return getDouble(index)
}
