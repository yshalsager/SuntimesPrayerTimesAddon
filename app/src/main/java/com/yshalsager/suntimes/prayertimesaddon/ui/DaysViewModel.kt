package com.yshalsager.suntimes.prayertimesaddon.ui

import android.app.Application
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.yshalsager.suntimes.prayertimesaddon.R
import com.yshalsager.suntimes.prayertimesaddon.core.HostConfigReader
import com.yshalsager.suntimes.prayertimesaddon.core.HostResolver
import com.yshalsager.suntimes.prayertimesaddon.core.Prefs
import com.yshalsager.suntimes.prayertimesaddon.core.add_days
import com.yshalsager.suntimes.prayertimesaddon.core.DayItem
import com.yshalsager.suntimes.prayertimesaddon.core.MonthSkeleton
import com.yshalsager.suntimes.prayertimesaddon.core.build_day_item
import com.yshalsager.suntimes.prayertimesaddon.core.build_month_skeleton
import java.util.TimeZone
import java.util.Collections
import java.util.concurrent.Executors

data class DaysUiState(
    val loading: Boolean = false,
    val title: String = "",
    val skeleton: MonthSkeleton? = null,
    val error: String? = null,
    val required_permission: String? = null
)

class DaysViewModel(app: Application) : AndroidViewModel(app) {
    private val main = Handler(Looper.getMainLooper())
    private var load_id = 0
    private var last_sig: String? = null

    var month_anchor: Long? = null
        private set

    var month_start: Long? = null
        private set

    var month_end: Long? = null
        private set

    val day_cache = mutableStateMapOf<Long, DayItem>()
    private val day_inflight = Collections.synchronizedSet(HashSet<Long>())
    private val workers = Executors.newFixedThreadPool(3)

    private var loaded_host: String? = null
    private var loaded_show_prohibited = false
    private var loaded_show_night = false

    var state by mutableStateOf(DaysUiState())
        private set

    fun load(force: Boolean = false) {
        val ctx = getApplication<Application>().applicationContext
        val host = HostResolver.ensure_default_selected(ctx)
        if (host == null) {
            state = DaysUiState(error = ctx.getString(R.string.no_host_found))
            return
        }

        val required_perm = HostResolver.get_required_permission(ctx, host)
        if (required_perm != null && ContextCompat.checkSelfPermission(ctx, required_perm) != PackageManager.PERMISSION_GRANTED) {
            state = DaysUiState(error = ctx.getString(R.string.missing_permission, required_perm), required_permission = required_perm)
            return
        }

        val show_prohibited = Prefs.get_days_show_prohibited(ctx)
        val show_night = Prefs.get_days_show_night_portions(ctx)
        val show_hijri = Prefs.get_days_show_hijri(ctx)
        val month_basis = Prefs.get_days_month_basis(ctx)
        val hijri_variant = Prefs.get_hijri_variant(ctx)
        val hijri_offset = Prefs.get_hijri_day_offset(ctx)

        val show_hijri_effective = show_hijri || month_basis == Prefs.days_month_basis_hijri
        val sig =
            listOf(
                host,
                month_anchor?.toString() ?: "",
                show_prohibited.toString(),
                show_night.toString(),
                show_hijri_effective.toString(),
                month_basis,
                hijri_variant,
                hijri_offset.toString(),
                Prefs.get_method_preset(ctx),
                Prefs.get_fajr_angle(ctx).toString(),
                Prefs.get_isha_mode(ctx),
                Prefs.get_isha_angle(ctx).toString(),
                Prefs.get_isha_fixed_minutes(ctx).toString(),
                Prefs.get_asr_factor(ctx).toString(),
                Prefs.get_maghrib_offset_minutes(ctx).toString(),
                Prefs.get_makruh_angle(ctx).toString(),
                Prefs.get_zawal_minutes(ctx).toString()
            ).joinToString("|")

        if (!force && sig == last_sig && (state.loading || state.skeleton != null)) return

        val this_id = ++load_id
        last_sig = sig
        loaded_host = host
        loaded_show_prohibited = show_prohibited
        loaded_show_night = show_night
        state = DaysUiState(loading = true)
        day_inflight.clear()
        day_cache.clear()

        workers.execute {
            try {
                val skel = build_month_skeleton(ctx, host, month_basis, show_hijri_effective, hijri_variant, hijri_offset, month_anchor)
                main.post {
                    if (this_id != load_id) return@post
                    month_start = skel.start
                    month_end = skel.end
                    state = DaysUiState(loading = false, title = skel.title, skeleton = skel)
                }
            } catch (_: ArithmeticException) {
                main.post {
                    if (this_id != load_id) return@post
                    state = DaysUiState(loading = false, error = ctx.getString(R.string.hijri_out_of_range))
                }
            }
        }
    }

    fun ensure_range_loaded(first_index: Int, last_index: Int) {
        val skel = state.skeleton ?: return
        val host = loaded_host ?: return
        val show_prohibited = loaded_show_prohibited
        val show_night = loaded_show_night

        val start = (first_index - 7).coerceAtLeast(0)
        val end = (last_index + 7).coerceAtMost(skel.days.lastIndex)
        val visible_start = first_index.coerceAtLeast(0).coerceAtMost(skel.days.lastIndex)
        val visible_end = last_index.coerceAtLeast(0).coerceAtMost(skel.days.lastIndex)

        // Prioritize rendering what's actually on screen, then prefetch the buffer around it.
        val indices =
            buildList {
                for (i in visible_start..visible_end) add(i)
                for (i in (visible_start - 1) downTo start) add(i)
                for (i in (visible_end + 1)..end) add(i)
            }

        for (i in indices) {
            val meta = skel.days[i]
            if (day_cache.containsKey(meta.day_start)) continue
            if (!day_inflight.add(meta.day_start)) continue

            workers.execute {
                val item = build_day_item(getApplication(), host, meta, show_prohibited, show_night)
                main.post {
                    day_cache[meta.day_start] = item
                    day_inflight.remove(meta.day_start)
                }
            }
        }
    }

    fun shift_month(delta: Int) {
        val ctx = getApplication<Application>().applicationContext
        val host = HostResolver.ensure_default_selected(ctx) ?: return
        val tz = HostConfigReader.read_config(ctx, host)?.timezone?.let(TimeZone::getTimeZone) ?: TimeZone.getDefault()

        val start = month_start
        val end = month_end
        if (start == null || end == null) return

        month_anchor = if (delta < 0) add_days(start, -1, tz) else add_days(end, 1, tz)
        load(force = true)
    }

    override fun onCleared() {
        workers.shutdownNow()
    }
}
