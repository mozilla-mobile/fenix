/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import mozilla.components.feature.search.widget.AppSearchWidgetProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.helpers.FenixRobolectricTestRunner
import org.mozilla.gecko.search.SearchWidgetProvider

@RunWith(FenixRobolectricTestRunner::class)
class SearchWidgetProviderTest {

    @Test
    fun `GIVEN widgets set on screen shown WHEN updateAllWidgets is called THEN it sends a broadcast to update all widgets`() {
        try {
            mockkStatic(AppWidgetManager::class)
            val widgetManager: AppWidgetManager = mockk()
            every { AppWidgetManager.getInstance(any()) } returns widgetManager
            val componentNameCaptor = slot<ComponentName>()
            val widgetsToUpdate = intArrayOf(1, 2)
            every { widgetManager.getAppWidgetIds(capture(componentNameCaptor)) } returns widgetsToUpdate
            val context: Context = mockk(relaxed = true)
            val intentCaptor = slot<Intent>()
            every { context.sendBroadcast(capture(intentCaptor)) } just Runs

            AppSearchWidgetProvider.updateAllWidgets(context, SearchWidgetProvider::class.java)

            verify { context.sendBroadcast(any()) }
            assertEquals(SearchWidgetProvider::class.java.name, componentNameCaptor.captured.className)
            assertEquals(SearchWidgetProvider::class.java.name, intentCaptor.captured.component!!.className)
            assertEquals(AppWidgetManager.ACTION_APPWIDGET_UPDATE, intentCaptor.captured.action)
            assertEquals(widgetsToUpdate, intentCaptor.captured.extras!!.get(AppWidgetManager.EXTRA_APPWIDGET_IDS))
        } finally {
            unmockkStatic(AppWidgetManager::class)
        }
    }

    @Test
    fun `GIVEN no widgets set shown WHEN updateAllWidgets is called THEN it does not try to update widgets`() {
        try {
            mockkStatic(AppWidgetManager::class)
            val widgetManager: AppWidgetManager = mockk()
            every { AppWidgetManager.getInstance(any()) } returns widgetManager
            val componentNameCaptor = slot<ComponentName>()
            val widgetsToUpdate = intArrayOf()
            every { widgetManager.getAppWidgetIds(capture(componentNameCaptor)) } returns widgetsToUpdate
            val context: Context = mockk(relaxed = true)
            val intentCaptor = slot<Intent>()
            every { context.sendBroadcast(capture(intentCaptor)) } just Runs

            AppSearchWidgetProvider.updateAllWidgets(context, SearchWidgetProvider::class.java)

            verify(exactly = 0) { context.sendBroadcast(any()) }
        } finally {
            unmockkStatic(AppWidgetManager::class)
        }
    }
}
