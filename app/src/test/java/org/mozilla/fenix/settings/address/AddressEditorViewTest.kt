/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.address

import android.view.LayoutInflater
import android.view.View
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import mozilla.components.browser.state.search.RegionState
import mozilla.components.concept.storage.Address
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.R
import org.mozilla.fenix.databinding.FragmentAddressEditorBinding
import org.mozilla.fenix.helpers.FenixRobolectricTestRunner
import org.mozilla.fenix.settings.address.interactor.AddressEditorInteractor
import org.mozilla.fenix.settings.address.view.AddressEditorView
import org.mozilla.fenix.settings.address.view.DEFAULT_COUNTRY

@RunWith(FenixRobolectricTestRunner::class)
class AddressEditorViewTest {

    private lateinit var view: View
    private lateinit var interactor: AddressEditorInteractor
    private lateinit var addressEditorView: AddressEditorView
    private lateinit var binding: FragmentAddressEditorBinding
    private lateinit var address: Address

    @Before
    fun setup() {
        view = LayoutInflater.from(testContext).inflate(R.layout.fragment_address_editor, null)
        binding = FragmentAddressEditorBinding.bind(view)
        interactor = mockk(relaxed = true)
        address = mockk(relaxed = true)
        every { address.guid } returns "123"

        addressEditorView = spyk(AddressEditorView(binding, interactor))
    }

    @Test
    fun `WHEN the cancel button is clicked THEN interactor is called`() {
        addressEditorView.bind()

        binding.cancelButton.performClick()

        verify { interactor.onCancelButtonClicked() }
    }

    @Test
    fun `GIVEN an existing address WHEN editor is opened THEN the form fields are correctly mapped to the address fields`() {
        val address = generateAddress()

        val addressEditorView = spyk(
            AddressEditorView(
                binding = binding,
                interactor = interactor,
                address = address,
            )
        )
        addressEditorView.bind()

        assertEquals("PostalCode", binding.zipInput.text.toString())
        assertEquals("State", binding.stateInput.text.toString())
        assertEquals("City", binding.cityInput.text.toString())
        assertEquals("Street", binding.streetAddressInput.text.toString())
        assertEquals("Family", binding.lastNameInput.text.toString())
        assertEquals("Given", binding.firstNameInput.text.toString())
        assertEquals("Additional", binding.middleNameInput.text.toString())
        assertEquals("email@mozilla.com", binding.emailInput.text.toString())
        assertEquals("Telephone", binding.phoneInput.text.toString())
    }

    @Test
    fun `GIVEN an existing address WHEN editor is opened THEN the delete address button is visible`() = runBlocking {
        val addressEditorView = spyk(
            AddressEditorView(
                binding = binding,
                interactor = interactor,
                address = address,
            )
        )
        addressEditorView.bind()

        assertEquals(View.VISIBLE, binding.deleteButton.visibility)
    }

    @Test
    fun `GIVEN an existing address WHEN the delete address button is clicked THEN confirm delete dialog is shown`() = runBlocking {
        val addressEditorView = spyk(
            AddressEditorView(
                binding = binding,
                interactor = interactor,
                address = address,
            )
        )
        addressEditorView.bind()

        binding.deleteButton.performClick()

        verify { addressEditorView.showConfirmDeleteAddressDialog(view.context, "123") }
    }

    @Test
    fun `GIVEN existing address WHEN country dropdown is bound THEN adapter sets country dropdown to address`() {
        val addressEditorView = spyk(
            AddressEditorView(
                binding = binding,
                interactor = interactor,
                address = generateAddress(country = "CA"),
            )
        )
        addressEditorView.bind()

        assertEquals(addressEditorView.countries["CA"]?.displayName, binding.countryDropDown.selectedItem.toString())
    }

    @Test
    fun `GIVEN existing address and region not in supported countries WHEN country dropdown is bound THEN adapter sets dropdown to lower priority`() {
        val addressEditorView = spyk(
            AddressEditorView(
                binding = binding,
                interactor = interactor,
                region = RegionState.Default,
                address = generateAddress(country = "XX"),
            )
        )
        addressEditorView.bind()

        assertEquals(addressEditorView.countries[DEFAULT_COUNTRY]!!.displayName, binding.countryDropDown.selectedItem.toString())
    }

    @Test
    fun `GIVEN search region and no address WHEN country dropdown is bound THEN adapter sets country dropdown to home region`() {
        val addressEditorView = AddressEditorView(
            binding = binding,
            interactor = interactor,
            region = RegionState("CA", "US"),
            address = null,
        )
        addressEditorView.bind()

        assertEquals(addressEditorView.countries["CA"]?.displayName, binding.countryDropDown.selectedItem.toString())
    }

    @Test
    fun `GIVEN search region not in supported countries WHEN country dropdown is bound THEN adapter sets dropdown to lower priority`() {
        val addressEditorView = AddressEditorView(
            binding = binding,
            interactor = interactor,
            region = RegionState.Default,
            address = null,
        )
        addressEditorView.bind()

        assertEquals(addressEditorView.countries[DEFAULT_COUNTRY]!!.displayName, binding.countryDropDown.selectedItem.toString())
    }

    @Test
    fun `GIVEN no address or search region WHEN country dropdown is bound THEN adapter sets dropdown to default`() {
        val addressEditorView = AddressEditorView(
            binding = binding,
            interactor = interactor,
            region = null,
            address = null,
        )
        addressEditorView.bind()

        assertEquals(addressEditorView.countries[DEFAULT_COUNTRY]!!.displayName, binding.countryDropDown.selectedItem.toString())
    }

    private fun generateAddress(country: String = "US") = Address(
        guid = "123",
        givenName = "Given",
        additionalName = "Additional",
        familyName = "Family",
        organization = "Organization",
        streetAddress = "Street",
        addressLevel3 = "Suburb",
        addressLevel2 = "City",
        addressLevel1 = "State",
        postalCode = "PostalCode",
        country = country,
        tel = "Telephone",
        email = "email@mozilla.com",
        timeCreated = 0L,
        timeLastUsed = 1L,
        timeLastModified = 1L,
        timesUsed = 2L
    )
}
