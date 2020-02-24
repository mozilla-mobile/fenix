/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.fenix.settings

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class DropDownListPreferenceTest {

    private lateinit var preference: DropDownListPreference

    @Before
    fun before() {
        preference = mockk()
    }

    @Test
    fun `WHEN findEntriesValue is called with a non-string THEN it returns null`() {
        assertNull(preference.findEntriesValue(null))
        assertNull(preference.findEntriesValue(1))
        assertNull(preference.findEntriesValue(Object()))
        assertNull(preference.findEntriesValue(listOf<Char>()))
    }

    @Test
    fun `GIVEN newValue is not found in entryValues WHEN findEntriesValue is called with newValue THEN it should return null`() {
        val newValue = "key"
        every { preference.findIndexOfValue(newValue) } returns -1

        assertNull(preference.findEntriesValue(newValue))
    }

    @Test
    fun `GIVEN entryValues and entries contain values WHEN findEntriesValue is called THEN it should return the entry`() {
        val entries = arrayOf("use private mode!", "use normal mode!", "use something else!")
        val entryValues = arrayOf("private", "normal", "other")

        every { preference. entries } returns entries
        entryValues.forEachIndexed { i, value ->
            every { preference.findIndexOfValue(value) } returns i
        }

        assertEquals(entries[0], preference.findEntriesValue(entryValues[0]))
        assertEquals(entries[1], preference.findEntriesValue(entryValues[1]))
        assertEquals(entries[2], preference.findEntriesValue(entryValues[2]))
    }
}
