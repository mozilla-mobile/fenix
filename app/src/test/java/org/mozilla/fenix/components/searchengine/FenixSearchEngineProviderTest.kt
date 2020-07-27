package org.mozilla.fenix.components.searchengine

import android.content.Context
import androidx.core.content.edit
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import mozilla.components.browser.search.SearchEngine
import mozilla.components.browser.search.provider.SearchEngineList
import mozilla.components.browser.search.provider.localization.LocaleSearchLocalizationProvider
import mozilla.components.browser.search.provider.localization.SearchLocalizationProvider
import mozilla.components.support.test.robolectric.testContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.helpers.FenixRobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(FenixRobolectricTestRunner::class)
class FenixSearchEngineProviderTest {

    private lateinit var fenixSearchEngineProvider: FenixSearchEngineProvider

    private val scope = TestCoroutineScope()

    @Before
    fun before() {
        fenixSearchEngineProvider = FakeFenixSearchEngineProvider(testContext)
        mockkObject(CustomSearchEngineStore)

        every { CustomSearchEngineStore.loadCustomSearchEngines(any()) } returns listOf(
            mockSearchEngine("my custom site", "my custom site")
        )
        coEvery { CustomSearchEngineStore.addSearchEngine(any(), any(), any()) } just Runs
    }

    @After
    fun teardown() {
        unmockkObject(CustomSearchEngineStore)
    }

    /*
    TODO TEST:
        - public API happy path
        - list ordering
        - deduping
        - the above after adding/removing
     */

    @Suppress("DEPRECATION")
    @Test
    fun `add custom engine`() = scope.runBlockingTest {
        val engineName = "Ecosia"
        val engineQuery = "www.ecosia.com/%s"
        val searchEngine: SearchEngine = mockk(relaxed = true)
        every { searchEngine.getSearchTemplate() } returns engineQuery
        every { searchEngine.name } returns engineName

        fenixSearchEngineProvider.installSearchEngine(testContext, searchEngine, true)

        coVerify { CustomSearchEngineStore.addSearchEngine(testContext, engineName, engineQuery) }
    }

    @Test
    fun `GIVEN sharedprefs does not contain installed engines WHEN installedSearchEngineIdentifiers THEN defaultEngines + customEngines ids are returned`() = scope.runBlockingTest {
        val expectedDefaults = fenixSearchEngineProvider.baseSearchEngines.toIdSet()
        val expectedCustom = fenixSearchEngineProvider.customSearchEngines.toIdSet()
        val expected = expectedDefaults + expectedCustom

        val actual = fenixSearchEngineProvider.installedSearchEngineIdentifiers()
        assertEquals(expected, actual)
    }

    @Test
    fun `GIVEN sharedprefs contains installed engines WHEN installedSearchEngineIdentifiers THEN defaultEngines + customEngines ids are returned`() = scope.runBlockingTest {
        val persistedInstalledEngines = setOf("bing", "ecosia")

        fenixSearchEngineProvider.prefs.edit(commit = true) {
            putStringSet(fenixSearchEngineProvider.localeAwareInstalledEnginesKey(), persistedInstalledEngines)
        }

        val expectedStored = persistedInstalledEngines
        val expectedCustom = fenixSearchEngineProvider.customSearchEngines.toIdSet()
        val expected = expectedStored + expectedCustom

        val actual = fenixSearchEngineProvider.installedSearchEngineIdentifiers()
        assertEquals(expected, actual)
    }

    fun mockSearchEngine(
        id: String,
        n: String = id
    ): SearchEngine {
        val engine = mockk<SearchEngine>()
        every { engine.identifier } returns id
        every { engine.name } returns n
        every { engine.icon } returns mockk()
        return engine
    }

    inner class FakeFenixSearchEngineProvider(
        context: Context
    ) : FenixSearchEngineProvider(context, scope) {

        override val localizationProvider: SearchLocalizationProvider
            get() = LocaleSearchLocalizationProvider()

        override var baseSearchEngines: Deferred<SearchEngineList>
            set(_) { throw NotImplementedError("Setting not currently supported on this fake") }
            get() {
                val google = mockSearchEngine(id = "google-b-1-m", n = "Google")

                return CompletableDeferred(
                    SearchEngineList(
                        listOf(
                            google,
                            mockSearchEngine("bing", "Bing"),
                            mockSearchEngine("amazondotcom", "Amazon.com")
                        ), default = google
                    )
                )
            }

        override val fallbackEngines: Deferred<SearchEngineList>
            get() {
                val google = mockSearchEngine(id = "google-b-1-m", n = "Google")

                return CompletableDeferred(
                    SearchEngineList(
                        listOf(
                            google,
                            mockSearchEngine("bing", "Bing"),
                            mockSearchEngine("amazondotcom", "Amazon.com")
                        ), default = google
                    )
                )
            }

        override val bundledSearchEngines = CompletableDeferred(
            SearchEngineList(
                listOf(
                    mockSearchEngine("ecosia", "Ecosia"),
                    mockSearchEngine("reddit", "Reddit"),
                    mockSearchEngine("startpage", "Startpage.com")
                ), default = null
            )
        )

        override var customSearchEngines: Deferred<SearchEngineList> = CompletableDeferred(
            SearchEngineList(
                listOf(
                    mockSearchEngine("my custom site", "my custom site")
                ), default = null
            )
        )

        override fun updateBaseSearchEngines() = Unit
    }

    private suspend fun Deferred<SearchEngineList>.toIdSet() =
        await().list.map { it.identifier }.toSet()
}
