package org.mozilla.fenix.detektrules

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Rule

abstract class MozillaRule(config: Config = Config.empty): Rule(config) {
    /**
     * Validate this rule's configuration
     *
     * Check whether or not this custom Detekt rule's configuration is valid. This is
     * called after the rule is configured.
     *
     * [MozillaRule]s with invalid configurations are not activated.
     *
     * @return A [Boolean]indicating whether the configuration is valid or not. Invalid
     * configurations will de-activate the rule.
     */
    open fun validate(): Boolean = true

    open fun configure(): Unit {

    }
}
