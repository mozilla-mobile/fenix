/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.google.android.gms.ads.identifier;

import android.content.Context;


public class AdvertisingIdClient {

    public static final class Info {

        private String mId;

        public Info() {
            mId = "";
        }

        public Info(String id, Boolean ignored) {
            // We need to preserve the passed ID to pass Mozilla's tests.
            mId = id;
        }

        public String getId() {
            return mId;
        }
        
        public String toString() {
            return mId;
        }

    }

    public static Info getAdvertisingIdInfo(Context context) {
        return new Info();
    }

}
