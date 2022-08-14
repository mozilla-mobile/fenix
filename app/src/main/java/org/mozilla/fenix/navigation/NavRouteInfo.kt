package org.mozilla.fenix.navigation

import androidx.annotation.StringRes


data class NavRouteInfo(
    val baseRoute: String,
    @StringRes
    val destinationLabelId: Int? = null,
    val screenArgs: List<ScreenArgsInfo<*>> = emptyList()
) {

    fun getGraphBuildRoute(): String {
        return "$baseRoute/${getRouteWithArgs()}"
    }

    private fun getRouteWithArgs(): String {
        val optionalArgs = screenArgs.filter { it.defaultValue == null }
        val args = screenArgs.filter { it.defaultValue != null }
        return args.joinToString("/") { "{${it.argName}}" } +
                if (optionalArgs.isNotEmpty()) {
                    "?" + optionalArgs.joinToString("&") { "${it.argName}={${it.argName}}" }
                } else {
                    ""
                }
    }
}