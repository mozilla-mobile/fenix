/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.sessioncontrol

import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.feature.tab.collections.TabCollection
import mozilla.components.feature.top.sites.TopSite
import org.mozilla.fenix.components.tips.Tip
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.home.HomeFragmentState
import org.mozilla.fenix.home.HomeScreenViewModel
import org.mozilla.fenix.home.Mode
import org.mozilla.fenix.home.OnboardingState

// This method got a little complex with the addition of the tab tray feature flag
// When we remove the tabs from the home screen this will get much simpler again.
@Suppress("ComplexMethod", "LongParameterList")
private fun normalModeAdapterItems(
    topSites: List<TopSite>,
    collections: List<TabCollection>,
    expandedCollections: Set<Long>,
    tip: Tip?,
    showCollectionsPlaceholder: Boolean,
    showSetAsDefaultBrowserCard: Boolean,
    recentTabs: List<TabSessionState>
): List<AdapterItem> {
    val items = mutableListOf<AdapterItem>()

    tip?.let { items.add(AdapterItem.TipItem(it)) }

    if (showSetAsDefaultBrowserCard) {
        items.add(AdapterItem.ExperimentDefaultBrowserCard)
    }

    if (topSites.isNotEmpty()) {
        items.add(AdapterItem.TopSitePager(topSites))
    }

    if (recentTabs.isNotEmpty()) {
        showRecentTabs(recentTabs, items)
    }

    if (collections.isEmpty()) {
        if (showCollectionsPlaceholder) {
            items.add(AdapterItem.NoCollectionsMessage)
        }
    } else {
        showCollections(collections, expandedCollections, items)
    }

    return items
}

private fun showRecentTabs(
    recentTabs: List<TabSessionState>,
    items: MutableList<AdapterItem>
) {
    items.add(AdapterItem.RecentTabsHeader)
    recentTabs.forEach {
        items.add(AdapterItem.RecentTabItem(it))
    }
}

private fun showCollections(
    collections: List<TabCollection>,
    expandedCollections: Set<Long>,
    items: MutableList<AdapterItem>
) {
    // If the collection is expanded, we want to add all of its tabs beneath it in the adapter
    items.add(AdapterItem.CollectionHeader)
    collections.map {
        AdapterItem.CollectionItem(it, expandedCollections.contains(it.id))
    }.forEach {
        items.add(it)
        if (it.expanded) {
            items.addAll(collectionTabItems(it.collection))
        }
    }
}

private fun privateModeAdapterItems() = listOf(AdapterItem.PrivateBrowsingDescription)

private fun onboardingAdapterItems(onboardingState: OnboardingState): List<AdapterItem> {
    val items: MutableList<AdapterItem> = mutableListOf(AdapterItem.OnboardingHeader)

    items.addAll(
        listOf(
            AdapterItem.OnboardingThemePicker,
            AdapterItem.OnboardingToolbarPositionPicker,
            AdapterItem.OnboardingTrackingProtection
        )
    )
    // Customize FxA items based on where we are with the account state:
    items.addAll(
        when (onboardingState) {
            OnboardingState.SignedOutNoAutoSignIn -> {
                listOf(
                    AdapterItem.OnboardingManualSignIn
                )
            }
            is OnboardingState.SignedOutCanAutoSignIn -> {
                listOf(
                    AdapterItem.OnboardingAutomaticSignIn(onboardingState)
                )
            }
            OnboardingState.SignedIn -> listOf()
        }
    )

    items.addAll(
        listOf(
            AdapterItem.OnboardingPrivacyNotice,
            AdapterItem.OnboardingFinish
        )
    )

    return items
}

private fun HomeFragmentState.toAdapterList(): List<AdapterItem> = when (mode) {
    is Mode.Normal -> normalModeAdapterItems(
        topSites,
        collections,
        expandedCollections,
        tip,
        showCollectionPlaceholder,
        showSetAsDefaultBrowserCard,
        recentTabs
    )
    is Mode.Private -> privateModeAdapterItems()
    is Mode.Onboarding -> onboardingAdapterItems(mode.state)
}

private fun collectionTabItems(collection: TabCollection) =
    collection.tabs.mapIndexed { index, tab ->
        AdapterItem.TabInCollectionItem(collection, tab, index == collection.tabs.lastIndex)
    }

class SessionControlView(
    override val containerView: View,
    viewLifecycleOwner: LifecycleOwner,
    interactor: SessionControlInteractor,
    private var homeScreenViewModel: HomeScreenViewModel
) : LayoutContainer {

    val view: RecyclerView = containerView as RecyclerView

    private val sessionControlAdapter = SessionControlAdapter(
        interactor,
        viewLifecycleOwner,
        containerView.context.components
    )

    init {
        view.apply {
            adapter = sessionControlAdapter
            layoutManager = LinearLayoutManager(containerView.context)
            val itemTouchHelper =
                ItemTouchHelper(
                    SwipeToDeleteCallback(
                        interactor
                    )
                )
            itemTouchHelper.attachToRecyclerView(this)
        }
    }

    fun update(state: HomeFragmentState) {

        val stateAdapterList = state.toAdapterList()
        if (homeScreenViewModel.shouldScrollToTopSites) {
            sessionControlAdapter.submitList(stateAdapterList) {

                val loadedTopSites = stateAdapterList.find { adapterItem ->
                    adapterItem is AdapterItem.TopSitePager && adapterItem.topSites.isNotEmpty()
                }
                loadedTopSites?.run {
                    homeScreenViewModel.shouldScrollToTopSites = false
                    view.scrollToPosition(0)
                }
            }
        } else {
            sessionControlAdapter.submitList(stateAdapterList)
        }
    }
}
