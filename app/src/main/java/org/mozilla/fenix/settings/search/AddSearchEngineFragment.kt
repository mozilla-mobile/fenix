package org.mozilla.fenix.settings.search

import android.content.res.Resources
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.RadioButton
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_add_search_engine.*
import kotlinx.android.synthetic.main.search_engine_radio_button.view.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mozilla.components.browser.search.SearchEngine

import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.logDebug

class AddSearchEngineFragment : Fragment(), CompoundButton.OnCheckedChangeListener {

    private var availableEngines: List<SearchEngine> = listOf()
    private var selectedIndex: Int = -1
    private val engineViews = mutableListOf<View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        availableEngines = runBlocking {
            requireContext().components.search.provider.uninstalledSearchEngines(requireContext()).list
        }

        selectedIndex = availableEngines.size - 1
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
            engineItem.radio_button.isChecked = selectedIndex == index
            engineViews.add(engineItem)
            search_engine_group.addView(engineItem, layoutParams)
        }

        availableEngines.forEachIndexed(setupSearchEngineItem)

        val engineItem = makeCustomButton(layoutInflater)
        engineItem.id = -1
        engineItem.radio_button.isChecked = selectedIndex == -1
        engineViews.add(engineItem)
        search_engine_group.addView(engineItem, layoutParams)
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
                when (selectedIndex) {
                    -1 -> createCustomEngine()
                    else -> {
                        val engine = availableEngines[selectedIndex]
                        installEngine(engine)
                    }
                }

                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun createCustomEngine() {
        logDebug("AddSearchEngineFragment", "Creating Engine!")
    }

    private fun installEngine(engine: SearchEngine) {
        viewLifecycleOwner.lifecycleScope.launch {
            requireContext().components.search.provider.installSearchEngine(
                requireContext(),
                engine
            )
        }.invokeOnCompletion { findNavController().popBackStack() }
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        engineViews.forEach {
            when (it.radio_button == buttonView) {
                true -> {
                    selectedIndex = it.id
                }
                false -> {
                    it.radio_button.setOnCheckedChangeListener(null)
                    it.radio_button.isChecked = false
                    it.radio_button.setOnCheckedChangeListener(this)
                }
            }
        }
    }

    private fun makeCustomButton(layoutInflater: LayoutInflater): View {
        val wrapper = layoutInflater.inflate(R.layout.custom_search_engine_radio_button, null) as ConstraintLayout
        wrapper.setOnClickListener { wrapper.radio_button.isChecked = true }
        wrapper.radio_button.setOnCheckedChangeListener(this)
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
