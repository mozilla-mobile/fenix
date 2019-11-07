package org.mozilla.fenix.settings.search

import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_add_search_engine.*
import kotlinx.android.synthetic.main.search_engine_radio_button.view.*
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mozilla.components.browser.search.SearchEngine
import mozilla.components.browser.search.provider.AssetsSearchEngineProvider
import mozilla.components.browser.search.provider.filter.SearchEngineFilter
import mozilla.components.browser.search.provider.localization.LocaleSearchLocalizationProvider
import mozilla.components.browser.search.provider.localization.SearchLocalizationProvider

import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.ext.searchEngineManager

class AddSearchEngineFragment : Fragment(), CompoundButton.OnCheckedChangeListener {

    private var availableEngines: List<SearchEngine> = listOf()
    private var selectedEngine: SearchEngine? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        availableEngines = runBlocking {
            requireContext().components.search.provider.uninstalledSearchEngines(requireContext()).list
        }

        selectedEngine = availableEngines.firstOrNull()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_search_engine, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val layoutInflater = LayoutInflater.from(context)
        val layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val setupSearchEngineItem: (Int, SearchEngine) -> Unit = { index, engine ->
            val engineId = engine.identifier
            val engineItem = makeButtonFromSearchEngine(engine, layoutInflater, requireContext().resources)
            engineItem.id = index
            engineItem.tag = engineId
            engineItem.radio_button.isChecked = selectedEngine?.identifier == engine.identifier
            search_engine_group.addView(engineItem, layoutParams)
        }

        availableEngines.forEachIndexed(setupSearchEngineItem)
    }

    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity).title = getString(R.string.search_engine_add_custom_search_engine_title)
        (activity as AppCompatActivity).supportActionBar?.show()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.add_custom_searchengine_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.addSearchEngine -> {
                viewLifecycleOwner.lifecycleScope.launch {
                    requireContext().components.search.provider.installSearchEngine(
                        requireContext(),
                        selectedEngine!!
                    )
                }.invokeOnCompletion { findNavController().popBackStack() }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        availableEngines.forEach { engine ->
            val wrapper: ConstraintLayout = search_engine_group?.findViewWithTag(engine.identifier) ?: return

            when (wrapper.radio_button == buttonView) {
                true -> selectedEngine = engine
                false -> {
                    wrapper.radio_button.setOnCheckedChangeListener(null)
                    wrapper.radio_button.isChecked = false
                    wrapper.radio_button.setOnCheckedChangeListener(this)
                }
            }
        }
    }

    private fun makeCustomButton(layoutInflater: LayoutInflater): View {
        val wrapper = layoutInflater.inflate(R.layout.search_engine_radio_button, null) as ConstraintLayout
        wrapper.setOnClickListener { wrapper.radio_button.isChecked = true }
        wrapper.radio_button.setOnCheckedChangeListener(this)
        wrapper.engine_text.text = "Custom"
        wrapper.engine_icon.visibility = View.GONE
        wrapper.overflow_menu.visibility = View.GONE
        return wrapper
    }

    private fun makeButtonFromSearchEngine(
        engine: SearchEngine,
        layoutInflater: LayoutInflater,
        res: Resources
    ): View {
        val wrapper = layoutInflater.inflate(R.layout.search_engine_radio_button, null) as ConstraintLayout
        wrapper.setOnClickListener { wrapper.radio_button.isChecked = true }
        wrapper.radio_button.setOnCheckedChangeListener(this)
        wrapper.engine_text.text = engine.name
        val iconSize = res.getDimension(R.dimen.preference_icon_drawable_size).toInt()
        val engineIcon = BitmapDrawable(res, engine.icon)
        engineIcon.setBounds(0, 0, iconSize, iconSize)
        wrapper.engine_icon.setImageDrawable(engineIcon)
        wrapper.overflow_menu.visibility = View.GONE
        return wrapper
    }
}
