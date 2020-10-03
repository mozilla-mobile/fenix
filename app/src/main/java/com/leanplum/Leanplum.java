/*
 * Copyright 2016, Leanplum, Inc. All rights reserved.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.leanplum;

import android.content.Context;
import com.leanplum.callbacks.StartCallback;
import java.util.Map;

public class Leanplum {
  public static void setAppIdForDevelopmentMode(String appId, String accessKey) {
  }

  public static void setAppIdForProductionMode(String appId, String accessKey) {
  }

  public static void setApplicationContext(Context context) {
  }

  public static void setDeviceId(String deviceId) {
  }

  public static void setIsTestModeEnabled(boolean isTestModeEnabled) {
  }

  public static void setUserAttributes(Map<String, ?> userAttributes) {
  }

  public static void start(Context context) {
  }

  public static void start(Context context, StartCallback callback) {
  }

  public static void start(Context context, Map<String, ?> userAttributes) {
  }

  public static void start(Context context, String userId) {
  }

  public static void start(Context context, String userId, StartCallback callback) {
  }

  public static void start(Context context, String userId, Map<String, ?> userAttributes) {
  }

  public static synchronized void start(final Context context, String userId, Map<String, ?> attributes, StartCallback response) {
  }

  static synchronized void start(final Context context, final String userId, final Map<String, ?> attributes, StartCallback response, final Boolean isBackground) {
  }

  public static void track(final String event, double value, String info, Map<String, ?> params) {
  }

  public static void track(String event) {
  }

  public static void track(String event, double value) {
  }

  public static void track(String event, String info) {
  }

  public static void track(String event, Map<String, ?> params) {
  }

  public static void track(String event, double value, Map<String, ?> params) {
  }

  public static void track(String event, double value, String info) {
  }

  public static String getDeviceId() { return "stub"; }

  public static String getUserId() { return "stub"; }
}
