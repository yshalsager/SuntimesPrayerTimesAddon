package com.yshalsager.suntimes.prayertimesaddon.core

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.ProviderInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

private const val known_host_package = "com.forrestguice.suntimeswidget"
private const val known_host_authority = "com.forrestguice.suntimeswidget.event.provider"
private const val nightly_host_package = "com.forrestguice.suntimeswidget.nightly"
private const val nightly_host_authority = "com.forrestguice.suntimeswidget.nightly.event.provider"
private const val stale_authority = "com.stale.host.event.provider"
private const val ad_hoc_authority = "com.any.host.event.provider"

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class HostResolverTest {
    private lateinit var context: Context

    @Before
    fun set_up() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE).edit().clear().apply()
    }

    @Test
    fun ensure_default_selected_returns_existing_when_provider_resolves() {
        register_provider(context.packageName)
        Prefs.set_host_event_authority(context, ad_hoc_authority)

        val selected = HostResolver.ensure_default_selected(context)

        assertEquals(ad_hoc_authority, selected)
    }

    @Test
    fun ensure_default_selected_falls_back_to_known_host_when_existing_is_stale() {
        Prefs.set_host_event_authority(context, stale_authority)
        register_host_package()

        val selected = HostResolver.ensure_default_selected(context)

        assertEquals(known_host_authority, selected)
        assertEquals(known_host_authority, Prefs.get_host_event_authority(context))
    }

    @Test
    fun ensure_default_selected_returns_null_when_no_provider_resolves() {
        Prefs.set_host_event_authority(context, stale_authority)

        val selected = HostResolver.ensure_default_selected(context)

        assertNull(selected)
    }

    @Test
    fun choose_default_host_prefers_authority_without_missing_permission() {
        register_host_package(read_permission = "com.forrestguice.suntimeswidget.permission.EVENTS")
        register_host_package(package_name = nightly_host_package, authority = nightly_host_authority)

        val selected = HostResolver.choose_default_host(context)

        assertEquals(nightly_host_authority, selected)
        assertEquals(nightly_host_authority, Prefs.get_host_event_authority(context))
    }

    @Test
    fun detect_hosts_filters_non_event_authorities_in_provider_list() {
        register_host_package(authority = "com.forrestguice.suntimeswidget.config.provider;$known_host_authority")

        val hosts = HostResolver.detect_hosts(context)

        assertEquals(listOf(known_host_authority), hosts.map { it.event_authority })
    }

    private fun register_host_package(
        package_name: String = known_host_package,
        authority: String = known_host_authority,
        read_permission: String? = null
    ) {
        val app_info =
            ApplicationInfo().apply {
                packageName = package_name
                name = package_name
            }
        val provider =
            ProviderInfo().apply {
                this.authority = authority
                packageName = package_name
                applicationInfo = app_info
                name = "$package_name.TestProvider"
                this.readPermission = read_permission
            }

        val pkg =
            PackageInfo().apply {
                packageName = package_name
                applicationInfo = app_info
                providers = arrayOf(provider)
            }

        val shadow_package_manager = shadowOf(context.packageManager)
        shadow_package_manager.installPackage(pkg)
        shadow_package_manager.addOrUpdateProvider(provider)
    }

    private fun register_provider(package_name: String) {
        val provider =
            ProviderInfo().apply {
                this.authority = ad_hoc_authority
                this.packageName = package_name
                this.applicationInfo = context.applicationInfo
                this.name = "$package_name.TestProvider"
            }
        shadowOf(context.packageManager).addOrUpdateProvider(provider)
    }
}
