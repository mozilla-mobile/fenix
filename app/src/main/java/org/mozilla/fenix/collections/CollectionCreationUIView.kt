/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.collections

import android.os.Handler
import android.text.InputFilter
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.AutoTransition
import androidx.transition.Transition
import androidx.transition.TransitionManager
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.functions.Consumer
import kotlinx.android.synthetic.main.component_collection_creation.*
import kotlinx.android.synthetic.main.component_collection_creation.view.*
import mozilla.components.support.ktx.android.view.hideKeyboard
import mozilla.components.support.ktx.android.view.showKeyboard
import org.mozilla.fenix.R
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.increaseTapArea
import org.mozilla.fenix.ext.urlToTrimmedHost
import org.mozilla.fenix.home.sessioncontrol.Tab
import org.mozilla.fenix.home.sessioncontrol.TabCollection
import org.mozilla.fenix.mvi.UIView

class CollectionCreationUIView(
    container: ViewGroup,
    actionEmitter: Observer<CollectionCreationAction>,
    changesObservable: Observable<CollectionCreationChange>
) : UIView<CollectionCreationState, CollectionCreationAction, CollectionCreationChange>(
    container,
    actionEmitter,
    changesObservable
) {
    override val view: View = LayoutInflater.from(container.context)
        .inflate(R.layout.component_collection_creation, container, true)

    var step: SaveCollectionStep = SaveCollectionStep.SelectTabs
        private set

    private val collectionCreationTabListAdapter = CollectionCreationTabListAdapter(actionEmitter)
    private val collectionSaveListAdapter = SaveCollectionListAdapter(actionEmitter)
    private var selectedCollection: TabCollection? = null
    private var selectedTabs: Set<Tab> = setOf()
    private val selectTabsConstraints = ConstraintSet()
    private val selectCollectionConstraints = ConstraintSet()
    private val nameCollectionConstraints = ConstraintSet()
    private val transition = AutoTransition()

    init {
        transition.duration = TRANSITION_DURATION

        selectTabsConstraints.clone(collection_constraint_layout)
        selectCollectionConstraints.clone(view.context, R.layout.component_collection_creation_select_collection)
        nameCollectionConstraints.clone(view.context, R.layout.component_collection_creation_name_collection)

        view.bottom_bar_icon_button.apply {
            increaseTapArea(increaseButtonByDps)
        }

        view.name_collection_edittext.filters += InputFilter.LengthFilter(COLLECTION_NAME_MAX_LENGTH)
        view.name_collection_edittext.setOnEditorActionListener { view, actionId, _ ->
            val text = view.text.toString()
            if (actionId == EditorInfo.IME_ACTION_DONE && text.isNotBlank()) {
                when (step) {
                    SaveCollectionStep.NameCollection -> {
                        CollectionCreationAction.SaveCollectionName(selectedTabs.toList(), text)
                    }
                    SaveCollectionStep.RenameCollection -> {
                        selectedCollection?.let {
                            CollectionCreationAction.RenameCollection(it, text)
                        }
                    }
                    else -> null
                }?.let { action ->
                    actionEmitter.onNext(action)
                }
            }
            false
        }

        view.tab_list.run {
            adapter = collectionCreationTabListAdapter
            itemAnimator = null
            layoutManager = LinearLayoutManager(container.context, RecyclerView.VERTICAL, true)
        }

        view.collections_list.run {
            adapter = collectionSaveListAdapter
            layoutManager = LinearLayoutManager(container.context, RecyclerView.VERTICAL, true)
        }
    }

    @Suppress("ComplexMethod")
    override fun updateView() = Consumer<CollectionCreationState> {
        step = it.saveCollectionStep
        selectedTabs = it.selectedTabs
        selectedCollection = it.selectedTabCollection

        view.tab_list.isClickable = step == SaveCollectionStep.SelectTabs

        when (it.saveCollectionStep) {
            SaveCollectionStep.SelectTabs -> {
                view.context.components.analytics.metrics.track(Event.CollectionTabSelectOpened)

                val allSelected = it.selectedTabs.size == it.tabs.size
                select_all_button.text =
                    if (allSelected)
                        view.context.getString(R.string.create_collection_deselect_all) else
                        view.context.getString(R.string.create_collection_select_all)

                view.select_all_button.setOnClickListener {
                    if (allSelected) {
                        actionEmitter.onNext(CollectionCreationAction.DeselectAllTapped)
                    } else {
                        actionEmitter.onNext(CollectionCreationAction.SelectAllTapped)
                    }
                }

                view.bottom_button_bar_layout.isClickable = false
                view.bottom_button_bar_layout.setOnClickListener(null)

                view.bottom_bar_icon_button.setOnClickListener {
                    actionEmitter.onNext(CollectionCreationAction.Close)
                }

                selectItems(
                    bottomBarIcon = R.drawable.ic_close,
                    constraintSet = selectTabsConstraints
                )

                collectionCreationTabListAdapter.updateData(it.tabs, it.selectedTabs)

                back_button.text = view.context.getString(R.string.create_collection_select_tabs)

                val selectTabsText = if (it.selectedTabs.isEmpty()) {
                    view.context.getString(R.string.create_collection_save_to_collection_empty)
                } else {
                    view.context.getString(
                        if (it.selectedTabs.size == 1)
                            R.string.create_collection_save_to_collection_tab_selected else
                            R.string.create_collection_save_to_collection_tabs_selected,
                        it.selectedTabs.size
                    )
                }

                view.bottom_bar_text.text = selectTabsText

                save_button.setOnClickListener { _ ->
                    if (selectedCollection != null) {
                        actionEmitter.onNext(
                            CollectionCreationAction.SelectCollection(
                                selectedCollection!!,
                                it.selectedTabs.toList()
                            )
                        )
                    } else {
                        actionEmitter.onNext(CollectionCreationAction.SaveTabsToCollection(selectedTabs.toList()))
                    }
                }

                save_button.isGone = it.selectedTabs.isEmpty()
            }
            SaveCollectionStep.SelectCollection -> {
                save_button.visibility = View.GONE

                view.bottom_bar_text.text =
                    view.context.getString(R.string.create_collection_add_new_collection)

                view.bottom_bar_icon_button.setOnClickListener {
                    actionEmitter.onNext(CollectionCreationAction.AddNewCollection)
                }

                view.bottom_button_bar_layout.isClickable = true
                view.bottom_button_bar_layout.setOnClickListener {
                    actionEmitter.onNext(CollectionCreationAction.AddNewCollection)
                }

                selectItems(
                    bottomBarIcon = R.drawable.ic_new,
                    constraintSet = selectCollectionConstraints
                )

                back_button.text =
                    view.context.getString(R.string.create_collection_select_collection)
            }
            SaveCollectionStep.NameCollection -> {
                collectionCreationTabListAdapter.updateData(it.selectedTabs.toList(), it.selectedTabs, true)
                editCollectionName(
                    view.context.getString(
                        R.string.create_collection_default_name,
                        it.tabCollections.size + 1
                    )
                )
                back_button.text = view.context.getString(R.string.create_collection_name_collection)
            }
            SaveCollectionStep.RenameCollection -> {
                it.selectedTabCollection?.let { tabCollection ->
                    tabCollection.tabs.map { tab ->
                        Tab(
                            tab.id.toString(),
                            tab.url,
                            tab.url.urlToTrimmedHost(view.context),
                            tab.title
                        )
                    }.let { tabs ->
                        collectionCreationTabListAdapter.updateData(tabs, tabs.toSet(), true)
                    }
                }

                editCollectionName(it.selectedTabCollection?.title)
                back_button.text = view.context.getString(R.string.create_collection_name_collection)
            }
        }
        collectionSaveListAdapter.updateData(it.tabCollections, it.selectedTabs)
    }

    fun onResumed() {
        if (step == SaveCollectionStep.NameCollection || step == SaveCollectionStep.RenameCollection) {
            view.name_collection_edittext.showKeyboard()
        }
    }

    fun onKey(keyCode: Int, event: KeyEvent): Boolean {
        return if (event.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
            actionEmitter.onNext(CollectionCreationAction.BackPressed(step))
            true
        } else {
            false
        }
    }

    /**
     * Shared logic for [SaveCollectionStep.SelectTabs] and [SaveCollectionStep.SelectCollection].
     */
    private fun selectItems(
        @DrawableRes bottomBarIcon: Int,
        constraintSet: ConstraintSet
    ) {
        back_button.setOnClickListener {
            actionEmitter.onNext(CollectionCreationAction.BackPressed(step))
        }

        val drawable = view.context.getDrawable(bottomBarIcon)
        drawable?.setTint(ContextCompat.getColor(view.context, R.color.photonWhite))
        view.bottom_bar_icon_button.setImageDrawable(drawable)

        TransitionManager.beginDelayedTransition(
            view.collection_constraint_layout,
            transition
        )
        constraintSet.applyTo(view.collection_constraint_layout)
    }

    /**
     * Shared logic for [SaveCollectionStep.NameCollection] and [SaveCollectionStep.RenameCollection].
     */
    private fun editCollectionName(initialName: String?) {
        back_button.setOnClickListener {
            name_collection_edittext.hideKeyboard()
            val handler = Handler()
            handler.postDelayed({
                actionEmitter.onNext(CollectionCreationAction.BackPressed(step))
            }, TRANSITION_DURATION)
        }
        transition.addListener(object : Transition.TransitionListener {
            override fun onTransitionEnd(transition: Transition) {
                view.name_collection_edittext.showKeyboard()
                transition.removeListener(this)
            }

            override fun onTransitionStart(transition: Transition) {}
            override fun onTransitionCancel(transition: Transition) {}
            override fun onTransitionPause(transition: Transition) {}
            override fun onTransitionResume(transition: Transition) {}
        })
        TransitionManager.beginDelayedTransition(view.collection_constraint_layout, transition)
        val constraint = nameCollectionConstraints
        constraint.applyTo(view.collection_constraint_layout)
        name_collection_edittext.setText(initialName)
        name_collection_edittext.setSelection(0, name_collection_edittext.text.length)
    }

    companion object {
        private const val TRANSITION_DURATION = 200L
        private const val increaseButtonByDps = 16
        private const val COLLECTION_NAME_MAX_LENGTH = 128
    }
}
