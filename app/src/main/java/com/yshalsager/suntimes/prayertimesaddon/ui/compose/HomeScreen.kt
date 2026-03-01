package com.yshalsager.suntimes.prayertimesaddon.ui.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yshalsager.suntimes.prayertimesaddon.R
import kotlinx.coroutines.flow.collectLatest

private val page_h_padding = 12.dp
private val page_v_padding = 12.dp
private val item_gap = 8.dp
private val card_h_padding = 12.dp
private val card_v_padding = 9.dp
private val prayer_card_h_padding = 14.dp
private val prayer_card_v_padding = 10.dp
private val timeline_lane_width = 40.dp
private val timeline_dot_size = 24.dp
private val timeline_icon_size = 14.dp
private val timeline_line_width = 2.dp

data class NextPrayerUi(
    val label: String,
    val time: String,
    val countdown: String,
    val progress: Float
)

sealed class HomeItemUi(open val sort_time: Long) {
    data class Prayer(
        override val sort_time: Long,
        val event_id: String,
        val label: String,
        val time: String,
        val countdown: String?,
        val is_next: Boolean,
        val is_passed: Boolean,
        val is_optional: Boolean,
        val dot_icon: Int
    ) : HomeItemUi(sort_time)

    data class Window(
        override val sort_time: Long,
        val label: String,
        val range: String,
        val duration: String?,
        val is_light: Boolean
    ) : HomeItemUi(sort_time)

    data class Night(
        override val sort_time: Long,
        val event_id: String,
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
    val show_reinstall_addon: Boolean,
    val days: List<HomeDayUiState>
)

data class HomeDayUiState(
    val day_start: Long,
    val day_label: String,
    val next_prayer: NextPrayerUi?,
    val items: List<HomeItemUi>
)

@Composable
fun HomeScreen(
    state: HomeUiState,
    on_open_days: () -> Unit,
    on_open_settings: () -> Unit,
    on_install_host: () -> Unit,
    on_reinstall_addon: () -> Unit,
    on_open_alarm: (String) -> Unit,
    on_shift_day: (Int) -> Unit
) {
    val ctx = LocalContext.current
    val pager_state = rememberPagerState(initialPage = 1, pageCount = { 3 })
    var override_center_day by remember { mutableStateOf<HomeDayUiState?>(null) }
    val latest_state by rememberUpdatedState(state)

    LaunchedEffect(state.days.getOrNull(1)?.day_start) {
        val o = override_center_day ?: return@LaunchedEffect
        if (state.days.getOrNull(1)?.day_start == o.day_start) override_center_day = null
    }

    LaunchedEffect(pager_state) {
        snapshotFlow { pager_state.isScrollInProgress }.collectLatest { in_progress ->
            if (in_progress) return@collectLatest
            val page = pager_state.currentPage
            if (page == 0) {
                override_center_day = latest_state.days.getOrNull(0)
                on_shift_day(-1)
                pager_state.scrollToPage(1)
            } else if (page == 2) {
                override_center_day = latest_state.days.getOrNull(2)
                on_shift_day(1)
                pager_state.scrollToPage(1)
            }
        }
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
        if (state.error != null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.spacedBy(item_gap),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = page_h_padding,
                    end = page_h_padding,
                    top = page_v_padding,
                    bottom = 24.dp
                )
            ) {
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
                if (state.error == ctx.getString(R.string.no_host_found)) {
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Button(onClick = on_install_host) {
                                Text(text = ctx.getString(R.string.install_host_action))
                            }
                        }
                    }
                }
                if (state.show_reinstall_addon) {
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Button(onClick = on_reinstall_addon) {
                                Text(text = ctx.getString(R.string.reinstall_addon_action))
                            }
                        }
                    }
                }
                item {
                    Spacer(Modifier.height(item_gap))
                    Text(
                        text = state.host_footer,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End
                    )
                }
            }
        } else {
            HorizontalPager(
                state = pager_state,
                beyondViewportPageCount = 1,
                modifier = Modifier.fillMaxSize().padding(padding)
            ) { page ->
                val day = if (page == 1) override_center_day ?: state.days.getOrNull(1) else state.days.getOrNull(page)
                day ?: return@HorizontalPager
                DayTimeline(day = day, host_footer = state.host_footer, on_open_alarm = on_open_alarm)
            }
        }
    }
}

@Composable
private fun DayTimeline(
    day: HomeDayUiState,
    host_footer: String,
    on_open_alarm: (String) -> Unit
) {
    // Pager reuses page slots; reset scroll when the underlying day changes.
    key(day.day_start) {
        val list_state = rememberLazyListState()
        val now_pos = day.items.indexOfFirst { it is HomeItemUi.Now }
        val static_items = 1 + (if (day.next_prayer != null) 1 else 0)

        LaunchedEffect(now_pos, static_items, day.day_start) {
            if (now_pos < 0) return@LaunchedEffect
            val target_index = static_items + now_pos
            val visible = list_state.layoutInfo.visibleItemsInfo.any { it.index == target_index }
            if (!visible) list_state.scrollToItem(target_index)
        }

        LazyColumn(
            state = list_state,
            verticalArrangement = Arrangement.spacedBy(0.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = page_h_padding,
                end = page_h_padding,
                top = page_v_padding,
                bottom = 24.dp
            )
        ) {
            item {
                Text(
                    text = day.day_label,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            item { Spacer(Modifier.height(item_gap)) }

            if (day.next_prayer != null) {
                item { NextCard(day.next_prayer) }
                item { Spacer(Modifier.height(item_gap)) }
            }

            itemsIndexed(day.items) { idx, item ->
                TimelineRow(
                    item = item,
                    is_first = idx == 0,
                    is_last = idx == day.items.lastIndex,
                    on_open_alarm = on_open_alarm
                )
            }

            item {
                Spacer(Modifier.height(item_gap))
                Text(
                    text = host_footer,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End
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
    is_last: Boolean,
    on_open_alarm: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(timeline_lane_width))
                when (item) {
                    is HomeItemUi.Prayer -> PrayerCard(item, on_open_alarm)
                    is HomeItemUi.Window -> WindowCard(item)
                    is HomeItemUi.Night -> NightCard(item, on_open_alarm)
                    is HomeItemUi.Now -> NowCard(item)
                }
            }
            if (!is_last) Spacer(Modifier.height(item_gap))
        }

        TimelineAnchor(
            item = item,
            is_first = is_first,
            is_last = is_last,
            modifier = Modifier.matchParentSize().align(Alignment.CenterStart)
        )
    }
}

@Composable
private fun TimelineAnchor(
    item: HomeItemUi,
    is_first: Boolean,
    is_last: Boolean,
    modifier: Modifier = Modifier
) {
    val line_color = MaterialTheme.colorScheme.outlineVariant
    val dot_bg: Color
    val dot_icon: Painter?
    val dot_icon_tint: Color

    when (item) {
        is HomeItemUi.Prayer -> {
            dot_bg = if (item.is_next) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            dot_icon = painterResource(item.dot_icon)
            dot_icon_tint =
                when {
                    item.is_next -> MaterialTheme.colorScheme.onPrimary
                    item.is_optional -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
        }

        is HomeItemUi.Window -> {
            dot_bg = if (item.is_light) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.secondary
            dot_icon = painterResource(R.drawable.ic_prohibited)
            dot_icon_tint = if (item.is_light) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSecondary
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

    Box(modifier = modifier, contentAlignment = Alignment.CenterStart) {
        Box(modifier = Modifier.fillMaxHeight().width(timeline_lane_width), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val line_w = timeline_line_width.toPx()
                val center_x = size.width / 2f
                val dot_h = timeline_dot_size.toPx()
                val center_y = size.height / 2f
                val top_end = center_y - (dot_h / 2f)
                val bottom_start = center_y + (dot_h / 2f)

                if (!is_first) {
                    drawLine(
                        color = line_color,
                        start = Offset(center_x, 0f),
                        end = Offset(center_x, top_end),
                        strokeWidth = line_w,
                        cap = StrokeCap.Butt
                    )
                }
                if (!is_last) {
                    drawLine(
                        color = line_color,
                        start = Offset(center_x, bottom_start),
                        end = Offset(center_x, size.height),
                        strokeWidth = line_w,
                        cap = StrokeCap.Butt
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(timeline_dot_size)
                    .clip(CircleShape)
                    .background(dot_bg),
                contentAlignment = Alignment.Center
            ) {
                if (dot_icon != null) {
                    Image(
                        painter = dot_icon,
                        contentDescription = null,
                        modifier = Modifier.size(timeline_icon_size),
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(dot_icon_tint)
                    )
                }
            }
        }
    }
}

@Composable
private fun PrayerCard(item: HomeItemUi.Prayer, on_open_alarm: (String) -> Unit) {
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
        onClick = { on_open_alarm(item.event_id) },
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
                        color = if (item.is_next) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (item.is_next) FontWeight.Bold else FontWeight.Medium
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
    val container = MaterialTheme.colorScheme.secondaryContainer
    val on_container = MaterialTheme.colorScheme.onSecondaryContainer
    val accent = if (item.is_light) on_container.copy(alpha = 0.78f) else MaterialTheme.colorScheme.secondary
    val tag = if (item.is_light) R.string.prohibited_tag_light else R.string.prohibited_tag_heavy

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = container),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(horizontal = card_h_padding, vertical = 8.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = item.label,
                    color = accent,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = stringResourceCompat(tag),
                    color = on_container.copy(alpha = 0.65f),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(3.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Text(
                    text = item.range,
                    color = on_container,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                if (item.duration != null) {
                    Text(
                        text = item.duration,
                        color = on_container.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun NightCard(item: HomeItemUi.Night, on_open_alarm: (String) -> Unit) {
    Card(
        onClick = { on_open_alarm(item.event_id) },
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
