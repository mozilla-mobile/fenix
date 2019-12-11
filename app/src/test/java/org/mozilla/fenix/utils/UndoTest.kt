package org.mozilla.fenix.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.TestApplication
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.mockkStatic
import io.mockk.verify
import io.mockk.every
import io.mockk.Runs
import io.mockk.just
import io.mockk.confirmVerified
import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.mozilla.fenix.components.FenixSnackbar
import android.app.AlertDialog
import org.mozilla.fenix.R
import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.navigation.fragment.NavHostFragment
import mozilla.components.support.test.robolectric.testContext
import kotlin.coroutines.CoroutineContext

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)

class UndoTest {

    /*@Test
    fun `GIVEN allow undo WHEN alert dialog undo created THEN send back a correctly-populated alert`() {
        val coroutineScope: CoroutineScope = mockk(relaxed=true)
        val mockView: View = mockk(relaxed=true)
        val message = "Message"
        val undoActionTitle = "Undo"
        mockkStatic(CoroutineContext::class)

        coroutineScope.allowUndo().showUndoDialog()

    }*/

    @Test
    fun `GIVEN toggled disability pref WHEN checking a11y prefs THEN send back the correct pref`() {
        val mockView: View = mockk(relaxed=true)
        val mockAm: AccessibilityManager = mockk(relaxed = true)

        every { mockView.context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager } returns mockAm
        every { mockAm.isTouchExplorationEnabled() } returns true

        assertEquals(true, touchExplorationEnabled(mockView))
    }

    @Test
    fun `GIVEN toggled false disability pref WHEN checking a11y prefs THEN send back the correct pref`() {
        val mockView: View = mockk(relaxed=true)
        val mockAm: AccessibilityManager = mockk(relaxed = true)

        every { mockView.context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager } returns mockAm
        every { mockAm.isTouchExplorationEnabled() } returns false

        assertEquals(false, touchExplorationEnabled(mockView))


    }
}