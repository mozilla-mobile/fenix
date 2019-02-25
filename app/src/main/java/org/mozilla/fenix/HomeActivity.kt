/* This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import mozilla.components.concept.engine.EngineView
import mozilla.components.feature.intent.IntentProcessor
import mozilla.components.support.base.feature.BackHandler
import mozilla.components.support.utils.SafeIntent
import org.mozilla.fenix.ext.components

open class HomeActivity : AppCompatActivity() {
    val themeManager = DefaultThemeManager().also {
        it.onThemeChange = { theme ->
            setTheme(theme)
            recreate()
        }
    }

    lateinit var browsingModeManager: DefaultBrowsingModeManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTheme(themeManager.currentTheme)
        DefaultThemeManager.applyStatusBarTheme(window, themeManager, this)

        browsingModeManager = DefaultBrowsingModeManager(this)

        setContentView(R.layout.activity_home)

        if (intent?.extras?.getBoolean(OPEN_TO_BROWSER) == true) {
            openToBrowser()
        }

        val host = supportFragmentManager.findFragmentById(R.id.container) as NavHostFragment
        val hostNavController = host.navController
        val appBarConfiguration = AppBarConfiguration.Builder(setOf(R.id.libraryFragment)).build()
        val navigationToolbar = findViewById<Toolbar>(R.id.navigationToolbar)
        setSupportActionBar(navigationToolbar)
        NavigationUI.setupWithNavController(navigationToolbar, hostNavController, appBarConfiguration)
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

    override fun onPause() {
        super.onPause()
        hideSoftwareKeyboard()
    }

    private fun hideSoftwareKeyboard() {
        (getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager).apply {
            currentFocus?.also {
                this.hideSoftInputFromWindow(it.windowToken, 0)
            }
        }
    }

    private fun openToBrowser() {
        val sessionId = SafeIntent(intent).getStringExtra(IntentProcessor.ACTIVE_SESSION_ID)
        val host = supportFragmentManager.findFragmentById(R.id.container) as NavHostFragment
        val directions = NavGraphDirections.actionGlobalBrowser(sessionId)
        host.navController.navigate(directions)
    }

    companion object {
        const val OPEN_TO_BROWSER = "open_to_browser"
    }
}
