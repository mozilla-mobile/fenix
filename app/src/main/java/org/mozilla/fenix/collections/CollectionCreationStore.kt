/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.collections

import mozilla.components.feature.tab.collections.TabCollection
import org.mozilla.fenix.home.sessioncontrol.Tab
import org.mozilla.fenix.mvi.Change
import org.mozilla.fenix.mvi.ViewState

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
) : ViewState

// any actions that don't map to a change will be handled in the controller, and don't need a LibState Action
// TODO update these to LibState Action
sealed class CollectionCreationChange : Change {
    data class TabListChange(val tabs: List<Tab>) : CollectionCreationChange()
    object AddAllTabs : CollectionCreationChange()
    object RemoveAllTabs : CollectionCreationChange()
    data class TabAdded(val tab: Tab) : CollectionCreationChange()
    data class TabRemoved(val tab: Tab) : CollectionCreationChange()
    data class StepChanged(val saveCollectionStep: SaveCollectionStep) : CollectionCreationChange() // TODO kdoc this. preferably rename it too
    data class CollectionSelected(val collection: TabCollection) : CollectionCreationChange()
}
