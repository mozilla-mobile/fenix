/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.google.android.play.core.review;

import android.content.Context;
import com.google.android.play.core.review.ReviewManager;


public class ReviewManagerFactory {

    public static ReviewManager create(Context context) {
        return new ReviewManager();
    }

}
