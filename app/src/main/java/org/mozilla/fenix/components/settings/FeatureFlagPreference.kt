/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.settings

import mozilla.components.support.ktx.android.content.PreferencesHolder
import mozilla.components.support.ktx.android.content.booleanPreference
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

private class DummyProperty : ReadWriteProperty<PreferencesHolder, Boolean> {
    override fun getValue(thisRef: PreferencesHolder, property: KProperty<*>) = false
    override fun setValue(thisRef: PreferencesHolder, property: KProperty<*>, value: Boolean) = Unit
}

/**
 * Property delegate for getting and setting a boolean shared preference gated by a feature flag.
 */
fun featureFlagPreference(key: String, default: Boolean, featureFlag: Boolean) =
    if (featureFlag) {
        booleanPreference(key, default)
    } else {
        DummyProperty()
    }

private class LazyPreference(val key: String, val default: () -> Boolean) :
    ReadWriteProperty<PreferencesHolder, Boolean> {
    private val property: ReadWriteProperty<PreferencesHolder, Boolean> by lazy {
        booleanPreference(key, default())
    }

    override fun getValue(thisRef: PreferencesHolder, property: KProperty<*>) =
        this.property.getValue(thisRef, property)

    override fun setValue(thisRef: PreferencesHolder, property: KProperty<*>, value: Boolean) =
        this.property.setValue(thisRef, property, value)
}

/**
 * Property delegate for getting and setting lazily a boolean shared preference gated by a feature flag.
 */
fun lazyFeatureFlagPreference(key: String, featureFlag: Boolean, default: () -> Boolean) =
    if (featureFlag) {
        LazyPreference(key, default)
    } else {
        DummyProperty()
    }
