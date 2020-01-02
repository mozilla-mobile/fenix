/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.share

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.android.synthetic.main.fragment_share.view.*
import mozilla.components.browser.state.action.ContentAction
import mozilla.components.browser.state.selector.findTabOrCustomTab
import mozilla.components.concept.engine.prompt.PromptRequest
import mozilla.components.feature.accounts.push.SendTabUseCases
import org.mozilla.fenix.R
import org.mozilla.fenix.components.FenixSnackbar
import org.mozilla.fenix.ext.getRootView
import org.mozilla.fenix.ext.requireComponents

class ShareFragment : AppCompatDialogFragment() {

    private val args by navArgs<ShareFragmentArgs>()
    private val viewModel: ShareViewModel by viewModels {
        AndroidViewModelFactory(requireActivity().application)
    }
    private lateinit var shareInteractor: ShareInteractor
    private lateinit var shareCloseView: ShareCloseView
    private lateinit var shareToAccountDevicesView: ShareToAccountDevicesView
    private lateinit var shareToAppsView: ShareToAppsView

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewModel.loadDevicesAndApps()
    }

    override fun dismiss() {
        consumePrompt { onDismiss() }
        super.dismiss()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.ShareDialogStyle)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_share, container, false)
        val shareData = args.data.toList()

        val accountManager = requireComponents.backgroundServices.accountManager

        shareInteractor = ShareInteractor(
            DefaultShareController(
                context = requireContext(),
                shareData = shareData,
                snackbar = FenixSnackbar.makeWithToolbarPadding(requireActivity().getRootView()!!),
                navController = findNavController(),
                sendTabUseCases = SendTabUseCases(accountManager)
            ) { result ->
                consumePrompt {
                    when (result) {
                        ShareController.Result.DISMISSED -> onDismiss()
                        ShareController.Result.SHARE_ERROR -> onFailure()
                        ShareController.Result.SUCCESS -> onSuccess()
                    }
                }
                super.dismiss()
            }
        )

        view.shareWrapper.setOnClickListener { shareInteractor.onShareClosed() }
        shareToAccountDevicesView =
            ShareToAccountDevicesView(view.devicesShareLayout, shareInteractor)

        if (args.showPage) {
            // Show the previous fragment underneath the share background scrim
            // by making it translucent.
            view.closeSharingScrim.alpha = SHOW_PAGE_ALPHA
            view.shareWrapper.setOnClickListener { shareInteractor.onShareClosed() }
        } else {
            // Otherwise, show a list of tabs to share.
            view.closeSharingScrim.alpha = 1.0f
            shareCloseView = ShareCloseView(view.closeSharingContent, shareInteractor)
            shareCloseView.setTabs(shareData)
        }
        shareToAppsView = ShareToAppsView(view.appsShareLayout, shareInteractor)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.devicesList.observe(viewLifecycleOwner) { devicesShareOptions ->
            shareToAccountDevicesView.setShareTargets(devicesShareOptions)
        }
        viewModel.appsList.observe(viewLifecycleOwner) { appsToShareTo ->
            shareToAppsView.setShareTargets(appsToShareTo)
        }
    }

    /**
     * If [ShareFragmentArgs.sessionId] is set and the session has a pending Web Share
     * prompt request, call [consume] then clean up the prompt.
     */
    private fun consumePrompt(
        consume: PromptRequest.Share.() -> Unit
    ) {
        val browserStore = requireComponents.core.store
        args.sessionId
            ?.let { sessionId -> browserStore.state.findTabOrCustomTab(sessionId) }
            ?.let { tab ->
                val promptRequest = tab.content.promptRequest
                if (promptRequest is PromptRequest.Share) {
                    consume(promptRequest)
                    browserStore.dispatch(ContentAction.ConsumePromptRequestAction(tab.id))
                }
            }
    }

    companion object {
        const val SHOW_PAGE_ALPHA = 0.6f
    }
}
