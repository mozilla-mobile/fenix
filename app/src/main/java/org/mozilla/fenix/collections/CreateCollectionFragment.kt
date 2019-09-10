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
import kotlinx.coroutines.launch
import org.mozilla.fenix.FenixViewModelProvider
import org.mozilla.fenix.R
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.home.sessioncontrol.Tab
import org.mozilla.fenix.home.sessioncontrol.toSessionBundle
import org.mozilla.fenix.mvi.ActionBusFactory
import org.mozilla.fenix.mvi.getAutoDisposeObservable
import org.mozilla.fenix.mvi.getManagedEmitter

class CreateCollectionFragment : DialogFragment() {
    private lateinit var collectionCreationComponent: CollectionCreationComponent
    private val viewModel: CreateCollectionViewModel by activityViewModels {
        ViewModelProvider.NewInstanceFactory() // this is a workaround for #4652
    }

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

        collectionCreationComponent = CollectionCreationComponent(
            view.createCollectionWrapper,
            ActionBusFactory.get(this),
            FenixViewModelProvider.create(
                this,
                CollectionCreationViewModel::class.java
            ) {
                CollectionCreationViewModel(viewModel.state)
            }
        )
        return view
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnKeyListener { _, keyCode, event ->
            (collectionCreationComponent.uiView as CollectionCreationUIView).onKey(keyCode, event)
        }
        return dialog
    }

    override fun onResume() {
        super.onResume()
        (collectionCreationComponent.uiView as CollectionCreationUIView).onResumed()
        subscribeToActions()
    }

    @Suppress("ComplexMethod")
    private fun subscribeToActions() {
        getAutoDisposeObservable<CollectionCreationAction>().subscribe {
            when (it) {
                is CollectionCreationAction.Close -> dismiss()
                is CollectionCreationAction.SaveTabsToCollection -> {
                    getManagedEmitter<CollectionCreationChange>()
                        .onNext(
                            CollectionCreationChange.StepChanged(
                                if (viewModel.state.tabCollections.isEmpty()) {
                                    SaveCollectionStep.NameCollection
                                } else {
                                    SaveCollectionStep.SelectCollection
                                }
                            )
                        )
                }
                is CollectionCreationAction.AddTabToSelection -> {
                    getManagedEmitter<CollectionCreationChange>()
                        .onNext(CollectionCreationChange.TabAdded(it.tab))
                }
                is CollectionCreationAction.RemoveTabFromSelection -> {
                    getManagedEmitter<CollectionCreationChange>()
                        .onNext(CollectionCreationChange.TabRemoved(it.tab))
                }
                is CollectionCreationAction.SelectAllTapped -> {
                    getManagedEmitter<CollectionCreationChange>()
                        .onNext(CollectionCreationChange.AddAllTabs)
                }
                is CollectionCreationAction.DeselectAllTapped -> {
                    getManagedEmitter<CollectionCreationChange>()
                        .onNext(CollectionCreationChange.RemoveAllTabs)
                }
                is CollectionCreationAction.AddNewCollection -> getManagedEmitter<CollectionCreationChange>().onNext(
                    CollectionCreationChange.StepChanged(SaveCollectionStep.NameCollection)
                )
                is CollectionCreationAction.BackPressed -> handleBackPress(backPressFrom = it.backPressFrom)
                is CollectionCreationAction.SaveCollectionName -> {
                    dismiss()

                    context?.let { context ->
                        val sessionBundle = it.tabs.toList().toSessionBundle(context)
                        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                            context.components.core.tabCollectionStorage.createCollection(it.name, sessionBundle)
                        }

                        context.components.analytics.metrics.track(
                            Event.CollectionSaved(normalSessionSize(), sessionBundle.size)
                        )

                        closeTabsIfNecessary(it.tabs)
                    }
                }
                is CollectionCreationAction.SelectCollection -> {
                    dismiss()
                    context?.let { context ->
                        val sessionBundle = it.tabs.toList().toSessionBundle(context)
                        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                            context.components.core.tabCollectionStorage
                                .addTabsToCollection(it.collection, sessionBundle)
                        }

                        context.components.analytics.metrics.track(
                            Event.CollectionTabsAdded(normalSessionSize(), sessionBundle.size)
                        )

                        closeTabsIfNecessary(it.tabs)
                    }
                }
                is CollectionCreationAction.RenameCollection -> {
                    dismiss()
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                        context?.components?.core?.tabCollectionStorage?.renameCollection(it.collection, it.name)
                        context?.components?.analytics?.metrics?.track(Event.CollectionRenamed)
                    }
                }
            }
        }
    }

    private fun normalSessionSize(): Int {
        return requireComponents.core.sessionManager.sessions.filter { session ->
            (!session.isCustomTabSession() && !session.private)
        }.size
    }

    private fun handleBackPress(backPressFrom: SaveCollectionStep) {
        val newStep = stepBack(backPressFrom)
        if (newStep != null) {
            getManagedEmitter<CollectionCreationChange>().onNext(CollectionCreationChange.StepChanged(newStep))
        } else {
            dismiss()
        }
    }

    private fun stepBack(backFromStep: SaveCollectionStep): SaveCollectionStep? {
        val state = viewModel.state
        return when (backFromStep) {
            SaveCollectionStep.SelectTabs, SaveCollectionStep.RenameCollection -> null
            SaveCollectionStep.SelectCollection -> if (state.tabs.size <= 1) {
                stepBack(SaveCollectionStep.SelectTabs)
            } else {
                SaveCollectionStep.SelectTabs
            }
            SaveCollectionStep.NameCollection -> if (state.tabCollections.isEmpty()) {
                stepBack(SaveCollectionStep.SelectCollection)
            } else {
                SaveCollectionStep.SelectCollection
            }
        }
    }

    private fun closeTabsIfNecessary(tabs: List<Tab>) {
        // Only close the tabs if the user is not on the BrowserFragment
        if (viewModel.previousFragmentId == R.id.browserFragment) {
            val components = requireComponents
            tabs.asSequence()
                .mapNotNull { tab -> components.core.sessionManager.findSessionById(tab.sessionId) }
                .forEach { session -> components.useCases.tabsUseCases.removeTab(session) }
        }
    }
}
