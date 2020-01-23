/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.detektrules

import io.gitlab.arturbosch.detekt.test.lint
import io.gitlab.arturbosch.detekt.api.YamlConfig
import io.gitlab.arturbosch.detekt.test.KtTestCompiler
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

internal class MozillaBannedMethodInvocationTest {
    @Test
    internal fun `match of banned method qualified invocation should warn`() {
        var rule = MozillaBannedMethodInvocation(YamlConfig.loadResource(this.javaClass.getResource("/config.yml")))
        rule.configure()
        rule.validate()

        val contextualFindings =
            rule.compileAndLintWithContext(
                KtTestCompiler.createEnvironment().env,
                NONCOMPLIANT_QUALIFIED_INVOCATION.trimIndent()
            )
        assertThat(contextualFindings).hasSize(1)
        assertThat(contextualFindings[0].issue.description).isEqualTo(MozillaBannedMethodInvocation.DESCR)

        val contextFreeFindings =
            rule.lint(
                NONCOMPLIANT_QUALIFIED_INVOCATION.trimIndent()
            )
        assertThat(contextFreeFindings).hasSize(1)
        assertThat(contextFreeFindings[0].issue.description).isEqualTo(MozillaBannedMethodInvocation.DESCR)
    }

    @Test
    internal fun `match of chained banned method qualified invocation through allowed method qualified invocation should warn if there is a context`() {
        var rule = MozillaBannedMethodInvocation(YamlConfig.loadResource(this.javaClass.getResource("/config.yml")))
        rule.configure()
        rule.validate()

        val contextualFindings =
            rule.compileAndLintWithContext(
                KtTestCompiler.createEnvironment().env,
                NONCOMPLIANT_CHAINED_QUALIFIED_INVOCATION.trimIndent()
            )
        assertThat(contextualFindings).hasSize(1)
        assertThat(contextualFindings[0].issue.description).isEqualTo(MozillaBannedMethodInvocation.DESCR)

        // This will not warn because "Foo.allowedMethod().bannedMethodY()" will be checked literally
        // because there is no resolution context to determine that bannedMethodY is actually
        // TestFoo.Foo.bannedMethodY. Our regular expression does not match in this case.
        val contextFreeFindings =
            rule.lint(
                NONCOMPLIANT_CHAINED_QUALIFIED_INVOCATION.trimIndent()
            )
        assertThat(contextFreeFindings).hasSize(0)
    }

    @Test
    internal fun `match of banned method direct invocation should warn`() {
        var rule = MozillaBannedMethodInvocation(YamlConfig.loadResource(this.javaClass.getResource("/config.yml")))
        rule.configure()
        rule.validate()

        val contextualFindings =
            rule.compileAndLintWithContext(
                KtTestCompiler.createEnvironment().env,
                NONCOMPLIANT_DIRECT_INVOCATION.trimIndent()
            )
        assertThat(contextualFindings).hasSize(1)
        assertThat(contextualFindings[0].issue.description).isEqualTo(MozillaBannedMethodInvocation.DESCR)

        val contextFreeFindings =
            rule.lint(
                NONCOMPLIANT_DIRECT_INVOCATION.trimIndent()
            )
        assertThat(contextFreeFindings).hasSize(1)
    }

    @DisplayName("compliant ")
    @MethodSource("compliantProvider")
    @ParameterizedTest(name = "{1} should not warn")
    internal fun testCompliantWhen(source: String) {
        var rule = MozillaBannedMethodInvocation(YamlConfig.loadResource(this.javaClass.getResource("/config.yml")))
        rule.configure()
        rule.validate()

        val findings =
            rule.compileAndLintWithContext(
                KtTestCompiler.createEnvironment().env,
                source
            )
        assertThat(findings).isEmpty()
    }

    companion object {
        @JvmStatic
        fun compliantProvider(): Stream<Arguments> =
            Stream.of(
                arguments(COMPLIANT_QUALIFIED_INVOCATION, "Allowed qualified method invocation"),
                arguments(COMPLIANT_DIRECT_INVOCATION, "Allowed direct method invocation"),
                arguments(COMPLIANT_MISCONFIGURED_INVOCATION, "match of misconfigured banned method direct invocation")
            )
    }
}

const val NONCOMPLIANT_QUALIFIED_INVOCATION = """
        class TestFoo {
            lateinit var x: String
            object Foo {
                val property: Boolean = false
                fun bannedMethodX(): Boolean {
                    return false
                }
                fun bannedMethodY(): Boolean {
                    return false
                }
                fun allowedMethod(): Foo {
                    return this
                }
            }
            init {
                if (Foo.bannedMethodX()) {
                    x = "banned"
                }
                if (Foo.property) {
                    x = "banned"
                }
            }
        }
        """
const val NONCOMPLIANT_CHAINED_QUALIFIED_INVOCATION = """
        class TestFoo {
            lateinit var x: String
            object Foo {
                val property: Boolean = false
                fun bannedMethodX(): Boolean {
                    return false
                }
                fun bannedMethodY(): Boolean {
                    return false
                }
                fun allowedMethod(): Foo {
                    return this
                }
            }
            init {
                if (Foo.allowedMethod().bannedMethodY()) {
                    x = "banned"
                }
                if (Foo.property) {
                    x = "banned"
                }
            }
        }
        """
const val COMPLIANT_QUALIFIED_INVOCATION = """
        class TestFoo {
            lateinit var x: String
            object Foo {
                val property: Boolean = false
                fun bannedMethodX(): Boolean {
                    return false
                }
                fun bannedMethodY(): Boolean {
                    return false
                }
                fun allowedMethod(): Foo {
                    return this
                }
            }
            init {
                if (Foo.allowedMethod()) {
                    x = "banned"
                }
                if (Foo.property) {
                    x = "banned"
                }
            }
        }
        """
const val NONCOMPLIANT_DIRECT_INVOCATION = """
        var x: String = ""
        fun forbidden(): Boolean {
            return true
        }
        fun doer() {
            if (forbidden()) {
                x = "allowed"
            }
        }
        fun doer() {
            if (allowed()) {
                x = "allowed"
            }
        }
        """
const val COMPLIANT_DIRECT_INVOCATION = """
        var x: String = ""
        fun allowed(): Boolean {
            return true
        }
        fun doer() {
            if (allowed()) {
                x = "allowed"
            }
        }
        """
const val COMPLIANT_MISCONFIGURED_INVOCATION = """
        var x: String = ""
        fun misconfigured(): Boolean {
            return true
        }
        fun doer() {
            if (misconfigured()) {
                x = "allowed"
            }
        }
        """
