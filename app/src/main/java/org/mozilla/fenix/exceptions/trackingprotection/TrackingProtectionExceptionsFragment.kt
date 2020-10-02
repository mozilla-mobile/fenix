/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.exceptions.trackingprotection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_exceptions.view.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.components.lib.state.ext.consumeFrom
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.components.StoreProvider
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.ext.showToolbar

/**
 * Displays a list of sites that are exempted from Tracking Protection,
 * along with controls to remove the exception.
 */
class TrackingProtectionExceptionsFragment : Fragment() {

    private lateinit var exceptionsStore: ExceptionsFragmentStore
    private lateinit var exceptionsView: TrackingProtectionExceptionsView
    private lateinit var exceptionsInteractor: DefaultTrackingProtectionExceptionsInteractor

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.preference_exceptions))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_exceptions, container, false)
        exceptionsStore = StoreProvider.get(this) {
            ExceptionsFragmentStore(
                ExceptionsFragmentState(items = emptyList())
            )
        }
        exceptionsInteractor = DefaultTrackingProtectionExceptionsInteractor(
            activity = activity as HomeActivity,
            exceptionsStore = exceptionsStore,
            trackingProtectionUseCases = requireComponents.useCases.trackingProtectionUseCases
        )
        exceptionsView = TrackingProtectionExceptionsView(
            view.exceptionsLayout,
            exceptionsInteractor
        )
        exceptionsInteractor.reloadExceptions()
        return view
    }

    @ExperimentalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        consumeFrom(exceptionsStore) {
            exceptionsView.update(it.items)
        }
    }
}
