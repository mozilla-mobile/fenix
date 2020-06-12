/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings

import android.os.Bundle
import androidx.navigation.findNavController
import androidx.preference.CheckBoxPreference
import androidx.preference.DropDownPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import org.mozilla.fenix.BrowserDirection
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.getPreferenceKey
import org.mozilla.fenix.ext.nav
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.ext.showToolbar
import org.mozilla.fenix.trackingprotection.TrackingProtectionMode

/**
 * Displays the toggle for tracking protection, options for tracking protection policy and a button
 * to open info about the tracking protection [org.mozilla.fenix.exceptions.ExceptionsFragment].
 */
class TrackingProtectionFragment : PreferenceFragmentCompat() {

    private val exceptionsClickListener = Preference.OnPreferenceClickListener {
        val directions =
            TrackingProtectionFragmentDirections.actionTrackingProtectionFragmentToExceptionsFragment()
        requireView().findNavController().navigate(directions)
        true
    }
    private lateinit var customCookies: CheckBoxPreference
    private lateinit var customCookiesSelect: DropDownPreference
    private lateinit var customTracking: CheckBoxPreference
    private lateinit var customTrackingSelect: DropDownPreference
    private lateinit var customCryptominers: CheckBoxPreference
    private lateinit var customFingerprinters: CheckBoxPreference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.tracking_protection_preferences, rootKey)
        val radioStrict = bindTrackingProtectionRadio(TrackingProtectionMode.STRICT)
        val radioStandard = bindTrackingProtectionRadio(TrackingProtectionMode.STANDARD)
        val radioCustom = bindCustom()
        setupRadioGroups(radioStrict, radioStandard, radioCustom)
        updateCustomOptionsVisibility()
    }

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.preference_enhanced_tracking_protection))

        // Tracking Protection Switch
        val trackingProtectionKey = getPreferenceKey(R.string.pref_key_tracking_protection)
        val preferenceTP = findPreference<SwitchPreference>(trackingProtectionKey)

        preferenceTP?.isChecked = requireContext().settings().shouldUseTrackingProtection
        preferenceTP?.setOnPreferenceChangeListener<Boolean> { preference, trackingProtectionOn ->
            preference.context.settings().shouldUseTrackingProtection =
                trackingProtectionOn
            with(preference.context.components) {
                val policy = core.trackingProtectionPolicyFactory
                    .createTrackingProtectionPolicy(trackingProtectionOn)
                useCases.settingsUseCases.updateTrackingProtection(policy)
                useCases.sessionUseCases.reload()
            }
            true
        }

        val trackingProtectionLearnMore =
            requireContext().getPreferenceKey(R.string.pref_key_etp_learn_more)
        val learnMorePreference = findPreference<Preference>(trackingProtectionLearnMore)
        learnMorePreference?.setOnPreferenceClickListener {
            (activity as HomeActivity).openToBrowserAndLoad(
                searchTermOrURL = SupportUtils.getGenericSumoURLForTopic
                    (SupportUtils.SumoTopic.TRACKING_PROTECTION),
                newTab = true,
                from = BrowserDirection.FromTrackingProtection
            )
            true
        }
        learnMorePreference?.summary = getString(
            R.string.preference_enhanced_tracking_protection_explanation,
            getString(R.string.app_name)
        )

        val exceptions = getPreferenceKey(R.string.pref_key_tracking_protection_exceptions)
        val preferenceExceptions = findPreference<Preference>(exceptions)
        preferenceExceptions?.onPreferenceClickListener = exceptionsClickListener
    }

    private fun bindTrackingProtectionRadio(
        mode: TrackingProtectionMode
    ): RadioButtonInfoPreference {
        val radio = requireNotNull(findPreference<RadioButtonInfoPreference>(
            getPreferenceKey(mode.preferenceKey)
        ))
        radio.contentDescription = getString(mode.contentDescriptionRes)

        val metrics = requireComponents.analytics.metrics
        radio.onClickListener {
            updateCustomOptionsVisibility()
            updateTrackingProtectionPolicy()
            when (mode) {
                TrackingProtectionMode.STANDARD ->
                    Event.TrackingProtectionSettingChanged.Setting.STANDARD
                TrackingProtectionMode.STRICT ->
                    Event.TrackingProtectionSettingChanged.Setting.STRICT
                TrackingProtectionMode.CUSTOM ->
                    Event.TrackingProtectionSettingChanged.Setting.CUSTOM
            }.let { setting ->
                metrics.track(Event.TrackingProtectionSettingChanged(setting))
            }
        }

        radio.onInfoClickListener {
            nav(
                R.id.trackingProtectionFragment,
                TrackingProtectionFragmentDirections
                    .actionTrackingProtectionFragmentToTrackingProtectionBlockingFragment(mode)
            )
        }

        return radio
    }

    private fun bindCustom(): RadioButtonInfoPreference {
        val radio = bindTrackingProtectionRadio(TrackingProtectionMode.CUSTOM)

        customCookies = requireNotNull(
            findPreference(
                getString(R.string.pref_key_tracking_protection_custom_cookies)
            )
        )

        customCookiesSelect = requireNotNull(
            findPreference(
                getString(R.string.pref_key_tracking_protection_custom_cookies_select)
            )
        )
        customTracking = requireNotNull(
            findPreference(
                getString(R.string.pref_key_tracking_protection_custom_tracking_content)
            )
        )
        customTrackingSelect = requireNotNull(
            findPreference(
                getString(R.string.pref_key_tracking_protection_custom_tracking_content_select)
            )
        )
        customCryptominers = requireNotNull(
            findPreference(
                getString(R.string.pref_key_tracking_protection_custom_cryptominers)
            )
        )
        customFingerprinters = requireNotNull(
            findPreference(
                getString(R.string.pref_key_tracking_protection_custom_fingerprinters)
            )
        )

        customCookies.onPreferenceChangeListener = object : SharedPreferenceUpdater() {
            override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
                customCookiesSelect.isVisible = !customCookies.isChecked
                return super.onPreferenceChange(preference, newValue).also {
                    updateTrackingProtectionPolicy()
                }
            }
        }

        customTracking.onPreferenceChangeListener = object : SharedPreferenceUpdater() {
            override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
                customTrackingSelect.isVisible = !customTracking.isChecked
                return super.onPreferenceChange(preference, newValue).also {
                    updateTrackingProtectionPolicy()
                }
            }
        }

        customCookiesSelect.onPreferenceChangeListener = object : StringSharedPreferenceUpdater() {
            override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
                return super.onPreferenceChange(preference, newValue).also {
                    updateTrackingProtectionPolicy()
                }
            }
        }

        customTrackingSelect.onPreferenceChangeListener = object : StringSharedPreferenceUpdater() {
            override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {

                return super.onPreferenceChange(preference, newValue).also {
                    updateTrackingProtectionPolicy()
                }
            }
        }

        customCryptominers.onPreferenceChangeListener = object : SharedPreferenceUpdater() {
            override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
                return super.onPreferenceChange(preference, newValue).also {
                    updateTrackingProtectionPolicy()
                }
            }
        }

        customFingerprinters.onPreferenceChangeListener = object : SharedPreferenceUpdater() {
            override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
                return super.onPreferenceChange(preference, newValue).also {
                    updateTrackingProtectionPolicy()
                }
            }
        }

        updateCustomOptionsVisibility()

        return radio
    }

    private fun updateTrackingProtectionPolicy() {
        context?.components?.let {
            val policy = it.core.trackingProtectionPolicyFactory
                .createTrackingProtectionPolicy()
            it.useCases.settingsUseCases.updateTrackingProtection.invoke(policy)
            it.useCases.sessionUseCases.reload.invoke()
        }
    }

    private fun setupRadioGroups(
        radioStrict: RadioButtonInfoPreference,
        radioStandard: RadioButtonInfoPreference,
        radioCustom: RadioButtonInfoPreference
    ) {
        radioStandard.addToRadioGroup(radioStrict)
        radioStrict.addToRadioGroup(radioStandard)

        radioStandard.addToRadioGroup(radioCustom)
        radioCustom.addToRadioGroup(radioStandard)

        radioStrict.addToRadioGroup(radioCustom)
        radioCustom.addToRadioGroup(radioStrict)
    }

    private fun updateCustomOptionsVisibility() {
        val isCustomSelected = requireContext().settings().useCustomTrackingProtection
        customCookies.isVisible = isCustomSelected
        customCookiesSelect.isVisible = isCustomSelected && customCookies.isChecked
        customTracking.isVisible = isCustomSelected
        customTrackingSelect.isVisible = isCustomSelected && customTracking.isChecked
        customCryptominers.isVisible = isCustomSelected
        customFingerprinters.isVisible = isCustomSelected
    }
}
