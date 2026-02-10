package com.yshalsager.suntimes.prayertimesaddon.ui.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yshalsager.suntimes.prayertimesaddon.core.AddonEvent

@Composable
fun EventPickerScreen(
    items: List<AddonEvent>,
    on_pick: (AddonEvent) -> Unit,
    padding: PaddingValues
) {
    Column(Modifier.padding(padding)) {
        LazyColumn(
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(items) { item ->
                Text(
                    text = title_for(item),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { on_pick(item) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

@Composable
private fun title_for(item: AddonEvent): String {
    return androidx.compose.ui.platform.LocalContext.current.getString(item.title_res)
}
