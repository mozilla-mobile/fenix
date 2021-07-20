package org.mozilla.fenix.feature.history

import androidx.appsearch.annotation.Document
import androidx.appsearch.app.AppSearchSchema
import org.mozilla.fenix.BuildConfig

const val HISTORY_DOCUMENT_NAMESPACE = BuildConfig.APPLICATION_ID + ".history_page"

@Document
data class HistoryDocument(

    @Document.Namespace
    val namespace: String = HISTORY_DOCUMENT_NAMESPACE,

    @Document.Id
    val id: String,

    @Document.Score
    val score: Int,

    @Document.StringProperty(indexingType = AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
    val text: String
)
