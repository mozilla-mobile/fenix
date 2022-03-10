/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.gleanplum

import android.content.Context
import androidx.core.net.toUri
import org.mozilla.experiments.nimbus.internal.FeatureHolder
import org.mozilla.fenix.BuildConfig
import org.mozilla.fenix.nimbus.Messaging
import org.mozilla.fenix.nimbus.StyleData
import java.util.SortedSet
import kotlin.Comparator

/**
 * Handles all interactions messages from nimbus.
 */
class MessagesManager(
    private val context: Context,
    private val storage: MessageStorage,
    private val messagingFeature: FeatureHolder<Messaging>
) {

    private val availableMessages: SortedSet<Message> = sortedSetOf(
        Comparator { message1, message2 -> message2.style.priority.compareTo(message1.style.priority) }
    )

    fun areMessagesAvailable(): Boolean {
        return availableMessages.isNotEmpty()
    }

    fun getNextMessage(): Message? {
        return availableMessages.first()
    }

    fun onMessagePressed(message: Message) {
        // Update storage
        storage.updateMetadata(
            message.metadata.copy(
                pressed = true
            )
        )
        availableMessages.remove(message)
    }

    fun onMessageDismissed(message: Message) {
        storage.updateMetadata(
            message.metadata.copy(
                dismissed = true
            )
        )
        availableMessages.remove(message)
    }

    fun onMessageDisplayed(message: Message) {
        val newMetadata = message.metadata.copy(
            displayCount = message.metadata.displayCount + 1
        )
        val newMessage = message.copy(
            metadata = newMetadata
        )

        storage.updateMetadata(newMetadata)
        availableMessages.remove(message)

        if (newMetadata.displayCount < message.data.maxDisplayCount) {
            availableMessages.add(newMessage)
        }
    }

    fun initialize() {
        val nimbusFeature = messagingFeature.value()
        val nimbusTriggers = nimbusFeature.triggers
        val nimbusStyles = nimbusFeature.styles

        val nimbusMessages = nimbusFeature.messages
        val defaultStyle = StyleData(context)
        val storageMetadata = storage.getMetadata().associateBy {
            it.id
        }

        availableMessages.addAll(nimbusMessages.map { (key, value) ->
            Message(
                id = key,
                data = value,
                style = nimbusStyles[value.style] ?: defaultStyle,
                metadata = storageMetadata[key]!!,
                triggers = value.trigger.mapNotNull {
                    nimbusTriggers[it]
                }
            )
        }.filter {
            it.data.maxDisplayCount < it.metadata.displayCount &&
                    !it.metadata.dismissed &&
                    !it.metadata.pressed
        })
    }
}