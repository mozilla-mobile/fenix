package org.mozilla.fenix.settings

import androidx.core.content.edit
import androidx.preference.Preference
import org.mozilla.fenix.ext.settings

/**
 * Updates the corresponding [android.content.SharedPreferences] when the boolean [Preference] is changed.
 * The preference key is used as the shared preference key.
 */
open class SharedPreferenceUpdater : Preference.OnPreferenceChangeListener {

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        val newBooleanValue = newValue as? Boolean ?: return false
        preference.context.settings.preferences.edit {
            putBoolean(preference.key, newBooleanValue)
        }
        return true
    }
}
