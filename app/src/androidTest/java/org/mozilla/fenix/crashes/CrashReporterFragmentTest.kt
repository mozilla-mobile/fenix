/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("unused")

package org.mozilla.fenix.crashes

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import mozilla.components.browser.session.Session
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations.initMocks
import org.mozilla.fenix.FenixApplication
import org.mozilla.fenix.R
import org.mozilla.fenix.components.TestComponents
import org.mozilla.fenix.components.WrappedCrashRecoveryUseCase
import org.mozilla.fenix.components.WrappedCrashReporter
import org.mozilla.fenix.components.WrappedRemoveTabUseCase
import org.mozilla.fenix.components.WrappedSessionManager
import org.mozilla.fenix.components.WrappedSessionUseCases
import org.mozilla.fenix.components.WrappedTabsUseCases
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.components.metrics.MetricController
import org.mozilla.fenix.helpers.click

@MediumTest
@RunWith(AndroidJUnit4::class)
class CrashReporterFragmentTest {

    @Mock
    lateinit var metricController: MetricController

    @Mock
    lateinit var sessionManager: WrappedSessionManager

    @Mock
    lateinit var crashReporter: WrappedCrashReporter

    @Mock
    lateinit var crashRecoveryUseCase: WrappedCrashRecoveryUseCase

    @Mock
    lateinit var removeTabUseCase: WrappedRemoveTabUseCase

    @Mock
    lateinit var navController: NavController

    @Before
    fun setUp() {
        initMocks(this)

        getApplicationContext<FenixApplication>().components.let { it as TestComponents }.apply {
            analytics = mock {
                on { metrics } doReturn metricController
                on { wrappedCrashReporter } doReturn crashReporter
            }
            core = mock {
                on { wrappedSessionManager } doReturn sessionManager
            }

            useCases = run {
                val sessionUseCases = mock<WrappedSessionUseCases> {
                    on { crashRecovery } doReturn crashRecoveryUseCase
                }
                val tabsUseCases = mock<WrappedTabsUseCases> {
                    on { removeTab } doReturn removeTabUseCase
                }

                mock {
                    on { wrappedSessionUseCases } doReturn sessionUseCases
                    on { wrappedTabsUseCases } doReturn tabsUseCases
                }
            }
        }
    }

    @Test
    fun defaultScreenState() {
        // When
        launchFragmentScenario()

        // Then
        verifyAll {
            sendCrashCheckbox.shouldBeChecked()
            closeTabButton.shouldBeDisplayed()
            restoreTabButton.shouldBeDisplayed()

            analyticsOpenedTracked()
            stayedOnScreen()
            noCrashReported()
        }
    }

    @Test
    fun noReactionOnInputIfThereIsNoActiveSession() {
        // Given
        whenever(sessionManager.selectedSession) doReturn null

        // When
        launchFragmentScenario()

        closeTabButton.click()
        restoreTabButton.click()

        // Then
        verifyAll {
            closeTabButton.shouldBeDisplayed()
            restoreTabButton.shouldBeDisplayed()

            stayedOnScreen()
            noCrashReported()
        }
    }

    @Test
    fun sendReportCheckboxIgnored() {
        // Given
        whenever(sessionManager.selectedSession) doReturn Session("")

        // When
        launchFragmentScenario()
            .also(::enableCrashReporting)

        sendCrashCheckbox.click() // This checkbox is actually ignored
        closeTabButton.click()

        // Then
        verifyAll {
            crashReported()
        }
    }

    @Test
    fun closeTab_enabledReporting() {
        // Given
        val currentSession = Session("")
        whenever(sessionManager.selectedSession) doReturn currentSession

        // When
        launchFragmentScenario()
            .also(::enableCrashReporting)

        closeTabButton.click()

        // Then
        verifyAll {
            analyticsOpenedTracked()
            analyticsClosedWithReportTracked()
            crashReported()
            tabRemoved(currentSession)
            recoveredFromCrash()
            navigatedHome()
        }
    }

    @Test
    fun closeTab_disabledReporting() {
        // Given
        val currentSession = Session("")
        whenever(sessionManager.selectedSession) doReturn currentSession

        // When
        launchFragmentScenario()
            .also(::disableCrashReporting)

        closeTabButton.click()

        // Then
        verifyAll {
            analyticsOpenedTracked()
            analyticsClosedWithoutReportTracked()
            noCrashReported()
            tabRemoved(currentSession)
            recoveredFromCrash()
            navigatedHome()
        }
    }

    @Test
    fun restoreTab_enabledReporting() {
        // Given
        val currentSession = Session("")
        whenever(sessionManager.selectedSession) doReturn currentSession

        // When
        launchFragmentScenario()
            .also(::enableCrashReporting)

        restoreTabButton.click()

        // Then
        verifyAll {
            analyticsOpenedTracked()
            analyticsClosedWithReportTracked()
            crashReported()
            tabStayed()
            recoveredFromCrash()
            navigatedBack()
        }
    }

    @Test
    fun restoreTab_disabledReporting() {
        // Given
        val currentSession = Session("")
        whenever(sessionManager.selectedSession) doReturn currentSession

        // When
        launchFragmentScenario()
            .also(::disableCrashReporting)

        restoreTabButton.click()

        // Then
        verifyAll {
            analyticsOpenedTracked()
            analyticsClosedWithoutReportTracked()
            noCrashReported()
            tabStayed()
            recoveredFromCrash()
            navigatedBack()
        }
    }

    private fun launchFragmentScenario() = launchFragmentScenario(navController = navController)

    // Verifications

    private fun VerificationScope.analyticsOpenedTracked() =
        verify(metricController).track(Event.CrashReporterOpened)

    private fun VerificationScope.analyticsClosedWithReportTracked() =
        verify(metricController).track(Event.CrashReporterClosed(crashSubmitted = true))

    private fun VerificationScope.analyticsClosedWithoutReportTracked() =
        verify(metricController).track(Event.CrashReporterClosed(crashSubmitted = false))

    private fun VerificationScope.crashReported() = verify(crashReporter).submitReport(any())
    private fun VerificationScope.noCrashReported() = verify(crashReporter, never()).submitReport(any())
    private fun VerificationScope.tabStayed() = verifyZeroInteractions(removeTabUseCase)
    private fun VerificationScope.tabRemoved(session: Session) = verify(removeTabUseCase).invoke(session)
    private fun VerificationScope.recoveredFromCrash() = verify(crashRecoveryUseCase).invoke()
    private fun VerificationScope.stayedOnScreen() = verifyZeroInteractions(navController)
    private fun VerificationScope.navigatedBack() = navController.popBackStack()
    private fun VerificationScope.navigatedHome() = navController.popBackStack(R.id.browserFragment, true)
}

private fun launchFragmentScenario(
    args: CrashReporterFragmentArgs = createFragmentArguments(Error()),
    navController: NavController
) = launchFragmentInContainer<CrashReporterFragment>(args.toBundle(), R.style.NormalTheme).apply {
    onFragment { fragment ->
        Navigation.setViewNavController(fragment.view!!, navController)
    }
}

private fun enableCrashReporting(scenario: FragmentScenario<CrashReporterFragment>) {
    scenario.onFragment { fragment ->
        fragment.crashReportingEnabled = { true }
    }
}

private fun disableCrashReporting(scenario: FragmentScenario<CrashReporterFragment>) {
    scenario.onFragment { fragment ->
        fragment.crashReportingEnabled = { false }
    }
}

private fun createFragmentArguments(throwable: Throwable) = CrashReporterFragmentArgs(
    Intent().also { intent ->
        intent.putExtra("mozilla.components.lib.crash.CRASH", Bundle().also { bundle ->
            bundle.putSerializable("exception", throwable)
        })
    }
)

// View

private val sendCrashCheckbox get() = onView(withId(R.id.send_crash_checkbox))
private val closeTabButton get() = onView(withId(R.id.close_tab_button))
private val restoreTabButton get() = onView(withId(R.id.restore_tab_button))

// Verification

private fun verifyAll(block: VerificationScope.() -> Unit) = VerificationScope.block()

private object VerificationScope {

    fun ViewInteraction.shouldBeChecked() = check(matches(isChecked()))!!
    fun ViewInteraction.shouldBeDisplayed() = check(matches(isDisplayed()))!!
}
