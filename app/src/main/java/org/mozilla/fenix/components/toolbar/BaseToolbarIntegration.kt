/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.toolbar

import android.content.Context
import androidx.appcompat.content.res.AppCompatResources
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
import mozilla.components.browser.toolbar.BrowserToolbar
import mozilla.components.feature.toolbar.ToolbarFeature
import mozilla.components.feature.toolbar.ToolbarPresenter
import mozilla.components.lib.publicsuffixlist.PublicSuffixList
import mozilla.components.support.base.feature.LifecycleAwareFeature
import org.mozilla.fenix.FeatureFlags
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.theme.ThemeManager

abstract class BaseToolbarIntegration(
    context: Context,
    toolbar: BrowserToolbar,
    toolbarMenu: ToolbarMenu,
    renderStyle: ToolbarFeature.RenderStyle,
    sessionId: String?,
    isPrivate: Boolean,
    displaySeperator: Boolean
) : LifecycleAwareFeature {

    init {
        toolbar.setMenuBuilder(toolbarMenu.menuBuilder)
        toolbar.private = isPrivate

        LottieCompositionFactory
            .fromRawRes(context, ThemeManager.resolveAttribute(R.attr.shieldLottieFile, context))
            .addListener { result ->
                val lottieDrawable = LottieDrawable()
                lottieDrawable.composition = result
                val useEnhancedTrackingProtection =
                    context.settings().shouldUseTrackingProtection && FeatureFlags.etpCategories
                toolbar.displayTrackingProtectionIcon = useEnhancedTrackingProtection
                toolbar.displaySeparatorView = displaySeperator && useEnhancedTrackingProtection

                toolbar.setTrackingProtectionIcons(
                    iconOnNoTrackersBlocked = AppCompatResources.getDrawable(
                        context,
                        R.drawable.ic_tracking_protection_enabled
                    )!!,
                    iconOnTrackersBlocked = lottieDrawable,
                    iconDisabledForSite = AppCompatResources.getDrawable(
                        context,
                        R.drawable.ic_tracking_protection_disabled
                    )!!
                )
            }
    }

    private val toolbarPresenter: ToolbarPresenter = ToolbarPresenter(
        toolbar,
        context.components.core.store,
        sessionId,
        ToolbarFeature.UrlRenderConfiguration(
            PublicSuffixList(context),
            registrableDomainColor = ThemeManager.resolveAttribute(R.attr.primaryText, context),
            renderStyle = renderStyle
        )
    )

    private var menuPresenter =
        MenuPresenter(toolbar, context.components.core.sessionManager, sessionId)

    final override fun start() {
        menuPresenter.start()
        toolbarPresenter.start()
    }

    final override fun stop() {
        menuPresenter.stop()
        toolbarPresenter.stop()
    }
}
