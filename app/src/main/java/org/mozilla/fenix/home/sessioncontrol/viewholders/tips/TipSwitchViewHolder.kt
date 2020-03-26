package org.mozilla.fenix.home.sessioncontrol.viewholders.tips

import android.view.View
import androidx.core.content.edit
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.tip_item.view.tip_close
import kotlinx.android.synthetic.main.tip_item.view.tip_description_text
import kotlinx.android.synthetic.main.tip_item.view.tip_header_text
import kotlinx.android.synthetic.main.tip_switch_item.view.*
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.increaseTapArea
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.home.sessioncontrol.SessionControlInteractor
import org.mozilla.fenix.home.sessioncontrol.viewholders.onboarding.setOnboardingIcon

class TipSwitchViewHolder(
    val view: View,
    val interactor: SessionControlInteractor
) : RecyclerView.ViewHolder(view) {
    var tip: Tip? = null

    fun bind(tip: Tip) {
        this.tip = tip

        view.apply {
            val appName = context.getString(R.string.app_name)
            val preferenceKey = context.getString(tip.preferenceKey!!)

            tip_header_text.text = view.context.getString(tip.title, appName)
            tip_description_text.text = view.context.getString(tip.description, appName)

            tip_switch_description.text = view.context.getString(tip.button)

            tip_header_text.setOnboardingIcon(tip.icon, tip.shouldColorIcon)

            tip_switch.isChecked = context.settings().preferences.getBoolean(preferenceKey, false)

            tip_switch.setOnCheckedChangeListener { _, value ->
                context.settings().preferences.edit {
                    putBoolean(preferenceKey, value)
                }
            }

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
        const val LAYOUT_ID = R.layout.tip_switch_item
    }
}
