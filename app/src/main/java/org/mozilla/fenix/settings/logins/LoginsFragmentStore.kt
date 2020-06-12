/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.logins

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import mozilla.components.concept.storage.Login
import mozilla.components.lib.state.Action
import mozilla.components.lib.state.State
import mozilla.components.lib.state.Store

/**
 * Class representing a parcelable saved logins item
 * @property guid The id of the saved login
 * @property origin Site of the saved login
 * @property username Username that's saved for this site
 * @property password Password that's saved for this site
 * @property timeLastUsed Time of last use in milliseconds from the unix epoch.
 */
@Parcelize
data class SavedLogin(
    val guid: String,
    val origin: String,
    val username: String,
    val password: String,
    val timeLastUsed: Long
) : Parcelable

fun Login.mapToSavedLogin(): SavedLogin =
    SavedLogin(
        guid = this.guid!!,
        origin = this.origin,
        username = this.username,
        password = this.password,
        timeLastUsed = this.timeLastUsed
    )

/**
 * The [Store] for holding the [LoginsListState] and applying [LoginsAction]s.
 */
class LoginsFragmentStore(initialState: LoginsListState) :
    Store<LoginsListState, LoginsAction>(
        initialState,
        ::savedLoginsStateReducer
    )

/**
 * Actions to dispatch through the `LoginsFragmentStore` to modify `LoginsListState` through the reducer.
 */
sealed class LoginsAction : Action {
    data class FilterLogins(val newText: String?) : LoginsAction()
    data class UpdateLoginsList(val list: List<SavedLogin>) : LoginsAction()
    data class UpdateCurrentLogin(val item: SavedLogin) : LoginsAction()
    data class SortLogins(val sortingStrategy: SortingStrategy) : LoginsAction()
}

/**
 * The state for the Saved Logins Screen
 * @property loginList Source of truth for local list of logins
 * @property loginList Filterable list of logins to display
 * @property currentItem The last item that was opened into the detail view
 * @property searchedForText String used by the user to filter logins
 * @property sortingStrategy sorting strategy selected by the user (Currently we support
 * sorting alphabetically and by last used)
 * @property highlightedItem The current selected sorting strategy from the sort menu
 */
data class LoginsListState(
    val isLoading: Boolean = false,
    val loginList: List<SavedLogin>,
    val filteredItems: List<SavedLogin>,
    val currentItem: SavedLogin? = null,
    val searchedForText: String?,
    val sortingStrategy: SortingStrategy,
    val highlightedItem: SavedLoginsSortingStrategyMenu.Item
) : State

/**
 * Handles changes in the saved logins list, including updates and filtering.
 */
private fun savedLoginsStateReducer(
    state: LoginsListState,
    action: LoginsAction
): LoginsListState {
    return when (action) {
        is LoginsAction.UpdateLoginsList -> {
            state.copy(
                isLoading = false,
                loginList = action.list,
                filteredItems = state.sortingStrategy(action.list)
            )
        }
        is LoginsAction.FilterLogins -> {
            filterItems(
                action.newText,
                state.sortingStrategy,
                state
            )
        }
        is LoginsAction.UpdateCurrentLogin -> {
            state.copy(
                currentItem = action.item
            )
        }
        is LoginsAction.SortLogins -> {
            filterItems(
                state.searchedForText,
                action.sortingStrategy,
                state
            )
        }
    }
}

/**
 * @return [LoginsListState] containing a new [LoginsListState.filteredItems]
 * with filtered [LoginsListState.items]
 *
 * @param searchedForText based on which [LoginsListState.items] will be filtered.
 * @param sortingStrategy based on which [LoginsListState.items] will be sorted.
 * @param state previous [LoginsListState] containing all the other properties
 * with which a new state will be created
 */
private fun filterItems(
    searchedForText: String?,
    sortingStrategy: SortingStrategy,
    state: LoginsListState
): LoginsListState {
    return if (searchedForText.isNullOrBlank()) {
        state.copy(
            isLoading = false,
            sortingStrategy = sortingStrategy,
            highlightedItem = sortingStrategyToMenuItem(sortingStrategy),
            searchedForText = searchedForText,
            filteredItems = sortingStrategy(state.loginList)
        )
    } else {
        state.copy(
            isLoading = false,
            sortingStrategy = sortingStrategy,
            highlightedItem = sortingStrategyToMenuItem(sortingStrategy),
            searchedForText = searchedForText,
            filteredItems = sortingStrategy(state.loginList).filter {
                it.origin.contains(
                    searchedForText
                )
            }
        )
    }
}

private fun sortingStrategyToMenuItem(sortingStrategy: SortingStrategy): SavedLoginsSortingStrategyMenu.Item {
    return when (sortingStrategy) {
        is SortingStrategy.Alphabetically -> {
            SavedLoginsSortingStrategyMenu.Item.AlphabeticallySort
        }

        is SortingStrategy.LastUsed -> {
            SavedLoginsSortingStrategyMenu.Item.LastUsedSort
        }
    }
}
