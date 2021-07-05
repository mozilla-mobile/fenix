/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.quicksettings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat.getColor
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.quicksettings_website_info.*
import mozilla.components.browser.icons.BrowserIcons
import mozilla.components.support.ktx.android.content.getDrawableWithTint
import mozilla.components.support.ktx.kotlin.tryGetHostFromUrl
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.loadIntoView

/**
 * MVI View that knows to display a whether the current website uses a secure connection or not.
 *
 * Currently it does not support any user interaction.
 *
 * @param container [ViewGroup] in which this View will inflate itself.
 * @param icons [BrowserIcons] instance for rendering the sites icon.
 */
class WebsiteInfoView(
    container: ViewGroup,
    private val icons: BrowserIcons = container.context.components.core.icons
) : LayoutContainer {

    override val containerView: View = LayoutInflater.from(container.context)
        .inflate(R.layout.quicksettings_website_info, container, true)

    /**
     * Allows changing what this View displays.
     *
     * @param state [WebsiteInfoState] to be rendered.
     */
    fun update(state: WebsiteInfoState) {
        icons.loadIntoView(favicon_image, state.websiteUrl)
        bindUrl(state.websiteUrl)
        bindSecurityInfo(state.websiteSecurityUiValues)
    }

    private fun bindUrl(websiteUrl: String) {
        url.text = websiteUrl.tryGetHostFromUrl()
    }

    private fun bindSecurityInfo(uiValues: WebsiteSecurityUiValues) {
        val tint = getColor(containerView.context, uiValues.iconTintRes)
        securityInfo.setText(uiValues.securityInfoRes)
        securityInfoIcon.setImageDrawable(
            containerView.context.getDrawableWithTint(uiValues.iconRes, tint)
        )
    }
}
