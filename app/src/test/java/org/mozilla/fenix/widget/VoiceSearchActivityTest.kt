/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.widget

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH
import android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL
import android.speech.RecognizerIntent.EXTRA_RESULTS
import android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.FenixApplication
import org.mozilla.fenix.HomeActivity.Companion.OPEN_TO_BROWSER_AND_LOAD
import org.mozilla.fenix.IntentReceiverActivity
import org.mozilla.fenix.helpers.FenixRobolectricTestRunner
import org.mozilla.fenix.widget.VoiceSearchActivity.Companion.PREVIOUS_INTENT
import org.mozilla.fenix.widget.VoiceSearchActivity.Companion.SPEECH_PROCESSING
import org.mozilla.fenix.widget.VoiceSearchActivity.Companion.SPEECH_REQUEST_CODE
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.shadows.ShadowActivity

@ExperimentalCoroutinesApi
@RunWith(FenixRobolectricTestRunner::class)
class VoiceSearchActivityTest {

    private lateinit var controller: ActivityController<VoiceSearchActivity>
    private lateinit var activity: VoiceSearchActivity
    private lateinit var shadow: ShadowActivity

    @Before
    fun setup() {
        val intent = Intent()
        intent.putExtra(SPEECH_PROCESSING, true)

        controller = Robolectric.buildActivity(VoiceSearchActivity::class.java, intent)
        activity = controller.get()
        shadow = shadowOf(activity)
    }

    private fun allowVoiceIntentToResolveActivity() {
        val context = ApplicationProvider.getApplicationContext<FenixApplication>()
        val shadowPackageManager = shadowOf(context.packageManager)
        val component = ComponentName("com.test", "Test")
        shadowPackageManager.addActivityIfNotPresent(component)
        shadowPackageManager.addIntentFilterForActivity(
            component,
            IntentFilter(ACTION_RECOGNIZE_SPEECH).apply { addCategory(Intent.CATEGORY_DEFAULT) })
    }

    @Test
    fun `process intent with speech processing set to true`() {
        allowVoiceIntentToResolveActivity()
        controller.create()

        val intentForResult = shadow.peekNextStartedActivityForResult()
        assertEquals(SPEECH_REQUEST_CODE, intentForResult.requestCode)
        assertEquals(ACTION_RECOGNIZE_SPEECH, intentForResult.intent.action)
        assertEquals(
            LANGUAGE_MODEL_FREE_FORM,
            intentForResult.intent.getStringExtra(EXTRA_LANGUAGE_MODEL)
        )
    }

    @Test
    fun `process intent with speech processing set to false`() {
        allowVoiceIntentToResolveActivity()
        val intent = Intent()
        intent.putExtra(SPEECH_PROCESSING, false)

        val controller = Robolectric.buildActivity(VoiceSearchActivity::class.java, intent)
        val activity = controller.get()

        controller.create()

        assertTrue(activity.isFinishing)
    }

    @Test
    fun `process null intent`() {
        allowVoiceIntentToResolveActivity()
        val controller = Robolectric.buildActivity(VoiceSearchActivity::class.java, null)
        val activity = controller.get()

        controller.create()

        assertTrue(activity.isFinishing)
    }

    @Test
    fun `save previous intent to instance state`() {
        allowVoiceIntentToResolveActivity()
        val previousIntent = Intent().apply {
            putExtra(SPEECH_PROCESSING, true)
        }
        val savedInstanceState = Bundle().apply {
            putParcelable(PREVIOUS_INTENT, previousIntent)
        }
        val outState = Bundle()

        controller.create(savedInstanceState)
        controller.saveInstanceState(outState)

        assertEquals(previousIntent, outState.getParcelable<Intent>(PREVIOUS_INTENT))
    }

    @Test
    fun `process intent with speech processing in previous intent set to true`() {
        allowVoiceIntentToResolveActivity()
        val savedInstanceState = Bundle()
        val previousIntent = Intent().apply {
            putExtra(SPEECH_PROCESSING, true)
        }
        savedInstanceState.putParcelable(PREVIOUS_INTENT, previousIntent)

        controller.create(savedInstanceState)

        assertFalse(activity.isFinishing)
        assertNull(shadow.peekNextStartedActivityForResult())
    }

    @Test
    fun `handle speech result`() {
        allowVoiceIntentToResolveActivity()
        controller.create()

        val resultIntent = Intent().apply {
            putStringArrayListExtra(EXTRA_RESULTS, arrayListOf("hello world"))
        }
        shadow.receiveResult(
            shadow.peekNextStartedActivityForResult().intent,
            RESULT_OK,
            resultIntent
        )

        val browserIntent = shadow.peekNextStartedActivity()

        assertTrue(activity.isFinishing)
        assertEquals(
            ComponentName(activity, IntentReceiverActivity::class.java),
            browserIntent.component
        )
        assertEquals("hello world", browserIntent.getStringExtra(SPEECH_PROCESSING))
        assertTrue(browserIntent.getBooleanExtra(OPEN_TO_BROWSER_AND_LOAD, false))
    }

    @Test
    fun `handle invalid result code`() {
        allowVoiceIntentToResolveActivity()
        controller.create()

        val resultIntent = Intent()
        shadow.receiveResult(
            shadow.peekNextStartedActivityForResult().intent,
            Activity.RESULT_CANCELED,
            resultIntent
        )

        assertTrue(activity.isFinishing)
    }

    @Test
    fun `handle no activity able to resolve voice intent`() {
        controller.create()
        assertTrue(activity.isFinishing)
    }
}
