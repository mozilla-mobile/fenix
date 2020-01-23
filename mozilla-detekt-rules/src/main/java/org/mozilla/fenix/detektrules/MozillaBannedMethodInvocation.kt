/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.detektrules

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.com.intellij.openapi.util.Key
import java.util.regex.PatternSyntaxException

class MozillaBannedMethodInvocation(config: Config = Config.empty) : MozillaRule(config) {
    companion object {
        const val DESCR = "Invoking banned method"
    }

    val checkedMethodKey = Key<Boolean>("CheckedMethod")

    override val issue = Issue(
        "MozillaBannedMethodInvocation",
        Severity.Defect,
        DESCR,
        Debt.FIVE_MINS
    )

    private lateinit var banned: MutableList<Regex>
    private var configOk = true

    override fun validate(): Boolean {
        return configOk
    }

    override fun configure() {
        // Initialize the banned list from the user-specified configuration.
        configOk = true
        banned = mutableListOf()
        val bannedMethodInvocationsList = valueOrDefault("bannedMethods", "").replace(" ", "")
        if (!bannedMethodInvocationsList.contentEquals("")) {
            bannedMethodInvocationsList.split(",").forEach {
                try {
                    val r = Regex(
                        it,
                        RegexOption.DOT_MATCHES_ALL
                    )
                    banned.add(r)

                } catch (pse: PatternSyntaxException) {
                    // Log a warning here about an invalid regular expression.
                    MozillaLogger.logInfo("MozillaBannedMethodInvocation: " +
                            "$it is an invalid description of a banned method. " +
                            "It will not be used.")
                    // It is just this method description that will not be included. Others might still be valid.
                    configOk = true
                }
            }
        }
    }

    /**
     * Issue a CodeSmell report if the invoked method matches a banned method
     *
     * If a method is invoked that is banned, issue a CodeSmell report.
     *
     * @param possiblyBannedMethodInvocation The invoked method to compare against the banned list
     * @param expression The expression from where this invocation came. It is used to provide context to the report.
     *
     * @return None
     */
    private fun reportIfBanned(
        possiblyBannedMethodInvocation: CharSequence,
        expression: KtExpression
    ) {
        banned.filter { it.matches(possiblyBannedMethodInvocation) }.map {
            CodeSmell(
                issue,
                Entity.from(expression),
                "Using $possiblyBannedMethodInvocation is not allowed because invoking " +
                        "${it.pattern} is against Mozilla policy. " +
                        "See 'mozilla-detekt-rules' stanza in 'config/detekt.yml' for more information.\n"
            )
        }.forEach { report(it) }
    }

    override fun visitQualifiedExpression(expression: KtQualifiedExpression) {
        var possiblyBannedMethodInvocation: String? = null

        // Separate the call (the selector) from the receiver. We store this so
        // we can later mark it as checked if we actually checked.
        var callDescendant: KtElement? = null
        if (expression.selectorExpression != null && expression.selectorExpression is KtCallElement) {
            callDescendant = expression.selectorExpression
        }

        // All we know at this point is that this expression is a simple qualified expression.
        // There are two ways to determine if this is a function call:

        // 1. If there is a binding context and the expression resolves to a function
        // call in that context, we know it is a function call.
        if (bindingContext != BindingContext.EMPTY) {
            possiblyBannedMethodInvocation =
                expression.getResolvedCall(bindingContext)?.resultingDescriptor?.fqNameOrNull()
                    ?.asString()
        }

        // 2. If the selector is a function call, then we know it is a function call.
        // Note: We do not want to override what we found above, if anything. Also, we
        // only have to check a single selector because chained method invocations will
        // result in multiple calls to this function.
        if (callDescendant != null &&
            (possiblyBannedMethodInvocation == null || possiblyBannedMethodInvocation == "")
        ) {
            // In order to create the literal against which we will check for a match in the banned list,
            // we have to recreate from the receiver and the selector. This is necessary for handling
            // chained method invocations.
            possiblyBannedMethodInvocation =
                expression.receiverExpression.node.chars.toString() + "." + callDescendant.node.chars.toString()
        }

        // If either of those two cases is true, then we will check to see if invoking this
        // function is banned. Mark that we have checked this function here so that we do not
        // recheck when visitCallExpression() is invoked on this expression.
        if (possiblyBannedMethodInvocation != null) {
            callDescendant?.putUserData(checkedMethodKey, true)
            reportIfBanned(possiblyBannedMethodInvocation, expression)
        }

        // Visit our children after we do our work. This makes it possible to qualified function
        // invocations before checking the function itself later. We always prefer checking the
        // most fully-qualified name against the banned list.
        super.visitQualifiedExpression(expression)
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        // Check first to see if this expression has already been checked!
        if (expression.getUserData(checkedMethodKey) == true) return

        // Start by assuming that we will check whether the string of characters
        // represented by this expression (which is a function call), is banned.
        var possiblyBannedMethodInvocation = expression.node.chars.toString()

        // If we can resolve this function call into it's fully-qualified name,
        // then let's do that and use that as the basis for the comparison. Remember,
        // we always prefer checking whether the most fully-qualified name is banned.
        if (bindingContext != BindingContext.EMPTY) {
            val resolvedCall = expression.getResolvedCall(bindingContext)
            possiblyBannedMethodInvocation =
                resolvedCall?.resultingDescriptor?.fqNameOrNull()?.asString()
                    ?: possiblyBannedMethodInvocation
        }
        reportIfBanned(possiblyBannedMethodInvocation, expression)
    }
}
