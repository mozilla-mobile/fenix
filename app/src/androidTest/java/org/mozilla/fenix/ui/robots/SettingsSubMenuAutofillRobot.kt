/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.uiautomator.UiSelector
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.endsWith
import org.junit.Assert.assertTrue
import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.MatcherHelper.assertItemContainingTextExists
import org.mozilla.fenix.helpers.MatcherHelper.assertItemWithDescriptionExists
import org.mozilla.fenix.helpers.MatcherHelper.assertItemWithResIdExists
import org.mozilla.fenix.helpers.MatcherHelper.itemContainingText
import org.mozilla.fenix.helpers.MatcherHelper.itemWithDescription
import org.mozilla.fenix.helpers.MatcherHelper.itemWithResId
import org.mozilla.fenix.helpers.TestAssetHelper.waitingTime
import org.mozilla.fenix.helpers.TestHelper.getStringResource
import org.mozilla.fenix.helpers.TestHelper.hasCousin
import org.mozilla.fenix.helpers.TestHelper.mDevice
import org.mozilla.fenix.helpers.TestHelper.packageName
import org.mozilla.fenix.helpers.TestHelper.scrollToElementByText
import org.mozilla.fenix.helpers.click

class SettingsSubMenuAutofillRobot {

    fun verifyAutofillToolbarTitle() = assertItemContainingTextExists(autofillToolbarTitle)
    fun verifyManageAddressesToolbarTitle() = assertItemContainingTextExists(manageAddressesToolbarTitle)

    fun verifyAddressAutofillSection(isAddressAutofillEnabled: Boolean, userHasSavedAddress: Boolean) {
        assertItemContainingTextExists(
            autofillToolbarTitle,
            addressesSectionTitle,
            saveAndAutofillAddressesOption,
            saveAndAutofillAddressesSummary,
        )

        if (userHasSavedAddress) {
            assertItemContainingTextExists(manageAddressesButton)
        } else {
            assertItemContainingTextExists(addAddressButton)
        }

        verifyAddressesAutofillToggle(isAddressAutofillEnabled)
    }

    fun verifyManageAddressesSection(vararg savedAddressDetails: String) {
        assertItemWithDescriptionExists(navigateBackButton)
        assertItemContainingTextExists(
            manageAddressesToolbarTitle,
            addAddressButton,
        )
        for (savedAddressDetail in savedAddressDetails) {
            assertTrue(
                mDevice.findObject(
                    UiSelector().textContains(savedAddressDetail),
                ).waitForExists(waitingTime),
            )
        }
    }

    fun verifyAddressesAutofillToggle(enabled: Boolean) =
        onView(withText(R.string.preferences_addresses_save_and_autofill_addresses))
            .check(
                matches(
                    hasCousin(
                        allOf(
                            withClassName(endsWith("Switch")),
                            if (enabled) {
                                isChecked()
                            } else {
                                isNotChecked()
                            },
                        ),
                    ),
                ),
            )

    fun verifyAddAddressView() {
        assertItemContainingTextExists(addAddressToolbarTitle)
        assertItemWithDescriptionExists(navigateBackButton)
        assertItemWithResIdExists(
            toolbarCheckmarkButton,
            firstNameTextInput,
            middleNameTextInput,
        )
        scrollToElementByText(getStringResource(R.string.addresses_street_address))
        assertItemWithResIdExists(
            lastNameTextInput,
            streetAddressTextInput,
        )
        scrollToElementByText(getStringResource(R.string.addresses_country))
        assertItemWithResIdExists(
            cityTextInput,
            subRegionDropDown,
            zipCodeTextInput,
        )
        scrollToElementByText(getStringResource(R.string.addresses_save_button))
        assertItemWithResIdExists(
            countryDropDown,
            phoneTextInput,
            emailTextInput,
        )
        assertItemWithResIdExists(
            saveButton,
            cancelButton,
        )
    }

    fun verifyEditAddressView() {
        assertItemContainingTextExists(editAddressToolbarTitle)
        assertItemWithDescriptionExists(navigateBackButton)
        assertItemWithResIdExists(
            toolbarDeleteAddressButton,
            toolbarCheckmarkButton,
            firstNameTextInput,
            middleNameTextInput,
        )
        scrollToElementByText(getStringResource(R.string.addresses_street_address))
        assertItemWithResIdExists(
            lastNameTextInput,
            streetAddressTextInput,
        )
        scrollToElementByText(getStringResource(R.string.addresses_country))
        assertItemWithResIdExists(
            cityTextInput,
            subRegionDropDown,
            zipCodeTextInput,
        )
        scrollToElementByText(getStringResource(R.string.addresses_save_button))
        assertItemWithResIdExists(
            countryDropDown,
            phoneTextInput,
            emailTextInput,
        )
        assertItemWithResIdExists(
            saveButton,
            cancelButton,
        )
        assertItemContainingTextExists(deleteAddressButton)
    }

    fun clickSaveAndAutofillAddressesOption() = saveAndAutofillAddressesOption.click()
    fun clickAddAddressButton() = addAddressButton.click()
    fun clickManageAddressesButton() = manageAddressesButton.click()
    fun clickSavedAddress(firstName: String) = savedAddress(firstName).clickAndWaitForNewWindow(waitingTime)
    fun clickDeleteAddressButton() {
        toolbarDeleteAddressButton.waitForExists(waitingTime)
        toolbarDeleteAddressButton.click()
    }
    fun clickCancelDeleteAddressButton() = cancelDeleteAddressButton.click()

    fun clickConfirmDeleteAddressButton() = confirmDeleteAddressButton.click()

    fun clickSubRegionOption(subRegion: String) {
        subRegionOption(subRegion).waitForExists(waitingTime)
        subRegionOption(subRegion).click()
    }
    fun clickCountryOption(country: String) {
        countryOption(country).waitForExists(waitingTime)
        countryOption(country).click()
    }
    fun verifyAddAddressButton() = assertTrue(addAddressButton.waitForExists(waitingTime))

    fun fillAndSaveAddress(
        firstName: String,
        middleName: String,
        lastName: String,
        streetAddress: String,
        city: String,
        state: String,
        zipCode: String,
        country: String,
        phoneNumber: String,
        emailAddress: String,
    ) {
        firstNameTextInput.waitForExists(waitingTime)
        firstNameTextInput.setText(firstName)
        middleNameTextInput.setText(middleName)
        lastNameTextInput.setText(lastName)
        streetAddressTextInput.setText(streetAddress)
        scrollToElementByText(getStringResource(R.string.addresses_city))
        cityTextInput.setText(city)
        subRegionDropDown.click()
        clickSubRegionOption(state)
        zipCodeTextInput.setText(zipCode)
        countryDropDown.click()
        clickCountryOption(country)
        scrollToElementByText(getStringResource(R.string.addresses_phone))
        phoneTextInput.setText(phoneNumber)
        emailTextInput.setText(emailAddress)
        scrollToElementByText(getStringResource(R.string.addresses_save_button))
        saveButton.click()
        manageAddressesButton.waitForExists(waitingTime)
    }

    fun clickAddCreditCardButton() = addCreditCardButton.click()
    fun clickManageSavedCardsButton() = manageSavedCardsButton.click()
    fun clickSecuredCreditCardsLaterButton() = securedCreditCardsLaterButton.click()
    fun clickSavedCreditCard() = savedCreditCardNumber.clickAndWaitForNewWindow(waitingTime)
    fun clickDeleteCreditCardButton() {
        deleteCreditCardButton.waitForExists(waitingTime)
        deleteCreditCardButton.click()
    }

    fun clickConfirmDeleteCreditCardButton() = confirmDeleteCreditCardButton.click()

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
            navigateBackButton.click()

            SettingsSubMenuAutofillRobot().interact()
            return SettingsSubMenuAutofillRobot.Transition()
        }

        fun goBackToBrowser(interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            mDevice.pressBack()

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }
    }
}

private val autofillToolbarTitle = itemContainingText(getStringResource(R.string.preferences_autofill))
private val addressesSectionTitle = itemContainingText(getStringResource(R.string.preferences_addresses))
private val manageAddressesToolbarTitle = itemContainingText(getStringResource(R.string.addresses_manage_addresses))
private val saveAndAutofillAddressesOption = itemContainingText(getStringResource(R.string.preferences_addresses_save_and_autofill_addresses))
private val saveAndAutofillAddressesSummary = itemContainingText(getStringResource(R.string.preferences_addresses_save_and_autofill_addresses_summary))
private val addAddressButton = itemContainingText(getStringResource(R.string.preferences_addresses_add_address))
private val manageAddressesButton = itemContainingText(getStringResource(R.string.preferences_addresses_manage_addresses))
private val addAddressToolbarTitle = itemContainingText(getStringResource(R.string.addresses_add_address))
private val editAddressToolbarTitle = itemContainingText(getStringResource(R.string.addresses_edit_address))
private val toolbarCheckmarkButton = itemWithResId("$packageName:id/save_address_button")
private val navigateBackButton = itemWithDescription(getStringResource(R.string.action_bar_up_description))
private val firstNameTextInput = itemWithResId("$packageName:id/first_name_input")
private val middleNameTextInput = itemWithResId("$packageName:id/middle_name_input")
private val lastNameTextInput = itemWithResId("$packageName:id/last_name_input")
private val streetAddressTextInput = itemWithResId("$packageName:id/street_address_input")
private val cityTextInput = itemWithResId("$packageName:id/city_input")
private val subRegionDropDown = itemWithResId("$packageName:id/subregion_drop_down")
private val zipCodeTextInput = itemWithResId("$packageName:id/zip_input")
private val countryDropDown = itemWithResId("$packageName:id/country_drop_down")
private val phoneTextInput = itemWithResId("$packageName:id/phone_input")
private val emailTextInput = itemWithResId("$packageName:id/email_input")
private val saveButton = itemWithResId("$packageName:id/save_button")
private val cancelButton = itemWithResId("$packageName:id/cancel_button")
private val deleteAddressButton = itemContainingText(getStringResource(R.string.addressess_delete_address_button))
private val toolbarDeleteAddressButton = itemWithResId("$packageName:id/delete_address_button")
private val cancelDeleteAddressButton = onView(withId(android.R.id.button2)).inRoot(RootMatchers.isDialog())
private val confirmDeleteAddressButton = onView(withId(android.R.id.button1)).inRoot(RootMatchers.isDialog())

private val addCreditCardButton = mDevice.findObject(UiSelector().textContains(getStringResource(R.string.preferences_credit_cards_add_credit_card)))
private val manageSavedCardsButton = mDevice.findObject(UiSelector().textContains(getStringResource(R.string.preferences_credit_cards_manage_saved_cards)))
private val cardNumberTextInput = mDevice.findObject(UiSelector().resourceId("$packageName:id/card_number_input"))
private val nameOnCardTextInput = mDevice.findObject(UiSelector().resourceId("$packageName:id/name_on_card_input"))
private val expiryMonthDropDown = mDevice.findObject(UiSelector().resourceId("$packageName:id/expiry_month_drop_down"))
private val expiryYearDropDown = mDevice.findObject(UiSelector().resourceId("$packageName:id/expiry_year_drop_down"))
private val savedCreditCardNumber = mDevice.findObject(UiSelector().resourceId("$packageName:id/credit_card_logo"))
private val deleteCreditCardButton = mDevice.findObject(UiSelector().resourceId("$packageName:id/delete_credit_card_button"))
private val confirmDeleteCreditCardButton = onView(withId(android.R.id.button1)).inRoot(RootMatchers.isDialog())
private val securedCreditCardsLaterButton = onView(withId(android.R.id.button2)).inRoot(RootMatchers.isDialog())

private fun savedAddress(firstName: String) = mDevice.findObject(UiSelector().textContains(firstName))
private fun subRegionOption(subRegion: String) = mDevice.findObject(UiSelector().textContains(subRegion))
private fun countryOption(country: String) = mDevice.findObject(UiSelector().textContains(country))

private fun expiryMonthOption(expiryMonth: String) = mDevice.findObject(UiSelector().textContains(expiryMonth))
private fun expiryYearOption(expiryYear: String) = mDevice.findObject(UiSelector().textContains(expiryYear))
