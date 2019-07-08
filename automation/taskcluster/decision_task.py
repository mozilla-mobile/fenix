# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

"""
Decision task for nightly releases.
"""

from __future__ import print_function

import argparse
import datetime
import os
import re

import taskcluster

from lib.gradle import get_variants_for_build_type, get_geckoview_versions
from lib.tasks import (
    fetch_mozharness_task_id,
    schedule_task_graph,
    TaskBuilder,
)
from lib.chain_of_trust import (
    populate_chain_of_trust_task_graph,
    populate_chain_of_trust_required_but_unused_files
)
from lib.variant import Variant

REPO_URL = os.environ.get('MOBILE_HEAD_REPOSITORY')
COMMIT = os.environ.get('MOBILE_HEAD_REV')
PR_TITLE = os.environ.get('GITHUB_PULL_TITLE', '')
SHORT_HEAD_BRANCH = os.environ.get('SHORT_HEAD_BRANCH')

# If we see this text inside a pull request title then we will not execute any tasks for this PR.
SKIP_TASKS_TRIGGER = '[ci skip]'


BUILDER = TaskBuilder(
    task_id=os.environ.get('TASK_ID'),
    repo_url=REPO_URL,
    git_ref=os.environ.get('MOBILE_HEAD_BRANCH'),
    short_head_branch=SHORT_HEAD_BRANCH,
    commit=COMMIT,
    owner="fenix-eng-notifications@mozilla.com",
    source='{}/raw/{}/.taskcluster.yml'.format(REPO_URL, COMMIT),
    scheduler_id=os.environ.get('SCHEDULER_ID', 'taskcluster-github'),
    tasks_priority=os.environ.get('TASKS_PRIORITY'),
    date_string=os.environ.get('BUILD_DATE'),
    trust_level=int(os.environ.get('TRUST_LEVEL')),
)


def pr_or_push(is_push):
    if not is_push and SKIP_TASKS_TRIGGER in PR_TITLE:
        print("Pull request title contains", SKIP_TASKS_TRIGGER)
        print("Exit")
        return {}

    build_tasks = {}
    signing_tasks = {}
    other_tasks = {}

    for variant in get_variants_for_build_type('debug'):
        assemble_task_id = taskcluster.slugId()
        build_tasks[assemble_task_id] = BUILDER.craft_assemble_task(variant)
        build_tasks[taskcluster.slugId()] = BUILDER.craft_test_task(variant)

    for craft_function in (
        BUILDER.craft_detekt_task,
        BUILDER.craft_ktlint_task,
        BUILDER.craft_lint_task,
        BUILDER.craft_compare_locales_task,
    ):
        other_tasks[taskcluster.slugId()] = craft_function()

    if is_push and SHORT_HEAD_BRANCH == 'master':
        other_tasks[taskcluster.slugId()] = BUILDER.craft_dependencies_task()

    return (build_tasks, signing_tasks, other_tasks)


def raptor(is_staging):
    build_tasks = {}
    signing_tasks = {}
    other_tasks = {}

    geckoview_beta_version = get_geckoview_versions()['beta']
    mozharness_task_id = fetch_mozharness_task_id(geckoview_beta_version)
    gecko_revision = taskcluster.Queue().task(mozharness_task_id)['payload']['env']['GECKO_HEAD_REV']

    for variant in [Variant.from_values(abi, False, 'forPerformanceTest') for abi in ('aarch64', 'arm')]:
        assemble_task_id = taskcluster.slugId()
        build_tasks[assemble_task_id] = BUILDER.craft_assemble_raptor_task(variant)
        signing_task_id = taskcluster.slugId()
        signing_tasks[signing_task_id] = BUILDER.craft_raptor_signing_task(assemble_task_id, variant, is_staging)

        all_raptor_craft_functions = [
            BUILDER.craft_raptor_tp6m_cold_task(for_suite=i)
            for i in range(1, 15)
        ]
        for craft_function in all_raptor_craft_functions:
            args = (signing_task_id, mozharness_task_id, variant, gecko_revision)
            other_tasks[taskcluster.slugId()] = craft_function(*args)

    return (build_tasks, signing_tasks, other_tasks)


def release(channel, is_staging, version_name):
    variants = get_variants_for_build_type(channel)
    architectures = [variant.abi for variant in variants]
    apk_paths = ["public/target.{}.apk".format(arch) for arch in architectures]

    build_tasks = {}
    signing_tasks = {}
    push_tasks = {}

    build_task_id = taskcluster.slugId()
    build_tasks[build_task_id] = BUILDER.craft_assemble_release_task(architectures, channel, is_staging, version_name)

    signing_task_id = taskcluster.slugId()
    signing_tasks[signing_task_id] = BUILDER.craft_release_signing_task(
        build_task_id,
        apk_paths=apk_paths,
        channel=channel,
        is_staging=is_staging,
    )

    push_task_id = taskcluster.slugId()
    push_tasks[push_task_id] = BUILDER.craft_push_task(
        signing_task_id,
        apks=apk_paths,
        channel=channel,
        # TODO until org.mozilla.fenix.nightly is made public, put it on the internally-testable track
        override_google_play_track=None if channel != "nightly" else "internal",
        is_staging=is_staging,
    )

    return (build_tasks, signing_tasks, push_tasks)


def nightly_to_production_app(is_staging, version_name):
    # Since the Fenix nightly was launched, we've pushed it to the production app "org.mozilla.fenix" on the
    # "nightly" track. We're moving towards having each channel be published to its own app, but we need to
    # keep updating this "backwards-compatible" nightly for a while yet
    build_type = 'nightlyLegacy'
    variants = get_variants_for_build_type(build_type)
    architectures = [variant.abi for variant in variants]
    apk_paths = ["public/target.{}.apk".format(arch) for arch in architectures]

    build_tasks = {}
    signing_tasks = {}
    push_tasks = {}

    build_task_id = taskcluster.slugId()
    build_tasks[build_task_id] = BUILDER.craft_assemble_release_task(architectures, build_type, is_staging, version_name, index_channel='nightly')

    signing_task_id = taskcluster.slugId()
    signing_tasks[signing_task_id] = BUILDER.craft_release_signing_task(
        build_task_id,
        apk_paths=apk_paths,
        channel='production',  # Since we're publishing to the "production" app, we need to sign for production
        index_channel='nightly',
        is_staging=is_staging,
    )

    push_task_id = taskcluster.slugId()
    push_tasks[push_task_id] = BUILDER.craft_push_task(
        signing_task_id,
        apks=apk_paths,
        channel='production',  # We're publishing to the "production" app on the "nightly" track
        override_google_play_track='nightly',
        is_staging=is_staging,
    )

    return (build_tasks, signing_tasks, push_tasks)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description='Creates and submit a graph of tasks on Taskcluster.'
    )

    subparsers = parser.add_subparsers(dest='command')

    subparsers.add_parser('pull-request')
    subparsers.add_parser('push')

    raptor_parser = subparsers.add_parser('raptor')
    raptor_parser.add_argument('--staging', action='store_true')

    nightly_parser = subparsers.add_parser('nightly')
    nightly_parser.add_argument('--staging', action='store_true')

    release_parser = subparsers.add_parser('github-release')
    release_parser.add_argument('tag')

    result = parser.parse_args()
    command = result.command
    taskcluster_queue = taskcluster.Queue({'baseUrl': 'http://taskcluster/queue/v1'})

    if command == 'pull-request':
        ordered_groups_of_tasks = pr_or_push(False)
    elif command == 'push':
        ordered_groups_of_tasks = pr_or_push(True)
    elif command == 'raptor':
        ordered_groups_of_tasks = raptor(result.staging)
    elif command == 'nightly':
        nightly_version = datetime.datetime.now().strftime('Nightly %y%m%d %H:%M')
        ordered_groups_of_tasks = release('nightly', result.staging, nightly_version) \
            + nightly_to_production_app(result.staging, nightly_version)
    elif command == 'github-release':
        version = result.tag[1:]  # remove prefixed "v"
        beta_semver = re.compile(r'^v\d+\.\d+\.\d+-beta\.\d+$')
        production_semver = re.compile(r'^v\d+\.\d+\.\d+(-rc\.\d+)?$')
        if beta_semver.match(result.tag):
            ordered_groups_of_tasks = release('beta', False, version)
        elif production_semver.match(result.tag):
            ordered_groups_of_tasks = release('production', False, version)
        else:
            raise ValueError('Github tag must be in semver format and prefixed with a "v", '
                             'e.g.: "v1.0.0-beta.0" (beta), "v1.0.0-rc.0" (production) or "v1.0.0" (production)')
    else:
        raise Exception('Unsupported command "{}"'.format(command))

    full_task_graph = schedule_task_graph(ordered_groups_of_tasks)

    populate_chain_of_trust_task_graph(full_task_graph)
    populate_chain_of_trust_required_but_unused_files()
