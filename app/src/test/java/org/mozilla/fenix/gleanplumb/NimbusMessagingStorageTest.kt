/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.gleanplumb

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.experiments.nimbus.GleanPlumbInterface
import org.mozilla.experiments.nimbus.GleanPlumbMessageHelper
import org.mozilla.experiments.nimbus.internal.FeatureHolder
import org.mozilla.experiments.nimbus.internal.NimbusException
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.helpers.FenixRobolectricTestRunner
import org.mozilla.fenix.nimbus.ControlMessageBehavior.SHOW_NEXT_MESSAGE
import org.mozilla.fenix.nimbus.MessageData
import org.mozilla.fenix.nimbus.Messaging
import org.mozilla.fenix.nimbus.StyleData

@RunWith(FenixRobolectricTestRunner::class)
class NimbusMessagingStorageTest {

    private val activity: HomeActivity = mockk(relaxed = true)
    private val storageNimbus: NimbusMessagingStorage = mockk(relaxed = true)
    private lateinit var storage: NimbusMessagingStorage
    private lateinit var metadataStorage: MessageMetadataStorage
    private lateinit var gleanPlumb: GleanPlumbInterface
    private lateinit var messagingFeature: FeatureHolder<Messaging>
    private lateinit var messaging: Messaging
    private var malformedWasReported = false
    private val reportMalformedMessage: (String) -> Unit = {
        malformedWasReported = true
    }
    @Before
    fun setup() {
        gleanPlumb = mockk(relaxed = true)
        metadataStorage = mockk(relaxed = true)
        malformedWasReported = false
        messagingFeature = createMessagingFeature()

        coEvery { metadataStorage.getMetadata() } returns mapOf("message-1" to Message.Metadata(id = "message-1"))

        storage = NimbusMessagingStorage(
            testContext,
            metadataStorage,
            reportMalformedMessage,
            gleanPlumb,
            messagingFeature
        )
    }

    @Test
    fun `WHEN calling getMessages THEN provide a list of available messages`() = runTest {
        val message = storage.getMessages().first()

        assertEquals("message-1", message.id)
        assertEquals("message-1", message.metadata.id)
    }

    @Test
    fun `WHEN calling getMessages THEN provide a list of sorted messages by priority`() =
        runTest {
            val messages = mapOf(
                "low-message" to createMessageData(style = "low-priority"),
                "high-message" to createMessageData(style = "high-priority"),
                "medium-message" to createMessageData(style = "medium-priority"),
            )
            val styles = mapOf(
                "high-priority" to createStyle(priority = 100),
                "medium-priority" to createStyle(priority = 50),
                "low-priority" to createStyle(priority = 1)
            )
            val metadataStorage: MessageMetadataStorage = mockk(relaxed = true)
            val messagingFeature = createMessagingFeature(
                styles = styles,
                messages = messages
            )

            coEvery { metadataStorage.getMetadata() } returns mapOf(
                "message-1" to Message.Metadata(
                    id = "message-1"
                )
            )

            val storage = NimbusMessagingStorage(
                testContext,
                metadataStorage,
                reportMalformedMessage,
                gleanPlumb,
                messagingFeature
            )

            val results = storage.getMessages()

            assertEquals("high-message", results[0].id)
            assertEquals("medium-message", results[1].id)
            assertEquals("low-message", results[2].id)
        }

    @Test
    fun `GIVEN pressed message WHEN calling getMessages THEN filter out the pressed message`() =
        runTest {
            val metadataList = mapOf(
                "pressed-message" to Message.Metadata(id = "pressed-message", pressed = true),
                "normal-message" to Message.Metadata(id = "normal-message", pressed = false)
            )
            val messages = mapOf(
                "pressed-message" to createMessageData(style = "high-priority"),
                "normal-message" to createMessageData(style = "high-priority"),
            )
            val styles = mapOf(
                "high-priority" to createStyle(priority = 100),
            )
            val metadataStorage: MessageMetadataStorage = mockk(relaxed = true)
            val messagingFeature = createMessagingFeature(
                styles = styles,
                messages = messages
            )

            coEvery { metadataStorage.getMetadata() } returns metadataList

            val storage = NimbusMessagingStorage(
                testContext,
                metadataStorage,
                reportMalformedMessage,
                gleanPlumb,
                messagingFeature
            )

            val results = storage.getMessages()

            assertEquals(1, results.size)
            assertEquals("normal-message", results[0].id)
        }

    @Test
    fun `GIVEN dismissed message WHEN calling getMessages THEN filter out the dismissed message`() =
        runTest {
            val metadataList = mapOf(
                "dismissed-message" to Message.Metadata(id = "dismissed-message", dismissed = true),
                "normal-message" to Message.Metadata(id = "normal-message", dismissed = false)
            )
            val messages = mapOf(
                "dismissed-message" to createMessageData(style = "high-priority"),
                "normal-message" to createMessageData(style = "high-priority"),
            )
            val styles = mapOf(
                "high-priority" to createStyle(priority = 100),
            )
            val metadataStorage: MessageMetadataStorage = mockk(relaxed = true)
            val messagingFeature = createMessagingFeature(
                styles = styles,
                messages = messages
            )

            coEvery { metadataStorage.getMetadata() } returns metadataList

            val storage = NimbusMessagingStorage(
                testContext,
                metadataStorage,
                reportMalformedMessage,
                gleanPlumb,
                messagingFeature
            )

            val results = storage.getMessages()

            assertEquals(1, results.size)
            assertEquals("normal-message", results[0].id)
        }

    @Test
    fun `GIVEN a message that the maxDisplayCount WHEN calling getMessages THEN filter out the message`() =
        runTest {
            val metadataList = mapOf(
                "shown-many-times-message" to Message.Metadata(
                    id = "shown-many-times-message",
                    displayCount = 10
                ),
                "normal-message" to Message.Metadata(id = "normal-message", displayCount = 0)
            )
            val messages = mapOf(
                "shown-many-times-message" to createMessageData(
                    style = "high-priority"
                ),
                "normal-message" to createMessageData(style = "high-priority"),
            )
            val styles = mapOf(
                "high-priority" to createStyle(priority = 100, maxDisplayCount = 2),
            )
            val metadataStorage: MessageMetadataStorage = mockk(relaxed = true)
            val messagingFeature = createMessagingFeature(
                styles = styles,
                messages = messages
            )

            coEvery { metadataStorage.getMetadata() } returns metadataList

            val storage = NimbusMessagingStorage(
                testContext,
                metadataStorage,
                reportMalformedMessage,
                gleanPlumb,
                messagingFeature
            )

            val results = storage.getMessages()

            assertEquals(1, results.size)
            assertEquals("normal-message", results[0].id)
        }

    @Test
    fun `GIVEN a malformed message WHEN calling getMessages THEN provide a list of messages ignoring the malformed one`() = runTest {
        val messages = storage.getMessages()
        val firstMessage = messages.first()

        assertEquals("message-1", firstMessage.id)
        assertEquals("message-1", firstMessage.metadata.id)
        assertTrue(messages.size == 1)
        assertTrue(malformedWasReported)
    }

    @Test
    fun `GIVEN a malformed action WHEN calling sanitizeAction THEN return null`() {
        val actionsMap = mapOf("action-1" to "action-1-url")

        val notFoundAction = storage.sanitizeAction("messageId", "no-found-action", actionsMap, false)
        val emptyAction = storage.sanitizeAction("messageId", "", actionsMap, false)
        val blankAction = storage.sanitizeAction("messageId", " ", actionsMap, false)

        assertNull(notFoundAction)
        assertNull(emptyAction)
        assertNull(blankAction)
        assertTrue(malformedWasReported)
    }

    @Test
    fun `GIVEN a previously stored malformed action WHEN calling sanitizeAction THEN return null and not report malFormed`() {
        val actionsMap = mapOf("action-1" to "action-1-url")

        storage.malFormedMap["malformed-action"] = "messageId"

        val action = storage.sanitizeAction("messageId", "malformed-action", actionsMap, false)

        assertNull(action)
        assertFalse(malformedWasReported)
    }

    @Test
    fun `GIVEN a non-previously stored malformed action WHEN calling sanitizeAction THEN return null and report malFormed`() {
        val actionsMap = mapOf("action-1" to "action-1-url")

        val action = storage.sanitizeAction("messageId", "malformed-action", actionsMap, false)

        assertNull(action)
        assertTrue(storage.malFormedMap.containsKey("malformed-action"))
        assertTrue(malformedWasReported)
    }

    @Test
    fun `WHEN calling updateMetadata THEN delegate to metadataStorage`() = runTest {

        storage.updateMetadata(mockk(relaxed = true))

        coEvery { metadataStorage.updateMetadata(any()) }
    }

    @Test
    fun `GIVEN a valid action WHEN calling sanitizeAction THEN return the action`() {
        val actionsMap = mapOf("action-1" to "action-1-url")

        val validAction = storage.sanitizeAction("messageId", "action-1", actionsMap, false)

        assertEquals("action-1-url", validAction)
    }

    @Test
    fun `GIVEN a valid action for control message WHEN calling sanitizeAction THEN return a empty action`() {
        val actionsMap = mapOf("action-1" to "action-1-url")

        val validAction = storage.sanitizeAction("messageId", "", actionsMap, true)

        assertEquals("CONTROL_ACTION", validAction)
        assertFalse(malformedWasReported)
    }

    @Test
    fun `GIVEN a trigger action WHEN calling sanitizeTriggers THEN return null`() {
        val triggersMap = mapOf("trigger-1" to "trigger-1-expression")

        val notFoundTrigger =
            storage.sanitizeTriggers("messageId", listOf("no-found-trigger"), triggersMap)
        val emptyTrigger = storage.sanitizeTriggers("messageId", listOf(""), triggersMap)
        val blankTrigger = storage.sanitizeTriggers("messageId", listOf(" "), triggersMap)

        assertNull(notFoundTrigger)
        assertNull(emptyTrigger)
        assertNull(blankTrigger)
        assertTrue(malformedWasReported)
    }

    @Test
    fun `GIVEN a previously stored malformed trigger WHEN calling sanitizeTriggers THEN no report malformed and return null`() {
        val triggersMap = mapOf("trigger-1" to "trigger-1-expression")

        storage.malFormedMap[" "] = "messageId"

        val trigger = storage.sanitizeTriggers("messageId", listOf(" "), triggersMap)

        assertNull(trigger)
        assertFalse(malformedWasReported)
    }

    @Test
    fun `GIVEN a non previously stored malformed trigger WHEN calling sanitizeTriggers THEN report malformed and return null`() {
        val triggersMap = mapOf("trigger-1" to "trigger-1-expression")

        val trigger = storage.sanitizeTriggers("messageId", listOf(" "), triggersMap)

        assertNull(trigger)
        assertTrue(storage.malFormedMap.containsKey(" "))
        assertTrue(malformedWasReported)
    }

    @Test
    fun `GIVEN a valid trigger WHEN calling sanitizeAction THEN return the trigger`() {
        val triggersMap = mapOf("trigger-1" to "trigger-1-expression")

        val validTrigger = storage.sanitizeTriggers("messageId", listOf("trigger-1"), triggersMap)

        assertEquals(listOf("trigger-1-expression"), validTrigger)
    }

    @Test
    fun `GIVEN a null or black expression WHEN calling isMessageUnderExperiment THEN return false`() {
        val message = Message(
            "id",
            mockk(relaxed = true),
            action = "action",
            mockk(relaxed = true),
            emptyList(),
            Message.Metadata("id")
        )

        val result = storage.isMessageUnderExperiment(message, null)

        assertFalse(result)
    }

    @Test
    fun `GIVEN messages id that ends with - WHEN calling isMessageUnderExperiment THEN return true`() {
        val message = Message(
            "end-",
            mockk(relaxed = true),
            action = "action",
            mockk(relaxed = true),
            emptyList(),
            Message.Metadata("end-")
        )

        val result = storage.isMessageUnderExperiment(message, "end-")

        assertTrue(result)
    }

    @Test
    fun `GIVEN message under experiment WHEN calling isMessageUnderExperiment THEN return true`() {
        val message = Message(
            "same-id",
            mockk(relaxed = true),
            action = "action",
            mockk(relaxed = true),
            emptyList(),
            Message.Metadata("same-id")
        )

        val result = storage.isMessageUnderExperiment(message, "same-id")

        assertTrue(result)
    }

    @Test
    fun `GIVEN an eligible message WHEN calling isMessageEligible THEN return true`() {
        val helper: GleanPlumbMessageHelper = mockk(relaxed = true)
        val message = Message(
            "same-id",
            mockk(relaxed = true),
            action = "action",
            mockk(relaxed = true),
            listOf("trigger"),
            Message.Metadata("same-id")
        )

        every { helper.evalJexl(any()) } returns true

        val result = storage.isMessageEligible(message, helper)

        assertTrue(result)
    }

    @Test
    fun `GIVEN a malformed trigger WHEN calling isMessageEligible THEN return false`() {
        val helper: GleanPlumbMessageHelper = mockk(relaxed = true)
        val message = Message(
            "same-id",
            mockk(relaxed = true),
            action = "action",
            mockk(relaxed = true),
            listOf("trigger"),
            Message.Metadata("same-id")
        )

        every { helper.evalJexl(any()) } throws NimbusException.EvaluationException("")

        val result = storage.isMessageEligible(message, helper)

        assertFalse(result)
    }

    @Test
    fun `GIVEN a previously malformed trigger WHEN calling isMessageEligible THEN return false and not evaluate`() {
        val helper: GleanPlumbMessageHelper = mockk(relaxed = true)
        val message = Message(
            "same-id",
            mockk(relaxed = true),
            action = "action",
            mockk(relaxed = true),
            listOf("trigger"),
            Message.Metadata("same-id")
        )

        storage.malFormedMap["trigger"] = "same-id"

        every { helper.evalJexl(any()) } throws NimbusException.EvaluationException("")

        val result = storage.isMessageEligible(message, helper)

        assertFalse(result)
        verify(exactly = 0) { helper.evalJexl("trigger") }
        assertFalse(malformedWasReported)
    }

    @Test
    fun `GIVEN a non previously malformed trigger WHEN calling isMessageEligible THEN return false and not evaluate`() {
        val helper: GleanPlumbMessageHelper = mockk(relaxed = true)
        val message = Message(
            "same-id",
            mockk(relaxed = true),
            action = "action",
            mockk(relaxed = true),
            listOf("trigger"),
            Message.Metadata("same-id")
        )

        every { helper.evalJexl(any()) } throws NimbusException.EvaluationException("")

        assertFalse(storage.malFormedMap.containsKey("trigger"))

        val result = storage.isMessageEligible(message, helper)

        assertFalse(result)
        assertTrue(storage.malFormedMap.containsKey("trigger"))
        assertTrue(malformedWasReported)
    }

    @Test
    fun `GIVEN none available messages are eligible WHEN calling getNextMessage THEN return null`() {
        val spiedStorage = spyk(storage)
        val message = Message(
            "same-id",
            mockk(relaxed = true),
            action = "action",
            mockk(relaxed = true),
            listOf("trigger"),
            Message.Metadata("same-id")
        )

        every { spiedStorage.isMessageEligible(any(), any()) } returns false

        val result = spiedStorage.getNextMessage(listOf(message))

        assertNull(result)
    }

    @Test
    fun `GIVEN an eligible message WHEN calling getNextMessage THEN return the message`() {
        val spiedStorage = spyk(storage)
        val message = Message(
            "same-id",
            mockk(relaxed = true),
            action = "action",
            mockk(relaxed = true),
            listOf("trigger"),
            Message.Metadata("same-id")
        )

        every { spiedStorage.isMessageEligible(any(), any()) } returns true
        every { spiedStorage.isMessageUnderExperiment(any(), any()) } returns false

        val result = spiedStorage.getNextMessage(listOf(message))

        assertEquals(message.id, result!!.id)
    }

    @Test
    fun `GIVEN a message under experiment WHEN calling getNextMessage THEN call recordExposure`() {
        val spiedStorage = spyk(storage)
        val messageData: MessageData = mockk(relaxed = true)

        every { messageData.isControl } returns false

        val message = Message(
            "same-id",
            messageData,
            action = "action",
            mockk(relaxed = true),
            listOf("trigger"),
            Message.Metadata("same-id")
        )

        every { spiedStorage.isMessageEligible(any(), any()) } returns true
        every { spiedStorage.isMessageUnderExperiment(any(), any()) } returns true

        val result = spiedStorage.getNextMessage(listOf(message))

        verify { messagingFeature.recordExposure() }
        assertEquals(message.id, result!!.id)
    }

    @Test
    fun `GIVEN a control message WHEN calling getNextMessage THEN return the next eligible message`() {
        val spiedStorage = spyk(storage)
        val messageData: MessageData = mockk(relaxed = true)
        val controlMessageData: MessageData = mockk(relaxed = true)

        every { messageData.isControl } returns false
        every { spiedStorage.getOnControlBehavior() } returns SHOW_NEXT_MESSAGE
        every { controlMessageData.isControl } returns true

        val message = Message(
            "id",
            messageData,
            action = "action",
            mockk(relaxed = true),
            listOf("trigger"),
            Message.Metadata("same-id")
        )

        val controlMessage = Message(
            "control-id",
            controlMessageData,
            action = "action",
            mockk(relaxed = true),
            listOf("trigger"),
            Message.Metadata("same-id")
        )

        every { spiedStorage.isMessageEligible(any(), any()) } returns true
        every { spiedStorage.isMessageUnderExperiment(any(), any()) } returns true

        val result = spiedStorage.getNextMessage(listOf(controlMessage, message))

        verify { messagingFeature.recordExposure() }
        assertEquals(message.id, result!!.id)
    }

    private fun createMessageData(
        action: String = "action-1",
        style: String = "style-1",
        triggers: List<String> = listOf("trigger-1")
    ): MessageData {
        val messageData1: MessageData = mockk(relaxed = true)
        every { messageData1.action } returns action
        every { messageData1.style } returns style
        every { messageData1.trigger } returns triggers
        return messageData1
    }

    private fun createMessagingFeature(
        triggers: Map<String, String> = mapOf("trigger-1" to "trigger-1-expression"),
        styles: Map<String, StyleData> = mapOf("style-1" to createStyle()),
        actions: Map<String, String> = mapOf("action-1" to "action-1-url"),
        messages: Map<String, MessageData> = mapOf(
            "message-1" to createMessageData(),
            "malformed" to mockk(relaxed = true)
        ),
    ): FeatureHolder<Messaging> {
        val messagingFeature: FeatureHolder<Messaging> = mockk(relaxed = true)

        messaging = mockk(relaxed = true)

        every { messaging.triggers } returns triggers
        every { messaging.styles } returns styles
        every { messaging.actions } returns actions
        every { messaging.messages } returns messages

        every { messagingFeature.value() } returns messaging
        return messagingFeature
    }

    private fun createStyle(priority: Int = 1, maxDisplayCount: Int = 5): StyleData {
        val style1: StyleData = mockk(relaxed = true)
        every { style1.priority } returns priority
        every { style1.maxDisplayCount } returns maxDisplayCount
        return style1
    }
}
