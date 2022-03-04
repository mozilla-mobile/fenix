/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.gleanplum

import org.mozilla.fenix.nimbus.MessageData
import org.mozilla.fenix.nimbus.StyleData

data class Message(
    val id: String,
    val data: MessageData,
    val style: StyleData,
    val triggers: List<String>,
    val metadata: MessageMetadata
)

data class MessageMetadata(
    val id: String,
    val displayCount: Int,
    val pressed: Boolean,
    val dismissed: Boolean
    // TODO: Add more specific info that it's not provide by nimbus, values that are handle in the app.
)