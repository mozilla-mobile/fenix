/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.detektrules

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider
import org.mozilla.fenix.detektrules.perf.MozillaBannedPropertyAccess
import org.mozilla.fenix.detektrules.perf.MozillaNavigateCheck
import org.mozilla.fenix.detektrules.perf.MozillaStrictModeSuppression
import org.mozilla.fenix.detektrules.perf.MozillaRunBlockingCheck
import org.mozilla.fenix.detektrules.perf.MozillaUseLazyMonitored

class CustomRulesetProvider : RuleSetProvider {
    override val ruleSetId: String = "mozilla-detekt-rules"

    override fun instance(config: Config): RuleSet = RuleSet(
        ruleSetId,
        listOf(
            MozillaBannedPropertyAccess(config),
            MozillaStrictModeSuppression(config),
            MozillaCorrectUnitTestRunner(config),
            MozillaRunBlockingCheck(config),
            MozillaUseLazyMonitored(config),
            MozillaNavigateCheck(config)
        )
    )
}
