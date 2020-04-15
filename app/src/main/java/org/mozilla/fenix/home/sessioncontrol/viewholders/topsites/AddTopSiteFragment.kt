/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.sessioncontrol.viewholders.topsites

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_add_top_site.*
import kotlinx.android.synthetic.main.fragment_create_shortcut.add_button
import kotlinx.android.synthetic.main.fragment_create_shortcut.cancel_button
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import mozilla.components.concept.fetch.Request
import mozilla.components.concept.fetch.Response
import mozilla.components.support.ktx.kotlin.toNormalizedUrl
import mozilla.components.support.ktx.kotlin.tryGetHostFromUrl
import mozilla.components.support.utils.URLStringUtils.isURLLike
import org.mozilla.fenix.R
import org.mozilla.fenix.components.FenixSnackbar
import org.mozilla.fenix.ext.removeExtraSpaces
import org.mozilla.fenix.ext.requireComponents
import java.io.IOException

class AddTopSiteFragment : DialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.CreateShortcutDialogStyle)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_add_top_site, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        url_text.addTextChangedListener {
            updateAddButtonEnabledState()
        }
        cancel_button.setOnClickListener { dismiss() }
        add_button.setOnClickListener {
            val normalizedUrl = url_text.text.toString().toNormalizedUrl()
            val response = validateUrl(normalizedUrl)
            if (response != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    requireComponents.core.topSiteStorage.addTopSite(
                        getPageTitle(response.body.string()) ?: normalizedUrl.tryGetHostFromUrl(),
                        normalizedUrl
                    )
                    MainScope().launch {
                        FenixSnackbar.make(
                            view,
                            duration = Snackbar.LENGTH_SHORT,
                            isDisplayedWithBrowserToolbar = true
                        )
                            .setText(
                                view.context.getString(R.string.snackbar_added_to_top_sites)
                            )
                            .show()
                    }
                }
                dismiss()
            } else {
                url_text_field.error = resources
                    .getString(R.string.add_top_site_edittext_error_message)
            }
        }
    }

    private fun updateAddButtonEnabledState() {
        val validUrl = !url_text.text.isNullOrEmpty() && isURLLike(url_text.text.toString())
        add_button.isEnabled = validUrl
        add_button.alpha = if (validUrl) ENABLED_ALPHA else DISABLED_ALPHA
    }

    private fun validateUrl(url: String): Response? {
        val request = Request(url)
        return try {
            requireComponents.core.client.fetch(request)
        } catch (e: IOException) {
            return null
        } catch (e: IllegalArgumentException) {
            return null
        }
    }

    private fun getPageTitle(bodyString: String): String? {
        val matcher = TITLE_TAG.find(bodyString)
        return matcher?.groups?.get(1)?.value?.removeExtraSpaces()
    }

    companion object {
        private const val ENABLED_ALPHA = 1.0f
        private const val DISABLED_ALPHA = 0.4f
        private val TITLE_TAG = "<title>(.*)</title>".toRegex(
            setOf(
                RegexOption.IGNORE_CASE,
                RegexOption.DOT_MATCHES_ALL
            )
        )
    }
}
