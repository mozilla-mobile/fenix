/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabtray

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeInfo.CollectionInfo
import androidx.annotation.IdRes
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.tabs.TabLayout
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.component_tabstray.view.*
import kotlinx.android.synthetic.main.component_tabstray_fab.view.*
import kotlinx.android.synthetic.main.tabs_tray_tab_counter.*
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mozilla.components.browser.menu.BrowserMenu
import mozilla.components.browser.menu.BrowserMenuBuilder
import mozilla.components.browser.menu.item.SimpleBrowserMenuItem
import mozilla.components.browser.state.selector.getNormalOrPrivateTabs
import mozilla.components.browser.state.selector.normalTabs
import mozilla.components.browser.state.selector.privateTabs
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.tabstray.TabViewHolder
import mozilla.components.feature.syncedtabs.SyncedTabsFeature
import mozilla.components.support.base.feature.ViewBoundFeatureWrapper
import org.mozilla.fenix.R
import org.mozilla.fenix.browser.InfoBanner
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.components.toolbar.TabCounter.Companion.INFINITE_CHAR_PADDING_BOTTOM
import org.mozilla.fenix.components.toolbar.TabCounter.Companion.MAX_VISIBLE_TABS
import org.mozilla.fenix.components.toolbar.TabCounter.Companion.SO_MANY_TABS_OPEN
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.tabtray.SaveToCollectionsButtonAdapter.MultiselectModeChange
import org.mozilla.fenix.tabtray.TabTrayDialogFragmentState.Mode
import java.text.NumberFormat
import mozilla.components.browser.storage.sync.Tab as SyncTab

/**
 * View that contains and configures the BrowserAwesomeBar
 */
@Suppress("LongParameterList", "TooManyFunctions", "LargeClass")
class TabTrayView(
    private val container: ViewGroup,
    private val tabsAdapter: FenixTabsAdapter,
    private val interactor: TabTrayInteractor,
    store: TabTrayDialogFragmentStore,
    isPrivate: Boolean,
    startingInLandscape: Boolean,
    lifecycleOwner: LifecycleOwner,
    private val filterTabs: (Boolean) -> Unit
) : LayoutContainer, TabLayout.OnTabSelectedListener {
    val lifecycleScope = lifecycleOwner.lifecycleScope
    val fabView = LayoutInflater.from(container.context)
        .inflate(R.layout.component_tabstray_fab, container, true)

    private val hasAccessibilityEnabled = container.context.settings().accessibilityServicesEnabled

    val view = LayoutInflater.from(container.context)
        .inflate(R.layout.component_tabstray, container, true)

    private val isPrivateModeSelected: Boolean get() = view.tab_layout.selectedTabPosition == PRIVATE_TAB_ID

    private val behavior = BottomSheetBehavior.from(view.tab_wrapper)

    private val concatAdapter = ConcatAdapter(tabsAdapter)
    private val tabTrayItemMenu: TabTrayItemMenu
    private var menu: BrowserMenu? = null

    private var tabsTouchHelper: TabsTouchHelper
    private val collectionsButtonAdapter = SaveToCollectionsButtonAdapter(interactor, isPrivate)

    private val syncedTabsController =
        SyncedTabsController(lifecycleOwner, view, store, concatAdapter)
    private val syncedTabsFeature = ViewBoundFeatureWrapper<SyncedTabsFeature>()

    private var hasLoaded = false

    override val containerView: View?
        get() = container

    private val components = container.context.components

    init {
        components.analytics.metrics.track(Event.TabsTrayOpened)

        toggleFabText(isPrivate)

        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                if (interactor.onModeRequested() is Mode.Normal && !hasAccessibilityEnabled) {
                    if (slideOffset >= SLIDE_OFFSET) {
                        fabView.new_tab_button.show()
                    } else {
                        fabView.new_tab_button.hide()
                    }
                }
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    components.analytics.metrics.track(Event.TabsTrayClosed)
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

        if (view.context.settings().syncedTabsInTabsTray) {
            syncedTabsFeature.set(
                feature = SyncedTabsFeature(
                    context = container.context,
                    storage = components.backgroundServices.syncedTabsStorage,
                    accountManager = components.backgroundServices.accountManager,
                    view = syncedTabsController,
                    lifecycleOwner = lifecycleOwner,
                    onTabClicked = ::handleTabClicked
                ),
                owner = lifecycleOwner,
                view = view
            )
        }

        view.tabsTray.apply {
            layoutManager = LinearLayoutManager(container.context).apply {
                reverseLayout = true
                stackFromEnd = true
            }
            adapter = concatAdapter

            tabsTouchHelper = TabsTouchHelper(
                observable = tabsAdapter,
                onViewHolderTouched = { it is TabViewHolder }
            )

            tabsTouchHelper.attachToRecyclerView(this)

            tabsAdapter.tabTrayInteractor = interactor
            tabsAdapter.onTabsUpdated = {
                // Put the 'Add to collections' button after the tabs have loaded.
                concatAdapter.addAdapter(0, collectionsButtonAdapter)

                // Put the Synced Tabs adapter at the end.
                concatAdapter.addAdapter(0, syncedTabsController.adapter)

                if (hasAccessibilityEnabled) {
                    tabsAdapter.notifyItemRangeChanged(0, tabs.size)
                }
                if (!hasLoaded) {
                    hasLoaded = true
                    scrollToTab(view.context.components.core.store.state.selectedTabId)
                    if (view.context.settings().accessibilityServicesEnabled) {
                        lifecycleScope.launch {
                            delay(SELECTION_DELAY.toLong())
                            lifecycleScope.launch(Main) {
                                layoutManager?.findViewByPosition(selectedBrowserTabIndex)
                                    ?.requestFocus()
                                layoutManager?.findViewByPosition(selectedBrowserTabIndex)
                                    ?.sendAccessibilityEvent(
                                        AccessibilityEvent.TYPE_VIEW_FOCUSED
                                    )
                            }
                        }
                    }
                }
            }
        }

        tabTrayItemMenu =
            TabTrayItemMenu(
                view.context,
                { tabs.isNotEmpty() && view.tab_layout.selectedTabPosition == 0 },
                { tabs.isNotEmpty() }) {
                when (it) {
                    is TabTrayItemMenu.Item.ShareAllTabs -> interactor.onShareTabsClicked(
                        isPrivateModeSelected
                    )
                    is TabTrayItemMenu.Item.OpenTabSettings -> interactor.onTabSettingsClicked()
                    is TabTrayItemMenu.Item.SaveToCollection -> interactor.onEnterMultiselect()
                    is TabTrayItemMenu.Item.CloseAllTabs -> interactor.onCloseAllTabsClicked(
                        isPrivateModeSelected
                    )
                    is TabTrayItemMenu.Item.OpenRecentlyClosed -> interactor.onOpenRecentlyClosedClicked()
                }
            }

        view.tab_tray_overflow.setOnClickListener {
            components.analytics.metrics.track(Event.TabsTrayMenuOpened)
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

        adjustNewTabButtonsForNormalMode()

        if (
            view.context.settings().shouldShowAutoCloseTabsBanner &&
            view.context.settings().canShowCfr &&
            tabs.size >= TAB_COUNT_SHOW_CFR
        ) {
            InfoBanner(
                context = view.context,
                message = view.context.getString(R.string.tab_tray_close_tabs_banner_message),
                dismissText = view.context.getString(R.string.tab_tray_close_tabs_banner_negative_button_text),
                actionText = view.context.getString(R.string.tab_tray_close_tabs_banner_positive_button_text),
                container = view.infoBanner,
                dismissByHiding = true,
                dismissAction = { view.context.settings().shouldShowAutoCloseTabsBanner = false }
            ) {
                interactor.onSetUpAutoCloseTabsClicked()
                view.context.settings().shouldShowAutoCloseTabsBanner = false
            }.apply {
                view.infoBanner.visibility = View.VISIBLE
                showBanner()
            }
        }
    }

    private fun handleTabClicked(tab: SyncTab) {
        interactor.onSyncedTabClicked(tab)
    }

    private fun adjustNewTabButtonsForNormalMode() {
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

        components.analytics.metrics.track(eventToSend)
    }

    fun expand() {
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    enum class TabChange {
        PRIVATE, NORMAL
    }

    private fun toggleSaveToCollectionButton(isPrivate: Boolean) {
        collectionsButtonAdapter.notifyItemChanged(
            0,
            if (isPrivate) TabChange.PRIVATE else TabChange.NORMAL
        )
    }

    override fun onTabSelected(tab: TabLayout.Tab?) {
        toggleFabText(isPrivateModeSelected)
        filterTabs.invoke(isPrivateModeSelected)
        toggleSaveToCollectionButton(isPrivateModeSelected)

        updateUINormalMode(view.context.components.core.store.state)
        scrollToTab(view.context.components.core.store.state.selectedTabId)

        if (isPrivateModeSelected) {
            components.analytics.metrics.track(Event.TabsTrayPrivateModeTapped)
        } else {
            components.analytics.metrics.track(Event.TabsTrayNormalModeTapped)
        }
    }

    override fun onTabReselected(tab: TabLayout.Tab?) = Unit
    override fun onTabUnselected(tab: TabLayout.Tab?) = Unit

    var mode: Mode = Mode.Normal
        private set

    fun updateState(state: TabTrayDialogFragmentState) {
        val oldMode = mode

        if (oldMode::class != state.mode::class) {
            updateTabsForMultiselectModeChanged(state.mode is Mode.MultiSelect)
            if (view.context.settings().accessibilityServicesEnabled) {
                view.announceForAccessibility(
                    if (state.mode == Mode.Normal) view.context.getString(
                        R.string.tab_tray_exit_multiselect_content_description
                    ) else view.context.getString(R.string.tab_tray_enter_multiselect_content_description)
                )
            }
        }

        mode = state.mode
        when (state.mode) {
            Mode.Normal -> {
                view.tabsTray.apply {
                    tabsTouchHelper.attachToRecyclerView(this)
                }

                toggleUIMultiselect(multiselect = false)

                updateUINormalMode(state.browserState)
            }
            is Mode.MultiSelect -> {
                // Disable swipe to delete while in multiselect
                tabsTouchHelper.attachToRecyclerView(null)

                toggleUIMultiselect(multiselect = true)

                fabView.new_tab_button.isVisible = false
                view.tab_tray_new_tab.isVisible = false
                view.collect_multi_select.isVisible = state.mode.selectedItems.isNotEmpty()

                view.multiselect_title.text = view.context.getString(
                    R.string.tab_tray_multi_select_title,
                    state.mode.selectedItems.size
                )
                view.collect_multi_select.setOnClickListener {
                    interactor.onSaveToCollectionClicked(state.mode.selectedItems)
                }
                view.exit_multi_select.setOnClickListener {
                    interactor.onBackPressed()
                }
            }
        }

        if (oldMode.selectedItems != state.mode.selectedItems) {
            val unselectedItems = oldMode.selectedItems - state.mode.selectedItems

            state.mode.selectedItems.union(unselectedItems).forEach { item ->
                if (view.context.settings().accessibilityServicesEnabled) {
                    view.announceForAccessibility(
                        if (unselectedItems.contains(item)) view.context.getString(
                            R.string.tab_tray_item_unselected_multiselect_content_description,
                            item.title
                        ) else view.context.getString(
                            R.string.tab_tray_item_selected_multiselect_content_description,
                            item.title
                        )
                    )
                }
                updateTabsForSelectionChanged(item.id)
            }
        }
    }

    private fun ConstraintLayout.setChildWPercent(percentage: Float, @IdRes childId: Int) {
        this.findViewById<View>(childId)?.let {
            val constraintSet = ConstraintSet()
            constraintSet.clone(this)
            constraintSet.constrainPercentWidth(it.id, percentage)
            constraintSet.applyTo(this)
            it.requestLayout()
        }
    }

    private fun updateUINormalMode(browserState: BrowserState) {
        val hasNoTabs = if (isPrivateModeSelected) {
            browserState.privateTabs.isEmpty()
        } else {
            browserState.normalTabs.isEmpty()
        }

        view.tab_tray_empty_view.isVisible = hasNoTabs
        if (hasNoTabs) {
            view.tab_tray_empty_view.text = if (isPrivateModeSelected) {
                view.context.getString(R.string.no_private_tabs_description)
            } else {
                view.context?.getString(R.string.no_open_tabs_description)
            }
        }

        view.tabsTray.visibility = if (hasNoTabs) {
            View.INVISIBLE
        } else {
            View.VISIBLE
        }

        counter_text.text = updateTabCounter(browserState.normalTabs.size)
        updateTabCounterContentDescription(browserState.normalTabs.size)

        adjustNewTabButtonsForNormalMode()
    }

    private fun toggleUIMultiselect(multiselect: Boolean) {
        view.multiselect_title.isVisible = multiselect
        view.collect_multi_select.isVisible = multiselect
        view.exit_multi_select.isVisible = multiselect

        view.topBar.setBackgroundColor(
            ContextCompat.getColor(
                view.context,
                if (multiselect) R.color.accent_normal_theme else R.color.foundation_normal_theme
            )
        )

        view.handle.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            height = view.resources.getDimensionPixelSize(
                if (multiselect) {
                    R.dimen.tab_tray_multiselect_handle_height
                } else {
                    R.dimen.bottom_sheet_handle_height
                }
            )
            topMargin = view.resources.getDimensionPixelSize(
                if (multiselect) {
                    R.dimen.tab_tray_multiselect_handle_top_margin
                } else {
                    R.dimen.bottom_sheet_handle_top_margin
                }
            )
        }

        view.tab_wrapper.setChildWPercent(
            if (multiselect) 1F else NORMAL_HANDLE_PERCENT_WIDTH,
            view.handle.id
        )

        view.handle.setBackgroundColor(
            ContextCompat.getColor(
                view.context,
                if (multiselect) R.color.accent_normal_theme else R.color.secondary_text_normal_theme
            )
        )

        view.tab_layout.isVisible = !multiselect
        view.tab_tray_empty_view.isVisible = !multiselect
        view.tab_tray_overflow.isVisible = !multiselect
        view.tab_layout.isVisible = !multiselect
    }

    private fun updateTabsForMultiselectModeChanged(inMultiselectMode: Boolean) {
        view.tabsTray.apply {
            val tabs = view.context.components.core.store.state.getNormalOrPrivateTabs(
                isPrivateModeSelected
            )

            collectionsButtonAdapter.notifyItemChanged(
                0,
                if (inMultiselectMode) MultiselectModeChange.MULTISELECT else MultiselectModeChange.NORMAL
            )

            tabsAdapter.notifyItemRangeChanged(0, tabs.size, true)
        }
    }

    private fun updateTabsForSelectionChanged(itemId: String) {
        view.tabsTray.apply {
            val tabs = view.context.components.core.store.state.getNormalOrPrivateTabs(
                isPrivateModeSelected
            )

            val selectedBrowserTabIndex = tabs.indexOfFirst { it.id == itemId }

            tabsAdapter.notifyItemChanged(
                selectedBrowserTabIndex, true
            )
        }
    }

    private fun updateTabCounterContentDescription(count: Int) {
        view.tab_layout.getTabAt(0)?.contentDescription = if (count == 1) {
            view.context?.getString(R.string.open_tab_tray_single)
        } else {
            view.context?.getString(R.string.open_tab_tray_plural, count.toString())
        }

        view.tabsTray.accessibilityDelegate = object : View.AccessibilityDelegate() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View?,
                info: AccessibilityNodeInfo?
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info?.let {
                    info.collectionInfo = CollectionInfo.obtain(
                        tabsAdapter.tabCount,
                        1,
                        false
                    )
                }
            }
        }
    }

    private fun updateTabCounter(count: Int): String {
        if (count > MAX_VISIBLE_TABS) {
            counter_text.updatePadding(bottom = INFINITE_CHAR_PADDING_BOTTOM)
            return SO_MANY_TABS_OPEN
        }
        return NumberFormat.getInstance().format(count.toLong())
    }

    fun setTopOffset(landscape: Boolean) {
        val topOffset = if (landscape) {
            0
        } else {
            view.resources.getDimensionPixelSize(R.dimen.tab_tray_top_offset)
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
                view.context.getString(R.string.add_private_tab)
        } else {
            fabView.new_tab_button.shrink()
            fabView.new_tab_button.contentDescription =
                view.context.getString(R.string.add_tab)
        }
    }

    fun onBackPressed(): Boolean {
        return interactor.onBackPressed()
    }

    fun scrollToTab(sessionId: String?) {
        view.tabsTray.apply {
            val tabs = if (isPrivateModeSelected) {
                view.context.components.core.store.state.privateTabs
            } else {
                view.context.components.core.store.state.normalTabs
            }

            val selectedBrowserTabIndex = tabs
                .indexOfFirst { it.id == sessionId }

            // We offset the tab index by the number of items in the other adapters.
            // We add the offset, because the layoutManager is initialized with `reverseLayout`.
            // We also add 1 to display the tab item above the selected browser tab.
            val recyclerViewIndex = selectedBrowserTabIndex +
                    collectionsButtonAdapter.itemCount +
                    syncedTabsController.adapter.itemCount +
                    1

            layoutManager?.scrollToPosition(recyclerViewIndex)
        }
    }

    companion object {
        private const val TAB_COUNT_SHOW_CFR = 6
        private const val DEFAULT_TAB_ID = 0
        private const val PRIVATE_TAB_ID = 1
        private const val EXPAND_AT_SIZE = 3
        private const val SLIDE_OFFSET = 0
        private const val SELECTION_DELAY = 500
        private const val NORMAL_HANDLE_PERCENT_WIDTH = 0.1F
    }
}

class TabTrayItemMenu(
    private val context: Context,
    private val shouldShowSaveToCollection: () -> Boolean,
    private val hasOpenTabs: () -> Boolean,
    private val onItemTapped: (Item) -> Unit = {}
) {

    sealed class Item {
        object ShareAllTabs : Item()
        object OpenTabSettings : Item()
        object SaveToCollection : Item()
        object CloseAllTabs : Item()
        object OpenRecentlyClosed : Item()
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
            }.apply { visible = hasOpenTabs },

            SimpleBrowserMenuItem(
                context.getString(R.string.tab_tray_menu_tab_settings),
                textColorResource = R.color.primary_text_normal_theme
            ) {
                onItemTapped.invoke(Item.OpenTabSettings)
            },

            SimpleBrowserMenuItem(
                context.getString(R.string.tab_tray_menu_recently_closed),
                textColorResource = R.color.primary_text_normal_theme
            ) {
                onItemTapped.invoke(Item.OpenRecentlyClosed)
            },

            SimpleBrowserMenuItem(
                context.getString(R.string.tab_tray_menu_item_close),
                textColorResource = R.color.primary_text_normal_theme
            ) {
                context.components.analytics.metrics.track(Event.TabsTrayCloseAllTabsPressed)
                onItemTapped.invoke(Item.CloseAllTabs)
            }.apply { visible = hasOpenTabs }
        )
    }
}
