package org.mozilla.fenix.components

import mozilla.components.lib.publicsuffixlist.PublicSuffixList

interface IComponents {

    val backgroundServices: BackgroundServices
    val services: Services
    val core: Core
    val search: Search
    val useCases: UseCases
    val utils: Utilities
    val analytics: Analytics
    val publicSuffixList: PublicSuffixList
}
