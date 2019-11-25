package org.mozilla.fenix.tabtray

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.component_tab_tray.view.*
import kotlinx.android.synthetic.main.tab_tray_list_item.view.*
import org.mozilla.fenix.R

class TabItemViewHolder(
    private val view: View
) : RecyclerView.ViewHolder(view) {


    fun bind(tab: Tab) {
        view.title.text = tab.title
        view.url.text= tab.url
    }

    companion object {
        const val LAYOUT_ID = R.layout.tab_tray_list_item
    }
}

class TabTrayAdapter: RecyclerView.Adapter<TabItemViewHolder>() {
    private var tabs = listOf<Tab>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TabItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(TabItemViewHolder.LAYOUT_ID, parent, false)
        return TabItemViewHolder(view)
    }

    override fun getItemCount() = tabs.size

    override fun onBindViewHolder(holder: TabItemViewHolder, position: Int) {
        holder.bind(tabs[position])
    }

    fun updateTabs(tabs: List<Tab>) {
        this.tabs = tabs
        notifyDataSetChanged()
    }
}

class TabTrayView(val container: ViewGroup) : LayoutContainer {

    val tabTrayAdapter = TabTrayAdapter()

    override val containerView: View?
        get() = container

    val view: View = LayoutInflater.from(container.context)
        .inflate(R.layout.component_tab_tray, container, true)

    init {
        view.tab_tray_list.apply {
            adapter = tabTrayAdapter
            (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        }
    }

    fun update(state: TabTrayFragmentState) {
        tabTrayAdapter.updateTabs(state.tabs)
    }
}