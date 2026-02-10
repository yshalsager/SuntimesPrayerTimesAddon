package com.yshalsager.suntimes.prayertimesaddon.ui

import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.color.DynamicColors
import com.yshalsager.suntimes.prayertimesaddon.R
import com.yshalsager.suntimes.prayertimesaddon.core.Prefs

open class ThemedActivity : AppCompatActivity() {
    private var applied_sig: String? = null

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        applied_sig = prefs_sig()
        val palette = Prefs.get_palette(this)
        val use_dynamic = palette == Prefs.palette_dynamic && DynamicColors.isDynamicColorAvailable()
        // Intentional layering:
        // - XML theme + DynamicColors: system UI (status/navigation bars) and window background.
        // - Compose dynamicColorScheme: in-app composables.
        val theme_res =
            if (use_dynamic) R.style.Theme_PrayerTimesAddon_Dynamic
            else when (palette) {
                Prefs.palette_sapphire -> R.style.Theme_PrayerTimesAddon_Sapphire
                Prefs.palette_rose -> R.style.Theme_PrayerTimesAddon_Rose
                else -> R.style.Theme_PrayerTimesAddon_Parchment
            }
        setTheme(theme_res)
        if (use_dynamic) DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        if (applied_sig != prefs_sig()) recreate()
    }

    private fun prefs_sig(): String =
        listOf(Prefs.get_palette(this), Prefs.get_theme(this), Prefs.get_language(this)).joinToString("|")
}
