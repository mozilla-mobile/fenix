/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.gleanplumb

import android.app.Activity
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mozilla.components.support.base.ids.SharedIdsHelper
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.nimbus.FxNimbus
import org.mozilla.fenix.nimbus.MessageSurfaceId
import org.mozilla.fenix.onboarding.MARKETING_CHANNEL_ID
import org.mozilla.fenix.utils.BootUtils
import org.mozilla.fenix.utils.IntentUtils
import org.mozilla.fenix.utils.createBaseNotification
import java.util.concurrent.TimeUnit

const val CLICKED_MESSAGE_ID = "clickedMessageId"
const val DISMISSED_MESSAGE_ID = "dismissedMessageId"

/**
 * Background [Worker] that polls Nimbus for available [Message]s at a given interval.
 * A [Notification] will be created using the configuration of the next highest priority [Message]
 * if it has not already been displayed.
 */
class MessageNotificationWorker(
    context: Context,
    workerParameters: WorkerParameters,
) : Worker(context, workerParameters) {

    @OptIn(DelicateCoroutinesApi::class) // GlobalScope usage.
    override fun doWork(): Result {
        GlobalScope.launch(Dispatchers.IO) {
            val context = applicationContext
            val messagingStorage = context.components.analytics.messagingStorage
            val messages = messagingStorage.getMessages()
            val nextMessage =
                messagingStorage.getNextMessage(MessageSurfaceId.NOTIFICATION, messages)
                    ?: return@launch

            val currentBootUniqueIdentifier = BootUtils.getBootIdentifier(context)
            val messageMetadata = nextMessage.metadata
            //  Device has NOT been power cycled.
            if (messageMetadata.latestBootIdentifier == currentBootUniqueIdentifier) {
                return@launch
            }

            val nimbusMessagingController = NimbusMessagingController(messagingStorage)

            // Update message as displayed.
            val updatedMessage =
                nimbusMessagingController.updateMessageAsDisplayed(
                    nextMessage,
                    currentBootUniqueIdentifier,
                )
            nimbusMessagingController.onMessageDisplayed(updatedMessage)

            NotificationManagerCompat.from(context).notify(
                MESSAGE_TAG,
                SharedIdsHelper.getIdForTag(context, updatedMessage.id),
                buildNotification(
                    context,
                    updatedMessage,
                ),
            )
        }

        return Result.success()
    }

    private fun buildNotification(
        context: Context,
        message: Message,
    ): Notification {
        val onClickPendingIntent = createOnClickPendingIntent(context, message)
        val onDismissPendingIntent = createOnDismissPendingIntent(context, message)

        return createBaseNotification(
            context,
            MARKETING_CHANNEL_ID,
            message.data.title,
            message.data.text,
            onClickPendingIntent,
            onDismissPendingIntent,
        )
    }

    private fun createOnClickPendingIntent(
        context: Context,
        message: Message,
    ): PendingIntent {
        val intent = Intent(context, NotificationClickedReceiverActivity::class.java)
        intent.putExtra(CLICKED_MESSAGE_ID, message.id)
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)

        // Activity intent.
        return PendingIntent.getActivity(
            context,
            SharedIdsHelper.getNextIdForTag(context, NOTIFICATION_PENDING_INTENT_TAG),
            intent,
            IntentUtils.defaultIntentPendingFlags,
        )
    }

    private fun createOnDismissPendingIntent(
        context: Context,
        message: Message,
    ): PendingIntent {
        val intent = Intent(context, NotificationDismissedService::class.java)
        intent.putExtra(DISMISSED_MESSAGE_ID, message.id)

        // Service intent.
        return PendingIntent.getService(
            context,
            SharedIdsHelper.getNextIdForTag(context, NOTIFICATION_PENDING_INTENT_TAG),
            intent,
            IntentUtils.defaultIntentPendingFlags,
        )
    }

    companion object {
        private const val NOTIFICATION_PENDING_INTENT_TAG = "org.mozilla.fenix.message"
        private const val MESSAGE_TAG = "org.mozilla.fenix.message.tag"
        private const val MESSAGE_WORK_NAME = "org.mozilla.fenix.message.work"

        /**
         * Initialize the [Worker] to begin polling Nimbus.
         */
        fun setMessageNotificationWorker(context: Context) {
            val featureConfig = FxNimbus.features.messaging.value()
            val notificationConfig = featureConfig.notificationConfig
            val pollingInterval = notificationConfig.refreshInterval.toLong()

            val messageWorkRequest = PeriodicWorkRequest.Builder(
                MessageNotificationWorker::class.java,
                pollingInterval,
                TimeUnit.MINUTES,
            ) // Only start polling after the given interval.
                .setInitialDelay(pollingInterval, TimeUnit.MINUTES)
                .build()

            val instanceWorkManager = WorkManager.getInstance(context)
            instanceWorkManager.enqueueUniquePeriodicWork(
                MESSAGE_WORK_NAME,
                // We want to keep any existing scheduled work.
                ExistingPeriodicWorkPolicy.KEEP,
                messageWorkRequest,
            )
        }
    }
}

/**
 * When a [Message] [Notification] is dismissed by the user record telemetry data and update the
 * [Message.metadata].
 *
 * This [Service] is only intended to be used by the [MessageNotificationWorker.createOnDismissPendingIntent] function.
 */
class NotificationDismissedService : Service() {

    /**
     * This service cannot be bound to.
     */
    override fun onBind(intent: Intent?): IBinder? = null

    @OptIn(DelicateCoroutinesApi::class) // GlobalScope usage.
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        GlobalScope.launch {
            if (intent != null) {
                val nimbusMessagingController =
                    NimbusMessagingController(applicationContext.components.analytics.messagingStorage)

                // Get the relevant message.
                val messageId = intent.getStringExtra(DISMISSED_MESSAGE_ID)!!
                val message = nimbusMessagingController.getMessage(messageId)

                if (message != null) {
                    // Update message as 'dismissed'.
                    nimbusMessagingController.onMessageDismissed(message.metadata)
                }
            }
        }

        return START_REDELIVER_INTENT
    }
}

/**
 * When a [Message] [Notification] is clicked by the user record telemetry data and update the
 * [Message.metadata].
 *
 * This [Activity] is only intended to be used by the [MessageNotificationWorker.createOnClickPendingIntent] function.
 */
class NotificationClickedReceiverActivity : Activity() {

    @OptIn(DelicateCoroutinesApi::class) // GlobalScope usage.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        GlobalScope.launch {
            val nimbusMessagingController =
                NimbusMessagingController(components.analytics.messagingStorage)

            // Get the relevant message.
            val messageId = intent.getStringExtra(CLICKED_MESSAGE_ID)!!
            val message = nimbusMessagingController.getMessage(messageId)

            if (message != null) {
                // Update message as 'clicked'.
                nimbusMessagingController.onMessageClicked(message.metadata)

                // Create the intent.
                val intent = nimbusMessagingController.getIntentForMessageAction(message.action)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

                // Start the message intent.
                startActivity(intent)
            }
        }

        // End this activity.
        finish()
    }
}
