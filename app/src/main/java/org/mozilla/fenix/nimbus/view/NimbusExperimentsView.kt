/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.nimbus.view

import android.content.Context
import androidx.navigation.NavController
import mozilla.components.service.nimbus.ui.NimbusExperimentsAdapterDelegate
import org.mozilla.experiments.nimbus.AvailableExperiment
import org.mozilla.fenix.R
import org.mozilla.fenix.components.FenixSnackbar
import org.mozilla.fenix.ext.getRootView
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.nimbus.NimbusExperimentsFragmentDirections
import org.mozilla.fenix.tabstray.ext.make

/**
 * View used for managing Nimbus experiments.
 */
class NimbusExperimentsView(
    private val context: Context,
    private val navController: NavController
) : NimbusExperimentsAdapterDelegate {

    override fun onExperimentItemClicked(experiment: AvailableExperiment) {
        if (context.settings().isExperimentationEnabled) {
            val directions =
                NimbusExperimentsFragmentDirections.actionNimbusExperimentsFragmentToNimbusBranchesFragment(
                    experimentId = experiment.slug,
                    experimentName = experiment.userFacingName
                )

            navController.navigate(directions)
        } else {
            val snackbarText = context.getString(R.string.experiments_snackbar)
            FenixSnackbar.make(view = context.getRootView()!!)
                .setText(snackbarText)
                .show()
        }
    }
}
