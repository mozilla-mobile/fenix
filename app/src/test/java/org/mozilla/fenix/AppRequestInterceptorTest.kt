/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix

import android.net.ConnectivityManager
import androidx.core.content.getSystemService
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import mozilla.components.browser.errorpages.ErrorPages
import mozilla.components.browser.errorpages.ErrorType
import mozilla.components.concept.engine.request.RequestInterceptor
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.AppRequestInterceptor.Companion.HIGH_RISK_ERROR_PAGES
import org.mozilla.fenix.AppRequestInterceptor.Companion.LOW_AND_MEDIUM_RISK_ERROR_PAGES
import org.mozilla.fenix.ext.isOnline
import org.mozilla.fenix.helpers.FenixRobolectricTestRunner

@RunWith(FenixRobolectricTestRunner::class)
class AppRequestInterceptorTest {

    private lateinit var interceptor: RequestInterceptor

    @Before
    fun setUp() {

        mockkStatic("org.mozilla.fenix.ext.ConnectivityManagerKt")

        every { testContext.getSystemService<ConnectivityManager>()!!.isOnline() } returns true

        interceptor = AppRequestInterceptor(testContext)
    }

    @Test
    fun `onErrorRequest results in correct error page for low risk level error`() {
        setOf(
            ErrorType.UNKNOWN,
            ErrorType.ERROR_NET_INTERRUPT,
            ErrorType.ERROR_NET_TIMEOUT,
            ErrorType.ERROR_CONNECTION_REFUSED,
            ErrorType.ERROR_UNKNOWN_SOCKET_TYPE,
            ErrorType.ERROR_REDIRECT_LOOP,
            ErrorType.ERROR_OFFLINE,
            ErrorType.ERROR_NET_RESET,
            ErrorType.ERROR_UNSAFE_CONTENT_TYPE,
            ErrorType.ERROR_CORRUPTED_CONTENT,
            ErrorType.ERROR_CONTENT_CRASHED,
            ErrorType.ERROR_INVALID_CONTENT_ENCODING,
            ErrorType.ERROR_UNKNOWN_HOST,
            ErrorType.ERROR_MALFORMED_URI,
            ErrorType.ERROR_FILE_NOT_FOUND,
            ErrorType.ERROR_FILE_ACCESS_DENIED,
            ErrorType.ERROR_PROXY_CONNECTION_REFUSED,
            ErrorType.ERROR_UNKNOWN_PROXY_HOST,
            ErrorType.ERROR_UNKNOWN_PROTOCOL
        ).forEach { error ->
            val actualPage = createActualErrorPage(error)
            val expectedPage = createExpectedErrorPage(
                error = error,
                html = LOW_AND_MEDIUM_RISK_ERROR_PAGES
            )

            assertEquals(expectedPage, actualPage)
        }
    }

    @Test
    fun `onErrorRequest results in correct error page for medium risk level error`() {
        setOf(
            ErrorType.ERROR_SECURITY_BAD_CERT,
            ErrorType.ERROR_SECURITY_SSL,
            ErrorType.ERROR_PORT_BLOCKED
        ).forEach { error ->
            val actualPage = createActualErrorPage(error)
            val expectedPage = createExpectedErrorPage(
                error = error,
                html = LOW_AND_MEDIUM_RISK_ERROR_PAGES
            )

            assertEquals(expectedPage, actualPage)
        }
    }

    @Test
    fun `onErrorRequest results in correct error page for high risk level error`() {
        setOf(
            ErrorType.ERROR_SAFEBROWSING_HARMFUL_URI,
            ErrorType.ERROR_SAFEBROWSING_MALWARE_URI,
            ErrorType.ERROR_SAFEBROWSING_PHISHING_URI,
            ErrorType.ERROR_SAFEBROWSING_UNWANTED_URI
        ).forEach { error ->
            val actualPage = createActualErrorPage(error)
            val expectedPage = createExpectedErrorPage(
                error = error,
                html = HIGH_RISK_ERROR_PAGES
            )

            assertEquals(expectedPage, actualPage)
        }
    }

    private fun createActualErrorPage(error: ErrorType): String {
        val errorPage = interceptor.onErrorRequest(session = mockk(), errorType = error, uri = null)
                as RequestInterceptor.ErrorResponse.Uri
        return errorPage.uri
    }

    private fun createExpectedErrorPage(error: ErrorType, html: String): String {
        return ErrorPages.createUrlEncodedErrorPage(
            context = testContext,
            errorType = error,
            htmlResource = html
        )
    }
}
