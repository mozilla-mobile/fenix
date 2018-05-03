/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.fenix.components.session

import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionProvider

class DefaultSessionProvider: SessionProvider {
    override fun getInitialSessions(): Pair<List<Session>, Int> {
        // For now we start with a very simple session provider that will always start with a session
        // that contains mozilla.org.
        return Pair(listOf(Session("https://www.mozilla.org")), 0)
    }
}
