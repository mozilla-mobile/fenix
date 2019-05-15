/* This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.mvi

import androidx.lifecycle.ViewModel
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject

interface UIComponentViewModel<S : ViewState, C : Change> {
    val changes: Observer<C>
    val state: Observable<S>
}

interface UIComponentViewModelProvider<S : ViewState, C : Change> {
    fun fetchViewModel(): UIComponentViewModel<S, C>
}

abstract class UIComponent<S : ViewState, A : Action, C : Change>(
    protected val actionEmitter: Observer<A>,
    protected val changesObservable: Observable<C>,
    private val viewModelProvider: UIComponentViewModelProvider<S, C>
) {
    open val uiView: UIView<S, A, C> by lazy { initView() }

    abstract fun initView(): UIView<S, A, C>
    open fun getContainerId() = uiView.containerId

    fun bind(): Disposable {
        val viewModel = viewModelProvider.fetchViewModel()

        changesObservable.subscribe(viewModel.changes)
        return viewModel.state.subscribe(uiView.updateView())
    }
}

abstract class UIComponentViewModelBase<S : ViewState, C: Change>(
    initialState: S,
    reducer: Reducer<S, C>
) : ViewModel(), UIComponentViewModel<S, C> {

    final override val changes: Observer<C>
    private var _state: BehaviorSubject<S> = BehaviorSubject.createDefault(initialState)
    override val state: Observable<S>
        get() = _state

    init {
        changes = PublishSubject.create()

        changes
            .withLatestFrom(_state)
            .map { reducer(it.second, it.first) }
            .distinctUntilChanged()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(_state)
    }
}
