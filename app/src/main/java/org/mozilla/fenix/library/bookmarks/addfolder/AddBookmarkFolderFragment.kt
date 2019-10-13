/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.bookmarks.addfolder

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import kotlinx.android.synthetic.main.fragment_add_bookmark_folder.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import mozilla.appservices.places.BookmarkRoot
import org.mozilla.fenix.R
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.ext.nav
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.library.bookmarks.BookmarksSharedViewModel

/**
 * Menu to create a new bookmark folder.
 */
class AddBookmarkFolderFragment : Fragment(R.layout.fragment_add_bookmark_folder) {

    private val sharedViewModel: BookmarksSharedViewModel by activityViewModels {
        ViewModelProvider.NewInstanceFactory() // this is a workaround for #4652
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity).title =
            getString(R.string.bookmark_add_folder_fragment_label)
        (activity as AppCompatActivity).supportActionBar?.show()

        lifecycleScope.launch(IO) {
            sharedViewModel.selectedFolder = sharedViewModel.selectedFolder
                ?: requireComponents.core.bookmarksStorage.getTree(BookmarkRoot.Mobile.id)
            launch(Main) {
                bookmarkAddFolderParentSelector.text = sharedViewModel.selectedFolder!!.title
                bookmarkAddFolderParentSelector.setOnClickListener {
                    nav(
                        R.id.bookmarkAddFolderFragment,
                        AddBookmarkFolderFragmentDirections
                            .actionBookmarkAddFolderFragmentToBookmarkSelectFolderFragment(
                                BookmarkRoot.Root.id,
                                true
                            )
                    )
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.bookmarks_add_folder, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.confirm_add_folder_button -> {
                if (bookmarkAddFolderTitleEdit.text.isNullOrBlank()) {
                    bookmarkAddFolderTitleEdit.error =
                        getString(R.string.bookmark_empty_title_error)
                    return true
                }
                lifecycleScope.launch(IO) {
                    val newGuid = requireComponents.core.bookmarksStorage.addFolder(
                        sharedViewModel.selectedFolder!!.guid, bookmarkAddFolderTitleEdit.text.toString(), null
                    )
                    sharedViewModel.selectedFolder = requireComponents.core.bookmarksStorage.getTree(newGuid)
                    requireComponents.analytics.metrics.track(Event.AddBookmarkFolder)
                    launch(Main) {
                        Navigation.findNavController(requireActivity(), R.id.container).popBackStack()
                    }
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
