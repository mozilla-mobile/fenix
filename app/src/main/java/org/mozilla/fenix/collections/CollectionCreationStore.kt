/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.collections

import mozilla.components.feature.tab.collections.TabCollection
import mozilla.components.lib.state.Action
import mozilla.components.lib.state.State
import mozilla.components.lib.state.Store
import org.mozilla.fenix.home.sessioncontrol.Tab

class CollectionCreationStore(
    initialState: CollectionCreationState
) : Store<CollectionCreationState, CollectionCreationAction>(
    initialState,
    ::collectionCreationReducer
)

enum class SaveCollectionStep {
    SelectTabs,
    SelectCollection,
    NameCollection,
    RenameCollection
}

data class CollectionCreationState(
    val tabs: List<Tab> = emptyList(),
    val selectedTabs: Set<Tab> = emptySet(),
    val saveCollectionStep: SaveCollectionStep = SaveCollectionStep.SelectTabs,
    val tabCollections: List<TabCollection> = emptyList(),
    val selectedTabCollection: TabCollection? = null
) : State

// any actions that don't map to a change will be handled in the controller, and don't need a LibState Action
sealed class CollectionCreationAction : Action {
    data class TabListChange(val tabs: List<Tab>) : CollectionCreationAction() // TODO kdoc
    object AddAllTabs : CollectionCreationAction()
    object RemoveAllTabs : CollectionCreationAction()
    data class TabAdded(val tab: Tab) : CollectionCreationAction()
    data class TabRemoved(val tab: Tab) : CollectionCreationAction()
    data class StepChanged(val saveCollectionStep: SaveCollectionStep) : CollectionCreationAction() // TODO kdoc (and possibly rename)
    data class CollectionSelected(val collection: TabCollection) : CollectionCreationAction()
}

private fun collectionCreationReducer(
    prevState: CollectionCreationState,
    action: CollectionCreationAction
): CollectionCreationState = when (action) {
    else -> TODO()
}
