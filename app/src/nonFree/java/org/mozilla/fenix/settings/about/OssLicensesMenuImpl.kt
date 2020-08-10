/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.about

import android.content.Context
import android.content.Intent
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity

class OssLicensesMenuImpl : OssLicensesMenu {
    override fun getIntent(context: Context?, title: String): Intent {
        OssLicensesMenuActivity.setActivityTitle(title)
        return Intent(context, OssLicensesMenuActivity::class.java)
    }
}
