/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.gleanplumb

import android.content.Context
import mozilla.components.support.base.log.logger.Logger
import org.json.JSONObject
import org.mozilla.experiments.nimbus.GleanPlumbInterface
import org.mozilla.experiments.nimbus.GleanPlumbMessageHelper
import org.mozilla.experiments.nimbus.internal.FeatureHolder
import org.mozilla.experiments.nimbus.internal.NimbusException
import org.mozilla.fenix.nimbus.Messaging
import org.mozilla.fenix.nimbus.StyleData
import java.util.SortedSet

/**
 * Handles all interactions messages from nimbus.
 */
class MessagesManager(
    private val context: Context,
    private val storage: MessageStorage,
    private val gleanPlumb: GleanPlumbInterface,
    private val messagingFeature: FeatureHolder<Messaging>
) {
    private val nimbusFeature = messagingFeature.value()
    private val logger = Logger("MessagesManager")

    private val customAttributes: JSONObject
        get() = JSONObject()

    // Exposure event
    //
    private val availableMessages: SortedSet<Message> = sortedSetOf(
        Comparator { message1, message2 -> message2.style.priority.compareTo(message1.style.priority) }
    )

    fun areMessagesAvailable(): Boolean {
        //messagingFeature.recordExposure()
        return availableMessages.isNotEmpty()
    }

    fun getNextMessage(): Message? {
        val helper = gleanPlumb.createMessageHelper(customAttributes)
        var message = availableMessages.firstOrNull {
            isMessageEligible(it, helper)
        } ?: return null


        if (isMessageUnderExperiment(message, nimbusFeature.messageUnderExperiment)) {
            messagingFeature.recordExposure()

            if (message.data.isControl) {
                message = availableMessages.firstOrNull {
                    !it.data.isControl && it.triggers.all { condition -> helper.evalJexl(condition) }
                } ?: return null
            }
        }

        return message
    }

    private fun isMessageEligible(
        message: Message,
        helper: GleanPlumbMessageHelper
    ): Boolean {
        return message.triggers.all { condition ->
            try {
                helper.evalJexl(condition)
            } catch (e: NimbusException.EvaluationException) {
                // TODO: report to glean as malformed message
                logger.info("Unable to evaluate $condition")
                false
            }
        }
    }

    fun onMessagePressed(message: Message): String {
        // Update storage
        storage.updateMetadata(
            message.metadata.copy(
                pressed = true
            )
        )
        availableMessages.remove(message)
        val helper = gleanPlumb.createMessageHelper(customAttributes)
        val uuid = helper.getUuid(message.action)
        // TODO: Record uuid metric in glean
        return helper.stringFormat(message.action, uuid)
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

    private fun addMetadata(id: String): Message.Metadata {
        return storage.addMetadata(
            Message.Metadata(
                id = id,
            )
        )
    }

    fun initialize() {
        val nimbusTriggers = nimbusFeature.triggers
        val nimbusStyles = nimbusFeature.styles
        val nimbusActions = nimbusFeature.actions

        val nimbusMessages = nimbusFeature.messages
        val defaultStyle = StyleData(context)
        val storageMetadata = storage.getMetadata().associateBy {
            it.id
        }


        availableMessages.addAll(nimbusMessages.map { (key, value) ->
            val action = if (value.action.startsWith("http")) {
                value.action
            } else {
                nimbusActions[value.action] ?: ""
            }

            Message(
                id = key,
                data = value,
                action = action, // empty or blank
                style = nimbusStyles[value.style] ?: defaultStyle,
                metadata = storageMetadata[key] ?: addMetadata(key),
                triggers = value.trigger.mapNotNull { // empty or blank
                    nimbusTriggers[it]
                }
            )
        }.filter {
            it.data.maxDisplayCount >= it.metadata.displayCount &&
                !it.metadata.dismissed &&
                !it.metadata.pressed
        })
    }

    private fun isMessageUnderExperiment(message: Message, expression: String?): Boolean {
        return when {
            expression.isNullOrBlank() -> {
                false
            }
            expression.endsWith("-") -> {
                message.id.startsWith(expression)
            }
            else -> {
                message.id == expression
            }
        }
    }
}