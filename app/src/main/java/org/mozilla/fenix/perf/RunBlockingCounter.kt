/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

// This class implements the alternative ways to invoke runBlocking with some
// monitoring by wrapping the raw methods. This lint check tells us not to use the raw
// methods so we suppress the check.
@file:Suppress("MozillaRunBlockingCheck")

package org.mozilla.fenix.perf

import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Counts the number of runBlocking calls made
 */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
object RunBlockingCounter {
    var count = 0L
}

/**
 * Wrapper around `runBlocking`
 */
fun <T> runBlockingIncrement(
    context: CoroutineContext = EmptyCoroutineContext,
    action: suspend CoroutineScope.() -> T
): T {
    RunBlockingCounter.count += 1
    if (context != EmptyCoroutineContext) {
        return runBlocking(context) { action() }
    }
    return runBlocking { action() }
}
