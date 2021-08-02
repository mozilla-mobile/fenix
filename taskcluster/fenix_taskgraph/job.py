# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

from __future__ import absolute_import, print_function, unicode_literals

from taskgraph.transforms.job import run_job_using, configure_taskdesc_for_run
from taskgraph.util import path
from taskgraph.util.schema import Schema, taskref_or_string
from voluptuous import Required, Optional
from six import text_type

from pipes import quote as shell_quote

secret_schema = {
    Required("name"): text_type,
    Required("path"): text_type,
    Required("key"): text_type,
    Optional("json"): bool,
}

dummy_secret_schema = {
    Required("content"): text_type,
    Required("path"): text_type,
    Optional("json"): bool,
}

gradlew_schema = Schema({
    Required("using"): "gradlew",
    Optional("pre-gradlew"): [[text_type]],
    Required("gradlew"): [text_type],
    Optional("post-gradlew"): [[text_type]],
    # Base work directory used to set up the task.
    Required("workdir"): text_type,
    Optional("use-caches"): bool,
    Optional("secrets"): [secret_schema],
    Optional("dummy-secrets"): [dummy_secret_schema],
})

run_commands_schema = Schema({
    Required("using"): "run-commands",
    Optional("pre-commands"): [[text_type]],
    Required("commands"): [[taskref_or_string]],
    Required("workdir"): text_type,
    Optional("use-caches"): bool,
    Optional("secrets"): [secret_schema],
    Optional("dummy-secrets"): [dummy_secret_schema],
})


@run_job_using("docker-worker", "run-commands", schema=run_commands_schema)
def configure_run_commands_schema(config, job, taskdesc):
    run = job["run"]
    pre_commands = run.pop("pre-commands", [])
    pre_commands += [
        _generate_dummy_secret_command(secret) for secret in run.pop("dummy-secrets", [])
    ]
    pre_commands += [
        _generate_secret_command(secret) for secret in run.get("secrets", [])
    ]

    all_commands = pre_commands + run.pop("commands", [])

    run["command"] = _convert_commands_to_string(all_commands)
    _inject_secrets_scopes(run, taskdesc)
    _set_run_task_attributes(job)
    configure_taskdesc_for_run(config, job, taskdesc, job["worker"]["implementation"])


@run_job_using("docker-worker", "gradlew", schema=gradlew_schema)
def configure_gradlew(config, job, taskdesc):
    run = job["run"]
    worker = taskdesc["worker"] = job["worker"]

    fetches_dir = path.join(run["workdir"], worker["env"]["MOZ_FETCHES_DIR"])
    worker.setdefault("env", {}).update({
        "ANDROID_SDK_ROOT": path.join(
            fetches_dir, "android-sdk-linux"
        )
    })

    run["command"] = _extract_gradlew_command(run, fetches_dir)
    _inject_secrets_scopes(run, taskdesc)
    _set_run_task_attributes(job)
    configure_taskdesc_for_run(config, job, taskdesc, job["worker"]["implementation"])


def _extract_gradlew_command(run, fetches_dir):
    pre_gradle_commands = run.pop("pre-gradlew", [])
    pre_gradle_commands += [
        _generate_dummy_secret_command(secret) for secret in run.pop("dummy-secrets", [])
    ]
    pre_gradle_commands += [
        _generate_secret_command(secret) for secret in run.get("secrets", [])
    ]

    maven_dependencies_dir = path.join(fetches_dir, "android-gradle-dependencies")
    gradle_repos_args = [
        "-P{repo_name}Repo=file://{dir}/{repo_name}".format(
            dir=maven_dependencies_dir, repo_name=repo_name
        )
        for repo_name in ("google", "central")
    ]
    gradle_command = ["./gradlew"] + gradle_repos_args + ["listRepositories"] + run.pop("gradlew")
    post_gradle_commands = run.pop("post-gradlew", [])

    commands = pre_gradle_commands + [gradle_command] + post_gradle_commands
    return _convert_commands_to_string(commands)


def _generate_secret_command(secret):
    secret_command = [
        "taskcluster/scripts/get-secret.py",
        "-s", secret["name"],
        "-k", secret["key"],
        "-f", secret["path"],
    ]
    if secret.get("json"):
        secret_command.append("--json")

    return secret_command


def _generate_dummy_secret_command(secret):
    secret_command = [
        "taskcluster/scripts/write-dummy-secret.py",
        "-f", secret["path"],
        "-c", secret["content"],
    ]
    if secret.get("json"):
        secret_command.append("--json")

    return secret_command


def _convert_commands_to_string(commands):
    should_artifact_reference = False
    should_task_reference = False

    sanitized_commands = []
    for command in commands:
        sanitized_parts = []
        for part in command:
            if isinstance(part, dict):
                if "artifact-reference" in part:
                    part_string = part["artifact-reference"]
                    should_artifact_reference = True
                elif "task-reference" in part:
                    part_string = part["task-reference"]
                    should_task_reference = True
                else:
                    raise ValueError('Unsupported dict: {}'.format(part))
            else:
                part_string = part

            sanitized_parts.append(part_string)
        sanitized_commands.append(sanitized_parts)

    shell_quoted_commands = [" ".join(map(shell_quote, command)) for command in sanitized_commands]
    full_string_command = " && ".join(shell_quoted_commands)

    if should_artifact_reference and should_task_reference:
        raise NotImplementedError('"arifact-reference" and "task-reference" cannot be both used')
    elif should_artifact_reference:
        return {"artifact-reference": full_string_command}
    elif should_task_reference:
        return {"task-reference": full_string_command}
    else:
        return full_string_command


def _inject_secrets_scopes(run, taskdesc):
    secrets = run.pop("secrets", [])
    scopes = taskdesc.setdefault("scopes", [])
    new_secret_scopes = ["secrets:get:{}".format(secret["name"]) for secret in secrets]
    new_secret_scopes = list(set(new_secret_scopes))  # Scopes must not have any duplicates
    scopes.extend(new_secret_scopes)


def _set_run_task_attributes(job):
    run = job["run"]
    run["cwd"] = "{checkout}"
    run["using"] = "run-task"
