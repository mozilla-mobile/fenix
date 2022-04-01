/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.bookmarks.edit

import android.content.DialogInterface
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mozilla.appservices.places.uniffi.PlacesException
import mozilla.components.concept.storage.BookmarkInfo
import mozilla.components.concept.storage.BookmarkNode
import mozilla.components.concept.storage.BookmarkNodeType
import mozilla.components.support.ktx.android.content.getColorFromAttr
import mozilla.components.support.ktx.android.view.hideKeyboard
import mozilla.components.support.ktx.android.view.showKeyboard
import mozilla.telemetry.glean.private.NoExtras
import org.mozilla.fenix.GleanMetrics.BookmarksManagement
import org.mozilla.fenix.NavHostActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.components.FenixSnackbar
import org.mozilla.fenix.databinding.FragmentEditBookmarkBinding
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.getRootView
import org.mozilla.fenix.ext.nav
import org.mozilla.fenix.ext.placeCursorAtEnd
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.ext.setToolbarColors
import org.mozilla.fenix.ext.toShortUrl
import org.mozilla.fenix.library.bookmarks.BookmarksSharedViewModel
import org.mozilla.fenix.library.bookmarks.friendlyRootTitle

/**
 * Menu to edit the name, URL, and location of a bookmark item.
 */
class EditBookmarkFragment : Fragment(R.layout.fragment_edit_bookmark) {
    private var _binding: FragmentEditBookmarkBinding? = null
    private val binding get() = _binding!!

    private val args by navArgs<EditBookmarkFragmentArgs>()
    private val sharedViewModel: BookmarksSharedViewModel by activityViewModels()
    private var bookmarkNode: BookmarkNode? = null
    private var bookmarkParent: BookmarkNode? = null
    private var initialParentGuid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentEditBookmarkBinding.bind(view)

        initToolbar()

        viewLifecycleOwner.lifecycleScope.launch(Main) {
            val context = requireContext()
            val bookmarkNodeBeforeReload = bookmarkNode
            val bookmarksStorage = context.components.core.bookmarksStorage

            bookmarkNode = withContext(IO) {
                bookmarksStorage.getBookmark(args.guidToEdit)
            }

            if (initialParentGuid == null) {
                initialParentGuid = bookmarkNode?.parentGuid
            }

            bookmarkParent = withContext(IO) {
                // Use user-selected parent folder if it's set, or node's current parent otherwise.
                if (sharedViewModel.selectedFolder != null) {
                    sharedViewModel.selectedFolder
                } else {
                    bookmarkNode?.parentGuid?.let { bookmarksStorage.getBookmark(it) }
                }
            }

            when (bookmarkNode?.type) {
                BookmarkNodeType.FOLDER -> {
                    activity?.title = getString(R.string.edit_bookmark_folder_fragment_title)
                    binding.inputLayoutBookmarkUrl.visibility = View.GONE
                    binding.bookmarkUrlEdit.visibility = View.GONE
                    binding.bookmarkUrlLabel.visibility = View.GONE
                }
                BookmarkNodeType.ITEM -> {
                    activity?.title = getString(R.string.edit_bookmark_fragment_title)
                }
                else -> throw IllegalArgumentException()
            }

            val currentBookmarkNode = bookmarkNode
            if (currentBookmarkNode != null && currentBookmarkNode != bookmarkNodeBeforeReload) {
                binding.bookmarkNameEdit.setText(currentBookmarkNode.title)
                binding.bookmarkUrlEdit.setText(currentBookmarkNode.url)
            }

            bookmarkParent?.let { node ->
                binding.bookmarkParentFolderSelector.text = friendlyRootTitle(context, node)
            }

            binding.bookmarkParentFolderSelector.setOnClickListener {
                sharedViewModel.selectedFolder = null
                nav(
                    R.id.bookmarkEditFragment,
                    EditBookmarkFragmentDirections
                        .actionBookmarkEditFragmentToBookmarkSelectFolderFragment(
                            allowCreatingNewFolder = false,
                            // Don't allow moving folders into themselves.
                            hideFolderGuid = when (bookmarkNode!!.type) {
                                BookmarkNodeType.FOLDER -> bookmarkNode!!.guid
                                else -> null
                            }
                        )
                )
            }

            binding.bookmarkNameEdit.apply {
                requestFocus()
                placeCursorAtEnd()
                showKeyboard()
            }

            binding.bookmarkUrlEdit.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    // NOOP
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    binding.bookmarkUrlEdit.onTextChanged(s)

                    binding.inputLayoutBookmarkUrl.error = null
                    binding.inputLayoutBookmarkUrl.errorIconDrawable = null
                }

                override fun afterTextChanged(s: Editable?) {
                    // NOOP
                }
            })
        }
    }

    private fun initToolbar() {
        val activity = activity as AppCompatActivity
        val actionBar = (activity as NavHostActivity).getSupportActionBarAndInflateIfNecessary()
        val toolbar = activity.findViewById<Toolbar>(R.id.navigationToolbar)
        toolbar?.setToolbarColors(
            foreground = activity.getColorFromAttr(R.attr.textPrimary),
            background = activity.getColorFromAttr(R.attr.layer1)
        )
        actionBar.show()
    }

    override fun onPause() {
        super.onPause()
        binding.bookmarkNameEdit.hideKeyboard()
        binding.bookmarkUrlEdit.hideKeyboard()
        binding.progressBarBookmark.visibility = View.GONE
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
            R.id.save_bookmark_button -> {
                updateBookmarkFromTextChanges()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun displayDeleteBookmarkDialog() {
        activity?.let { activity ->
            AlertDialog.Builder(activity).apply {
                setMessage(R.string.bookmark_deletion_confirmation)
                setNegativeButton(R.string.bookmark_delete_negative) { dialog: DialogInterface, _ ->
                    dialog.cancel()
                }
                setPositiveButton(R.string.tab_collection_dialog_positive) { dialog: DialogInterface, _ ->
                    // Use fragment's lifecycle; the view may be gone by the time dialog is interacted with.
                    lifecycleScope.launch(IO) {
                        requireComponents.core.bookmarksStorage.deleteNode(args.guidToEdit)
                        BookmarksManagement.removed.record(NoExtras())

                        launch(Main) {
                            Navigation.findNavController(requireActivity(), R.id.container)
                                .popBackStack()

                            bookmarkNode?.let { bookmark ->
                                FenixSnackbar.make(
                                    view = activity.getRootView()!!,
                                    isDisplayedWithBrowserToolbar = args.requiresSnackbarPaddingForToolbar
                                )
                                    .setText(
                                        getString(
                                            R.string.bookmark_deletion_snackbar_message,
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

    private fun updateBookmarkFromTextChanges() {
        binding.progressBarBookmark.visibility = View.VISIBLE
        val nameText = binding.bookmarkNameEdit.text.toString()
        val urlText = binding.bookmarkUrlEdit.text.toString()
        updateBookmarkNode(nameText, urlText)
    }

    private fun updateBookmarkNode(title: String?, url: String?) {
        viewLifecycleOwner.lifecycleScope.launch(IO) {
            try {
                requireComponents.let { components ->
                    if (title != bookmarkNode?.title || url != bookmarkNode?.url) {
                        BookmarksManagement.edited.record(NoExtras())
                    }
                    val parentGuid = sharedViewModel.selectedFolder?.guid ?: bookmarkNode!!.parentGuid
                    val parentChanged = initialParentGuid != parentGuid
                    // Only track the 'moved' event if new parent was selected.
                    if (parentChanged) {
                        BookmarksManagement.moved.record(NoExtras())
                    }
                    components.core.bookmarksStorage.updateNode(
                        args.guidToEdit,
                        BookmarkInfo(
                            parentGuid,
                            // Setting position to 'null' is treated as a 'move to the end' by the storage API.
                            if (parentChanged) null else bookmarkNode?.position,
                            title,
                            if (bookmarkNode?.type == BookmarkNodeType.ITEM) url else null
                        )
                    )
                }
                withContext(Main) {
                    binding.inputLayoutBookmarkUrl.error = null
                    binding.inputLayoutBookmarkUrl.errorIconDrawable = null

                    findNavController().popBackStack()
                }
            } catch (e: PlacesException.UrlParseFailed) {
                withContext(Main) {
                    binding.inputLayoutBookmarkUrl.error = getString(R.string.bookmark_invalid_url_error)
                    binding.inputLayoutBookmarkUrl.setErrorIconDrawable(R.drawable.mozac_ic_warning_with_bottom_padding)
                    binding.inputLayoutBookmarkUrl.setErrorIconTintList(
                        ColorStateList.valueOf(
                            ContextCompat.getColor(requireContext(), R.color.fx_mobile_text_color_warning)
                        )
                    )
                }
            }
        }
        binding.progressBarBookmark.visibility = View.INVISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }
}
