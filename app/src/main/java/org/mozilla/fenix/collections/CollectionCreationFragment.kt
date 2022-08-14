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
import androidx.navigation.NavType
import androidx.navigation.fragment.navArgs
import mozilla.components.lib.state.ext.consumeFrom
import org.mozilla.fenix.R
import org.mozilla.fenix.components.StoreProvider
import org.mozilla.fenix.databinding.FragmentCreateCollectionBinding
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.navigation.NavRouteInfo
import org.mozilla.fenix.navigation.ScreenArgsInfo

class CollectionCreationFragment : DialogFragment() {
    private lateinit var collectionCreationView: CollectionCreationView
    private lateinit var collectionCreationStore: CollectionCreationStore
    private lateinit var collectionCreationInteractor: CollectionCreationInteractor

    private var _binding: FragmentCreateCollectionBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
        setStyle(STYLE_NO_TITLE, R.style.CreateCollectionDialogStyle)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateCollectionBinding.inflate(inflater, container, false)
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
                requireComponents.core.store,
                ::dismiss,
                requireComponents.core.tabCollectionStorage,
                scope = lifecycleScope
            )
        )
        collectionCreationView = CollectionCreationView(
            binding.createCollectionWrapper,
            collectionCreationInteractor
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        consumeFrom(collectionCreationStore) { newState ->
            collectionCreationView.update(newState)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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

    companion object {
        const val ARG_TAB_IDS = "tabIds"
        const val ARG_SELECTED_TAB_IDS = "selectedTabIds"
        const val ARG_SELECTED_TAB_COLLECTION_ID = "selectedTabCollectionId"
        const val ARG_SAVE_COLLECTION_STEP = "saveCollectionStep"

        val NAV_ROUTE_INFO = NavRouteInfo(
            baseRoute = "collection_creation",
            screenArgs = listOf(
                ScreenArgsInfo(ARG_TAB_IDS, NavType.StringArrayType),
                ScreenArgsInfo(ARG_SELECTED_TAB_IDS, NavType.StringArrayType),
                ScreenArgsInfo(ARG_SELECTED_TAB_COLLECTION_ID, NavType.LongType, -1),
                ScreenArgsInfo(ARG_SAVE_COLLECTION_STEP, NavType.EnumType(type = SaveCollectionStep::class.java), SaveCollectionStep.NameCollection)
            )
        )
    }
}
