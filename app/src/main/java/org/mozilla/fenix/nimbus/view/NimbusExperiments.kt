/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.nimbus.view

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.mozilla.experiments.nimbus.AvailableExperiment
import org.mozilla.fenix.compose.list.TextListItem
import org.mozilla.fenix.theme.FirefoxTheme

/**
 * List of Nimbus Experiments.
 *
 * @param experiments List of [AvailableExperiment] that are going to be displayed.
 * @param onExperimentClick Invoked when the user clicks on an [AvailableExperiment].
 */
@Composable
fun NimbusExperiments(
    availableExperiments: List<AvailableExperiment> = listOf(),
    onSelectedExperiment: (AvailableExperiment) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        items(availableExperiments) { experiment ->
            TextListItem(
                label = experiment.userFacingName,
                description = experiment.userFacingDescription,
                onClick = {
                    onSelectedExperiment(experiment)
                },
            )
        }
    }
}


@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
private fun NimbusExperimentsPreview() {
    val testExperiment = AvailableExperiment(
        userFacingName = "user facing mane",
        userFacingDescription = "user facing description",
        slug = "slug",
        branches = emptyList(),
        referenceBranch = null,
    )
    FirefoxTheme {
        NimbusExperiments(
            availableExperiments = listOf(
                testExperiment,
                testExperiment,
                testExperiment,
                testExperiment,
            ),
            onSelectedExperiment = {},
        )
    }
}
