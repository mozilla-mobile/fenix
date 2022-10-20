package org.mozilla.fenix.utils

import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import mozilla.components.support.test.robolectric.testContext
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.helpers.FenixRobolectricTestRunner

@RunWith(FenixRobolectricTestRunner::class)
class UndoTest {
    private lateinit var fragment: Fragment
    private lateinit var view: ViewGroup

    @Before
    fun setup() {
        fragment = mockk(relaxed = true)
        view = mockk(relaxed = true)
    }

    private fun testUndoCloseTabSnackBar(
        isPrivate: Boolean,
        multiple: Boolean,
        @StringRes messageId: Int,
    ) {
        // setup lifecycle
        mockkStatic(LifecycleOwner::lifecycleScope)
        val lifecycleScope: LifecycleCoroutineScope = mockk(relaxed = true)
        every { any<LifecycleOwner>().lifecycleScope } returns lifecycleScope

        // setup allowUndo
        mockkStatic(CoroutineScope::allowUndo)
        every {
            any<CoroutineScope>().allowUndo(
                view = any(),
                message = any(),
                undoActionTitle = any(),
                onCancel = captureLambda(),
                operation = any(),
                anchorView = any(),
                elevation = any(),
                paddedForBottomToolbar = any(),
            )
        } answers {
            // simulate cancellation
            runBlocking {
                lambda<suspend () -> Unit>().captured.invoke()
            }
        }

        // setup fragment and context
        every { fragment.requireContext() } returns testContext // needed for getString()
        every { testContext.components.useCases.tabsUseCases.undo.invoke() } just Runs

        // setup show
        val onCancel = mockk<() -> Unit>()
        val anchorView: ViewGroup = mockk()
        val elevation: Float = mockk(relaxed = true)
        val paddedForBottomToolbar: Boolean = mockk(relaxed = true)
        every { onCancel.invoke() } just Runs

        UndoCloseTabSnackBar.show(
            fragment = fragment,
            isPrivate = isPrivate,
            multiple = multiple,
            onCancel = onCancel,
            view = view,
            anchorView = anchorView,
            elevation = elevation,
            paddedForBottomToolbar = paddedForBottomToolbar,
        )

        // verify that the correct allowUndo call is made, including the proper strings
        verify {
            lifecycleScope.allowUndo(
                view = view,
                message = testContext.getString(messageId),
                undoActionTitle = testContext.getString(R.string.snackbar_deleted_undo),
                onCancel = any(),
                operation = any(),
                anchorView = anchorView,
                elevation = elevation,
                paddedForBottomToolbar = paddedForBottomToolbar,
            )
        }
        // verify that the following were called on cancellation
        verifyOrder {
            testContext.components.useCases.tabsUseCases.undo.invoke()
            onCancel.invoke()
        }
    }

    @Test
    fun `GIVEN single normal tab closed WHEN UndoCloseTabSnackBar#show is called THEN appropriate allowUndo call is made`() {
        testUndoCloseTabSnackBar(
            isPrivate = false,
            multiple = false,
            messageId = R.string.snackbar_tab_closed,
        )
    }

    @Test
    fun `GIVEN single private tab closed WHEN UndoCloseTabSnackBar#show is called THEN appropriate allowUndo call is made`() {
        testUndoCloseTabSnackBar(
            isPrivate = true,
            multiple = false,
            messageId = R.string.snackbar_private_tab_closed,
        )
    }

    @Test
    fun `GIVEN multiple normal tabs closed WHEN UndoCloseTabSnackBar#show is called THEN appropriate allowUndo call is made`() {
        testUndoCloseTabSnackBar(
            isPrivate = false,
            multiple = true,
            messageId = R.string.snackbar_tabs_closed,
        )
    }

    @Test
    fun `GIVEN multiple private tabs closed WHEN UndoCloseTabSnackBar#show is called THEN appropriate allowUndo call is made`() {
        testUndoCloseTabSnackBar(
            isPrivate = true,
            multiple = true,
            messageId = R.string.snackbar_private_tabs_closed,
        )
    }
}
