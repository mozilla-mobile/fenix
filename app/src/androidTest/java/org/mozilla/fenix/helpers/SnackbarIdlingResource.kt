package org.mozilla.fenix.helpers

import androidx.test.espresso.IdlingResource

class SnackbarIdlingResource : IdlingResource {
    lateinit var resourceCallback: IdlingResource.ResourceCallback

    override fun getName(): String {
        return this.javaClass.name
    }

    override fun isIdleNow(): Boolean {
//        Thread.sleep(5000)
        resourceCallback.onTransitionToIdle()
        return true
    }

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
        when (callback) {
            null -> {
            }
            else -> this.resourceCallback = callback
        }
    }
}
