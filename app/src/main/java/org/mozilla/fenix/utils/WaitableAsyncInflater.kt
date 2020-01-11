package org.mozilla.fenix.utils

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Wrapper around an asynchronous layout inflater that gives callers the ability to
 * synchronously access the inflated view, whether the asynchronous inflation is
 * complete or in progress.
 *
 * A WaitiableAsyncInflater is a wrapper around an asynchronous layout inflater upon which
 * a caller can invoke get() to obtain the inflated view. If the inflation already happened
 * and the view is created by the time the call to get() happens, the view is returned
 * immediately. If the inflation is in progress asynchronously at the time of the get()
 * call, the function waits for inflation to complete and then returns the view. If the
 * inflation has not yet started by the time of the get() call, the inflation happens
 * synchronously and the view is returned.
 *
 * Users invoke inflate() when they simply want to start the inflation asynchronously and
 * are not interested in the view itself.
 *
 * This class is thread safe.
 */
class WaitableAsyncInflater(val resId: Int) {

    var asyncInflationStarted = false
    var condition = Object()
    var view: View? = null
    val scope = CoroutineScope(Dispatchers.IO)

    private fun startAsynchronousInflation(context: Context, group: ViewGroup, attachToRoot: Boolean = false) {
        scope.launch {
            view = LayoutInflater.from(context).inflate(resId, group, attachToRoot)
            synchronized(condition) {
                condition.notifyAll()
            }
        }
        asyncInflationStarted = true
    }

    private fun doSynchronousInflation(context: Context, group: ViewGroup, attachToRoot: Boolean = false) {
        val inflater = LayoutInflater.from(context)
        view = inflater.inflate(resId, group, attachToRoot)
    }

    /**
     * TODO
     *
     * In this documentation, be sure to mention that *it is assumed* that the view can be inflated
     * off the MT.
     */
    @Synchronized
    fun inflate(context: Context, group: ViewGroup, attachToRoot: Boolean = false) {
        if (view == null && !asyncInflationStarted) {
            startAsynchronousInflation(context, group, attachToRoot)
        }
    }

    /**
     * TODO
     */
    @Synchronized
    fun get(context: Context, group: ViewGroup, attachToRoot: Boolean = false): View {
        if (view == null && !asyncInflationStarted) {
            // When there is neither a view already created nor an asynchronous inflation in progress
            // do the inflation on demand.
            doSynchronousInflation(context, group, attachToRoot)
        } else {
            // Otherwise, potentially wait for the view to be created.
            while (view == null) {
                synchronized(condition) {
                    condition.wait()
                }
            }
        }
        return view!!
    }
}
