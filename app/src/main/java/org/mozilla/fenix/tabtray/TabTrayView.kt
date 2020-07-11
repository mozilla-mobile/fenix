/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabtray

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.tabs.TabLayout
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.component_tabstray.view.*
import kotlinx.android.synthetic.main.component_tabstray_fab.view.*
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mozilla.components.browser.menu.BrowserMenu
import mozilla.components.browser.menu.BrowserMenuBuilder
import mozilla.components.browser.menu.item.SimpleBrowserMenuItem
import mozilla.components.browser.state.selector.normalTabs
import mozilla.components.browser.state.selector.privateTabs
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.tabstray.BrowserTabsTray
import org.mozilla.fenix.R
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.settings

/**
 * View that contains and configures the BrowserAwesomeBar
 */
@Suppress("LongParameterList")
class TabTrayView(
    private val container: ViewGroup,
    private val interactor: TabTrayInteractor,
    isPrivate: Boolean,
    startingInLandscape: Boolean,
    lifecycleScope: LifecycleCoroutineScope,
    private val filterTabs: ((TabSessionState) -> Boolean) -> Unit
) : LayoutContainer, TabLayout.OnTabSelectedListener {
    val fabView = LayoutInflater.from(container.context)
        .inflate(R.layout.component_tabstray_fab, container, true)

    val view = LayoutInflater.from(container.context)
        .inflate(R.layout.component_tabstray, container, true)

    val isPrivateModeSelected: Boolean get() = view.tab_layout.selectedTabPosition == PRIVATE_TAB_ID

    private val behavior = BottomSheetBehavior.from(view.tab_wrapper)

    private val tabTrayItemMenu: TabTrayItemMenu
    private var menu: BrowserMenu? = null

    private var hasLoaded = false

    override val containerView: View?
        get() = container

    init {
        container.context.components.analytics.metrics.track(Event.TabsTrayOpened)

        val hasAccessibilityEnabled = view.context.settings().accessibilityServicesEnabled

        toggleFabText(isPrivate)

        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                if (!hasAccessibilityEnabled) {
                    if (slideOffset >= SLIDE_OFFSET) {
                        fabView.new_tab_button.show()
                    } else {
                        fabView.new_tab_button.hide()
                    }
                }
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    container.context.components.analytics.metrics.track(Event.TabsTrayClosed)
                    interactor.onTabTrayDismissed()
                }
            }
        })

        val selectedTabIndex = if (!isPrivate) {
            DEFAULT_TAB_ID
        } else {
            PRIVATE_TAB_ID
        }

        view.tab_layout.getTabAt(selectedTabIndex)?.also {
            view.tab_layout.selectTab(it, true)
        }

        view.tab_layout.addOnTabSelectedListener(this)

        val tabs = if (isPrivate) {
            view.context.components.core.store.state.privateTabs
        } else {
            view.context.components.core.store.state.normalTabs
        }

        val selectedBrowserTabIndex = tabs
            .indexOfFirst { it.id == view.context.components.core.store.state.selectedTabId }

        if (tabs.size > EXPAND_AT_SIZE || startingInLandscape) {
            expand()
        }

        setTopOffset(startingInLandscape)

        (view.tabsTray as? BrowserTabsTray)?.also { tray ->
            TabsTouchHelper(tray.tabsAdapter).attachToRecyclerView(tray)
            (tray.tabsAdapter as? FenixTabsAdapter)?.also { adapter ->
                adapter.onTabsUpdated = {
                    if (hasAccessibilityEnabled) {
                        adapter.notifyDataSetChanged()
                    }
                    if (!hasLoaded) {
                        hasLoaded = true
                        scrollToTab(view.context.components.core.store.state.selectedTabId)
                        if (view.context.settings().accessibilityServicesEnabled) {
                            lifecycleScope.launch {
                                delay(SELECTION_DELAY.toLong())
                                lifecycleScope.launch(Main) {
                                    tray.layoutManager?.findViewByPosition(selectedBrowserTabIndex)
                                        ?.requestFocus()
                                    tray.layoutManager?.findViewByPosition(selectedBrowserTabIndex)
                                        ?.sendAccessibilityEvent(
                                            AccessibilityEvent.TYPE_VIEW_FOCUSED
                                        )
                                }
                            }
                        }
                    }
                }
            }
        }

        tabTrayItemMenu =
            TabTrayItemMenu(view.context, { view.tab_layout.selectedTabPosition == 0 }) {
                when (it) {
                    is TabTrayItemMenu.Item.ShareAllTabs -> interactor.onShareTabsClicked(
                        isPrivateModeSelected
                    )
                    is TabTrayItemMenu.Item.SaveToCollection -> interactor.onSaveToCollectionClicked()
                    is TabTrayItemMenu.Item.CloseAllTabs -> interactor.onCloseAllTabsClicked(
                        isPrivateModeSelected
                    )
                }
            }

        view.tab_tray_overflow.setOnClickListener {
            container.context.components.analytics.metrics.track(Event.TabsTrayMenuOpened)
            menu = tabTrayItemMenu.menuBuilder.build(container.context)
            menu?.show(it)
                ?.also { pu ->
                    (pu.contentView as? CardView)?.setCardBackgroundColor(
                        ContextCompat.getColor(
                            view.context,
                            R.color.foundation_normal_theme
                        )
                    )
                }
        }

        view.tab_tray_new_tab.apply {
            isVisible = hasAccessibilityEnabled
            setOnClickListener {
                sendNewTabEvent(isPrivateModeSelected)
                interactor.onNewTabTapped(isPrivateModeSelected)
            }
        }

        fabView.new_tab_button.apply {
            isVisible = !hasAccessibilityEnabled
            setOnClickListener {
                sendNewTabEvent(isPrivateModeSelected)
                interactor.onNewTabTapped(isPrivateModeSelected)
            }
        }
    }

    private fun sendNewTabEvent(isPrivateModeSelected: Boolean) {
        val eventToSend = if (isPrivateModeSelected) {
            Event.NewPrivateTabTapped
        } else {
            Event.NewTabTapped
        }

        container.context.components.analytics.metrics.track(eventToSend)
    }

    fun expand() {
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    override fun onTabSelected(tab: TabLayout.Tab?) {
        // We need a better way to determine which tab was selected.
        val filter: (TabSessionState) -> Boolean = when (tab?.position) {
            1 -> { state -> state.content.private }
            else -> { state -> !state.content.private }
        }

        toggleFabText(isPrivateModeSelected)
        filterTabs.invoke(filter)

        updateState(view.context.components.core.store.state)
        scrollToTab(view.context.components.core.store.state.selectedTabId)

        if (isPrivateModeSelected) {
            container.context.components.analytics.metrics.track(Event.TabsTrayPrivateModeTapped)
        } else {
            container.context.components.analytics.metrics.track(Event.TabsTrayNormalModeTapped)
        }
    }

    override fun onTabReselected(tab: TabLayout.Tab?) { /*noop*/
    }

    override fun onTabUnselected(tab: TabLayout.Tab?) { /*noop*/
    }

    fun updateState(state: BrowserState) {
        view.let {
            val hasNoTabs = if (isPrivateModeSelected) {
                state.privateTabs.isEmpty()
            } else {
                state.normalTabs.isEmpty()
            }

            view.tab_tray_empty_view.isVisible = hasNoTabs
            if (hasNoTabs) {
                view.tab_tray_empty_view.text = if (isPrivateModeSelected) {
                    view.context.getString(R.string.no_private_tabs_description)
                } else {
                    view.context?.getString(R.string.no_open_tabs_description)
                }
            }

            view.tabsTray.asView().visibility = if (hasNoTabs) {
                View.INVISIBLE
            } else {
                View.VISIBLE
            }
            view.tab_tray_overflow.isVisible = !hasNoTabs
        }
    }

    fun setTopOffset(landscape: Boolean) {
        val topOffset = if (landscape) {
            0
        } else {
            view.context.resources.getDimension(R.dimen.tab_tray_top_offset).toInt()
        }

        behavior.setExpandedOffset(topOffset)
    }

    fun dismissMenu() {
        menu?.dismiss()
    }

    private fun toggleFabText(private: Boolean) {
        if (private) {
            fabView.new_tab_button.extend()
            fabView.new_tab_button.contentDescription =
                view.context.resources.getString(R.string.add_private_tab)
        } else {
            fabView.new_tab_button.shrink()
            fabView.new_tab_button.contentDescription =
                view.context.resources.getString(R.string.add_tab)
        }
    }

    fun scrollToTab(sessionId: String?) {
        (view.tabsTray as? BrowserTabsTray)?.also { tray ->
            val tabs = if (isPrivateModeSelected) {
                view.context.components.core.store.state.privateTabs
            } else {
                view.context.components.core.store.state.normalTabs
            }

            val selectedBrowserTabIndex = tabs
                .indexOfFirst { it.id == sessionId }

            tray.layoutManager?.scrollToPosition(selectedBrowserTabIndex)
        }
    }

    companion object {
        private const val DEFAULT_TAB_ID = 0
        private const val PRIVATE_TAB_ID = 1
        private const val EXPAND_AT_SIZE = 3
        private const val SLIDE_OFFSET = 0
        private const val SELECTION_DELAY = 500
    }
}

class TabTrayItemMenu(
    private val context: Context,
    private val shouldShowSaveToCollection: () -> Boolean,
    private val onItemTapped: (Item) -> Unit = {}
) {

    sealed class Item {
        object ShareAllTabs : Item()
        object SaveToCollection : Item()
        object CloseAllTabs : Item()
    }

    val menuBuilder by lazy { BrowserMenuBuilder(menuItems) }

    private val menuItems by lazy {
        listOf(
            SimpleBrowserMenuItem(
                context.getString(R.string.tab_tray_menu_item_save),
                textColorResource = R.color.primary_text_normal_theme
            ) {
                context.components.analytics.metrics.track(Event.TabsTraySaveToCollectionPressed)
                onItemTapped.invoke(Item.SaveToCollection)
            }.apply { visible = shouldShowSaveToCollection },

            SimpleBrowserMenuItem(
                context.getString(R.string.tab_tray_menu_item_share),
                textColorResource = R.color.primary_text_normal_theme
            ) {
                context.components.analytics.metrics.track(Event.TabsTrayShareAllTabsPressed)
                onItemTapped.invoke(Item.ShareAllTabs)
            },

            SimpleBrowserMenuItem(
                context.getString(R.string.tab_tray_menu_item_close),
                textColorResource = R.color.primary_text_normal_theme
            ) {
                context.components.analytics.metrics.track(Event.TabsTrayCloseAllTabsPressed)
                onItemTapped.invoke(Item.CloseAllTabs)
            }
        )
    }
}
