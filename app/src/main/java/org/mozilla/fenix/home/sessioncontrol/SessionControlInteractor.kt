/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.sessioncontrol

import mozilla.components.feature.tab.collections.Tab
import mozilla.components.feature.tab.collections.TabCollection

/**
 * Interface for collection related actions in the [SessionControlInteractor].
 */
interface CollectionInteractor {
    /**
     * Shows the Collection Creation fragment for selecting the tabs to add to the given tab
     * collection. Called when a user taps on the "Add tab" collection menu item.
     *
     * @param collection The collection of tabs that will be modified.
     */
    fun onCollectionAddTabTapped(collection: TabCollection)

    /**
     * Opens the given tab. Called when a user clicks on a tab in the tab collection.
     *
     * @param tab The tab to open from the tab collection.
     */
    fun onCollectionOpenTabClicked(tab: Tab)

    /**
     * Opens all the tabs in a given tab collection. Called when a user taps on the "Open tabs"
     * collection menu item.
     *
     * @param collection The collection of tabs to open.
     */
    fun onCollectionOpenTabsTapped(collection: TabCollection)

    /**
     * Removes the given tab from the given tab collection. Called when a user swipes to remove a
     * tab or clicks on the tab close button.
     *
     * @param collection The collection of tabs that will be modified.
     * @param tab The tab to remove from the tab collection.
     */
    fun onCollectionRemoveTab(collection: TabCollection, tab: Tab)

    /**
     * Shares the tabs in the given tab collection. Called when a user clicks on the Collection
     * Share button.
     *
     * @param collection The collection of tabs to share.
     */
    fun onCollectionShareTabsClicked(collection: TabCollection)

    /**
     * Shows a prompt for deleting the given tab collection. Called when a user taps on the
     * "Delete collection" collection menu item.
     *
     * @param collection The collection of tabs to delete.
     */
    fun onDeleteCollectionTapped(collection: TabCollection)

    /**
     * Shows the Collection Creation fragment for renaming the given tab collection. Called when a
     * user taps on the "Rename collection" collection menu item.
     *
     * @param collection The collection of tabs to rename.
     */
    fun onRenameCollectionTapped(collection: TabCollection)

    /**
     * Toggles expanding or collapsing the given tab collection. Called when a user clicks on a
     * [CollectionViewHolder].
     *
     * @param collection The collection of tabs that will be collapsed.
     * @param expand True if the given tab collection should be expanded or collapse if false.
     */
    fun onToggleCollectionExpanded(collection: TabCollection, expand: Boolean)
}

/**
 * Interface for onboarding related actions in the [SessionControlInteractor].
 */
interface OnboardingInteractor {
    /**
     * Hides the onboarding. Called when a user clicks on the "Start Browsing" button.
     */
    fun onStartBrowsingClicked()
}

/**
 * Interactor for the Home screen.
 * Provides implementations for the CollectionInteractor, OnboardingInteractor and
 * TabSessionInteractor.
 */
@SuppressWarnings("TooManyFunctions")
class SessionControlInteractor(
    private val controller: SessionControlController
) : CollectionInteractor, OnboardingInteractor {
    override fun onCollectionAddTabTapped(collection: TabCollection) {
        controller.handleCollectionAddTabTapped(collection)
    }

    override fun onCollectionOpenTabClicked(tab: Tab) {
        controller.handleCollectionOpenTabClicked(tab)
    }

    override fun onCollectionOpenTabsTapped(collection: TabCollection) {
        controller.handleCollectionOpenTabsTapped(collection)
    }

    override fun onCollectionRemoveTab(collection: TabCollection, tab: Tab) {
        controller.handleCollectionRemoveTab(collection, tab)
    }

    override fun onCollectionShareTabsClicked(collection: TabCollection) {
        controller.handleCollectionShareTabsClicked(collection)
    }

    override fun onDeleteCollectionTapped(collection: TabCollection) {
        controller.handleDeleteCollectionTapped(collection)
    }

    override fun onRenameCollectionTapped(collection: TabCollection) {
        controller.handleRenameCollectionTapped(collection)
    }

    override fun onStartBrowsingClicked() {
        controller.handleStartBrowsingClicked()
    }

    override fun onToggleCollectionExpanded(collection: TabCollection, expand: Boolean) {
        controller.handleToggleCollectionExpanded(collection, expand)
    }
}
