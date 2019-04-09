/* This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.bookmarks.addfolder

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.Navigation
import kotlinx.android.synthetic.main.fragment_add_bookmark_folder.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import mozilla.appservices.places.BookmarkRoot
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.getColorFromAttr
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.library.bookmarks.BookmarksSharedViewModel
import kotlin.coroutines.CoroutineContext

class AddBookmarkFolderFragment : Fragment(), CoroutineScope {

    private lateinit var sharedViewModel: BookmarksSharedViewModel
    private lateinit var job: Job

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        job = Job()
        setHasOptionsMenu(true)
        sharedViewModel = activity?.run {
            ViewModelProviders.of(this).get(BookmarksSharedViewModel::class.java)
        }!!
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_add_bookmark_folder, container, false)
    }

    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity).supportActionBar?.show()

        launch(IO) {
            sharedViewModel.selectedFolder = sharedViewModel.selectedFolder
                ?: requireComponents.core.bookmarksStorage.getTree(BookmarkRoot.Mobile.id)
            launch(Main) {
                bookmark_add_folder_parent_selector.text = sharedViewModel.selectedFolder!!.title
                bookmark_add_folder_parent_selector.setOnClickListener {
                    Navigation.findNavController(requireActivity(), R.id.container)
                        .navigate(
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

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.bookmarks_add_folder, menu)
        menu.findItem(R.id.confirm_add_folder_button).icon.colorFilter =
            PorterDuffColorFilter(R.attr.iconColor.getColorFromAttr(context!!), PorterDuff.Mode.SRC_IN)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.confirm_add_folder_button -> {
                if (bookmark_add_folder_title_edit.text.isEmpty()) {
                    bookmark_add_folder_title_edit.error = getString(R.string.bookmark_empty_title_error)
                    return true
                }
                launch(IO) {
                    requireComponents.core.bookmarksStorage.addFolder(
                        sharedViewModel.selectedFolder!!.guid, bookmark_add_folder_title_edit.text.toString(), null
                    )
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
