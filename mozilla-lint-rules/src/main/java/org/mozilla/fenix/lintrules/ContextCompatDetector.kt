/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.lintrules

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.*
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.asJava.getRepresentativeLightMethod
import org.jetbrains.uast.*

class ContextCompatDetector : Detector(), SourceCodeScanner {

    companion object {

        private val Implementation = Implementation(
            ContextCompatDetector::class.java,
            Scope.JAVA_FILE_SCOPE
        )

        val ISSUE_GET_DRAWABLE_CALL = Issue.create(
            id = "UnsafeCompatGetDrawable",
            briefDescription = "TODO", // TODO
            explanation = "TODO", // TODO
            category = Category.CORRECTNESS,
            severity = Severity.ERROR,
            implementation = Implementation
        )

        val ISSUE_GET_COLOR_STATE_LIST_CALL = Issue.create(
            id = "UnsafeCompatGetColorStateList",
            briefDescription = "TODO", // TODO
            explanation = "TODO", // TODO
            category = Category.CORRECTNESS,
            severity = Severity.ERROR,
            implementation = Implementation
        )

        val ISSUES = listOf(
            ISSUE_GET_DRAWABLE_CALL,
            ISSUE_GET_COLOR_STATE_LIST_CALL
        )

    }

    override fun getApplicableUastTypes(): List<Class<out UElement>>? =
        listOf(UCallExpression::class.java)


    override fun createUastHandler(context: JavaContext): UElementHandler? =
        ContextCompatChecker(context)

    override fun getApplicableMethodNames(): List<String>? = listOf(
        "getDrawable",
        "getColorStateList"
    )

}

class ContextCompatChecker(private val context: JavaContext) : UElementHandler() {

    companion object {
        private const val ContextCompatClass = "androidx.core.content.ContextCompat"
    }

    override fun visitCallExpression(node: UCallExpression) {
        val evaluator = context.evaluator

        if (!evaluator.isMemberInClass(node.resolve(), ContextCompatClass)) {
            return
        }

        when (node.methodName) {
            "getDrawable" -> reportGetDrawableCall(context, node)
            "getColorStateList" -> reportGetColorStateListCall(context, node)
        }
    }

    private fun reportGetDrawableCall(context: JavaContext, node: UCallExpression) = context.report(
        ContextCompatDetector.ISSUE_GET_DRAWABLE_CALL,
        context.getLocation(node),
        "This call can lead to crashes, replace with AppCompatResources.getDrawable",
        replaceUnsafeGetDrawableQuickFix(node)
    )

    private fun reportGetColorStateListCall(context: JavaContext, node: UCallExpression) =
        context.report(
            ContextCompatDetector.ISSUE_GET_COLOR_STATE_LIST_CALL,
            context.getLocation(node),
            "This call can lead to crashes, replace with AppCompatResources.getColorStateList",
            replaceUnsafeGetColorStateListCallQuickFix(node)
        )

    private fun replaceUnsafeGetDrawableQuickFix(node: UCallExpression): LintFix {
        val arguments = node.valueArguments.joinToString { it.asSourceString() }
        val newText = "AppCompatResources.getDrawable($arguments)"

        return LintFix.create()
            .name("Replace with AppCompatResources.getDrawable")
            .replace()
            .all()
            .with(newText)
            .build()
    }

    private fun replaceUnsafeGetColorStateListCallQuickFix(node: UCallExpression): LintFix {
        val arguments = node.valueArguments.joinToString { it.asSourceString() }
        val newText = "AppCompatResources.getColorStateList($arguments)"

        return LintFix.create()
            .name("Replace with AppCompatResources.getColorStateList")
            .replace()
            .all()
            .with(newText)
            .build()
    }

}