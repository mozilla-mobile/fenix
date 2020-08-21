/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.advanced

import android.os.Parcelable
import androidx.annotation.StringRes
import kotlinx.android.parcel.Parcelize
import org.mozilla.fenix.R

@Parcelize
enum class DownloadPathMode(
    @StringRes val preferenceKey: Int,
    @StringRes val titleRes: Int,
    @StringRes val contentDescriptionRes: Int
) : Parcelable {

    DEFAULT(
        preferenceKey = R.string.pref_key_default_download_path,
        titleRes = R.string.preferences_default_download_path,
        contentDescriptionRes = R.string.preferences_default_download_path_description
    ),
    CUSTOM(
        preferenceKey = R.string.pref_key_custom_download_path,
        titleRes = R.string.preferences_custom_download_path,
        contentDescriptionRes = R.string.preferences_custom_download_path_description
    )
}
