/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Counts the number of runBlocking calls made
 */
private class RunblockingCounter{
    companion object{
        var runBlockingCount = 0
    }
}

/**
 * Wrapper around `runBlocking`
 */
fun <T> runBlockingCounter(context: CoroutineContext = EmptyCoroutineContext, action: suspend CoroutineScope.() -> T): T {
    RunblockingCounter.runBlockingCount += 1
    if(context != EmptyCoroutineContext){
        return runBlocking(context) { action()}
    }
    return runBlocking { action() }
}
