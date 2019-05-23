/* This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home

import android.content.DialogInterface
import android.content.res.Resources
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.Navigation
import androidx.navigation.fragment.FragmentNavigator
import androidx.transition.TransitionInflater
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.fragment_home.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mozilla.components.browser.menu.BrowserMenu
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import mozilla.components.concept.sync.AccountObserver
import mozilla.components.concept.sync.OAuthAccount
import mozilla.components.concept.sync.Profile
import org.jetbrains.anko.constraint.layout.ConstraintSetBuilder.Side.BOTTOM
import org.jetbrains.anko.constraint.layout.ConstraintSetBuilder.Side.END
import org.jetbrains.anko.constraint.layout.ConstraintSetBuilder.Side.START
import org.jetbrains.anko.constraint.layout.ConstraintSetBuilder.Side.TOP
import org.jetbrains.anko.constraint.layout.applyConstraintSet
import org.mozilla.fenix.BrowserDirection
import org.mozilla.fenix.BrowsingModeManager
import org.mozilla.fenix.DefaultThemeManager
import org.mozilla.fenix.FenixViewModelProvider
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.collections.CreateCollectionViewModel
import org.mozilla.fenix.collections.SaveCollectionStep
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.ext.allowUndo
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.ext.urlToTrimmedHost
import org.mozilla.fenix.home.sessioncontrol.CollectionAction
import org.mozilla.fenix.home.sessioncontrol.Mode
import org.mozilla.fenix.home.sessioncontrol.OnboardingAction
import org.mozilla.fenix.home.sessioncontrol.SessionControlAction
import org.mozilla.fenix.home.sessioncontrol.SessionControlChange
import org.mozilla.fenix.home.sessioncontrol.SessionControlComponent
import org.mozilla.fenix.home.sessioncontrol.SessionControlState
import org.mozilla.fenix.home.sessioncontrol.SessionControlViewModel
import org.mozilla.fenix.home.sessioncontrol.Tab
import org.mozilla.fenix.home.sessioncontrol.TabAction
import org.mozilla.fenix.home.sessioncontrol.TabCollection
import org.mozilla.fenix.home.sessioncontrol.OnboardingState
import org.mozilla.fenix.lib.Do
import org.mozilla.fenix.mvi.ActionBusFactory
import org.mozilla.fenix.mvi.getAutoDisposeObservable
import org.mozilla.fenix.mvi.getManagedEmitter
import org.mozilla.fenix.onboarding.FenixOnboarding
import org.mozilla.fenix.settings.SupportUtils
import org.mozilla.fenix.utils.ItsNotBrokenSnack
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToInt

@SuppressWarnings("TooManyFunctions", "LargeClass")
class HomeFragment : Fragment(), CoroutineScope, AccountObserver {
    private val bus = ActionBusFactory.get(this)
    private var sessionObserver: SessionManager.Observer? = null
    private var tabCollectionObserver: Observer<List<TabCollection>>? = null

    private val singleSessionObserver = object : Session.Observer {
        override fun onTitleChanged(session: Session, title: String) {
            super.onTitleChanged(session, title)
            emitSessionChanges()
        }
    }

    private var homeMenu: HomeMenu? = null

    var deleteAllSessionsJob: (suspend () -> Unit)? = null
    var deleteSessionJob: (suspend () -> Unit)? = null

    private val onboarding by lazy { FenixOnboarding(requireContext()) }
    private lateinit var sessionControlComponent: SessionControlComponent

    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        postponeEnterTransition()
        sharedElementEnterTransition = TransitionInflater.from(context).inflateTransition(android.R.transition.move)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        job = Job()
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        val mode = currentMode()

        sessionControlComponent = SessionControlComponent(
            view.homeLayout,
            bus,
            FenixViewModelProvider.create(
                this,
                SessionControlViewModel::class.java
            ) {
                SessionControlViewModel(SessionControlState(listOf(), setOf(), listOf(), mode))
            }
        )

        view.homeLayout.applyConstraintSet {
            sessionControlComponent.view {
                connect(
                    TOP to BOTTOM of view.homeDivider,
                    START to START of PARENT_ID,
                    END to END of PARENT_ID,
                    BOTTOM to BOTTOM of PARENT_ID
                )
            }
        }

        postponeEnterTransition()
        val listener = object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                startPostponedEnterTransition()
                sessionControlComponent.view.viewTreeObserver.removeOnPreDrawListener(this)
                return true
            }
        }
        sessionControlComponent.view.viewTreeObserver.addOnPreDrawListener(listener)

        ActionBusFactory.get(this).logMergedObservables()
        val activity = activity as HomeActivity
        DefaultThemeManager.applyStatusBarTheme(activity.window, activity.themeManager, activity)

        return view
    }

    @SuppressWarnings("LongMethod")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupHomeMenu()

        launch(Dispatchers.Default) {
            val iconSize = resources.getDimension(R.dimen.preference_icon_drawable_size).toInt()

            val searchIcon = requireComponents.search.searchEngineManager.getDefaultSearchEngine(
                requireContext()
            ).let {
                BitmapDrawable(resources, it.icon)
            }
            searchIcon.setBounds(0, 0, iconSize, iconSize)

            runBlocking(Dispatchers.Main) {
                view.toolbar.setCompoundDrawables(searchIcon, null, null, null)
            }
        }

        view.menuButton.setOnClickListener {
            homeMenu?.menuBuilder?.build(requireContext())?.show(
                anchor = it,
                orientation = BrowserMenu.Orientation.DOWN
            )
        }
        val roundToInt =
            (toolbarPaddingDp * Resources.getSystem().displayMetrics.density).roundToInt()
        view.toolbar.compoundDrawablePadding = roundToInt
        view.toolbar.setOnClickListener {
            invokePendingDeleteJobs()
            onboarding.finish()
            val directions = HomeFragmentDirections.actionHomeFragmentToSearchFragment(null)
            Navigation.findNavController(it).navigate(directions)

            requireComponents.analytics.metrics.track(Event.SearchBarTapped(Event.SearchBarTapped.Source.HOME))
        }

        val isPrivate = (activity as HomeActivity).browsingModeManager.isPrivate

        privateBrowsingButton.contentDescription =
            contentDescriptionForPrivateBrowsingButton(isPrivate)

        privateBrowsingButton.setOnClickListener {
            val browsingModeManager = (activity as HomeActivity).browsingModeManager
            val newMode = when (browsingModeManager.mode) {
                BrowsingModeManager.Mode.Normal -> BrowsingModeManager.Mode.Private
                BrowsingModeManager.Mode.Private -> BrowsingModeManager.Mode.Normal
            }

            val mode = if (newMode == BrowsingModeManager.Mode.Private) Mode.Private else Mode.Normal
            getManagedEmitter<SessionControlChange>().onNext(SessionControlChange.ModeChange(mode))

            browsingModeManager.mode = newMode
        }

        // We need the shadow to be above the components.
        homeDividerShadow.bringToFront()
    }

    override fun onDestroyView() {
        homeMenu = null
        job.cancel()
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity).supportActionBar?.hide()

        requireComponents.backgroundServices.accountManager.register(this, owner = this)
    }

    @SuppressWarnings("ComplexMethod")
    override fun onStart() {
        super.onStart()
        if (isAdded) {
            getAutoDisposeObservable<SessionControlAction>()
                .subscribe {
                    when (it) {
                        is SessionControlAction.Tab -> handleTabAction(it.action)
                        is SessionControlAction.Collection -> handleCollectionAction(it.action)
                        is SessionControlAction.Onboarding -> handleOnboardingAction(it.action)
                        is SessionControlAction.ReloadData -> {
                            val homeViewModel = activity?.run {
                                ViewModelProviders.of(this).get(HomeScreenViewModel::class.java)
                            }
                            homeViewModel?.layoutManagerState?.also { parcelable ->
                                sessionControlComponent.view.layoutManager?.onRestoreInstanceState(parcelable)
                            }
                            val progress = homeViewModel?.motionLayoutProgress
                            homeLayout?.progress =
                                if (progress ?: 0F > MOTION_LAYOUT_PROGRESS_ROUND_POINT) 1.0f else 0f
                            homeViewModel?.layoutManagerState = null
                        }
                    }
                }
        }

        val mode = currentMode()
        getManagedEmitter<SessionControlChange>().onNext(SessionControlChange.ModeChange(mode))

        emitSessionChanges()
        sessionObserver = subscribeToSessions()
        tabCollectionObserver = subscribeToTabCollections()
    }

    override fun onStop() {
        sessionObserver?.let {
            requireComponents.core.sessionManager.unregister(it)
        }
        tabCollectionObserver?.let {
            requireComponents.core.tabCollectionStorage.getCollections().removeObserver(it)
        }

        super.onStop()
    }

    private fun handleOnboardingAction(action: OnboardingAction) {
        Do exhaustive when (action) {
            is OnboardingAction.Finish -> {
                onboarding.finish()

                val mode = currentMode()
                getManagedEmitter<SessionControlChange>().onNext(SessionControlChange.ModeChange(mode))
            }
        }
    }

    @SuppressWarnings("ComplexMethod")
    private fun handleTabAction(action: TabAction) {
        Do exhaustive when (action) {
            is TabAction.SaveTabGroup -> {
                if ((activity as HomeActivity).browsingModeManager.isPrivate) {
                    return
                }
                invokePendingDeleteJobs()
                showCollectionCreationFragment(action.selectedTabSessionId)
            }
            is TabAction.Select -> {
                invokePendingDeleteJobs()
                val session =
                    requireComponents.core.sessionManager.findSessionById(action.sessionId)
                requireComponents.core.sessionManager.select(session!!)
                val directions = HomeFragmentDirections.actionHomeFragmentToBrowserFragment(null)
                val extras =
                    FragmentNavigator.Extras.Builder()
                        .addSharedElement(action.tabView, "$TAB_ITEM_TRANSITION_NAME${action.sessionId}")
                        .build()
                Navigation.findNavController(action.tabView).navigate(directions, extras)
            }
            is TabAction.Close -> {
                if (deleteSessionJob == null) removeTabWithUndo(action.sessionId) else {
                    deleteSessionJob?.let {
                        launch {
                            it.invoke()
                        }.invokeOnCompletion {
                            deleteSessionJob = null
                            removeTabWithUndo(action.sessionId)
                        }
                    }
                }
            }
            is TabAction.Share -> {
                invokePendingDeleteJobs()
                requireComponents.core.sessionManager.findSessionById(action.sessionId)
                    ?.let { session ->
                        share(session.url)
                    }
            }
            is TabAction.CloseAll -> {
                removeAllTabsWithUndo(action.private)
            }
            is TabAction.PrivateBrowsingLearnMore -> {
                (activity as HomeActivity).openToBrowserAndLoad(
                    searchTermOrURL = SupportUtils.getGenericSumoURLForTopic
                        (SupportUtils.SumoTopic.PRIVATE_BROWSING_MYTHS),
                    newTab = true,
                    from = BrowserDirection.FromHome
                )
            }
            is TabAction.Add -> {
                invokePendingDeleteJobs()
                val directions = HomeFragmentDirections.actionHomeFragmentToSearchFragment(null)
                Navigation.findNavController(view!!).navigate(directions)
            }
            is TabAction.ShareTabs -> {
                invokePendingDeleteJobs()
                val shareText = requireComponents.core.sessionManager.sessions.joinToString("\n") {
                    it.url
                }
                share(shareText)
            }
        }
    }

    private fun invokePendingDeleteJobs() {
        deleteSessionJob?.let {
            launch {
                it.invoke()
            }.invokeOnCompletion {
                deleteSessionJob = null
            }
        }

        deleteAllSessionsJob?.let {
            launch {
                it.invoke()
            }.invokeOnCompletion {
                deleteAllSessionsJob = null
            }
        }
    }

    private fun createDeleteCollectionPrompt(tabCollection: TabCollection) {
        AlertDialog.Builder(
            ContextThemeWrapper(
                activity,
                R.style.DialogStyle
            )
        ).apply {
            val message = context.getString(R.string.tab_collection_dialog_message, tabCollection.title)
            setMessage(message)
            setNegativeButton(R.string.tab_collection_dialog_negative) { dialog: DialogInterface, _ ->
                dialog.cancel()
            }
            setPositiveButton(R.string.tab_collection_dialog_positive) { dialog: DialogInterface, _ ->
                launch(Dispatchers.IO) {
                    requireComponents.core.tabCollectionStorage.removeCollection(tabCollection)
                }.invokeOnCompletion {
                    dialog.dismiss()
                }
            }
            create()
        }.show()
    }

    @Suppress("ComplexMethod")
    private fun handleCollectionAction(action: CollectionAction) {
        when (action) {
            is CollectionAction.Expand -> {
                getManagedEmitter<SessionControlChange>()
                    .onNext(SessionControlChange.ExpansionChange(action.collection, true))
            }
            is CollectionAction.Collapse -> {
                getManagedEmitter<SessionControlChange>()
                    .onNext(SessionControlChange.ExpansionChange(action.collection, false))
            }
            is CollectionAction.Delete -> {
                createDeleteCollectionPrompt(action.collection)
            }
            is CollectionAction.AddTab -> {
                ItsNotBrokenSnack(context!!).showSnackbar(issueNumber = "1575")
            }
            is CollectionAction.Rename -> {
                showCollectionCreationFragment(
                    selectedTabCollection = action.collection,
                    step = SaveCollectionStep.RenameCollection
                )
            }
            is CollectionAction.OpenTab -> {
                invokePendingDeleteJobs()
                (activity as HomeActivity).openToBrowserAndLoad(
                    searchTermOrURL = action.tab.url,
                    newTab = true,
                    from = BrowserDirection.FromHome
                )
            }
            is CollectionAction.OpenTabs -> {
                invokePendingDeleteJobs()
                action.collection.tabs.forEach {
                    requireComponents.useCases.tabsUseCases.addTab.invoke(it.url)
                }
            }
            is CollectionAction.ShareTabs -> {
                val shareText = action.collection.tabs.joinToString("\n") {
                    it.url
                }
                share(shareText)
            }
            is CollectionAction.RemoveTab -> {
                launch(Dispatchers.IO) {
                    requireComponents.core.tabCollectionStorage.removeTabFromCollection(action.collection, action.tab)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        val homeViewModel = activity?.run {
            ViewModelProviders.of(this).get(HomeScreenViewModel::class.java)
        }
        homeViewModel?.layoutManagerState =
            sessionControlComponent.view.layoutManager?.onSaveInstanceState()
        homeViewModel?.motionLayoutProgress = homeLayout?.progress ?: 0F
        sessionObserver?.let {
            requireComponents.core.sessionManager.unregister(it)
        }
    }

    private fun setupHomeMenu() {
        homeMenu = HomeMenu(requireContext()) {
            when (it) {
                HomeMenu.Item.Settings -> {
                    invokePendingDeleteJobs()
                    onboarding.finish()
                    Navigation.findNavController(homeLayout).navigate(
                        HomeFragmentDirections.actionHomeFragmentToSettingsFragment()
                    )
                }
                HomeMenu.Item.Library -> {
                    invokePendingDeleteJobs()
                    onboarding.finish()
                    Navigation.findNavController(homeLayout).navigate(
                        HomeFragmentDirections.actionHomeFragmentToLibraryFragment()
                    )
                }
                HomeMenu.Item.Help -> {
                    invokePendingDeleteJobs()
                    (activity as HomeActivity).openToBrowserAndLoad(
                        searchTermOrURL = SupportUtils.getSumoURLForTopic(
                            context!!,
                            SupportUtils.SumoTopic.HELP
                        ),
                        newTab = true,
                        from = BrowserDirection.FromHome
                    )
                }
            }
        }
    }

    private fun contentDescriptionForPrivateBrowsingButton(isPrivate: Boolean): String {
        val resourceId =
            if (isPrivate) R.string.content_description_disable_private_browsing_button else
                R.string.content_description_private_browsing_button

        return getString(resourceId)
    }

    private fun subscribeToTabCollections(): Observer<List<TabCollection>> {
        val observer = Observer<List<TabCollection>> {
            requireComponents.core.tabCollectionStorage.cachedTabCollections = it
            getManagedEmitter<SessionControlChange>().onNext(SessionControlChange.CollectionsChange(it))
        }
        requireComponents.core.tabCollectionStorage.getCollections().observe(this, observer)
        return observer
    }

    private fun subscribeToSessions(): SessionManager.Observer {
        val observer = object : SessionManager.Observer {
            override fun onSessionAdded(session: Session) {
                super.onSessionAdded(session)
                session.register(singleSessionObserver, this@HomeFragment)
                emitSessionChanges()
            }

            override fun onSessionRemoved(session: Session) {
                super.onSessionRemoved(session)
                session.unregister(singleSessionObserver)
                emitSessionChanges()
            }

            override fun onSessionSelected(session: Session) {
                super.onSessionSelected(session)
                emitSessionChanges()
            }

            override fun onSessionsRestored() {
                super.onSessionsRestored()
                requireComponents.core.sessionManager.sessions.forEach {
                    it.register(singleSessionObserver, this@HomeFragment)
                }
                emitSessionChanges()
            }

            override fun onAllSessionsRemoved() {
                super.onAllSessionsRemoved()
                requireComponents.core.sessionManager.sessions.forEach {
                    it.unregister(singleSessionObserver)
                }
                emitSessionChanges()
            }
        }
        requireComponents.core.sessionManager.register(observer)
        return observer
    }

    private fun removeAllTabsWithUndo(isPrivate: Boolean) {
        getManagedEmitter<SessionControlChange>().onNext(SessionControlChange.TabsChange(listOf()))
        deleteAllSessionsJob = {
            requireComponents.useCases.tabsUseCases.removeAllTabsOfType.invoke(isPrivate)
        }

        CoroutineScope(Dispatchers.Main).allowUndo(
            view!!, getString(R.string.snackbar_tabs_deleted),
            getString(R.string.snackbar_deleted_undo), {
                deleteAllSessionsJob = null
                emitSessionChanges()
            }
        ) {
            requireComponents.useCases.tabsUseCases.removeAllTabsOfType.invoke(isPrivate)
        }
    }

    private fun removeTabWithUndo(sessionId: String) {
        val sessionManager = requireComponents.core.sessionManager

        // Update the UI with the tab removed, but don't remove it from storage yet
        getManagedEmitter<SessionControlChange>().onNext(
            SessionControlChange.TabsChange(
                sessionManager.sessions
                    .filter { (activity as HomeActivity).browsingModeManager.isPrivate == it.private }
                    .filter { it.id != sessionId }
                    .map {
                        val selected =
                            it == sessionManager.selectedSession
                        Tab(
                            it.id,
                            it.url,
                            it.url.urlToTrimmedHost(),
                            it.title,
                            selected,
                            it.thumbnail
                        )
                    }
            )
        )

        deleteSessionJob = {
            sessionManager.findSessionById(sessionId)
                ?.let { session ->
                    sessionManager.remove(session)
                }
        }

        CoroutineScope(Dispatchers.Main).allowUndo(
            view!!, getString(R.string.snackbar_tab_deleted),
            getString(R.string.snackbar_deleted_undo), {
                deleteSessionJob = null
                emitSessionChanges()
            }
        ) {
            sessionManager.findSessionById(sessionId)
                ?.let { session ->
                    sessionManager.remove(session)
                }
        }
    }

    private fun emitSessionChanges() {
        val sessionManager = requireComponents.core.sessionManager

        getManagedEmitter<SessionControlChange>().onNext(
            SessionControlChange.TabsChange(
                sessionManager.sessions
                    .filter { (activity as HomeActivity).browsingModeManager.isPrivate == it.private }
                    .map {
                        val selected = it == sessionManager.selectedSession
                        Tab(
                            it.id,
                            it.url,
                            it.url.urlToTrimmedHost(),
                            it.title,
                            selected,
                            it.thumbnail
                        )
                    }
            )
        )
    }

    private fun emitAccountChanges() {
        val mode = currentMode()
        getManagedEmitter<SessionControlChange>().onNext(SessionControlChange.ModeChange(mode))
    }

    private fun showCollectionCreationFragment(
        selectedTabId: String? = null,
        selectedTabCollection: TabCollection? = null,
        step: SaveCollectionStep = SaveCollectionStep.SelectTabs
    ) {
        val tabs = requireComponents.core.sessionManager.sessions.filter { !it.private }
            .map { Tab(it.id, it.url, it.url.urlToTrimmedHost(), it.title) }

        val viewModel = activity?.run {
            ViewModelProviders.of(this).get(CreateCollectionViewModel::class.java)
        }
        viewModel?.tabs = tabs
        val selectedTabs = tabs.find { tab -> tab.sessionId == selectedTabId }
        val selectedSet = if (selectedTabs == null) mutableSetOf() else mutableSetOf(selectedTabs)
        viewModel?.selectedTabs = selectedSet
        viewModel?.saveCollectionStep = step
        viewModel?.tabCollections = requireComponents.core.tabCollectionStorage.cachedTabCollections.reversed()
        viewModel?.selectedTabCollection = selectedTabCollection

        view?.let {
            val directions = HomeFragmentDirections.actionHomeFragmentToCreateCollectionFragment()
            Navigation.findNavController(it).navigate(directions)
        }
    }

    private fun share(text: String) {
        val directions = HomeFragmentDirections.actionHomeFragmentToShareFragment(text)
        Navigation.findNavController(view!!).navigate(directions)
    }

    private fun currentMode(): Mode = if (!onboarding.userHasBeenOnboarded()) {
        val account = requireComponents.backgroundServices.accountManager.authenticatedAccount()
        if (account == null) {
            Mode.Onboarding(OnboardingState.SignedOut)
        } else {
            Mode.Onboarding(OnboardingState.ManuallySignedIn)
        }
    } else if ((activity as HomeActivity).browsingModeManager.isPrivate) {
        Mode.Private
    } else {
        Mode.Normal
    }

    override fun onAuthenticated(account: OAuthAccount) { emitAccountChanges() }
    override fun onError(error: Exception) { emitAccountChanges() }
    override fun onLoggedOut() { emitAccountChanges() }
    override fun onProfileUpdated(profile: Profile) { emitAccountChanges() }

    companion object {
        private const val TAB_ITEM_TRANSITION_NAME = "tab_item"
        private const val toolbarPaddingDp = 12f
        private const val MOTION_LAYOUT_PROGRESS_ROUND_POINT = 0.25f
    }
}
