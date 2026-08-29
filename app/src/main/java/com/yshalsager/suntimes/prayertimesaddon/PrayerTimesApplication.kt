package com.yshalsager.suntimes.prayertimesaddon

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.yshalsager.suntimes.prayertimesaddon.core.HostResolver
import com.yshalsager.suntimes.prayertimesaddon.core.Prefs
import com.yshalsager.suntimes.prayertimesaddon.widget.WidgetUpdate
import net.time4j.android.ApplicationStarter

class PrayerTimesApplication : android.app.Application() {
    private val host_package_receiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (!HostResolver.is_known_package(intent.data?.schemeSpecificPart)) return
                if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false) && intent.action != Intent.ACTION_PACKAGE_REPLACED) return
                WidgetUpdate.request(context)
            }
        }

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

        val host_package_filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(this, host_package_receiver, host_package_filter, ContextCompat.RECEIVER_EXPORTED)
    }
}
