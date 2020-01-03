/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.bookmarks.edit

import android.content.DialogInterface
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import kotlinx.android.synthetic.main.fragment_edit_bookmark.bookmarkNameEdit
import kotlinx.android.synthetic.main.fragment_edit_bookmark.bookmarkParentFolderSelector
import kotlinx.android.synthetic.main.fragment_edit_bookmark.bookmarkUrlEdit
import kotlinx.android.synthetic.main.fragment_edit_bookmark.bookmarkUrlLabel
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import mozilla.appservices.places.UrlParseFailed
import mozilla.components.concept.storage.BookmarkInfo
import mozilla.components.concept.storage.BookmarkNode
import mozilla.components.concept.storage.BookmarkNodeType
import mozilla.components.support.ktx.android.content.getColorFromAttr
import mozilla.components.support.ktx.android.view.hideKeyboard
import mozilla.components.support.ktx.android.view.toScope
import org.mozilla.fenix.R
import org.mozilla.fenix.components.FenixSnackbar
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.getRootView
import org.mozilla.fenix.ext.nav
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.ext.setToolbarColors
import org.mozilla.fenix.ext.toShortUrl
import org.mozilla.fenix.library.bookmarks.BookmarksSharedViewModel
import org.mozilla.fenix.library.bookmarks.DesktopFolders

/**
 * Menu to edit the name, URL, and location of a bookmark item.
 */
@FlowPreview
@ExperimentalCoroutinesApi
@InternalCoroutinesApi // Cannot use collect as a lambda due to Kotlin SAM conversion issue
class EditBookmarkFragment : Fragment(R.layout.fragment_edit_bookmark) {

    private lateinit var guidToEdit: String
    private val sharedViewModel: BookmarksSharedViewModel by activityViewModels {
        ViewModelProvider.NewInstanceFactory() // this is a workaround for #4652
    }
    private var bookmarkNode: BookmarkNode? = null
    private var bookmarkParent: BookmarkNode? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onResume() {
        super.onResume()

        initToolbar()

        guidToEdit = EditBookmarkFragmentArgs.fromBundle(arguments!!).guidToEdit
        lifecycleScope.launch(Main) {
            val context = requireContext()

            withContext(IO) {
                val bookmarksStorage = context.components.core.bookmarksStorage
                bookmarkNode = bookmarksStorage.getTree(guidToEdit)
                bookmarkParent = sharedViewModel.selectedFolder
                    ?: bookmarkNode?.parentGuid
                        ?.let { bookmarksStorage.getTree(it) }
                        ?.let { DesktopFolders(context, showMobileRoot = true).withRootTitle(it) }
            }

            when (bookmarkNode?.type) {
                BookmarkNodeType.FOLDER -> {
                    activity?.title = getString(R.string.edit_bookmark_folder_fragment_title)
                    bookmarkUrlEdit.visibility = View.GONE
                    bookmarkUrlLabel.visibility = View.GONE
                }
                BookmarkNodeType.ITEM -> {
                    activity?.title = getString(R.string.edit_bookmark_fragment_title)
                }
                else -> throw IllegalArgumentException()
            }

            bookmarkNode?.let { bookmarkNode ->
                bookmarkNameEdit.setText(bookmarkNode.title)
                bookmarkUrlEdit.setText(bookmarkNode.url)

                if (sharedViewModel.selectedFolder != null && bookmarkNode.title != null) {
                    updateBookmarkNode(bookmarkNode.title, bookmarkNode.url)
                }
            }

            bookmarkParent?.let { node ->
                    bookmarkParentFolderSelector.text = node.title
                    bookmarkParentFolderSelector.setOnClickListener {
                        sharedViewModel.selectedFolder = null
                        nav(
                            R.id.bookmarkEditFragment,
                            EditBookmarkFragmentDirections
                                .actionBookmarkEditFragmentToBookmarkSelectFolderFragment(null)
                        )
                    }
                }
            }

        updateBookmarkFromTextChanges()
    }

    private fun initToolbar() {
        val activity = activity as? AppCompatActivity
        val toolbar = activity?.findViewById<Toolbar>(R.id.navigationToolbar)
        context?.let {
            toolbar?.setToolbarColors(
                foreground = it.getColorFromAttr(R.attr.primaryText),
                background = it.getColorFromAttr(R.attr.foundation)
            )
        }
        activity?.supportActionBar?.show()
    }

    override fun onPause() {
        super.onPause()
        bookmarkNameEdit.hideKeyboard()
        bookmarkUrlEdit.hideKeyboard()
    }

    private fun updateBookmarkFromTextChanges() {
        fun EditText.observe() = channelFlow {
            this@observe.doOnTextChanged { text, _, _, _ ->
                runBlocking { send(text.toString()) }
            }
            awaitClose()
        }

        val nameText = bookmarkNameEdit.observe()
        val urlText = bookmarkUrlEdit.observe()

        bookmarkNameEdit.toScope().launch {
            nameText.combine(urlText) { name, url -> name to url }
                .drop(1)
                .filter { (name) -> name.isNotBlank() }
                .debounce(timeoutMillis = debouncePeriodInMs)
                // TODO convert collect to lambda when Kotlin SAM conversions are supported
                .collect(object : FlowCollector<Pair<String, String>> {
                    override suspend fun emit(value: Pair<String, String>) {
                        updateBookmarkNode(value.first, value.second)
                    }
                })
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.bookmarks_edit, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.delete_bookmark_button -> {
                displayDeleteBookmarkDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun displayDeleteBookmarkDialog() {
        activity?.let { activity ->
            AlertDialog.Builder(activity).apply {
                setMessage(R.string.bookmark_deletion_confirmation)
                setNegativeButton(android.R.string.cancel) { dialog: DialogInterface, _ ->
                    dialog.cancel()
                }
                setPositiveButton(R.string.tab_collection_dialog_positive) { dialog: DialogInterface, _ ->
                    lifecycleScope.launch(IO) {
                        requireComponents.core.bookmarksStorage.deleteNode(guidToEdit)
                        requireComponents.analytics.metrics.track(Event.RemoveBookmark)

                        launch(Main) {
                            Navigation.findNavController(requireActivity(), R.id.container)
                                .popBackStack()

                            bookmarkNode?.let { bookmark ->
                                FenixSnackbar.makeWithToolbarPadding(activity.getRootView()!!)
                                    .setText(
                                        getString(R.string.bookmark_deletion_snackbar_message,
                                            bookmark.url?.toShortUrl(context.components.publicSuffixList)
                                                ?: bookmark.title
                                        )
                                    )
                                    .show()
                            }
                        }
                    }
                    dialog.dismiss()
                }
                create()
            }.show()
        }
    }

    private fun updateBookmarkNode(title: String?, url: String?) {
        lifecycleScope.launch(IO) {
            try {
                requireComponents.let { components ->
                    if (title != bookmarkNode?.title || url != bookmarkNode?.url) {
                        components.analytics.metrics.track(Event.EditedBookmark)
                    }
                    if (sharedViewModel.selectedFolder != null) {
                        components.analytics.metrics.track(Event.MovedBookmark)
                    }
                    components.core.bookmarksStorage.updateNode(
                        guidToEdit,
                        BookmarkInfo(
                            sharedViewModel.selectedFolder?.guid ?: bookmarkNode!!.parentGuid,
                            bookmarkNode?.position,
                            title,
                            if (bookmarkNode?.type == BookmarkNodeType.ITEM) url else null
                        )
                    )
                }
            } catch (e: UrlParseFailed) {
                withContext(Main) {
                    bookmarkUrlEdit.error = getString(R.string.bookmark_invalid_url_error)
                }
            }
        }
    }

    companion object {
        private const val debouncePeriodInMs = 500L
    }
}
