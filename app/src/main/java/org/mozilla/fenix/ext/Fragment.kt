/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("TooManyFunctions")

package org.mozilla.fenix.ext

import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.size
import androidx.fragment.app.Fragment
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment.findNavController
import androidx.navigation.fragment.findNavController
import mozilla.components.concept.base.crash.Breadcrumb
import org.mozilla.fenix.NavHostActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.components.Components

/**
 * Get the requireComponents of this application.
 */
val Fragment.requireComponents: Components
    get() = requireContext().components

fun Fragment.nav(@IdRes id: Int?, directions: NavDirections, options: NavOptions? = null) {
    findNavController(this).nav(id, directions, options)
}

fun Fragment.getPreferenceKey(@StringRes resourceId: Int): String = getString(resourceId)

/**
 * Displays the activity toolbar with the given [title].
 * Throws if the fragment is not attached to an [AppCompatActivity].
 */
fun Fragment.showToolbar(title: String) {
    (requireActivity() as AppCompatActivity).title = title
    activity?.setNavigationIcon(R.drawable.ic_back_button)
    (activity as NavHostActivity).getSupportActionBarAndInflateIfNecessary().show()
}

/**
 * Run the [block] only if the [Fragment] is attached.
 *
 * @param block A callback to be executed if the container [Fragment] is attached.
 */
internal inline fun Fragment.runIfFragmentIsAttached(block: () -> Unit) {
    context?.let {
        block()
    }
}

/**
 * Hides the activity toolbar.
 * Throws if the fragment is not attached to an [AppCompatActivity].
 */
fun Fragment.hideToolbar() {
    (requireActivity() as AppCompatActivity).supportActionBar?.hide()
}

/**
 * Pops the backstack to force users to re-auth if they put the app in the background and return to
 * it while being inside a secured flow (e.g. logins or credit cards).
 *
 * Does nothing if the user is currently navigating to any of the [destinations] given as a
 * parameter.
 */
fun Fragment.redirectToReAuth(
    destinations: List<Int>,
    currentDestination: Int?,
    currentLocation: Int
) {
    if (currentDestination !in destinations) {
        // Workaround for memory leak caused by Android SDK bug
        // https://issuetracker.google.com/issues/37125819
        activity?.invalidateOptionsMenu()
        when (currentLocation) {
            R.id.loginDetailFragment,
            R.id.editLoginFragment,
            R.id.addLoginFragment,
            R.id.savedLoginsFragment -> {
                findNavController().popBackStack(R.id.savedLoginsAuthFragment, false)
            }
            R.id.creditCardEditorFragment,
            R.id.creditCardsManagementFragment -> {
                findNavController().popBackStack(R.id.creditCardsSettingFragment, false)
            }
        }
    }
}

fun Fragment.breadcrumb(
    message: String,
    data: Map<String, String> = emptyMap()
) {
    val activityName = activity?.let { it::class.java.simpleName } ?: "null"

    requireComponents.analytics.crashReporter.recordCrashBreadcrumb(
        Breadcrumb(
            category = this::class.java.simpleName,
            message = message,
            data = data + mapOf(
                "instance" to hashCode().toString(),
                "activityInstance" to activity?.hashCode().toString(),
                "activityName" to activityName
            ),
            level = Breadcrumb.Level.INFO
        )
    )
}

/**
 * Sets the [WindowManager.LayoutParams.FLAG_SECURE] flag for the current activity window.
 */
fun Fragment.secure() {
    this.activity?.window?.addFlags(
        WindowManager.LayoutParams.FLAG_SECURE
    )
}

/**
 * Clears the [WindowManager.LayoutParams.FLAG_SECURE] flag for the current activity window.
 */
fun Fragment.removeSecure() {
    this.activity?.window?.clearFlags(
        WindowManager.LayoutParams.FLAG_SECURE
    )
}

/**
 * Configure the [SearchView] in a [Menu] handling automatically hiding all other menu options when
 * the [SearchView] is opened and showing them again when the [SearchView] is closed and informing callers
 * of all important events related to the state of the [SearchView].
 *
 * Calling this method for a [Menu] containing no [SearchView] has no effect.
 *
 * @param menu [Menu] containing a [SearchView] and optionally other [MenuItem]s.
 * @param queryHint The hint shown to the user in [SearchView] when no other text is entered.
 * @param onQueryTextChange Callback for when the currently entered text is updated.
 * Contains two arguments: the previous query (starting as empty) and the current query.
 * @param onQueryTextSubmit Callback for when the user presses the "Done" button on the keyboard.
 * @param onSearchStarted Callback for when [SearchView] is opened
 * (and all other [MenuItem]s are hidden from toolbar).
 * @param onSearchEnded Callback for when [SearchView] is closed
 * (and all [MenuItem]s hidden when the [SearchView] was opened are again shown).
 */
@Suppress("LongParameterList")
fun Fragment.configureSearchViewInMenu(
    menu: Menu,
    queryHint: String,
    onQueryTextChange: (String, String) -> Unit = { _, _ -> },
    onQueryTextSubmit: (String) -> Unit = { },
    onSearchStarted: () -> Unit = { },
    onSearchEnded: () -> Unit = { }
) {
    val (searchViewItem: MenuItem?, otherVisibleItems: List<MenuItem>) = menu.splitSearchViewItem()
    if (searchViewItem == null) {
        return
    }

    val searchView = searchViewItem.actionView as SearchView
    searchView.queryHint = queryHint
    searchView.imeOptions = EditorInfo.IME_ACTION_DONE
    searchView.maxWidth = Int.MAX_VALUE

    var previousQuery = ""

    searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(query: String): Boolean {
            onQueryTextSubmit(query)
            return false
        }

        override fun onQueryTextChange(query: String): Boolean {
            onQueryTextChange(previousQuery, query)
            previousQuery = query
            return false
        }
    })

    if (otherVisibleItems.isNotEmpty()) {
        searchViewItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(searchViewMenuItem: MenuItem): Boolean {
                onSearchStarted()
                otherVisibleItems.forEach { it.isVisible = false }
                return true
            }

            override fun onMenuItemActionCollapse(searchViewMenuItem: MenuItem): Boolean {
                onSearchEnded()
                activity?.invalidateOptionsMenu()
                return true
            }
        })
    }
}

/**
 * Helper method for separating a [SearchView] [MenuItem] from all others shown in this [Menu].
 *
 * @return Pair of the [MenuItem] containing a [SearchView] and the list of all other [MenuItem]s.
 */
@VisibleForTesting
internal fun Menu.splitSearchViewItem(): Pair<MenuItem?, List<MenuItem>> {
    var searchViewItem: MenuItem? = null
    val otherItems = mutableListOf<MenuItem>()

    for (i in 0 until size) {
        val item = getItem(i)
        if (item.actionView is SearchView) searchViewItem = item
        else if (item.isVisible) otherItems.add(item)
    }

    return searchViewItem to otherItems
}
