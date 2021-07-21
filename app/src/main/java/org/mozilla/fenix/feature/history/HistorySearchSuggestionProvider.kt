package org.mozilla.fenix.feature.history


import java.util.UUID
import mozilla.components.concept.awesomebar.AwesomeBar
import mozilla.components.feature.session.SessionUseCases

class HistorySearchSuggestionProvider(
    private val storage: HistorySearchStorage,
    private val loadUrlUseCase: SessionUseCases.LoadUrlUseCase,
) : AwesomeBar.SuggestionProvider {

    override val id: String = UUID.randomUUID().toString()

    override suspend fun onInputChanged(text: String): List<AwesomeBar.Suggestion> {
        if (text.isEmpty()) {
            return emptyList()
        }

        val result = storage.search(text).firstOrNull()?.getDocument(HistoryDocument::class.java)
            ?: return emptyList()

        val item = AwesomeBar.Suggestion(
            provider = this,
           title = result.title,
            description = result.text.take(100),
            onSuggestionClicked = {
                loadUrlUseCase.invoke(result.id)
            }
        )

        return listOf(item)
    }
}