/* This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.browser

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.component_search.*
import kotlinx.android.synthetic.main.fragment_browser.*
import kotlinx.android.synthetic.main.fragment_browser.view.*
import kotlinx.android.synthetic.main.fragment_search.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import mozilla.appservices.places.BookmarkRoot
import mozilla.components.browser.session.Session
import mozilla.components.feature.contextmenu.ContextMenuCandidate
import mozilla.components.feature.contextmenu.ContextMenuFeature
import mozilla.components.feature.downloads.DownloadsFeature
import mozilla.components.feature.prompts.PromptFeature
import mozilla.components.feature.session.FullScreenFeature
import mozilla.components.feature.session.SessionFeature
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.feature.session.ThumbnailsFeature
import mozilla.components.feature.sitepermissions.SitePermissions
import mozilla.components.feature.sitepermissions.SitePermissionsFeature
import mozilla.components.feature.sitepermissions.SitePermissionsRules
import mozilla.components.support.base.feature.BackHandler
import mozilla.components.support.base.feature.ViewBoundFeatureWrapper
import mozilla.components.support.ktx.android.view.enterToImmersiveMode
import mozilla.components.support.ktx.android.view.exitImmersiveModeIfNeeded
import mozilla.components.support.ktx.kotlin.toUri
import org.mozilla.fenix.BrowsingModeManager
import org.mozilla.fenix.DefaultThemeManager
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.IntentReceiverActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.components.FenixSnackbar
import org.mozilla.fenix.components.FindInPageIntegration
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.components.metrics.Event.BrowserMenuItemTapped.Item
import org.mozilla.fenix.components.toolbar.SearchAction
import org.mozilla.fenix.components.toolbar.SearchState
import org.mozilla.fenix.components.toolbar.ToolbarComponent
import org.mozilla.fenix.components.toolbar.ToolbarIntegration
import org.mozilla.fenix.components.toolbar.ToolbarMenu
import org.mozilla.fenix.components.toolbar.ToolbarUIView
import org.mozilla.fenix.customtabs.CustomTabsIntegration
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.ext.share
import org.mozilla.fenix.lib.Do
import org.mozilla.fenix.mvi.ActionBusFactory
import org.mozilla.fenix.mvi.getAutoDisposeObservable
import org.mozilla.fenix.quickactionsheet.QuickActionAction
import org.mozilla.fenix.quickactionsheet.QuickActionComponent
import org.mozilla.fenix.settings.quicksettings.QuickSettingsSheetDialogFragment
import org.mozilla.fenix.utils.ItsNotBrokenSnack
import org.mozilla.fenix.utils.Settings
import kotlin.coroutines.CoroutineContext

@SuppressWarnings("TooManyFunctions", "LargeClass")
class BrowserFragment : Fragment(), BackHandler, CoroutineScope {
    private lateinit var toolbarComponent: ToolbarComponent

    private var sessionObserver: Session.Observer? = null
    private val sessionFeature = ViewBoundFeatureWrapper<SessionFeature>()
    private val contextMenuFeature = ViewBoundFeatureWrapper<ContextMenuFeature>()
    private val downloadsFeature = ViewBoundFeatureWrapper<DownloadsFeature>()
    private val promptsFeature = ViewBoundFeatureWrapper<PromptFeature>()
    private val findInPageIntegration = ViewBoundFeatureWrapper<FindInPageIntegration>()
    private val toolbarIntegration = ViewBoundFeatureWrapper<ToolbarIntegration>()
    private val sitePermissionsFeature = ViewBoundFeatureWrapper<SitePermissionsFeature>()
    private val fullScreenFeature = ViewBoundFeatureWrapper<FullScreenFeature>()
    private val thumbnailsFeature = ViewBoundFeatureWrapper<ThumbnailsFeature>()
    private val customTabsIntegration = ViewBoundFeatureWrapper<CustomTabsIntegration>()
    private lateinit var job: Job

    var sessionId: String? = null

    override val coroutineContext: CoroutineContext get() = Dispatchers.IO + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        job = Job()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        require(arguments != null)
        sessionId = BrowserFragmentArgs.fromBundle(arguments!!).sessionId

        val view = inflater.inflate(R.layout.fragment_browser, container, false)

        toolbarComponent = ToolbarComponent(
            view.browserLayout,
            ActionBusFactory.get(this), sessionId,
            (activity as HomeActivity).browsingModeManager.isPrivate,
            SearchState("", isEditing = false),
            search_engine_icon
        )

        toolbarComponent.uiView.view.apply {
            setBackgroundColor(
                ContextCompat.getColor(
                    view.context,
                    DefaultThemeManager.resolveAttribute(R.attr.foundation, context)
                )
            )

            (layoutParams as CoordinatorLayout.LayoutParams).apply {
                gravity = getAppropriateLayoutGravity()

                view.nestedScrollQuickAction.visibility = if (gravity == Gravity.TOP) {
                    View.GONE
                } else {
                    View.VISIBLE
                }

                height = (resources.displayMetrics.density * TOOLBAR_HEIGHT).toInt()
            }
        }

        QuickActionComponent(view.nestedScrollQuickAction, ActionBusFactory.get(this))

        val activity = activity as HomeActivity
        DefaultThemeManager.applyStatusBarTheme(activity.window, activity.themeManager, activity, false)

        return view
    }

    private fun getAppropriateLayoutGravity(): Int {
        sessionId?.let { sessionId ->
            if (requireComponents.core.sessionManager.findSessionById(sessionId)?.isCustomTabSession() == true) {
                return Gravity.TOP
            }
        }

        return Gravity.BOTTOM
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionId = BrowserFragmentArgs.fromBundle(arguments!!).sessionId

        val sessionManager = requireComponents.core.sessionManager

        contextMenuFeature.set(
            feature = ContextMenuFeature(
                requireFragmentManager(),
                sessionManager,
                ContextMenuCandidate.defaultCandidates(
                    requireContext(),
                    requireComponents.useCases.tabsUseCases,
                    view
                ),
                view.engineView
            ),
            owner = this,
            view = view
        )

        downloadsFeature.set(
            feature = DownloadsFeature(
                requireContext(),
                sessionManager = sessionManager,
                fragmentManager = childFragmentManager,
                onNeedToRequestPermissions = { permissions ->
                    requestPermissions(permissions, REQUEST_CODE_DOWNLOAD_PERMISSIONS)
                }),
            owner = this,
            view = view
        )

        promptsFeature.set(
            feature = PromptFeature(
                fragment = this,
                sessionManager = sessionManager,
                fragmentManager = requireFragmentManager(),
                onNeedToRequestPermissions = { permissions ->
                    requestPermissions(permissions, REQUEST_CODE_PROMPT_PERMISSIONS)
                }),
            owner = this,
            view = view
        )

        sessionFeature.set(
            feature = SessionFeature(
                sessionManager,
                SessionUseCases(sessionManager),
                view.engineView,
                sessionId
            ),
            owner = this,
            view = view
        )

        findInPageIntegration.set(
            feature = FindInPageIntegration(
                requireComponents.core.sessionManager, view.findInPageView, view.engineView
            ),
            owner = this,
            view = view
        )

        toolbarIntegration.set(
            feature = (toolbarComponent.uiView as ToolbarUIView).toolbarIntegration,
            owner = this,
            view = view
        )

        sitePermissionsFeature.set(
            feature = SitePermissionsFeature(
                anchorView = view.findInPageView,
                sessionManager = sessionManager
            ) { permissions ->
                requestPermissions(permissions, REQUEST_CODE_APP_PERMISSIONS)
            },
            owner = this,
            view = view
        )

        fullScreenFeature.set(
            feature = FullScreenFeature(
                sessionManager,
                SessionUseCases(sessionManager),
                sessionId
            ) {
                if (it) {
                    Snackbar.make(view.rootView, getString(R.string.full_screen_notification), Snackbar.LENGTH_LONG)
                        .show()
                    activity?.enterToImmersiveMode()
                    toolbar.visibility = View.GONE
                    nestedScrollQuickAction.visibility = View.GONE
                } else {
                    activity?.exitImmersiveModeIfNeeded()
                    toolbar.visibility = View.VISIBLE
                    nestedScrollQuickAction.visibility = View.VISIBLE
                }
            },
            owner = this,
            view = view
        )

        thumbnailsFeature.set(
            feature = ThumbnailsFeature(requireContext(), view.engineView, requireComponents.core.sessionManager),
            owner = this,
            view = view
        )

        val actionEmitter = ActionBusFactory.get(this).getManagedEmitter(SearchAction::class.java)

        sessionId?.let { sessionId ->
            if (sessionManager.findSessionById(sessionId)?.isCustomTabSession() == true) {
                customTabsIntegration.set(
                    feature = CustomTabsIntegration(
                        requireContext(),
                        requireComponents.core.sessionManager,
                        toolbar,
                        sessionId,
                        activity,
                        onItemTapped = { actionEmitter.onNext(SearchAction.ToolbarMenuItemTapped(it)) }
                    ),
                    owner = this,
                    view = view
                )
            }
        }

        toolbarComponent.getView().setOnSiteSecurityClickedListener {
            showQuickSettingsDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity).supportActionBar?.hide()
    }

    @Suppress("ComplexMethod")
    override fun onStart() {
        super.onStart()
        sessionObserver = subscribeToSession()
        getAutoDisposeObservable<SearchAction>()
            .subscribe {
                when (it) {
                    is SearchAction.ToolbarClicked -> {
                        Navigation
                            .findNavController(toolbarComponent.getView())
                            .navigate(
                                BrowserFragmentDirections.actionBrowserFragmentToSearchFragment(
                                    getSessionByIdOrUseSelectedSession().id
                                )
                            )

                        requireComponents.analytics.metrics.track(
                            Event.SearchBarTapped(Event.SearchBarTapped.Source.BROWSER)
                        )
                    }
                    is SearchAction.ToolbarMenuItemTapped -> {
                        trackToolbarItemInteraction(it)
                        handleToolbarItemInteraction(it)
                    }
                    is SearchAction.ToolbarLongClicked -> {
                        getSessionByIdOrUseSelectedSession().copyUrl(requireContext())
                        FenixSnackbar.make(view!!, Snackbar.LENGTH_LONG)
                            .setText(resources.getString(R.string.url_copied))
                            .show()
                    }
                }
            }

        getAutoDisposeObservable<QuickActionAction>()
            .subscribe {
                when (it) {
                    is QuickActionAction.Opened -> {
                        requireComponents.analytics.metrics.track(Event.QuickActionSheetOpened)
                    }
                    is QuickActionAction.Closed -> {
                        requireComponents.analytics.metrics.track(Event.QuickActionSheetClosed)
                    }
                    is QuickActionAction.SharePressed -> {
                        requireComponents.analytics.metrics.track(Event.QuickActionSheetShareTapped)
                        getSessionByIdOrUseSelectedSession().url.apply { requireContext().share(this) }
                    }
                    is QuickActionAction.DownloadsPressed -> {
                        requireComponents.analytics.metrics.track(Event.QuickActionSheetDownloadTapped)
                        ItsNotBrokenSnack(context!!).showSnackbar(issueNumber = "348")
                    }
                    is QuickActionAction.BookmarkPressed -> {
                        requireComponents.analytics.metrics.track(Event.QuickActionSheetBookmarkTapped)
                        val session = getSessionByIdOrUseSelectedSession()
                        CoroutineScope(IO).launch {
                            val guid = requireComponents.core.bookmarksStorage
                                .addItem(BookmarkRoot.Mobile.id, session.url, session.title, null)
                            launch(Main) {
                                requireComponents.analytics.metrics.track(Event.AddBookmark)
                                FenixSnackbar.make(
                                    view!!,
                                    Snackbar.LENGTH_LONG
                                )
                                    .setAction(getString(R.string.edit_bookmark_snackbar_action)) {
                                        Navigation.findNavController(requireActivity(), R.id.container)
                                            .navigate(
                                                BrowserFragmentDirections
                                                    .actionBrowserFragmentToBookmarkEditFragment(
                                                        guid
                                                    )
                                            )
                                    }
                                    .setText(getString(R.string.bookmark_saved_snackbar))
                                    .show()
                            }
                        }
                    }
                    is QuickActionAction.ReadPressed -> {
                        requireComponents.analytics.metrics.track(Event.QuickActionSheetReadTapped)
                        ItsNotBrokenSnack(context!!).showSnackbar(issueNumber = "908")
                    }
                }
            }
        assignSitePermissionsRules()
    }

    override fun onStop() {
        super.onStop()
        sessionObserver?.let {
            requireComponents.core.sessionManager.selectedSession?.unregister(it)
        }
    }

    override fun onBackPressed(): Boolean {
        return when {
            findInPageIntegration.onBackPressed() -> true
            sessionFeature.onBackPressed() -> true
            else -> false
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_CODE_DOWNLOAD_PERMISSIONS -> downloadsFeature.withFeature {
                it.onPermissionsResult(permissions, grantResults)
            }
            REQUEST_CODE_PROMPT_PERMISSIONS -> promptsFeature.withFeature {
                it.onPermissionsResult(permissions, grantResults)
            }
            REQUEST_CODE_APP_PERMISSIONS -> sitePermissionsFeature.withFeature {
                it.onPermissionsResult(grantResults)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        promptsFeature.withFeature { it.onActivityResult(requestCode, resultCode, data) }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    // This method triggers the complexity warning. However it's actually not that hard to understand.
    @SuppressWarnings("ComplexMethod")
    private fun trackToolbarItemInteraction(action: SearchAction.ToolbarMenuItemTapped) {
        val item = when (action.item) {
            ToolbarMenu.Item.Back -> Item.BACK
            ToolbarMenu.Item.Forward -> Item.FORWARD
            ToolbarMenu.Item.Reload -> Item.RELOAD
            ToolbarMenu.Item.Stop -> Item.STOP
            ToolbarMenu.Item.Settings -> Item.SETTINGS
            ToolbarMenu.Item.Library -> Item.LIBRARY
            is ToolbarMenu.Item.RequestDesktop ->
                if (action.item.isChecked) Item.DESKTOP_VIEW_ON else Item.DESKTOP_VIEW_OFF
            ToolbarMenu.Item.NewPrivateTab -> Item.NEW_PRIVATE_TAB
            ToolbarMenu.Item.FindInPage -> Item.FIND_IN_PAGE
            ToolbarMenu.Item.ReportIssue -> Item.REPORT_SITE_ISSUE
            ToolbarMenu.Item.Help -> Item.HELP
            ToolbarMenu.Item.NewTab -> Item.NEW_TAB
            ToolbarMenu.Item.OpenInFenix -> Item.OPEN_IN_FENIX
            ToolbarMenu.Item.Share -> Item.SHARE
        }

        requireComponents.analytics.metrics.track(Event.BrowserMenuItemTapped(item))
    }

    // This method triggers the complexity warning. However it's actually not that hard to understand.
    @SuppressWarnings("ComplexMethod")
    private fun handleToolbarItemInteraction(action: SearchAction.ToolbarMenuItemTapped) {
        val sessionUseCases = requireComponents.useCases.sessionUseCases
        Do exhaustive when (action.item) {
            ToolbarMenu.Item.Back -> sessionUseCases.goBack.invoke()
            ToolbarMenu.Item.Forward -> sessionUseCases.goForward.invoke()
            ToolbarMenu.Item.Reload -> sessionUseCases.reload.invoke()
            ToolbarMenu.Item.Stop -> sessionUseCases.stopLoading.invoke()
            ToolbarMenu.Item.Settings -> Navigation.findNavController(toolbarComponent.getView())
                .navigate(BrowserFragmentDirections.actionBrowserFragmentToSettingsFragment())
            ToolbarMenu.Item.Library -> Navigation.findNavController(toolbarComponent.getView())
                .navigate(BrowserFragmentDirections.actionBrowserFragmentToLibraryFragment())
            is ToolbarMenu.Item.RequestDesktop -> sessionUseCases.requestDesktopSite.invoke(action.item.isChecked)
            ToolbarMenu.Item.Share -> getSessionByIdOrUseSelectedSession().url.apply { requireContext().share(this) }
            ToolbarMenu.Item.NewPrivateTab -> {
                val directions = BrowserFragmentDirections
                    .actionBrowserFragmentToSearchFragment(null)
                Navigation.findNavController(view!!).navigate(directions)
                (activity as HomeActivity).browsingModeManager.mode = BrowsingModeManager.Mode.Private
            }
            ToolbarMenu.Item.FindInPage -> {
                FindInPageIntegration.launch?.invoke()
                requireComponents.analytics.metrics.track(Event.FindInPageOpened)
            }
            ToolbarMenu.Item.ReportIssue -> getSessionByIdOrUseSelectedSession().url.apply {
                val reportUrl = String.format(REPORT_SITE_ISSUE_URL, this)
                sessionUseCases.loadUrl.invoke(reportUrl)
            }
            ToolbarMenu.Item.Help -> {
                // TODO Help #1016
                ItsNotBrokenSnack(context!!).showSnackbar(issueNumber = "1016")
            }
            ToolbarMenu.Item.NewTab -> {
                val directions = BrowserFragmentDirections
                    .actionBrowserFragmentToSearchFragment(null)
                Navigation.findNavController(view!!).navigate(directions)
                (activity as HomeActivity).browsingModeManager.mode = BrowsingModeManager.Mode.Normal
            }
            ToolbarMenu.Item.OpenInFenix -> {
                val intent = Intent(context, IntentReceiverActivity::class.java)
                val session = context!!.components.core.sessionManager.findSessionById(sessionId!!)
                intent.action = Intent.ACTION_VIEW
                intent.data = Uri.parse(session?.url)
                startActivity(intent)
            }
        }
    }

    private fun assignSitePermissionsRules() {
        val settings = Settings.getInstance(requireContext())

        val rules: SitePermissionsRules = settings.getSitePermissionsCustomSettingsRules()

        sitePermissionsFeature.withFeature {
            it.sitePermissionsRules = rules
        }
    }

    private fun showQuickSettingsDialog() {
        val session = getSessionByIdOrUseSelectedSession()
        val host = requireNotNull(session.url.toUri().host)

        launch {
            val storage = requireContext().components.storage
            val sitePermissions: SitePermissions? = storage.findSitePermissionsBy(host)

            launch(Main) {
                val quickSettingsSheet = QuickSettingsSheetDialogFragment.newInstance(
                    url = session.url,
                    isSecured = session.securityInfo.secure,
                    isTrackingProtectionOn = Settings.getInstance(context!!).shouldUseTrackingProtection,
                    sitePermissions = sitePermissions
                )
                quickSettingsSheet.sitePermissions = sitePermissions
                quickSettingsSheet.show(
                    requireFragmentManager(),
                    QuickSettingsSheetDialogFragment.FRAGMENT_TAG
                )
            }
        }
    }

    private fun getSessionByIdOrUseSelectedSession(): Session {
        return if (sessionId != null) {
            requireNotNull(requireContext().components.core.sessionManager.findSessionById(requireNotNull(sessionId)))
        } else {
            requireContext().components.core.sessionManager.selectedSessionOrThrow
        }
    }

    private fun Session.copyUrl(context: Context) {
        val clipBoard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val uri = Uri.parse(url)
        clipBoard.primaryClip = ClipData.newRawUri("Uri", uri)
    }

    private fun subscribeToSession(): Session.Observer {
        val observer = object : Session.Observer {
            override fun onLoadingStateChanged(session: Session, loading: Boolean) {
                super.onLoadingStateChanged(session, loading)
                setToolbarBehavior(loading)
            }
        }
        requireComponents.core.sessionManager.selectedSession?.register(observer)
        return observer
    }

    private fun setToolbarBehavior(loading: Boolean) {
        val toolbarView = toolbarComponent.uiView.view
        (toolbarView.layoutParams as CoordinatorLayout.LayoutParams).apply {
            // Stop toolbar from collapsing if TalkBack is enabled or page is loading
            val accessibilityManager = context
                ?.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
            if (!accessibilityManager.isTouchExplorationEnabled) {
                if (!loading) {
                    behavior = BrowserToolbarBottomBehavior(context, null)
                } else {
                    (behavior as? BrowserToolbarBottomBehavior)?.forceExpand(toolbarView)
                    behavior = null
                }
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_DOWNLOAD_PERMISSIONS = 1
        private const val REQUEST_CODE_PROMPT_PERMISSIONS = 2
        private const val REQUEST_CODE_APP_PERMISSIONS = 3
        private const val TOOLBAR_HEIGHT = 56f
        const val REPORT_SITE_ISSUE_URL = "https://webcompat.com/issues/new?url=%s&label=browser-fenix"
    }
}
