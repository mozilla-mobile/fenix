/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

private object Versions {
    const val kotlin = "1.3.11"
    const val android_gradle_plugin = "3.2.1"
    const val geckoNightly = "66.0.20190128092811"
    const val rxAndroid = "2.1.0"
    const val rxKotlin = "2.3.0"
    const val anko = "0.10.8"

    const val androidx_appcompat = "1.0.2"
    const val androidx_constraint_layout = "2.0.0-alpha3"

    const val mozilla_android_components = "0.41.0-SNAPSHOT"

    const val junit = "4.12"
    const val test_tools = "1.0.2"
    const val espresso_core = "2.2.2"

    const val androidx_legacy = "1.0.0"
    const val android_arch_navigation = "1.0.0-alpha11"
}

@Suppress("unused")
object Deps {
    const val tools_androidgradle = "com.android.tools.build:gradle:${Versions.android_gradle_plugin}"
    const val tools_kotlingradle = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
    const val kotlin_stdlib = "org.jetbrains.kotlin:kotlin-stdlib-jdk7:${Versions.kotlin}"

    const val rxKotlin = "io.reactivex.rxjava2:rxkotlin:${Versions.rxKotlin}"
    const val rxAndroid = "io.reactivex.rxjava2:rxandroid:${Versions.rxAndroid}"

    const val anko_commons = "org.jetbrains.anko:anko-commons:${Versions.anko}"
    const val anko_sdk = "org.jetbrains.anko:anko-sdk25:${Versions.anko}"
    const val anko_appcompat = "org.jetbrains.anko:anko-appcompat-v7:${Versions.anko}"
    const val anko_constraintlayout = "org.jetbrains.anko:anko-constraint-layout:${Versions.anko}"

    const val geckoview_nightly_arm = "org.mozilla.geckoview:geckoview-nightly-armeabi-v7a:${Versions.geckoNightly}"
    const val geckoview_nightly_x86 = "org.mozilla.geckoview:geckoview-nightly-x86:${Versions.geckoNightly}"

    const val androidx_appcompat = "androidx.appcompat:appcompat:${Versions.androidx_appcompat}"
    const val androidx_constraintlayout = "androidx.constraintlayout:constraintlayout:${Versions.androidx_constraint_layout}"

    const val mozilla_concept_engine = "org.mozilla.components:concept-engine:${Versions.mozilla_android_components}"
    const val mozilla_concept_tabstray = "org.mozilla.components:concept-tabstray:${Versions.mozilla_android_components}"
    const val mozilla_concept_toolbar = "org.mozilla.components:concept-toolbar:${Versions.mozilla_android_components}"
    const val mozilla_concept_storage = "org.mozilla.components:concept-storage:${Versions.mozilla_android_components}"

    const val mozilla_browser_awesomebar = "org.mozilla.components:browser-awesomebar:${Versions.mozilla_android_components}"
    const val mozilla_browser_engine_gecko_nightly = "org.mozilla.components:browser-engine-gecko-nightly:${Versions.mozilla_android_components}"
    const val mozilla_browser_domains = "org.mozilla.components:browser-domains:${Versions.mozilla_android_components}"
    const val mozilla_browser_search = "org.mozilla.components:browser-search:${Versions.mozilla_android_components}"
    const val mozilla_browser_session = "org.mozilla.components:browser-session:${Versions.mozilla_android_components}"
    const val mozilla_browser_tabstray = "org.mozilla.components:browser-tabstray:${Versions.mozilla_android_components}"
    const val mozilla_browser_toolbar = "org.mozilla.components:browser-toolbar:${Versions.mozilla_android_components}"
    const val mozilla_browser_menu = "org.mozilla.components:browser-menu:${Versions.mozilla_android_components}"
    const val mozilla_browser_errorpages = "org.mozilla.components:browser-errorpages:${Versions.mozilla_android_components}"
    const val mozilla_browser_storage_sync = "org.mozilla.components:browser-storage-sync:${Versions.mozilla_android_components}"

    const val mozilla_feature_awesomebar = "org.mozilla.components:feature-awesomebar:${Versions.mozilla_android_components}"
    const val mozilla_feature_contextmenu = "org.mozilla.components:feature-contextmenu:${Versions.mozilla_android_components}"
    const val mozilla_feature_customtabs = "org.mozilla.components:feature-customtabs:${Versions.mozilla_android_components}"
    const val mozilla_feature_intent = "org.mozilla.components:feature-intent:${Versions.mozilla_android_components}"
    const val mozilla_feature_search = "org.mozilla.components:feature-search:${Versions.mozilla_android_components}"
    const val mozilla_feature_session = "org.mozilla.components:feature-session:${Versions.mozilla_android_components}"
    const val mozilla_feature_sync = "org.mozilla.components:feature-sync:${Versions.mozilla_android_components}"
    const val mozilla_feature_tabs = "org.mozilla.components:feature-tabs:${Versions.mozilla_android_components}"
    const val mozilla_feature_downloads = "org.mozilla.components:feature-downloads:${Versions.mozilla_android_components}"
    const val mozilla_feature_storage = "org.mozilla.components:feature-storage:${Versions.mozilla_android_components}"
    const val mozilla_feature_prompts = "org.mozilla.components:feature-prompts:${Versions.mozilla_android_components}"
    const val mozilla_feature_toolbar = "org.mozilla.components:feature-toolbar:${Versions.mozilla_android_components}"

    const val mozilla_support_ktx = "org.mozilla.components:support-ktx:${Versions.mozilla_android_components}"

    const val junit = "junit:junit:${Versions.junit}"
    const val tools_test_runner = "com.android.support.test:runner:${Versions.test_tools}"
    const val tools_espresso_core = "com.android.support.test.espresso:espresso-core:${Versions.espresso_core}"

    const val androidx_legacy = "androidx.legacy:legacy-support-v4:${Versions.androidx_legacy}"
    const val android_arch_navigation = "android.arch.navigation:navigation-fragment:${Versions.android_arch_navigation}"
}

