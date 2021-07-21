package org.mozilla.fenix.feature.history

import androidx.appsearch.app.AppSearchSession
import androidx.appsearch.app.PutDocumentsRequest
import androidx.appsearch.app.SearchResult
import androidx.appsearch.app.SearchSpec
import androidx.appsearch.app.SearchSpec.RANKING_STRATEGY_USAGE_COUNT
import androidx.appsearch.app.SetSchemaRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import mozilla.components.support.base.log.logger.Logger

class HistorySearchStorage(
    private val session: Deferred<AppSearchSession>,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : HistorySearchStorageDelegate{
    val logger = Logger("HistorySearchStorage")
    val scope = CoroutineScope(ioDispatcher) + SupervisorJob()

    init {
        scope.launch {
            val setSchemaRequest =
                SetSchemaRequest.Builder().addDocumentClasses(HistoryDocument::class.java)
                    .build()
            session.await().setSchema(setSchemaRequest)
        }
    }

    suspend fun search(term: String): List<SearchResult> = withContext(scope.coroutineContext) {
        val searchSpec = SearchSpec.Builder()
            .addFilterNamespaces(HISTORY_DOCUMENT_NAMESPACE)
            .setRankingStrategy(SearchSpec.RANKING_STRATEGY_RELEVANCE_SCORE)
            .build();

        val result =session.await().search(term, searchSpec).nextPage.await()
        val first = result.firstOrNull() ?: return@withContext emptyList()
        println("this is the result imageUrl: ${first.getDocument(HistoryDocument::class.java).imageUrl}")
        println("this is the result title: ${first.getDocument(HistoryDocument::class.java).title}")
        println("this is the result: ${first.getDocument(HistoryDocument::class.java).text}")
        println("this is the result id: ${first.genericDocument.id}")
        println("this is the result rankingSignal: ${first.rankingSignal}")
        println("this is the result creationTimestampMillis: ${first.genericDocument.creationTimestampMillis}")
        result
    }

    override fun store(title: String?, url: String, content: String, imageUrl: String?) {
        val note = HistoryDocument(
            id = url,
            title = title,
            text = content.take(20_000),
            score = 1001,
            imageUrl = imageUrl
        )

        val putRequest = PutDocumentsRequest.Builder().addDocuments(note).build()

        scope.launch {
            val result = session.await().put(putRequest).await()
            if (result.isSuccess) {
                logger.info(result.successes.toString())
            } else {
                logger.error(result.failures.toString())
            }
            search("nicolas")
        }
    }
}