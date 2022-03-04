/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.gleanplum

interface MessageStorage {
    /**
     * Provide all the message metadata saved in the storage.
     */
    fun getMetadata(): List<MessageMetadata>

    /**
     * Given a [metadata] update the message metadata on the storage.
     */
    fun updateMetadata(metadata: MessageMetadata)

}