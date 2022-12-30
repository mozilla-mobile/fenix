/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.onboarding.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.insets.navigationBarsPadding
import com.google.accompanist.insets.statusBarsPadding
import kotlinx.coroutines.flow.collectLatest
import mozilla.telemetry.glean.private.NoExtras
import org.mozilla.fenix.GleanMetrics.Onboarding
import org.mozilla.fenix.R
import org.mozilla.fenix.compose.button.PrimaryButton
import org.mozilla.fenix.compose.button.SecondaryButton
import org.mozilla.fenix.theme.FirefoxTheme

/**
 * A screen for displaying a welcome and sync sign in onboarding.
 *
 * @param isUserSignedIn Whether or not the user is signed into their Firefox Sync account.
 * @param onDismiss Invoked when the user clicks on the close or "Skip" button.
 * @param onSignInButtonClick Invoked when the user clicks on the "Sign In" button
 * @param viewModel [OnboardingViewModel] holding the screen state.
 */
@Composable
fun OnboardingScreen(
    isUserSignedIn: Boolean,
    onDismiss: () -> Unit,
    onSignInButtonClick: () -> Unit,
    viewModel: OnboardingViewModel,
) {
    val onboardingState by viewModel.state.collectAsState()

    // Only show Content when the state is Content
    if (onboardingState is OnboardingScreenState.Content) {
        OnboardingContent(
            content = onboardingState as OnboardingScreenState.Content,
            onDismiss = onDismiss,
            onPrimaryButtonClick = viewModel::onPrimaryButtonClick,
            onSecondaryButtonClick = viewModel::onSecondaryButtonClick,
        )
    }

    LaunchedEffect(Unit) {
        viewModel.onLaunch(isUserSignedIn)

        viewModel.navigationEvent.collectLatest {
            when (it) {
                OnboardingNavigationEvent.DISMISS -> onDismiss()
                OnboardingNavigationEvent.SIGN_IN -> onSignInButtonClick()
            }
        }
    }
}

@Composable
private fun OnboardingContent(
    content: OnboardingScreenState.Content,
    onDismiss: () -> Unit,
    onPrimaryButtonClick: () -> Unit,
    onSecondaryButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val boxWithConstraintsScope = this
        Column(
            modifier = modifier
                .background(FirefoxTheme.colors.layer1)
                .fillMaxSize()
                .padding(bottom = FirefoxTheme.dimens.grid_4)
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            IconButton(
                onClick = {
                    when (content.onboardingState) {
                        OnboardingState.Welcome -> Onboarding.welcomeCloseClicked.record(NoExtras())
                        OnboardingState.SyncSignIn -> Onboarding.syncCloseClicked.record(NoExtras())
                    }
                    onDismiss()
                },
                modifier = Modifier.align(Alignment.End),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.mozac_ic_close),
                    contentDescription = stringResource(R.string.onboarding_home_content_description_close_button),
                    tint = FirefoxTheme.colors.iconPrimary,
                )
            }

            OnboardingPage(
                content.pageUiState,
                onPrimaryButtonClick = onPrimaryButtonClick,
                onSecondaryButtonClick = onSecondaryButtonClick,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = FirefoxTheme.dimens.grid_2)
                    .verticalScroll(rememberScrollState()),
                imageModifier = Modifier
                    .height(boxWithConstraintsScope.maxHeight.times(IMAGE_HEIGHT_RATIO)),
            )

            Spacer(modifier = Modifier.height(FirefoxTheme.dimens.grid_2))

            Indicators(
                onboardingState = content.onboardingState,
            )
        }
    }
}

@Composable
private fun OnboardingPage(
    onboardingPageUiState: OnboardingPageUiState,
    onPrimaryButtonClick: () -> Unit,
    onSecondaryButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
    imageModifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        // Since the vertical arrangement is SpaceEvenly, the text and buttons are wrapped
        // their own containing Composables so the OuterColumn only has 3 children with
        // space in between and around them
        // Item 1
        Image(
            painter = painterResource(id = onboardingPageUiState.image),
            contentDescription = null,
            modifier = imageModifier,
        )

        // Item 2
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(id = onboardingPageUiState.title),
                color = FirefoxTheme.colors.textPrimary,
                textAlign = TextAlign.Center,
                style = FirefoxTheme.typography.headline5,
            )

            Spacer(modifier = Modifier.height(FirefoxTheme.dimens.grid_2))

            Text(
                text = stringResource(id = onboardingPageUiState.description),
                color = FirefoxTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
                style = FirefoxTheme.typography.body2,
            )
        }

        // Item 3
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PrimaryButton(
                text = stringResource(id = onboardingPageUiState.primaryButtonText),
                onClick = onPrimaryButtonClick,
            )

            if (onboardingPageUiState.secondaryButtonText != null) {
                Spacer(modifier = Modifier.height(FirefoxTheme.dimens.grid_1))
                SecondaryButton(
                    text = stringResource(id = onboardingPageUiState.secondaryButtonText),
                    onClick = onSecondaryButtonClick,
                )
            }
        }
    }

    LaunchedEffect(onboardingPageUiState) {
        // record impression event for onboarding page
        onboardingPageUiState.recordImpressionEvent()
    }
}

@Composable
private fun Indicators(onboardingState: OnboardingState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        Indicator(
            color = if (onboardingState == OnboardingState.Welcome) {
                FirefoxTheme.colors.indicatorActive
            } else {
                FirefoxTheme.colors.indicatorInactive
            },
        )

        Spacer(modifier = Modifier.width(8.dp))

        Indicator(
            color = if (onboardingState == OnboardingState.SyncSignIn) {
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

private const val IMAGE_HEIGHT_RATIO = 0.4f

@Preview
@Composable
private fun PreviewOnboardingScreen() {
    FirefoxTheme {
        OnboardingContent(
            content = OnboardingScreenState.Content(
                onboardingState = OnboardingState.SyncSignIn,
                isUserSignedIn = false,
            ),
            onDismiss = { },
            onPrimaryButtonClick = {},
            onSecondaryButtonClick = {},
        )
    }
}
