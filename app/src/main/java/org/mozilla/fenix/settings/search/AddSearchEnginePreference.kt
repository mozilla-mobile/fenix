package org.mozilla.fenix.settings.search

import android.content.Context
import android.util.AttributeSet
import android.widget.CompoundButton
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import mozilla.components.browser.search.SearchEngine
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.settings

class AddSearchEnginePreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.preferenceStyle
) : Preference(context, attrs, defStyleAttr) {

    init {
        layoutResource = R.layout.preference_search_add_engine
    }
}