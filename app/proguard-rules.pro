-dontobfuscate

####################################################################################################
# Sentry
####################################################################################################

# Recommended config via https://docs.sentry.io/clients/java/modules/android/#manual-integration
# Since we don't obfuscate, we don't need to use their Gradle plugin to upload ProGuard mappings.
-keepattributes LineNumberTable,SourceFile
-dontwarn org.slf4j.**
-dontwarn javax.**

# Our addition: this class is saved to disk via Serializable, which ProGuard doesn't like.
# If we exclude this, upload silently fails (Sentry swallows a NPE so we don't crash).
# I filed https://github.com/getsentry/sentry-java/issues/572
#
# If Sentry ever mysteriously stops working after we upgrade it, this could be why.
-keep class io.sentry.event.Event { *; }

####################################################################################################
# Android and GeckoView built-ins
####################################################################################################

-dontwarn android.**
-dontwarn androidx.**
-dontwarn com.google.**
-dontwarn org.mozilla.geckoview.**

####################################################################################################
# Kotlinx
####################################################################################################

-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

####################################################################################################
# Mozilla Application Services
####################################################################################################

-keep class mozilla.appservices.FenixMegazord  { *; }

####################################################################################################
# Adjust
####################################################################################################

-keep public class com.adjust.sdk.** { *; }
-keep class com.google.android.gms.common.ConnectionResult {
    int SUCCESS;
}
-keep class com.google.android.gms.ads.identifier.AdvertisingIdClient {
    com.google.android.gms.ads.identifier.AdvertisingIdClient$Info getAdvertisingIdInfo(android.content.Context);
}
-keep class com.google.android.gms.ads.identifier.AdvertisingIdClient$Info {
    java.lang.String getId();
    boolean isLimitAdTrackingEnabled();
}
-keep public class com.android.installreferrer.** { *; }
-keep class dalvik.system.VMRuntime {
    java.lang.String getRuntime();
}
-keep class android.os.Build {
    java.lang.String[] SUPPORTED_ABIS;
    java.lang.String CPU_ABI;
}
-keep class android.content.res.Configuration {
    android.os.LocaledList getLocales();
    java.util.Locale locale;
}
-keep class android.os.LocaleList {
    java.util.Locale get(int);
}

# Keep code generated from Glean Metrics
-keep class org.mozilla.fenix.GleanMetrics.** {  *; }

# Keep methods that are called by MotionLayout
-keep class org.mozilla.fenix.home.SearchView { *; }
