/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home

import androidx.annotation.VisibleForTesting
import androidx.datastore.core.DataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import mozilla.components.lib.state.Action
import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.MiddlewareContext
import mozilla.components.lib.state.Store
import mozilla.components.service.pocket.PocketStoriesService
import mozilla.components.service.pocket.PocketStory
import mozilla.components.service.pocket.PocketStory.PocketRecommendedStory
import mozilla.components.service.pocket.PocketStory.PocketSponsoredStory
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.appstate.AppAction
import org.mozilla.fenix.components.appstate.AppState
import org.mozilla.fenix.datastore.SelectedPocketStoriesCategories
import org.mozilla.fenix.datastore.SelectedPocketStoriesCategories.SelectedPocketStoriesCategory
import org.mozilla.fenix.home.pocket.PocketRecommendedStoriesCategory
import org.mozilla.fenix.home.pocket.PocketRecommendedStoriesSelectedCategory

/**
 * [AppStore] middleware reacting in response to Pocket related [Action]s.
 *
 * @param pocketStoriesService [PocketStoriesService] used for updating details about the Pocket recommended stories.
 * @param selectedPocketCategoriesDataStore [DataStore] used for reading or persisting details about the
 * currently selected Pocket recommended stories categories.
 * @param coroutineScope [CoroutineScope] used for long running operations like disk IO.
 */
class PocketUpdatesMiddleware(
    private val pocketStoriesService: PocketStoriesService,
    private val selectedPocketCategoriesDataStore: DataStore<SelectedPocketStoriesCategories>,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
) : Middleware<AppState, AppAction> {
    override fun invoke(
        context: MiddlewareContext<AppState, AppAction>,
        next: (AppAction) -> Unit,
        action: AppAction
    ) {
        // Pre process actions
        when (action) {
            is AppAction.PocketStoriesCategoriesChange -> {
                // Intercept the original action which would only update categories and
                // dispatch a new action which also updates which categories are selected by the user
                // from previous locally persisted data.
                restoreSelectedCategories(
                    coroutineScope = coroutineScope,
                    currentCategories = action.storiesCategories,
                    store = context.store,
                    selectedPocketCategoriesDataStore = selectedPocketCategoriesDataStore
                )
            }
            else -> {
                // no-op
            }
        }

        next(action)

        // Post process actions
        when (action) {
            is AppAction.PocketStoriesShown -> {
                persistStoriesImpressions(
                    coroutineScope = coroutineScope,
                    pocketStoriesService = pocketStoriesService,
                    updatedStories = action.storiesShown
                )
            }
            is AppAction.SelectPocketStoriesCategory,
            is AppAction.DeselectPocketStoriesCategory -> {
                persistSelectedCategories(
                    coroutineScope = coroutineScope,
                    currentCategoriesSelections = context.state.pocketStoriesCategoriesSelections,
                    selectedPocketCategoriesDataStore = selectedPocketCategoriesDataStore
                )
            }
            else -> {
                // no-op
            }
        }
    }
}

/**
 * Persist [updatedStories] for making their details available in between app restarts.
 *
 * @param coroutineScope [CoroutineScope] used for reading the locally persisted data.
 * @param pocketStoriesService [PocketStoriesService] used for updating details about the Pocket recommended stories.
 * @param updatedStories the list of stories to persist.
 */
@VisibleForTesting
internal fun persistStoriesImpressions(
    coroutineScope: CoroutineScope,
    pocketStoriesService: PocketStoriesService,
    updatedStories: List<PocketStory>
) {
    coroutineScope.launch {
        pocketStoriesService.updateStoriesTimesShown(
            updatedStories.filterIsInstance<PocketRecommendedStory>()
                .map {
                    it.copy(timesShown = it.timesShown.inc())
                }
        )

        pocketStoriesService.recordStoriesImpressions(
            updatedStories.filterIsInstance<PocketSponsoredStory>()
                .map { it.id }
        )
    }
}

/**
 * Persist [currentCategoriesSelections] for making this details available in between app restarts.
 *
 * @param coroutineScope [CoroutineScope] used for reading the locally persisted data.
 * @param currentCategoriesSelections Currently selected Pocket recommended stories categories.
 * @param selectedPocketCategoriesDataStore - DataStore used for persisting [currentCategoriesSelections].
 */
@VisibleForTesting
internal fun persistSelectedCategories(
    coroutineScope: CoroutineScope,
    currentCategoriesSelections: List<PocketRecommendedStoriesSelectedCategory>,
    selectedPocketCategoriesDataStore: DataStore<SelectedPocketStoriesCategories>
) {
    val selectedCategories = currentCategoriesSelections
        .map {
            SelectedPocketStoriesCategory.newBuilder().apply {
                name = it.name
                selectionTimestamp = it.selectionTimestamp
            }.build()
        }

    // Irrespective of the current selections or their number overwrite everything we had.
    coroutineScope.launch {
        selectedPocketCategoriesDataStore.updateData { data ->
            data.newBuilderForType().addAllValues(selectedCategories).build()
        }
    }
}

/**
 * Combines [currentCategories] with the locally persisted data about previously selected categories
 * and emits a new [AppAction.PocketStoriesCategoriesSelectionsChange] to update these in store.
 *
 * @param coroutineScope [CoroutineScope] used for reading the locally persisted data.
 * @param currentCategories Stories categories currently available
 * @param store [Store] that will be updated.
 * @param selectedPocketCategoriesDataStore [DataStore] containing details about the previously selected
 * stories categories.
 */
@VisibleForTesting
internal fun restoreSelectedCategories(
    coroutineScope: CoroutineScope,
    currentCategories: List<PocketRecommendedStoriesCategory>,
    store: Store<AppState, AppAction>,
    selectedPocketCategoriesDataStore: DataStore<SelectedPocketStoriesCategories>
) {
    coroutineScope.launch {
        store.dispatch(
            AppAction.PocketStoriesCategoriesSelectionsChange(
                currentCategories,
                selectedPocketCategoriesDataStore.data.first()
                    .valuesList.map {
                        PocketRecommendedStoriesSelectedCategory(
                            name = it.name,
                            selectionTimestamp = it.selectionTimestamp
                        )
                    }
            )
        )
    }
}
