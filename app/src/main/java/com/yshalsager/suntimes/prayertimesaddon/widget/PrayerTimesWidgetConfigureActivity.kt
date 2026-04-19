package com.yshalsager.suntimes.prayertimesaddon.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yshalsager.suntimes.prayertimesaddon.R
import com.yshalsager.suntimes.prayertimesaddon.core.HostConfigReader
import com.yshalsager.suntimes.prayertimesaddon.core.HostResolver
import com.yshalsager.suntimes.prayertimesaddon.core.SavedLocations
import com.yshalsager.suntimes.prayertimesaddon.ui.ThemedActivity
import com.yshalsager.suntimes.prayertimesaddon.ui.compose.AppScaffold
import com.yshalsager.suntimes.prayertimesaddon.ui.compose.PrayerTimesTheme

private data class WidgetLocationOptionUi(
    val key: String,
    val title: String,
    val subtitle: String?
)

class PrayerTimesWidgetConfigureActivity : ThemedActivity() {
    private var app_widget_id = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)

        app_widget_id = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (app_widget_id == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val host = HostResolver.ensure_default_selected(this)
        val host_label = host?.let { HostConfigReader.read_config(this, it)?.display_label() } ?: getString(R.string.unknown_location)
        val host_entry = WidgetLocationOptionUi(key = SavedLocations.home_source_host, title = getString(R.string.home_location_host_prefix), subtitle = host_label)
        val saved_entries =
            SavedLocations.load(this).map {
                WidgetLocationOptionUi(
                    key = "${SavedLocations.home_source_saved}:${it.id}",
                    title = it.display_label(),
                    subtitle = null
                )
            }
        val options = listOf(host_entry) + saved_entries
        val initial_key = WidgetPrefs.get_saved_location_id(this, app_widget_id)?.let { "${SavedLocations.home_source_saved}:$it" } ?: SavedLocations.home_source_host

        setContent {
            PrayerTimesTheme {
                WidgetLocationConfigureScreen(
                    title = getString(R.string.widget_location_config_title),
                    confirm_label = getString(R.string.widget_location_config_confirm),
                    cancel_label = getString(android.R.string.cancel),
                    options = options,
                    initial_key = initial_key,
                    on_cancel = { finish() },
                    on_confirm = { selected_key -> apply_selection(selected_key) }
                )
            }
        }
    }

    private fun apply_selection(selected_key: String) {
        val saved_id = if (selected_key.startsWith("${SavedLocations.home_source_saved}:")) selected_key.removePrefix("${SavedLocations.home_source_saved}:")
            .takeIf { it.isNotBlank() } else null
        WidgetPrefs.set_saved_location_id(this, app_widget_id, saved_id)
        WidgetUpdate.request(this, intArrayOf(app_widget_id))
        setResult(
            Activity.RESULT_OK,
            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, app_widget_id)
        )
        finish()
    }
}

@Composable
private fun WidgetLocationConfigureScreen(
    title: String,
    confirm_label: String,
    cancel_label: String,
    options: List<WidgetLocationOptionUi>,
    initial_key: String,
    on_cancel: () -> Unit,
    on_confirm: (String) -> Unit
) {
    var selected_key by remember { mutableStateOf(initial_key) }

    AppScaffold(
        title = title,
        nav_content_description = cancel_label,
        on_nav = on_cancel
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f, fill = true),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(options, key = { it.key }) { option ->
                    WidgetLocationOptionRow(
                        option = option,
                        selected = option.key == selected_key,
                        on_select = { selected_key = option.key }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = on_cancel) { Text(cancel_label) }
                TextButton(onClick = { on_confirm(selected_key) }) { Text(confirm_label) }
            }
        }
    }
}

@Composable
private fun WidgetLocationOptionRow(
    option: WidgetLocationOptionUi,
    selected: Boolean,
    on_select: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = on_select),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f, fill = true)) {
                Text(
                    text = option.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface
                )
                option.subtitle?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            RadioButton(selected = selected, onClick = on_select)
        }
    }
}
