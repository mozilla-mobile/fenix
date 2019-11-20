package org.mozilla.fenix.components

import mozilla.components.concept.sync.DevicePushSubscription
import mozilla.components.feature.push.AutoPushFeature
import mozilla.components.feature.push.AutoPushSubscription
import mozilla.components.service.fxa.manager.FxaAccountManager
import org.mozilla.fenix.components.push.Dispatchee

class AccountManagerDispatchee(private val accountManager: Lazy<FxaAccountManager>) :
    Dispatchee {
    var registered = false
    override fun onServiceTypeEvent(message: String, dispatcher: AutoPushFeature) {
        if (!registered) {
            accountManager.value.register(PushAccountObserver(dispatcher))
            registered = true
        }
        accountManager.value.authenticatedAccount()?.deviceConstellation()
            ?.processRawEventAsync(message)
    }

    override fun onServiceTypeSubscriptionAvailable(subscription: AutoPushSubscription, dispatcher: AutoPushFeature) {
        if (!registered) {
            accountManager.value.register(PushAccountObserver(dispatcher))
            registered = true
        }
        accountManager.value.authenticatedAccount()
            ?.deviceConstellation()
            ?.setDevicePushSubscriptionAsync(
                DevicePushSubscription(
                    endpoint = subscription.endpoint,
                    publicKey = subscription.publicKey,
                    authKey = subscription.authKey
                )
            )
    }
}
