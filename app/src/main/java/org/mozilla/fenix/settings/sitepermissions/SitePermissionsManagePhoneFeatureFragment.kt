/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.sitepermissions

import android.app.Application
import android.content.Context
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
import org.mozilla.fenix.ext.application
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.ext.showToolbar
import org.mozilla.fenix.settings.PhoneFeature
import org.mozilla.fenix.settings.PhoneFeature.AUTOPLAY_AUDIBLE
import org.mozilla.fenix.settings.PhoneFeature.AUTOPLAY_INAUDIBLE
import org.mozilla.fenix.settings.initBlockedByAndroidView
import org.mozilla.fenix.settings.setStartCheckedIndicator
import org.mozilla.fenix.utils.OnWifiChanged
import org.mozilla.fenix.utils.Settings

private const val AUTOPLAY_ALLOW_ALL = 0
private const val AUTOPLAY_ALLOW_ON_WIFI = 1
private const val AUTOPLAY_BLOCK_AUDIBLE = 2
private const val AUTOPLAY_BLOCK_ALL = 4

@SuppressWarnings("TooManyFunctions")
class SitePermissionsManagePhoneFeatureFragment : Fragment() {
    private lateinit var phoneFeature: PhoneFeature
    private lateinit var settings: Settings
    private lateinit var blockedByAndroidView: View

    companion object {

        private var wifiConnectedListener: OnWifiChanged? = null

        fun maybeAddWifiConnectedListener(app: Application) {
            if (app.settings().getAutoplayUserSetting(AUTOPLAY_BLOCK_ALL) == AUTOPLAY_ALLOW_ON_WIFI) {
                addWifiConnectedListener(app)
            }
        }

        private fun getWifiConnectedListener(settings: Settings): OnWifiChanged {
            if (wifiConnectedListener == null) {
                wifiConnectedListener = OnWifiChanged { connected ->
                    val setting = if (connected) ALLOWED else BLOCKED
                    settings.setSitePermissionsPhoneFeatureAction(AUTOPLAY_AUDIBLE, setting)
                    settings.setSitePermissionsPhoneFeatureAction(AUTOPLAY_INAUDIBLE, setting)
                }
            }
            return wifiConnectedListener!!
        }

        private fun removeWifiConnectedListener(app: Application) {
            app.components.wifiConnectionListener.removeOnWifiConnectedChangedListener(
                getWifiConnectedListener(app.settings())
            )
        }

        private fun addWifiConnectedListener(app: Application) {
            app.components.wifiConnectionListener.addOnWifiConnectedChangedListener(
                getWifiConnectedListener(app.settings())
            )
        }
    }

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
        with (rootView.ask_to_allow_radio) {
            if (phoneFeature == AUTOPLAY_AUDIBLE) {
                text = getString(R.string.preference_option_autoplay_allowed2)
                setOnClickListener {
                    saveActionInSettings(it.context, AUTOPLAY_ALLOW_ALL)
                }
                restoreState(AUTOPLAY_ALLOW_ALL)
            } else {
                text = getCombinedLabel(
                    getString(R.string.preference_option_phone_feature_ask_to_allow),
                    getString(R.string.phone_feature_recommended)
                )
                setOnClickListener {
                    saveActionInSettings(ASK_TO_ALLOW)
                }
                restoreState(ASK_TO_ALLOW)
            }
        }
    }

    private fun initSecondRadio(rootView: View) {
        with (rootView.block_radio) {
            if (phoneFeature == AUTOPLAY_AUDIBLE) {
                text = getString(R.string.preference_option_autoplay_allowed_wifi_only2)
                setOnClickListener {
                    saveActionInSettings(it.context, AUTOPLAY_ALLOW_ON_WIFI)
                }
                restoreState(AUTOPLAY_ALLOW_ON_WIFI)
            } else {
                text = getString(R.string.preference_option_phone_feature_blocked)
                setOnClickListener {
                    saveActionInSettings(BLOCKED)
                }
                restoreState(BLOCKED)
            }
        }
    }

    private fun initThirdRadio(rootView: View) {
        with (rootView.third_radio) {
            if (phoneFeature == AUTOPLAY_AUDIBLE) {
                visibility = View.VISIBLE
                text = getString(R.string.preference_option_autoplay_block_audio)
                setOnClickListener {
                    saveActionInSettings(it.context, AUTOPLAY_BLOCK_AUDIBLE)
                }
                restoreState(AUTOPLAY_BLOCK_AUDIBLE)
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
                    saveActionInSettings(it.context, AUTOPLAY_BLOCK_ALL)
                }
                restoreState(AUTOPLAY_BLOCK_ALL)
            } else {
                visibility = View.GONE
            }
        }
    }

    private fun RadioButton.restoreState(buttonAction: SitePermissionsRules.Action) {
        if (phoneFeature.getAction(settings) == buttonAction) {
            this.isChecked = true
            this.setStartCheckedIndicator()
        }
    }

    private fun RadioButton.restoreState(buttonAutoplaySetting: Int) {
        if (settings.getAutoplayUserSetting(AUTOPLAY_BLOCK_ALL) == buttonAutoplaySetting) {
            this.isChecked = true
            this.setStartCheckedIndicator()
        }
    }

    private fun saveActionInSettings(action: SitePermissionsRules.Action) {
        settings.setSitePermissionsPhoneFeatureAction(phoneFeature, action)
    }

    /**
     * @param TODO
     */
    private fun saveActionInSettings(context: Context, autoplaySetting: Int) {
        settings.setAutoplayUserSetting(autoplaySetting)
        val (audible, inaudible) = when (autoplaySetting) {
            AUTOPLAY_ALLOW_ALL -> ALLOWED to ALLOWED
            AUTOPLAY_ALLOW_ON_WIFI -> {
                addWifiConnectedListener(context.application)
                return
            }
            AUTOPLAY_BLOCK_AUDIBLE -> BLOCKED to ALLOWED
            AUTOPLAY_BLOCK_ALL -> BLOCKED to BLOCKED
            else -> return
        }
        removeWifiConnectedListener(context.application)
        settings.setSitePermissionsPhoneFeatureAction(AUTOPLAY_AUDIBLE, audible)
        settings.setSitePermissionsPhoneFeatureAction(AUTOPLAY_INAUDIBLE, inaudible)
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
