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
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.custom_search_engine.*
import kotlinx.android.synthetic.main.fragment_add_search_engine.*
import kotlinx.android.synthetic.main.search_engine_radio_button.view.*
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mozilla.components.browser.search.SearchEngine
import org.mozilla.fenix.R
import org.mozilla.fenix.components.FenixSnackbar
import org.mozilla.fenix.components.searchengine.CustomSearchEngineStore
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.requireComponents
import java.util.Locale

@SuppressWarnings("LargeClass", "TooManyFunctions")
class AddSearchEngineFragment : Fragment(), CompoundButton.OnCheckedChangeListener {
    private var availableEngines: List<SearchEngine> = listOf()
    private var selectedIndex: Int = -1
    private val engineViews = mutableListOf<View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        availableEngines = runBlocking {
            requireContext()
                .components
                .search
                .provider
                .uninstalledSearchEngines(requireContext())
                .list
        }

        selectedIndex = if (availableEngines.isEmpty()) CUSTOM_INDEX else FIRST_INDEX
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
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
            val engineItem = makeButtonFromSearchEngine(
                engine = engine,
                layoutInflater = layoutInflater,
                res = requireContext().resources
            )
            engineItem.id = index
            engineItem.tag = engineId
            engineItem.radio_button.isChecked = selectedIndex == index
            engineViews.add(engineItem)
            search_engine_group.addView(engineItem, layoutParams)
        }

        availableEngines.forEachIndexed(setupSearchEngineItem)

        val engineItem = makeCustomButton(layoutInflater)
        engineItem.id = CUSTOM_INDEX
        engineItem.radio_button.isChecked = selectedIndex == CUSTOM_INDEX
        engineViews.add(engineItem)
        search_engine_group.addView(engineItem, layoutParams)

        toggleCustomForm(selectedIndex == CUSTOM_INDEX)
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
                    CUSTOM_INDEX -> createCustomEngine()
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
        custom_search_engine_name_field.error = ""
        custom_search_engine_search_string_field.error = ""

        val name = edit_engine_name.text?.toString() ?: ""
        val searchString = edit_search_string.text?.toString() ?: ""

        var hasError = false
        if (name.isEmpty()) {
            custom_search_engine_name_field.error = resources
                .getString(R.string.search_add_custom_engine_error_empty_name)
            hasError = true
        }

        val existingIdentifiers = requireComponents
            .search
            .provider
            .allSearchEngineIdentifiers()
            .map { it.toLowerCase(Locale.ROOT) }

        if (existingIdentifiers.contains(name.toLowerCase(Locale.ROOT))) {
            custom_search_engine_name_field.error = resources
                .getString(R.string.search_add_custom_engine_error_existing_name, name)
            hasError = true
        }

        if (searchString.isEmpty()) {
            custom_search_engine_search_string_field
                .error = resources.getString(R.string.search_add_custom_engine_error_empty_search_string)
            hasError = true
        }

        if (!searchString.contains("%s")) {
            custom_search_engine_name_field
                .error = resources.getString(R.string.search_add_custom_engine_error_missing_template)
            hasError = true
        }

        if (hasError) { return }

        viewLifecycleOwner.lifecycleScope.launch {
            val result = SearchStringValidator.isSearchStringValid(searchString)

            launch(Main) {
                when (result) {
                    SearchStringValidator.Result.MalformedURL -> {
                        custom_search_engine_search_string_field.error = "Malformed URL"
                    }
                    SearchStringValidator.Result.CannotReach -> {
                        custom_search_engine_search_string_field.error = "Cannot Reach"
                    }
                    SearchStringValidator.Result.Success -> {
                        CustomSearchEngineStore.addSearchEngine(
                            context = requireContext(),
                            engineName = name,
                            searchQuery = searchString
                        )
                        requireComponents.search.provider.reload()
                        val successMessage = resources
                            .getString(R.string.search_add_custom_engine_success_message, name)

                        view?.also {
                            FenixSnackbar.make(it, FenixSnackbar.LENGTH_SHORT)
                                .setText(successMessage)
                                .show()
                        }

                        findNavController().popBackStack()
                    }
                }
            }
        }
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

        toggleCustomForm(selectedIndex == -1)
    }

    private fun makeCustomButton(layoutInflater: LayoutInflater): View {
        val wrapper = layoutInflater
            .inflate(R.layout.custom_search_engine_radio_button, null) as ConstraintLayout
        wrapper.setOnClickListener { wrapper.radio_button.isChecked = true }
        wrapper.radio_button.setOnCheckedChangeListener(this)
        return wrapper
    }

    private fun toggleCustomForm(isEnabled: Boolean) {
        custom_search_engine_form.alpha = if (isEnabled) ENABLED_ALPHA else DISABLED_ALPHA
        edit_search_string.isEnabled = isEnabled
        edit_engine_name.isEnabled = isEnabled
    }

    private fun makeButtonFromSearchEngine(
        engine: SearchEngine,
        layoutInflater: LayoutInflater,
        res: Resources
    ): View {
        val wrapper = layoutInflater
            .inflate(R.layout.search_engine_radio_button, null) as ConstraintLayout
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

    companion object {
        private const val ENABLED_ALPHA = 1.0f
        private const val DISABLED_ALPHA = 0.2f
        private const val CUSTOM_INDEX = -1
        private const val FIRST_INDEX = 0
    }
}
