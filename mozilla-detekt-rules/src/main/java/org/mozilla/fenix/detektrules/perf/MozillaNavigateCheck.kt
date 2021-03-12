/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.detektrules.perf

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getCall

private const val VIOLATION_MSG = "Please use `org.mozilla.fenix.ext.NavController.navigateBlockingForAsyncNavGraph` " +
        "instead because using `navigate` directly in the code can lead to the NavGraph not being loaded" +
        "since it relies on a blocking call done in `navigateBlockingForAsyncNavGraph`."

/**
 * A check to prevent the use of `navController.navigate`. Since the NavGraph is loaded dynamically
 * and asynchronously, there is a check in place to make sure the graph is loaded by wrapping the
 * `navigate` function. However, using it directly might lead to code that needs the NavGraph being
 * called before it being inflated.
 */
class MozillaNavigateCheck(config: Config = Config.empty) : Rule(config) {

    override val issue = Issue(
        "MozillaNavigateCheck",
        Severity.Performance,
        "Prevents us from calling `navController.navigate` instead of the functions that" +
                "wrap it",
        Debt.TEN_MINS
    )


    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        val calledMethod = expression.calleeExpression?.firstChild?.node?.chars

        //We check for the navigate method and we have to ignore our extension function file, since
        //we call navigate there
        if (calledMethod == "navigate" && expression.containingFile.name != "NavController.kt") {
            val call = expression.getCall(bindingContext)
            //If we have a local function called navigate, the callElement will be null and we ust
            //ignore it since it is only a local function
            if(call?.callElement?.firstChild != null) {
                report(CodeSmell(issue, Entity.from(expression), VIOLATION_MSG))
            }
        }
    }
}
