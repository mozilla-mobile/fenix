package com.google.firebase.messaging;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class FirebaseMessagingService extends Service {

    private final IBinder mBinder = new Binder();

    public void onMessageReceived(RemoteMessage message) {
    }

    public void onMessageSent(String msgId) {
    }

    public void onNewToken(String token) {
    }

    public void onSendError(String msgId, Exception exception) {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

}
