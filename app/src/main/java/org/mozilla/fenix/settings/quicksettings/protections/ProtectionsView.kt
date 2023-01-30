/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.quicksettings.protections

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.view.isVisible
import org.mozilla.fenix.R
import org.mozilla.fenix.compose.annotation.LightDarkPreview
import org.mozilla.fenix.databinding.QuicksettingsProtectionsPanelBinding
import org.mozilla.fenix.settings.quicksettings.protections.cookiebanners.CookieBannerState
import org.mozilla.fenix.theme.FirefoxTheme
import org.mozilla.fenix.trackingprotection.ProtectionsState
import org.mozilla.fenix.utils.Settings

/**
 * MVI View that displays the tracking protection, cookie banner handling toggles and the navigation
 * to additional tracking protection details.
 *
 * @param containerView [ViewGroup] in which this View will inflate itself.
 * @param interactor [ProtectionsInteractor] which will have delegated to all user
 * @param settings [Settings] application settings.
 * interactions.
 */
class ProtectionsView(
    val containerView: ViewGroup,
    val interactor: ProtectionsInteractor,
    val settings: Settings,
) {

    /**
     * Allows changing what this View displays.
     */
    fun update(state: ProtectionsState) {
        bindTrackingProtectionInfo(state.isTrackingProtectionEnabled)
        bindCookieBannerProtection(state.cookieBannerState)
        binding.trackingProtectionSwitch.isVisible = settings.shouldUseTrackingProtection
        binding.cookieBannerItem.isVisible = shouldShowCookieBanner

        binding.trackingProtectionDetails.setOnClickListener {
            interactor.onTrackingProtectionDetailsClicked()
        }
    }

    @VisibleForTesting
    internal fun updateDetailsSection(show: Boolean) {
        binding.trackingProtectionDetails.isVisible = show
    }

    private fun bindTrackingProtectionInfo(isTrackingProtectionEnabled: Boolean) {
        binding.trackingProtectionSwitch.isChecked = isTrackingProtectionEnabled
        binding.trackingProtectionSwitch.setOnCheckedChangeListener { _, isChecked ->
            interactor.onTrackingProtectionToggled(isChecked)
        }
    }

    @VisibleForTesting
    internal val binding = QuicksettingsProtectionsPanelBinding.inflate(
        LayoutInflater.from(containerView.context),
        containerView,
        true,
    )

    private val shouldShowCookieBanner: Boolean
        get() = settings.shouldShowCookieBannerUI && settings.shouldUseCookieBanner

    private fun bindCookieBannerProtection(cookieBannerState: CookieBannerState) {
        val context = binding.cookieBannerItem.context
        val label = context.getString(R.string.preferences_cookie_banner_reduction)
        binding.cookieBannerItem.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                FirefoxTheme {
                    CookieBannerItem(
                        label = label,
                        description = context.getString(cookieBannerState.description),
                        startIconPainter = painterResource(cookieBannerState.iconRes),
                        endIconPainter = when (cookieBannerState) {
                            CookieBannerState.ON -> painterResource(R.drawable.ic_arrowhead_right)
                            CookieBannerState.OFF, CookieBannerState.NO_SUPPORTED -> null
                        },
                        onClick = if (cookieBannerState == CookieBannerState.ON) {
                            { interactor.onCookieBannerHandlingDetailsClicked() }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CookieBannerItem(
    label: String,
    description: String,
    startIconPainter: Painter,
    endIconPainter: Painter? = null,
    onClick: (() -> Unit)? = null,
) {
    val modifier = if (onClick != null) {
        Modifier.clickable { onClick() }
    } else {
        Modifier
    }
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = startIconPainter,
            contentDescription = null,
            modifier = Modifier.padding(horizontal = 0.dp),
            tint = FirefoxTheme.colors.iconPrimary,
        )
        Column(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .weight(1f),
        ) {
            Text(
                text = label,
                color = FirefoxTheme.colors.textPrimary,
                style = FirefoxTheme.typography.subtitle1,
                maxLines = 1,
            )
            Text(
                text = description,
                color = FirefoxTheme.colors.textSecondary,
                style = FirefoxTheme.typography.body2,
                maxLines = 1,
            )
        }
        if (endIconPainter != null) {
            Icon(
                modifier = Modifier
                    .padding(end = 0.dp)
                    .size(24.dp),
                painter = endIconPainter,
                contentDescription = null,
                tint = FirefoxTheme.colors.iconPrimary,
            )
        }
    }
}

@Composable
@LightDarkPreview
private fun CookieBannerItemPreview() {
    FirefoxTheme {
        Box(Modifier.background(FirefoxTheme.colors.layer1)) {
            CookieBannerItem(
                label = "Cookie Banner Reduction",
                description = "On for this site",
                startIconPainter = painterResource(R.drawable.ic_cookies_enabled),
                endIconPainter = painterResource(R.drawable.ic_arrowhead_right),
                onClick = { println("list item click") },
            )
        }
    }
}

@Composable
@LightDarkPreview
private fun NoDetectedCookieBannerItemPreview() {
    FirefoxTheme {
        Box(Modifier.background(FirefoxTheme.colors.layer1)) {
            CookieBannerItem(
                label = "Cookie Banner Reduction",
                description = "Site currently not supported",
                startIconPainter = painterResource(R.drawable.ic_cookies_disabled),
            )
        }
    }
}
