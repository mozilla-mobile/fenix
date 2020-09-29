/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.search

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import kotlinx.coroutines.launch
import mozilla.components.support.ktx.android.view.hideKeyboard
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.getPreferenceKey
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.ext.showToolbar
import org.mozilla.fenix.settings.SharedPreferenceUpdater
import org.mozilla.fenix.settings.requirePreference

class SearchEngineFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.search_preferences, rootKey)
        view?.hideKeyboard()
    }

    override fun onResume() {
        super.onResume()
        view?.hideKeyboard()
        setUpPreferences()
    }

    private fun setUpPreferences() {
        showToolbar(getString(R.string.preferences_search))

        val searchSuggestionsPreference =
            requirePreference<SwitchPreference>(R.string.pref_key_show_search_suggestions).apply {
                isChecked = context.settings().shouldShowSearchSuggestions
            }

        val autocompleteURLsPreference =
            requirePreference<SwitchPreference>(R.string.pref_key_enable_autocomplete_urls).apply {
                isChecked = context.settings().shouldAutocompleteInAwesomebar
            }

        val searchSuggestionsInPrivatePreference =
            requirePreference<CheckBoxPreference>(R.string.pref_key_show_search_suggestions_in_private).apply {
                isChecked = context.settings().shouldShowSearchSuggestionsInPrivate
            }

        val showSearchShortcuts =
            requirePreference<SwitchPreference>(R.string.pref_key_show_search_engine_shortcuts).apply {
                isChecked = context.settings().shouldShowSearchShortcuts
            }

        val showHistorySuggestions =
            requirePreference<SwitchPreference>(R.string.pref_key_search_browsing_history).apply {
                isChecked = context.settings().shouldShowHistorySuggestions
            }

        val showBookmarkSuggestions =
            requirePreference<SwitchPreference>(R.string.pref_key_search_bookmarks).apply {
                isChecked = context.settings().shouldShowBookmarkSuggestions
            }

        val showSyncedTabsSuggestions =
            requirePreference<SwitchPreference>(R.string.pref_key_search_synced_tabs).apply {
                isChecked = context.settings().shouldShowSyncedTabsSuggestions
            }

        val showClipboardSuggestions =
            requirePreference<SwitchPreference>(R.string.pref_key_show_clipboard_suggestions).apply {
                isChecked = context.settings().shouldShowClipboardSuggestions
            }

        val searchEngineListPreference =
            requirePreference<SearchEngineListPreference>(R.string.pref_key_search_engine_list)

        val showVoiceSearchPreference =
            requirePreference<SwitchPreference>(R.string.pref_key_show_voice_search).apply {
                isChecked = context.settings().shouldShowVoiceSearch
            }

        lifecycleScope.launch { searchEngineListPreference.reload(requireContext()) }

        setPreferenceListeners(
            listOf(
                searchSuggestionsPreference,
                showSearchShortcuts,
                showHistorySuggestions,
                showBookmarkSuggestions,
                showSyncedTabsSuggestions,
                showClipboardSuggestions,
                searchSuggestionsInPrivatePreference,
                showVoiceSearchPreference,
                autocompleteURLsPreference
            )
        )

        searchSuggestionsPreference.setOnPreferenceClickListener {
            if (!searchSuggestionsPreference.isChecked) {
                searchSuggestionsInPrivatePreference.isChecked = false
                searchSuggestionsInPrivatePreference.callChangeListener(false)
            }
            true
        }
    }

    private fun setPreferenceListeners(preferences: List<Preference>) {
        for (pref in preferences) {
            pref.onPreferenceChangeListener = SharedPreferenceUpdater()
        }
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        when (preference.key) {
            getPreferenceKey(R.string.pref_key_add_search_engine) -> {
                val directions = SearchEngineFragmentDirections
                    .actionSearchEngineFragmentToAddSearchEngineFragment()
                findNavController().navigate(directions)
            }
        }

        return super.onPreferenceTreeClick(preference)
    }
}
