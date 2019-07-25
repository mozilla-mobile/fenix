package org.mozilla.fenix.quickactionsheet

/**
 * An interface that handles the view manipulation of the QuickActionSheet, triggered by the Interactor
 */
interface QuickActionSheetController {
    fun handleShare()
    fun handleDownload()
    fun handleBookmark()
    fun handleOpenLink()
}

class DefaultQuickActionSheetController() : QuickActionSheetController {
    override fun handleShare() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun handleDownload() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun handleBookmark() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun handleOpenLink() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}