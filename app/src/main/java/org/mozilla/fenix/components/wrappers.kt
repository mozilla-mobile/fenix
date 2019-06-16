/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components

import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.feature.tabs.TabsUseCases
import mozilla.components.lib.crash.Crash
import mozilla.components.lib.crash.CrashReporter

/*
 * TODO this interfaces/classes are used for gentle transition to IoC implementation
 */

interface WrappedSessionUseCases {

    val crashRecovery: WrappedCrashRecoveryUseCase
}

class RealWrappedSessionUseCases(delegate: SessionUseCases) :
    WrappedSessionUseCases {

    override val crashRecovery = RealWrappedCrashRecoveryUseCase(delegate.crashRecovery)
}

interface WrappedCrashRecoveryUseCase {

    fun invoke(): Boolean
}

class RealWrappedCrashRecoveryUseCase(private val delegate: SessionUseCases.CrashRecoveryUseCase) :
    WrappedCrashRecoveryUseCase {

    override fun invoke() = delegate.invoke()
}

interface WrappedTabsUseCases {

    val removeTab: WrappedRemoveTabUseCase
}

class RealWrappedTabsUseCases(delegate: TabsUseCases) :
    WrappedTabsUseCases {

    override val removeTab = RealWrappedRemoveTabUseCase(delegate.removeTab)
}

interface WrappedRemoveTabUseCase {

    fun invoke(session: Session)
}

class RealWrappedRemoveTabUseCase(private val delegate: TabsUseCases.RemoveTabUseCase) :
    WrappedRemoveTabUseCase {

    override fun invoke(session: Session) = delegate.invoke(session)
}

interface WrappedCrashReporter {

    fun submitReport(crash: Crash)
}

class RealWrappedCrashReporter(private val delegate: CrashReporter) :
    WrappedCrashReporter {

    override fun submitReport(crash: Crash) = delegate.submitReport(crash)
}

interface WrappedSessionManager {

    val selectedSession: Session?
}

class RealWrappedSessionManager(private val delegate: SessionManager) :
    WrappedSessionManager {

    override val selectedSession get() = delegate.selectedSession
}
