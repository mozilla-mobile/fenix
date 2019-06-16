package org.mozilla.fenix.components

class TestComponents : IComponents {

    override val backgroundServices get() = notDefined()
    override val services get() = notDefined()
    override val core get() = notDefined()
    override val search get() = notDefined()
    override val useCases get() = notDefined()
    override val utils get() = notDefined()
    override val analytics get() = notDefined()
    override val publicSuffixList get() = notDefined()
}

private fun notDefined(): Nothing = throw NotImplementedError("Test component not provided")
