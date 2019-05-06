package org.mozilla.fenix.collections

/* This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/. */

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Observer
import kotlinx.android.synthetic.main.collections_list_item.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.mozilla.fenix.R
import org.mozilla.fenix.home.sessioncontrol.TabCollection
import kotlin.coroutines.CoroutineContext

class SaveCollectionListAdapter(
    val actionEmitter: Observer<CollectionCreationAction>
) : RecyclerView.Adapter<CollectionViewHolder>() {

    private var collections: List<TabCollection> = listOf()
    private lateinit var job: Job

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CollectionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(CollectionViewHolder.LAYOUT_ID, parent, false)

        return CollectionViewHolder(view, actionEmitter, job)
    }

    override fun onBindViewHolder(holder: CollectionViewHolder, position: Int) {
        val collection = collections[position]
        holder.bind(collection)
    }

    override fun getItemCount(): Int = collections.size

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        job = Job()
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        job.cancel()
    }
}

class CollectionViewHolder(
    val view: View,
    actionEmitter: Observer<CollectionCreationAction>,
    val job: Job
) :
    RecyclerView.ViewHolder(view), CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    private var collection: TabCollection? = null

    private val listener = View.OnClickListener {
        collection?.apply {
            val action = CollectionCreationAction.SelectCollection(this)
            actionEmitter.onNext(action)
        }
    }

    init {
        view.setOnClickListener(listener)
    }

    fun bind(collection: TabCollection) {
        this.collection = collection
        view.collection_item.text = collection.title
    }

    companion object {
        const val LAYOUT_ID = R.layout.collections_list_item
    }
}
