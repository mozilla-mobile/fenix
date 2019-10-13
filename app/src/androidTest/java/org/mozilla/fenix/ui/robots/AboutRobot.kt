package org.mozilla.fenix.ui.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.startsWith
import org.mozilla.fenix.BuildConfig
import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.click


/**
 * Implementation of Robot Pattern for the about app fragment.
 */
class AboutRobot {
    fun verifyAboutView() = assertAboutView()
    fun verifyAboutFragmentLinks() = assertLinks()
    class Transition{
        val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        fun goBack(interact: SettingsRobot.() -> Unit): SettingsRobot.Transition {

            mDevice.waitForIdle()
            goBackButton().perform(ViewActions.click())

            SettingsRobot().interact()
            return SettingsRobot.Transition()
        }
    }
}


private val context = InstrumentationRegistry.getInstrumentation().targetContext
private val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
private const val buildDate = BuildConfig.BUILD_DATE
private const val componentsVersion = mozilla.components.Build.version + ", " + mozilla.components.Build.gitHash
private const val geckoVersion = org.mozilla.geckoview.BuildConfig.MOZ_APP_VERSION + "-" + org.mozilla.geckoview.BuildConfig.MOZ_APP_BUILDID

private val appName = context.resources.getString(R.string.app_name)
private val aboutAppHeading = "About $appName"

private fun goBackButton() =
    onView(CoreMatchers.allOf(ViewMatchers.withContentDescription("Navigate up")))

private fun assertAboutView(){

    onView(CoreMatchers.allOf(ViewMatchers.withText(aboutAppHeading))).check(
        ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE))
    )
    onView(CoreMatchers.allOf(ViewMatchers.withText(startsWith(packageInfo.versionName)))).check(
        ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))

    onView(CoreMatchers.allOf(ViewMatchers.withText(containsString(componentsVersion)))).check(
        ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
    onView(CoreMatchers.allOf(ViewMatchers.withText(containsString(geckoVersion)))).check(
        ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
    onView(CoreMatchers.allOf(ViewMatchers.withText(buildDate))).check(
        ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
}

private fun assertLinks(){
    onView(CoreMatchers.allOf(ViewMatchers.withText("Open source libraries we use"))).check(
        ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
    onView(CoreMatchers.allOf(ViewMatchers.withText("Open source libraries we use"))).click()
    goBackButton().click()

    onView(CoreMatchers.allOf(ViewMatchers.withText("What's new in $appName"))).check(
        ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
    onView(CoreMatchers.allOf(ViewMatchers.withText("What's new in $appName"))).click()
    mDevice.pressBack()
}