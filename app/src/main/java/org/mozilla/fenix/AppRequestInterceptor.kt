/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix

import android.content.Context
import mozilla.components.browser.errorpages.ErrorPages
import mozilla.components.browser.errorpages.ErrorType
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.request.RequestInterceptor
import org.mozilla.fenix.ext.components

class AppRequestInterceptor(private val context: Context) : RequestInterceptor {
    override fun onLoadRequest(session: EngineSession, uri: String): RequestInterceptor.InterceptionResponse? {
        // Accounts uses interception to check for a "success URL" in the sign-in flow to finalize authentication.
        return context.components.services.accountsAuthFeature.interceptor.onLoadRequest(session, uri)
    }

    override fun onErrorRequest(
        session: EngineSession,
        errorType: ErrorType,
        uri: String?
    ): RequestInterceptor.ErrorResponse? {
        return RequestInterceptor.ErrorResponse(ErrorPages.createErrorPage(context, errorType))
    }
}
