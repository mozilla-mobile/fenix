/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.collections

import org.mozilla.fenix.home.sessioncontrol.Tab
import org.mozilla.fenix.home.sessioncontrol.TabCollection

class DefaultCollectionCreationInteractor(
    private val controller: CollectionCreationController
) : CollectionCreationInteractor {
    override fun onNewCollectionNameSaved(tabs: List<Tab>, name: String) {
        controller.saveCollectionName(tabs, name)
    }

    override fun onCollectionRenamed(collection: TabCollection, name: String) {
        controller.renameCollection(collection, name)
    }

    override fun onBackPressed(fromStep: SaveCollectionStep) {
        controller.backPressed(fromStep)
    }

    override fun selectAllTapped() {
        controller.selectAllTapped()
    }

    override fun deselectAllTapped() {
        controller.deselectAllTapped()
    }

    override fun close() {
        controller.close()
    }

    override fun selectCollection(collection: TabCollection, tabs: List<Tab>) {
        controller.selectCollection(collection, tabs)
    }

    override fun saveTabsToCollection(tabs: List<Tab>) {
        controller.saveTabsToCollection(tabs)
    }

    override fun addNewCollection() {
        controller.addNewCollection()
    }

    override fun addTabToSelection(tab: Tab) {
        controller.addTabToSelection(tab)
    }

    override fun removeTabFromSelection(tab: Tab) {
        controller.removeTabFromSelection(tab)
    }
}