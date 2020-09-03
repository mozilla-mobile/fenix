package com.adjust.sdk;

import android.content.Context;

import java.util.List;

public class AdjustConfig {
    public static final String ENVIRONMENT_SANDBOX = "sandbox";
    public static final String ENVIRONMENT_PRODUCTION = "production";

    public AdjustConfig(Context context, String appToken, String environment) {
    }

    public AdjustConfig(Context context, String appToken, String environment, boolean allowSuppressLogLevel) {
    }

    public void setOnAttributionChangedListener(OnAttributionChangedListener onAttributionChangedListener) {
    }

    public void setLogLevel(LogLevel logLevel) {
    }
}
