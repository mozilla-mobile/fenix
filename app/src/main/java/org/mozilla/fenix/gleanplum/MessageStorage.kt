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