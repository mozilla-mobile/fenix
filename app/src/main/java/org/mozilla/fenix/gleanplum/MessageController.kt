/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.gleanplum

/**
 * Controls all the interactions with a [Message].
 */
interface MessageController {
    /**
     * Indicates the provided [message] was pressed press by the user.
     */
    fun onMessagePressed(message: Message)

    /**
     * Indicates the provided [message] was dismissed by
     * the user.
     */
    fun onMessageDismissed(message: Message)

    /**
     * Indicates the provided [message] was displayed
     * to the users.
     */
    fun onMessageDisplayed(message: Message)
}