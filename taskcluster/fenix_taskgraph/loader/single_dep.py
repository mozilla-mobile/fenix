# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

from __future__ import absolute_import, print_function, unicode_literals

import copy

from voluptuous import Required

from taskgraph.task import Task
from taskgraph.util.schema import Schema

schema = Schema({Required("primary-dependency", "primary dependency task"): Task})


def loader(kind, path, config, params, loaded_tasks):
    """
    Load tasks based on the jobs dependant kinds.

    Optional `only-for-attributes` kind configuration, if specified, will limit
    the jobs chosen to ones which have the specified attribute, with the specified
    value.

    Optional `job-template` kind configuration value, if specified, will be used to
    pass configuration down to the specified transforms used.
    """
    only_attributes = config.get("only-for-attributes")
    only_build_type = config.get('only-for-build-types')
    job_template = config.get("job-template")

    for task in loaded_tasks:
        if task.kind not in config.get("kind-dependencies", []):
            continue

        if only_attributes:
            config_attrs = set(only_attributes)
            if not config_attrs & set(task.attributes):
                # make sure any attribute exists
                continue

        if only_build_type:
            build_type = task.attributes.get('build-type')
            if build_type not in only_build_type:
                continue

        job = {"primary-dependency": task}

        if job_template:
            job.update(copy.deepcopy(job_template))

        yield job
