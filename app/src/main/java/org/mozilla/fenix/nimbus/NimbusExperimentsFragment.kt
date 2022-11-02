/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.nimbus

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import org.mozilla.experiments.nimbus.AvailableExperiment
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.showToolbar
import org.mozilla.fenix.theme.FirefoxTheme

/**
 * Fragment use for managing Nimbus experiments.
 */
@Suppress("TooGenericExceptionCaught")
class NimbusExperimentsFragment : Fragment() {

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.preferences_nimbus_experiments))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {

        setContent {
            FirefoxTheme {
                val experiments =
                    requireContext().components.analytics.experiments.getAvailableExperiments()

                NimbusExperiments(
                    availableExperiments = experiments,
                    onSelectedExperiment = { selectedExperiment ->
                        val directions =
                            NimbusExperimentsFragmentDirections.actionNimbusExperimentsFragmentToNimbusBranchesFragment(
                                experimentId = selectedExperiment.slug,
                                experimentName = selectedExperiment.userFacingName,
                            )

                        findNavController().navigate(directions)
                    }
                )
            }
        }
    }

    /**
     * The list for the Nimbus Experiments,
     *
     * @param availableExperiments List of available items
     * @param onSelectedExperiment Callback for when item is selected.
     */
    @Composable
    fun NimbusExperiments(
        availableExperiments: List<AvailableExperiment> = listOf(),
        onSelectedExperiment: (AvailableExperiment) -> Unit
    ) {
        LazyColumn (
            modifier = Modifier
                .fillMaxSize()
        ) {
            items(availableExperiments) { experiment ->
                NimbusExperimentItem(item = experiment, onSelectedExperiment = onSelectedExperiment)
            }
        }
    }
    /**
        * Nimbus experiment item
        *
        * @param item current experiment
    */
    @Composable
    fun NimbusExperimentItem(
        item: AvailableExperiment,
        onSelectedExperiment: (AvailableExperiment) -> Unit
    ) {
        Box (
            modifier = Modifier.clickable {
                    onSelectedExperiment(item)
                }
        ) {
            Column (
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ){
                Text(
                    text = item.userFacingName,
                    fontSize = 16.sp,
                    maxLines = 1,
                    color = FirefoxTheme.colors.textPrimary,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.userFacingDescription,
                    color = FirefoxTheme.colors.textPrimary,
                    fontSize = 14.sp
                )
            }
        }
    }
}
