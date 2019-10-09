/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.collections

import org.mozilla.fenix.home.sessioncontrol.Tab
import org.mozilla.fenix.home.sessioncontrol.TabCollection

/**
// * TODO
 */
class CollectionCreationInteractor(
    private val controller: CollectionCreationController
) : CollectionViewInteractor {
    override fun saveCollectionName(tabs: List<Tab>, name: String) {
        controller.saveCollectionName(tabs, name)
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun renameCollection(collection: TabCollection, name: String) {
        controller.renameCollection(collection, name)
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun backPressed(fromStep: SaveCollectionStep) {
        controller.backPressed(fromStep)
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun selectAllTapped() {
        controller.selectAllTapped()
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deselectAllTapped() {
        controller.deselectAllTapped()
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun close() {
        controller.close()
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun selectCollection(collection: TabCollection, tabs: List<Tab>) {
        controller.selectCollection(collection, tabs)
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun saveTabsToCollection(tabs: List<Tab>) {
        controller.saveTabsToCollection(tabs)
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun addNewCollection() {
        controller.addNewCollection()
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun addTabToSelection(tab: Tab) {
        controller.addTabToSelection(tab)
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun removeTabFromSelection(tab: Tab) {
        controller.removeTabFromSelection(tab)
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}