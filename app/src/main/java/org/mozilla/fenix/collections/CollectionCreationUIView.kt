package org.mozilla.fenix.collections

/* This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/. */

import android.os.Handler
import android.text.InputFilter
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import mozilla.components.support.ktx.android.view.hideKeyboard
import mozilla.components.support.ktx.android.view.showKeyboard
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.increaseTapArea
import org.mozilla.fenix.ext.urlToTrimmedHost
import org.mozilla.fenix.home.sessioncontrol.Tab
import org.mozilla.fenix.home.sessioncontrol.TabCollection
import org.mozilla.fenix.mvi.UIView
import kotlin.coroutines.CoroutineContext

class CollectionCreationUIView(
    container: ViewGroup,
    actionEmitter: Observer<CollectionCreationAction>,
    changesObservable: Observable<CollectionCreationChange>
) : UIView<CollectionCreationState, CollectionCreationAction, CollectionCreationChange>(
    container,
    actionEmitter,
    changesObservable
), CoroutineScope {
    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override val view = LayoutInflater.from(container.context)
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
        job = Job()
        transition.duration = TRANSITION_DURATION

        selectTabsConstraints.clone(collection_constraint_layout)
        selectCollectionConstraints.clone(
            view.context,
            R.layout.component_collection_creation_select_collection
        )
        nameCollectionConstraints.clone(
            view.context,
            R.layout.component_collection_creation_name_collection
        )

        view.bottom_bar_icon_button.apply {
            increaseTapArea(increaseButtonByDps)
        }

        view.name_collection_edittext.filters += InputFilter.LengthFilter(COLLECTION_NAME_MAX_LENGTH)
        view.name_collection_edittext.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE && v.text.toString().isNotBlank()) {
                when (step) {
                    is SaveCollectionStep.NameCollection -> {
                        actionEmitter.onNext(
                            CollectionCreationAction.SaveCollectionName(
                                selectedTabs.toList(),
                                v.text.toString()
                            )
                        )
                    }
                    is SaveCollectionStep.RenameCollection -> {
                        selectedCollection?.let {
                            actionEmitter.onNext(CollectionCreationAction.RenameCollection(it, v.text.toString()))
                        }
                    }
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

        when (it.saveCollectionStep) {
            is SaveCollectionStep.SelectTabs -> {
                view.tab_list.isClickable = true

                back_button.setOnClickListener {
                    actionEmitter.onNext(CollectionCreationAction.BackPressed(SaveCollectionStep.SelectTabs))
                }
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

                view.bottom_button_bar_layout.setOnClickListener(null)
                view.bottom_button_bar_layout.isClickable = false

                val drawable = view.context.getDrawable(R.drawable.ic_close)
                drawable?.setTint(ContextCompat.getColor(view.context, R.color.photonWhite))
                view.bottom_bar_icon_button.setImageDrawable(drawable)

                view.bottom_bar_icon_button.setOnClickListener {
                    actionEmitter.onNext(CollectionCreationAction.Close)
                }

                TransitionManager.beginDelayedTransition(
                    view.collection_constraint_layout,
                    transition
                )
                val constraint = selectTabsConstraints
                constraint.applyTo(view.collection_constraint_layout)

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

                save_button.visibility = if (it.selectedTabs.isEmpty()) {
                    View.GONE
                } else {
                    View.VISIBLE
                }
            }
            is SaveCollectionStep.SelectCollection -> {
                view.tab_list.isClickable = false

                save_button.visibility = View.GONE

                view.bottom_bar_text.text =
                    view.context.getString(R.string.create_collection_add_new_collection)

                val drawable = view.context.getDrawable(R.drawable.ic_new)
                drawable?.setTint(ContextCompat.getColor(view.context, R.color.photonWhite))
                view.bottom_bar_icon_button.setImageDrawable(drawable)
                view.bottom_bar_icon_button.setOnClickListener {
                    actionEmitter.onNext(CollectionCreationAction.AddNewCollection)
                }

                view.bottom_button_bar_layout.isClickable = true
                view.bottom_button_bar_layout.setOnClickListener {
                    actionEmitter.onNext(CollectionCreationAction.AddNewCollection)
                }

                back_button.setOnClickListener {
                    actionEmitter.onNext(CollectionCreationAction.BackPressed(SaveCollectionStep.SelectCollection))
                }
                TransitionManager.beginDelayedTransition(
                    view.collection_constraint_layout,
                    transition
                )
                val constraint = selectCollectionConstraints
                constraint.applyTo(view.collection_constraint_layout)
                back_button.text =
                    view.context.getString(R.string.create_collection_select_collection)
            }
            is SaveCollectionStep.NameCollection -> {
                view.tab_list.isClickable = false

                collectionCreationTabListAdapter.updateData(it.selectedTabs.toList(), it.selectedTabs, true)
                back_button.setOnClickListener {
                    name_collection_edittext.hideKeyboard()
                    val handler = Handler()
                    handler.postDelayed({
                        actionEmitter.onNext(CollectionCreationAction.BackPressed(SaveCollectionStep.NameCollection))
                    }, TRANSITION_DURATION)
                }
                transition.addListener(object : Transition.TransitionListener {
                    override fun onTransitionStart(transition: Transition) {
                    }

                    override fun onTransitionEnd(transition: Transition) {
                        view.name_collection_edittext.showKeyboard()
                        transition.removeListener(this)
                    }

                    override fun onTransitionCancel(transition: Transition) {}
                    override fun onTransitionPause(transition: Transition) {}
                    override fun onTransitionResume(transition: Transition) {}
                })
                TransitionManager.beginDelayedTransition(
                    view.collection_constraint_layout,
                    transition
                )
                val constraint = nameCollectionConstraints
                constraint.applyTo(view.collection_constraint_layout)
                name_collection_edittext.setText(
                    view.context.getString(
                        R.string.create_collection_default_name,
                        it.tabCollections.size + 1
                    )
                )
                name_collection_edittext.setSelection(0, name_collection_edittext.text.length)
                back_button.text =
                    view.context.getString(R.string.create_collection_name_collection)
            }
            is SaveCollectionStep.RenameCollection -> {
                view.tab_list.isClickable = false

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

                back_button.setOnClickListener {
                    name_collection_edittext.hideKeyboard()
                    val handler = Handler()
                    handler.postDelayed({
                        actionEmitter.onNext(CollectionCreationAction.BackPressed(SaveCollectionStep.RenameCollection))
                    }, TRANSITION_DURATION)
                }
                transition.addListener(object : Transition.TransitionListener {
                    override fun onTransitionStart(transition: Transition) {
                    }

                    override fun onTransitionEnd(transition: Transition) {
                        view.name_collection_edittext.showKeyboard()
                        transition.removeListener(this)
                    }

                    override fun onTransitionCancel(transition: Transition) {}
                    override fun onTransitionPause(transition: Transition) {}
                    override fun onTransitionResume(transition: Transition) {}
                })
                TransitionManager.beginDelayedTransition(
                    view.collection_constraint_layout,
                    transition
                )
                val constraint = nameCollectionConstraints
                constraint.applyTo(view.collection_constraint_layout)
                name_collection_edittext.setText(it.selectedTabCollection?.title)
                name_collection_edittext.setSelection(0, name_collection_edittext.text.length)
                back_button.text =
                    view.context.getString(R.string.create_collection_name_collection)
            }
        }
        collectionSaveListAdapter.updateData(it.tabCollections, it.selectedTabs)
    }

    fun onResumed() {
        if (step == SaveCollectionStep.NameCollection) {
            view.name_collection_edittext.showKeyboard()
        }
    }

    fun onKey(keyCode: Int, event: KeyEvent?): Boolean {
        if (event?.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
            when (step) {
                SaveCollectionStep.SelectTabs -> {
                    actionEmitter.onNext(CollectionCreationAction.BackPressed(SaveCollectionStep.SelectTabs))
                }
                SaveCollectionStep.SelectCollection -> {
                    actionEmitter.onNext(CollectionCreationAction.BackPressed(SaveCollectionStep.SelectCollection))
                }
                SaveCollectionStep.NameCollection -> {
                    actionEmitter.onNext(CollectionCreationAction.BackPressed(SaveCollectionStep.NameCollection))
                }
                SaveCollectionStep.RenameCollection -> {
                    actionEmitter.onNext(CollectionCreationAction.BackPressed(SaveCollectionStep.RenameCollection))
                }
            }
            return true
        } else {
            return false
        }
    }

    companion object {
        private const val TRANSITION_DURATION = 200L
        private const val increaseButtonByDps = 16
        private const val COLLECTION_NAME_MAX_LENGTH = 128
    }
}
