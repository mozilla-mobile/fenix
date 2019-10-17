/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ext

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.TestApplication
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_SEND
import android.content.Intent.EXTRA_SUBJECT
import android.content.Intent.EXTRA_TEXT
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.DimenRes
import androidx.annotation.StringRes
import androidx.fragment.app.FragmentActivity
import mozilla.components.browser.search.SearchEngineManager
import mozilla.components.support.base.log.Log
import mozilla.components.support.base.log.Log.Priority.WARN
import mozilla.components.support.ktx.android.content.getColorFromAttr
import org.jetbrains.anko.dimen
import org.jetbrains.anko.px2dip
import org.mozilla.fenix.BuildConfig
import org.mozilla.fenix.FenixApplication
import org.mozilla.fenix.R
import org.mozilla.fenix.components.Components
import org.mozilla.fenix.components.metrics.MetricController
import org.mozilla.fenix.utils.Settings
import mozilla.components.support.test.robolectric.testContext

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class ContextTest () {

    @Test
    fun `GIVEN context WHEN seeking application of context THEN send back application context`() {
        val expectedAppValue = testContext.getApplicationContext() as FenixApplication
        val resultAppValue = testContext.application
        assertEquals(expectedAppValue, resultAppValue)
    }

    @Test
    fun `GIVEN context WHEN requiring components THEN send back application components`() {
        val expectedComponentsValue = (testContext.getApplicationContext() as FenixApplication).components
        val resultComponentsValue = testContext.components
        assertEquals(expectedComponentsValue, resultComponentsValue)
    }

    @Test
    fun `GIVEN context WHEN getting metrics controller THEN send back metrics`() {
        val expectedMetricsValue = (testContext.getApplicationContext() as FenixApplication).components.analytics.metrics
        val resultMetricsValue = testContext.metrics
        assertEquals(expectedMetricsValue, resultMetricsValue)
    }

    @Test
    fun `GIVEN context WHEN make it an activity`() {
        val expectedActivity = testContext as? Activity
        val actualResult = testContext.asActivity()
        assertEquals(expectedActivity, actualResult)
    }

    @Test
    fun `GIVEN context WHEN make it a fragment activity`() {
        val expectedActivity = testContext as? FragmentActivity
        val actualResult = testContext.asFragmentActivity()
        assertEquals(expectedActivity, actualResult)
    }

    @Test
    fun `GIVEN context WHEN given a preference key THEN send back the right string`() {
        val comparisonStr = testContext.getString(R.string.app_name_private)
        val stringId = R.string.app_name_private.toInt()
        val actualStr = testContext.getPreferenceKey(stringId)
        assertEquals(comparisonStr, actualStr)
    }
}
