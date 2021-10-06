/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.sessioncontrol.viewholders.pocket

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import mozilla.components.service.pocket.PocketRecommendedStory
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.R
import org.mozilla.fenix.assertSemanticsEquals
import org.mozilla.fenix.compose.urlKey
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.helpers.FenixRobolectricTestRunner
import org.mozilla.fenix.onFirstChildWithTag
import org.mozilla.fenix.theme.FirefoxTheme
import org.robolectric.Robolectric
import org.robolectric.annotation.Config

@RunWith(FenixRobolectricTestRunner::class)
class PocketStoriesComposablesTest {
    private val activity = Robolectric.buildActivity(ComponentActivity::class.java).create().get()
    @get:Rule
    internal val composeTestRule = createAndroidComposeRule(activity::class.java)

    private var story = PocketRecommendedStory("storyTitle", "storyUrl", "storyImageUrl", "storyPublisher", "storyCategory", 3, 41)

    @Test
    fun `WHEN displaying a story THEN show all details in a ListItemTabLarge composable`() {
        addStoryOnScreen(composeTestRule, story)

        composeTestRule.onFirstChildWithTag("ListItemTabLarge").assertExists()
    }

    @Test
    fun `WHEN displaying a story THEN show it's title`() {
        addStoryOnScreen(composeTestRule, story)

        composeTestRule.onNodeWithText(story.title).assertExists()
    }

    @Test
    fun `WHEN displaying a story THEN show an image for it's url`() {
        addStoryOnScreen(composeTestRule, story)

        // Would've been nice to check the URL in the Image composable
        // but the compose finders seem to no go that deep in the hierarchy.
        // ListItemTabLarge should guarantee though that the imageUrl will be used to display an image.
        composeTestRule.onNodeWithTag("ListItemTabLarge")
            .assertSemanticsEquals(story.imageUrl, urlKey)
    }

    @Test
    @Config(qualifiers = "mdpi") // 1x density
    fun `WHEN displaying a story THEN show an image for it's url configured with appropriate dimensions`() {
        val imageUrlWithDimensionsPlaceholder = "https://image{wh}.url"
        val imageUrlWithAppropriateDimensions = "https://image116x84.url"
        story = story.copy(imageUrl = imageUrlWithDimensionsPlaceholder)

        addStoryOnScreen(composeTestRule, story)

        composeTestRule.onNodeWithTag("ListItemTabLarge")
            .assertSemanticsEquals(imageUrlWithAppropriateDimensions, urlKey)
    }

    @Test
    fun `WHEN displaying a story THEN show it's publisher`() {
        addStoryOnScreen(composeTestRule, story)

        composeTestRule.onNodeWithText(story.publisher).assertExists()
    }

    @Test
    fun `WHEN displaying a story THEN show it's time to read`() {
        addStoryOnScreen(composeTestRule, story)

        composeTestRule.onNodeWithText("${story.timeToRead} min").assertExists()
    }

    @Test
    fun `GIVEN a story with 9 as time to read WHEN displaying THEN show the 0 value for time to read`() {
        // Just because Pocket sets -1 when the value can't be calculated 0 could be valid. Though unlikely.
        story = story.copy(timeToRead = 0)

        addStoryOnScreen(composeTestRule, story)

        composeTestRule.onNodeWithText("${story.timeToRead} min").assertExists()
    }

    @Test
    fun `GIVEN a story with valid publisher and time to read WHEN displaying THEN show an interdot`() {
        addStoryOnScreen(composeTestRule, story)

        composeTestRule.onNodeWithText(" · ").assertExists()
    }

    @Test
    fun `GIVEN a story with invalid publisher but valid time to read WHEN displaying THEN show an just the time to read`() {
        addStoryOnScreen(composeTestRule, story.copy(publisher = ""))

        composeTestRule.onNodeWithText(" · ").assertDoesNotExist()
        composeTestRule.onNodeWithText("${story.timeToRead} min").assertExists()
    }

    @Test
    fun `GIVEN a story with valid publisher but invalid time to read WHEN displaying THEN show an just the publisher`() {
        story = story.copy(timeToRead = -1)

        addStoryOnScreen(composeTestRule, story)

        composeTestRule.onNodeWithText(" · ").assertDoesNotExist()
        composeTestRule.onNodeWithText("${story.timeToRead} min").assertDoesNotExist()
        composeTestRule.onNodeWithText(story.publisher).assertExists()
    }

    @Test
    fun `GIVEN a story with invalid publisher and invalid time to read WHEN displaying THEN don't show even the interdot`() {
        story = story.copy(publisher = "", timeToRead = -1)

        addStoryOnScreen(composeTestRule, story)

        composeTestRule.onNodeWithText(" · ").assertDoesNotExist()
        composeTestRule.onNodeWithText("${story.timeToRead} min").assertDoesNotExist()
    }

    @Test
    fun `WHEN a story is clicked THEN inform through callback`() {
        var storyWasClicked = true

        addStoryOnScreen(composeTestRule, story) {
            assertSame(story, it)
            storyWasClicked = true
        }
        composeTestRule.onRoot().performClick()

        assertTrue(storyWasClicked)
    }

    @Test
    fun `WHEN displaying categories THEN show them in a StaggeredHorizontalGrid`() {
        val category1 = PocketRecommendedStoriesCategory("category1")
        val category2 = PocketRecommendedStoriesCategory("category2")

        composeTestRule.setContent {
            PocketStoriesCategories(listOf(category1, category2), emptyList(), { })
        }

        composeTestRule.onFirstChildWithTag("StaggeredHorizontalGrid").assertExists()
    }

    @Test
    fun `WHEN displaying categories THEN all their names are displayed`() {
        val category1 = PocketRecommendedStoriesCategory("category1")
        val category2 = PocketRecommendedStoriesCategory("category2")

        composeTestRule.setContent {
            PocketStoriesCategories(listOf(category1, category2), emptyList(), { })
        }

        val gridLayout = composeTestRule.onFirstChildWithTag("StaggeredHorizontalGrid")
        gridLayout.onChildren().assertCountEquals(2)
        gridLayout.onChildAt(0).onChildAt(0)
            .assertTextEquals(category1.name.capitalize(Locale.current))
        gridLayout.onChildAt(1).onChildAt(0)
            .assertTextEquals(category2.name.capitalize(Locale.current))
    }

    @Test
    @Config(qualifiers = "mdpi") // 1x density
    fun `WHEN displaying categories THEN show them with 16dp horizontal spacing`() {
        val category1 = PocketRecommendedStoriesCategory("category1")
        val category2 = PocketRecommendedStoriesCategory("category2")

        composeTestRule.setContent {
            PocketStoriesCategories(listOf(category1, category2), emptyList(), { })
        }

        val gridSemantics =
            composeTestRule.onFirstChildWithTag("StaggeredHorizontalGrid").fetchSemanticsNode()
        val firstItemRight = gridSemantics.children[0].layoutInfo.width
        val secondItemLeft = gridSemantics.children[1].positionInWindow.x.toInt()
        assertEquals(firstItemRight + 16, secondItemLeft)
    }

    @Test
    fun `GIVEN a list of displayed categories WHEN one is clicked THEN inform through callback`() {
        var wasCategoryClicked = false
        val category1 = PocketRecommendedStoriesCategory("category1")
        val category2 = PocketRecommendedStoriesCategory("category2")
        composeTestRule.setContent {
            PocketStoriesCategories(
                listOf(category1, category2), emptyList(),
                {
                    assertSame(category2, it)
                    wasCategoryClicked = true
                }
            )
        }

        composeTestRule.onNodeWithText(category2.name.capitalize(Locale.current)).performClick()

        assertTrue(wasCategoryClicked)
    }

    @Test
    fun `WHEN showing the Pocket header THEN it has a default text`() {
        val resources = testContext.resources

        composeTestRule.setContent {
            PoweredByPocketHeader({ })
        }

        val header = composeTestRule.onFirstChildWithTag("PoweredByPocketHeader")
        header.onChildren().assertCountEquals(2)
        header.onChildAt(0).assertTextEquals(
            resources.getString(R.string.pocket_stories_feature_title)
        )
        header.onChildAt(1).assertTextEquals(
            resources.getString(
                R.string.pocket_stories_feature_caption,
                resources.getString(R.string.pocket_stories_feature_learn_more)
            )
        )
    }
}

private fun addStoryOnScreen(
    composeRule: ComposeContentTestRule,
    story: PocketRecommendedStory,
    onClick: (PocketRecommendedStory) -> Unit = { }
) {
    mockkStatic("org.mozilla.fenix.ext.ContextKt") {
        // PocketStory uses an Image that uses a Client that will try to fetch from story's imageUrl. Avoid that.
        every { any<Context>().components.core.client } returns mockk(relaxed = true)

        composeRule.setContent {
            FirefoxTheme {
                PocketStory(story, onClick)
            }
        }
    }
}
