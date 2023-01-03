/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.onboarding.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.insets.navigationBarsPadding
import com.google.accompanist.insets.statusBarsPadding
import mozilla.telemetry.glean.private.NoExtras
import org.mozilla.fenix.GleanMetrics.Onboarding
import org.mozilla.fenix.R
import org.mozilla.fenix.theme.FirefoxTheme

/**
 * Enum that represents the onboarding page that is displayed.
 */
private enum class UpgradeOnboardingState {
    Welcome,
    SyncSignIn,
}

/**
 * A screen for displaying a welcome and sync sign in onboarding.
 *
 * @param isUserSignedIn Whether or not the user is signed into their Firefox Sync account.
 * @param onDismiss Invoked when the user clicks on the close or "Skip" button.
 * @param onSignInButtonClick Invoked when the user clicks on the "Sign In" button
 */
@Composable
fun UpgradeOnboardingScreen(
    isUserSignedIn: Boolean,
    onDismiss: () -> Unit,
    onSignInButtonClick: () -> Unit,
) {
    var onboardingState by remember { mutableStateOf(UpgradeOnboardingState.Welcome) }

    OnboardingContent(
        onboardingState = onboardingState,
        onCloseClick = {
            when (onboardingState) {
                UpgradeOnboardingState.Welcome -> Onboarding.welcomeCloseClicked.record(NoExtras())
                UpgradeOnboardingState.SyncSignIn -> Onboarding.syncCloseClicked.record(NoExtras())
            }
            onDismiss()
        },
        onPrimaryButtonClick = {
            when (onboardingState) {
                UpgradeOnboardingState.Welcome -> {
                    Onboarding.welcomeGetStartedClicked.record(NoExtras())
                    if (isUserSignedIn) {
                        onDismiss()
                    } else {
                        onboardingState = UpgradeOnboardingState.SyncSignIn
                    }
                }
                UpgradeOnboardingState.SyncSignIn -> {
                    Onboarding.syncSignInClicked.record(NoExtras())
                    onSignInButtonClick()
                }
            }
        },
        onSecondaryButtonClick = {
            when (onboardingState) {
                UpgradeOnboardingState.Welcome -> {
                    // nothing as welcome doesn't have a secondary button
                }
                UpgradeOnboardingState.SyncSignIn -> {
                    Onboarding.syncSkipClicked.record(NoExtras())
                    onDismiss()
                }
            }
        },
    )
}

@Composable
private fun OnboardingContent(
    onboardingState: UpgradeOnboardingState,
    onCloseClick: () -> Unit,
    onPrimaryButtonClick: () -> Unit,
    onSecondaryButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(FirefoxTheme.colors.layer1)
            .fillMaxSize()
            .padding(bottom = 32.dp)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconButton(
            onClick = onCloseClick,
            modifier = Modifier.align(Alignment.End),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.mozac_ic_close),
                contentDescription = stringResource(R.string.onboarding_home_content_description_close_button),
                tint = FirefoxTheme.colors.iconPrimary,
            )
        }

        OnboardingPage(
            onboardingPageState = when (onboardingState) {
                UpgradeOnboardingState.Welcome -> OnboardingPageState(
                    image = R.drawable.ic_onboarding_welcome,
                    title = R.string.onboarding_home_welcome_title_2,
                    description = R.string.onboarding_home_welcome_description,
                    primaryButtonText = R.string.onboarding_home_get_started_button,
                    onRecordImpressionEvent = { Onboarding.welcomeCardImpression.record(NoExtras()) },
                )
                UpgradeOnboardingState.SyncSignIn -> OnboardingPageState(
                    image = R.drawable.ic_onboarding_sync,
                    title = R.string.onboarding_home_sync_title_3,
                    description = R.string.onboarding_home_sync_description,
                    primaryButtonText = R.string.onboarding_home_sign_in_button,
                    secondaryButtonText = R.string.onboarding_home_skip_button,
                    onRecordImpressionEvent = { Onboarding.syncCardImpression.record(NoExtras()) },
                )
            },
            onPrimaryButtonClick = onPrimaryButtonClick,
            onSecondaryButtonClick = onSecondaryButtonClick,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        )

        Spacer(modifier = Modifier.height(24.dp))

        Indicators(
            onboardingState = onboardingState,
        )
    }
}

@Composable
private fun Indicators(onboardingState: UpgradeOnboardingState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        Indicator(
            color = if (onboardingState == UpgradeOnboardingState.Welcome) {
                FirefoxTheme.colors.indicatorActive
            } else {
                FirefoxTheme.colors.indicatorInactive
            },
        )

        Spacer(modifier = Modifier.width(8.dp))

        Indicator(
            color = if (onboardingState == UpgradeOnboardingState.SyncSignIn) {
                FirefoxTheme.colors.indicatorActive
            } else {
                FirefoxTheme.colors.indicatorInactive
            },
        )
    }
}

@Composable
private fun Indicator(color: Color) {
    Box(
        modifier = Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(color),
    )
}

@Composable
@Preview
private fun UpgradeOnboardingScreenPreview() {
    FirefoxTheme {
        UpgradeOnboardingScreen(
            isUserSignedIn = false,
            onDismiss = {},
            onSignInButtonClick = {},
        )
    }
}
