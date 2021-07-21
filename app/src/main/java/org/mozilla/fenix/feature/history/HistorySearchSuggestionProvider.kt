package org.mozilla.fenix.feature.history


import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mozilla.components.concept.awesomebar.AwesomeBar
import mozilla.components.concept.fetch.Client
import mozilla.components.concept.fetch.Request
import mozilla.components.concept.fetch.isSuccess
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.support.ktx.kotlin.sanitizeURL

class HistorySearchSuggestionProvider(
    private val storage: HistorySearchStorage,
    private val loadUrlUseCase: SessionUseCases.LoadUrlUseCase,
    private val client: Client
) : AwesomeBar.SuggestionProvider {

    override val id: String = UUID.randomUUID().toString()

    override suspend fun onInputChanged(text: String): List<AwesomeBar.Suggestion> {
        if (text.isEmpty()) {
            return emptyList()
        }

        val result = storage.search(text).firstOrNull()?.getDocument(HistoryDocument::class.java)
            ?: return emptyList()

        val icon = withContext(Dispatchers.IO) {
            getIconBitmap(result.imageUrl)
        }

        val item = AwesomeBar.Suggestion(
            provider = this,
            icon = icon,
            title = result.title,
            description = result.text.take(100),
            onSuggestionClicked = {
                loadUrlUseCase.invoke(result.id)
            }
        )
        return listOf(item)
    }


    suspend fun getIconBitmap(iconUrl: String?): Bitmap? {
        var bitmap: Bitmap? = null
        if (!iconUrl.isNullOrBlank()) {
            client.fetch(
                Request(url = iconUrl.sanitizeURL())
            ).use { response ->
                if (response.isSuccess) {
                    response.body.useStream {
                        bitmap = BitmapFactory.decodeStream(it)
                    }
                }
            }
        }

        return bitmap
    }
}