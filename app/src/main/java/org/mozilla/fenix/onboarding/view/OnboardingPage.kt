/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.onboarding.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.mozilla.fenix.R
import org.mozilla.fenix.compose.button.PrimaryButton
import org.mozilla.fenix.compose.button.SecondaryButton
import org.mozilla.fenix.theme.FirefoxTheme

/**
 * A page for displaying Onboarding Content.
 *
 * @param onboardingPageState The page content that's displayed.
 * @param onPrimaryButtonClick Invoked when the user clicks the primary button.
 * @param onSecondaryButtonClick Invoked when the user clicks the secondary button.
 * @param modifier The modifier to be applied to the Composable.
 */
@Composable
fun OnboardingPage(
    onboardingPageState: OnboardingPageState,
    onPrimaryButtonClick: () -> Unit,
    onSecondaryButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Image(
            painter = painterResource(id = onboardingPageState.image),
            contentDescription = null,
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(id = onboardingPageState.title),
                color = FirefoxTheme.colors.textPrimary,
                textAlign = TextAlign.Center,
                style = FirefoxTheme.typography.headline5,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(id = onboardingPageState.description),
                color = FirefoxTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
                style = FirefoxTheme.typography.body2,
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PrimaryButton(
                text = stringResource(id = onboardingPageState.primaryButtonText),
                onClick = onPrimaryButtonClick,
            )

            if (onboardingPageState.secondaryButtonText != null) {
                Spacer(modifier = Modifier.height(8.dp))
                SecondaryButton(
                    text = stringResource(id = onboardingPageState.secondaryButtonText),
                    onClick = onSecondaryButtonClick,
                )
            }
        }
    }

    LaunchedEffect(onboardingPageState) {
        onboardingPageState.onRecordImpressionEvent()
    }
}

@Composable
@Preview(showBackground = true)
private fun OnboardingPagePreview() {
    FirefoxTheme {
        OnboardingPage(
            onboardingPageState = OnboardingPageState(
                image = R.drawable.ic_onboarding_sync,
                title = R.string.onboarding_home_sync_title_3,
                description = R.string.onboarding_home_sync_description,
                primaryButtonText = R.string.onboarding_home_sign_in_button,
                secondaryButtonText = R.string.onboarding_home_skip_button,
                onRecordImpressionEvent = {},
            ),
            onPrimaryButtonClick = {},
            onSecondaryButtonClick = {},
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        )
    }
}
