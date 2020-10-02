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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import kotlinx.android.synthetic.main.fragment_create_collection.view.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.components.lib.state.ext.consumeFrom
import org.mozilla.fenix.R
import org.mozilla.fenix.components.StoreProvider
import org.mozilla.fenix.ext.requireComponents

@ExperimentalCoroutinesApi
class CollectionCreationFragment : DialogFragment() {
    private lateinit var collectionCreationView: CollectionCreationView
    private lateinit var collectionCreationStore: CollectionCreationStore
    private lateinit var collectionCreationInteractor: CollectionCreationInteractor

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
        val args: CollectionCreationFragmentArgs by navArgs()

        collectionCreationStore = StoreProvider.get(this) {
            CollectionCreationStore(
                createInitialCollectionCreationState(
                    browserState = requireComponents.core.store.state,
                    tabCollectionStorage = requireComponents.core.tabCollectionStorage,
                    publicSuffixList = requireComponents.publicSuffixList,
                    saveCollectionStep = args.saveCollectionStep,
                    tabIds = args.tabIds,
                    selectedTabIds = args.selectedTabIds,
                    selectedTabCollectionId = args.selectedTabCollectionId
                )
            )
        }
        collectionCreationInteractor = DefaultCollectionCreationInteractor(
            DefaultCollectionCreationController(
                collectionCreationStore,
                ::dismiss,
                requireComponents.analytics.metrics,
                requireComponents.core.tabCollectionStorage,
                requireComponents.core.sessionManager,
                scope = lifecycleScope
            )
        )
        collectionCreationView = CollectionCreationView(
            view.createCollectionWrapper,
            collectionCreationInteractor
        )

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        consumeFrom(collectionCreationStore) { newState ->
            collectionCreationView.update(newState)
        }
    }

    override fun onResume() {
        super.onResume()
        collectionCreationView.onResumed()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnKeyListener { _, keyCode, event ->
            collectionCreationView.onKey(keyCode, event)
        }
        return dialog
    }
}
