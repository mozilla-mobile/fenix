#!/usr/bin/env bash
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

# This script does the following:
# 1. Retrieves gcloud service account token
# 2. Activates gcloud service account
# 3. Connects to google Firebase (using TestArmada's Flank tool)
# 4. Executes UI tests
# 5. Puts test artifacts into the test_artifacts folder

# NOTE:
# Flank supports sharding across multiple devices at a time, but gcloud API
# only supports 1 defined APK per test run.


# If a command fails then do not proceed and fail this script too.
set -e

#########################
# The command line help #
#########################
display_help() {
    echo "Usage: $0 Build_Variant [Number_Shards...]"
    echo
    echo "Examples:"
    echo "To run UI tests on ARM device shard (1 test / shard)"
    echo "$ ui-test.sh arm -1"
    echo
    echo "To run UI tests on X86 device (on 3 shards)"
    echo "$ ui-test.sh feature x86 3"
    echo
}

# Basic parameter check
if [[ $# -lt 1 ]]; then
    echo "Error: please provide at least one build variant (arm|x86)"
    display_help
    exit 1
fi

device_type="$1"  # arm | x86
if [[ ! -z "$2" ]]; then
    num_shards=$2
fi

JAVA_BIN="/usr/bin/java"
PATH_TEST="./automation/taskcluster/androidTest"
FLANK_BIN="/build/test-tools/flank.jar"
FLANK_CONF_ARM="${PATH_TEST}/flank-arm.yml"
FLANK_CONF_X86="${PATH_TEST}/flank-x86.yml"

echo
echo "RETRIEVE SERVICE ACCT TOKEN"
echo
python automation/taskcluster/helper/get-secret.py --json -s project/mobile/fenix/firebase -k firebaseToken  -f $GOOGLE_APPLICATION_CREDENTIALS
echo
echo

echo
echo "ACTIVATE SERVICE ACCT"
echo
# this is where the Google Testcloud project ID is set
gcloud config set project "$GOOGLE_PROJECT" 
echo

gcloud auth activate-service-account --key-file "$GOOGLE_APPLICATION_CREDENTIALS" 
echo
echo

# From now on disable exiting on error. If the tests fail we want to continue
# and try to download the artifacts. We will exit with the actual error code later.
set +e

if [[ "${device_type,,}" == "x86" ]]
then
    deviceType="X86"
    flank_template="$FLANK_CONF_X86"
else
    deviceType="Arm"
    flank_template="$FLANK_CONF_ARM"
fi

APK_APP="./app/build/outputs/apk/${deviceType,,}/debug/app-${deviceType,,}-debug.apk"
APK_TEST="./app/build/outputs/apk/androidTest/${deviceType,,}/debug/app-${deviceType,,}-debug-androidTest.apk"


# function to exit script with exit code from test run.
# (Only 0 if all test executions passed)
function failure_check() {
    if [[ $exitcode -ne 0 ]]; then
        echo
        echo
        echo "ERROR: UI test run failed, please check above URL"
    fi
    exit $exitcode
}

echo
echo "EXECUTE TEST(S)"
echo
$JAVA_BIN -jar $FLANK_BIN android run --config=$flank_template --max-test-shards=$num_shards --app=$APK_APP --test=$APK_TEST --project=$GOOGLE_PROJECT
exitcode=$?
failure_check
echo
echo

echo
echo "COPY ARTIFACTS"
echo
cp -r ./results ./test_artifacts
exitcode=$?
failure_check
echo
echo

echo
echo "RESULTS"
echo
ls -la ./results
echo 
echo

echo "All UI test(s) have passed!"
echo
echo



