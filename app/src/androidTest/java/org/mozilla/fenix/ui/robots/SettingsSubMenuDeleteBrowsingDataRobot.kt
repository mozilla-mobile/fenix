package org.mozilla.fenix.ui.robots

import android.view.View
import android.widget.Checkable
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.hamcrest.BaseMatcher
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.isA
import org.hamcrest.Description
import org.mozilla.fenix.helpers.TestHelper
import org.mozilla.fenix.helpers.click
import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.TestAssetHelper
import org.mozilla.fenix.helpers.ext.waitNotNull

class SettingsSubMenuDeleteBrowsingDataRobot {
    fun verifyTabsCount(tabsCount: String) = assertOpenTabsCount(tabsCount)

    fun verifyAddressesCount(addressCount: String) = assertAddressesCount(addressCount)

    fun clickDeleteBrowsingData() {
        //check tabs and addresses
        openTabsCheckBox().perform(checkBox(true))
        addressesCheckBox().perform(checkBox(true))

        deleteBrowsingDataButton().click()

        TestHelper.waitUntilObjectIsFound("android:id/button1")

        onView(withText("Delete")).perform(click())
    }

    class Transition {
        val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        fun openHomeScreen(interact: HomeScreenRobot.() -> Unit): HomeScreenRobot.Transition {
            goBackButton().click()

            mDevice.waitNotNull(
                Until.findObject(By.res("org.mozilla.fenix.debug:id/header_text")),
                TestAssetHelper.waitingTime
            )

            HomeScreenRobot().interact()
            return HomeScreenRobot.Transition()
        }

        fun goBack(interact: SettingsRobot.() -> Unit): SettingsRobot.Transition {
            goBackButton().perform(click())

            SettingsRobot().interact()
            return SettingsRobot.Transition()
        }
    }
}

private fun goBackButton() = onView(CoreMatchers.allOf(withContentDescription("Navigate up")))

private fun deleteBrowsingDataButton() = onView(withId(R.id.delete_data))

private fun openTabsCheckBox() =
    onView(allOf(withId(R.id.checkbox), hasSibling(withText("Open Tabs"))))

private fun addressesCheckBox() =
    onView(allOf(withId(R.id.checkbox), hasSibling(withText("Browsing history and site data"))))

private fun assertOpenTabs() = onView(withText("Open Tabs")).check(
    matches(
        withEffectiveVisibility(
            ViewMatchers.Visibility.VISIBLE
        )
    )
)

private fun assertOpenTabsCount(tabsCount: String) =
    onView(allOf(withId(R.id.subtitle), hasSibling(withText("Open Tabs")))).check(
        matches(
            withText(tabsCount + " tabs")
        )
    )

private fun assertAddresses() = onView(withText("Browsing history and site data")).check(
    matches(
        withEffectiveVisibility(
            ViewMatchers.Visibility.VISIBLE
        )
    )
)

private fun assertAddressesCount(addressCount: String) = onView(
    allOf(
        withId(R.id.subtitle),
        hasSibling(withText("Browsing history and site data"))
    )
).check(
    matches(
        withText(addressCount + " addresses")
    )
)

private fun checkBox(checked: Boolean): ViewAction {
    return object : ViewAction {
        override fun getConstraints(): BaseMatcher<View?> {
            return object : BaseMatcher<View?>() {
                override fun matches(item: Any): Boolean {
                    return isA(Checkable::class.java).matches(item)
                }

                override fun describeMismatch(
                    item: Any?,
                    mismatchDescription: Description?
                ) {
                }

                override fun describeTo(description: Description?) {}
            }
        }

        override fun getDescription(): String? {
            return null
        }

        override fun perform(uiController: UiController?, view: View) {
            val checkableView = view as Checkable
            checkableView.isChecked = checked
        }
    }
}