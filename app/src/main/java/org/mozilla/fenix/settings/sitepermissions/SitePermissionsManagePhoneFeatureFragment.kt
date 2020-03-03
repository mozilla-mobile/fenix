/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.sitepermissions

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned.SPAN_EXCLUSIVE_INCLUSIVE
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_manage_site_permissions_feature_phone.view.ask_to_allow_radio
import kotlinx.android.synthetic.main.fragment_manage_site_permissions_feature_phone.view.block_radio
import kotlinx.android.synthetic.main.fragment_manage_site_permissions_feature_phone.view.fourth_radio
import kotlinx.android.synthetic.main.fragment_manage_site_permissions_feature_phone.view.third_radio
import mozilla.components.feature.sitepermissions.SitePermissionsRules
import mozilla.components.feature.sitepermissions.SitePermissionsRules.Action.ALLOWED
import mozilla.components.feature.sitepermissions.SitePermissionsRules.Action.ASK_TO_ALLOW
import mozilla.components.feature.sitepermissions.SitePermissionsRules.Action.BLOCKED
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.ext.showToolbar
import org.mozilla.fenix.settings.PhoneFeature
import org.mozilla.fenix.settings.PhoneFeature.AUTOPLAY_AUDIBLE
import org.mozilla.fenix.settings.initBlockedByAndroidView
import org.mozilla.fenix.settings.setStartCheckedIndicator
import org.mozilla.fenix.utils.Settings

@SuppressWarnings("TooManyFunctions")
class SitePermissionsManagePhoneFeatureFragment : Fragment() {
    private lateinit var phoneFeature: PhoneFeature
    private lateinit var settings: Settings
    private lateinit var blockedByAndroidView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        phoneFeature = SitePermissionsManagePhoneFeatureFragmentArgs
            .fromBundle(requireArguments())
            .permission.toPhoneFeature()

        showToolbar(phoneFeature.getLabel(requireContext()))
        settings = requireContext().settings()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(
            R.layout.fragment_manage_site_permissions_feature_phone,
            container,
            false
        )

        initFirstRadio(rootView)
        initSecondRadio(rootView)
        initThirdRadio(rootView)
        initFourthRadio(rootView)
        bindBlockedByAndroidContainer(rootView)

        return rootView
    }

    override fun onResume() {
        super.onResume()
        initBlockedByAndroidView(phoneFeature, blockedByAndroidView)
    }

    private fun initFirstRadio(rootView: View) {
        val (label, expectedAction) = if (phoneFeature == AUTOPLAY_AUDIBLE) {
            getString(R.string.preference_option_autoplay_allowed2) to BLOCKED // TODO not blocked
        } else {
            getCombinedLabel(
                getString(R.string.preference_option_phone_feature_ask_to_allow),
                getString(R.string.phone_feature_recommended)
            ) to ASK_TO_ALLOW
        }

        with (rootView.ask_to_allow_radio) {
            text = label
            setOnClickListener {
                saveActionInSettings(expectedAction)
            }
            restoreState(expectedAction) // TODO different for autoplay
        }
    }

    private fun RadioButton.restoreState(action: SitePermissionsRules.Action) {
        if (phoneFeature.getAction(settings) == action) {
            this.isChecked = true
            this.setStartCheckedIndicator()
        }
    }

    private fun initSecondRadio(rootView: View) {
        val (label, expectedAction) = if (phoneFeature == AUTOPLAY_AUDIBLE) {
            getString(R.string.preference_option_autoplay_allowed_wifi_only2) to ALLOWED // TODO not allowed
        } else {
            getString(R.string.preference_option_phone_feature_blocked) to BLOCKED
        }

        with (rootView.block_radio) {
            text = label
            setOnClickListener {
                saveActionInSettings(expectedAction)
            }
            restoreState(expectedAction) // TODO different for autoplay
        }
    }

    private fun initThirdRadio(rootView: View) {
        with (rootView.third_radio) {
            if (phoneFeature == AUTOPLAY_AUDIBLE) {
                visibility = View.VISIBLE
                text = getString(R.string.preference_option_autoplay_block_audio)
                setOnClickListener {
                    // TODO
                }
                // TODO restore state
            } else {
                visibility = View.GONE
            }
        }
    }

    private fun initFourthRadio(rootView: View) {
        with(rootView.fourth_radio) {
            if (phoneFeature == AUTOPLAY_AUDIBLE) {
                visibility = View.VISIBLE
                text = getCombinedLabel(
                    getString(R.string.preference_option_autoplay_blocked2),
                    getString(R.string.phone_feature_recommended)
                )
                setOnClickListener {
                    // TODO
                }
                // TODO restore state
            } else {
                visibility = View.GONE
            }
        }
    }

    private fun bindBlockedByAndroidContainer(rootView: View) {
        blockedByAndroidView = rootView.findViewById<View>(R.id.permissions_blocked_container)
        initSettingsButton(blockedByAndroidView)
    }

    private fun Int.toPhoneFeature(): PhoneFeature {
        return requireNotNull(PhoneFeature.values().find { feature ->
            this == feature.id
        }) {
            "$this is a invalid PhoneFeature"
        }
    }

    private fun initSettingsButton(rootView: View) {
        val button = rootView.findViewById<Button>(R.id.settings_button)
        button.setOnClickListener {
            openSettings()
        }
    }

    private fun openSettings() {
        val intent = Intent(ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", requireContext().packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    private fun saveActionInSettings(action: SitePermissionsRules.Action) {
        settings.setSitePermissionsPhoneFeatureAction(phoneFeature, action)
    }

    /**
     * Returns a [CharSequence] that arranges and styles [mainText], a line break, and then [subText]
     */
    private fun getCombinedLabel(mainText: CharSequence, subText: CharSequence): CharSequence {
        val subTextSize =
            resources.getDimensionPixelSize(R.dimen.phone_feature_label_recommended_text_size)
        val recommendedSpannable = SpannableString(subText)

        recommendedSpannable.setSpan(
            ForegroundColorSpan(Color.GRAY),
            0,
            recommendedSpannable.length,
            SPAN_EXCLUSIVE_INCLUSIVE
        )

        recommendedSpannable.setSpan(
            AbsoluteSizeSpan(subTextSize), 0,
            recommendedSpannable.length,
            SPAN_EXCLUSIVE_INCLUSIVE
        )

        return with(SpannableStringBuilder()) {
            append(mainText)
            append("\n")
            append(recommendedSpannable)
            this
        }
    }
}
