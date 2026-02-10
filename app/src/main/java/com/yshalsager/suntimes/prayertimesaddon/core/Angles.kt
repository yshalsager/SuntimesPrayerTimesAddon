package com.yshalsager.suntimes.prayertimesaddon.core

import java.math.BigDecimal
import java.math.RoundingMode

// Host event ids embed angles as plain decimals (no scientific notation).
// We cap precision to keep ids stable, and always include a decimal point (e.g. 5.0) to match host formatting.
fun format_angle_id(v: Double): String {
    val s0 =
        BigDecimal.valueOf(v)
            .setScale(6, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
    val s = if (s0 == "-0") "0" else s0
    return if (s.contains('.')) s else "$s.0"
}

