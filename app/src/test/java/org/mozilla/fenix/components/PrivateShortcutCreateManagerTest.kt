package org.mozilla.fenix.components

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.verify
import kotlin.random.Random
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.FenixRobolectricTestRunner
import org.mozilla.fenix.home.intent.StartSearchIntentProcessor


@RunWith(FenixRobolectricTestRunner::class)
class PrivateShortcutCreateManagerTest {

    @Test
    fun `GIVEN shortcut pinning is supported WHEN createPrivateShortcut is called THEN create a pinned shortcut`() {
        val context: Context = ApplicationProvider.getApplicationContext()

        val privateShortcutCreateManager = spyk(PrivateShortcutCreateManager)

        val intent = privateShortcutCreateManager.getHomeScreenActivityIntent(context)
        `assert HomeActivityIntent correctly built`(intent)

        val shortcut = privateShortcutCreateManager.getShortcut(context, intent)
        `assert shortcut is correctly built`(shortcut, intent)

        val intentSender: IntentSender = spyk(privateShortcutCreateManager.getIntentSender(context))

        /**
         *  Robolectric returns null for binder ue to which
         *  intentSender.asBinder() calls in hashCode() fails with NullPointerException
         */

        val intentSenderHashCode = Random.nextInt()
        every { intentSender.hashCode() } returns intentSenderHashCode

        every { privateShortcutCreateManager.getIntentSender(context) } returns intentSender
        every { privateShortcutCreateManager.getHomeScreenActivityIntent(context) } returns intent
        every { privateShortcutCreateManager.getShortcut(context, intent) } returns shortcut

        mockkStatic(ShortcutManagerCompat::class)

        privateShortcutCreateManager.createPrivateShortcut(context)

        verify { ShortcutManagerCompat.isRequestPinShortcutSupported(context) }
        assert(ShortcutManagerCompat.isRequestPinShortcutSupported(context))

        verify { ShortcutManagerCompat.requestPinShortcut(context, shortcut, intentSender) }

    }

    private fun `assert HomeActivityIntent correctly built`(intent: Intent) {

        assert(intent.action == Intent.ACTION_VIEW)
        assert(intent.flags == Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

        val bundle = intent.extras

        assertNotNull(bundle)

        assert(bundle!!.getBoolean(HomeActivity.PRIVATE_BROWSING_MODE))
        assert(bundle.getString(HomeActivity.OPEN_TO_SEARCH) == StartSearchIntentProcessor.PRIVATE_BROWSING_PINNED_SHORTCUT)

    }

    private fun `assert shortcut is correctly built`(
        shortcut: ShortcutInfoCompat,
        intent: Intent
    ) {

        assert(shortcut.shortLabel == "Private Firefox Preview")
        assert(shortcut.longLabel == "Private Firefox Preview")
        assert(shortcut.icon.resId == R.mipmap.ic_launcher_private_round)
        assert(shortcut.intent == intent)

    }

}
