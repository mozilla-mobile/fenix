/* This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home

import android.animation.Animator
import android.content.DialogInterface
import android.content.res.Resources
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.fragment_home.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
import org.mozilla.fenix.FenixViewModelProvider
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.ThemeManager
import org.mozilla.fenix.collections.CreateCollectionViewModel
import org.mozilla.fenix.collections.SaveCollectionStep
import org.mozilla.fenix.components.FenixSnackbar
import org.mozilla.fenix.components.TabCollectionStorage
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.ext.nav
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.ext.toTab
import org.mozilla.fenix.home.sessioncontrol.CollectionAction
import org.mozilla.fenix.home.sessioncontrol.Mode
import org.mozilla.fenix.home.sessioncontrol.OnboardingAction
import org.mozilla.fenix.home.sessioncontrol.OnboardingState
import org.mozilla.fenix.home.sessioncontrol.SessionControlAction
import org.mozilla.fenix.home.sessioncontrol.SessionControlChange
import org.mozilla.fenix.home.sessioncontrol.SessionControlComponent
import org.mozilla.fenix.home.sessioncontrol.SessionControlState
import org.mozilla.fenix.home.sessioncontrol.SessionControlViewModel
import org.mozilla.fenix.home.sessioncontrol.Tab
import org.mozilla.fenix.home.sessioncontrol.TabAction
import org.mozilla.fenix.home.sessioncontrol.TabCollection
import org.mozilla.fenix.home.sessioncontrol.viewholders.CollectionViewHolder
import org.mozilla.fenix.lib.Do
import org.mozilla.fenix.mvi.ActionBusFactory
import org.mozilla.fenix.mvi.getAutoDisposeObservable
import org.mozilla.fenix.mvi.getManagedEmitter
import org.mozilla.fenix.onboarding.FenixOnboarding
import org.mozilla.fenix.settings.SupportUtils
import org.mozilla.fenix.share.ShareTab
import org.mozilla.fenix.utils.allowUndo
import kotlin.math.roundToInt

@SuppressWarnings("TooManyFunctions", "LargeClass")
class HomeFragment : Fragment(), AccountObserver {
    private val bus = ActionBusFactory.get(this)
    private var tabCollectionObserver: Observer<List<TabCollection>>? = null

    private val singleSessionObserver = object : Session.Observer {
        override fun onTitleChanged(session: Session, title: String) {
            super.onTitleChanged(session, title)
            if (deleteAllSessionsJob != null) return
            emitSessionChanges()
        }
    }

    private lateinit var sessionObserver: BrowserSessionsObserver

    private val collectionStorageObserver = object : TabCollectionStorage.Observer {
        override fun onCollectionCreated(title: String, sessions: List<Session>) {
            super.onCollectionCreated(title, sessions)
            scrollAndAnimateCollection(sessions.size)
        }

        override fun onTabsAdded(
            tabCollection: mozilla.components.feature.tab.collections.TabCollection,
            sessions: List<Session>
        ) {
            super.onTabsAdded(tabCollection, sessions)
            scrollAndAnimateCollection(sessions.size, tabCollection)
        }

        override fun onCollectionRenamed(
            tabCollection: mozilla.components.feature.tab.collections.TabCollection,
            title: String
        ) {
            super.onCollectionRenamed(tabCollection, title)
            showRenamedSnackbar()
        }
    }

    private val preDrawListener = object : ViewTreeObserver.OnPreDrawListener {
        override fun onPreDraw(): Boolean {
            viewLifecycleOwner.lifecycleScope.launch {
                delay(ANIM_SCROLL_DELAY)
                restoreLayoutState()
//                startPostponedEnterTransition()
            }.invokeOnCompletion { sessionControlComponent.view.viewTreeObserver.removeOnPreDrawListener(this) }
            return true
        }
    }

    private var homeMenu: HomeMenu? = null

    private val sessionManager: SessionManager
        get() = requireComponents.core.sessionManager

    var deleteAllSessionsJob: (suspend () -> Unit)? = null
    private var pendingSessionDeletion: PendingSessionDeletion? = null

    data class PendingSessionDeletion(val deletionJob: (suspend () -> Unit), val sessionId: String)

    private val onboarding by lazy { FenixOnboarding(requireContext()) }
    private lateinit var sessionControlComponent: SessionControlComponent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
// Disabled while awaiting a better solution to #3209
//        postponeEnterTransition()
//        sharedElementEnterTransition = TransitionInflater.from(context).inflateTransition(android.R.transition.move)
//            .setDuration(SHARED_TRANSITION_MS)

        sessionObserver = BrowserSessionsObserver(sessionManager, singleSessionObserver) {
            emitSessionChanges()
        }

        if (!onboarding.userHasBeenOnboarded()) {
            requireComponents.analytics.metrics.track(Event.OpenedAppFirstRun)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        val mode = currentMode()

        sessionControlComponent = SessionControlComponent(
            view.homeLayout,
            bus,
            FenixViewModelProvider.create(
                this,
                SessionControlViewModel::class.java
            ) {
                SessionControlViewModel(
                    SessionControlState(
                        listOf(),
                        setOf(),
                        requireComponents.core.tabCollectionStorage.cachedTabCollections,
                        mode
                    )
                )
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

//        postponeEnterTransition()

        ActionBusFactory.get(this).logMergedObservables()
        val activity = activity as HomeActivity
        ThemeManager.applyStatusBarTheme(activity.window, activity.themeManager, activity)

        sessionControlComponent.view.viewTreeObserver.addOnPreDrawListener(preDrawListener)

        return view
    }

    private fun restoreLayoutState() {
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

    @SuppressWarnings("LongMethod")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupHomeMenu()

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
            val iconSize = resources.getDimension(R.dimen.preference_icon_drawable_size).toInt()

            val searchIcon = requireComponents.search.searchEngineManager.getDefaultSearchEngine(
                requireContext()
            ).let {
                BitmapDrawable(resources, it.icon)
            }
            searchIcon.setBounds(0, 0, iconSize, iconSize)

            runBlocking(Dispatchers.Main) {
                search_engine_icon?.setImageDrawable(searchIcon)
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
// Disabled while awaiting a better solution to #3209
//            val extras =
//                FragmentNavigator.Extras.Builder()
//                    .addSharedElement(toolbar_wrapper, "toolbar_wrapper_transition")
//                    .build()
//            nav(R.id.homeFragment, directions, extras)
            nav(R.id.homeFragment, directions)
            requireComponents.analytics.metrics.track(Event.SearchBarTapped(Event.SearchBarTapped.Source.HOME))
        }

        val isPrivate = (activity as HomeActivity).browsingModeManager.isPrivate

        privateBrowsingButton.contentDescription =
            contentDescriptionForPrivateBrowsingButton(isPrivate)

        privateBrowsingButton.setOnClickListener {
            invokePendingDeleteJobs()
            val browsingModeManager = (activity as HomeActivity).browsingModeManager
            val newMode = when (browsingModeManager.mode) {
                BrowsingModeManager.Mode.Normal -> BrowsingModeManager.Mode.Private
                BrowsingModeManager.Mode.Private -> BrowsingModeManager.Mode.Normal
            }

            if (onboarding.userHasBeenOnboarded()) {
                val mode = if (newMode == BrowsingModeManager.Mode.Private) Mode.Private else Mode.Normal
                getManagedEmitter<SessionControlChange>().onNext(SessionControlChange.ModeChange(mode))
            }

            browsingModeManager.mode = newMode
        }

        // We need the shadow to be above the components.
        homeDividerShadow.bringToFront()
    }

    override fun onDestroyView() {
        homeMenu = null
        sessionControlComponent.view.viewTreeObserver.removeOnPreDrawListener(preDrawListener)
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()

        getAutoDisposeObservable<SessionControlAction>()
            .subscribe {
                when (it) {
                    is SessionControlAction.Tab -> handleTabAction(it.action)
                    is SessionControlAction.Collection -> handleCollectionAction(it.action)
                    is SessionControlAction.Onboarding -> handleOnboardingAction(it.action)
                }
            }

        getManagedEmitter<SessionControlChange>().onNext(
            SessionControlChange.Change(
                tabs = getListOfSessions().toTabs(),
                mode = currentMode(),
                collections = requireComponents.core.tabCollectionStorage.cachedTabCollections
            )
        )

        (activity as AppCompatActivity).supportActionBar?.hide()

        requireComponents.backgroundServices.accountManager.register(this, owner = this)
    }

    override fun onStart() {
        super.onStart()
        sessionObserver.onStart()
        tabCollectionObserver = subscribeToTabCollections()

        // We only want this observer live just before we navigate away to the collection creation screen
        requireComponents.core.tabCollectionStorage.unregister(collectionStorageObserver)
    }

    override fun onStop() {
        sessionObserver.onStop()
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
                val session = sessionManager.findSessionById(action.sessionId)
                sessionManager.select(session!!)
                val directions = HomeFragmentDirections.actionHomeFragmentToBrowserFragment(null)
// Disabled while awaiting a better solution to #3209
//                val extras =
//                    FragmentNavigator.Extras.Builder()
//                        .addSharedElement(action.tabView, "$TAB_ITEM_TRANSITION_NAME${action.sessionId}")
//                        .build()
//                nav(R.id.homeFragment, directions, extras)
                nav(R.id.homeFragment, directions)
            }
            is TabAction.Close -> {
                if (pendingSessionDeletion?.deletionJob == null) {
                    removeTabWithUndo(action.sessionId)
                } else {
                    pendingSessionDeletion?.deletionJob?.let {
                        viewLifecycleOwner.lifecycleScope.launch {
                            it.invoke()
                        }.invokeOnCompletion {
                            pendingSessionDeletion = null
                            removeTabWithUndo(action.sessionId)
                        }
                    }
                }
            }
            is TabAction.Share -> {
                invokePendingDeleteJobs()
                sessionManager.findSessionById(action.sessionId)?.let { session ->
                    share(session.url)
                }
            }
            is TabAction.CloseAll -> {
                if (pendingSessionDeletion?.deletionJob == null) removeAllTabsWithUndo(
                    sessionManager.filteredSessions(action.private)
                ) else {
                    pendingSessionDeletion?.deletionJob?.let {
                        viewLifecycleOwner.lifecycleScope.launch {
                            it.invoke()
                        }.invokeOnCompletion {
                            pendingSessionDeletion = null
                            removeAllTabsWithUndo(sessionManager.filteredSessions(action.private))
                        }
                    }
                }
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
                nav(R.id.homeFragment, directions)
            }
            is TabAction.ShareTabs -> {
                invokePendingDeleteJobs()
                val shareTabs = sessionManager.sessions.map {
                    ShareTab(it.url, it.title, it.id)
                }
                share(tabs = shareTabs)
            }
        }
    }

    private fun invokePendingDeleteJobs() {
        pendingSessionDeletion?.deletionJob?.let {
            viewLifecycleOwner.lifecycleScope.launch {
                it.invoke()
            }.invokeOnCompletion {
                pendingSessionDeletion = null
            }
        }

        deleteAllSessionsJob?.let {
            viewLifecycleOwner.lifecycleScope.launch {
                it.invoke()
            }.invokeOnCompletion {
                deleteAllSessionsJob = null
            }
        }
    }

    private fun createDeleteCollectionPrompt(tabCollection: TabCollection) {
        context?.let {
            AlertDialog.Builder(it).apply {
                val message = context.getString(R.string.tab_collection_dialog_message, tabCollection.title)
                setMessage(message)
                setNegativeButton(R.string.tab_collection_dialog_negative) { dialog: DialogInterface, _ ->
                    dialog.cancel()
                }
                setPositiveButton(R.string.tab_collection_dialog_positive) { dialog: DialogInterface, _ ->
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                        requireComponents.core.tabCollectionStorage.removeCollection(tabCollection)
                    }.invokeOnCompletion {
                        dialog.dismiss()
                    }
                }
                create()
            }.show()
        }
    }

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
                showCollectionCreationFragment(
                    selectedTabCollection = action.collection,
                    step = SaveCollectionStep.SelectTabs
                )
            }
            is CollectionAction.Rename -> {
                showCollectionCreationFragment(
                    selectedTabCollection = action.collection,
                    step = SaveCollectionStep.RenameCollection
                )
            }
            is CollectionAction.OpenTab -> {
                invokePendingDeleteJobs()
                val tabSnapshot = action.tab.restore(
                    context = context!!,
                    engine = requireComponents.core.engine,
                    tab = action.tab,
                    restoreSessionId = false
                )
                if (tabSnapshot.isEmpty()) {
                    // We were unable to create a snapshot, so just load the tab instead
                    (activity as HomeActivity).openToBrowserAndLoad(
                        searchTermOrURL = action.tab.url,
                        newTab = true,
                        from = BrowserDirection.FromHome
                    )
                } else {
                    requireComponents.core.sessionManager.restore(
                        tabSnapshot,
                        true
                    )
                    (activity as HomeActivity).openToBrowser(BrowserDirection.FromHome)
                }
            }
            is CollectionAction.OpenTabs -> {
                invokePendingDeleteJobs()
                action.collection.tabs.forEach {
                    val tabSnapshot = it.restore(
                        context = context!!,
                        engine = requireComponents.core.engine,
                        tab = it,
                        restoreSessionId = false
                    )
                    if (tabSnapshot.isEmpty()) {
                        // We were unable to create a snapshot, so just load the tab instead
                        requireComponents.useCases.tabsUseCases.addTab.invoke(it.url)
                    } else {
                        requireComponents.core.sessionManager.restore(
                            tabSnapshot,
                            requireComponents.core.sessionManager.selectedSession == null
                        )
                    }
                }
                viewLifecycleOwner.lifecycleScope.launch {
                    delay(ANIM_SCROLL_DELAY)
                    sessionControlComponent.view.smoothScrollToPosition(0)
                }
            }
            is CollectionAction.ShareTabs -> {
                val shareTabs = action.collection.tabs.map { ShareTab(it.url, it.title) }
                share(tabs = shareTabs)
            }
            is CollectionAction.RemoveTab -> {
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    requireComponents.core.tabCollectionStorage.removeTabFromCollection(action.collection, action.tab)
                }
            }
        }
    }

    override fun onPause() {
        invokePendingDeleteJobs()
        super.onPause()
        val homeViewModel = activity?.run {
            ViewModelProviders.of(this).get(HomeScreenViewModel::class.java)
        }
        homeViewModel?.layoutManagerState =
            sessionControlComponent.view.layoutManager?.onSaveInstanceState()
        homeViewModel?.motionLayoutProgress = homeLayout?.progress ?: 0F
    }

    private fun setupHomeMenu() {
        homeMenu = HomeMenu(requireContext()) {
            when (it) {
                HomeMenu.Item.Settings -> {
                    invokePendingDeleteJobs()
                    onboarding.finish()
                    nav(
                        R.id.homeFragment,
                        HomeFragmentDirections.actionHomeFragmentToSettingsFragment()
                    )
                }
                HomeMenu.Item.Library -> {
                    invokePendingDeleteJobs()
                    onboarding.finish()
                    nav(
                        R.id.homeFragment,
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

    private fun removeAllTabsWithUndo(listOfSessionsToDelete: List<Session>) {
        val sessionManager = requireComponents.core.sessionManager

        getManagedEmitter<SessionControlChange>().onNext(SessionControlChange.TabsChange(listOf()))

        val deleteOperation: (suspend () -> Unit) = {
            listOfSessionsToDelete.forEach {
                sessionManager.remove(it)
            }
        }
        deleteAllSessionsJob = deleteOperation

        viewLifecycleOwner.lifecycleScope.allowUndo(
            view!!,
            getString(R.string.snackbar_tabs_deleted),
            getString(R.string.snackbar_deleted_undo), {
                deleteAllSessionsJob = null
                emitSessionChanges()
            },
            operation = deleteOperation
        )
    }

    private fun removeTabWithUndo(sessionId: String) {
        val sessionManager = requireComponents.core.sessionManager
        val deleteOperation: (suspend () -> Unit) = {
            sessionManager.findSessionById(sessionId)
                ?.let { session ->
                    pendingSessionDeletion = null
                    sessionManager.remove(session)
                }
        }

        pendingSessionDeletion = PendingSessionDeletion(deleteOperation, sessionId)

        viewLifecycleOwner.lifecycleScope.allowUndo(
            view!!,
            getString(R.string.snackbar_tab_deleted),
            getString(R.string.snackbar_deleted_undo), {
                pendingSessionDeletion = null
                emitSessionChanges()
            },
            operation = deleteOperation
        )

        // Update the UI with the tab removed, but don't remove it from storage yet
        emitSessionChanges()
    }

    private fun emitSessionChanges() {
        getManagedEmitter<SessionControlChange>().onNext(
            SessionControlChange.TabsChange(
                getListOfSessions().toTabs()
            )
        )
    }

    private fun getListOfSessions(): List<Session> {
        val isPrivate = (activity as HomeActivity).browsingModeManager.isPrivate
        val notPendingDeletion: (Session) -> Boolean = { it.id != pendingSessionDeletion?.sessionId }
        return sessionManager.filteredSessions(isPrivate, notPendingDeletion)
    }

    private fun emitAccountChanges() {
        val mode = currentMode()
        getManagedEmitter<SessionControlChange>().onNext(SessionControlChange.ModeChange(mode))
    }

    private fun showCollectionCreationFragment(
        selectedTabId: String? = null,
        selectedTabCollection: TabCollection? = null,
        step: SaveCollectionStep? = null
    ) {
        if (findNavController(this).currentDestination?.id == R.id.createCollectionFragment) return

        val tabs = getListOfSessions().toTabs()

        val viewModel = activity?.run {
            ViewModelProviders.of(this).get(CreateCollectionViewModel::class.java)
        }
        viewModel?.tabs = tabs
        val selectedTabs =
            tabs.find { tab -> tab.sessionId == selectedTabId } ?: if (tabs.size == 1) tabs[0] else null
        val selectedSet = if (selectedTabs == null) mutableSetOf() else mutableSetOf(selectedTabs)
        viewModel?.selectedTabs = selectedSet
        viewModel?.tabCollections = requireComponents.core.tabCollectionStorage.cachedTabCollections.reversed()
        viewModel?.selectedTabCollection = selectedTabCollection
        viewModel?.saveCollectionStep =
            step ?: viewModel?.getStepForTabsAndCollectionSize() ?: SaveCollectionStep.SelectTabs
        viewModel?.previousFragmentId = R.id.homeFragment

        // Only register the observer right before moving to collection creation
        requireComponents.core.tabCollectionStorage.register(collectionStorageObserver, this)

        view?.let {
            val directions = HomeFragmentDirections.actionHomeFragmentToCreateCollectionFragment()
            nav(R.id.homeFragment, directions)
        }
    }

    private fun share(url: String? = null, tabs: List<ShareTab>? = null) {
        val directions =
            HomeFragmentDirections.actionHomeFragmentToShareFragment(url = url, tabs = tabs?.toTypedArray())
        nav(R.id.homeFragment, directions)
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

    override fun onAuthenticationProblems() {
        emitAccountChanges()
    }

    override fun onAuthenticated(account: OAuthAccount) {
        emitAccountChanges()
    }

    override fun onError(error: Exception) {
        emitAccountChanges()
    }

    override fun onLoggedOut() {
        emitAccountChanges()
    }

    override fun onProfileUpdated(profile: Profile) {
        emitAccountChanges()
    }

    private fun scrollAndAnimateCollection(tabsAddedToCollectionSize: Int, changedCollection: TabCollection? = null) {
        viewLifecycleOwner.lifecycleScope.launch {
            val recyclerView = sessionControlComponent.view
            delay(ANIM_SCROLL_DELAY)
            val tabsSize = getListOfSessions().size

            var indexOfCollection = tabsSize + NON_TAB_ITEM_NUM
            changedCollection?.let { changedCollection ->
                requireComponents.core.tabCollectionStorage.cachedTabCollections.filterIndexed { index, tabCollection ->
                    if (tabCollection.id == changedCollection.id) {
                        indexOfCollection = tabsSize + NON_TAB_ITEM_NUM + index
                        return@filterIndexed true
                    }
                    false
                }
            }
            val lastVisiblePosition =
                (recyclerView.layoutManager as? LinearLayoutManager)?.findLastCompletelyVisibleItemPosition() ?: 0
            if (lastVisiblePosition < indexOfCollection) {
                val onScrollListener = object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        super.onScrollStateChanged(recyclerView, newState)
                        if (newState == SCROLL_STATE_IDLE) {
                            animateCollection(tabsAddedToCollectionSize, indexOfCollection)
                            recyclerView.removeOnScrollListener(this)
                        }
                    }
                }
                recyclerView.addOnScrollListener(onScrollListener)
                recyclerView.smoothScrollToPosition(indexOfCollection)
            } else {
                animateCollection(tabsAddedToCollectionSize, indexOfCollection)
            }
        }
    }

    private fun animateCollection(addedTabsSize: Int, indexOfCollection: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            val viewHolder = sessionControlComponent.view.findViewHolderForAdapterPosition(indexOfCollection)
            val border = (viewHolder as? CollectionViewHolder)?.view?.findViewById<View>(R.id.selected_border)
            val listener = object : Animator.AnimatorListener {
                override fun onAnimationCancel(animation: Animator?) {
                    border?.visibility = View.GONE
                }

                override fun onAnimationStart(animation: Animator?) {}
                override fun onAnimationRepeat(animation: Animator?) {}
                override fun onAnimationEnd(animation: Animator?) {
                    border?.animate()?.alpha(0.0F)?.setStartDelay(ANIM_ON_SCREEN_DELAY)
                        ?.setDuration(FADE_ANIM_DURATION)
                        ?.start()
                }
            }
            border?.animate()?.alpha(1.0F)?.setStartDelay(ANIM_ON_SCREEN_DELAY)?.setDuration(FADE_ANIM_DURATION)
                ?.setListener(listener)?.start()
        }.invokeOnCompletion {
            showSavedSnackbar(addedTabsSize)
        }
    }

    private fun showSavedSnackbar(tabSize: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            delay(ANIM_SNACKBAR_DELAY)
            view?.let { view ->
                @StringRes
                val stringRes = if (tabSize > 1) {
                    R.string.create_collection_tabs_saved
                } else {
                    R.string.create_collection_tab_saved
                }
                FenixSnackbar.make(view, Snackbar.LENGTH_LONG).setText(view.context.getString(stringRes)).show()
            }
        }
    }

    private fun showRenamedSnackbar() {
        view?.let { view ->
            val string = view.context.getString(R.string.snackbar_collection_renamed)
            FenixSnackbar.make(view, Snackbar.LENGTH_LONG).setText(string).show()
        }
    }

    private fun SessionManager.filteredSessions(
        private: Boolean,
        sessionFilter: ((Session) -> Boolean)? = null
    ): List<Session> {
        return this.sessions
            .filter { private == it.private }
            .filter { sessionFilter?.invoke(it) ?: true }
    }

    private fun List<Session>.toTabs(): List<Tab> {
        val selected = sessionManager.selectedSession
        return this.map { it.toTab(requireContext(), it == selected) }
    }

    companion object {
        private const val NON_TAB_ITEM_NUM = 3
        private const val ANIM_SCROLL_DELAY = 100L
        private const val ANIM_ON_SCREEN_DELAY = 200L
        private const val FADE_ANIM_DURATION = 150L
        private const val ANIM_SNACKBAR_DELAY = 100L
        private const val SHARED_TRANSITION_MS = 200L
        private const val TAB_ITEM_TRANSITION_NAME = "tab_item"
        private const val toolbarPaddingDp = 12f
        private const val MOTION_LAYOUT_PROGRESS_ROUND_POINT = 0.25f
    }
}

/**
 * Wrapper around sessions manager to observe changes in sessions.
 * Similar to [mozilla.components.browser.session.utils.AllSessionsObserver] but ignores CustomTab sessions.
 *
 * Call [onStart] to start receiving updates into [onChanged] callback.
 * Call [onStop] to stop receiving updates.
 *
 * @param manager [SessionManager] instance to subscribe to.
 * @param observer [Session.Observer] instance that will recieve updates.
 * @param onChanged callback that will be called when any of [SessionManager.Observer]'s events are fired.
 */
private class BrowserSessionsObserver(
    private val manager: SessionManager,
    private val observer: Session.Observer,
    private val onChanged: () -> Unit
) {

    // TODO This is workaround. Should be removed when [mozilla.components.support.base.observer.ObserverRegistry]
    // will not allow to subscribe to single session more than once.
    private val observedSessions = mutableSetOf<Session>()

    /**
     * Start observing
     */
    fun onStart() {
        manager.register(managerObserver)
        subscribeToAll()
    }

    /**
     * Stop observing (will not receive updates till next [onStop] call)
     */
    fun onStop() {
        manager.unregister(managerObserver)
        unsubscribeFromAll()
    }

    private fun subscribeToAll() {
        manager.sessions.forEach(::subscribeTo)
    }

    private fun unsubscribeFromAll() {
        manager.sessions.forEach(::unsubscribeFrom)
    }

    private fun subscribeTo(session: Session) {
        if (!observedSessions.contains(session)) {
            session.register(observer)
            observedSessions += session
        }
    }

    private fun unsubscribeFrom(session: Session) {
        if (observedSessions.contains(session)) {
            session.unregister(observer)
            observedSessions -= session
        }
    }

    private val managerObserver = object : SessionManager.Observer {

        override fun onSessionAdded(session: Session) {
            subscribeTo(session)
            onChanged()
        }

        override fun onSessionsRestored() {
            subscribeToAll()
            onChanged()
        }

        override fun onAllSessionsRemoved() {
            unsubscribeFromAll()
            onChanged()
        }

        override fun onSessionRemoved(session: Session) {
            unsubscribeFrom(session)
            onChanged()
        }

        override fun onSessionSelected(session: Session) {
            onChanged()
        }
    }
}
