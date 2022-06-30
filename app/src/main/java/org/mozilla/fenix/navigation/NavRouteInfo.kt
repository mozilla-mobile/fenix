package org.mozilla.fenix.navigation

import androidx.annotation.StringRes


data class NavRouteInfo(
    val navRoute: String,
    @StringRes
    val destinationLabelId: Int? = null,
    val screenArgs: List<ScreenArgsInfo<*>> = emptyList()
)