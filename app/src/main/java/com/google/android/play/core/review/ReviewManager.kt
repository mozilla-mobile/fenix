/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package com.google.android.play.core.review
class ReviewManager {

    class FakeReviewFlowTaskResult {
        val isSuccessful: Boolean = false
        val result: Any = false
    }
    class FakeReviewFlowTask {
        @Suppress("UNUSED_PARAMETER", "UNUSED_EXPRESSION")
        fun addOnCompleteListener(ignored: (FakeReviewFlowTaskResult) -> Unit) {
            1
        }
    }
    fun requestReviewFlow(): FakeReviewFlowTask {
        return FakeReviewFlowTask()
    }
    @Suppress("UNUSED_PARAMETER", "UNUSED_EXPRESSION")
    fun launchReviewFlow(ignored1: Any, ignored2: Any) {
        1
    }
}
