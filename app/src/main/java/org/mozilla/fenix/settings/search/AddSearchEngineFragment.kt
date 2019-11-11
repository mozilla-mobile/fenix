package org.mozilla.fenix.settings.search

import android.content.res.Resources
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
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
import mozilla.components.support.ktx.kotlin.toNormalizedUrl

import org.mozilla.fenix.R
import org.mozilla.fenix.components.FenixSnackbar
import org.mozilla.fenix.components.searchengine.CustomSearchEngineStore
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.logDebug
import org.mozilla.fenix.ext.requireComponents
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

sealed class SearchStringResult {
    object Success : SearchStringResult()
    object MalformedURL : SearchStringResult()
    object CannotReach : SearchStringResult()
}

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

        selectedIndex = if (availableEngines.isEmpty()) -1 else 0
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

        toggleCustomForm(selectedIndex == -1)
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
        custom_search_engine_name_field.error = ""
        custom_search_engine_search_string_field.error = ""

        val name = edit_engine_name.text?.toString() ?: ""
        val searchString = edit_search_string.text?.toString() ?: ""


        var hasError = false
        if (name.isEmpty()) {
            custom_search_engine_name_field.error = resources.getString(R.string.search_add_custom_engine_error_empty_name)
            hasError = true
        }

        if (searchString.isEmpty()) {
            custom_search_engine_search_string_field.error = resources.getString(R.string.search_add_custom_engine_error_empty_search_string)
            hasError = true
        }

        if (hasError) { return }

        viewLifecycleOwner.lifecycleScope.launch {
            val result = validateSearchString(searchString)

            launch(Main) {
                when (result) {
                    SearchStringResult.MalformedURL -> {
                        custom_search_engine_search_string_field.error = "Malformed URL"
                    }
                    SearchStringResult.CannotReach -> {
                        custom_search_engine_search_string_field.error = "Cannot Reach"
                    }
                    SearchStringResult.Success -> {
                        CustomSearchEngineStore.addSearchEngine(requireContext(), name, searchString)
                        requireComponents.search.provider.reload()
                        val successMessage = resources.getString(R.string.search_add_custom_engine_success_message, name)

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

    private fun validateSearchString(searchString: String): SearchStringResult {
        // we should share the code to substitute and normalize the search string (see SearchEngine.buildSearchUrl).
        val encodedTestQuery = Uri.encode("testSearchEngineValidation")

        val normalizedHttpsSearchURLStr = searchString.toNormalizedUrl()
        val searchURLStr = normalizedHttpsSearchURLStr.replace("%s".toRegex(), encodedTestQuery)
        val searchURL = try { URL(searchURLStr) } catch (e: MalformedURLException) {
            return SearchStringResult.MalformedURL
        }

        val connection = searchURL.openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = true
        connection.connectTimeout = SEARCH_QUERY_VALIDATION_TIMEOUT_MILLIS
        connection.readTimeout = SEARCH_QUERY_VALIDATION_TIMEOUT_MILLIS

        return try {
            if (connection.responseCode < VALID_RESPONSE_CODE_UPPER_BOUND) {
                SearchStringResult.Success
            } else {
                SearchStringResult.CannotReach
            }
        } catch (e: IOException) {
            logDebug(LOGTAG, "Failure to get response code from server: returning invalid search query")
            SearchStringResult.CannotReach
        } finally {
            try { connection.inputStream.close() } catch (_: IOException) {
                logDebug(LOGTAG, "connection.inputStream failed to close")
            }

            connection.disconnect()
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
        val wrapper = layoutInflater.inflate(R.layout.custom_search_engine_radio_button, null) as ConstraintLayout
        wrapper.setOnClickListener { wrapper.radio_button.isChecked = true }
        wrapper.radio_button.setOnCheckedChangeListener(this)
        return wrapper
    }

    private fun toggleCustomForm(isEnabled: Boolean) {
        custom_search_engine_form.alpha = if (isEnabled) {
            1.0f
        } else {
            0.2f
        }

        edit_search_string.isEnabled = isEnabled
        edit_engine_name.isEnabled = isEnabled
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

    companion object {
        private const val LOGTAG = "AddSearchEngineFragment"
        private val SEARCH_QUERY_VALIDATION_TIMEOUT_MILLIS = 4000
        private val VALID_RESPONSE_CODE_UPPER_BOUND = 300
    }
}
