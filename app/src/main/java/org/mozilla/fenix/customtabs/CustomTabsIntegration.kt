/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.customtabs

import android.app.Activity
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.browser.toolbar.BrowserToolbar
import mozilla.components.browser.toolbar.display.DisplayToolbar
import mozilla.components.feature.customtabs.CustomTabsToolbarFeature
import mozilla.components.feature.tabs.CustomTabsUseCases
import mozilla.components.support.base.feature.LifecycleAwareFeature
import mozilla.components.support.base.feature.UserInteractionHandler
import org.mozilla.fenix.R
import org.mozilla.fenix.components.toolbar.ToolbarMenu
import org.mozilla.fenix.ext.settings

class CustomTabsIntegration(
    store: BrowserStore,
    useCases: CustomTabsUseCases,
    toolbar: BrowserToolbar,
    sessionId: String,
    activity: Activity,
    onItemTapped: (ToolbarMenu.Item) -> Unit = {},
    shouldReverseItems: Boolean,
    isPrivate: Boolean
) : LifecycleAwareFeature, UserInteractionHandler {

    init {
        // Remove toolbar shadow
        toolbar.elevation = 0f

        val uncoloredEtpShield = getDrawable(activity, R.drawable.ic_tracking_protection_enabled)!!

        toolbar.display.icons = toolbar.display.icons.copy(
            // Custom private tab backgrounds have bad contrast against the colored shield
            trackingProtectionTrackersBlocked = uncoloredEtpShield,
            trackingProtectionNothingBlocked = uncoloredEtpShield,
            trackingProtectionException = getDrawable(
                activity,
                R.drawable.ic_tracking_protection_disabled
            )!!
        )

        toolbar.display.displayIndicatorSeparator = false
        if (activity.settings().shouldUseTrackingProtection) {
            toolbar.display.indicators = listOf(
                DisplayToolbar.Indicators.SECURITY,
                DisplayToolbar.Indicators.TRACKING_PROTECTION
            )
        } else {
            toolbar.display.indicators = listOf(
                DisplayToolbar.Indicators.SECURITY
            )
        }

        // If in private mode, override toolbar background to use private color
        // See #5334
        if (isPrivate) {
            toolbar.background = getDrawable(activity, R.drawable.toolbar_background)
        }
    }

    private val customTabToolbarMenu by lazy {
        CustomTabToolbarMenu(
            activity,
            store,
            sessionId,
            shouldReverseItems,
            onItemTapped = onItemTapped
        )
    }

    private val feature = CustomTabsToolbarFeature(
        store,
        toolbar,
        sessionId,
        useCases,
        menuBuilder = customTabToolbarMenu.menuBuilder,
        menuItemIndex = START_OF_MENU_ITEMS_INDEX,
        window = activity.window,
        shareListener = { onItemTapped.invoke(ToolbarMenu.Item.Share) },
        closeListener = { activity.finishAndRemoveTask() },
        updateToolbarBackground = !isPrivate,
        forceActionButtonTinting = isPrivate
    )

    override fun start() = feature.start()
    override fun stop() = feature.stop()
    override fun onBackPressed() = feature.onBackPressed()

    companion object {
        private const val START_OF_MENU_ITEMS_INDEX = 2
    }
}
