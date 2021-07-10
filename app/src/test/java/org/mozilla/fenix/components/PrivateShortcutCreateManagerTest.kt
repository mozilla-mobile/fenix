package org.mozilla.fenix.components

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.FenixRobolectricTestRunner
import org.mozilla.fenix.home.intent.StartSearchIntentProcessor

@RunWith(FenixRobolectricTestRunner::class)
class PrivateShortcutCreateManagerTest {

    @Test
    fun `GIVEN shortcut pinning is not supported WHEN createPrivateShortcut is called THEN do not create a pinned shortcut`() {
        val privateShortcutCreateManager = spyk<PrivateShortcutCreateManager>()
        mockkStatic(ShortcutManagerCompat::class)

        every { ShortcutManagerCompat.isRequestPinShortcutSupported(testContext) } returns false

        privateShortcutCreateManager.createPrivateShortcut(testContext)

        verify(exactly = 0) { ShortcutManagerCompat.requestPinShortcut(testContext, any(), any()) }
    }

    @Test
    fun `GIVEN shortcut pinning is supported WHEN createPrivateShortcut is called THEN create a pinned shortcut`() {
        val shortcut = slot<ShortcutInfoCompat>()
        val intentSender = slot<IntentSender>()
        val intent = slot<Intent>()

        val privateShortcutCreateManager = spyk<PrivateShortcutCreateManager>()
        mockkStatic(ShortcutManagerCompat::class)
        mockkStatic(PendingIntent::class)

        every { ShortcutManagerCompat.isRequestPinShortcutSupported(testContext) } returns true

        privateShortcutCreateManager.createPrivateShortcut(testContext)

        verify { PendingIntent.getActivity(testContext, 0, capture(intent), PendingIntent.FLAG_UPDATE_CURRENT) }
        verify { ShortcutManagerCompat.requestPinShortcut(testContext, capture(shortcut), capture(intentSender)) }
        `assert shortcutInfoCompat is build correctly`(shortcut, testContext)
        `assert homeScreenIntent is built correctly`(intent)
    }

    private fun `assert shortcutInfoCompat is build correctly`(shortcutInfoCompat: CapturingSlot<ShortcutInfoCompat>, context: Context) {
        shortcutInfoCompat.apply {
            assertEquals(context.getString(R.string.app_name_private_5, context.getString(R.string.app_name)), captured.shortLabel)
            assertEquals(context.getString(R.string.app_name_private_5, context.getString(R.string.app_name)), captured.longLabel)
            assertEquals(R.mipmap.ic_launcher_private_round, captured.icon.resId)
            `assert homeActivity intent is built correctly`(captured.intent)
        }
    }

    private fun `assert homeActivity intent is built correctly`(intent: Intent) {
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK, intent.flags)
        assertEquals(HomeActivity::class.qualifiedName, intent.component?.className)
        assertEquals(true, intent.extras?.getBoolean(HomeActivity.PRIVATE_BROWSING_MODE))
        assertEquals(StartSearchIntentProcessor.PRIVATE_BROWSING_PINNED_SHORTCUT, intent.extras?.getString(HomeActivity.OPEN_TO_SEARCH))
    }

    private fun `assert homeScreenIntent is built correctly`(intent: CapturingSlot<Intent>) {
        intent.apply {
            assertEquals(Intent.ACTION_MAIN, captured.action)
            assert(captured.categories.contains(Intent.CATEGORY_HOME))
            assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK, captured.flags)
        }
    }
}
