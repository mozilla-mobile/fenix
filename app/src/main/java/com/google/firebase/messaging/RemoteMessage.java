package com.google.firebase.messaging;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Map;

public class RemoteMessage implements Parcelable {

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
    }

    public Map<String, String> getData() {
        return null;
    }

}
