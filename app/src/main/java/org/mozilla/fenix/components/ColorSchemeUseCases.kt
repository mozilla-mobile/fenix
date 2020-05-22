package org.mozilla.fenix.components

import android.content.res.Configuration
import android.content.res.Resources
import mozilla.components.concept.engine.mediaquery.PreferredColorScheme
import mozilla.components.feature.session.SessionUseCases
import org.mozilla.fenix.utils.Settings

object ColorSchemeUseCases {
    class RetrieveColorSchemeUseCase(
        private val resources: Resources,
        private val settings: Settings
    ) {

        /**
         * Sets Preferred Color scheme based on Dark/Light Theme Settings or Current Configuration.
         */
        fun getPreferredColorScheme(): PreferredColorScheme {
            val inDark =
                (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                        Configuration.UI_MODE_NIGHT_YES
            return when {
                settings.shouldUseDarkTheme -> PreferredColorScheme.Dark
                settings.shouldUseLightTheme -> PreferredColorScheme.Light
                inDark -> PreferredColorScheme.Dark
                else -> PreferredColorScheme.Light
            }
        }
    }

    class CustomizeColorSchemeUseCase(
        private val engineSettings: mozilla.components.concept.engine.Settings,
        private val reloadUrlUseCase: SessionUseCases.ReloadUrlUseCase,
        private val retrieveColorScheme: RetrieveColorSchemeUseCase
    ) {

        /**
         * Updates the browser engine with the preferred color scheme.
         */
        fun customizeEngine() {
            val preferredColorScheme = retrieveColorScheme.getPreferredColorScheme()
            if (engineSettings.preferredColorScheme != preferredColorScheme) {
                engineSettings.preferredColorScheme = preferredColorScheme
                reloadUrlUseCase()
            }
        }
    }
}
