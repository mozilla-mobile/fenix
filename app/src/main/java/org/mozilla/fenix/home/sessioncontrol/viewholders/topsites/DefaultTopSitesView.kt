/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.sessioncontrol.viewholders.topsites

import mozilla.components.feature.top.sites.TopSite
import mozilla.components.feature.top.sites.view.TopSitesView
import org.mozilla.fenix.home.HomeFragmentAction
import org.mozilla.fenix.home.HomeFragmentStore

class DefaultTopSitesView(
    val store: HomeFragmentStore
) : TopSitesView {

    override fun displayTopSites(topSites: List<TopSite>) {
        store.dispatch(HomeFragmentAction.TopSitesChange(topSites))
    }
}
