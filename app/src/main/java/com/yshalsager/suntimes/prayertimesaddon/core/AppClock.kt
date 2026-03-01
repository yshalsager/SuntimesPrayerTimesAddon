package com.yshalsager.suntimes.prayertimesaddon.core

object AppClock {
    @Volatile
    private var fixed_now_millis: Long? = null

    fun now_millis(): Long = fixed_now_millis ?: System.currentTimeMillis()

    fun set_fixed_now_millis(value: Long?) {
        fixed_now_millis = value
    }
}
