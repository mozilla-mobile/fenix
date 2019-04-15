package org.mozilla.fenix.components.toolbar

import mozilla.components.browser.menu.BrowserMenuBuilder
import mozilla.components.browser.menu.item.BrowserMenuItemToolbar

interface ToolbarMenu {
    sealed class Item {
        object Help : Item()
        object Settings : Item()
        object Library : Item()
        data class RequestDesktop(val isChecked: Boolean) : Item()
        object FindInPage : Item()
        object NewPrivateTab : Item()
        object NewTab : Item()
        object Share : Item()
        object Back : Item()
        object Forward : Item()
        object Reload : Item()
        object Stop : Item()
        object ReportIssue : Item()
        object OpenInFenix : Item()
    }

    val menuBuilder: BrowserMenuBuilder
    val menuToolbar: BrowserMenuItemToolbar

    companion object {
        const val CAPTION_TEXT_SIZE = 12f
    }
}
