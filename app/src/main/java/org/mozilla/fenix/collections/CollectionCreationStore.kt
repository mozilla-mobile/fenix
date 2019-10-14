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
    val previousFragmentId: Int,
    val tabs: List<Tab> = emptyList(),
    val selectedTabs: Set<Tab> = emptySet(),
    val saveCollectionStep: SaveCollectionStep = SaveCollectionStep.SelectTabs,
    val tabCollections: List<TabCollection> = emptyList(),
    val selectedTabCollection: TabCollection? = null
) : State

sealed class CollectionCreationAction : Action {
    object AddAllTabs : CollectionCreationAction()
    object RemoveAllTabs : CollectionCreationAction()
    data class TabAdded(val tab: Tab) : CollectionCreationAction()
    data class TabRemoved(val tab: Tab) : CollectionCreationAction()
    // TODO kdoc (and possibly rename)
    data class StepChanged(val saveCollectionStep: SaveCollectionStep) : CollectionCreationAction()
}

private fun collectionCreationReducer(
    prevState: CollectionCreationState,
    action: CollectionCreationAction
): CollectionCreationState = when (action) {
    is CollectionCreationAction.AddAllTabs -> prevState.copy(selectedTabs = prevState.tabs.toSet())
    is CollectionCreationAction.RemoveAllTabs -> prevState.copy(selectedTabs = emptySet())
    is CollectionCreationAction.TabAdded -> prevState.copy(selectedTabs = prevState.selectedTabs + action.tab)
    is CollectionCreationAction.TabRemoved -> prevState.copy(selectedTabs = prevState.selectedTabs - action.tab)
    is CollectionCreationAction.StepChanged -> prevState.copy(saveCollectionStep = action.saveCollectionStep)
}
