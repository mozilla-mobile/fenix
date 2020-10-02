/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("TooManyFunctions")

package org.mozilla.fenix.collections

import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import mozilla.components.feature.tab.collections.TabCollection
import org.mozilla.fenix.components.TabCollectionStorage
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.components.metrics.MetricController
import org.mozilla.fenix.ext.getDefaultCollectionNumber
import org.mozilla.fenix.ext.normalSessionSize
import org.mozilla.fenix.home.Tab

interface CollectionCreationController {

    fun saveCollectionName(tabs: List<Tab>, name: String)

    fun renameCollection(collection: TabCollection, name: String)

    /**
     * See [CollectionCreationInteractor.onBackPressed]
     */
    fun backPressed(fromStep: SaveCollectionStep)

    /**
     * See [CollectionCreationInteractor.selectAllTapped]
     */
    fun selectAllTabs()

    /**
     * See [CollectionCreationInteractor.deselectAllTapped]
     */
    fun deselectAllTabs()

    /**
     * See [CollectionCreationInteractor.close]
     */
    fun close()

    fun selectCollection(collection: TabCollection, tabs: List<Tab>)

    /**
     * See [CollectionCreationInteractor.saveTabsToCollection]
     */
    fun saveTabsToCollection(tabs: List<Tab>)

    fun addNewCollection()

    fun addTabToSelection(tab: Tab)

    fun removeTabFromSelection(tab: Tab)
}

fun List<Tab>.toSessionBundle(sessionManager: SessionManager): List<Session> {
    return this.mapNotNull { sessionManager.findSessionById(it.sessionId) }
}

/**
 * @param store Store used to hold in-memory collection state.
 * @param dismiss Callback to dismiss the collection creation dialog.
 * @param metrics Controller that handles telemetry events.
 * @param tabCollectionStorage Storage used to save tab collections to disk.
 * @param sessionManager Used to query and serialize tabs.
 * @param scope Coroutine scope to launch coroutines.
 */
class DefaultCollectionCreationController(
    private val store: CollectionCreationStore,
    private val dismiss: () -> Unit,
    private val metrics: MetricController,
    private val tabCollectionStorage: TabCollectionStorage,
    private val sessionManager: SessionManager,
    private val scope: CoroutineScope
) : CollectionCreationController {

    companion object {
        const val DEFAULT_INCREMENT_VALUE = 1
        const val DEFAULT_COLLECTION_NUMBER_POSITION = 1
    }

    override fun saveCollectionName(tabs: List<Tab>, name: String) {
        dismiss()

        val sessionBundle = tabs.toSessionBundle(sessionManager)
        scope.launch {
            tabCollectionStorage.createCollection(name, sessionBundle)
        }

        metrics.track(
            Event.CollectionSaved(sessionManager.normalSessionSize(), sessionBundle.size)
        )
    }

    override fun renameCollection(collection: TabCollection, name: String) {
        dismiss()
        scope.launch {
            tabCollectionStorage.renameCollection(collection, name)
        }
        metrics.track(Event.CollectionRenamed)
    }

    override fun backPressed(fromStep: SaveCollectionStep) {
        val newStep = stepBack(fromStep)
        if (newStep != null) {
            store.dispatch(CollectionCreationAction.StepChanged(newStep))
        } else {
            dismiss()
        }
    }

    override fun selectAllTabs() {
        store.dispatch(CollectionCreationAction.AddAllTabs)
    }

    override fun deselectAllTabs() {
        store.dispatch(CollectionCreationAction.RemoveAllTabs)
    }

    override fun close() {
        dismiss()
    }

    override fun selectCollection(collection: TabCollection, tabs: List<Tab>) {
        dismiss()
        val sessionBundle = tabs.toList().toSessionBundle(sessionManager)
        scope.launch {
            tabCollectionStorage
                .addTabsToCollection(collection, sessionBundle)
        }

        metrics.track(
            Event.CollectionTabsAdded(sessionManager.normalSessionSize(), sessionBundle.size)
        )
    }

    override fun saveTabsToCollection(tabs: List<Tab>) {
        store.dispatch(
            CollectionCreationAction.StepChanged(
                saveCollectionStep = if (store.state.tabCollections.isEmpty()) {
                    SaveCollectionStep.NameCollection
                } else {
                    SaveCollectionStep.SelectCollection
                },
                defaultCollectionNumber = store.state.tabCollections.getDefaultCollectionNumber()
            )
        )
    }

    override fun addNewCollection() {
        store.dispatch(
            CollectionCreationAction.StepChanged(
                SaveCollectionStep.NameCollection,
                store.state.tabCollections.getDefaultCollectionNumber()
            )
        )
    }

    override fun addTabToSelection(tab: Tab) {
        store.dispatch(CollectionCreationAction.TabAdded(tab))
    }

    override fun removeTabFromSelection(tab: Tab) {
        store.dispatch(CollectionCreationAction.TabRemoved(tab))
    }

    /**
     * Will return the next valid state according to this diagram.
     *
     * Name Collection -> Select Collection -> Select Tabs -> (dismiss fragment) <- Rename Collection
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun stepBack(
        backFromStep: SaveCollectionStep
    ): SaveCollectionStep? {
        val tabCollectionCount = store.state.tabCollections.size
        val tabCount = store.state.tabs.size

        return when (backFromStep) {
            SaveCollectionStep.NameCollection -> if (tabCollectionCount > 0) {
                SaveCollectionStep.SelectCollection
            } else {
                stepBack(SaveCollectionStep.SelectCollection)
            }
            SaveCollectionStep.SelectCollection -> if (tabCount > 1) {
                SaveCollectionStep.SelectTabs
            } else {
                stepBack(SaveCollectionStep.SelectTabs)
            }
            SaveCollectionStep.SelectTabs, SaveCollectionStep.RenameCollection -> null
        }
    }
}
