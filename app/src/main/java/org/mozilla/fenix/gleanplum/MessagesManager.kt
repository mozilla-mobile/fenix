package org.mozilla.fenix.gleanplum

import android.content.Context
import org.mozilla.experiments.nimbus.internal.FeatureHolder
import org.mozilla.fenix.nimbus.Messaging
import org.mozilla.fenix.nimbus.StyleData
import java.util.SortedSet
import kotlin.Comparator

class MessagesManager(
    private val context: Context,
    private val storage: MessageStorage,
    private val messagingFeature: FeatureHolder<Messaging>
) : MessageController {

    private val availableMessages: SortedSet<Message> = sortedSetOf(
        Comparator { message1, message2 -> message2.style.priority.compareTo(message1.style.priority) }
    )

    override fun getNextMessage(): Message? {
        return availableMessages.first()
    }

    override fun onMessagePressed(message: Message) {
        // TODO: Report telemetry event
        storage.updateMetadata(
            message.metadata.copy(
                pressed = true
            )
        )
        availableMessages.remove(message)
    }

    override fun onMessageDismissed(message: Message) {
        // TODO: Report telemetry event
        storage.updateMetadata(
            message.metadata.copy(
                dismissed = true
            )
        )
        availableMessages.remove(message)
    }

    override fun onMessageDisplayed(message: Message) {
        // TODO: Report telemetry event
        val newMetadata = message.metadata.copy(
            displayCount = message.metadata.displayCount + 1
        )
        val newMessage = message.copy(
            metadata = newMetadata
        )

        storage.updateMetadata(newMetadata)
        availableMessages.remove(message)
        availableMessages.add(newMessage)
    }

    override fun initialize() {
        val nimbusTriggers = messagingFeature.value().triggers
        val nimbusStyles = messagingFeature.value().styles
        val nimbusMessages = messagingFeature.value().messages
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