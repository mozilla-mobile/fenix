# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

"""
This script is executed on taskcluster whenever we want to release builds
to Google Play. It will schedule the taskcluster tasks for building,
signing and uploading a release.
"""

import argparse
import json
import os
import taskcluster
import lib.tasks

TASK_ID = os.environ.get('TASK_ID')

BUILDER = lib.tasks.TaskBuilder(
    task_id=TASK_ID,
    repo_url=os.environ.get('GITHUB_HEAD_REPO_URL'),
    branch=os.environ.get('GITHUB_HEAD_BRANCH'),
    commit=os.environ.get('GITHUB_HEAD_SHA'),
    owner="skaspari@mozilla.com",
    source="https://github.com/mozilla-mobile/fenix/tree/master/automation/taskcluster"
)

def generate_build_task(apks, tag):
    artifacts = {}
    for apk in apks:
        artifact = {
            "type": 'file',
            "path": apk,
            "expires": taskcluster.stringDate(taskcluster.fromNow('1 year'))
        }
        artifacts["public/%s" % os.path.basename(apk)] = artifact

    checkout = "git fetch origin && git reset --hard origin/master" if tag is None else "git fetch origin && git checkout %s" % (tag)

    assemble_task = 'assembleNightly'

    if tag:
        # Non-tagged (nightly) builds should contain all languages
        checkout = checkout + ' && python tools/l10n/filter-release-translations.py'
        assemble_task = 'assembleRelease'


    return taskcluster.slugId(), BUILDER.build_task(
        name="Fenix - Build task",
        description="Build Fenix from source code.",
        command=(checkout +
                 ' && ./gradlew --no-daemon clean test ' + assemble_task),
        features = {
            "chainOfTrust": True
        },
        artifacts = artifacts,
        worker_type='gecko-focus',
        scopes=[]
    )

def generate_signing_task(build_task_id, apks, tag):
    artifacts = []
    for apk in apks:
        artifacts.append("public/" + os.path.basename(apk))

    routes = []
    scopes = [
        "project:mobile:fenix:releng:signing:cert:release-signing",
        "project:mobile:fenix:releng:signing:format:autograph_fenix"
    ]

    if tag:
        pass
        # TODO Activate this *before* merging this PR
        # index = "index.project.mobile.fenix.release.latest"
        # routes.append(index)
        # scopes.append("queue:route:" + index)
    else:
        pass
        # TODO Activate this *before* merging this PR
        # index = "index.project.mobile.fenix.nightly.latest"
        # routes.append(index)
        # scopes.append("queue:route:" + index)

    return taskcluster.slugId(), BUILDER.build_signing_task(
        build_task_id,
        name="Fenix - Signing task",
        description="Sign release builds of Fenix",
        apks=artifacts,
        scopes=scopes,
        routes=routes
    )


def populate_chain_of_trust_required_but_unused_files():
    # Thoses files are needed to keep chainOfTrust happy. However, they have no need for Fenix,
    # at the moment. For more details, see:
    # https://github.com/mozilla-releng/scriptworker/pull/209/files#r184180585

    for file_names in ('actions.json', 'parameters.yml'):
        with open(file_names, 'w') as f:
            json.dump({}, f)    # Yaml is a super-set of JSON.


def release(apks, track, commit, tag):
    queue = taskcluster.Queue({ 'baseUrl': 'http://taskcluster/queue/v1' })

    task_graph = {}

    build_task_id, build_task = generate_build_task(apks, tag)
    lib.tasks.schedule_task(queue, build_task_id, build_task)

    task_graph[build_task_id] = {}
    task_graph[build_task_id]["task"] = queue.task(build_task_id)

    sign_task_id, sign_task = generate_signing_task(build_task_id, apks, tag)
    lib.tasks.schedule_task(queue, sign_task_id, sign_task)

    task_graph[sign_task_id] = {}
    task_graph[sign_task_id]["task"] = queue.task(sign_task_id)

    print json.dumps(task_graph, indent=4, separators=(',', ': '))

    task_graph_path = "task-graph.json"
    with open(task_graph_path, 'w') as f:
        json.dump(task_graph, f)

    populate_chain_of_trust_required_but_unused_files()



if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description='Create a release pipeline (build, sign, publish) on taskcluster.')

    parser.add_argument('--track', dest="track", action="store", choices=['internal', 'alpha', 'nightly'], help="", required=True)
    parser.add_argument('--commit', dest="commit", action="store_true", help="commit the google play transaction")
    parser.add_argument('--tag', dest="tag", action="store", help="git tag to build from")
    parser.add_argument('--apk', dest="apks", metavar="path", action="append", help="Path to APKs to sign and upload", required=True)
    parser.add_argument('--output', dest="output", metavar="path", action="store", help="Path to the build output", required=True)

    result = parser.parse_args()

    apks = map(lambda x: result.output + '/' + x, result.apks)

    release(apks, result.track, result.commit, result.tag)
