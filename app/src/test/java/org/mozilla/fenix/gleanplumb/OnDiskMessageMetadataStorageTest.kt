/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.gleanplumb

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.test.runBlockingTest
import mozilla.components.support.test.robolectric.testContext
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.experiments.nimbus.GleanPlumbInterface
import org.mozilla.experiments.nimbus.internal.FeatureHolder
import org.mozilla.fenix.helpers.FenixRobolectricTestRunner
import org.mozilla.fenix.nimbus.Messaging

@RunWith(FenixRobolectricTestRunner::class)
class OnDiskMessageMetadataStorageTest {

    private lateinit var storage: OnDiskMessageMetadataStorage
    private lateinit var metadataStorage: MessageMetadataStorage
    private lateinit var gleanPlumb: GleanPlumbInterface
    private lateinit var messagingFeature: FeatureHolder<Messaging>
    private lateinit var messaging: Messaging

    @Before
    fun setup() {
        storage = OnDiskMessageMetadataStorage(
            testContext
        )
    }

    @Test
    fun `GIVEN metadata is not loaded from disk WHEN calling getMetadata THEN load it`() =
        runBlockingTest {
            val spiedStorage = spyk(storage)

            coEvery { spiedStorage.readFromDisk() } returns emptyMap()

            spiedStorage.getMetadata()

            verify { spiedStorage.readFromDisk() }
        }

    @Test
    fun `GIVEN metadata is loaded from disk WHEN calling getMetadata THEN do not load it from disk`() =
        runBlockingTest {
            val spiedStorage = spyk(storage)

            spiedStorage.metadataMap = hashMapOf("" to Message.Metadata("id"))

            spiedStorage.getMetadata()

            verify(exactly = 0) { spiedStorage.readFromDisk() }
        }

    @Test
    fun `WHEN calling addMetadata THEN add in memory and disk`() = runBlockingTest {
        val spiedStorage = spyk(storage)

        assertTrue(spiedStorage.metadataMap.isEmpty())

        coEvery { spiedStorage.writeToDisk() } just Runs

        spiedStorage.addMetadata(Message.Metadata("id"))

        assertFalse(spiedStorage.metadataMap.isEmpty())
        coVerify { spiedStorage.writeToDisk() }
    }

    @Test
    fun `WHEN calling updateMetadata THEN delegate to addMetadata`() = runBlockingTest {
        val spiedStorage = spyk(storage)
        val metadata = Message.Metadata("id")
        coEvery { spiedStorage.writeToDisk() } just Runs

        spiedStorage.updateMetadata(metadata)

        coVerify { spiedStorage.addMetadata(metadata) }
    }

    @Test
    fun `WHEN calling toJson THEN return an string json representation`() {
        val metadata = Message.Metadata(
            id = "id",
            displayCount = 1,
            pressed = false,
            dismissed = false,
            lastTimeShown = 0L,
        )

        val expected =
            """{"id":"id","displayCount":1,"pressed":false,"dismissed":false,"lastTimeShown":0}"""

        assertEquals(expected, metadata.toJson())
    }

    @Test
    fun `WHEN calling toMetadata THEN return Metadata representation`() {
        val json =
            """{"id":"id","displayCount":1,"pressed":false,"dismissed":false,"lastTimeShown":0}"""

        val jsonObject = JSONObject(json)

        val metadata = Message.Metadata(
            id = "id",
            displayCount = 1,
            pressed = false,
            dismissed = false,
            lastTimeShown = 0L,
        )

        assertEquals(metadata, jsonObject.toMetadata())
    }

    @Test
    fun `WHEN calling toMetadataMap THEN return map representation`() {
        val json =
            """[{"id":"id","displayCount":1,"pressed":false,"dismissed":false,"lastTimeShown":0}]"""

        val jsonArray = JSONArray(json)

        val metadata = Message.Metadata(
            id = "id",
            displayCount = 1,
            pressed = false,
            dismissed = false,
            lastTimeShown = 0L,
        )

        assertEquals(metadata, jsonArray.toMetadataMap()[metadata.id])
    }
}
