package com.yshalsager.suntimes.prayertimesaddon.ui.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yshalsager.suntimes.prayertimesaddon.R

data class SavedLocationCardUi(
    val id: String,
    val title: String,
    val subtitle: String,
    val is_friday: Boolean,
    val fajr: String,
    val fajr_extra_1: String?,
    val duha: String?,
    val eid_start: String?,
    val eid_end: String?,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String,
    val isha_extra_1: String?,
    val prohibited_dawn: String?,
    val prohibited_sunrise: String?,
    val prohibited_zawal: String?,
    val prohibited_after_asr: String?,
    val prohibited_sunset: String?,
    val night_midpoint: String?,
    val night_last_third: String?,
    val night_last_sixth: String?
)

data class SavedLocationsCardsUiState(
    val title: String,
    val subtitle: String?,
    val cards: List<SavedLocationCardUi>,
    val host_footer: String,
    val error: String?,
    val show_reinstall_addon: Boolean
)

@Composable
fun SavedLocationsCardsScreen(
    state: SavedLocationsCardsUiState,
    on_back: () -> Unit,
    on_open_settings: () -> Unit,
    on_install_host: () -> Unit,
    on_reinstall_addon: () -> Unit
) {
    val no_host_found = stringResource(R.string.no_host_found)
    val install_host_action = stringResource(R.string.install_host_action)
    val reinstall_addon_action = stringResource(R.string.reinstall_addon_action)
    val nav_cancel = stringResource(android.R.string.cancel)

    AppScaffold(
        title = state.title,
        subtitle = state.subtitle,
        nav_content_description = nav_cancel,
        on_nav = on_back,
        actions = {
            IconButton(onClick = on_open_settings) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.error != null) {
                item {
                    SavedLocationsInfoCard {
                        Column(Modifier.padding(12.dp)) {
                            Text(text = state.error, style = MaterialTheme.typography.bodyMedium)
                            if (state.error == no_host_found) {
                                Spacer(Modifier.height(10.dp))
                                Button(onClick = on_install_host) {
                                    Text(text = install_host_action)
                                }
                            }
                            if (state.show_reinstall_addon) {
                                Spacer(Modifier.height(10.dp))
                                Button(onClick = on_reinstall_addon) {
                                    Text(text = reinstall_addon_action)
                                }
                            }
                        }
                    }
                }
            } else if (state.cards.isEmpty()) {
                item {
                    SavedLocationsInfoCard {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                text = stringResource(R.string.saved_locations_empty_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.saved_locations_empty_summary),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(state.cards, key = { it.id }) { card ->
                    SavedLocationPrayerCard(card)
                }
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
private fun SavedLocationsInfoCard(content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) { content() }
}

@Composable
private fun SavedLocationPrayerCard(card: SavedLocationCardUi) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                text = card.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = card.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(10.dp))

            PrayerRow(
                is_friday = card.is_friday,
                fajr = card.fajr,
                duha = card.duha,
                dhuhr = card.dhuhr,
                asr = card.asr,
                maghrib = card.maghrib,
                isha = card.isha
            )

            val has_optional_prayers = card.fajr_extra_1 != null || card.eid_start != null || card.eid_end != null || card.isha_extra_1 != null
            if (has_optional_prayers) {
                Spacer(Modifier.height(10.dp))
                OptionalPrayerRow(card)
            }

            val has_prohibited =
                card.prohibited_dawn != null ||
                    card.prohibited_sunrise != null ||
                    card.prohibited_zawal != null ||
                    card.prohibited_after_asr != null ||
                    card.prohibited_sunset != null
            if (has_prohibited) {
                Spacer(Modifier.height(10.dp))
                ProhibitedRow(card)
            }

            val has_night =
                card.night_midpoint != null ||
                    card.night_last_third != null ||
                    card.night_last_sixth != null
            if (has_night) {
                Spacer(Modifier.height(10.dp))
                NightRow(card)
            }
        }
    }
}

@Composable
private fun PrayerRow(
    is_friday: Boolean,
    fajr: String,
    duha: String?,
    dhuhr: String,
    asr: String,
    maghrib: String,
    isha: String
) {
    val fajr_label = stringResource(R.string.event_prayer_fajr)
    val duha_label = stringResource(R.string.event_prayer_duha)
    val dhuhr_label = if (is_friday) stringResource(R.string.event_prayer_jummah) else stringResource(R.string.event_prayer_dhuhr)
    val asr_label = stringResource(R.string.event_prayer_asr)
    val maghrib_label = stringResource(R.string.event_prayer_maghrib)
    val isha_label = stringResource(R.string.event_prayer_isha)
    val labels = buildList {
        add(Triple(fajr_label, fajr, false))
        if (duha != null) add(Triple(duha_label, duha, true))
        add(Triple(dhuhr_label, dhuhr, false))
        add(Triple(asr_label, asr, false))
        add(Triple(maghrib_label, maghrib, false))
        add(Triple(isha_label, isha, false))
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        labels.forEach { (label, time, is_optional) ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (is_optional) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = time,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (is_optional) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun OptionalPrayerRow(card: SavedLocationCardUi) {
    val labels =
        listOf(
            Triple(stringResource(R.string.event_prayer_fajr_extra_1), card.fajr_extra_1, true),
            Triple(stringResource(R.string.event_prayer_duha), card.duha, true),
            Triple(stringResource(R.string.event_prayer_eid_start), card.eid_start, true),
            Triple(stringResource(R.string.event_prayer_eid_end), card.eid_end, true),
            Triple(stringResource(R.string.event_prayer_isha_extra_1), card.isha_extra_1, true)
        )

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        labels.forEach { (label, time, is_optional) ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (is_optional) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = time ?: "--",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = if (is_optional) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ProhibitedRow(card: SavedLocationCardUi) {
    val light_color = MaterialTheme.colorScheme.primary
    val heavy_color = MaterialTheme.colorScheme.onSurfaceVariant
    val labels =
        listOf(
            Triple(stringResource(R.string.prohibited_dawn), card.prohibited_dawn, true),
            Triple(stringResource(R.string.prohibited_sunrise), card.prohibited_sunrise, false),
            Triple(stringResource(R.string.prohibited_zawal), card.prohibited_zawal, false),
            Triple(stringResource(R.string.prohibited_after_asr), card.prohibited_after_asr, true),
            Triple(stringResource(R.string.prohibited_sunset), card.prohibited_sunset, false)
        )

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        labels.forEach { (label, range, is_light) ->
            val item_color = if (is_light) light_color else heavy_color
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = item_color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = range ?: "--",
                    style = MaterialTheme.typography.bodySmall,
                    color = item_color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun NightRow(card: SavedLocationCardUi) {
    val labels =
        listOf(
            stringResource(R.string.night_midpoint) to card.night_midpoint,
            stringResource(R.string.night_last_third) to card.night_last_third,
            stringResource(R.string.night_last_sixth) to card.night_last_sixth
        )

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        labels.forEach { (label, time) ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = time ?: "--",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
