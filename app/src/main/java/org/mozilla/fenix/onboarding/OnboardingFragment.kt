/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.onboarding

import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import mozilla.components.browser.state.state.selectedOrDefaultSearchEngine
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.lib.state.ext.consumeFlow
import mozilla.components.lib.state.ext.consumeFrom
import mozilla.components.support.ktx.kotlinx.coroutines.flow.ifChanged
import mozilla.components.ui.tabcounter.TabCounterMenu
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.browser.BrowserAnimator
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.components.StoreProvider
import org.mozilla.fenix.components.toolbar.FenixTabCounterMenu
import org.mozilla.fenix.databinding.FragmentHomeBinding
import org.mozilla.fenix.ext.nav
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.home.HomeFragmentDirections
import org.mozilla.fenix.onboarding.interactor.DefaultOnboardingInteractor
import org.mozilla.fenix.onboarding.interactor.OnboardingInteractor
import org.mozilla.fenix.onboarding.view.OnboardingView
import org.mozilla.fenix.theme.ThemeManager
import java.lang.ref.WeakReference

/**
 * TODO
 */
class OnboardingFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var onboardingStore: OnboardingStore
    private lateinit var interactor: OnboardingInteractor
    private lateinit var onboardingAccountObserver: OnboardingAccountObserver
    private lateinit var onboardingView: OnboardingView

    private val store: BrowserStore
        get() = requireComponents.core.store

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        onboardingStore = StoreProvider.get(this) {
            OnboardingStore()
        }

        onboardingAccountObserver = OnboardingAccountObserver(
            context = requireContext(),
            dispatchChanges = { mode ->
                if (mode != onboardingStore.state.state) {
                    onboardingStore.dispatch(OnboardingFragmentAction.UpdateState(mode))
                }
            }
        )

        interactor = DefaultOnboardingInteractor()

        onboardingView = OnboardingView(
            containerView = binding.sessionControlRecyclerView,
            interactor = interactor
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeSearchEngineChanges()
        createHomeMenu(requireContext(), WeakReference(binding.menuButton))
        createTabCounterMenu()

        binding.menuButton.setColorFilter(
            ContextCompat.getColor(
                requireContext(),
                ThemeManager.resolveAttribute(R.attr.textPrimary, requireContext())
            )
        )

        binding.toolbar.compoundDrawablePadding =
            view.resources.getDimensionPixelSize(R.dimen.search_bar_search_engine_icon_padding)
        binding.toolbarWrapper.setOnClickListener {
            navigateToSearch()
        }

        binding.tabButton.setOnClickListener {
            openTabsTray()
        }

        consumeFrom(onboardingStore) { state ->
            onboardingView.update(state)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }

    private fun observeSearchEngineChanges() {
        consumeFlow(store) { flow ->
            flow.map { state -> state.search.selectedOrDefaultSearchEngine }
                .ifChanged()
                .collect { searchEngine ->
                    if (searchEngine != null) {
                        val iconSize =
                            requireContext().resources.getDimensionPixelSize(R.dimen.preference_icon_drawable_size)
                        val searchIcon =
                            BitmapDrawable(requireContext().resources, searchEngine.icon)
                        searchIcon.setBounds(0, 0, iconSize, iconSize)
                        binding.searchEngineIcon.setImageDrawable(searchIcon)
                    } else {
                        binding.searchEngineIcon.setImageDrawable(null)
                    }
                }
        }
    }

    private fun navigateToSearch() {
        val directions =
            OnboardingFragmentDirections.actionGlobalSearchDialog(
                sessionId = null
            )

        nav(R.id.homeFragment, directions, BrowserAnimator.getToolbarNavOptions(requireContext()))
    }

    private fun createTabCounterMenu() {
        val browsingModeManager = (activity as HomeActivity).browsingModeManager
        val mode = browsingModeManager.mode

        val onItemTapped: (TabCounterMenu.Item) -> Unit = {
            if (it is TabCounterMenu.Item.NewTab) {
                browsingModeManager.mode = BrowsingMode.Normal
            } else if (it is TabCounterMenu.Item.NewPrivateTab) {
                browsingModeManager.mode = BrowsingMode.Private
            }
        }

        val tabCounterMenu = FenixTabCounterMenu(
            requireContext(),
            onItemTapped,
            iconColor = if (mode == BrowsingMode.Private) {
                ContextCompat.getColor(
                    requireContext(),
                    R.color.fx_mobile_private_text_color_primary
                )
            } else {
                null
            }
        )

        val inverseBrowsingMode = when (mode) {
            BrowsingMode.Normal -> BrowsingMode.Private
            BrowsingMode.Private -> BrowsingMode.Normal
        }

        tabCounterMenu.updateMenu(showOnly = inverseBrowsingMode)
        binding.tabButton.setOnLongClickListener {
            tabCounterMenu.menuController.show(anchor = it)
            true
        }
    }

    private fun openTabsTray() {
        findNavController().nav(
            R.id.homeFragment,
            HomeFragmentDirections.actionGlobalTabsTrayFragment()
        )
    }
}
