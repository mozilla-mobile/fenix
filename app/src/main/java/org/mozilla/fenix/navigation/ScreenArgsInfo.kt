package org.mozilla.fenix.navigation

import androidx.navigation.NavType

data class ScreenArgsInfo<T>(
    val argName: String,
    val argType: NavType<T>,
    val defaultValue:T? = null
)
