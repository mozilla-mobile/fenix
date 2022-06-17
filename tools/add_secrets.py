#!/usr/bin/env python3
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.

"""
A script to help move a local copy of Fenix secrets into their usual destinations. This can
be used to generate builds more closely mirroring release builds for performance testing.

To use this, copy the YAML file defined in TaskCluster into a local file in the source root named
.local_secrets.

For example, the nightly secrets can be found here: https://firefox-ci-tc.services.mozilla.com/secrets/project%2Fmobile%2Ffenix%2Fnightly.local_secrets
This may require requesting additional TaskCluster scopes.

This script can then be invoked using:
tools/add_secrets.py <release_type>
"""

import sys
import yaml

try:
    build_type = sys.argv[1]
except IndexError:
    print(
        """
        Expected 1 argument defined build variant. Probably 'nightly', 'beta', or 'release'.
        For example: python3 tools/add_secrets.py nightly
        """
    )
    sys.exit(2)

# These mappings are borrowed from taskcluster/transforms/build.py and should be kept in sync.
secret_filenames = [
    ("adjust", ".adjust_token"),
    (
        "firebase",
        "app/src/{}/res/values/firebase.xml".format(build_type),
    ),
    ("sentry_dsn", ".sentry_token"),
    ("mls", ".mls_token"),
    ("nimbus_url", ".nimbus"),
    ("wallpaper_url", ".wallpaper_url"),
    ("pocket_consumer_key", ".pocket_consumer_key"),
]

# Load the YAML secrets copied stored in .local_secrets, which need to be copied from the remote source.
stream = open('.local_secrets', 'r')
dict = yaml.safe_load(stream)

# Copy the secrets into the appropriate target_file
for key, target_file in secret_filenames:
    with open(target_file, 'w') as f:
       f.write(dict[key])
       f.close()
