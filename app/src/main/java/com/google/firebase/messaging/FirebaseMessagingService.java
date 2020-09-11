// Copyright 2020 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

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
