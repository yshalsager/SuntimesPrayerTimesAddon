package com.yshalsager.suntimes.prayertimesaddon.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.core.net.toUri
import com.yshalsager.suntimes.prayertimesaddon.R
import com.yshalsager.suntimes.prayertimesaddon.core.AddonEvent
import com.yshalsager.suntimes.prayertimesaddon.core.AlarmEventContract
import com.yshalsager.suntimes.prayertimesaddon.core.visible_addon_events
import com.yshalsager.suntimes.prayertimesaddon.provider.PrayerTimesProvider
import com.yshalsager.suntimes.prayertimesaddon.ui.compose.AppScaffold
import com.yshalsager.suntimes.prayertimesaddon.ui.compose.EventPickerScreen
import com.yshalsager.suntimes.prayertimesaddon.ui.compose.PrayerTimesTheme

class EventPickerActivity : ThemedActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PrayerTimesTheme {
                val items = remember { visible_addon_events(this@EventPickerActivity) }
                AppScaffold(
                    title = getString(R.string.picker_title),
                    nav_content_description = getString(android.R.string.cancel),
                    on_nav = { finish() }
                ) { padding ->
                    EventPickerScreen(
                        items = items,
                        on_pick = { e ->
                            val uri =
                                "content://${PrayerTimesProvider.authority}/${AlarmEventContract.query_event_info}/${e.event_id}".toUri()
                            setResult(
                                RESULT_OK,
                                Intent()
                                    .setData(uri)
                                    .putExtra(AlarmEventContract.column_config_provider, PrayerTimesProvider.authority)
                                    .putExtra(AlarmEventContract.column_event_name, e.event_id)
                            )
                            finish()
                        },
                        padding = padding
                    )
                }
            }
        }
    }
}
