/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.advanced

import android.app.Activity
import mozilla.components.support.locale.LocaleManager
import org.mozilla.fenix.LocaleUpdater
import java.util.Locale

interface LocaleSettingsController {
    fun handleLocaleSelected(locale: Locale)
    fun handleSearchQueryTyped(query: String)
    fun handleDefaultLocaleSelected()
}

class DefaultLocaleSettingsController(
    private val activity: Activity,
    private val localeSettingsStore: LocaleSettingsStore
) : LocaleSettingsController {

    private val localeUpdater = LocaleUpdater(activity)

    override fun handleLocaleSelected(locale: Locale) {
        if (localeSettingsStore.state.selectedLocale == locale &&
            !LocaleManager.isDefaultLocaleSelected(activity)) {
            return
        }
        localeSettingsStore.dispatch(LocaleSettingsAction.Select(locale))
        LocaleManager.setNewLocale(activity, locale.toLanguageTag())
        localeUpdater.updateBaseConfiguration(activity, locale)
        activity.recreate()
    }

    override fun handleDefaultLocaleSelected() {
        if (LocaleManager.isDefaultLocaleSelected(activity)) {
            return
        }
        localeSettingsStore.dispatch(LocaleSettingsAction.Select(localeSettingsStore.state.localeList[0]))
        LocaleManager.resetToSystemDefault(activity)
        localeUpdater.updateBaseConfiguration(activity, localeSettingsStore.state.localeList[0])
        activity.recreate()
    }

    override fun handleSearchQueryTyped(query: String) {
        localeSettingsStore.dispatch(LocaleSettingsAction.Search(query))
    }
}
