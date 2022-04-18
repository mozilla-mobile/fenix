/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.autofill

import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import androidx.preference.Preference
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.TestCoroutineDispatcher
import mozilla.components.concept.storage.CreditCard
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.getPreferenceKey
import org.mozilla.fenix.helpers.FenixRobolectricTestRunner
import org.mozilla.fenix.settings.creditcards.CreditCardsFragmentStore
import org.mozilla.fenix.settings.creditcards.CreditCardsListState
import org.robolectric.Robolectric

@RunWith(FenixRobolectricTestRunner::class)
class AutofillSettingFragmentTest {

    private val testDispatcher = TestCoroutineDispatcher()
    private lateinit var autofillSettingFragment: AutofillSettingFragment
    private val navController: NavController = mockk(relaxed = true)

    @Before
    fun setUp() {
        every { testContext.components.settings } returns mockk(relaxed = true)

        autofillSettingFragment = AutofillSettingFragment()

        val activity = Robolectric.buildActivity(FragmentActivity::class.java).create().get()

        activity.supportFragmentManager.beginTransaction()
            .add(autofillSettingFragment, "CreditCardsSettingFragmentTest")
            .commitNow()
        testDispatcher.advanceUntilIdle()
    }

    @Test
    fun `GIVEN the list of credit cards is not empty, WHEN fragment is displayed THEN the manage credit cards pref is 'Manage saved cards'`() {
        val preferenceTitle =
            testContext.getString(R.string.preferences_credit_cards_manage_saved_cards)
        val manageCardsPreference = autofillSettingFragment.findPreference<Preference>(
            autofillSettingFragment.getPreferenceKey(R.string.pref_key_credit_cards_manage_cards)
        )

        val creditCards: List<CreditCard> = listOf(mockk(), mockk())

        val creditCardsState = CreditCardsListState(creditCards = creditCards)
        val creditCardsStore = CreditCardsFragmentStore(creditCardsState)

        autofillSettingFragment.updateCardManagementPreference(
            creditCardsStore.state.creditCards.isNotEmpty(),
            navController
        )

        assertNull(manageCardsPreference?.icon)
        assertEquals(preferenceTitle, manageCardsPreference?.title)
    }

    @Test
    fun `GIVEN the list of credit cards is empty, WHEN fragment is displayed THEN the manage credit cards pref is 'Add card'`() {
        val preferenceTitle =
            testContext.getString(R.string.preferences_credit_cards_add_credit_card)
        val manageCardsPreference = autofillSettingFragment.findPreference<Preference>(
            autofillSettingFragment.getPreferenceKey(R.string.pref_key_credit_cards_manage_cards)
        )

        val directions =
            AutofillSettingFragmentDirections
                .actionAutofillSettingFragmentToCreditCardEditorFragment()

        val creditCardsState = CreditCardsListState(creditCards = emptyList())
        val creditCardsStore = CreditCardsFragmentStore(creditCardsState)

        autofillSettingFragment.updateCardManagementPreference(
            creditCardsStore.state.creditCards.isNotEmpty(),
            navController
        )

        assertNotNull(manageCardsPreference?.icon)
        assertEquals(preferenceTitle, manageCardsPreference?.title)

        manageCardsPreference?.performClick()

        verify { navController.navigate(directions) }
    }
}
