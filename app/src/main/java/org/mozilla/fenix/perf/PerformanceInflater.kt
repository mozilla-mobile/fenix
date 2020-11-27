/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.perf

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.mozilla.fenix.ext.getAndIncrementNoOverflow
import java.util.concurrent.atomic.AtomicInteger


/**
 * Counts the number of inflations fenix does. This class behaves only as an inflation counter since
 * it takes the `inflater` that is given by the base system. This is done in order not to change
 * the behavior of the app since all we want to do is count the inflations done.
 *
 */
open class PerformanceInflater(
    val inflater: LayoutInflater,
    context: Context
) : LayoutInflater(
    inflater,
    context
){

    private val sClassPrefixList = arrayOf(
        "android.widget.",
        "android.webkit.",
        "android.app."
    )

    override fun cloneInContext(newContext: Context?): LayoutInflater {
        return PerformanceInflater(inflater, newContext!!)
    }

    override fun inflate(resource: Int, root: ViewGroup?, attachToRoot: Boolean): View {
        InflationCounter.inflationCount.getAndIncrementNoOverflow()
        return super.inflate(resource, root, attachToRoot)
    }

    /**
     * This code was taken from the PhoneLayoutInflater.java (Similarly, AsyncLayoutInflater
     * implements it the exact same way too). Looking at the `super.OnCreateView(name, attrs)`,
     * it hardcodes the prefix as "android.view." this means that a xml element such as
     * ImageButton will crash the app using android.view.ImageButton.
     */
    @Throws(ClassNotFoundException::class)
    override fun onCreateView(name: String?, attrs: AttributeSet?): View? {
        for (prefix in sClassPrefixList) {
            try {
                val view = createView(name, prefix, attrs)
                if (view != null) {
                    return view
                }
            } catch (e: ClassNotFoundException) {

            }
        }
        return super.onCreateView(name, attrs)
    }


}

object InflationCounter {
    val inflationCount = AtomicInteger(0)
}
