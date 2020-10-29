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
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext

/**
 * Counts the number of runBlocking calls made
 */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
object RunBlockingCounter {
    var count = AtomicInteger(0)

    fun incrementCounter() {
        var prev: Int
        var next: Int
        do {
            prev = count.get()
            next = prev + 1
            if (next == Integer.MAX_VALUE) next = 0
        } while (!count.compareAndSet(prev, next))
    }
}

/**
 * Wrapper around `runBlocking`. RunBlocking seems to be a "fix-all" to return values to the thread
 * where the coroutine is called. The official doc explains runBlocking:  "Runs a new coroutine and
 * blocks the current thread interruptibly until its completion`. This can have negative
 * side-effects on the our main thread which could lead to significant jank. This wrapper aims to
 * count the number of runBlocking call to try to limit them as much as possible to encourage
 * alternatives solutions whenever this function might be needed.
 */
fun <T> runBlockingIncrement(
    context: CoroutineContext? = null,
    action: suspend CoroutineScope.() -> T
): T {
    RunBlockingCounter.incrementCounter()

    return if (context != null) {
        runBlocking(context) { action() }
    } else {
        runBlocking { action() }
    }
}
