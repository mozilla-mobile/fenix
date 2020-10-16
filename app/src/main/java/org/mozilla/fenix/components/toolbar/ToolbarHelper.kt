/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.toolbar

class ToolbarHelper {
    companion object {
        const val MAX_VISIBLE_TABS = 99

        const val SO_MANY_TABS_OPEN = "∞"

        const val INFINITE_CHAR_PADDING_BOTTOM = 6

        const val ONE_DIGIT_SIZE_RATIO = 0.5f
        const val TWO_DIGITS_SIZE_RATIO = 0.4f
        const val TWO_DIGITS_TAB_COUNT_THRESHOLD = 10

        // createBoxAnimatorSet
        const val ANIM_BOX_FADEOUT_FROM = 1.0f
        const val ANIM_BOX_FADEOUT_TO = 0.0f
        const val ANIM_BOX_FADEOUT_DURATION = 33L

        const val ANIM_BOX_MOVEUP1_FROM = 0.0f
        const val ANIM_BOX_MOVEUP1_TO = -5.3f
        const val ANIM_BOX_MOVEUP1_DURATION = 50L

        const val ANIM_BOX_MOVEDOWN2_FROM = -5.3f
        const val ANIM_BOX_MOVEDOWN2_TO = -1.0f
        const val ANIM_BOX_MOVEDOWN2_DURATION = 167L

        const val ANIM_BOX_FADEIN_FROM = 0.01f
        const val ANIM_BOX_FADEIN_TO = 1.0f
        const val ANIM_BOX_FADEIN_DURATION = 66L
        const val ANIM_BOX_MOVEDOWN3_FROM = -1.0f
        const val ANIM_BOX_MOVEDOWN3_TO = 2.7f
        const val ANIM_BOX_MOVEDOWN3_DURATION = 116L

        const val ANIM_BOX_MOVEDOWN4_FROM = 2.7f
        const val ANIM_BOX_MOVEDOWN4_TO = 0.0f
        const val ANIM_BOX_MOVEDOWN4_DURATION = 133L

        const val ANIM_BOX_SCALEUP1_FROM = 0.02f
        const val ANIM_BOX_SCALEUP1_TO = 1.05f
        const val ANIM_BOX_SCALEUP1_DURATION = 100L
        const val ANIM_BOX_SCALEUP1_DELAY = 16L

        const val ANIM_BOX_SCALEDOWN2_FROM = 1.05f
        const val ANIM_BOX_SCALEDOWN2_TO = 0.99f
        const val ANIM_BOX_SCALEDOWN2_DURATION = 116L

        const val ANIM_BOX_SCALEUP3_FROM = 0.99f
        const val ANIM_BOX_SCALEUP3_TO = 1.00f
        const val ANIM_BOX_SCALEUP3_DURATION = 133L

        // createTextAnimatorSet
        const val ANIM_TEXT_FADEOUT_FROM = 1.0f
        const val ANIM_TEXT_FADEOUT_TO = 0.0f
        const val ANIM_TEXT_FADEOUT_DURATION = 33L

        const val ANIM_TEXT_FADEIN_FROM = 0.01f
        const val ANIM_TEXT_FADEIN_TO = 1.0f
        const val ANIM_TEXT_FADEIN_DURATION = 66L
        const val ANIM_TEXT_FADEIN_DELAY = 16L * 6

        const val ANIM_TEXT_MOVEDOWN_FROM = 0.0f
        const val ANIM_TEXT_MOVEDOWN_TO = 4.4f
        const val ANIM_TEXT_MOVEDOWN_DURATION = 66L
        const val ANIM_TEXT_MOVEDOWN_DELAY = 16L * 6

        const val ANIM_TEXT_MOVEUP_FROM = 4.4f
        const val ANIM_TEXT_MOVEUP_DURATION = 66L
        const val ANIM_TEXT_MOVEUP_TO = 0.0f
    }
}
