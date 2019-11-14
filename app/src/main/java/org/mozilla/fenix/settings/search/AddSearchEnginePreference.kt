package org.mozilla.fenix.settings.search

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import org.mozilla.fenix.R

class AddSearchEnginePreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.preferenceStyle
) : Preference(context, attrs, defStyleAttr) {
    init {
        layoutResource = R.layout.preference_search_add_engine
    }
}
