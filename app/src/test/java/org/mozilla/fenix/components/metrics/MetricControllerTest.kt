/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.metrics

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyAll
import io.mockk.impl.annotations.MockK
import mozilla.components.feature.autofill.facts.AutofillFacts
import mozilla.components.feature.awesomebar.facts.AwesomeBarFacts
import mozilla.components.feature.awesomebar.provider.BookmarksStorageSuggestionProvider
import mozilla.components.feature.awesomebar.provider.ClipboardSuggestionProvider
import mozilla.components.feature.awesomebar.provider.HistoryStorageSuggestionProvider
import mozilla.components.feature.awesomebar.provider.SearchSuggestionProvider
import mozilla.components.feature.awesomebar.provider.SessionSuggestionProvider
import mozilla.components.feature.contextmenu.facts.ContextMenuFacts
import mozilla.components.feature.media.facts.MediaFacts
import mozilla.components.feature.prompts.dialog.LoginDialogFacts
import mozilla.components.feature.prompts.facts.CreditCardAutofillDialogFacts
import mozilla.components.feature.pwa.ProgressiveWebAppFacts
import mozilla.components.feature.search.telemetry.ads.AdsTelemetry
import mozilla.components.feature.search.telemetry.incontent.InContentTelemetry
import mozilla.components.feature.syncedtabs.facts.SyncedTabsFacts
import mozilla.components.feature.top.sites.facts.TopSitesFacts
import mozilla.components.support.base.Component
import mozilla.components.support.base.facts.Action
import mozilla.components.support.base.facts.Fact
import mozilla.components.support.base.log.logger.Logger
import mozilla.components.support.test.robolectric.testContext
import mozilla.components.support.webextensions.facts.WebExtensionFacts
import mozilla.telemetry.glean.testing.GleanTestRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.GleanMetrics.AndroidAutofill
import org.mozilla.fenix.GleanMetrics.Awesomebar
import org.mozilla.fenix.GleanMetrics.BrowserSearch
import org.mozilla.fenix.GleanMetrics.ContextualMenu
import org.mozilla.fenix.GleanMetrics.CreditCards
import org.mozilla.fenix.GleanMetrics.LoginDialog
import org.mozilla.fenix.GleanMetrics.MediaNotification
import org.mozilla.fenix.GleanMetrics.PerfAwesomebar
import org.mozilla.fenix.GleanMetrics.ProgressiveWebApp
import org.mozilla.fenix.GleanMetrics.SyncedTabs
import org.mozilla.fenix.components.metrics.ReleaseMetricController.Companion
import org.mozilla.fenix.helpers.FenixRobolectricTestRunner
import org.mozilla.fenix.search.awesomebar.ShortcutsSuggestionProvider
import org.mozilla.fenix.utils.Settings
import mozilla.components.compose.browser.awesomebar.AwesomeBarFacts as ComposeAwesomeBarFacts

@RunWith(FenixRobolectricTestRunner::class)
class MetricControllerTest {

    @get:Rule
    val gleanTestRule = GleanTestRule(testContext)

    @MockK(relaxUnitFun = true) private lateinit var dataService1: MetricsService
    @MockK(relaxUnitFun = true) private lateinit var dataService2: MetricsService
    @MockK(relaxUnitFun = true) private lateinit var marketingService1: MetricsService
    @MockK(relaxUnitFun = true) private lateinit var marketingService2: MetricsService

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        every { dataService1.type } returns MetricServiceType.Data
        every { dataService2.type } returns MetricServiceType.Data
        every { marketingService1.type } returns MetricServiceType.Marketing
        every { marketingService2.type } returns MetricServiceType.Marketing
    }

    @Test
    fun `debug metric controller emits logs`() {
        val logger = mockk<Logger>(relaxed = true)
        val controller = DebugMetricController(logger)

        controller.start(MetricServiceType.Data)
        verify { logger.debug("DebugMetricController: start") }

        controller.stop(MetricServiceType.Data)
        verify { logger.debug("DebugMetricController: stop") }
    }

    @Test
    fun `release metric controller starts and stops all data services`() {
        var enabled = true
        val controller = ReleaseMetricController(
            services = listOf(dataService1, marketingService1, dataService2, marketingService2),
            isDataTelemetryEnabled = { enabled },
            isMarketingDataTelemetryEnabled = { enabled },
            mockk()
        )

        controller.start(MetricServiceType.Data)
        verify { dataService1.start() }
        verify { dataService2.start() }

        enabled = false

        controller.stop(MetricServiceType.Data)
        verify { dataService1.stop() }
        verify { dataService2.stop() }

        verifyAll(inverse = true) {
            marketingService1.start()
            marketingService1.stop()
            marketingService2.start()
            marketingService2.stop()
        }
    }

    @Test
    fun `release metric controller starts data service only if enabled`() {
        val controller = ReleaseMetricController(
            services = listOf(dataService1),
            isDataTelemetryEnabled = { false },
            isMarketingDataTelemetryEnabled = { true },
            mockk()
        )

        controller.start(MetricServiceType.Data)
        verify(inverse = true) { dataService1.start() }

        controller.stop(MetricServiceType.Data)
        verify(inverse = true) { dataService1.stop() }
    }

    @Test
    fun `release metric controller starts service only once`() {
        var enabled = true
        val controller = ReleaseMetricController(
            services = listOf(dataService1),
            isDataTelemetryEnabled = { enabled },
            isMarketingDataTelemetryEnabled = { true },
            mockk()
        )

        controller.start(MetricServiceType.Data)
        controller.start(MetricServiceType.Data)
        verify(exactly = 1) { dataService1.start() }

        enabled = false

        controller.stop(MetricServiceType.Data)
        controller.stop(MetricServiceType.Data)
        verify(exactly = 1) { dataService1.stop() }
    }

    @Test
    fun `WHEN AwesomeBar duration fact is processed THEN the correct metric is recorded`() {
        val controller = ReleaseMetricController(emptyList(), { true }, { true }, mockk())
        val action = mockk<Action>()
        val duration = 1000L
        var metadata = mapOf<String, Pair<*, Long>>(
            ComposeAwesomeBarFacts.MetadataKeys.DURATION_PAIR to Pair(
                mockk<HistoryStorageSuggestionProvider>(),
                duration
            )
        )
        var fact = Fact(
            Component.COMPOSE_AWESOMEBAR,
            action,
            ComposeAwesomeBarFacts.Items.PROVIDER_DURATION,
            metadata = metadata
        )
        // Verify history based suggestions
        assertFalse(PerfAwesomebar.historySuggestions.testHasValue())

        with(controller) {
            fact.process()
        }

        assertTrue(PerfAwesomebar.historySuggestions.testHasValue())

        // Verify bookmark based suggestions
        metadata = mapOf(
            ComposeAwesomeBarFacts.MetadataKeys.DURATION_PAIR to Pair(
                mockk<BookmarksStorageSuggestionProvider>(),
                duration
            )
        )
        fact = fact.copy(metadata = metadata)
        assertFalse(PerfAwesomebar.bookmarkSuggestions.testHasValue())

        with(controller) {
            fact.process()
        }

        assertTrue(PerfAwesomebar.bookmarkSuggestions.testHasValue())

        // Verify session based suggestions
        metadata = mapOf(
            ComposeAwesomeBarFacts.MetadataKeys.DURATION_PAIR to Pair(
                mockk<SessionSuggestionProvider>(),
                duration
            )
        )
        fact = fact.copy(metadata = metadata)
        assertFalse(PerfAwesomebar.sessionSuggestions.testHasValue())

        with(controller) {
            fact.process()
        }

        assertTrue(PerfAwesomebar.sessionSuggestions.testHasValue())

        // Verify search engine suggestions
        metadata = mapOf(
            ComposeAwesomeBarFacts.MetadataKeys.DURATION_PAIR to Pair(
                mockk<SearchSuggestionProvider>(),
                duration
            )
        )
        fact = fact.copy(metadata = metadata)
        assertFalse(PerfAwesomebar.searchEngineSuggestions.testHasValue())

        with(controller) {
            fact.process()
        }

        assertTrue(PerfAwesomebar.searchEngineSuggestions.testHasValue())

        // Verify clipboard based suggestions
        metadata = mapOf(
            ComposeAwesomeBarFacts.MetadataKeys.DURATION_PAIR to Pair(
                mockk<ClipboardSuggestionProvider>(),
                duration
            )
        )
        fact = fact.copy(metadata = metadata)
        assertFalse(PerfAwesomebar.clipboardSuggestions.testHasValue())

        with(controller) {
            fact.process()
        }

        assertTrue(PerfAwesomebar.clipboardSuggestions.testHasValue())

        // Verify shortcut based suggestions
        metadata = mapOf(
            ComposeAwesomeBarFacts.MetadataKeys.DURATION_PAIR to Pair(
                mockk<ShortcutsSuggestionProvider>(),
                duration
            )
        )
        fact = fact.copy(metadata = metadata)
        assertFalse(PerfAwesomebar.shortcutsSuggestions.testHasValue())

        with(controller) {
            fact.process()
        }

        assertTrue(PerfAwesomebar.shortcutsSuggestions.testHasValue())
    }

    @Test
    fun `release metric controller starts and stops all marketing services`() {
        var enabled = true
        val controller = ReleaseMetricController(
            services = listOf(dataService1, marketingService1, dataService2, marketingService2),
            isDataTelemetryEnabled = { enabled },
            isMarketingDataTelemetryEnabled = { enabled },
            mockk()
        )

        controller.start(MetricServiceType.Marketing)
        verify { marketingService1.start() }
        verify { marketingService2.start() }

        enabled = false

        controller.stop(MetricServiceType.Marketing)
        verify { marketingService1.stop() }
        verify { marketingService2.stop() }

        verifyAll(inverse = true) {
            dataService1.start()
            dataService1.stop()
            dataService2.start()
            dataService2.stop()
        }
    }

    @Test
    fun `topsites fact should set value in SharedPreference`() {
        val enabled = true
        val settings: Settings = mockk(relaxed = true)
        val controller = ReleaseMetricController(
            services = listOf(dataService1),
            isDataTelemetryEnabled = { enabled },
            isMarketingDataTelemetryEnabled = { enabled },
            settings
        )

        val fact = Fact(
            Component.FEATURE_TOP_SITES,
            Action.INTERACTION,
            TopSitesFacts.Items.COUNT,
            "1"
        )

        verify(exactly = 0) { settings.topSitesSize = any() }
        with(controller) {
            fact.process()
        }
        verify(exactly = 1) { settings.topSitesSize = any() }
    }

    @Test
    fun `web extension fact should set value in SharedPreference`() {
        val enabled = true
        val settings = Settings(testContext)
        val controller = ReleaseMetricController(
            services = listOf(dataService1),
            isDataTelemetryEnabled = { enabled },
            isMarketingDataTelemetryEnabled = { enabled },
            settings
        )
        val fact = Fact(
            Component.SUPPORT_WEBEXTENSIONS,
            Action.INTERACTION,
            WebExtensionFacts.Items.WEB_EXTENSIONS_INITIALIZED,
            metadata = mapOf(
                "installed" to listOf("test1", "test2", "test3", "test4"),
                "enabled" to listOf("test2", "test4")
            )
        )

        assertEquals(settings.installedAddonsCount, 0)
        assertEquals(settings.installedAddonsList, "")
        assertEquals(settings.enabledAddonsCount, 0)
        assertEquals(settings.enabledAddonsList, "")
        with(controller) {
            fact.process()
        }
        assertEquals(settings.installedAddonsCount, 4)
        assertEquals(settings.installedAddonsList, "test1,test2,test3,test4")
        assertEquals(settings.enabledAddonsCount, 2)
        assertEquals(settings.enabledAddonsList, "test2,test4")
    }

    @Test
    fun `WHEN processing a fact with FEATURE_PROMPTS component THEN the right metric is recorded with no extras`() {
        val controller = ReleaseMetricController(emptyList(), { true }, { true }, mockk())
        val action = mockk<Action>()

        // Verify display interaction
        assertFalse(LoginDialog.displayed.testHasValue())
        var fact = Fact(Component.FEATURE_PROMPTS, action, LoginDialogFacts.Items.DISPLAY)

        controller.run {
            fact.process()
        }

        assertTrue(LoginDialog.displayed.testHasValue())
        assertEquals(1, LoginDialog.displayed.testGetValue().size)
        assertNull(LoginDialog.displayed.testGetValue().single().extra)

        // Verify cancel interaction
        assertFalse(LoginDialog.cancelled.testHasValue())
        fact = Fact(Component.FEATURE_PROMPTS, action, LoginDialogFacts.Items.CANCEL)

        controller.run {
            fact.process()
        }

        assertTrue(LoginDialog.cancelled.testHasValue())
        assertEquals(1, LoginDialog.cancelled.testGetValue().size)
        assertNull(LoginDialog.cancelled.testGetValue().single().extra)

        // Verify never save interaction
        assertFalse(LoginDialog.neverSave.testHasValue())
        fact = Fact(Component.FEATURE_PROMPTS, action, LoginDialogFacts.Items.NEVER_SAVE)

        controller.run {
            fact.process()
        }

        assertTrue(LoginDialog.neverSave.testHasValue())
        assertEquals(1, LoginDialog.neverSave.testGetValue().size)
        assertNull(LoginDialog.neverSave.testGetValue().single().extra)

        // Verify save interaction
        assertFalse(LoginDialog.saved.testHasValue())
        fact = Fact(Component.FEATURE_PROMPTS, action, LoginDialogFacts.Items.SAVE)

        controller.run {
            fact.process()
        }

        assertTrue(LoginDialog.saved.testHasValue())
        assertEquals(1, LoginDialog.saved.testGetValue().size)
        assertNull(LoginDialog.saved.testGetValue().single().extra)
    }

    @Test
    fun `WHEN processing a FEATURE_MEDIA NOTIFICATION fact THEN the right metric is recorded`() {
        val controller = ReleaseMetricController(emptyList(), { true }, { true }, mockk())
        // Verify the play action
        var fact = Fact(Component.FEATURE_MEDIA, Action.PLAY, MediaFacts.Items.NOTIFICATION)
        assertFalse(MediaNotification.play.testHasValue())

        controller.run {
            fact.process()
        }

        assertTrue(MediaNotification.play.testHasValue())
        assertEquals(1, MediaNotification.play.testGetValue().size)
        assertNull(MediaNotification.play.testGetValue().single().extra)

        // Verify the pause action
        fact = Fact(Component.FEATURE_MEDIA, Action.PAUSE, MediaFacts.Items.NOTIFICATION)
        assertFalse(MediaNotification.pause.testHasValue())

        controller.run {
            fact.process()
        }

        assertTrue(MediaNotification.pause.testHasValue())
        assertEquals(1, MediaNotification.pause.testGetValue().size)
        assertNull(MediaNotification.pause.testGetValue().single().extra)
    }

    @Test
    fun `WHEN processing a FEATURE_AUTOFILL fact THEN the right metric is recorded`() {
        val controller = ReleaseMetricController(emptyList(), { true }, { true }, mockk())
        var fact = Fact(
            Component.FEATURE_AUTOFILL,
            mockk(relaxed = true),
            AutofillFacts.Items.AUTOFILL_REQUEST,
            metadata = mapOf(AutofillFacts.Metadata.HAS_MATCHING_LOGINS to true)
        )

        with(controller) {
            assertFalse(AndroidAutofill.requestMatchingLogins.testHasValue())

            fact.process()

            assertTrue(AndroidAutofill.requestMatchingLogins.testHasValue())

            fact = fact.copy(metadata = mapOf(AutofillFacts.Metadata.HAS_MATCHING_LOGINS to false))
            assertFalse(AndroidAutofill.requestNoMatchingLogins.testHasValue())

            fact.process()

            assertTrue(AndroidAutofill.requestNoMatchingLogins.testHasValue())

            fact = fact.copy(item = AutofillFacts.Items.AUTOFILL_SEARCH, action = Action.DISPLAY, metadata = null)
            assertFalse(AndroidAutofill.searchDisplayed.testHasValue())

            fact.process()

            assertTrue(AndroidAutofill.searchDisplayed.testHasValue())

            fact = fact.copy(action = Action.SELECT)
            assertFalse(AndroidAutofill.searchItemSelected.testHasValue())

            fact.process()

            assertTrue(AndroidAutofill.searchItemSelected.testHasValue())

            fact = fact.copy(item = AutofillFacts.Items.AUTOFILL_CONFIRMATION, action = Action.CONFIRM)
            assertFalse(AndroidAutofill.confirmSuccessful.testHasValue())

            fact.process()

            assertTrue(AndroidAutofill.confirmSuccessful.testHasValue())

            fact = fact.copy(action = Action.DISPLAY)
            assertFalse(AndroidAutofill.confirmCancelled.testHasValue())

            fact.process()

            assertTrue(AndroidAutofill.confirmCancelled.testHasValue())

            fact = fact.copy(item = AutofillFacts.Items.AUTOFILL_LOCK, action = Action.CONFIRM)
            assertFalse(AndroidAutofill.unlockSuccessful.testHasValue())

            fact.process()

            assertTrue(AndroidAutofill.unlockSuccessful.testHasValue())

            fact = fact.copy(action = Action.DISPLAY)
            assertFalse(AndroidAutofill.unlockCancelled.testHasValue())

            fact.process()

            assertTrue(AndroidAutofill.unlockCancelled.testHasValue())
        }
    }

    @Test
    fun `WHEN processing a ContextualMenu fact THEN the right metric is recorded`() {
        val controller = ReleaseMetricController(emptyList(), { true }, { true }, mockk())
        val action = mockk<Action>()
        // Verify copy button interaction
        var fact = Fact(
            Component.FEATURE_CONTEXTMENU,
            action,
            ContextMenuFacts.Items.TEXT_SELECTION_OPTION,
            metadata = mapOf("textSelectionOption" to Companion.CONTEXT_MENU_COPY)
        )
        assertFalse(ContextualMenu.copyTapped.testHasValue())

        with(controller) {
            fact.process()
        }

        assertTrue(ContextualMenu.copyTapped.testHasValue())
        assertEquals(1, ContextualMenu.copyTapped.testGetValue().size)
        assertNull(ContextualMenu.copyTapped.testGetValue().single().extra)

        // Verify search button interaction
        fact = Fact(
            Component.FEATURE_CONTEXTMENU,
            action,
            ContextMenuFacts.Items.TEXT_SELECTION_OPTION,
            metadata = mapOf("textSelectionOption" to Companion.CONTEXT_MENU_SEARCH)
        )
        assertFalse(ContextualMenu.searchTapped.testHasValue())

        with(controller) {
            fact.process()
        }

        assertTrue(ContextualMenu.searchTapped.testHasValue())
        assertEquals(1, ContextualMenu.searchTapped.testGetValue().size)
        assertNull(ContextualMenu.searchTapped.testGetValue().single().extra)

        // Verify select all button interaction
        fact = Fact(
            Component.FEATURE_CONTEXTMENU,
            action,
            ContextMenuFacts.Items.TEXT_SELECTION_OPTION,
            metadata = mapOf("textSelectionOption" to Companion.CONTEXT_MENU_SELECT_ALL)
        )
        assertFalse(ContextualMenu.selectAllTapped.testHasValue())

        with(controller) {
            fact.process()
        }

        assertTrue(ContextualMenu.selectAllTapped.testHasValue())
        assertEquals(1, ContextualMenu.selectAllTapped.testGetValue().size)
        assertNull(ContextualMenu.selectAllTapped.testGetValue().single().extra)

        // Verify share button interaction
        fact = Fact(
            Component.FEATURE_CONTEXTMENU,
            action,
            ContextMenuFacts.Items.TEXT_SELECTION_OPTION,
            metadata = mapOf("textSelectionOption" to Companion.CONTEXT_MENU_SHARE)
        )
        assertFalse(ContextualMenu.shareTapped.testHasValue())

        with(controller) {
            fact.process()
        }

        assertTrue(ContextualMenu.shareTapped.testHasValue())
        assertEquals(1, ContextualMenu.shareTapped.testGetValue().size)
        assertNull(ContextualMenu.shareTapped.testGetValue().single().extra)
    }

    @Test
    fun `WHEN processing a CreditCardAutofillDialog fact THEN the right metric is recorded`() {
        val controller = ReleaseMetricController(emptyList(), { true }, { true }, mockk())
        val action = mockk<Action>(relaxed = true)
        val itemsToEvents = listOf(
            CreditCardAutofillDialogFacts.Items.AUTOFILL_CREDIT_CARD_FORM_DETECTED to CreditCards.formDetected,
            CreditCardAutofillDialogFacts.Items.AUTOFILL_CREDIT_CARD_SUCCESS to CreditCards.autofilled,
            CreditCardAutofillDialogFacts.Items.AUTOFILL_CREDIT_CARD_PROMPT_SHOWN to CreditCards.autofillPromptShown,
            CreditCardAutofillDialogFacts.Items.AUTOFILL_CREDIT_CARD_PROMPT_EXPANDED to CreditCards.autofillPromptExpanded,
            CreditCardAutofillDialogFacts.Items.AUTOFILL_CREDIT_CARD_PROMPT_DISMISSED to CreditCards.autofillPromptDismissed,
            CreditCardAutofillDialogFacts.Items.AUTOFILL_CREDIT_CARD_CREATED to CreditCards.savePromptCreate,
            CreditCardAutofillDialogFacts.Items.AUTOFILL_CREDIT_CARD_UPDATED to CreditCards.savePromptUpdate,
        )

        itemsToEvents.forEach { (item, event) ->
            val fact = Fact(Component.FEATURE_PROMPTS, action, item)
            controller.run {
                fact.process()
            }

            assertEquals(true, event.testHasValue())
            assertEquals(1, event.testGetValue().size)
            assertEquals(null, event.testGetValue().single().extra)
        }
    }

    @Test
    fun `GIVEN pwa facts WHEN they are processed THEN the right metric is recorded`() {
        val controller = ReleaseMetricController(emptyList(), { true }, { true }, mockk())
        val action = mockk<Action>(relaxed = true)

        // a PWA shortcut from homescreen was opened
        val openPWA = Fact(
            Component.FEATURE_PWA,
            action,
            ProgressiveWebAppFacts.Items.HOMESCREEN_ICON_TAP,
        )

        assertFalse(ProgressiveWebApp.homescreenTap.testHasValue())
        controller.run {
            openPWA.process()
        }
        assertTrue(ProgressiveWebApp.homescreenTap.testHasValue())

        // a PWA shortcut was installed
        val installPWA = Fact(
            Component.FEATURE_PWA,
            action,
            ProgressiveWebAppFacts.Items.INSTALL_SHORTCUT,
        )

        assertFalse(ProgressiveWebApp.installTap.testHasValue())

        controller.run {
            installPWA.process()
        }

        assertTrue(ProgressiveWebApp.installTap.testHasValue())
    }

    @Test
    fun `WHEN processing a suggestion fact THEN the right metric is recorded`() {
        val controller = ReleaseMetricController(emptyList(), { true }, { true }, mockk())

        // Verify synced tabs suggestion clicked
        assertFalse(SyncedTabs.syncedTabsSuggestionClicked.testHasValue())
        var fact = Fact(Component.FEATURE_SYNCEDTABS, Action.CANCEL, SyncedTabsFacts.Items.SYNCED_TABS_SUGGESTION_CLICKED)

        with(controller) {
            fact.process()
        }

        assertTrue(SyncedTabs.syncedTabsSuggestionClicked.testHasValue())

        // Verify bookmark suggestion clicked
        assertFalse(Awesomebar.bookmarkSuggestionClicked.testHasValue())
        fact = Fact(Component.FEATURE_AWESOMEBAR, Action.CANCEL, AwesomeBarFacts.Items.BOOKMARK_SUGGESTION_CLICKED)

        with(controller) {
            fact.process()
        }

        assertTrue(Awesomebar.bookmarkSuggestionClicked.testHasValue())

        // Verify clipboard suggestion clicked
        assertFalse(Awesomebar.clipboardSuggestionClicked.testHasValue())
        fact = Fact(Component.FEATURE_AWESOMEBAR, Action.CANCEL, AwesomeBarFacts.Items.CLIPBOARD_SUGGESTION_CLICKED)

        with(controller) {
            fact.process()
        }

        assertTrue(Awesomebar.clipboardSuggestionClicked.testHasValue())

        // Verify history suggestion clicked
        assertFalse(Awesomebar.historySuggestionClicked.testHasValue())
        fact = Fact(Component.FEATURE_AWESOMEBAR, Action.CANCEL, AwesomeBarFacts.Items.HISTORY_SUGGESTION_CLICKED)

        with(controller) {
            fact.process()
        }

        assertTrue(Awesomebar.historySuggestionClicked.testHasValue())

        // Verify search action clicked
        assertFalse(Awesomebar.searchActionClicked.testHasValue())
        fact = Fact(Component.FEATURE_AWESOMEBAR, Action.CANCEL, AwesomeBarFacts.Items.SEARCH_ACTION_CLICKED)

        with(controller) {
            fact.process()
        }

        assertTrue(Awesomebar.searchActionClicked.testHasValue())

        // Verify bookmark opened tab suggestion clicked
        assertFalse(Awesomebar.openedTabSuggestionClicked.testHasValue())
        fact = Fact(Component.FEATURE_AWESOMEBAR, Action.CANCEL, AwesomeBarFacts.Items.OPENED_TAB_SUGGESTION_CLICKED)

        with(controller) {
            fact.process()
        }

        assertTrue(Awesomebar.openedTabSuggestionClicked.testHasValue())
    }

    @Test
    fun `GIVEN advertising search facts WHEN the list is processed THEN the right metric is recorded`() {
        val controller = ReleaseMetricController(emptyList(), { true }, { false }, mockk())
        val action = mockk<Action>()

        // an ad was clicked in a Search Engine Result Page
        val addClickedInSearchFact = Fact(
            Component.FEATURE_SEARCH,
            action,
            AdsTelemetry.SERP_ADD_CLICKED,
            "provider"
        )

        assertFalse(BrowserSearch.adClicks["provider"].testHasValue())
        controller.run {
            addClickedInSearchFact.process()
        }
        assertTrue(BrowserSearch.adClicks["provider"].testHasValue())
        assertEquals(1, BrowserSearch.adClicks["provider"].testGetValue())

        // the user opened a Search Engine Result Page of one of our search providers which contains ads
        val searchWithAdsOpenedFact = Fact(
            Component.FEATURE_SEARCH,
            action,
            AdsTelemetry.SERP_SHOWN_WITH_ADDS,
            "provider"
        )

        assertFalse(BrowserSearch.withAds["provider"].testHasValue())

        controller.run {
            searchWithAdsOpenedFact.process()
        }

        assertTrue(BrowserSearch.withAds["provider"].testHasValue())
        assertEquals(1, BrowserSearch.withAds["provider"].testGetValue())

        // the user performed a search
        val inContentSearchFact = Fact(
            Component.FEATURE_SEARCH,
            action,
            InContentTelemetry.IN_CONTENT_SEARCH,
            "provider"
        )

        assertFalse(BrowserSearch.inContent["provider"].testHasValue())

        controller.run {
            inContentSearchFact.process()
        }

        assertTrue(BrowserSearch.inContent["provider"].testHasValue())
        assertEquals(1, BrowserSearch.inContent["provider"].testGetValue())

        // the user performed another search
        controller.run {
            inContentSearchFact.process()
        }

        assertEquals(2, BrowserSearch.inContent["provider"].testGetValue())
    }
}
