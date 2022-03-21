# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

from taskgraph.target_tasks import _target_task


@_target_task("release")
def target_tasks_default(full_task_graph, parameters, graph_config):

    # TODO Use shipping-phase
    def filter(task, parameters):
        return task.attributes.get("release-type", "") == parameters["release_type"]

    return [l for l, t in full_task_graph.tasks.items() if filter(t, parameters)]


@_target_task("nightly")
def target_tasks_nightly(full_task_graph, parameters, graph_config):
    """Select the set of tasks required for a nightly build."""

    def filter(task, parameters):
        return task.attributes.get("nightly", False)

    return [l for l, t in full_task_graph.tasks.items() if filter(t, parameters)]


def _filter_fennec(fennec_type, task, parameters):
    return task.attributes.get("build-type", "") == "fennec-{}".format(fennec_type)


@_target_task("fennec-production")
def target_tasks_fennec_nightly(full_task_graph, parameters, graph_config):
    """Select the set of tasks required for a production build signed with the fennec key."""

    return [
        l
        for l, t in full_task_graph.tasks.items()
        if _filter_fennec("production", t, parameters)
    ]


@_target_task("bump_android_components")
def target_tasks_bump_android_components(full_task_graph, parameters, graph_config):
    """Select the set of tasks required to update android components."""

    def filter(task, parameters):
        return task.attributes.get("bump-type", "") == "android-components"

    return [l for l, t in full_task_graph.tasks.items() if filter(t, parameters)]


@_target_task("screenshots")
def target_tasks_screnshots(full_task_graph, parameters, graph_config):
    """Select the set of tasks required to generate screenshots on a real device."""

    def filter(task, parameters):
        return task.attributes.get("screenshots", False)

    return [l for l, t in full_task_graph.tasks.items() if filter(t, parameters)]
