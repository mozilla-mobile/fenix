/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.onboarding.view

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mozilla.telemetry.glean.private.NoExtras
import org.mozilla.fenix.GleanMetrics.Onboarding
import org.mozilla.fenix.R

/**
 * ViewModel holding the screen state for [OnboardingScreen].
 */
class OnboardingViewModel : ViewModel() {

    private val _state = MutableStateFlow<OnboardingScreenState>(OnboardingScreenState.Initial)
    internal val state: StateFlow<OnboardingScreenState> = _state
    private val _navigationEvent = MutableSharedFlow<OnboardingNavigationEvent>()
    internal val navigationEvent: SharedFlow<OnboardingNavigationEvent> = _navigationEvent

    /**
     * This should be called onLaunch of [OnboardingScreen] to update to the Content State.
     */
    fun onLaunch(isUserSignedIn: Boolean) {
        if (_state.value is OnboardingScreenState.Initial) {
            _state.value = OnboardingScreenState.Content(
                onboardingState = OnboardingState.Welcome,
                isUserSignedIn = isUserSignedIn,
            )
        }
    }

    /**
     * Click handler for primary button.
     */
    fun onPrimaryButtonClick() {
        viewModelScope.launch {
            stateIsContent {
                when (it.onboardingState) {
                    OnboardingState.Welcome -> {
                        Onboarding.welcomeGetStartedClicked.record(NoExtras())
                        if (it.isUserSignedIn) {
                            _navigationEvent.emit(OnboardingNavigationEvent.DISMISS)
                        } else {
                            _state.value = it.copy(onboardingState = OnboardingState.SyncSignIn)
                        }
                    }
                    OnboardingState.SyncSignIn -> {
                        Onboarding.syncSignInClicked.record(NoExtras())
                        _navigationEvent.emit(OnboardingNavigationEvent.SIGN_IN)
                    }
                }
            }
        }
    }

    /**
     * Click handler for secondary button.
     */
    fun onSecondaryButtonClick() {
        viewModelScope.launch {
            stateIsContent {
                when (it.onboardingState) {
                    OnboardingState.Welcome -> {
                        // nothing as welcome doesn't have a secondary button
                    }
                    OnboardingState.SyncSignIn -> {
                        Onboarding.syncSkipClicked.record(NoExtras())
                        _navigationEvent.emit(OnboardingNavigationEvent.DISMISS)
                    }
                }
            }
        }
    }

    private suspend fun stateIsContent(block: suspend (OnboardingScreenState.Content) -> Unit) {
        if (_state.value is OnboardingScreenState.Content) {
            block(_state.value as OnboardingScreenState.Content)
        }
    }
}

/**
 * Enum that represents the onboarding page that is displayed.
 */
enum class OnboardingState {
    Welcome,
    SyncSignIn,
}

/**
 * State of the Onboarding Screen.
 */
sealed class OnboardingScreenState {
    /**
     * The Initial state of the screen when the content isn't loaded.
     * Can be used to show a progress bar if required.
     */
    object Initial : OnboardingScreenState()

    /**
     * The Content state of the screen.
     * @param onboardingState the page that is selected.
     * @param isUserSignedIn boolean that signifies if the user has signed in.
     * pageUiState is a derived property that contains the page data based on [onboardingState].
     */
    data class Content(
        val onboardingState: OnboardingState,
        val isUserSignedIn: Boolean,
    ) : OnboardingScreenState() {
        val pageUiState: OnboardingPageUiState = when (onboardingState) {
            OnboardingState.Welcome -> OnboardingPageUiState(
                image = R.drawable.ic_onboarding_welcome,
                title = R.string.onboarding_home_welcome_title_2,
                description = R.string.onboarding_home_welcome_description,
                primaryButtonText = R.string.onboarding_home_get_started_button,
                recordImpressionEvent = { Onboarding.welcomeCardImpression.record(NoExtras()) },
            )
            OnboardingState.SyncSignIn -> OnboardingPageUiState(
                image = R.drawable.ic_onboarding_sync,
                title = R.string.onboarding_home_sync_title_3,
                description = R.string.onboarding_home_sync_description,
                primaryButtonText = R.string.onboarding_home_sign_in_button,
                secondaryButtonText = R.string.onboarding_home_skip_button,
                recordImpressionEvent = { Onboarding.syncCardImpression.record(NoExtras()) },
            )
        }
    }
}

/**
 * Onboarding Page data state.
 */
data class OnboardingPageUiState(
    @DrawableRes val image: Int,
    @StringRes val title: Int,
    @StringRes val description: Int,
    @StringRes val primaryButtonText: Int,
    @StringRes val secondaryButtonText: Int? = null,
    val recordImpressionEvent: () -> Unit,
)

/**
 * Navigation events based on user actions.
 */
enum class OnboardingNavigationEvent {
    DISMISS,
    SIGN_IN,
}
