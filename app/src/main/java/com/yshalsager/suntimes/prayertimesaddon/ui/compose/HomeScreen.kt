package com.yshalsager.suntimes.prayertimesaddon.ui.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yshalsager.suntimes.prayertimesaddon.R

private val page_h_padding = 12.dp
private val page_v_padding = 12.dp
private val item_gap = 8.dp
private val anchor_gap_small = 6.dp
private val anchor_gap = 30.dp
private val card_h_padding = 12.dp
private val card_v_padding = 9.dp
private val prayer_card_h_padding = 14.dp
private val prayer_card_v_padding = 10.dp

data class NextPrayerUi(
    val label: String,
    val time: String,
    val countdown: String,
    val progress: Float
)

sealed class HomeItemUi(open val sort_time: Long) {
    data class Prayer(
        override val sort_time: Long,
        val label: String,
        val time: String,
        val countdown: String?,
        val is_next: Boolean,
        val is_passed: Boolean,
        val dot_icon: Int
    ) : HomeItemUi(sort_time)

    data class Window(
        override val sort_time: Long,
        val label: String,
        val range: String,
        val duration: String?
    ) : HomeItemUi(sort_time)

    data class Night(
        override val sort_time: Long,
        val label: String,
        val time: String
    ) : HomeItemUi(sort_time)

    data class Now(
        override val sort_time: Long,
        val time: String
    ) : HomeItemUi(sort_time)
}

data class HomeUiState(
    val method_summary: String,
    val location_summary: String,
    val host_footer: String,
    val error: String?,
    val next_prayer: NextPrayerUi?,
    val items: List<HomeItemUi>
)

@Composable
fun HomeScreen(
    state: HomeUiState,
    on_open_days: () -> Unit,
    on_open_settings: () -> Unit
) {
    val list_state = rememberLazyListState()
    val now_pos = state.items.indexOfFirst { it is HomeItemUi.Now }
    val static_items = (if (state.error != null) 1 else 0) + (if (state.next_prayer != null) 1 else 0)

    LaunchedEffect(now_pos, static_items) {
        if (now_pos < 0) return@LaunchedEffect
        val target_index = static_items + now_pos
        if (list_state.firstVisibleItemIndex > static_items) return@LaunchedEffect
        val visible = list_state.layoutInfo.visibleItemsInfo.any { it.index == target_index }
        if (!visible) list_state.scrollToItem(target_index)
    }

    AppScaffold(
        title = state.location_summary,
        subtitle = state.method_summary,
        actions = {
            IconButton(onClick = on_open_days, modifier = Modifier.size(44.dp)) {
                Icon(painterResource(R.drawable.ic_days), contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
            }
            IconButton(onClick = on_open_settings, modifier = Modifier.size(44.dp)) {
                Icon(painterResource(R.drawable.ic_settings), contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
            }
        }
    ) { padding ->
        LazyColumn(
            state = list_state,
            modifier = Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.spacedBy(item_gap),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = page_h_padding,
                end = page_h_padding,
                top = page_v_padding,
                bottom = 24.dp
            )
        ) {
            if (state.error != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            text = state.error,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            if (state.next_prayer != null) {
                item {
                    NextCard(state.next_prayer)
                }
            }

            itemsIndexed(state.items) { idx, item ->
                TimelineRow(
                    item = item,
                    is_first = idx == 0,
                    is_last = idx == state.items.lastIndex
                )
            }

            item {
                Text(
                    text = state.host_footer,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun NextCard(next: NextPrayerUi) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(next.progress)
                        .height(3.dp)
                        .align(Alignment.CenterStart)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = next.label,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = next.time,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .align(Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_timer),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = next.countdown,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun TimelineRow(
    item: HomeItemUi,
    is_first: Boolean,
    is_last: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.Top
    ) {
        TimelineAnchor(item, is_first, is_last)
        Spacer(Modifier.width(12.dp))
        when (item) {
            is HomeItemUi.Prayer -> PrayerCard(item)
            is HomeItemUi.Window -> WindowCard(item)
            is HomeItemUi.Night -> NightCard(item)
            is HomeItemUi.Now -> NowCard(item)
        }
    }
}

@Composable
private fun TimelineAnchor(
    item: HomeItemUi,
    is_first: Boolean,
    is_last: Boolean
) {
    val line_color = MaterialTheme.colorScheme.outlineVariant
    val dot_bg: Color
    val dot_icon: Painter?
    val dot_icon_tint: Color

    when (item) {
        is HomeItemUi.Prayer -> {
            dot_bg = when {
                item.is_next -> MaterialTheme.colorScheme.primary
                item.is_passed -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
            dot_icon = painterResource(item.dot_icon)
            dot_icon_tint = if (item.is_next) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        }

        is HomeItemUi.Window -> {
            dot_bg = MaterialTheme.colorScheme.secondary
            dot_icon = painterResource(R.drawable.ic_prohibited)
            dot_icon_tint = MaterialTheme.colorScheme.onSecondary
        }

        is HomeItemUi.Night -> {
            dot_bg = MaterialTheme.colorScheme.surfaceVariant
            dot_icon = painterResource(R.drawable.ic_night_portion)
            dot_icon_tint = MaterialTheme.colorScheme.onSurfaceVariant
        }

        is HomeItemUi.Now -> {
            dot_bg = MaterialTheme.colorScheme.primary
            dot_icon = null
            dot_icon_tint = MaterialTheme.colorScheme.onPrimary
        }
    }

    Box(modifier = Modifier.width(28.dp), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!is_first) Box(Modifier.width(2.dp).height(anchor_gap_small).background(line_color))
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(dot_bg),
                contentAlignment = Alignment.Center
            ) {
                if (dot_icon != null) {
                    Image(painter = dot_icon, contentDescription = null, modifier = Modifier.size(14.dp), colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(dot_icon_tint))
                }
            }
            if (!is_last) Box(Modifier.width(2.dp).weight(1f).background(line_color))
        }
    }
}

@Composable
private fun PrayerCard(item: HomeItemUi.Prayer) {
    val container =
        when {
            item.is_next -> MaterialTheme.colorScheme.primaryContainer
            item.is_passed -> MaterialTheme.colorScheme.surfaceVariant
            else -> MaterialTheme.colorScheme.surface
        }
    val stroke =
        when {
            item.is_next -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            item.is_passed -> Color.Transparent
            else -> MaterialTheme.colorScheme.outlineVariant
        }
    val elevation = if (item.is_next) 2.dp else 0.dp

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        shape = RoundedCornerShape(26.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, stroke)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = prayer_card_h_padding, vertical = prayer_card_v_padding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.label,
                    color = if (item.is_next) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (item.is_next) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.countdown != null) {
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = item.countdown,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = item.time,
                color = if (item.is_next) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (item.is_next) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun WindowCard(item: HomeItemUi.Window) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(horizontal = card_h_padding, vertical = 8.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = item.label,
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = stringResourceCompat(R.string.prohibited_tag),
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.65f),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(3.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Text(
                    text = item.range,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                if (item.duration != null) {
                    Text(
                        text = item.duration,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun NightCard(item: HomeItemUi.Night) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = card_h_padding, vertical = card_v_padding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.label,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = item.time,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun NowCard(item: HomeItemUi.Now) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = card_h_padding, vertical = card_v_padding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResourceCompat(R.string.now),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = item.time,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun stringResourceCompat(id: Int): String {
    // Avoid pulling in extra compose-resources helpers; this keeps deps small.
    return androidx.compose.ui.platform.LocalContext.current.getString(id)
}
