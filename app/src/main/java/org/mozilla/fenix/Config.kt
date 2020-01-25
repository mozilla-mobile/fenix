/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix

enum class ReleaseChannel {
    FenixDebug,

    FenixNightly,
    FenixBeta,
    FenixProduction,

    FennecProduction,
    FennecBeta,
    FennecNightly;

    val isReleased: Boolean
        get() = when (this) {
            FenixDebug -> false
            else -> true
        }

    /**
     * True if this is a debug release channel, false otherwise.
     *
     * This constant should often be used instead of [BuildConfig.DEBUG], which indicates
     * if the `debuggable` flag is set which can be true even on released channel builds
     * (e.g. performance).
     */
    val isDebug: Boolean
        get() = !this.isReleased

    val isReleaseOrBeta: Boolean
        get() = when (this) {
            FenixProduction -> true
            FenixBeta -> true
            FennecProduction -> true
            FennecBeta -> true
            else -> false
        }

    val isBeta: Boolean
        get() = when (this) {
            FennecBeta -> true
            FenixBeta -> true
            else -> false
        }

    val isNightlyOrDebug: Boolean
        get() = when (this) {
            FenixNightly -> true
            FennecNightly -> true
            FenixDebug -> true
            else -> false
        }

    val isFennec: Boolean
        get() = this in fennecChannels

    val isFenix: Boolean
        get() = !isFennec
}

object Config {
    val channel = when (BuildConfig.BUILD_TYPE) {
        "fenixProduction" -> ReleaseChannel.FenixProduction
        "fenixBeta" -> ReleaseChannel.FenixBeta
        "fenixNightly" -> ReleaseChannel.FenixNightly
        "debug" -> ReleaseChannel.FenixDebug
        "fennecProduction" -> ReleaseChannel.FennecProduction
        "fennecBeta" -> ReleaseChannel.FennecBeta
        "fennecNightly" -> ReleaseChannel.FennecNightly

        // Builds for local performance analysis, recording benchmarks, automation, etc.
        // This should be treated like a released channel because we want to test
        // what users experience and there are performance-impacting changes in debug
        // release channels (e.g. logging) that are never intended to be shipped.
        "forPerformanceTest" -> ReleaseChannel.FenixProduction

        else -> {
            throw IllegalStateException("Unknown build type: ${BuildConfig.BUILD_TYPE}")
        }
    }
}

private val fennecChannels: List<ReleaseChannel> = listOf(
    ReleaseChannel.FennecNightly,
    ReleaseChannel.FennecBeta,
    ReleaseChannel.FennecProduction
)
