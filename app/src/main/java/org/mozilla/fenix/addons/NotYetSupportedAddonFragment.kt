/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.addons

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import mozilla.components.feature.addons.Addon
import mozilla.components.feature.addons.ui.UnsupportedAddonsAdapter
import mozilla.components.feature.addons.ui.UnsupportedAddonsAdapterDelegate
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.showToolbar

/**
 * Fragment for displaying and managing add-ons that are not yet supported by the browser.
 */
class NotYetSupportedAddonFragment : Fragment(), UnsupportedAddonsAdapterDelegate {
    private val addons: List<Addon> by lazy {
        NotYetSupportedAddonFragmentArgs.fromBundle(requireNotNull(arguments)).addons.toList()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_not_yet_supported_addons, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView: RecyclerView = view.findViewById(R.id.unsupported_add_ons_list)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = UnsupportedAddonsAdapter(
            addonManager = requireContext().components.addonManager,
            unsupportedAddonsAdapterDelegate = this@NotYetSupportedAddonFragment,
            unsupportedAddons = addons
        )
    }

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.mozac_feature_addons_unsupported_section))
    }

    override fun onUninstallError(addonId: String, throwable: Throwable) {
        this@NotYetSupportedAddonFragment.view?.let { view ->
            showSnackBar(view, getString(R.string.mozac_feature_addons_failed_to_remove, ""))
        }
    }

    override fun onUninstallSuccess() {
        this@NotYetSupportedAddonFragment.view?.let { view ->
            showSnackBar(view, getString(R.string.mozac_feature_addons_successfully_removed, ""))
        }
    }
}
