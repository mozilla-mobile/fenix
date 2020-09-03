package com.google.android.gms.ads.identifier;

import android.content.Context;

public class AdvertisingIdClient {

    public static final class Info {

        public String getId() {
            return "";
        }

    }

    public static Info getAdvertisingIdInfo(Context context) {
        return new Info();
    }

}
