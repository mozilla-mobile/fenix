/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.fenix

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.AttributeSet
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import mozilla.components.concept.engine.EngineView
import mozilla.components.feature.session.SessionFeature
import mozilla.components.feature.toolbar.ToolbarFeature
import mozilla.fenix.components.FeatureLifecycleObserver
import mozilla.fenix.ext.components

class MainActivity : AppCompatActivity() {
    private lateinit var toolbarFeature: ToolbarFeature
    private lateinit var sessionFeature: SessionFeature

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toolbarFeature = ToolbarFeature(
            toolbar,
            components.sessionProvider.sessionManager,
            components.sessionUseCases.loadUrl)

        sessionFeature = SessionFeature(
            components.sessionProvider,
            components.sessionUseCases,
            components.engine,
            engineView)

        lifecycle.addObserver(FeatureLifecycleObserver(sessionFeature, toolbarFeature))
    }

    override fun onBackPressed() {
        if (toolbarFeature.handleBackPressed())
            return

        if (sessionFeature.handleBackPressed())
            return

        super.onBackPressed()
    }

    override fun onCreateView(parent: View?, name: String?, context: Context?, attrs: AttributeSet?): View? {
        if (name == EngineView::class.java.name) {
            return components.engine.createView(context!!, attrs).asView()
        }

        return super.onCreateView(parent, name, context, attrs)
    }
}
