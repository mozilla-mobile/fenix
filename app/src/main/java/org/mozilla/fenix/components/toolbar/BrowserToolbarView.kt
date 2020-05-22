/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.toolbar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
import com.google.android.material.appbar.AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED
import com.google.android.material.appbar.AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
import com.google.android.material.appbar.AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.component_browser_top_toolbar.*
import mozilla.components.browser.domains.autocomplete.ShippedDomainsProvider
import mozilla.components.browser.session.Session
import mozilla.components.browser.state.selector.findCustomTabOrSelectedTab
import mozilla.components.browser.toolbar.BrowserToolbar
import mozilla.components.browser.toolbar.behavior.BrowserToolbarBottomBehavior
import mozilla.components.browser.toolbar.display.DisplayToolbar.Gravity
import mozilla.components.support.ktx.android.content.getColorFromAttr
import mozilla.components.support.utils.URLStringUtils
import org.mozilla.fenix.FeatureFlags
import org.mozilla.fenix.R
import org.mozilla.fenix.customtabs.CustomTabToolbarIntegration
import org.mozilla.fenix.customtabs.CustomTabToolbarMenu
import org.mozilla.fenix.ext.components

interface BrowserToolbarViewInteractor {
    fun onBrowserToolbarPaste(text: String)
    fun onBrowserToolbarPasteAndGo(text: String)
    fun onBrowserToolbarClicked()
    fun onBrowserToolbarMenuItemTapped(item: ToolbarMenu.Item)
    fun onTabCounterClicked()
    fun onBrowserMenuDismissed(lowPrioHighlightItems: List<ToolbarMenu.Item>)
    fun onScrolled(offset: Int)
}

/**
 * Sets up the [BrowserToolbar] view with the correct appearance and listeners.
 */
@SuppressWarnings("LargeClass")
class BrowserToolbarView(
    container: ViewGroup,
    shouldUseBottomToolbar: Boolean,
    private val shouldUseFixedTopToolbar: Boolean,
    private val interactor: BrowserToolbarViewInteractor,
    private val customTabSession: Session?,
    lifecycleOwner: LifecycleOwner
) : LayoutContainer {

    /**
     * Where the toolbar is located on the screen.
     * [Gravity.TOP] if positioned on the top, [Gravity.BOTTOM] if positioned on the bottom.
     */
    val gravity = if (shouldUseBottomToolbar) Gravity.TOP else Gravity.BOTTOM

    /**
     * Used to lookup and cache Kotlin extensions on the view.
     */
    override val containerView: View get() = layout

    private val context = container.context
    private val components = context.components

    private val layout = LayoutInflater.from(container.context)
        .inflate(getToolbarLayout(), container, true)

    val view: BrowserToolbar = layout.findViewById(R.id.toolbar)
    val toolbarIntegration: ToolbarIntegration

    init {
        if (gravity == Gravity.TOP) {
            val offsetChangedListener = AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
                interactor.onScrolled(verticalOffset)
            }

            app_bar.addOnOffsetChangedListener(offsetChangedListener)
        }

        setScrollFlags(shouldDisableScroll = false)

        view.elevation = context.resources.getDimension(R.dimen.browser_toolbar_elevation)
        view.display.apply {
            if (customTabSession == null) {
                // In custom tabs, Android Components manages the background
                // based on the containing app.
                setUrlBackground(getDrawable(context, R.drawable.search_url_background))
            }

            progressGravity = gravity
            hint = context.getString(R.string.search_hint)

            val primaryTextColor = context.getColorFromAttr(R.attr.primaryText)
            val secondaryTextColor = context.getColorFromAttr(R.attr.secondaryText)
            val separatorColor = context.getColorFromAttr(R.attr.toolbarDivider)
            colors = colors.copy(
                text = primaryTextColor,
                securityIconSecure = primaryTextColor,
                securityIconInsecure = primaryTextColor,
                menu = primaryTextColor,
                hint = secondaryTextColor,
                separator = separatorColor,
                trackingProtection = primaryTextColor
            )

            urlFormatter = { url -> URLStringUtils.toDisplayUrl(url) }
            onUrlClicked = {
                interactor.onBrowserToolbarClicked()
                false
            }
            setOnUrlLongClickListener {
                BrowserToolbarPopupWindow(
                    context = view.context,
                    tab = components.core.store.state.findCustomTabOrSelectedTab(customTabSession?.id),
                    clipboard = components.clipboardHandler,
                    interactor = interactor
                ).showAsDropDown(view)

                true
            }
        }

        val menuToolbar: ToolbarMenu
        if (customTabSession != null) {
            menuToolbar = CustomTabToolbarMenu(
                context,
                components.core.sessionManager,
                customTabSession.id,
                shouldReverseItems = gravity == Gravity.TOP,
                onItemTapped = {
                    interactor.onBrowserToolbarMenuItemTapped(it)
                }
            )
            toolbarIntegration = CustomTabToolbarIntegration(
                context,
                view,
                menuToolbar,
                customTabSession.id,
                isPrivate = customTabSession.private
            )
        } else {
            menuToolbar = DefaultToolbarMenu(
                context = context,
                hasAccountProblem = components.backgroundServices.accountManager.accountNeedsReauth(),
                shouldReverseItems = gravity == Gravity.TOP,
                onItemTapped = { interactor.onBrowserToolbarMenuItemTapped(it) },
                lifecycleOwner = lifecycleOwner,
                sessionManager = components.core.sessionManager,
                store = components.core.store,
                bookmarksStorage = components.core.bookmarksStorage
            )
            toolbarIntegration = DefaultToolbarIntegration(
                context,
                view,
                menuToolbar,
                ShippedDomainsProvider().apply { initialize(context) },
                components.core.historyStorage,
                components.core.sessionManager,
                sessionId = null,
                isPrivate = components.core.sessionManager.selectedSession?.private ?: false,
                interactor = interactor,
                engine = components.core.engine
            )
            view.display.setMenuDismissAction {
                interactor.onBrowserMenuDismissed(menuToolbar.getLowPrioHighlightItems())
                view.invalidateActions()
            }
        }
    }

    fun expand() {
        when (gravity) {
            Gravity.BOTTOM -> {
                if (FeatureFlags.dynamicBottomToolbar) {
                    (view.layoutParams as? CoordinatorLayout.LayoutParams)?.apply {
                        (behavior as BrowserToolbarBottomBehavior).forceExpand(view)
                    }
                }
            }
            Gravity.TOP -> {
                app_bar?.setExpanded(true)
            }
        }
    }

    /**
     * Dynamically sets scroll flags for the toolbar when the user does not have a screen reader enabled
     * Note that the bottom toolbar has a feature flag for being dynamic, so it may not get flags set.
     */
    fun setScrollFlags(shouldDisableScroll: Boolean) {
        when (gravity) {
            Gravity.BOTTOM -> {
                if (FeatureFlags.dynamicBottomToolbar) {
                    (view.layoutParams as? CoordinatorLayout.LayoutParams)?.apply {
                        behavior = BrowserToolbarBottomBehavior(view.context, null)
                    }
                }
            }
            Gravity.TOP -> {
                view.updateLayoutParams<AppBarLayout.LayoutParams> {
                    scrollFlags = if (shouldUseFixedTopToolbar || shouldDisableScroll) {
                        // Force expand the toolbar so the user is not stuck with a hidden toolbar
                        expand()
                        0
                    } else {
                        SCROLL_FLAG_SCROLL or
                                SCROLL_FLAG_ENTER_ALWAYS or
                                SCROLL_FLAG_SNAP or
                                SCROLL_FLAG_EXIT_UNTIL_COLLAPSED
                    }
                }
            }
        }
    }

    @LayoutRes
    private fun getToolbarLayout() = when (gravity) {
        Gravity.BOTTOM -> R.layout.component_bottom_browser_toolbar
        Gravity.TOP -> R.layout.component_browser_top_toolbar
    }
}
