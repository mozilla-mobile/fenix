/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home

import android.view.View
import io.mockk.mockk
import io.mockk.verify
import mozilla.components.feature.tab.collections.Tab
import mozilla.components.feature.tab.collections.TabCollection
import org.junit.Before
import org.junit.Test
import org.mozilla.fenix.home.sessioncontrol.DefaultSessionControlController
import org.mozilla.fenix.home.sessioncontrol.SessionControlInteractor

class SessionControlInteractorTest {

    private val controller: DefaultSessionControlController = mockk(relaxed = true)

    private lateinit var interactor: SessionControlInteractor

    @Before
    fun setup() {
        interactor = SessionControlInteractor(controller)
    }

    @Test
    fun onCloseTab() {
        val sessionId = "hello"
        interactor.onCloseTab(sessionId)
        verify { controller.handleCloseTab(sessionId) }
    }

    @Test
    fun onCloseAllTabs() {
        val isPrivateMode = true
        interactor.onCloseAllTabs(isPrivateMode)
        verify { controller.handleCloseAllTabs(isPrivateMode) }
    }

    @Test
    fun onCollectionAddTabTapped() {
        val collection: TabCollection = mockk(relaxed = true)
        interactor.onCollectionAddTabTapped(collection)
        verify { controller.handleCollectionAddTabTapped(collection) }
    }

    @Test
    fun onCollectionOpenTabClicked() {
        val tab: Tab = mockk(relaxed = true)
        interactor.onCollectionOpenTabClicked(tab)
        verify { controller.handleCollectionOpenTabClicked(tab) }
    }

    @Test
    fun onCollectionOpenTabsTapped() {
        val collection: TabCollection = mockk(relaxed = true)
        interactor.onCollectionOpenTabsTapped(collection)
        verify { controller.handleCollectionOpenTabsTapped(collection) }
    }

    @Test
    fun onCollectionRemoveTab() {
        val collection: TabCollection = mockk(relaxed = true)
        val tab: Tab = mockk(relaxed = true)
        interactor.onCollectionRemoveTab(collection, tab)
        verify { controller.handleCollectionRemoveTab(collection, tab) }
    }

    @Test
    fun onCollectionShareTabsClicked() {
        val collection: TabCollection = mockk(relaxed = true)
        interactor.onCollectionShareTabsClicked(collection)
        verify { controller.handleCollectionShareTabsClicked(collection) }
    }

    @Test
    fun onDeleteCollectionTapped() {
        val collection: TabCollection = mockk(relaxed = true)
        interactor.onDeleteCollectionTapped(collection)
        verify { controller.handleDeleteCollectionTapped(collection) }
    }

    @Test
    fun onPauseMediaClicked() {
        interactor.onPauseMediaClicked()
        verify { controller.handlePauseMediaClicked() }
    }

    @Test
    fun onPlayMediaClicked() {
        interactor.onPlayMediaClicked()
        verify { controller.handlePlayMediaClicked() }
    }

    @Test
    fun onPrivateBrowsingLearnMoreClicked() {
        interactor.onPrivateBrowsingLearnMoreClicked()
        verify { controller.handlePrivateBrowsingLearnMoreClicked() }
    }

    @Test
    fun onRenameCollectionTapped() {
        val collection: TabCollection = mockk(relaxed = true)
        interactor.onRenameCollectionTapped(collection)
        verify { controller.handleRenameCollectionTapped(collection) }
    }

    @Test
    fun onSaveToCollection() {
        interactor.onSaveToCollection(null)
        verify { controller.handleSaveTabToCollection(null) }
    }

    @Test
    fun onSelectTab() {
        val tabView: View = mockk(relaxed = true)
        val sessionId = "hello"
        interactor.onSelectTab(tabView, sessionId)
        verify { controller.handleSelectTab(tabView, sessionId) }
    }

    @Test
    fun onShareTabs() {
        interactor.onShareTabs()
        verify { controller.handleShareTabs() }
    }

    @Test
    fun onStartBrowsingClicked() {
        interactor.onStartBrowsingClicked()
        verify { controller.handleStartBrowsingClicked() }
    }

    @Test
    fun onToggleCollectionExpanded() {
        val collection: TabCollection = mockk(relaxed = true)
        interactor.onToggleCollectionExpanded(collection, true)
        verify { controller.handleToggleCollectionExpanded(collection, true) }
    }
}
