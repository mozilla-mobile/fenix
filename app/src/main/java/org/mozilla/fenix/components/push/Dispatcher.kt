package org.mozilla.fenix.components.push

import android.content.Context
import androidx.annotation.VisibleForTesting
import mozilla.components.concept.push.Bus
import mozilla.components.feature.push.AutoPushFeature
import mozilla.components.feature.push.AutoPushSubscription
import mozilla.components.feature.push.PushConfig
import mozilla.components.feature.push.PushSubscriptionObserver
import mozilla.components.feature.push.PushType
import mozilla.components.support.base.log.logger.Logger
import org.mozilla.fenix.R
import org.mozilla.fenix.components.FirebasePush
import org.mozilla.fenix.test.Mockable

@Mockable
class Dispatcher(private val context: Context, private val dispatchees: List<Dispatchee>) {
    val pushDispatcher by lazy { makePushConfig()?.let { makePush(it) } }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun makePush(pushConfig: PushConfig): AutoPushFeature {
        var apf = AutoPushFeature(
            context = context,
            service = FirebasePush(),
            config = pushConfig
        )
        /*
        Tell the dispatchee about an event of Service type.
         */
        apf.registerForPushMessages(
            PushType.Services,
            object : Bus.Observer<PushType, String> {
                override fun onEvent(type: PushType, message: String) {
                    for (dispatchee in dispatchees) {
                        dispatchee.onServiceTypeEvent(message, apf)
                    }
                }
            }
        )
        /*
        Tell the dispatchee about a subscription available of Service type.
         */
        apf.registerForSubscriptions(object : PushSubscriptionObserver {
            override fun onSubscriptionAvailable(subscription: AutoPushSubscription) {
                if (subscription.type == PushType.Services) {
                    for (dispatchee in dispatchees) {
                        dispatchee.onServiceTypeSubscriptionAvailable(subscription, apf)
                    }
                }
            }
        })
        return apf
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun makePushConfig(): PushConfig? {
        val logger = Logger("PushConfig")
        val projectIdKey = context.getString(R.string.pref_key_push_project_id)
        val resId = context.resources.getIdentifier(projectIdKey, "string", context.packageName)
        if (resId == 0) {
            logger.warn("No firebase configuration found; cannot support push service.")
            return null
        }

        logger.debug("Creating push configuration for autopush.")
        val projectId = context.resources.getString(resId)
        return PushConfig(projectId)
    }
}
