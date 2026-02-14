package com.yshalsager.suntimes.prayertimesaddon.ui.compose

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yshalsager.suntimes.prayertimesaddon.R
import com.yshalsager.suntimes.prayertimesaddon.core.DayItem
import com.yshalsager.suntimes.prayertimesaddon.core.DayMeta
import com.yshalsager.suntimes.prayertimesaddon.core.Prefs
import com.yshalsager.suntimes.prayertimesaddon.ui.DaysUiState
import com.yshalsager.suntimes.prayertimesaddon.ui.DaysViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull

@Composable
fun DaysScreen(vm: DaysViewModel, on_back: () -> Unit) {
    val ctx = LocalContext.current

    val permission_request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.load(force = true)
    }

    val state = vm.state
    val skel = state.skeleton
    val list_state = rememberLazyListState()
    var did_scroll_to_today by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(skel?.days?.size, skel?.today_pos) {
        if (did_scroll_to_today) return@LaunchedEffect
        val pos = skel?.today_pos ?: -1
        if (pos >= 0) {
            list_state.scrollToItem(pos)
            vm.ensure_range_loaded(pos, pos)
            did_scroll_to_today = true
        }
    }

    LaunchedEffect(skel?.days?.size) {
        if (skel == null) return@LaunchedEffect
        snapshotFlow<Pair<Int, Int>?> { list_state.visible_range() }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { range: Pair<Int, Int> -> vm.ensure_range_loaded(range.first, range.second) }
    }

    AppScaffold(
        title = state.title.ifBlank { ctx.getString(R.string.days_title) },
        nav_content_description = ctx.getString(android.R.string.cancel),
        on_nav = on_back,
        actions = {
            IconButton(onClick = { vm.shift_month(-1) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = ctx.getString(R.string.prev_month),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            IconButton(onClick = { vm.shift_month(1) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = ctx.getString(R.string.next_month),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    ) { padding ->
        DaysContent(
            vm = vm,
            state = state,
            list_state = list_state,
            padding = padding,
            on_request_permission = { perm -> permission_request.launch(perm) }
        )
    }
}

@Composable
private fun DaysContent(
    vm: DaysViewModel,
    state: DaysUiState,
    list_state: LazyListState,
    padding: PaddingValues,
    on_request_permission: (String) -> Unit
) {
    Column(Modifier.padding(padding)) {
        if (state.error != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.large,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.padding(12.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(text = state.error, style = MaterialTheme.typography.bodyMedium)
                    val perm = state.required_permission
                    if (perm != null) {
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = { on_request_permission(perm) }) {
                            Text(text = androidx.compose.ui.platform.LocalContext.current.getString(R.string.grant_permission))
                        }
                    }
                }
            }
        }

        val days = state.skeleton?.days ?: emptyList()
        LazyColumn(
            state = list_state,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(days.size, key = { i -> days[i].day_start }) { i ->
                val meta = days[i]
                DayCard(meta = meta, item = vm.day_cache[meta.day_start])
            }
        }
    }
}

@Composable
private fun DayCard(meta: DayMeta, item: DayItem?) {
    val ctx = LocalContext.current
    val month_basis = Prefs.get_days_month_basis(ctx)
    val show_night = item?.let { it.night_midpoint != null && it.night_last_third != null && it.night_last_sixth != null } ?: false
    val has_prohibited =
        item?.let {
            it.prohibited_dawn != null ||
                it.prohibited_sunrise != null ||
                it.prohibited_zawal != null ||
                it.prohibited_after_asr != null ||
                it.prohibited_sunset != null
        } ?: false

    val border =
        if (item?.is_today == true || meta.is_today) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f))
        else null

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.extraLarge,
        border = border,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    val hijri = item?.hijri ?: meta.hijri
                    val greg = item?.title ?: meta.title
                    val primary = if (month_basis == Prefs.days_month_basis_hijri && hijri != null) hijri else greg
                    val secondary = if (month_basis == Prefs.days_month_basis_hijri) greg else hijri

                    Text(
                        text = primary,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (secondary != null && secondary != primary) {
                        Text(
                            text = secondary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (item?.is_today == true || meta.is_today) {
                    Text(
                        text = ctx.getString(R.string.today),
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            PrayerRow(
                is_friday = item?.is_friday ?: meta.is_friday,
                fajr = item?.fajr ?: "--",
                dhuhr = item?.dhuhr ?: "--",
                asr = item?.asr ?: "--",
                maghrib = item?.maghrib ?: "--",
                isha = item?.isha ?: "--"
            )

            if (item != null && has_prohibited) {
                Spacer(Modifier.height(10.dp))
                ProhibitedRow(item)
            }

            if (item != null && show_night) {
                Spacer(Modifier.height(10.dp))
                NightRow(item)
            }
        }
    }
}

@Composable
private fun PrayerRow(is_friday: Boolean, fajr: String, dhuhr: String, asr: String, maghrib: String, isha: String) {
    val ctx = LocalContext.current
    val dhuhr_label = if (is_friday) ctx.getString(R.string.event_prayer_jummah) else ctx.getString(R.string.event_prayer_dhuhr)
    val labels = listOf(
        ctx.getString(R.string.event_prayer_fajr) to fajr,
        dhuhr_label to dhuhr,
        ctx.getString(R.string.event_prayer_asr) to asr,
        ctx.getString(R.string.event_prayer_maghrib) to maghrib,
        ctx.getString(R.string.event_prayer_isha) to isha
    )

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        labels.forEach { (label, time) ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = time, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

private fun LazyListState.visible_range(): Pair<Int, Int>? {
    val info = layoutInfo.visibleItemsInfo
    if (info.isEmpty()) return null
    return info.first().index to info.last().index
}

@Composable
private fun ProhibitedRow(item: DayItem) {
    val ctx = LocalContext.current
    val labels = listOf(
        ctx.getString(R.string.prohibited_dawn) to item.prohibited_dawn,
        ctx.getString(R.string.prohibited_sunrise) to item.prohibited_sunrise,
        ctx.getString(R.string.prohibited_zawal) to item.prohibited_zawal,
        ctx.getString(R.string.prohibited_after_asr) to item.prohibited_after_asr,
        ctx.getString(R.string.prohibited_sunset) to item.prohibited_sunset
    )

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        labels.forEach { (label, range) ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = range ?: "--", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun NightRow(item: DayItem) {
    val ctx = LocalContext.current
    val labels = listOf(
        ctx.getString(R.string.night_midpoint) to item.night_midpoint,
        ctx.getString(R.string.night_last_third) to item.night_last_third,
        ctx.getString(R.string.night_last_sixth) to item.night_last_sixth
    )

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        labels.forEach { (label, time) ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = time ?: "--", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
