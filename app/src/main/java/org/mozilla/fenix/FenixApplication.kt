/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix

import android.annotation.SuppressLint
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.StrictMode
import android.util.Log.INFO
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.getSystemService
import androidx.work.Configuration.Builder
import androidx.work.Configuration.Provider
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mozilla.appservices.Megazord
import mozilla.components.browser.session.Session
import mozilla.components.concept.push.PushProcessor
import mozilla.components.feature.addons.update.GlobalAddonDependencyProvider
import mozilla.components.lib.crash.CrashReporter
import mozilla.components.service.experiments.Experiments
import mozilla.components.service.glean.Glean
import mozilla.components.service.glean.config.Configuration
import mozilla.components.service.glean.net.ConceptFetchHttpUploader
import mozilla.components.support.base.log.Log
import mozilla.components.support.base.log.logger.Logger
import mozilla.components.support.base.log.sink.AndroidLogSink
import mozilla.components.support.ktx.android.content.isMainProcess
import mozilla.components.support.ktx.android.content.runOnlyInMainProcess
import mozilla.components.support.locale.LocaleAwareApplication
import mozilla.components.support.rusthttp.RustHttpConfig
import mozilla.components.support.rustlog.RustLog
import mozilla.components.support.utils.logElapsedTime
import mozilla.components.support.webextensions.WebExtensionSupport
import org.mozilla.fenix.StrictModeManager.enableStrictMode
import org.mozilla.fenix.components.Components
import org.mozilla.fenix.components.metrics.MetricServiceType
import org.mozilla.fenix.ext.resetPoliciesAfter
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.perf.StorageStatsMetrics
import org.mozilla.fenix.perf.StartupTimeline
import org.mozilla.fenix.push.PushFxaIntegration
import org.mozilla.fenix.push.WebPushEngineIntegration
import org.mozilla.fenix.session.PerformanceActivityLifecycleCallbacks
import org.mozilla.fenix.session.VisibilityLifecycleCallback
import org.mozilla.fenix.utils.BrowsersCache

/**
 *The main application class for Fenix. Records data to measure initialization performance.
 *  Installs [CrashReporter], initializes [Glean]  in fenix builds and setup Megazord in the main process.
 */
@Suppress("Registered", "TooManyFunctions", "LargeClass")
open class FenixApplication : LocaleAwareApplication(), Provider {
    init {
        recordOnInit() // DO NOT MOVE ANYTHING ABOVE HERE: the timing of this measurement is critical.
    }

    private val logger = Logger("FenixApplication")

    open val components by lazy { Components(this) }

    var visibilityLifecycleCallback: VisibilityLifecycleCallback? = null
        private set

    override fun onCreate() {
        super.onCreate()

        setupInAllProcesses()

        if (!isMainProcess()) {
            // If this is not the main process then do not continue with the initialization here. Everything that
            // follows only needs to be done in our app's main process and should not be done in other processes like
            // a GeckoView child process or the crash handling process. Most importantly we never want to end up in a
            // situation where we create a GeckoRuntime from the Gecko child process.
            return
        }

        if (Config.channel.isFenix) {
            // We need to always initialize Glean and do it early here.
            // Note that we are only initializing Glean here for "fenix" builds. "fennec" builds
            // will initialize in MigratingFenixApplication because we first need to migrate the
            // user's choice from Fennec.
            initializeGlean()
        }

        setupInMainProcessOnly()
    }

    protected open fun initializeGlean() {
        val telemetryEnabled = settings().isTelemetryEnabled

        logger.debug("Initializing Glean (uploadEnabled=$telemetryEnabled, isFennec=${Config.channel.isFennec})")

        Glean.initialize(
            applicationContext = this,
            configuration = Configuration(
                channel = BuildConfig.BUILD_TYPE,
                httpClient = ConceptFetchHttpUploader(
                    lazy(LazyThreadSafetyMode.NONE) { components.core.client }
                )),
            uploadEnabled = telemetryEnabled
        )
    }

    @CallSuper
    open fun setupInAllProcesses() {
        setupCrashReporting()

        // We want the log messages of all builds to go to Android logcat
        Log.addSink(AndroidLogSink())
    }

    @CallSuper
    open fun setupInMainProcessOnly() {
        run {
            // Attention: Do not invoke any code from a-s in this scope.
            val megazordSetup = setupMegazord()

            setDayNightTheme()
            enableStrictMode(true)
            warmBrowsersCache()

            // Make sure the engine is initialized and ready to use.
            StrictMode.allowThreadDiskReads().resetPoliciesAfter {
                components.core.engine.warmUp()
            }
            initializeWebExtensionSupport()

            // Just to make sure it is impossible for any application-services pieces
            // to invoke parts of itself that require complete megazord initialization
            // before that process completes, we wait here, if necessary.
            if (!megazordSetup.isCompleted) {
                runBlocking { megazordSetup.await(); }
            }
        }

        prefetchForHomeFragment()
        setupLeakCanary()
        startMetricsIfEnabled()
        setupPush()

        visibilityLifecycleCallback = VisibilityLifecycleCallback(getSystemService())
        registerActivityLifecycleCallbacks(visibilityLifecycleCallback)

        // Storage maintenance disabled, for now, as it was interfering with background migrations.
        // See https://github.com/mozilla-mobile/fenix/issues/7227 for context.
        // if ((System.currentTimeMillis() - settings().lastPlacesStorageMaintenance) > ONE_DAY_MILLIS) {
        //    runStorageMaintenance()
        // }

        initVisualCompletenessQueueAndQueueTasks()
    }

    private fun initVisualCompletenessQueueAndQueueTasks() {
        val queue = components.performance.visualCompletenessQueue.queue

        fun initQueue() {
            registerActivityLifecycleCallbacks(PerformanceActivityLifecycleCallbacks(queue))
        }

        fun queueInitExperiments() {
            if (settings().isExperimentationEnabled) {
                queue.runIfReadyOrQueue {
                    Experiments.initialize(
                        applicationContext = applicationContext,
                        onExperimentsUpdated = {
                            ExperimentsManager.initSearchWidgetExperiment(this)
                        },
                        configuration = mozilla.components.service.experiments.Configuration(
                            httpClient = components.core.client,
                            kintoEndpoint = KINTO_ENDPOINT_PROD
                        )
                    )
                    ExperimentsManager.initSearchWidgetExperiment(this)
                }
            } else {
                // We should make a better way to opt out for when we have more experiments
                // See https://github.com/mozilla-mobile/fenix/issues/6278
                ExperimentsManager.optOutSearchWidgetExperiment(this)
            }
        }

        fun queueInitStorageAndServices() {
            components.performance.visualCompletenessQueue.queue.runIfReadyOrQueue {
                GlobalScope.launch(Dispatchers.IO) {
                    logger.info("Running post-visual completeness tasks...")
                    logElapsedTime(logger, "Storage initialization") {
                        components.core.historyStorage.warmUp()
                        components.core.bookmarksStorage.warmUp()
                        components.core.passwordsStorage.warmUp()
                    }
                }
                // Account manager initialization needs to happen on the main thread.
                GlobalScope.launch(Dispatchers.Main) {
                    logElapsedTime(logger, "Kicking-off account manager") {
                        components.backgroundServices.accountManager
                    }
                }
            }
        }

        fun queueMetrics() {
            if (SDK_INT >= Build.VERSION_CODES.O) { // required by StorageStatsMetrics.
                queue.runIfReadyOrQueue {
                    // Because it may be slow to capture the storage stats, it might be preferred to
                    // create a WorkManager task for this metric, however, I ran out of
                    // implementation time and WorkManager is harder to test.
                    StorageStatsMetrics.report(this.applicationContext)
                }
            }
        }

        initQueue()

        // We init these items in the visual completeness queue to avoid them initing in the critical
        // startup path, before the UI finishes drawing (i.e. visual completeness).
        queueInitExperiments()
        queueInitStorageAndServices()
        queueMetrics()
    }

    private fun startMetricsIfEnabled() {
        if (settings().isTelemetryEnabled) {
            components.analytics.metrics.start(MetricServiceType.Data)
        }

        if (settings().isMarketingTelemetryEnabled) {
            components.analytics.metrics.start(MetricServiceType.Marketing)
        }
    }

    // See https://github.com/mozilla-mobile/fenix/issues/7227 for context.
    // To re-enable this, we need to do so in a way that won't interfere with any startup operations
    // which acquire reserved+ sqlite lock. Currently, Fennec migrations need to write to storage
    // on startup, and since they run in a background service we can't simply order these operations.
    private fun runStorageMaintenance() {
        GlobalScope.launch(Dispatchers.IO) {
            // Bookmarks and history storage sit on top of the same db file so we only need to
            // run maintenance on one - arbitrarily using bookmarks.
            components.core.bookmarksStorage.runMaintenance()
        }
        settings().lastPlacesStorageMaintenance = System.currentTimeMillis()
    }

    protected open fun setupLeakCanary() {
        // no-op, LeakCanary is disabled by default
    }

    open fun updateLeakCanaryState(isEnabled: Boolean) {
        // no-op, LeakCanary is disabled by default
    }

    // This is for issue https://github.com/mozilla-mobile/fenix/issues/11660. We prefetch our info for startup
    // so that we're sure that we have all the data available as our fragment is launched.
    private fun prefetchForHomeFragment() {
        StrictMode.allowThreadDiskReads().resetPoliciesAfter {
            components.core.topSiteStorage.prefetch()
        }
    }

    private fun setupPush() {
        // Sets the PushFeature as the singleton instance for push messages to go to.
        // We need the push feature setup here to deliver messages in the case where the service
        // starts up the app first.
        components.push.feature?.let {
            Logger.info("AutoPushFeature is configured, initializing it...")

            // Install the AutoPush singleton to receive messages.
            PushProcessor.install(it)

            WebPushEngineIntegration(components.core.engine, it).start()

            // Perform a one-time initialization of the account manager if a message is received.
            PushFxaIntegration(it, lazy { components.backgroundServices.accountManager }).launch()

            // Initialize the service. This could potentially be done in a coroutine in the future.
            it.initialize()
        }
    }

    private fun setupCrashReporting() {
        components
            .analytics
            .crashReporter
            .install(this)
    }

    /**
     * Initiate Megazord sequence! Megazord Battle Mode!
     *
     * The application-services combined libraries are known as the "megazord". We use the default `full`
     * megazord - it contains everything that fenix needs, and (currently) nothing more.
     *
     * Documentation on what megazords are, and why they're needed:
     * - https://github.com/mozilla/application-services/blob/master/docs/design/megazords.md
     * - https://mozilla.github.io/application-services/docs/applications/consuming-megazord-libraries.html
     */
    private fun setupMegazord(): Deferred<Unit> {
        // Note: Megazord.init() must be called as soon as possible ...
        Megazord.init()

        return GlobalScope.async(Dispatchers.IO) {
            // ... but RustHttpConfig.setClient() and RustLog.enable() can be called later.
            RustHttpConfig.setClient(lazy { components.core.client })
            RustLog.enable(components.analytics.crashReporter)
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)

        runOnlyInMainProcess {
            components.core.icons.onTrimMemory(level)
            components.core.sessionManager.onTrimMemory(level)
        }
    }

    @SuppressLint("WrongConstant")
    // Suppressing erroneous lint warning about using MODE_NIGHT_AUTO_BATTERY, a likely library bug
    private fun setDayNightTheme() {
        val settings = this.settings()
        when {
            settings.shouldUseLightTheme -> {
                AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_NO
                )
            }
            settings.shouldUseDarkTheme -> {
                AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_YES
                )
            }
            SDK_INT < Build.VERSION_CODES.P && settings.shouldUseAutoBatteryTheme -> {
                AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
                )
            }
            SDK_INT >= Build.VERSION_CODES.P && settings.shouldFollowDeviceTheme -> {
                AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                )
            }
            // First run of app no default set, set the default to Follow System for 28+ and Normal Mode otherwise
            else -> {
                if (SDK_INT >= Build.VERSION_CODES.P) {
                    AppCompatDelegate.setDefaultNightMode(
                        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    )
                    settings.shouldFollowDeviceTheme = true
                } else {
                    AppCompatDelegate.setDefaultNightMode(
                        AppCompatDelegate.MODE_NIGHT_NO
                    )
                    settings.shouldUseLightTheme = true
                }
            }
        }
    }

    private fun warmBrowsersCache() {
        // We avoid blocking the main thread for BrowsersCache on startup by loading it on
        // background thread.
        GlobalScope.launch(Dispatchers.Default) {
            BrowsersCache.all(this@FenixApplication)
        }
    }

    private fun initializeWebExtensionSupport() {
        try {
            GlobalAddonDependencyProvider.initialize(
                components.addonManager,
                components.addonUpdater,
                onCrash = { exception ->
                    components.analytics.crashReporter.submitCaughtException(exception)
                }
            )
            WebExtensionSupport.initialize(
                components.core.engine,
                components.core.store,
                onNewTabOverride = {
                    _, engineSession, url ->
                        val shouldCreatePrivateSession =
                            components.core.sessionManager.selectedSession?.private
                                ?: components.settings.openLinksInAPrivateTab

                        val session = Session(url, shouldCreatePrivateSession)
                        components.core.sessionManager.add(session, true, engineSession)
                        session.id
                },
                onCloseTabOverride = {
                    _, sessionId -> components.useCases.tabsUseCases.removeTab(sessionId)
                },
                onSelectTabOverride = {
                    _, sessionId ->
                        val selected = components.core.sessionManager.findSessionById(sessionId)
                        selected?.let { components.useCases.tabsUseCases.selectTab(it) }
                },
                onExtensionsLoaded = { extensions ->
                    components.addonUpdater.registerForFutureUpdates(extensions)
                    components.supportedAddonsChecker.registerForChecks()
                },
                onUpdatePermissionRequest = components.addonUpdater::onUpdatePermissionRequest
            )
        } catch (e: UnsupportedOperationException) {
            Logger.error("Failed to initialize web extension support", e)
        }
    }

    protected fun recordOnInit() {
        // This gets called by more than one process. Ideally we'd only run this in the main process
        // but the code to check which process we're in crashes because the Context isn't valid yet.
        //
        // This method is not covered by our internal crash reporting: be very careful when modifying it.
        StartupTimeline.onApplicationInit() // DO NOT MOVE ANYTHING ABOVE HERE: the timing is critical.
    }

    override fun onConfigurationChanged(config: android.content.res.Configuration) {
        // Workaround for androidx appcompat issue where follow system day/night mode config changes
        // are not triggered when also using createConfigurationContext like we do in LocaleManager
        // https://issuetracker.google.com/issues/143570309#comment3
        applicationContext.resources.configuration.uiMode = config.uiMode

        // random StrictMode onDiskRead violation even when Fenix is not running in the background.
        StrictMode.allowThreadDiskReads().resetPoliciesAfter {
            super.onConfigurationChanged(config)
        }
    }

    companion object {
        private const val KINTO_ENDPOINT_PROD = "https://firefox.settings.services.mozilla.com/v1"
    }

    override fun getWorkManagerConfiguration() = Builder().setMinimumLoggingLevel(INFO).build()
}
