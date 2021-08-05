/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabstray.browser

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.ConcatAdapter
import mozilla.components.browser.tabstray.TabViewHolder
import mozilla.components.feature.tabs.tabstray.TabsFeature
import org.mozilla.fenix.FeatureFlags
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.tabstray.ext.browserAdapter
import org.mozilla.fenix.tabstray.ext.inactiveTabsAdapter
import org.mozilla.fenix.tabstray.ext.isNormalTabActive
import org.mozilla.fenix.tabstray.ext.isNormalTabInactive
import java.util.concurrent.TimeUnit

/**
 * The time until which a tab is considered in-active (in days).
 */
const val DEFAULT_ACTIVE_DAYS = 4L

class NormalBrowserTrayList @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractBrowserTrayList(context, attrs, defStyleAttr) {

    /**
     * The maximum time from when a tab was created or accessed until it is considered "inactive".
     */
    var maxActiveTime = TimeUnit.DAYS.toMillis(DEFAULT_ACTIVE_DAYS)

    private val concatAdapter by lazy { adapter as ConcatAdapter }

    override val tabsFeature by lazy {
        val tabsAdapter = concatAdapter.browserAdapter

        TabsFeature(
            tabsAdapter,
            context.components.core.store,
            selectTabUseCase,
            removeTabUseCase,
            { state ->
                if (!FeatureFlags.inactiveTabs) {
                    return@TabsFeature !state.content.private
                }
                state.isNormalTabActive(maxActiveTime)
            },
            {}
        )
    }

    private val inactiveFeature by lazy {
        val tabsAdapter = concatAdapter.inactiveTabsAdapter

        TabsFeature(
            tabsAdapter,
            context.components.core.store,
            selectTabUseCase,
            removeTabUseCase,
            { state ->
                if (!FeatureFlags.inactiveTabs) {
                    return@TabsFeature false
                }
                state.isNormalTabInactive(maxActiveTime)
            },
            {}
        )
    }

    private val touchHelper by lazy {
        TabsTouchHelper(
            observable = concatAdapter.browserAdapter,
            onViewHolderTouched = {
                it is TabViewHolder && swipeToDelete.isSwipeable
            },
            onViewHolderDraw = { context.components.settings.gridTabView.not() }
        )
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        tabsFeature.start()
        inactiveFeature.start()

        touchHelper.attachToRecyclerView(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        tabsFeature.stop()
        inactiveFeature.stop()

        touchHelper.attachToRecyclerView(null)
    }
}
