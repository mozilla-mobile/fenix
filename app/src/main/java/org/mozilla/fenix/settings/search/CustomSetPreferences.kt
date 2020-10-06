/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.search

import mozilla.components.support.ktx.android.content.PreferencesHolder
import mozilla.components.support.ktx.android.content.StringSetPreference
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Property delegate for getting and setting a shared preference with a set containing a pair
 * of strings.
 *
 * Example usage:
 * ```
 * class Settings : PreferenceHolder {
 *     ...
 *     var connectedDevices by customStringSetPreference("connected_devices", default = emptySet())
 * }
 * ```
 */
fun customStringSetPreference(key: String, default: Set<Pair<String, String>>)
        : ReadWriteProperty<PreferencesHolder, Set<Pair<String, String>> =
    CustomStringSetPreference(key, default)


private class CustomStringSetPreference(
    private val key: String,
    private val default: Set<Pair<String, String>>
) : ReadWriteProperty<PreferencesHolder, Set<Pair<String, String>> {

    override fun getValue(thisRef: PreferencesHolder, property: KProperty<*>)
            : Set<Pair<String, String>> =
        thisRef.preferences.getValue(key, default) ?: default

    override fun setValue(thisRef: PreferencesHolder, property: KProperty<*>, value: Set<String>) =
        thisRef.preferences.edit().putValue(key, value).apply()
}