# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

from __future__ import print_function

import arrow
import datetime
import json
import taskcluster

from lib.util import upper_case_first_letter, convert_camel_case_into_kebab_case, lower_case_first_letter

DEFAULT_EXPIRES_IN = '1 year'
DEFAULT_APK_ARTIFACT_LOCATION = 'public/target.apk'
_OFFICIAL_REPO_URL = 'https://github.com/mozilla-mobile/fenix'
_DEFAULT_TASK_URL = 'https://queue.taskcluster.net/v1/task'


class TaskBuilder(object):
    def __init__(
        self,
        task_id,
        repo_url,
        git_ref,
        short_head_branch, commit, owner, source, scheduler_id, date_string,
        tasks_priority='lowest',
        trust_level=1
    ):
        self.task_id = task_id
        self.repo_url = repo_url
        self.git_ref = git_ref
        self.short_head_branch = short_head_branch
        self.commit = commit
        self.owner = owner
        self.source = source
        self.scheduler_id = scheduler_id
        self.trust_level = trust_level
        self.tasks_priority = tasks_priority
        self.date = arrow.get(date_string)
        self.trust_level = trust_level

    def craft_assemble_release_task(self, architectures, build_type, is_staging, version_name, index_channel=None):
        index_channel = index_channel or build_type
        artifacts = {
            'public/target.{}.apk'.format(arch): {
                "type": 'file',
                "path": '/opt/fenix/app/build/outputs/apk/'
                        '{arch}/{build_type}/app-{arch}-{build_type}-unsigned.apk'.format(arch=arch, build_type=build_type),
                "expires": taskcluster.stringDate(taskcluster.fromNow(DEFAULT_EXPIRES_IN)),
            }
            for arch in architectures
        }

        if is_staging:
            secret_index = 'garbage/staging/project/mobile/fenix'
        else:
            secret_index = 'project/mobile/fenix/{}'.format(index_channel)

        pre_gradle_commands = (
            'python automation/taskcluster/helper/get-secret.py -s {} -k {} -f {}'.format(
                secret_index, key, target_file
            )
            for key, target_file in (
                ('sentry_dsn', '.sentry_token'),
                ('leanplum', '.leanplum_token'),
                ('adjust', '.adjust_token'),
            )
        )

        capitalized_build_type = upper_case_first_letter(build_type)
        gradle_commands = (
            './gradlew --no-daemon -PversionName="{}" clean test assemble{}'.format(
                version_name, capitalized_build_type),
        )

        command = ' && '.join(
            cmd
            for commands in (pre_gradle_commands, gradle_commands)
            for cmd in commands
            if cmd
        )

        routes = [] if is_staging else [
            "notify.email.fenix-eng-notifications@mozilla.com.on-failed"
        ]

        return self._craft_build_ish_task(
            name='Build {} task'.format(capitalized_build_type),
            description='Build Fenix {} from source code'.format(capitalized_build_type),
            command=command,
            scopes=[
                "secrets:get:{}".format(secret_index)
            ],
            artifacts=artifacts,
            routes=routes,
            treeherder={
                'jobKind': 'build',
                'machine': {
                    'platform': 'android-all',
                },
                'symbol': '{}-A'.format(build_type),
                'tier': 1,
            },
        )

    def craft_assemble_raptor_task(self, variant):
        command = ' && '.join((
            'echo "https://fake@sentry.prod.mozaws.net/368" > .sentry_token',
            'echo "--" > .adjust_token',
            'echo "-:-" > .leanplum_token',
            './gradlew --no-daemon clean assemble{}'.format(variant.for_gradle_command),
        ))

        return self._craft_build_ish_task(
            name='assemble: {}'.format(variant.raw),
            description='Building and testing variant {}'.format(variant.raw),
            command=command,
            artifacts=_craft_artifacts_from_variant(variant),
            treeherder={
                'groupSymbol': variant.build_type,
                'jobKind': 'build',
                'machine': {
                    'platform': variant.platform,
                },
                'symbol': 'A',
                'tier': 1,
            },
        )

    def craft_assemble_task(self, variant):
        return self._craft_clean_gradle_task(
            name='assemble: {}'.format(variant.raw),
            description='Building and testing variant {}'.format(variant.raw),
            gradle_task='assemble{}'.format(variant.for_gradle_command),
            artifacts=_craft_artifacts_from_variant(variant),
            treeherder={
                'groupSymbol': variant.build_type,
                'jobKind': 'build',
                'machine': {
                  'platform': variant.platform,
                },
                'symbol': 'A',
                'tier': 1,
            },
        )

    def craft_test_task(self, variant):
        return self._craft_clean_gradle_task(
            name='test: {}'.format(variant.raw),
            description='Building and testing variant {}'.format(variant.raw),
            gradle_task='test{}UnitTest'.format(variant.for_gradle_command),
            treeherder={
                'groupSymbol': variant.build_type,
                'jobKind': 'test',
                'machine': {
                  'platform': variant.platform,
                },
                'symbol': 'T',
                'tier': 1,
            },
        )

    def craft_detekt_task(self):
        return self._craft_clean_gradle_task(
            name='detekt',
            description='Running detekt code quality checks',
            gradle_task='detekt',
            treeherder={
                'jobKind': 'test',
                'machine': {
                  'platform': 'lint',
                },
                'symbol': 'detekt',
                'tier': 1,
            }

        )

    def craft_ktlint_task(self):
        return self._craft_clean_gradle_task(
            name='ktlint',
            description='Running ktlint code quality checks',
            gradle_task='ktlint',
            treeherder={
                'jobKind': 'test',
                'machine': {
                  'platform': 'lint',
                },
                'symbol': 'ktlint',
                'tier': 1,
            }
        )

    def craft_lint_task(self):
        return self._craft_clean_gradle_task(
            name='lint',
            description='Running lint for aarch64 release variant',
            gradle_task='lintDebug',
            treeherder={
                'jobKind': 'test',
                'machine': {
                  'platform': 'lint',
                },
                'symbol': 'lint',
                'tier': 1,
            },
        )

    def craft_dependencies_task(self):
        # Output the dependencies to an artifact.  This is used by the
        # telemetry probe scraper to determine all of the metrics that
        # Fenix might send (both from itself and any of its dependent
        # libraries that use Glean).
        return self._craft_clean_gradle_task(
            name='dependencies',
            description='Write dependencies to a build artifact',
            gradle_task='app:dependencies --configuration implementation > dependencies.txt',
            treeherder={
                'jobKind': 'test',
                'machine': {
                    'platform': 'lint',
                },
                'symbol': 'dependencies',
                'tier': 1,
            },
            routes=[
                'index.project.mobile.fenix.v2.branch.master.revision.{}'.format(self.commit)
            ],
            artifacts={
                'public/dependencies.txt': {
                    "type": 'file',
                    "path": '/opt/fenix/dependencies.txt',
                    "expires": taskcluster.stringDate(taskcluster.fromNow(DEFAULT_EXPIRES_IN)),
                }
            },
        )

    def _craft_clean_gradle_task(
        self, name, description, gradle_task, artifacts=None, routes=None, treeherder=None
    ):
        return self._craft_build_ish_task(
            name=name,
            description=description,
            command='./gradlew --no-daemon clean {}'.format(gradle_task),
            artifacts=artifacts,
            routes=routes,
            treeherder=treeherder,
        )

    def craft_compare_locales_task(self):
        return self._craft_build_ish_task(
            name='compare-locales',
            description='Validate strings.xml with compare-locales',
            command=(
                'pip install "compare-locales>=5.0.2,<6.0" && '
                'compare-locales --validate l10n.toml .'
            ),
            treeherder={
                'jobKind': 'test',
                'machine': {
                  'platform': 'lint',
                },
                'symbol': 'compare-locale',
                'tier': 2,
            }
        )

    def _craft_build_ish_task(
        self, name, description, command, dependencies=None, artifacts=None, scopes=None,
        routes=None, treeherder=None
    ):
        dependencies = [] if dependencies is None else dependencies
        artifacts = {} if artifacts is None else artifacts
        scopes = [] if scopes is None else scopes
        routes = [] if routes is None else routes

        checkout_command = ' && '.join([
            "export TERM=dumb",
            "git fetch {} {}".format(self.repo_url, self.git_ref),
            "git config advice.detachedHead false",
            "git checkout FETCH_HEAD",
        ])

        command = '{} && {}'.format(checkout_command, command)

        features = {}
        if artifacts:
            features['chainOfTrust'] = True
        if any(scope.startswith('secrets:') for scope in scopes):
            features['taskclusterProxy'] = True

        payload = {
            "features": features,
            "maxRunTime": 7200,
            "image": "mozillamobile/fenix:1.3",
            "command": [
                "/bin/bash",
                "--login",
                "-cx",
                command
            ],
            "artifacts": artifacts,
        }

        return self._craft_default_task_definition(
            worker_type='mobile-{}-b-fenix'.format(self.trust_level),
            provisioner_id='aws-provisioner-v1',
            name=name,
            description=description,
            payload=payload,
            dependencies=dependencies,
            routes=routes,
            scopes=scopes,
            treeherder=treeherder,
        )

    def _craft_signing_task(self, name, description, signing_type, assemble_task_id, apk_paths, routes, treeherder):
        signing_format = "autograph_apk"
        payload = {
            'upstreamArtifacts': [{
                'paths': apk_paths,
                'formats': [signing_format],
                'taskId': assemble_task_id,
                'taskType': 'build'
            }]
        }

        return self._craft_default_task_definition(
            worker_type='mobile-signing-dep-v1' if signing_type == 'dep' else 'mobile-signing-v1',
            provisioner_id='scriptworker-prov-v1',
            dependencies=[assemble_task_id],
            routes=routes,
            scopes=[
                "project:mobile:fenix:releng:signing:format:{}".format(signing_format),
                "project:mobile:fenix:releng:signing:cert:{}-signing".format(signing_type),
            ],
            name=name,
            description=description,
            payload=payload,
            treeherder=treeherder,
        )

    def _craft_default_task_definition(
        self,
        worker_type,
        provisioner_id,
        name,
        description,
        payload,
        dependencies=None,
        routes=None,
        scopes=None,
        treeherder=None,
    ):
        dependencies = [] if dependencies is None else dependencies
        scopes = [] if scopes is None else scopes
        routes = [] if routes is None else routes
        treeherder = {} if treeherder is None else treeherder

        created = datetime.datetime.now()
        deadline = taskcluster.fromNow('1 day')
        expires = taskcluster.fromNow(DEFAULT_EXPIRES_IN)

        if self.trust_level == 3:
            routes.append('tc-treeherder.v2.fenix.{}'.format(self.commit))

        return {
            "provisionerId": provisioner_id,
            "workerType": worker_type,
            "taskGroupId": self.task_id,
            "schedulerId": self.scheduler_id,
            "created": taskcluster.stringDate(created),
            "deadline": taskcluster.stringDate(deadline),
            "expires": taskcluster.stringDate(expires),
            "retries": 5,
            "tags": {},
            "priority": self.tasks_priority,
            "dependencies": [self.task_id] + dependencies,
            "requires": "all-completed",
            "routes": routes,
            "scopes": scopes,
            "payload": payload,
            "extra": {
                "treeherder": treeherder,
            },
            "metadata": {
                "name": "Fenix - {}".format(name),
                "description": description,
                "owner": self.owner,
                "source": self.source,
            },
        }

    def craft_raptor_signing_task(
        self, assemble_task_id, variant, is_staging,
    ):
        staging_prefix = '.staging' if is_staging else ''
        routes = [
            "index.project.mobile.fenix.v2{}.performance-test.{}.{}.{}.latest.{}".format(
                staging_prefix, self.date.year, self.date.month, self.date.day, variant.abi
            ),
            "index.project.mobile.fenix.v2{}.performance-test.{}.{}.{}.revision.{}.{}".format(
                staging_prefix, self.date.year, self.date.month, self.date.day, self.commit, variant.abi
            ),
            "index.project.mobile.fenix.v2{}.performance-test.latest.{}".format(staging_prefix, variant.abi),
        ]

        return self._craft_signing_task(
            name='sign: {}'.format(variant.raw),
            description='Dep-signing variant {}'.format(variant.raw),
            signing_type="dep",
            assemble_task_id=assemble_task_id,
            apk_paths=["public/target.apk"],
            routes=routes,
            treeherder={
                'groupSymbol': variant.build_type,
                'jobKind': 'other',
                'machine': {
                    'platform': variant.platform,
                },
                'symbol': 'As',
                'tier': 1,
            },
        )

    def craft_release_signing_task(
        self, build_task_id, apk_paths, channel, is_staging, index_channel=None
    ):
        index_channel = index_channel or channel
        staging_prefix = '.staging' if is_staging else ''

        routes = [
            "index.project.mobile.fenix.v2{}.{}.{}.{}.{}.latest".format(
                staging_prefix, index_channel, self.date.year, self.date.month, self.date.day
            ),
            "index.project.mobile.fenix.v2{}.{}.{}.{}.{}.revision.{}".format(
                staging_prefix, index_channel, self.date.year, self.date.month, self.date.day, self.commit
            ),
            "index.project.mobile.fenix.v2{}.{}.latest".format(staging_prefix, index_channel),
        ]

        capitalized_channel = upper_case_first_letter(channel)
        return self._craft_signing_task(
            name="Signing {} task".format(capitalized_channel),
            description="Sign {} builds of Fenix".format(capitalized_channel),
            signing_type="dep" if is_staging else channel,
            assemble_task_id=build_task_id,
            apk_paths=apk_paths,
            routes=routes,
            treeherder={
                'jobKind': 'other',
                'machine': {
                  'platform': 'android-all',
                },
                'symbol': '{}-s'.format(channel),
                'tier': 1,
            },
        )

    def craft_push_task(
        self, signing_task_id, apks, channel, is_staging=False, override_google_play_track=None
    ):
        payload = {
            "commit": True,
            "channel": channel,
            "certificate_alias": 'fenix' if is_staging else 'fenix-{}'.format(channel),
            "upstreamArtifacts": [
                {
                    "paths": apks,
                    "taskId": signing_task_id,
                    "taskType": "signing"
                }
            ]
        }

        if override_google_play_track:
            payload['google_play_track'] = override_google_play_track

        return self._craft_default_task_definition(
            worker_type='mobile-pushapk-dep-v1' if is_staging else 'mobile-pushapk-v1',
            provisioner_id='scriptworker-prov-v1',
            dependencies=[signing_task_id],
            routes=[],
            scopes=[
                "project:mobile:fenix:releng:googleplay:product:fenix{}".format(
                    ':dep' if is_staging else ''
                )
            ],
            name="Push task",
            description="Upload signed release builds of Fenix to Google Play",
            payload=payload,
            treeherder={
                'jobKind': 'other',
                'machine': {
                  'platform': 'android-all',
                },
                'symbol': '{}-gp'.format(channel),
                'tier': 1,
            },
        )

    def craft_raptor_tp6m_cold_task(self, for_suite):

        def craft_function(signing_task_id, mozharness_task_id, variant, gecko_revision, force_run_on_64_bit_device=False):
            return self._craft_raptor_task(
                signing_task_id,
                mozharness_task_id,
                variant,
                gecko_revision,
                name_prefix='raptor tp6m-cold-{}'.format(for_suite),
                description='Raptor tp6m cold on Fenix',
                test_name='raptor-tp6m-cold-{}'.format(for_suite),
                job_symbol='tp6m-c-{}'.format(for_suite),
                force_run_on_64_bit_device=force_run_on_64_bit_device,
            )
        return craft_function

    def _craft_raptor_task(
        self,
        signing_task_id,
        mozharness_task_id,
        variant,
        gecko_revision,
        name_prefix,
        description,
        test_name,
        job_symbol,
        group_symbol=None,
        force_run_on_64_bit_device=False,
    ):
        worker_type = 'gecko-t-bitbar-gw-perf-p2' if force_run_on_64_bit_device or variant.abi == 'aarch64' else 'gecko-t-bitbar-gw-perf-g5'

        if force_run_on_64_bit_device:
            treeherder_platform = 'android-hw-p2-8-0-arm7-api-16'
        elif variant.abi == 'arm':
            treeherder_platform = 'android-hw-g5-7-0-arm7-api-16'
        elif variant.abi == 'aarch64':
            treeherder_platform = 'android-hw-p2-8-0-android-aarch64'
        else:
            raise ValueError('Unsupported architecture "{}"'.format(variant.abi))

        task_name = '{}: {} {}'.format(
            name_prefix, variant.raw, '(on 64-bit-device)' if force_run_on_64_bit_device else ''
        )

        apk_url = '{}/{}/artifacts/{}'.format(_DEFAULT_TASK_URL, signing_task_id,
                                                        DEFAULT_APK_ARTIFACT_LOCATION)
        return self._craft_default_task_definition(
            worker_type=worker_type,
            provisioner_id='proj-autophone',
            dependencies=[signing_task_id],
            name=task_name,
            description=description,
            payload={
                "maxRunTime": 2700,
                "artifacts": [{
                    'path': '{}'.format(worker_path),
                    'expires': taskcluster.stringDate(taskcluster.fromNow(DEFAULT_EXPIRES_IN)),
                    'type': 'directory',
                    'name': 'public/{}/'.format(public_folder)
                } for worker_path, public_folder in (
                    ('artifacts/public', 'test'),
                    ('workspace/logs', 'logs'),
                    ('workspace/build/blobber_upload_dir', 'test_info'),
                )],
                "command": [[
                    "/builds/taskcluster/script.py",
                    "bash",
                    "./test-linux.sh",
                    "--cfg=mozharness/configs/raptor/android_hw_config.py",
                    "--test={}".format(test_name),
                    "--app=fenix",
                    "--binary=org.mozilla.fenix.performancetest",
                    "--activity=org.mozilla.fenix.browser.BrowserPerformanceTestActivity",
                    "--download-symbols=ondemand",
                ]],
                "env": {
                    "EXTRA_MOZHARNESS_CONFIG": json.dumps({
                        "test_packages_url": "{}/{}/artifacts/public/build/en-US/target.test_packages.json".format(_DEFAULT_TASK_URL, mozharness_task_id),
                        "installer_url": apk_url,
                    }),
                    "GECKO_HEAD_REPOSITORY": "https://hg.mozilla.org/releases/mozilla-beta",
                    "GECKO_HEAD_REV": gecko_revision,
                    "MOZ_AUTOMATION": "1",
                    "MOZ_HIDE_RESULTS_TABLE": "1",
                    "MOZ_NO_REMOTE": "1",
                    "MOZ_NODE_PATH": "/usr/local/bin/node",
                    "MOZHARNESS_CONFIG": "raptor/android_hw_config.py",
                    "MOZHARNESS_SCRIPT": "raptor_script.py",
                    "MOZHARNESS_URL": "{}/{}/artifacts/public/build/en-US/mozharness.zip".format(_DEFAULT_TASK_URL, mozharness_task_id),
                    "MOZILLA_BUILD_URL": apk_url,
                    "NEED_XVFB": "false",
                    "NO_FAIL_ON_TEST_ERRORS": "1",
                    "SCCACHE_DISABLE": "1",
                    "TASKCLUSTER_WORKER_TYPE": worker_type[len('gecko-'):],
                    "TRY_COMMIT_MSG": "",
                    "TRY_SELECTOR": "fuzzy",
                    "XPCOM_DEBUG_BREAK": "warn",
                },
                "mounts": [{
                    "content": {
                        "url": "https://hg.mozilla.org/releases/mozilla-beta/raw-file/{}/taskcluster/scripts/tester/test-linux.sh".format(gecko_revision),
                    },
                    "file": "test-linux.sh",
                }]
            },
            treeherder={
                'jobKind': 'test',
                'groupSymbol': 'Rap' if group_symbol is None else group_symbol,
                'machine': {
                    'platform': treeherder_platform,
                },
                'symbol': job_symbol,
                'tier': 2,
            }
        )


def _craft_artifacts_from_variant(variant):
    return {
        DEFAULT_APK_ARTIFACT_LOCATION: {
            'type': 'file',
            'path': variant.apk_absolute_path(),
            'expires': taskcluster.stringDate(taskcluster.fromNow(DEFAULT_EXPIRES_IN)),
        }
    }


def schedule_task(queue, taskId, task):
    print("TASK", taskId)
    print(json.dumps(task, indent=4, separators=(',', ': ')))

    result = queue.createTask(taskId, task)
    print("RESULT", taskId)
    print(json.dumps(result))


def schedule_task_graph(ordered_groups_of_tasks):
    queue = taskcluster.Queue({'baseUrl': 'http://taskcluster/queue/v1'})
    full_task_graph = {}

    # TODO: Switch to async python to speed up submission
    for group_of_tasks in ordered_groups_of_tasks:
        for task_id, task_definition in group_of_tasks.items():
            schedule_task(queue, task_id, task_definition)

            full_task_graph[task_id] = {
                # Some values of the task definition are automatically filled. Querying the task
                # allows to have the full definition. This is needed to make Chain of Trust happy
                'task': queue.task(task_id),
            }

    return full_task_graph


def fetch_mozharness_task_id(geckoview_beta_version):
    raptor_index = 'gecko.v2.mozilla-beta.geckoview-version.{}.mobile.android-x86_64-beta-opt'.format(
        geckoview_beta_version
    )
    return taskcluster.Index().findTask(raptor_index)['taskId']
