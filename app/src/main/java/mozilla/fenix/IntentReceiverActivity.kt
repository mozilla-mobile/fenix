package mozilla.fenix

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import mozilla.components.browser.session.tab.CustomTabConfig
import mozilla.components.support.utils.SafeIntent
import mozilla.fenix.ext.components

class IntentReceiverActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        components.sessionIntentProcessor.process(intent)

        val intent = Intent(intent)
        if (CustomTabConfig.isCustomTabIntent(SafeIntent(intent))) {
            intent.setClassName(applicationContext, CustomTabActivity::class.java.name)
        } else {
            intent.setClassName(applicationContext, BrowserActivity::class.java.name)
        }

        startActivity(intent)
        finish()
    }

}