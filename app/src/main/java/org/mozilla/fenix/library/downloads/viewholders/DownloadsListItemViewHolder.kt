/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.downloads.viewholders

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.download_list_item.view.*
import kotlinx.android.synthetic.main.library_site_item.view.*
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.hideAndDisable
import org.mozilla.fenix.library.SelectionHolder
import org.mozilla.fenix.library.downloads.DownloadInteractor
import org.mozilla.fenix.library.downloads.DownloadItem
import mozilla.components.feature.downloads.toMegabyteString

class DownloadsListItemViewHolder(
    view: View,
    private val downloadInteractor: DownloadInteractor,
    private val selectionHolder: SelectionHolder<DownloadItem>
) : RecyclerView.ViewHolder(view) {

    private var item: DownloadItem? = null

    fun bind(
        item: DownloadItem
    ) {
        itemView.download_layout.visibility = View.VISIBLE
        itemView.download_layout.titleView.text = item.fileName
        itemView.download_layout.urlView.text = item.size.toLong().toMegabyteString()

        itemView.download_layout.setSelectionInteractor(item, selectionHolder, downloadInteractor)
        itemView.download_layout.changeSelected(item in selectionHolder.selectedItems)

        itemView.overflow_menu.hideAndDisable()

        assignIconToFile(item.contentType, item.fileName)
        itemView.favicon.isClickable = false

        this.item = item
    }

    private fun getIconCornerCases(fileName: String?): Int {
        if (fileName != null) {
            if (fileName.endsWith("apk")) {
                return R.drawable.ic_file_type_apk
            }

            // Content Type for ZIP is often octet-stream
            // so we need to check if Zip here
            else if (fileName.endsWith("zip")) {
                return R.drawable.ic_file_type_zip
            }
        }

        return R.drawable.ic_file_type_default
    }

    private fun assignIconToFile(contentType: String?, fileName: String?) {
        when (contentType) {
            // Image Types
            "image/tiff" -> itemView.favicon.setImageResource(R.drawable.ic_file_type_image)
            "image/png" -> itemView.favicon.setImageResource(R.drawable.ic_file_type_image)
            "image/jpeg" -> itemView.favicon.setImageResource(R.drawable.ic_file_type_image)
            "image/jpg" -> itemView.favicon.setImageResource(R.drawable.ic_file_type_image)
            "image/gif" -> itemView.favicon.setImageResource(R.drawable.ic_file_type_image)
            "image/bmp" -> itemView.favicon.setImageResource(R.drawable.ic_file_type_image)
            "image/*" -> itemView.favicon.setImageResource(R.drawable.ic_file_type_image)

            // Audio Types
            "audio/*" -> itemView.favicon.setImageResource(R.drawable.ic_file_type_audio_note)
            "audio/midi" -> itemView.favicon.setImageResource(R.drawable.ic_file_type_audio_note)
            "audio/xmidi" -> itemView.favicon.setImageResource(R.drawable.ic_file_type_audio_note)
            "audio/mp3" -> itemView.favicon.setImageResource(R.drawable.ic_file_type_audio_note)
            "audio/mpeg3" -> itemView.favicon.setImageResource(R.drawable.ic_file_type_audio_note)
            "audio/mpeg" -> itemView.favicon.setImageResource(R.drawable.ic_file_type_audio_note)
            "audio/aac" -> itemView.favicon.setImageResource(R.drawable.ic_file_type_audio_note)
            "audio/oog" -> itemView.favicon.setImageResource(R.drawable.ic_file_type_audio_note)
            "audio/opus" -> itemView.favicon.setImageResource(R.drawable.ic_file_type_audio_note)
            "audio/wav" -> itemView.favicon.setImageResource(R.drawable.ic_file_type_audio_note)
            "audio/webm" -> itemView.favicon.setImageResource(R.drawable.ic_file_type_audio_note)
            "audio/3gpp" -> itemView.favicon.setImageResource(R.drawable.ic_file_type_audio_note)
            "audio/3gpp2" -> itemView.favicon.setImageResource(R.drawable.ic_file_type_audio_note)

            // Video Types
            "video/x-msvideo" -> itemView.favicon.setImageResource(R.drawable.ic_file_type_video)
            "video/mpeg" -> itemView.favicon.setImageResource(R.drawable.ic_file_type_video)
            "video/ogg" -> itemView.favicon.setImageResource(R.drawable.ic_file_type_video)
            "video/mp4" -> itemView.favicon.setImageResource(R.drawable.ic_file_type_video)
            "video/mp2t" -> itemView.favicon.setImageResource(R.drawable.ic_file_type_video)
            "video/webm" -> itemView.favicon.setImageResource(R.drawable.ic_file_type_video)
            "video/3gpp" -> itemView.favicon.setImageResource(R.drawable.ic_file_type_video)
            "video/3gpp2" -> itemView.favicon.setImageResource(R.drawable.ic_file_type_video)

            // Document Types
            "application/x-abiword" -> itemView.favicon.setImageResource(R.drawable.ic_file_type_document)
            "text/csv" -> itemView.favicon.setImageResource(R.drawable.ic_file_type_document)
            "application/msword" -> itemView.favicon.setImageResource(R.drawable.ic_file_type_document)
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ->
                itemView.favicon.setImageResource(R.drawable.ic_file_type_document)
            "application/vnd.oasis.opendocument.presentation" ->
                itemView.favicon.setImageResource(R.drawable.ic_file_type_document)
            "application/vnd.oasis.opendocument.spreadsheet" ->
                itemView.favicon.setImageResource(R.drawable.ic_file_type_document)
            "application/vnd.oasis.opendocument.text" ->
                itemView.favicon.setImageResource(R.drawable.ic_file_type_document)
            "application/pdf" -> itemView.favicon.setImageResource(R.drawable.ic_file_type_document)
            "application/vnd.ms-powerpoint" -> itemView.favicon.setImageResource(R.drawable.ic_file_type_document)
            "application/vnd.openxmlformats-officedocument.presentationml.presentation" ->
                itemView.favicon.setImageResource(R.drawable.ic_file_type_document)
            "application/rtf" -> itemView.favicon.setImageResource(R.drawable.ic_file_type_document)
            "text/plain" -> itemView.favicon.setImageResource(R.drawable.ic_file_type_document)
            "application/vnd.ms-excel" -> itemView.favicon.setImageResource(R.drawable.ic_file_type_document)
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" ->
                itemView.favicon.setImageResource(R.drawable.ic_file_type_document)

            // Archive Types
            "application/x-freearc" -> itemView.favicon.setImageResource(R.drawable.ic_file_type_zip)
            "application/x-bzip" -> itemView.favicon.setImageResource(R.drawable.ic_file_type_zip)
            "application/x-bzip2" -> itemView.favicon.setImageResource(R.drawable.ic_file_type_zip)
            "application/gzip" -> itemView.favicon.setImageResource(R.drawable.ic_file_type_zip)
            "application/vnd.rar" -> itemView.favicon.setImageResource(R.drawable.ic_file_type_zip)
            "application/x-tar" -> itemView.favicon.setImageResource(R.drawable.ic_file_type_zip)
            "application/x-7z-compressed" -> itemView.favicon.setImageResource(R.drawable.ic_file_type_zip)

            // Other
            else -> itemView.favicon.setImageResource(getIconCornerCases(fileName))
        }
    }

    companion object {
        const val LAYOUT_ID = R.layout.download_list_item
    }
}
