package org.mozilla.fenix.feature.history

import android.view.View
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.history_search_suggestion.*
import mozilla.components.browser.awesomebar.BrowserAwesomeBar
import mozilla.components.browser.awesomebar.layout.DefaultSuggestionLayout
import mozilla.components.browser.awesomebar.layout.SuggestionLayout
import mozilla.components.browser.awesomebar.layout.SuggestionViewHolder
import mozilla.components.concept.awesomebar.AwesomeBar
import org.mozilla.fenix.R

object HistorySearchSuggestionLayout : SuggestionLayout {
    private val defaultLayout = DefaultSuggestionLayout()
    override fun createViewHolder(
        awesomeBar: BrowserAwesomeBar,
        view: View,
        layoutId: Int
    ): SuggestionViewHolder {
        if (layoutId == HistorySearchSuggestionViewHolder.LAYOUT_ID) {
            return HistorySearchSuggestionViewHolder(view)
        }
        return defaultLayout.createViewHolder(awesomeBar, view, layoutId)
    }

    override fun getLayoutResource(suggestion: AwesomeBar.Suggestion): Int {
        if (suggestion.provider is HistorySearchSuggestionProvider) {
            return HistorySearchSuggestionViewHolder.LAYOUT_ID
        }
        return defaultLayout.getLayoutResource(suggestion)
    }
}

class HistorySearchSuggestionViewHolder(
    override val containerView: View
) : SuggestionViewHolder(containerView), LayoutContainer {
    override fun bind(
        suggestion: AwesomeBar.Suggestion,
        customizeForBottomToolbar: Boolean,
        selectionListener: () -> Unit
    ) {
        history_search_icon.setImageBitmap(suggestion.icon)
        history_search_title.text = suggestion.title
        history_search_description.text = suggestion.description
        history_search_url.text = suggestion.editSuggestion
            ?.toShortUrl(containerView.context.components.publicSuffixList)
            ?.take(MAX_URI_LENGTH)

        containerView.setOnClickListener {
            suggestion.onSuggestionClicked?.invoke()
        }
    }

    override fun recycle() {
        // remove image bitmap here.
    }

    companion object {
        const val LAYOUT_ID = R.layout.history_search_suggestion
    }
}