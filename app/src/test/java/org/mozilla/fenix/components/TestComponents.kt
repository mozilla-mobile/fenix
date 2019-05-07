package org.mozilla.fenix.components

import android.content.Context
import io.mockk.mockk

class TestComponents(private val context: Context) : Components(context) {
    override val backgroundServices by lazy {
        mockk<BackgroundServices>(relaxed = true)
    }
    override val services by lazy { Services(backgroundServices.accountManager, useCases.tabsUseCases) }
    override val core by lazy { TestCore(context) }
    override val search by lazy { Search(context) }
    override val useCases by lazy { UseCases(context, core.sessionManager, search.searchEngineManager) }
    override val utils by lazy {
        Utilities(
            context,
            core.sessionManager,
            useCases.sessionUseCases,
            useCases.searchUseCases
        )
    }
    override val analytics by lazy { Analytics(context) }
    override val storage by lazy { Storage(context) }
}