/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.exceptions

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.extensions.LayoutContainer
import org.mozilla.fenix.databinding.ComponentExceptionsBinding

/**
 * View that contains and configures the Exceptions List
 */
abstract class ExceptionsView<T : Any>(
    container: ViewGroup,
    protected val interactor: ExceptionsInteractor<T>
) : LayoutContainer {

    private val binding = ComponentExceptionsBinding.inflate(
        LayoutInflater.from(container.context),
        container,
        true
    )

    val exceptionsList = binding.exceptionsList
    val exceptionsLearnMore = binding.exceptionsLearnMore
    val exceptionsEmptyView = binding.exceptionsEmptyView

    override val containerView: FrameLayout = binding.exceptionsWrapper

    protected abstract val exceptionsAdapter: ExceptionsAdapter<T>

    init {
        exceptionsList.apply {
            layoutManager = LinearLayoutManager(containerView.context)
        }
    }

    fun update(items: List<T>) {
        binding.exceptionsEmptyView.isVisible = items.isEmpty()
        exceptionsList.isVisible = items.isNotEmpty()
        exceptionsAdapter.updateData(items)
    }
}
