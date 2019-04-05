/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.crashes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import kotlinx.android.synthetic.main.fragment_crash_reporter.*
import mozilla.components.browser.session.Session
import mozilla.components.lib.crash.Crash
import org.mozilla.fenix.R
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.utils.Settings

class CrashReporterFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_crash_reporter, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val crash = Crash.fromIntent(CrashReporterFragmentArgs.fromBundle(arguments!!).crashIntent)

        view.findViewById<TextView>(R.id.title).text =
            getString(R.string.tab_crash_title, context!!.getString(R.string.app_name))

        requireContext().components.analytics.metrics.track(Event.CrashReporterOpened)

        val selectedSession = requireComponents.core.sessionManager.selectedSession

        restore_tab_button.setOnClickListener {
            selectedSession?.let { session -> closeFragment(true, session, crash) }
        }

        close_tab_button.setOnClickListener {
            selectedSession?.let { session -> closeFragment(false, session, crash) }
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity).supportActionBar?.hide()
    }

    private fun closeFragment(shouldRestore: Boolean, session: Session, crash: Crash) {
        submitReportIfNecessary(crash)

        if (shouldRestore) {
            requireComponents.useCases.sessionUseCases.crashRecovery.invoke(session)
            Navigation.findNavController(view!!).popBackStack()
        } else {
            requireComponents.useCases.tabsUseCases.removeTab.invoke(session)
            navigateHome(view!!)
        }
    }

    private fun submitReportIfNecessary(crash: Crash) {
        var didSubmitCrashReport = false
        if (Settings.getInstance(context!!).isCrashReportingEnabled) {
            requireComponents.analytics.crashReporter.submitReport(crash)
            didSubmitCrashReport = true
        }
        requireContext().components.analytics.metrics.track(Event.CrashReporterClosed(didSubmitCrashReport))
    }

    private fun navigateHome(fromView: View) {
        val directions = CrashReporterFragmentDirections.actionCrashReporterFragmentToHomeFragment()
        Navigation.findNavController(fromView).navigate(directions)
    }
}
