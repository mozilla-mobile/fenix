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
 * List of Nimbus Experiments,
 *
 * @param availableExperiments list of available experiments that are going to be displayed
 * @param onSelectedExperiment Callback when experiment is selected it returns [AvailableExperiment]
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
            NimbusExperimentItem(experiment = experiment, onSelectedExperiment = onSelectedExperiment)
        }
    }
}

/**
 * Nimbus experiment item
 *
 * @param experiment current experiment
 * @param onSelectedExperiment Callback when experiment is selected it returns [AvailableExperiment]
 */
@Composable
fun NimbusExperimentItem(
    experiment: AvailableExperiment,
    onSelectedExperiment: (AvailableExperiment) -> Unit,
) {
    TextListItem(
        label = experiment.userFacingName,
        description = experiment.userFacingDescription,
        onClick = {
            onSelectedExperiment(experiment)
        },
    )
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
