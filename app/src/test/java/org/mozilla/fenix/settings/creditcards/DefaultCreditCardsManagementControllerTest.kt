/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.creditcards

import androidx.navigation.NavController
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import mozilla.components.concept.storage.CreditCard
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mozilla.fenix.settings.creditcards.controller.DefaultCreditCardsManagementController
import org.mozilla.fenix.helpers.DisableNavGraphProviderAssertionRule

class DefaultCreditCardsManagementControllerTest {

    private val navController: NavController = mockk(relaxed = true)

    private lateinit var controller: DefaultCreditCardsManagementController

    @get:Rule
    val disableNavGraphProviderAssertionRule = DisableNavGraphProviderAssertionRule()

    @Before
    fun setup() {
        controller = spyk(
            DefaultCreditCardsManagementController(
                navController = navController
            )
        )
    }

    @Test
    fun handleCreditCardClicked() {
        val creditCard = CreditCard(
            guid = "id",
            billingName = "Banana Apple",
            cardNumber = "4111111111111110",
            expiryMonth = 1,
            expiryYear = 2030,
            cardType = "amex",
            timeCreated = 1L,
            timeLastUsed = 1L,
            timeLastModified = 1L,
            timesUsed = 1L
        )

        controller.handleCreditCardClicked(creditCard)

        verify {
            navController.navigate(
                CreditCardsManagementFragmentDirections
                    .actionCreditCardsManagementFragmentToCreditCardEditorFragment(
                        creditCard = creditCard
                    )
            )
        }
    }
}
