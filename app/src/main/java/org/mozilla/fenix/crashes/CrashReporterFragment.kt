/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.crashes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import kotlinx.android.synthetic.main.fragment_crash_reporter.*
import mozilla.components.lib.crash.Crash
import org.mozilla.fenix.R
import org.mozilla.fenix.components.WrappedCrashRecoveryUseCase
import org.mozilla.fenix.components.WrappedCrashReporter
import org.mozilla.fenix.components.WrappedRemoveTabUseCase
import org.mozilla.fenix.components.WrappedSessionManager
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.components.metrics.MetricController
import org.mozilla.fenix.ext.hideActionBar
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.utils.Settings

@SuppressWarnings("TooManyFunctions")
class CrashReporterFragment : Fragment() {

    private lateinit var sessionManager: WrappedSessionManager
    private lateinit var metricsController: MetricController
    private lateinit var crashReporter: WrappedCrashReporter
    private lateinit var crashRecoveryUseCase: WrappedCrashRecoveryUseCase
    private lateinit var removeTabUseCase: WrappedRemoveTabUseCase
    private lateinit var navController: () -> NavController

    @VisibleForTesting
    lateinit var crashReportingEnabled: () -> Boolean

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_crash_reporter, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        injectDependencies()

        initUI()

        trackAnalyticsOpen()
    }

    override fun onResume() {
        super.onResume()
        activity?.hideActionBar()
    }

    private fun injectDependencies() {
        requireComponents.let { components ->
            sessionManager = components.core.wrappedSessionManager
            metricsController = components.analytics.metrics
            crashReporter = components.analytics.wrappedCrashReporter
            crashRecoveryUseCase = components.useCases.wrappedSessionUseCases.crashRecovery
            removeTabUseCase = components.useCases.wrappedTabsUseCases.removeTab
        }

        crashReportingEnabled = { Settings.getInstance(context!!).isCrashReportingEnabled }
        navController = { Navigation.findNavController(requireView()) }
    }

    private fun initUI() {
        title.text = getString(R.string.tab_crash_title_2, getString(R.string.app_name))

        restore_tab_button.setOnClickListener {
            closeFragment(restoreTab = true)
        }
        close_tab_button.setOnClickListener {
            closeFragment(restoreTab = false)
        }
    }

    private fun closeFragment(restoreTab: Boolean) {
        val session = sessionManager.selectedSession ?: return

        val isSubmitted = submitCrashReport()
        trackAnalyticsClosed(isSubmitted)

        if (restoreTab) {
            crashRecoveryUseCase.invoke()
            navigateBack()
        } else {
            removeTabUseCase.invoke(session)
            crashRecoveryUseCase.invoke()
            navigateHome()
        }
    }

    private fun submitCrashReport(): Boolean = if (crashReportingEnabled()) {
        crashReporter.submitReport(getCrashFromArguments())
        true
    } else false

    private fun getCrashFromArguments() = Crash.fromIntent(
        CrashReporterFragmentArgs.fromBundle(arguments!!).crashIntent
    )

    // Navigation

    private fun navigateBack() {
        navController().popBackStack()
    }

    private fun navigateHome() {
        navController().popBackStack(R.id.browserFragment, true)
    }

    // Analytics

    private fun trackAnalyticsOpen() {
        metricsController.track(Event.CrashReporterOpened)
    }

    private fun trackAnalyticsClosed(wasReportSubmitted: Boolean) {
        metricsController.track(Event.CrashReporterClosed(wasReportSubmitted))
    }
}
