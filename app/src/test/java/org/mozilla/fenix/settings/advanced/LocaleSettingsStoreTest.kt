/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.advanced

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

class LocaleSettingsStoreTest {

    private lateinit var localeSettingsStore: LocaleSettingsStore
    private val selectedLocale = Locale("en", "UK")
    private val otherLocale = Locale("fr")

    @Before
    fun setup() {
        val localeList = listOf(
            Locale("fr"), // default
            otherLocale,
            selectedLocale
        )

        localeSettingsStore =
            LocaleSettingsStore(LocaleSettingsState(localeList, localeList, selectedLocale))
    }

    @Test
    fun `change selected locale`() = runBlocking {
        localeSettingsStore.dispatch(LocaleSettingsAction.Select(otherLocale)).join()

        assertEquals(otherLocale, localeSettingsStore.state.selectedLocale)
    }

    @Test
    fun `change selected list by search query`() = runBlocking {
        localeSettingsStore.dispatch(LocaleSettingsAction.Search("Eng")).join()

        assertEquals(2, (localeSettingsStore.state.searchedLocaleList as ArrayList).size)
        assertEquals(selectedLocale, localeSettingsStore.state.searchedLocaleList[1])
    }
}
