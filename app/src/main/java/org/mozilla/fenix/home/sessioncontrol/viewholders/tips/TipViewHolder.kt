package org.mozilla.fenix.home.sessioncontrol.viewholders.tips

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.tip_item.view.*
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.increaseTapArea
import org.mozilla.fenix.home.sessioncontrol.SessionControlInteractor
import org.mozilla.fenix.home.sessioncontrol.viewholders.onboarding.setOnboardingIcon

class TipViewHolder(
    val view: View,
    val interactor: SessionControlInteractor
) : RecyclerView.ViewHolder(view) {
    var tip: Tip? = null

    fun bind(tip: Tip) {
        this.tip = tip

        view.apply {
            val appName = context.getString(R.string.app_name)

            tip_header_text.text = view.context.getString(tip.title, appName)
            tip_description_text.text = view.context.getString(tip.description, appName)
            tip_button.text = view.context.getString(tip.button)

            tip_button.setOnClickListener {
                tip.action.invoke()
            }

            tip_header_text.setOnboardingIcon(tip.icon, tip.shouldColorIcon)

            when (tip.priority) {
                TipPriority.HIGH -> {
                    tip_close.visibility = View.GONE
                }

                else -> {
                    tip_close.visibility = View.VISIBLE

                    tip_close.increaseTapArea(36)

                    tip_close.setOnClickListener {
                        interactor.onCloseTip(tip)
                    }
                }
            }
        }
    }

    companion object {
        const val LAYOUT_ID = R.layout.tip_item
    }
}
