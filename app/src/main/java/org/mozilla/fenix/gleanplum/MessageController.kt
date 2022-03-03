package org.mozilla.fenix.gleanplum

interface MessageController {
    /**
     * Finds the next message to be displayed.
     */
    fun getNextMessage(): Message?

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

    fun initialize()
}