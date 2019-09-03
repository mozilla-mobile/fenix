package org.mozilla.fenix.share

import android.content.pm.ResolveInfo
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import org.junit.Test
import org.robolectric.shadows.ShadowResolveInfo.newResolveInfo

class ShareFragmentTest {
    @Test
    fun `filterProviders() removes unwanted providers`() {
        val provider1 = newResolveInfo(
            "provider 1", "activity 1", "package 1")
        val provider2 = newResolveInfo(
            "provider 2", "activity 2", "package 2")
        val unwantedProvider1 = newResolveInfo(
            "unwanted provider 1",
            "com.google.android.apps.maps",
            "com.google.android.apps.gmm.sharing.SendTextToClipboardActivity")

        val initialProviders: List<ResolveInfo> = listOf(provider1, provider2)
        val unwantedProviders: List<ResolveInfo> = listOf(unwantedProvider1)

        val allProviders = initialProviders + unwantedProviders
        val acceptedProviders = with(ShareFragment()) {
            filterProviders(allProviders)
        }

        assertThat(acceptedProviders).isNotNull()
        assertThat(acceptedProviders!!.size).isEqualTo(initialProviders.size)
        assertThat(acceptedProviders.any { unwantedProviders.contains(it) }).isFalse()
    }
}
