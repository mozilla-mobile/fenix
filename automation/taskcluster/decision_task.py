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

from lib.gradle import get_build_variants, get_geckoview_versions
from lib.tasks import (
    fetch_mozharness_task_id,
    get_architecture_and_build_type_from_variant,
    schedule_task_graph,
    TaskBuilder,
)
from lib.chain_of_trust import (
    populate_chain_of_trust_task_graph,
    populate_chain_of_trust_required_but_unused_files
)

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
    trust_level=os.environ.get('TRUST_LEVEL'),
)


def pr_or_push(is_push):
    if not is_push and SKIP_TASKS_TRIGGER in PR_TITLE:
        print("Pull request title contains", SKIP_TASKS_TRIGGER)
        print("Exit")
        return {}

    debug_variants = [variant for variant in get_build_variants() if variant.endswith('Debug')]
    geckoview_nightly_version = get_geckoview_versions()['nightly']
    mozharness_task_id = fetch_mozharness_task_id(geckoview_nightly_version)
    gecko_revision = taskcluster.Queue().task(mozharness_task_id)['payload']['env']['GECKO_HEAD_REV']

    build_tasks = {}
    signing_tasks = {}
    other_tasks = {}

    for variant in debug_variants:
        assemble_task_id = taskcluster.slugId()
        build_tasks[assemble_task_id] = BUILDER.craft_assemble_task(variant)
        build_tasks[taskcluster.slugId()] = BUILDER.craft_test_task(variant)

    if is_push and SHORT_HEAD_BRANCH == 'master':
        other_tasks[taskcluster.slugId()] = BUILDER.craft_dependencies_task()

        for variant in ('armRaptor', 'aarch64Raptor'):
            assemble_task_id = taskcluster.slugId()
            build_tasks[assemble_task_id] = BUILDER.craft_assemble_task(variant)
            signing_task_id = taskcluster.slugId()
            signing_tasks[signing_task_id] = BUILDER.craft_raptor_signing_task(assemble_task_id, variant)

            ALL_RAPTOR_CRAFT_FUNCTIONS = [
                BUILDER.craft_raptor_tp6m_cold_task(for_suite=i)
                for i in range(1, 2)  # TODO change to range(1, 15) once https://bugzilla.mozilla.org/show_bug.cgi?id=1549948 is fixed
            ]
            for craft_function in ALL_RAPTOR_CRAFT_FUNCTIONS:
                args = (signing_task_id, mozharness_task_id, variant, gecko_revision)
                other_tasks[taskcluster.slugId()] = craft_function(*args)

    for craft_function in (
        BUILDER.craft_detekt_task,
        BUILDER.craft_ktlint_task,
        BUILDER.craft_lint_task,
        BUILDER.craft_compare_locales_task,
    ):
        other_tasks[taskcluster.slugId()] = craft_function()

    return (build_tasks, signing_tasks, other_tasks)


def release(track, is_staging, version_name):
    architectures = ['x86', 'arm', 'aarch64']
    apk_paths = ["public/target.{}.apk".format(arch) for arch in architectures]

    build_tasks = {}
    signing_tasks = {}
    push_tasks = {}

    build_task_id = taskcluster.slugId()
    build_tasks[build_task_id] = BUILDER.craft_assemble_release_task(architectures, track, is_staging, version_name)

    signing_task_id = taskcluster.slugId()
    signing_tasks[signing_task_id] = BUILDER.craft_release_signing_task(
        build_task_id,
        apk_paths=apk_paths,
        track=track,
        is_staging=is_staging,
    )

    push_task_id = taskcluster.slugId()
    push_tasks[push_task_id] = BUILDER.craft_push_task(
        signing_task_id,
        apks=apk_paths,
        track=track,
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

    nightly_parser = subparsers.add_parser('nightly')
    nightly_parser.add_argument('--staging', action='store_true')

    release_parser = subparsers.add_parser('beta')
    release_parser.add_argument('tag')

    result = parser.parse_args()
    command = result.command
    taskcluster_queue = taskcluster.Queue({'baseUrl': 'http://taskcluster/queue/v1'})

    if command == 'pull-request':
        ordered_groups_of_tasks = pr_or_push(False)
    elif command == 'push':
        ordered_groups_of_tasks = pr_or_push(True)
    elif command == 'nightly':
        formatted_date = datetime.datetime.now().strftime('%y%V')
        ordered_groups_of_tasks = release('nightly', result.staging, '1.0.{}'.format(formatted_date))
    elif command == 'beta':
        semver = re.compile(r'^\d+\.\d+\.\d+-beta\.\d+$')
        if not semver.match(result.tag):
            raise ValueError('Github tag must be in beta semver format, e.g.: "1.0.0-beta.0')
        ordered_groups_of_tasks = release('beta', False, result.tag)
    else:
        raise Exception('Unsupported command "{}"'.format(command))

    full_task_graph = schedule_task_graph(ordered_groups_of_tasks)

    populate_chain_of_trust_task_graph(full_task_graph)
    populate_chain_of_trust_required_but_unused_files()
