package com.yshalsager.suntimes.prayertimesaddon.core

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ProviderInfo

private const val event_provider_suffix = ".event.provider"

data class HostInfo(
    val label: String,
    val package_name: String,
    val event_authority: String,
    val required_permission: String?
)

object HostResolver {
    private val known_packages = listOf(
        "com.forrestguice.suntimeswidget",
        "com.forrestguice.suntimeswidget.nightly",
        "com.forrestguice.suntimeswidget.legacy"
    )

    fun detect_hosts(context: Context): List<HostInfo> {
        val pm = context.packageManager
        val hosts = ArrayList<HostInfo>(4)
        for (pkg in known_packages) {
            val info = get_pkg_info(pm, pkg) ?: continue
            val base = info.applicationInfo?.let { pm.getApplicationLabel(it).toString() } ?: pkg
            val flavor =
                when (pkg) {
                    "com.forrestguice.suntimeswidget.nightly" -> "nightly"
                    "com.forrestguice.suntimeswidget.legacy" -> "legacy"
                    else -> "stable"
                }
            val label = "$base ($flavor)"
            val authorities = info.providers?.asSequence()
                ?.flatMap { it.authority?.split(';')?.asSequence() ?: emptySequence() }
                ?.map { it.trim() }
                ?.filter { it.endsWith(event_provider_suffix) }
                ?.toList()
                ?: emptyList()
            for (authority in authorities) {
                hosts.add(HostInfo(label, pkg, authority, resolve_required_permission(pm, authority)))
            }
        }
        return hosts
    }

    fun ensure_default_selected(context: Context): String? {
        val existing = Prefs.get_host_event_authority(context)
        return if (!existing.isNullOrBlank()) existing else choose_default_host(context)
    }

    fun choose_default_host(context: Context): String? {
        val pm = context.packageManager
        val detected = detect_hosts(context)
        val selected = (detected.firstOrNull { it.required_permission == null || pm.checkPermission(it.required_permission, context.packageName) == PackageManager.PERMISSION_GRANTED }
            ?: detected.firstOrNull())?.event_authority ?: return null
        Prefs.set_host_event_authority(context, selected)
        return selected
    }

    fun get_required_permission(context: Context, authority: String): String? =
        resolve_required_permission(context.packageManager, authority)

    @Suppress("DEPRECATION")
    private fun get_pkg_info(pm: PackageManager, pkg: String): PackageInfo? {
        return try {
            pm.getPackageInfo(pkg, PackageManager.GET_PROVIDERS)
        } catch (_: Exception) {
            null
        }
    }

    private fun resolve_required_permission(pm: PackageManager, authority: String): String? {
        val provider: ProviderInfo = resolve_content_provider(pm, authority) ?: return null
        return provider.readPermission ?: provider.writePermission
    }

    @Suppress("DEPRECATION")
    private fun resolve_content_provider(pm: PackageManager, authority: String): ProviderInfo? {
        return pm.resolveContentProvider(authority, 0)
    }
}
