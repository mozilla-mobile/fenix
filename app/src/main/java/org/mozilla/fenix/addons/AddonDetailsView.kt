/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.addons

import android.net.Uri
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.view.View
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import androidx.core.text.getSpans
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_add_on_details.*
import mozilla.components.feature.addons.Addon
import mozilla.components.feature.addons.ui.translatedDescription
import mozilla.components.feature.addons.ui.updatedAtDate
import org.mozilla.fenix.R
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Locale

interface AddonDetailsInteractor {

    /**
     * Open the given addon siteUrl in the browser.
     */
    fun openWebsite(addonSiteUrl: Uri)

    /**
     * Display the updater dialog.
     */
    fun showUpdaterDialog(addon: Addon)
}

/**
 * Shows the details of an add-on.
 */
class AddonDetailsView(
    override val containerView: View,
    private val interactor: AddonDetailsInteractor
) : LayoutContainer {

    private val dateFormatter = DateFormat.getDateInstance()
    private val numberFormatter = NumberFormat.getNumberInstance(Locale.getDefault())

    fun bind(addon: Addon) {
        bindDetails(addon)
        bindAuthors(addon)
        bindVersion(addon)
        bindLastUpdated(addon)
        bindWebsite(addon)
        bindRating(addon)
    }

    private fun bindRating(addon: Addon) {
        addon.rating?.let { rating ->
            val resources = containerView.resources
            val ratingContentDescription =
                resources.getString(R.string.mozac_feature_addons_rating_content_description)
            rating_view.contentDescription = String.format(ratingContentDescription, rating.average)
            rating_view.rating = rating.average

            users_count.text = numberFormatter.format(rating.reviews)
        }
    }

    private fun bindWebsite(addon: Addon) {
        home_page_label.setOnClickListener {
            interactor.openWebsite(addon.siteUrl.toUri())
        }
    }

    private fun bindLastUpdated(addon: Addon) {
        last_updated_text.text = dateFormatter.format(addon.updatedAtDate)
    }

    private fun bindVersion(addon: Addon) {
        var version = addon.installedState?.version
        if (version.isNullOrEmpty()) {
            version = addon.version
        }
        version_text.text = version

        if (addon.isInstalled()) {
            version_text.setOnLongClickListener {
                interactor.showUpdaterDialog(addon)
                true
            }
        } else {
            version_text.setOnLongClickListener(null)
        }
    }

    private fun bindAuthors(addon: Addon) {
        author_text.text = addon.authors.joinToString { author -> author.name }.trim()
    }

    private fun bindDetails(addon: Addon) {
        val detailsText = addon.translatedDescription

        val parsedText = detailsText.replace("\n", "<br/>")
        val text = HtmlCompat.fromHtml(parsedText, HtmlCompat.FROM_HTML_MODE_COMPACT)

        val spannableStringBuilder = SpannableStringBuilder(text)
        val links = spannableStringBuilder.getSpans<URLSpan>()
        for (link in links) {
            addActionToLinks(spannableStringBuilder, link)
        }
        details.text = spannableStringBuilder
        details.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun addActionToLinks(
        spannableStringBuilder: SpannableStringBuilder,
        link: URLSpan
    ) {
        val start = spannableStringBuilder.getSpanStart(link)
        val end = spannableStringBuilder.getSpanEnd(link)
        val flags = spannableStringBuilder.getSpanFlags(link)
        val clickable: ClickableSpan = object : ClickableSpan() {
            override fun onClick(view: View) {
                view.setOnClickListener {
                    interactor.openWebsite(link.url.toUri())
                }
            }
        }
        spannableStringBuilder.setSpan(clickable, start, end, flags)
        spannableStringBuilder.removeSpan(link)
    }
}
