/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.search

import android.net.Uri
import mozilla.components.support.ktx.kotlin.toNormalizedUrl
import org.mozilla.fenix.ext.logDebug
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

object SearchStringValidator {
    sealed class Result {
        object Success : Result()
        object MalformedURL : Result()
        object CannotReach : Result()
    }

    fun isSearchStringValid(searchString: String): Result {
        val searchURL = createSearchURL(searchString) ?: return Result.MalformedURL
        val connection = openConnection(searchURL)

        return try {
            if (connection.hasValidResponseCode) Result.Success else Result.CannotReach
        } catch (e: IOException) {
            logDebug(LOGTAG, "Failure to get response code from server: returning invalid search query")
            Result.CannotReach
        } finally {
            try { connection.inputStream.close() } catch (_: IOException) {
                logDebug(LOGTAG, "connection.inputStream failed to close")
            }

            connection.disconnect()
        }
    }

    private val HttpURLConnection.hasValidResponseCode: Boolean
        get() = responseCode < VALID_RESPONSE_CODE_UPPER_BOUND

    private fun createSearchURL(searchString: String): URL? {
        // we should share the code to substitute and normalize the search string (see SearchEngine.buildSearchUrl).
        val encodedTestQuery = Uri.encode("testSearchEngineValidation")

        val normalizedHttpsSearchURLStr = searchString.toNormalizedUrl()
        val searchURLStr = normalizedHttpsSearchURLStr.replace("%s".toRegex(), encodedTestQuery)
        return try { URL(searchURLStr) } catch (e: MalformedURLException) {
            return null
        }
    }

    private fun openConnection(url: URL): HttpURLConnection {
        val connection = url.openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = true
        connection.connectTimeout = SEARCH_QUERY_VALIDATION_TIMEOUT_MILLIS
        connection.readTimeout = SEARCH_QUERY_VALIDATION_TIMEOUT_MILLIS

        return connection
    }

    private const val LOGTAG = "AddSearchEngineFragment"
    private const val SEARCH_QUERY_VALIDATION_TIMEOUT_MILLIS = 4000
    private const val VALID_RESPONSE_CODE_UPPER_BOUND = 300
}
