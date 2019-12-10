package org.mozilla.fenix.tabtray

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.component_tab_tray.view.*
import kotlinx.android.synthetic.main.tab_tray_list_item.view.*
import org.mozilla.fenix.R
import org.mozilla.fenix.components.ui.SelectableListItemView
import org.mozilla.fenix.components.ui.SelectionHolder
import org.mozilla.fenix.components.ui.SelectionInteractor
import org.mozilla.fenix.library.LibraryPageView

interface TabTrayInteractor : SelectionInteractor<Tab> {
    fun closeButtonTapped(tab: Tab)
}

class TabItemViewHolder(
    private val view: SelectableListItemView,
    private val interactor: TabTrayInteractor,
    private val selectionHolder: SelectionHolder<Tab>
) : RecyclerView.ViewHolder(view) {
    private var tab: Tab? = null
    private var mode: TabTrayFragmentState.Mode? =null

    init {
        view.displayAs(SelectableListItemView.ItemType.CLOSABLE_ITEM)
        view.accessoryView.setOnClickListener {
            if (mode is TabTrayFragmentState.Mode.Editing) return@setOnClickListener
            tab?.apply(interactor::closeButtonTapped)
        }
    }

    fun bind(tab: Tab, mode: TabTrayFragmentState.Mode) {
        this.tab = tab
        this.mode = mode
        view.title.text = tab.title
        view.url.text= tab.url
        view.loadFavicon(tab.url)
        view.setSelectionInteractor(tab, selectionHolder, interactor)
        view.changeSelected(tab in selectionHolder.selectedItems)
        view.accessoryView.visibility = if (mode is TabTrayFragmentState.Mode.Normal) {
            View.VISIBLE
        } else {
            View.INVISIBLE
        }
    }

    companion object {
        const val LAYOUT_ID = R.layout.selectable_list_item
    }
}

class TabTrayAdapter(
    private val interactor: TabTrayInteractor
): RecyclerView.Adapter<TabItemViewHolder>(), SelectionHolder<Tab> {
    private var state = TabTrayFragmentState(listOf(), TabTrayFragmentState.Mode.Normal)


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TabItemViewHolder {
        val view = SelectableListItemView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }

        return TabItemViewHolder(view, interactor, this)
    }

    override fun getItemCount() = state.tabs.size

    override fun onBindViewHolder(holder: TabItemViewHolder, position: Int) {
        holder.bind(state.tabs[position], state.mode)
    }

    fun updateState(state: TabTrayFragmentState) {
        this.state = state
        notifyDataSetChanged()
    }

    override val selectedItems: Set<Tab>
        get() = state.mode.selectedTabs
}

class TabTrayView(
    val container: ViewGroup,
    val interactor: TabTrayInteractor
) : LibraryPageView(container), LayoutContainer {

    val tabTrayAdapter = TabTrayAdapter(interactor)
    
    val view: View = LayoutInflater.from(container.context)
        .inflate(R.layout.component_tab_tray, container, true)

    init {
        view.tab_tray_list.apply {
            adapter = tabTrayAdapter
            (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        }
    }

    fun update(state: TabTrayFragmentState) {
        tabTrayAdapter.updateState(state)
    }
}