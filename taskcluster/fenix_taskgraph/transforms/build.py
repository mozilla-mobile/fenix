# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
"""
Apply some defaults and minor modifications to the jobs defined in the build
kind.
"""

from __future__ import absolute_import, print_function, unicode_literals

import datetime

from taskgraph.transforms.base import TransformSequence
from fenix_taskgraph.gradle import get_variant
from fenix_taskgraph.util import upper_case_first_letter


transforms = TransformSequence()


@transforms.add
def add_variant_config(config, tasks):
    for task in tasks:
        attributes = task.setdefault("attributes", {})
        if not attributes.get("build-type"):
            attributes["build-type"] = task["name"]

        yield task


@transforms.add
def add_shippable_secrets(config, tasks):
    for task in tasks:
        secrets = task["run"].setdefault("secrets", [])

        if task.pop("include-shippable-secrets", False) and config.params["level"] == "3":
            build_type = task["attributes"]["build-type"]
            gradle_build_type = task["run"]["gradle-build-type"]
            secret_index = 'project/mobile/fenix/{}'.format(build_type)
            secrets.extend([{
                "key": key,
                "name": secret_index,
                "path": target_file,
            } for key, target_file in (
                ('adjust', '.adjust_token'),
                ('firebase', 'app/src/{}/res/values/firebase.xml'.format(gradle_build_type)),
                ('digital_asset_links', '.digital_asset_links_token'),
                ('leanplum', '.leanplum_token'),
                ('sentry_dsn', '.sentry_token'),
            )])
        else:
            task["run"]["pre-gradlew"] = [[
                "echo", '"{}"'.format(fake_value), ">", target_file
            ] for fake_value, target_file in (
                ("--", ".adjust_token"),
                ("", ".digital_asset_links_token"),
                ("-:-", ".leanplum_token"),
                ("https://fake@sentry.prod.mozaws.net/368", ".sentry_token"),
            )]

        yield task


@transforms.add
def build_gradle_command(config, tasks):
    for task in tasks:
        gradle_build_type = task["run"]["gradle-build-type"]
        geckoview_engine = task["run"]["geckoview-engine"]
        variant_config = get_variant(gradle_build_type, geckoview_engine)

        task["run"]["gradlew"] = [
            "clean",
            "assemble{}".format(upper_case_first_letter(variant_config["name"]))
        ]

        yield task


@transforms.add
def add_nightly_version(config, tasks):
    push_date_string = config.params["moz_build_date"]
    push_date_time = datetime.datetime.strptime(push_date_string, "%Y%m%d%H%M%S")
    formated_date_time = 'Nightly {}'.format(push_date_time.strftime('%y%m%d %H:%M'))

    for task in tasks:
        if task.pop("include-nightly-version", False):
            task["run"]["gradlew"].append('-PversionName={}'.format(formated_date_time))
        yield task


@transforms.add
def add_release_version(config, tasks):
    for task in tasks:
        if task.pop("include-release-version", False):
            task["run"]["gradlew"].append(
                '-PversionName={}'.format(config.params["release_version"])
            )
        yield task


@transforms.add
def add_artifacts(config, tasks):
    for task in tasks:
        gradle_build_type = task["run"].pop("gradle-build-type")
        geckoview_engine = task["run"].pop("geckoview-engine")
        variant_config = get_variant(gradle_build_type, geckoview_engine)
        artifacts = task.setdefault("worker", {}).setdefault("artifacts", [])
        task["attributes"]["apks"] = apks = {}

        if "apk-artifact-template" in task:
            artifact_template = task.pop("apk-artifact-template")
            for apk in variant_config["apks"]:
                apk_name = artifact_template["name"].format(
                    geckoview_engine=geckoview_engine, **apk
                )
                artifacts.append({
                    "type": artifact_template["type"],
                    "name": apk_name,
                    "path": artifact_template["path"].format(
                        geckoview_engine=geckoview_engine,
                        gradle_build_type=gradle_build_type,
                        **apk
                    ),
                })
                apks[apk["abi"]] = apk_name

        yield task
