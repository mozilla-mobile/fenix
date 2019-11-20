package org.mozilla.fenix.components.push

import mozilla.components.feature.push.AutoPushFeature
import mozilla.components.feature.push.AutoPushSubscription

interface Dispatchee {
    /*
    When the push dispatcher gets a message with a Service type, this method is invoked.
     */
    fun onServiceTypeEvent(message: String, dispatcher: AutoPushFeature)
    /*
    When the push dispatcher gets a subscription available notification with a Service type,
    this method is invoked.
     */
    fun onServiceTypeSubscriptionAvailable(subscription: AutoPushSubscription, dispatcher: AutoPushFeature)
}
