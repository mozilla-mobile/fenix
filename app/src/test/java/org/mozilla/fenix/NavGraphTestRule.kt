package org.mozilla.fenix

import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.mockkStatic
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.mozilla.fenix.ext.navigateBlockingForAsyncNavGraph
import org.mozilla.fenix.perf.NavGraphProvider


class NavGraphTestRule(private val navController: NavController) : TestWatcher() {

    override fun starting(description: Description?) {
        mockkStatic("org.mozilla.fenix.ext.NavControllerKt")
        every { navController.navigateBlockingForAsyncNavGraph(any() as NavDirections, any<NavOptions>()) } returns Unit

        mockkObject(NavGraphProvider)
        every { NavGraphProvider.blockForNavGraphInflation(any()) } returns Unit

    }
}