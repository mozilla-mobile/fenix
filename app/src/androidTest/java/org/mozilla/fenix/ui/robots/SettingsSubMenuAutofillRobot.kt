/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.robots

import androidx.test.uiautomator.UiSelector
import org.junit.Assert.assertTrue
import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.TestAssetHelper.waitingTime
import org.mozilla.fenix.helpers.TestHelper.getStringResource
import org.mozilla.fenix.helpers.TestHelper.mDevice
import org.mozilla.fenix.helpers.TestHelper.packageName

class SettingsSubMenuAutofillRobot {

    fun clickAddCreditCardButton() = addCreditCardButton.click()
    fun clickManageSavedCardsButton() = manageSavedCardsButton.click()
    fun clickSecuredCreditCardsLaterButton() = securedCreditCardsLaterButton.click()
    fun clickSavedCreditCard() = savedCreditCardNumber.clickAndWaitForNewWindow(waitingTime)
    fun clickDeleteCreditCardButton() {
        deleteCreditCardButton.waitForExists(waitingTime)
        deleteCreditCardButton.click()
    }

    fun clickConfirmDeleteCreditCardButton() {
        confirmDeleteCreditCardButton.waitForExists(waitingTime)
        confirmDeleteCreditCardButton.click()
    }

    fun clickExpiryMonthOption(expiryMonth: String) {
        expiryMonthOption(expiryMonth).waitForExists(waitingTime)
        expiryMonthOption(expiryMonth).click()
    }

    fun clickExpiryYearOption(expiryYear: String) {
        expiryYearOption(expiryYear).waitForExists(waitingTime)
        expiryYearOption(expiryYear).click()
    }

    fun verifyAddCreditCardsButton() = assertTrue(addCreditCardButton.waitForExists(waitingTime))

    fun fillAndSaveCreditCard(cardNumber: String, cardName: String, expiryMonth: String, expiryYear: String) {
        cardNumberTextInput.waitForExists(waitingTime)
        cardNumberTextInput.setText(cardNumber)
        nameOnCardTextInput.setText(cardName)
        expiryMonthDropDown.click()
        clickExpiryMonthOption(expiryMonth)
        expiryYearDropDown.click()
        clickExpiryYearOption(expiryYear)

        saveButton.click()
        manageSavedCardsButton.waitForExists(waitingTime)
    }

    class Transition {
        fun goBack(interact: SettingsRobot.() -> Unit): SettingsRobot.Transition {
            mDevice.pressBack()

            SettingsRobot().interact()
            return SettingsRobot.Transition()
        }

        fun goBackToAutofillSettings(interact: SettingsSubMenuAutofillRobot.() -> Unit): SettingsSubMenuAutofillRobot.Transition {
            mDevice.pressBack()

            SettingsSubMenuAutofillRobot().interact()
            return SettingsSubMenuAutofillRobot.Transition()
        }
    }
}
private val addCreditCardButton = mDevice.findObject(UiSelector().textContains(getStringResource(R.string.preferences_credit_cards_add_credit_card)))
private val manageSavedCardsButton = mDevice.findObject(UiSelector().textContains(getStringResource(R.string.preferences_credit_cards_manage_saved_cards)))

private val cardNumberTextInput = mDevice.findObject(UiSelector().resourceId("$packageName:id/card_number_input"))
private val nameOnCardTextInput = mDevice.findObject(UiSelector().resourceId("$packageName:id/name_on_card_input"))
private val expiryMonthDropDown = mDevice.findObject(UiSelector().resourceId("$packageName:id/expiry_month_drop_down"))
private val expiryYearDropDown = mDevice.findObject(UiSelector().resourceId("$packageName:id/expiry_year_drop_down"))
private val savedCreditCardNumber = mDevice.findObject(UiSelector().resourceId("$packageName:id/credit_card_logo"))

private val deleteCreditCardButton = mDevice.findObject(UiSelector().resourceId("$packageName:id/delete_credit_card_button"))
private val saveButton = mDevice.findObject(UiSelector().resourceId("$packageName:id/save_button"))
private val confirmDeleteCreditCardButton = mDevice.findObject(UiSelector().resourceId("android:id/button1"))

private val securedCreditCardsLaterButton = mDevice.findObject(UiSelector().resourceId("android:id/button2"))

private fun expiryMonthOption(expiryMonth: String) = mDevice.findObject(UiSelector().textContains(expiryMonth))
private fun expiryYearOption(expiryYear: String) = mDevice.findObject(UiSelector().textContains(expiryYear))
