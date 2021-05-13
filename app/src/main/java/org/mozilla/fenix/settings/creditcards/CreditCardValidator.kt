/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.creditcards

import androidx.annotation.StringRes
import mozilla.components.concept.storage.CreditCard
import mozilla.components.concept.storage.CreditCardsAddressesStorage
import org.mozilla.fenix.R

class CreditCardValidator(
    private val creditCardsList: List<CreditCard>,
    private val storage: CreditCardsAddressesStorage
) {

    /**
     * Validates the credit card information entered by the user.
     * @return a [ValidationResult] that describes the validation result,
     * either a [ValidationResult.Valid] or a [ValidationResult.Invalid]
     */
    internal fun validateCreditCard(creditCardNumber: String, guid: String?): ValidationResult {
        return when (creditCardNumber.validateCreditCardNumber()) {
            false -> ValidationResult.Invalid(R.string.credit_cards_number_validation_error_message)
            true -> if (isDuplicate(creditCardNumber, guid)) {
                ValidationResult.Invalid(R.string.credit_cards_number_duplication_error_message)
            } else {
                ValidationResult.Valid
            }
        }
    }

    /**
     * Validates the credit card number against existing cards list
     * @return true if the credit card number is a duplicate, false otherwise.
     */
    private fun isDuplicate(creditCardNumber: String, guid: String?): Boolean {
        val crypto = storage.getCreditCardCrypto()
        val key = crypto.key()
        val otherCreditCardNumberList: List<String> = creditCardsList
            .filterNot {
                it.guid == guid
            }.map {
                crypto.decrypt(key, it.encryptedCardNumber).toString()
            }

        return (creditCardNumber in otherCreditCardNumberList)
    }

    /**
     * A class that describes a credit card validation result.
     */
    sealed class ValidationResult {
        data class Invalid(@StringRes val errorMessageRes: Int) : ValidationResult()
        object Valid : ValidationResult()
    }
}
