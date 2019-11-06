/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.customtabs

import android.app.Activity
import android.view.Gravity
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.widget.NestedScrollView
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
import mozilla.components.browser.session.SessionManager
import mozilla.components.browser.toolbar.BrowserToolbar
import mozilla.components.browser.toolbar.display.DisplayToolbar
import mozilla.components.feature.customtabs.CustomTabsToolbarFeature
import mozilla.components.support.base.feature.BackHandler
import mozilla.components.support.base.feature.LifecycleAwareFeature
import org.mozilla.fenix.R
import org.mozilla.fenix.components.toolbar.ToolbarMenu
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.theme.ThemeManager

class CustomTabsIntegration(
    sessionManager: SessionManager,
    toolbar: BrowserToolbar,
    sessionId: String,
    activity: Activity,
    quickActionbar: NestedScrollView,
    engineLayout: View,
    onItemTapped: (ToolbarMenu.Item) -> Unit = {}
) : LifecycleAwareFeature, BackHandler {

    init {
        // Remove toolbar shadow
        toolbar.elevation = 0f

        // Reduce margin height of EngineView from the top for the toolbar
        engineLayout.run {
            (layoutParams as CoordinatorLayout.LayoutParams).apply {
                val toolbarHeight = resources.getDimension(R.dimen.browser_toolbar_height).toInt()
                setMargins(0, toolbarHeight, 0, 0)
            }
        }

        // Make the toolbar go to the top.
        toolbar.run {
            (layoutParams as CoordinatorLayout.LayoutParams).apply {
                gravity = Gravity.TOP
            }
        }

        // Hide the Quick Action Bar.
        quickActionbar.visibility = View.GONE

        val task = LottieCompositionFactory
            .fromRawRes(
                activity,
                ThemeManager.resolveAttribute(R.attr.shieldLottieFile, activity)
            )
        task.addListener { result ->
            val lottieDrawable = LottieDrawable()
            lottieDrawable.composition = result

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

            toolbar.display.icons = toolbar.display.icons.copy(
                trackingProtectionTrackersBlocked = lottieDrawable,
                trackingProtectionNothingBlocked = AppCompatResources.getDrawable(
                    activity,
                    R.drawable.ic_tracking_protection_enabled
                )!!,
                trackingProtectionException = AppCompatResources.getDrawable(
                    activity,
                    R.drawable.ic_tracking_protection_disabled
                )!!
            )
        }
    }

    private val customTabToolbarMenu by lazy {
        CustomTabToolbarMenu(
            activity,
            sessionManager,
            sessionId,
            onItemTapped = onItemTapped
        )
    }

    private val feature = CustomTabsToolbarFeature(
        sessionManager,
        toolbar,
        sessionId,
        menuBuilder = customTabToolbarMenu.menuBuilder,
        menuItemIndex = START_OF_MENU_ITEMS_INDEX,
        window = activity.window,
        closeListener = { activity.finish() }
    )

    override fun start() {
        feature.start()
    }

    override fun stop() {
        feature.stop()
    }

    override fun onBackPressed(): Boolean {
        return feature.onBackPressed()
    }

    companion object {
        const val START_OF_MENU_ITEMS_INDEX = 2
    }
}
