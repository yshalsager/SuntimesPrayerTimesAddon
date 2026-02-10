package com.yshalsager.suntimes.prayertimesaddon

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.yshalsager.suntimes.prayertimesaddon.core.Prefs
import net.time4j.android.ApplicationStarter

class PrayerTimesApplication : android.app.Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        // ContentProviders initialize before Application.onCreate(); Time4J must be activated before any Time4J class loads.
        ApplicationStarter.initialize(this)
    }

    override fun onCreate() {
        super.onCreate()
        val night_mode =
            when (Prefs.get_theme(this)) {
                Prefs.theme_light -> AppCompatDelegate.MODE_NIGHT_NO
                Prefs.theme_dark -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        AppCompatDelegate.setDefaultNightMode(night_mode)

        val lang = Prefs.get_language(this)
        val locales = if (lang == "system") LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(lang)
        AppCompatDelegate.setApplicationLocales(locales)
    }
}
