package org.mozilla.fenix.helpers.idlingresource

import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.NavHostFragment
import androidx.test.espresso.IdlingResource
import mozilla.components.feature.addons.ui.AddonInstallationDialogFragment

class AddonsInstallingIdlingResource(
    val fragmentManager: FragmentManager
) :
    IdlingResource {
    private var resourceCallback: IdlingResource.ResourceCallback? = null
    private var isAddonInstalled = false

    override fun getName(): String {
        return this::javaClass.name
    }

    override fun isIdleNow(): Boolean {
        return isInstalledAddonDialogShown()
    }

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
        if (callback != null)
            resourceCallback = callback
    }

    private fun isInstalledAddonDialogShown(): Boolean {
        val activityChildFragments =
            (fragmentManager.fragments.first() as NavHostFragment)
                .childFragmentManager.fragments

        for (childFragment in activityChildFragments.indices) {
            if (activityChildFragments[childFragment] is AddonInstallationDialogFragment) {
                resourceCallback?.onTransitionToIdle()
                isAddonInstalled = true
                return isAddonInstalled
            }
        }
        return isAddonInstalled
    }
}
