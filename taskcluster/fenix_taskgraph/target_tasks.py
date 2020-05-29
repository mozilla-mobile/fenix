# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

from __future__ import absolute_import, print_function, unicode_literals

from taskgraph.target_tasks import _target_task, filter_for_tasks_for


@_target_task('default')
def target_tasks_default(full_task_graph, parameters, graph_config):
    """Target the tasks which have indicated they should be run on this project
    via the `run_on_projects` attributes."""

    filter = filter_for_tasks_for
    return [l for l, t in full_task_graph.tasks.iteritems() if filter_for_tasks_for(t, parameters)]


@_target_task('release')
def target_tasks_default(full_task_graph, parameters, graph_config):

    # TODO Use shipping-phase once we retire github-releases
    def filter(task, parameters):
        return task.attributes.get("release-type", "") == parameters["release_type"]

    return [l for l, t in full_task_graph.tasks.iteritems() if filter(t, parameters)]


@_target_task("nightly")
def target_tasks_nightly(full_task_graph, parameters, graph_config):
    """Select the set of tasks required for a nightly build."""

    def filter(task, parameters):
        # We don't want to ship nightly while Google Play is still behind manual review.
        # See bug 1628413 for more context.
        return task.attributes.get("nightly", False) and task.kind != "push-apk"

    return [l for l, t in full_task_graph.tasks.iteritems() if filter(t, parameters)]


@_target_task("nightly-on-google-play")
def target_tasks_nightly_on_google_play(full_task_graph, parameters, graph_config):
    """Select the set of tasks required for a nightly build that goes on Google Play."""

    def filter(task, parameters):
        return (
            task.attributes.get("nightly", False) and
            # This target_task is temporary while Google Play processes APKs slower than usually
            # (bug 1628413). So we want this target task to be only about shipping APKs to GP and
            # not doing any other miscellaneous tasks like performance testing
            task.kind not in ("browsertime", "visual-metrics", "raptor")
        )

    return [l for l, t in full_task_graph.tasks.iteritems() if filter(t, parameters)]


def _filter_fennec(fennec_type, task, parameters):
    return task.attributes.get("build-type", "") == "fennec-{}".format(fennec_type)


@_target_task("fennec-production")
def target_tasks_fennec_nightly(full_task_graph, parameters, graph_config):
    """Select the set of tasks required for a production build signed with the fennec key."""

    return [l for l, t in full_task_graph.tasks.iteritems() if _filter_fennec("production", t, parameters)]


@_target_task("bump_android_components")
def target_tasks_bump_android_components(full_task_graph, parameters, graph_config):
    """Select the set of tasks required to update android components."""

    def filter(task, parameters):
        return task.attributes.get("bump-type", "") == "android-components"

    return [l for l, t in full_task_graph.tasks.iteritems() if filter(t, parameters)]
