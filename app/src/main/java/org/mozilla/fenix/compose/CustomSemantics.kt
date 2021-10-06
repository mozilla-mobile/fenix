/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.compose

import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver

/**
 * Custom semantics key for the url this composable uses.
 * No guarantee that the value is set. Users must check everytime the result validity.
 *
 * See [custom-semantics-properties](https://developer.android.com/jetpack/compose/testing#custom-semantics-properties)
 */
val urlKey = SemanticsPropertyKey<String>("url")

/**
 * Custom semantics property for exposing in tests the url this composable uses.
 */
var SemanticsPropertyReceiver.url by urlKey
