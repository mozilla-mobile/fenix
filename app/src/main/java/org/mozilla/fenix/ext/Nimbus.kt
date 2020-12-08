/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ext

import mozilla.components.service.nimbus.NimbusApi

/**
 * Gets the branch of the given `experimentId` and transforms it with given closure.
 *
 * If we're enrolled in the experiment, the transform is passed the branch id/slug as a `String`.
 *
 * If we're not enrolled in the experiment, or the experiment is not valid then the transform
 * is passed a `null`.
 */
fun <T> NimbusApi.withExperiment(experimentId: String, transform: (String?) -> T) =
    this.getExperimentBranch(experimentId).let(transform)

/**
 * The degenerate case of `withExperiment(String, (String?) -> T))`, with an identity transform.
 *
 * Short-hand for `mozilla.components.service.nimbus.NimbusApi.getExperimentBranch`.
 */
fun NimbusApi.withExperiment(experimentId: String) =
    this.getExperimentBranch(experimentId)
