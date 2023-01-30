/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.quicksettings.protections.cookiebanners

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import kotlinx.parcelize.Parcelize
import org.mozilla.fenix.R

@Parcelize
enum class CookieBannerState(
    @DrawableRes val iconRes: Int,
    @StringRes val description: Int,
    @StringRes val longDescription: Int,
) :
    Parcelable {
    OFF(
        R.drawable.ic_cookies_enabled,
        R.string.reduce_cookie_banner_off_for_site,
        R.string.reduce_cookie_banner_details_panel_description_on_for_site,
    ),
    ON(
        R.drawable.ic_cookies_disabled,
        R.string.reduce_cookie_banner_on_for_site,
        R.string.reduce_cookie_banner_details_panel_description_off_for_site,
    ),
    NO_SUPPORTED(
        R.drawable.ic_cookies_disabled,
        R.string.reduce_cookie_banner_not_supported_for_site,
        R.string.reduce_cookie_banner_details_panel_description_off_for_site,
    ),
    ;

    fun toggle(): CookieBannerState {
        return when (this) {
            OFF -> ON
            ON -> OFF
            NO_SUPPORTED -> throw IllegalStateException("Unable to toggle NO_SUPPORTED")
        }
    }
}
