/* This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import mozilla.components.concept.engine.EngineView
import mozilla.components.feature.intent.IntentProcessor
import mozilla.components.support.utils.SafeIntent
import org.mozilla.fenix.browser.BrowserFragment
import org.mozilla.fenix.ext.components

open class HomeActivity : AppCompatActivity() {
    val themeManager = DefaultThemeManager().also {
        it.onThemeChange = { theme ->
            setTheme(theme)
            recreate()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        setTheme(themeManager.currentTheme)
        applyStatusBarTheme()

        if (intent?.extras?.getBoolean(OPEN_TO_BROWSER) == true) {
            openToBrowser()
        }
    }

    override fun onCreateView(
        parent: View?,
        name: String,
        context: Context,
        attrs: AttributeSet
    ): View? =
        when (name) {
            EngineView::class.java.name -> components.core.engine.createView(context, attrs).asView()
            else -> super.onCreateView(parent, name, context, attrs)
        }

    override fun onBackPressed() {
        supportFragmentManager.primaryNavigationFragment?.childFragmentManager?.fragments?.forEach {
            if (it is BackHandler && it.onBackPressed()) {
                return
            }
        }

        super.onBackPressed()
    }

    private fun openToBrowser() {
        val sessionId = SafeIntent(intent).getStringExtra(IntentProcessor.ACTIVE_SESSION_ID)
        val host = supportFragmentManager.findFragmentById(R.id.container) as NavHostFragment

        host.navController.navigate(R.id.action_global_browser, Bundle().apply {
            putString(BrowserFragment.SESSION_ID, sessionId)
        })
    }

    // Handles status bar theme change since the window does not dynamically recreate
    private fun applyStatusBarTheme() {
        window.statusBarColor = ContextCompat
            .getColor(this, DefaultThemeManager
                .resolveAttribute(android.R.attr.statusBarColor, this))

        window.navigationBarColor = ContextCompat
            .getColor(this, DefaultThemeManager
                .resolveAttribute(android.R.attr.navigationBarColor, this))

        when (themeManager.currentTheme) {
            ThemeManager.Theme.Light -> {
                window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or
                        View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }
            ThemeManager.Theme.Private -> {
                window.decorView.systemUiVisibility = window.decorView.systemUiVisibility and
                        View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv() and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
            }
        }
    }

    companion object {
        const val OPEN_TO_BROWSER = "open_to_browser"
    }
}
