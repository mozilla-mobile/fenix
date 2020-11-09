#!/usr/bin/env bash
# Install the Android SDK and all the parts Gradle doesn't figure out to install itself

# Install the SDK
mkdir -p $HOME/android-sdk/android-sdk-linux
pushd $HOME/android-sdk/android-sdk-linux
mkdir -p licenses
echo "8933bad161af4178b1185d1a37fbf41ea5269c55" >> licenses/android-sdk-license
echo "d56f5187479451eabf01fb78af6dfcb131a6481e" >> licenses/android-sdk-license
echo "24333f8a63b6825ea9c5514f83c2829b004d1fee" >> licenses/android-sdk-license
if [ ! -e cmdline-tools ] ; then
    mkdir -p cmdline-tools
    pushd cmdline-tools
    wget --quiet "$(curl -s https://developer.android.com/studio | grep -oP "https://dl.google.com/android/repository/commandlinetools-linux-[0-9]+_latest.zip")"
    unzip commandlinetools-linux-*_latest.zip
    mv cmdline-tools tools
    popd
fi
popd
export ANDROID_SDK_ROOT=$HOME/android-sdk/android-sdk-linux

# Install the weirdly missing NDK
${ANDROID_SDK_ROOT}/cmdline-tools/tools/bin/sdkmanager "ndk;21.0.6113669"

# Point the build at the tools
echo "sdk.dir=${ANDROID_SDK_ROOT}" >> local.properties
