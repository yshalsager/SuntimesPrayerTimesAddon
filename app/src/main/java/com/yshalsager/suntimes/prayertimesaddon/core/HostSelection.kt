package com.yshalsager.suntimes.prayertimesaddon.core

class HostSelection internal constructor(
    val selection: String?,
    val selection_args: Array<String>?,
    private val values: Map<String, String>
) {
    operator fun get(name: String): String? = values[name]

    fun with_values(overrides: Map<String, String?>): HostSelection {
        val merged = LinkedHashMap(values)
        overrides.forEach { (name, value) ->
            if (value == null) merged.remove(name) else merged[name] = value
        }
        return HostSelection(
            merged.keys.joinToString(" AND ") { "$it=?" },
            merged.values.toTypedArray(),
            merged
        )
    }
}

fun parse_host_selection(selection: String?, selection_args: Array<String>?): HostSelection {
    val values = linkedMapOf<String, String>()
    if (selection == null) {
        fun put(name: String, index: Int) {
            selection_args?.getOrNull(index)?.let { values[name] = it }
        }
        put(AlarmEventContract.extra_alarm_now, 0)
        put(AlarmEventContract.extra_alarm_offset, 1)
        put(AlarmEventContract.extra_alarm_repeat, 2)
        put(AlarmEventContract.extra_alarm_repeat_days, 3)
        put(CalculatorConfigContract.column_latitude, 4)
        put(CalculatorConfigContract.column_longitude, 5)
        selection_args?.getOrNull(6)?.let {
            values[if (it.toDoubleOrNull() != null) CalculatorConfigContract.column_altitude else CalculatorConfigContract.column_timezone] = it
        }
        put(CalculatorConfigContract.column_altitude, 7)
    } else {
        var completed: String = selection
        selection_args?.filterNotNull()?.forEach { completed = completed.replaceFirst("?", it) }
        completed.split(Regex("\\s+(?i:and|or)\\s+")).forEach { expression ->
            val parts = expression.split('=')
            if (parts.size == 2) values[parts[0].trim()] = parts[1].trim()
        }
    }
    return HostSelection(selection, selection_args, values)
}
