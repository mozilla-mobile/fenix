/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.nimbus

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.findFragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mozilla.components.lib.state.ext.consumeFrom
import mozilla.components.lib.state.ext.observeAsComposableState
import mozilla.components.support.base.log.logger.Logger
import org.mozilla.experiments.nimbus.Branch
import org.mozilla.fenix.R
import org.mozilla.fenix.components.StoreProvider
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.showToolbar
import org.mozilla.fenix.nimbus.controller.NimbusBranchesController
import org.mozilla.fenix.theme.FirefoxTheme

/**
 * A fragment to show the branches of a Nimbus experiment.
 */
@Suppress("TooGenericExceptionCaught")
class NimbusBranchesFragment : Fragment() {

    private lateinit var nimbusBranchesStore: NimbusBranchesStore

    private val args by navArgs<NimbusBranchesFragmentArgs>()

    override fun onResume() {
        super.onResume()
        showToolbar(args.experimentName)
    }

    private fun loadExperimentBranches() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val experiments = requireContext().components.analytics.experiments
                val branches = experiments.getExperimentBranches(args.experimentId) ?: emptyList()
                val selectedBranch = experiments.getExperimentBranch(args.experimentId) ?: ""

                nimbusBranchesStore.dispatch(
                    NimbusBranchesAction.UpdateBranches(
                        branches,
                        selectedBranch,
                    ),
                )
            } catch (e: Throwable) {
                Logger.error("Failed to getActiveExperiments()", e)
            }
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {

        nimbusBranchesStore = StoreProvider.get(this) {
            NimbusBranchesStore(NimbusBranchesState(branches = emptyList()))
        }

        val controller = NimbusBranchesController(
            context = requireContext(),
            navController = findNavController(),
            nimbusBranchesStore = nimbusBranchesStore,
            experiments = requireContext().components.analytics.experiments,
            experimentId = args.experimentId,
        )

        loadExperimentBranches()

        return ComposeView(requireContext()).apply {

            setContent {
                FirefoxTheme {

                    val branches = nimbusBranchesStore.observeAsComposableState { state -> state.branches }
                    val selectedBranch = nimbusBranchesStore.observeAsComposableState { state -> state.selectedBranch }

                    NimbusBranches(
                        selectedBranch = selectedBranch.value ?: "",
                        branchesList = branches.value ?: emptyList(),
                        onSelectedBranch = { selected ->
                            controller.onBranchItemClicked(selected)
                        }
                    )

                }
            }
        }
    }


    /**
     * The list for the Nimbus Branches,
     *
     * @param branchesList List of branches
     * @param onSelectedBranch Callback for when item is selected.
     */
    @Composable
    fun NimbusBranches(
        selectedBranch: String,
        branchesList: List<Branch> = listOf(),
        onSelectedBranch: (Branch) -> Unit
    ) {

        LazyColumn (
            modifier = Modifier
                .fillMaxSize()
        ) {
            items(branchesList) { branch ->
                NimbusBranchItem(
                    item = branch,
                    onSelectedBranch = onSelectedBranch,
                    selectedBranch = selectedBranch
                )
            }
        }
    }

    /**
     * Nimbus branch item
     *
     * @param item current branch
     */
    @Composable
    fun NimbusBranchItem(
        item: Branch,
        onSelectedBranch: (Branch) -> Unit,
        selectedBranch: String
    ) {
        Row (modifier = Modifier.fillMaxWidth()
            .clickable {
                onSelectedBranch(item)
            },
            verticalAlignment = Alignment.CenterVertically
        ){
            AnimatedVisibility(item.slug.equals(selectedBranch)) {
                Icon(
                    modifier = Modifier.padding(start = 16.dp),
                    painter = painterResource(R.drawable.mozac_ic_check),
                    contentDescription = stringResource(
                        R.string.content_description_close_button,
                    ),
                    tint = FirefoxTheme.colors.iconPrimary,
                )
            }
            Column (modifier = Modifier.padding(horizontal = 16.dp)){
                Text(
                    text = item.slug,
                    fontSize = 16.sp,
                    maxLines = 1,
                    color = FirefoxTheme.colors.textPrimary,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.slug,
                    fontSize = 12.sp,
                    maxLines = 1,
                    color = FirefoxTheme.colors.textPrimary,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
