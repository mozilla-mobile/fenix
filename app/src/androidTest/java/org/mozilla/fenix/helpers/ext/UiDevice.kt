package org.mozilla.fenix.helpers.ext

import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.SearchCondition
import androidx.test.uiautomator.UiDevice
import org.junit.Assert
import org.mozilla.fenix.helpers.TestAssetHelper

/**
 * Blocks the test for [waitTime] miliseconds before continuing.
 *
 * Will cause the test to fail is the condition is not met before the timeout.
 */
fun UiDevice.waitNotNull(
    searchCondition: SearchCondition<*>,
    waitTime: Long = TestAssetHelper.waitingTimeShort
) = Assert.assertNotNull(wait(searchCondition, waitTime))

/**
 * Searches for an object based on the [selector] condition.
 *
 * Will cause the test to fail if the object can be found.
 */
fun UiDevice.assertObjectDoesNotExist(selector: BySelector) =
    Assert.assertFalse(this.hasObject(selector))

fun UiDevice.isObjectNotNull(
    searchCondition: SearchCondition<*>,
    waitTime: Long = TestAssetHelper.waitingTimeShort
): Boolean {
    return (wait(searchCondition, waitTime) != null)
}
