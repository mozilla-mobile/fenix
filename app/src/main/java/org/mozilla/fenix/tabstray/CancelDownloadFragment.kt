/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabstray

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.mozilla.fenix.R
import org.mozilla.fenix.databinding.FragmentCancelDownloadBinding
import org.mozilla.fenix.ext.runIfFragmentIsAttached

class CancelDownloadFragment : AppCompatDialogFragment() {

    private var _binding: FragmentCancelDownloadBinding? = null
    private val binding get() = _binding!!
    private val args by navArgs<CancelDownloadFragmentArgs>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        BottomSheetDialog(requireContext(), this.theme).apply {
            setOnShowListener {
                val bottomSheet =
                    findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout
                val behavior = BottomSheetBehavior.from(bottomSheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCancelDownloadBinding.inflate(inflater, container, false)

        binding.cancelDownloadMessage.text = String.format(
            binding.root.context.getString(
                R.string.cancel_active_download_warning_content_description
            ),
            args.downloadCount
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.cancelDownloadAccept.setOnClickListener {
            setFragmentResult(
                RESULT_KEY,
                Bundle().apply {
                    putBoolean(ACCEPT_KEY, true)
                    args.tabId?.let { putString(TAB_KEY, it) }
                    args.source?.let { putString(SOURCE_KEY, it) }
                    putBoolean(PRIVATE_KEY, args.private)
                }
            )
            runIfFragmentIsAttached {
                if (this.isVisible) {
                    dismiss()
                }
                findNavController().popBackStack()
            }
        }

        binding.cancelDownloadDeny.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val RESULT_KEY = "cancelDownloadFragmentResultKey"
        const val ACCEPT_KEY = "cancelDownloadFragmentAcceptKey"
        const val TAB_KEY = "cancelDownloadFragmentTabKey"
        const val SOURCE_KEY = "cancelDownloadFragmentSourceKey"
        const val PRIVATE_KEY = "cancelDownloadFragmentPrivateKey"
    }
}

data class CancelDownloadFragmentArguments(
    val downloadCount: Int,
    val trigger: Trigger,
    val tabId: String? = null,
    val source: String? = null,
    val private: Boolean = true
)

enum class Trigger {
    SINGLE_TAB, CLOSE_ALL
}
