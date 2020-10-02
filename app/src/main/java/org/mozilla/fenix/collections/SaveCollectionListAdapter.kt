/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.collections

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.BlendModeColorFilterCompat.createBlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat.SRC_IN
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.collections_list_item.*
import mozilla.components.feature.tab.collections.TabCollection
import org.mozilla.fenix.R
import org.mozilla.fenix.components.description
import org.mozilla.fenix.ext.getIconColor
import org.mozilla.fenix.home.Tab
import org.mozilla.fenix.utils.view.ViewHolder

class SaveCollectionListAdapter(
    private val interactor: CollectionCreationInteractor
) : RecyclerView.Adapter<CollectionViewHolder>() {

    private var tabCollections = listOf<TabCollection>()
    private var selectedTabs: Set<Tab> = setOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CollectionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(CollectionViewHolder.LAYOUT_ID, parent, false)

        return CollectionViewHolder(view)
    }

    override fun onBindViewHolder(holder: CollectionViewHolder, position: Int) {
        val collection = tabCollections[position]
        holder.bind(collection)
        holder.itemView.setOnClickListener {
            interactor.selectCollection(collection, selectedTabs.toList())
        }
    }

    override fun getItemCount(): Int = tabCollections.size

    fun updateData(tabCollections: List<TabCollection>, selectedTabs: Set<Tab>) {
        this.tabCollections = tabCollections
        this.selectedTabs = selectedTabs
        notifyDataSetChanged()
    }
}

class CollectionViewHolder(view: View) : ViewHolder(view) {

    fun bind(collection: TabCollection) {
        collection_item.text = collection.title
        collection_description.text = collection.description(itemView.context)
        collection_icon.colorFilter =
            createBlendModeColorFilterCompat(collection.getIconColor(itemView.context), SRC_IN)
    }

    companion object {
        const val LAYOUT_ID = R.layout.collections_list_item
    }
}
