/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix

import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onRoot
import org.junit.Assert.assertEquals

/**
 * Check recursively all first children starting from the root for one with the indicated testTag.
 * Allows inferring that a particular child composable is all that this composable displays.
 *
 * @param tag TestTag to find in the layout.
 */
fun ComposeContentTestRule.onFirstChildWithTag(tag: String): SemanticsNodeInteraction {
    return getFirstChildWithTag(onRoot(true), tag)
}

/**
 * Assert that the semantic identified by [keyForActual] is set and has the value of [expected].
 *
 * @param expected The expected value of a certain semantic set for this node.
 * @param keyForActual Semantics key set for this node, value of which will be compare to [expected].
 */
fun <T> SemanticsNodeInteraction.assertSemanticsEquals(expected: T, keyForActual: SemanticsPropertyKey<T>) {
    val actual = fetchSemanticsNode().config.getOrElse(keyForActual) {
        throw AssertionError("\"$keyForActual\" cannot be found.")
    }

    assertEquals(expected, actual)
}

private fun getFirstChildWithTag(parent: SemanticsNodeInteraction, tag: String): SemanticsNodeInteraction {
    val firstChild = parent.onChildAt(0)

    firstChild.assertExists("No first child found with tag: \"$tag\"")

    return try {
        firstChild.assert(hasTestTag(tag))
    } catch (e: AssertionError) {
        return getFirstChildWithTag(firstChild, tag)
    }
}
