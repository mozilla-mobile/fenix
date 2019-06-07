package org.mozilla.fenix.collections

/* This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/. */

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Observer
import kotlinx.android.synthetic.main.collections_list_item.view.*
import kotlinx.android.synthetic.main.collections_list_item.view.collection_description
import kotlinx.android.synthetic.main.collections_list_item.view.collection_icon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.mozilla.fenix.R
import org.mozilla.fenix.components.description
import org.mozilla.fenix.home.sessioncontrol.Tab
import org.mozilla.fenix.home.sessioncontrol.TabCollection
import kotlin.coroutines.CoroutineContext

class SaveCollectionListAdapter(
    val actionEmitter: Observer<CollectionCreationAction>
) : RecyclerView.Adapter<CollectionViewHolder>() {

    private lateinit var job: Job
    private var tabCollections = listOf<TabCollection>()
    private var selectedTabs: Set<Tab> = setOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CollectionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(CollectionViewHolder.LAYOUT_ID, parent, false)

        return CollectionViewHolder(view, actionEmitter, job)
    }

    override fun onBindViewHolder(holder: CollectionViewHolder, position: Int) {
        val collection = tabCollections[position]
        holder.bind(collection)
        holder.view.setOnClickListener {
            collection.apply {
                val action = CollectionCreationAction.SelectCollection(this, selectedTabs.toList())
                actionEmitter.onNext(action)
            }
        }
    }

    override fun getItemCount(): Int = tabCollections.size

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        job = Job()
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        job.cancel()
    }

    fun updateData(tabCollections: List<TabCollection>, selectedTabs: Set<Tab>) {
        this.tabCollections = tabCollections
        this.selectedTabs = selectedTabs
        notifyDataSetChanged()
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

    fun bind(collection: TabCollection) {
        this.collection = collection
        launch(Dispatchers.Main) {
            view.collection_item.text = collection.title
            view.collection_description.text = collection.description(view.context)

            view.collection_icon.setColorFilter(
                ContextCompat.getColor(
                    view.context,
                    getIconColor(collection.id)
                ),
                android.graphics.PorterDuff.Mode.SRC_IN
            )
        }
    }

    @Suppress("ComplexMethod", "MagicNumber")
    private fun getIconColor(id: Long): Int {
        return when ((id % 5).toInt()) {
            0 -> R.color.collection_icon_color_violet
            1 -> R.color.collection_icon_color_blue
            2 -> R.color.collection_icon_color_pink
            3 -> R.color.collection_icon_color_green
            4 -> R.color.collection_icon_color_yellow
            else -> R.color.white_color
        }
    }

    companion object {
        const val LAYOUT_ID = R.layout.collections_list_item
        const val maxTitleLength = 20
    }
}
