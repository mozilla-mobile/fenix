/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.trackingprotection

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.appcompat.view.ContextThemeWrapper
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenStarted
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.android.synthetic.main.fragment_tracking_protection.view.*
import kotlinx.coroutines.launch
import mozilla.components.browser.session.Session
import mozilla.components.concept.engine.content.blocking.Tracker
import mozilla.components.feature.session.TrackingProtectionUseCases
import mozilla.components.lib.state.ext.observe
import mozilla.components.support.base.feature.BackHandler
import mozilla.components.support.base.log.logger.Logger
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.components.StoreProvider
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.metrics
import org.mozilla.fenix.ext.nav
import org.mozilla.fenix.ext.requireComponents

class TrackingProtectionPanelDialogFragment : AppCompatDialogFragment(), BackHandler {

    private val safeArguments get() = requireNotNull(arguments)

    private val sessionId: String by lazy {
        TrackingProtectionPanelDialogFragmentArgs.fromBundle(
            safeArguments
        ).sessionId
    }

    private val url: String by lazy {
        TrackingProtectionPanelDialogFragmentArgs.fromBundle(safeArguments).url
    }

    private val trackingProtectionEnabled: Boolean by lazy {
        TrackingProtectionPanelDialogFragmentArgs.fromBundle(safeArguments)
            .trackingProtectionEnabled
    }

    private val promptGravity: Int by lazy {
        TrackingProtectionPanelDialogFragmentArgs.fromBundle(
            safeArguments
        ).gravity
    }

    private fun inflateRootView(container: ViewGroup? = null): View {
        val contextThemeWrapper = ContextThemeWrapper(
            activity,
            (activity as HomeActivity).themeManager.currentThemeResource
        )
        return LayoutInflater.from(contextThemeWrapper).inflate(
            R.layout.fragment_tracking_protection,
            container,
            false
        )
    }

    private lateinit var trackingProtectionStore: TrackingProtectionStore
    private lateinit var trackingProtectionView: TrackingProtectionPanelView
    private lateinit var trackingProtectionInteractor: TrackingProtectionPanelInteractor

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflateRootView(container)
        val session = requireComponents.core.sessionManager.findSessionById(sessionId)
        session?.register(sessionObserver, view = view)
        trackingProtectionStore = StoreProvider.get(this) {
            TrackingProtectionStore(
                TrackingProtectionState(
                    session,
                    url,
                    trackingProtectionEnabled,
                    listOf(),
                    TrackingProtectionState.Mode.Normal
                )
            )
        }
        trackingProtectionInteractor = TrackingProtectionPanelInteractor(
            trackingProtectionStore,
            ::toggleTrackingProtection,
            ::openTrackingProtectionSettings
        )
        trackingProtectionView =
            TrackingProtectionPanelView(view.fragment_tp, trackingProtectionInteractor)
        updateTrackers()
        return view
    }

    private val sessionObserver = object : Session.Observer {
        override fun onUrlChanged(session: Session, url: String) {
            trackingProtectionStore.dispatch(
                TrackingProtectionAction.UrlChange(url)
            )
        }

        override fun onTrackerBlocked(session: Session, tracker: Tracker, all: List<Tracker>) {
            updateTrackers()
        }

        override fun onTrackerLoaded(session: Session, tracker: Tracker, all: List<Tracker>) {
            updateTrackers()
        }
    }

    private fun updateTrackers() {
        context?.let { context ->
            val session =
                context.components.core.sessionManager.findSessionById(sessionId) ?: return
            val useCase = TrackingProtectionUseCases(
                sessionManager = context.components.core.sessionManager,
                engine = context.components.core.engine
            )

            useCase.fetchTrackingLogs(
                session,
                onSuccess = {
                    trackingProtectionStore.dispatch(
                        TrackingProtectionAction.TrackerLogChange(it)
                    )
                },
                onError = {
                    Logger.error("TrackingProtectionUseCases - fetchTrackingLogs onError", it)
                }
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        trackingProtectionStore.observe(view) {
            viewLifecycleOwner.lifecycleScope.launch {
                whenStarted {
                    trackingProtectionView.update(it)
                }
            }
        }
    }

    private fun openTrackingProtectionSettings() {
        requireContext().metrics.track(Event.TrackingProtectionSettingsPanel)
        nav(
            R.id.trackingProtectionPanelDialogFragment,
            TrackingProtectionPanelDialogFragmentDirections
                .actionTrackingProtectionPanelDialogFragmentToTrackingProtectionFragment()
        )
    }

    private fun toggleTrackingProtection(isEnabled: Boolean) {
        context?.let { context ->
            val useCase = TrackingProtectionUseCases(
                sessionManager = context.components.core.sessionManager,
                engine = context.components.core.engine
            )
            val session = context.components.core.sessionManager.findSessionById(sessionId)
            session?.let {
                if (isEnabled) {
                    useCase.removeException(it)
                } else {
                    context.metrics.track(Event.TrackingProtectionException)
                    useCase.addException(it)
                }

                with(context.components) {
                    useCases.sessionUseCases.reload.invoke(session)
                }
            }
        }
        trackingProtectionStore.dispatch(TrackingProtectionAction.TrackerBlockingChanged(isEnabled))
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return if (promptGravity == Gravity.BOTTOM) {
            object : BottomSheetDialog(requireContext(), this.theme) {
                override fun onBackPressed() {
                    this@TrackingProtectionPanelDialogFragment.onBackPressed()
                }
            }.apply {
                setOnShowListener {
                    val bottomSheet =
                        findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout
                    val behavior = BottomSheetBehavior.from(bottomSheet)
                    behavior.state = BottomSheetBehavior.STATE_EXPANDED
                }
            }
        } else {
            object : Dialog(requireContext()) {
                override fun onBackPressed() {
                    this@TrackingProtectionPanelDialogFragment.onBackPressed()
                }
            }.applyCustomizationsForTopDialog(inflateRootView())
        }
    }

    private fun Dialog.applyCustomizationsForTopDialog(rootView: View): Dialog {
        addContentView(
            rootView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        )

        window?.apply {
            setGravity(promptGravity)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            // This must be called after addContentView, or it won't fully fill to the edge.
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        return this
    }

    override fun onBackPressed(): Boolean {
        if (!trackingProtectionView.onBackPressed()) {
            dismiss()
        }
        return true
    }
}
