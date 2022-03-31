/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.gleanplumb.state

import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.MiddlewareContext
import org.mozilla.fenix.components.appstate.AppAction
import org.mozilla.fenix.components.appstate.AppAction.MessagingAction.ConsumeMessageToShow
import org.mozilla.fenix.components.appstate.AppAction.MessagingAction.Evaluate
import org.mozilla.fenix.components.appstate.AppAction.MessagingAction.MessageClicked
import org.mozilla.fenix.components.appstate.AppAction.MessagingAction.MessageDismissed
import org.mozilla.fenix.components.appstate.AppAction.MessagingAction.MessageDisplayed
import org.mozilla.fenix.components.appstate.AppAction.MessagingAction.Restore
import org.mozilla.fenix.components.appstate.AppAction.MessagingAction.UpdateMessageToShow
import org.mozilla.fenix.components.appstate.AppAction.MessagingAction.UpdateMessages
import org.mozilla.fenix.components.appstate.AppState
import org.mozilla.fenix.gleanplumb.Message
import org.mozilla.fenix.gleanplumb.NimbusMessagingStorage

typealias AppStoreMiddlewareContext = MiddlewareContext<AppState, AppAction>

class MessagingMiddleware(
    private val messagingStorage: NimbusMessagingStorage,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
) : Middleware<AppState, AppAction> {

    override fun invoke(
        context: AppStoreMiddlewareContext,
        next: (AppAction) -> Unit,
        action: AppAction
    ) {
        when (action) {
            is Restore -> {
                coroutineScope.launch {
                    val messages = messagingStorage.getMessages()
                    context.store.dispatch(UpdateMessages(messages))
                }
            }

            is Evaluate -> {
                val message = messagingStorage.getNextMessage(context.state.messaging.messages)
                if (message != null) {
                    context.dispatch(UpdateMessageToShow(message))
                } else {
                    context.dispatch(ConsumeMessageToShow)
                }
            }

            is MessageClicked -> onMessageClicked(action.message, context)

            is MessageDismissed -> onMessageDismissed(context, action.message)

            is MessageDisplayed -> onMessagedDisplayed(action.message, context)
        }
        next(action)
    }

    @VisibleForTesting
    internal fun onMessagedDisplayed(
        oldMessage: Message,
        context: AppStoreMiddlewareContext
    ) {
        val newMetadata = oldMessage.metadata.copy(
            displayCount = oldMessage.metadata.displayCount + 1,
            lastTimeShown = now()
        )
        val newMessage = oldMessage.copy(
            metadata = newMetadata
        )
        val newMessages = if (newMetadata.displayCount < oldMessage.maxDisplayCount) {
            updateMessage(context, oldMessage, newMessage)
        } else {
            consumeMessageToShowIfNeeded(context, oldMessage)
            removeMessage(context, oldMessage)
        }
        context.dispatch(UpdateMessages(newMessages))
        coroutineScope.launch {
            messagingStorage.updateMetadata(newMetadata)
        }
    }

    @VisibleForTesting
    internal fun onMessageDismissed(
        context: AppStoreMiddlewareContext,
        message: Message
    ) {
        val newMessages = removeMessage(context, message)
        context.dispatch(UpdateMessages(newMessages))
        consumeMessageToShowIfNeeded(context, message)
        coroutineScope.launch {
            val updatedMetadata = message.metadata.copy(dismissed = true)
            messagingStorage.updateMetadata(updatedMetadata)
        }
    }

    @VisibleForTesting
    internal fun onMessageClicked(
        message: Message,
        context: AppStoreMiddlewareContext
    ) {
        // Update Nimbus storage.
        coroutineScope.launch {
            val updatedMetadata = message.metadata.copy(pressed = true)
            messagingStorage.updateMetadata(updatedMetadata)
        }
        // Update app state.
        val newMessages = removeMessage(context, message)
        context.dispatch(UpdateMessages(newMessages))
        consumeMessageToShowIfNeeded(context, message)
    }

    @VisibleForTesting
    internal fun consumeMessageToShowIfNeeded(
        context: AppStoreMiddlewareContext,
        message: Message
    ) {
        if (context.state.messaging.messageToShow?.id == message.id) {
            context.dispatch(ConsumeMessageToShow)
        }
    }

    @VisibleForTesting
    internal fun removeMessage(
        context: AppStoreMiddlewareContext,
        message: Message
    ): List<Message> {
        return context.state.messaging.messages.filter { it.id != message.id }
    }

    @VisibleForTesting
    internal fun updateMessage(
        context: AppStoreMiddlewareContext,
        oldMessage: Message,
        updatedMessage: Message
    ): List<Message> {
        val actualMessageToShow = context.state.messaging.messageToShow

        if (actualMessageToShow?.id == oldMessage.id) {
            context.dispatch(UpdateMessageToShow(updatedMessage))
        }
        return removeMessage(context, oldMessage) + updatedMessage
    }

    @VisibleForTesting
    internal fun now(): Long = System.currentTimeMillis()
}
