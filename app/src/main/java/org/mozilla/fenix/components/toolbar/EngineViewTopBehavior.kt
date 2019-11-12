package org.mozilla.fenix.components.toolbar

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import mozilla.components.concept.engine.EngineView
import mozilla.components.support.ktx.android.view.findViewInHierarchy

/**
 * A [CoordinatorLayout.Behavior] implementation to be used with [EngineView] when placing a toolbar at the
 * bottom of the screen.
 *
 * Using this behavior requires the toolbar to use the BrowserToolbarTopBehavior.
 *
 * This implementation will update the vertical clipping of the [EngineView] so that top-aligned web content will
 * be drawn above the native toolbar.
 */
class EngineViewTopBehavior(
    context: Context?,
    attrs: AttributeSet?
) : CoordinatorLayout.Behavior<View>(context, attrs) {
    @SuppressLint("LogUsage")
    override fun layoutDependsOn(parent: CoordinatorLayout, child: View, dependency: View): Boolean {
        // This package does not have access to "BrowserToolbar" ... so we are just checking the class name here since
        // we actually do not need anything from that class - we only need to identify the instance.
        // Right now we do not have a component that has access to (concept/browser-toolbar and concept-engine).
        // Creating one just for this behavior is too excessive.
        if (dependency::class.java.simpleName == "BrowserToolbar") {
            return true
        }

        return super.layoutDependsOn(parent, child, dependency)
    }

    /**
     * Apply vertical clipping to [EngineView]. This requires [EngineViewTopehavior] to be set
     * in/on the [EngineView] or its parent. Must be a direct descending child of [CoordinatorLayout].
     */
    override fun onDependentViewChanged(parent: CoordinatorLayout, child: View, dependency: View): Boolean {
        val engineView = child.findViewInHierarchy { it is EngineView } as EngineView?
        engineView?.setVerticalClipping(dependency.height - dependency.translationY.toInt())
        return true
    }
}
