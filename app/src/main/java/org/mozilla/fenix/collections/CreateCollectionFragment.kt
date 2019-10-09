/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.collections

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.fragment_create_collection.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import mozilla.components.lib.state.ext.consumeFrom
import org.mozilla.fenix.FenixViewModelProvider
import org.mozilla.fenix.R
import org.mozilla.fenix.components.StoreProvider
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.home.sessioncontrol.Tab
import org.mozilla.fenix.home.sessioncontrol.toSessionBundle
import org.mozilla.fenix.mvi.ActionBusFactory
import org.mozilla.fenix.mvi.getAutoDisposeObservable
import org.mozilla.fenix.mvi.getManagedEmitter

@ExperimentalCoroutinesApi
class CreateCollectionFragment : DialogFragment() {
    private lateinit var collectionCreationView: CollectionCreationView
    private lateinit var collectionCreationStore: CollectionCreationStore
    private lateinit var collectionViewInteractor: CollectionViewInteractor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
        setStyle(STYLE_NO_TITLE, R.style.CreateCollectionDialogStyle)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_create_collection, container, false)

        collectionCreationStore = StoreProvider.get(this) {
            CollectionCreationStore(
                CollectionCreationState(
                    // TODO initial state
                )
            )
        }
        collectionViewInteractor = CollectionCreationInteractor(
            DefaultCollectionCreationController(
                collectionCreationStore,
                ::dismiss,
                requireComponents.analytics,
                requireComponents.core.tabCollectionStorage,
                requireComponents.useCases.tabsUseCases,
                requireComponents.core.sessionManager,
                viewLifecycleOwner.lifecycleScope
            )
        )

        return view
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnKeyListener { _, keyCode, event ->
            collectionCreationView.onKey(keyCode, event)
        }
        return dialog
    }


    override fun onResume() {
        super.onResume()
        collectionCreationView.onResumed()
    }
}
