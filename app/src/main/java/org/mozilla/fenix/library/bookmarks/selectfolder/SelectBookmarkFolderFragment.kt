/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.bookmarks.selectfolder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.android.synthetic.main.fragment_select_bookmark_folder.*
import kotlinx.android.synthetic.main.fragment_select_bookmark_folder.view.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mozilla.appservices.places.BookmarkRoot
import mozilla.components.concept.storage.BookmarkNode
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.nav
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.ext.showToolbar
import org.mozilla.fenix.library.bookmarks.BookmarksSharedViewModel
import org.mozilla.fenix.library.bookmarks.DesktopFolders
import org.mozilla.fenix.library.bookmarks.SignInView

class SelectBookmarkFolderFragment : Fragment() {

    private val sharedViewModel: BookmarksSharedViewModel by activityViewModels {
        ViewModelProvider.NewInstanceFactory() // this is a workaround for #4652
    }
    private var bookmarkNode: BookmarkNode? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_select_bookmark_folder, container, false)

        val signInView = SignInView(view.selectBookmarkLayout, findNavController())
        sharedViewModel.signedIn.observe(viewLifecycleOwner, signInView)

        return view
    }

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.bookmark_select_folder_fragment_label))

        val accountManager = requireComponents.backgroundServices.accountManager
        sharedViewModel.observeAccountManager(accountManager, owner = this)

        lifecycleScope.launch(Main) {
            bookmarkNode = withContext(IO) {
                val context = requireContext()
                context.components.core.bookmarksStorage
                    .getTree(BookmarkRoot.Root.id, recursive = true)
                    ?.let { DesktopFolders(context, showMobileRoot = true).withOptionalDesktopFolders(it) }
            }
            activity?.title = bookmarkNode?.title ?: getString(R.string.library_bookmarks)
            val adapter = SelectBookmarkFolderAdapter(sharedViewModel)
            recylerViewBookmarkFolders.adapter = adapter
            adapter.updateData(bookmarkNode)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        val args: SelectBookmarkFolderFragmentArgs by navArgs()
        if (!args.visitedAddBookmark) {
            inflater.inflate(R.menu.bookmarks_select_folder, menu)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.add_folder_button -> {
                lifecycleScope.launch(Main) {
                    nav(
                        R.id.bookmarkSelectFolderFragment,
                        SelectBookmarkFolderFragmentDirections
                            .actionBookmarkSelectFolderFragmentToBookmarkAddFolderFragment()
                    )
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
