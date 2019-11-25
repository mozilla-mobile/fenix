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
import mozilla.components.feature.sendtab.SendTabUseCases
import org.mozilla.fenix.R
import org.mozilla.fenix.components.FenixSnackbarPresenter
import org.mozilla.fenix.ext.getRootView
import org.mozilla.fenix.ext.requireComponents

class ShareFragment : AppCompatDialogFragment() {

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
        val args by navArgs<ShareFragmentArgs>()
        val shareData = args.data.toList()

        val accountManager = requireComponents.backgroundServices.accountManager

        shareInteractor = ShareInteractor(
            DefaultShareController(
                context = requireContext(),
                shareData = shareData,
                snackbarPresenter = FenixSnackbarPresenter(activity!!.getRootView()!!),
                navController = findNavController(),
                sendTabUseCases = SendTabUseCases(accountManager),
                dismiss = ::dismiss
            )
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

    companion object {
        const val SHOW_PAGE_ALPHA = 0.6f
    }
}
