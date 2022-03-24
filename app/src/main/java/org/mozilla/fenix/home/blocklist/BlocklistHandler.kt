/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.blocklist

import androidx.annotation.VisibleForTesting
import mozilla.components.support.ktx.kotlin.sha1
import org.mozilla.fenix.home.recentbookmarks.RecentBookmark
import org.mozilla.fenix.home.recenttabs.RecentTab
import org.mozilla.fenix.home.recentvisits.RecentlyVisitedItem
import org.mozilla.fenix.utils.Settings

/**
 * Class for interacting with the a blocklist stored in [settings].
 * The blocklist is a set of SHA1 hashed URLs, which are stripped
 * of protocols and common subdomains like "www" or "mobile".
 */
class BlocklistHandler(private val settings: Settings) {

    /**
     * Add an URL to the blocklist. The URL will be stripped and hashed,
     * so no pre-formatted is required.
     */
    fun addUrlToBlocklist(url: String) {
        val updatedBlocklist = settings.homescreenBlocklist + url.stripAndHash()
        settings.homescreenBlocklist = updatedBlocklist
    }

    /**
     * Filter a list of recent bookmarks by the blocklist. Requires this class to be contextually
     * in a scope.
     */
    @JvmName("filterRecentBookmark")
    fun List<RecentBookmark>.filteredByBlocklist(): List<RecentBookmark> =
        settings.homescreenBlocklist.let { blocklist ->
            filterNot {
                it.url?.let { url -> blocklistContainsUrl(blocklist, url) } ?: false
            }
        }

    /**
     * Filter a list of recent tabs by the blocklist. Requires this class to be contextually
     * in a scope.
     */
    @JvmName("filterRecentTab")
    fun List<RecentTab>.filteredByBlocklist(): List<RecentTab> =
        settings.homescreenBlocklist.let { blocklist ->
            filterNot {
                it is RecentTab.Tab && blocklistContainsUrl(blocklist, it.state.content.url)
            }
        }

    /**
     * Filter a list of recent history items by the blocklist. Requires this class to be contextually
     * in a scope.
     */
    @JvmName("filterRecentHistory")
    fun List<RecentlyVisitedItem>.filteredByBlocklist(): List<RecentlyVisitedItem> =
        settings.homescreenBlocklist.let { blocklist ->
            filterNot {
                it is RecentlyVisitedItem.RecentHistoryHighlight &&
                    blocklistContainsUrl(blocklist, it.url)
            }
        }

    private fun blocklistContainsUrl(blocklist: Set<String>, url: String): Boolean =
        blocklist.any { it == url.stripAndHash() }
}

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal fun String.stripAndHash(): String =
    this.stripProtocolAndCommonSubdomains().sha1()

// Eventually, this should be standardize in A-C and this can then be removed
// https://github.com/mozilla-mobile/android-components/issues/11743
private fun String.stripProtocolAndCommonSubdomains(): String {
    val stripped = this.substringAfter("://").dropLastWhile { it == '/' }
    // This kind of stripping allows us to match "twitter" to "mobile.twitter.com".
    // Borrowed from DomainMatcher in A-C
    val domainsToStrip = listOf("www", "mobile", "m")

    domainsToStrip.forEach { domain ->
        if (stripped.startsWith("$domain.")) {
            return stripped.substring(domain.length + 1)
        }
    }
    return stripped
}
