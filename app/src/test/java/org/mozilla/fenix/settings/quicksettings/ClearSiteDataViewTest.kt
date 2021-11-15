/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.quicksettings

import android.view.View
import android.widget.FrameLayout
import androidx.core.view.isVisible
import io.mockk.MockKAnnotations
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.databinding.QuicksettingsClearSiteDataBinding
import org.mozilla.fenix.helpers.FenixRobolectricTestRunner

@RunWith(FenixRobolectricTestRunner::class)
class ClearSiteDataViewTest {
    private lateinit var view: ClearSiteDataView
    private lateinit var binding: QuicksettingsClearSiteDataBinding
    private lateinit var interactor: ClearSiteDataViewInteractor

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        interactor = mockk(relaxed = true)
        view = spyk(ClearSiteDataView(FrameLayout(testContext), View(testContext), interactor))
        binding = view.binding
    }

    @Test
    fun `clear site`() {
        val state = WebsiteInfoState(
            websiteUrl = "https://mozilla.org",
            websiteTitle = "Mozilla",
            websiteSecurityUiValues = WebsiteSecurityUiValues.SECURE,
            certificateName = "Certificate"
        )
        testWithWebsiteInfoState(state, "mozilla.org")
    }

    @Test
    fun `clear site with subdomain`() {
        val state = WebsiteInfoState(
            websiteUrl = "https://developers.mozilla.org",
            websiteTitle = "Mozilla",
            websiteSecurityUiValues = WebsiteSecurityUiValues.SECURE,
            certificateName = "Certificate"
        )
        testWithWebsiteInfoState(state, "mozilla.org")
    }

    @Test
    fun `clear site with secondary TLD`() {
        val state = WebsiteInfoState(
            websiteUrl = "https://www.gov.uk",
            websiteTitle = "UK Gov",
            websiteSecurityUiValues = WebsiteSecurityUiValues.SECURE,
            certificateName = "Certificate"
        )
        testWithWebsiteInfoState(state, "www.gov.uk")
    }

    private fun testWithWebsiteInfoState(websiteInfoState : WebsiteInfoState, expectedBaseDomain : String) {
        view.update(websiteInfoState)

        Assert.assertTrue(binding.root.isVisible)
        Assert.assertEquals(expectedBaseDomain, view.baseDomain)

        binding.clearSiteData.callOnClick()
        verify {
            view.askToClear()
        }
    }
}