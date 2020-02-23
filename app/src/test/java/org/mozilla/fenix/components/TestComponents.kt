/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components

import android.content.Context
import io.mockk.mockk
import mozilla.components.support.test.mock
import org.mockito.Mockito.`when`
import org.mozilla.fenix.utils.ClipboardHandler

class TestComponents(private val context: Context) : Components(context) {
    override val backgroundServices by lazy {
        mockk<BackgroundServices>(relaxed = true)
    }
    override val services by lazy { Services(context, backgroundServices.accountManager) }
    override val core by lazy { TestCore(context) }
    override val search by lazy { Search(context) }
    override val useCases by lazy {
        UseCases(
            context,
            core.sessionManager,
            core.store,
            core.engine.settings,
            search.searchEngineManager,
            core.webAppShortcutManager
        )
    }
    override val intentProcessors by lazy {
        val processors: IntentProcessors = mock()
        `when`(processors.externalAppIntentProcessors).thenReturn(emptyList())
        `when`(processors.privateIntentProcessor).thenReturn(mock())
        `when`(processors.intentProcessor).thenReturn(mock())
        `when`(processors.customTabIntentProcessor).thenReturn(mock())
        `when`(processors.privateCustomTabIntentProcessor).thenReturn(mock())
        processors
    }
    override val analytics by lazy { Analytics(context) }

    override val clipboardHandler by lazy { ClipboardHandler(context) }
}
