/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.metrics

import android.content.Context
import android.content.pm.PackageManager
import mozilla.components.support.utils.Browsers

object MozillaProductDetector {
    enum class MozillaProducts(val productName: String) {
        // Browsers
        FIREFOX("org.mozilla.firefox"),
        FIREFOX_BETA("org.mozilla.firefox_beta"),
        FIREFOX_AURORA("org.mozilla.fennec_aurora"),
        FIREFOX_NIGHTLY("org.mozilla.fennec"),
        FIREFOX_FDROID("org.mozilla.fennec_fdroid"),
        FIREFOX_LITE("org.mozilla.rocket"),
        REFERENCE_BROWSER("org.mozilla.reference.browser"),
        REFERENCE_BROWSER_DEBUG("org.mozilla.reference.browser.debug"),
        FENIX("org.mozilla.fenix"),
        FOCUS("org.mozilla.focus"),
        KLAR("org.mozilla.klar"),

        // Other products
        LOCKWISE("mozilla.lockbox")
    }

    // The results of getInstalledMozillaProducts or getMozillaBrowserDefault can be cached. When
    // either of those functions is called with the same context as the previous invocation, return
    // the cached results from cachedMozillaProducts or cachedDefaultBrowserPackageName, respectively.
    private var cachedContext: Context? = null
    private var cachedMozillaProducts: MutableList<String>? = null
    private var cachedDefaultBrowserPackageName: String? = null
    private var cachedDefaultBrowserPackageNameSet: Boolean = false

    /**
     * Returns a list of the Mozilla products installed on the user's device.
     */
    @Synchronized
    fun getInstalledMozillaProducts(context: Context): List<String> {
        // If there is a cached context and there is a cached list of products, return the cached
        // list of products if _context_ and _cachedContext_ are equal.
        if (cachedContext != null && cachedMozillaProducts != null && context == cachedContext) {
            return cachedMozillaProducts!!
        }

        // Otherwise, set the cached context and build the list of installed products.
        cachedContext = context

        var mozillaProducts = mutableListOf<String>()

        for (product in MozillaProducts.values()) {
            if (packageIsInstalled(context, product.productName)) { mozillaProducts.add(product.productName) }
        }

        getMozillaBrowserDefault(context)?.let {
            if (!mozillaProducts.contains(it)) {
                mozillaProducts.add(it)
            }
        }

        cachedMozillaProducts = mozillaProducts
        return cachedMozillaProducts!!
    }

    private fun packageIsInstalled(context: Context, packageName: String): Boolean {
        try {
            context.packageManager.getPackageInfo(packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            return false
        }

        return true
    }

    /**
     * Returns the default browser if and only if it is a Mozilla product.
     */
    @Synchronized
    fun getMozillaBrowserDefault(context: Context): String? {
        var browserPackageName: String?
        // If there is a cached context and there is a cached default browser package name,
        // use that to determine whether the default browser is a Mozilla product.
        if (cachedContext != null && cachedDefaultBrowserPackageNameSet && cachedContext == context) {
            browserPackageName = cachedDefaultBrowserPackageName
        } else {
            // Otherwise, store the context and the package name of the system's default browser
            // in the cache. Also, set a flag that says this fetch has been done once. The flag
            // is necessary because the user may not have a default browser set so the nullable
            // cachedDefaultBrowserPackageName will continue to be null, rendering it useless to use
            // as a condition here.
            cachedContext = context
            cachedDefaultBrowserPackageName = Browsers.all(context).defaultBrowser?.packageName
            cachedDefaultBrowserPackageNameSet = true
            browserPackageName = cachedDefaultBrowserPackageName
        }
        return if (isMozillaProduct(browserPackageName)) { browserPackageName } else { null }
    }

    // Note: we intentionally do not use a-c `firefoxBrandedBrowser` as this only gives us the first from that list
    private fun isMozillaProduct(packageName: String?): Boolean {
        packageName ?: return false
        return MozillaProducts.values().any { product -> product.productName == packageName }
    }
}
