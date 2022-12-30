package org.mozilla.fenix.onboarding

import app.cash.turbine.test
import mozilla.components.support.test.rule.MainCoroutineRule
import mozilla.components.support.test.rule.runTestOnMain
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mozilla.fenix.onboarding.view.OnboardingNavigationEvent
import org.mozilla.fenix.onboarding.view.OnboardingScreenState
import org.mozilla.fenix.onboarding.view.OnboardingState
import org.mozilla.fenix.onboarding.view.OnboardingViewModel

class OnboardingViewModelTest {

    @get:Rule
    val coroutinesTestRule = MainCoroutineRule()

    private val tested = OnboardingViewModel()

    @Test
    fun `on launch state should update to include isUserSignedIn`() {
        tested.onLaunch(false)

        val expected = OnboardingScreenState.Content(
            isUserSignedIn = false,
            onboardingState = OnboardingState.Welcome,
        )

        assertEquals(expected, tested.state.value)
    }

    @Test
    fun `update state to sign in onPrimaryButtonClick when onboardingState is Welcome`() =
        runTestOnMain {
            tested.onLaunch(false)
            val initial = OnboardingScreenState.Content(
                isUserSignedIn = false,
                onboardingState = OnboardingState.Welcome,
            )
            assertEquals(initial, tested.state.value)

            tested.onPrimaryButtonClick()
            val expected = OnboardingScreenState.Content(
                isUserSignedIn = false,
                onboardingState = OnboardingState.SyncSignIn,
            )

            assertEquals(expected, tested.state.value)
        }

    @Test
    fun `dismiss dialog onPrimaryButtonClick when onboardingState is Welcome and isUserSignedIn is true`() =
        runTestOnMain {
            tested.onLaunch(true)
            val initial = OnboardingScreenState.Content(
                isUserSignedIn = true,
                onboardingState = OnboardingState.Welcome,
            )
            assertEquals(initial, tested.state.value)

            tested.navigationEvent.test {
                tested.onPrimaryButtonClick()
                assertEquals(OnboardingNavigationEvent.DISMISS, awaitItem())
            }
        }

    @Test
    fun `navigate to signIn onPrimaryButtonClick when onboardingState is SyncSignIn`() =
        runTestOnMain {
            tested.onLaunch(false)
            val preTestState = OnboardingScreenState.Content(
                isUserSignedIn = false,
                onboardingState = OnboardingState.SyncSignIn,
            )
            tested.onPrimaryButtonClick()
            assertEquals(preTestState, tested.state.value)

            tested.navigationEvent.test {
                tested.onPrimaryButtonClick()
                assertEquals(OnboardingNavigationEvent.SIGN_IN, awaitItem())
            }
        }

    @Test
    fun `dismiss dialog onSecondaryButtonClick when onboardingState is SyncSignIn`() =
        runTestOnMain {
            tested.onLaunch(false)
            val preTestState = OnboardingScreenState.Content(
                isUserSignedIn = false,
                onboardingState = OnboardingState.SyncSignIn,
            )
            tested.onPrimaryButtonClick()
            assertEquals(preTestState, tested.state.value)

            tested.navigationEvent.test {
                tested.onSecondaryButtonClick()
                assertEquals(OnboardingNavigationEvent.DISMISS, awaitItem())
            }
        }
}
