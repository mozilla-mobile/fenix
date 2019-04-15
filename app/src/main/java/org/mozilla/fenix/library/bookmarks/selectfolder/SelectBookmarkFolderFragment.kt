/* This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.bookmarks.selectfolder

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
import kotlinx.android.synthetic.main.fragment_select_bookmark_folder.*
import kotlinx.android.synthetic.main.fragment_select_bookmark_folder.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import mozilla.appservices.places.BookmarkRoot
import mozilla.components.concept.storage.BookmarkNode
import mozilla.components.concept.sync.AccountObserver
import mozilla.components.concept.sync.OAuthAccount
import mozilla.components.concept.sync.Profile
import org.mozilla.fenix.BrowserDirection
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.getColorFromAttr
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.library.bookmarks.BookmarksSharedViewModel
import org.mozilla.fenix.library.bookmarks.SignInAction
import org.mozilla.fenix.library.bookmarks.SignInChange
import org.mozilla.fenix.library.bookmarks.SignInComponent
import org.mozilla.fenix.mvi.ActionBusFactory
import org.mozilla.fenix.mvi.getAutoDisposeObservable
import org.mozilla.fenix.mvi.getManagedEmitter
import kotlin.coroutines.CoroutineContext

@SuppressWarnings("TooManyFunctions")
class SelectBookmarkFolderFragment : Fragment(), CoroutineScope, AccountObserver {

    private lateinit var sharedViewModel: BookmarksSharedViewModel
    private lateinit var job: Job
    private var folderGuid: String? = null
    private var bookmarkNode: BookmarkNode? = null

    private lateinit var signInComponent: SignInComponent

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
        val view = inflater.inflate(R.layout.fragment_select_bookmark_folder, container, false)
        signInComponent = SignInComponent(view.select_bookmark_layout, ActionBusFactory.get(this))
        return view
    }

    override fun onStart() {
        super.onStart()
        getAutoDisposeObservable<SignInAction>()
            .subscribe {
                when (it) {
                    is SignInAction.ClickedSignIn -> {
                        requireComponents.services.accountsAuthFeature.beginAuthentication()
                        view?.let {
                            (activity as HomeActivity).openToBrowser(null, BrowserDirection.FromBookmarksFolderSelect)
                        }
                    }
                }
            }
    }

    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity).title =
            getString(R.string.bookmark_select_folder_fragment_label)
        (activity as AppCompatActivity).supportActionBar?.show()

        folderGuid = SelectBookmarkFolderFragmentArgs.fromBundle(arguments!!).folderGuid ?: BookmarkRoot.Root.id
        checkIfSignedIn()

        launch(IO) {
            bookmarkNode = requireComponents.core.bookmarksStorage.getTree(folderGuid!!, true)
            launch(Main) {
                (activity as HomeActivity).title = bookmarkNode?.title ?: getString(R.string.library_bookmarks)
                val adapter = SelectBookmarkFolderAdapter(sharedViewModel)
                recylerView_bookmark_folders.adapter = adapter
                adapter.updateData(bookmarkNode)
            }
        }
    }

    private fun checkIfSignedIn() {
        val accountManager = requireComponents.backgroundServices.accountManager
        accountManager.register(this, owner = this)
        accountManager.authenticatedAccount()?.let { getManagedEmitter<SignInChange>().onNext(SignInChange.SignedIn) }
            ?: getManagedEmitter<SignInChange>().onNext(SignInChange.SignedOut)
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        val visitedAddBookmark = SelectBookmarkFolderFragmentArgs.fromBundle(arguments!!).visitedAddBookmark
        if (!visitedAddBookmark) {
            inflater.inflate(R.menu.bookmarks_select_folder, menu)
            menu.findItem(R.id.add_folder_button).icon.colorFilter =
                PorterDuffColorFilter(R.attr.primaryText.getColorFromAttr(context!!), PorterDuff.Mode.SRC_IN)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.add_folder_button -> {
                launch(Main) {
                    Navigation.findNavController(requireActivity(), R.id.container).navigate(
                        SelectBookmarkFolderFragmentDirections
                            .actionBookmarkSelectFolderFragmentToBookmarkAddFolderFragment()
                    )
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onAuthenticated(account: OAuthAccount) {
        getManagedEmitter<SignInChange>().onNext(SignInChange.SignedIn)
    }

    override fun onError(error: Exception) {
    }

    override fun onLoggedOut() {
        getManagedEmitter<SignInChange>().onNext(SignInChange.SignedOut)
    }

    override fun onProfileUpdated(profile: Profile) {
    }
}
