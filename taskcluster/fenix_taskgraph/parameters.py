# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

import os

from taskgraph.parameters import extend_parameters_schema
from voluptuous import All, Any, Optional, Range, Required


extend_parameters_schema(
    {
        Required("pull_request_number"): Any(All(int, Range(min=1)), None),
        Required("release_type", default=""): str,
        Optional("shipping_phase"): Any("build", "promote", "ship", None),
    }
)


def get_decision_parameters(graph_config, parameters):
    parameters.setdefault("release_type", "")

    pr_number = os.environ.get("MOBILE_PULL_REQUEST_NUMBER", None)
    parameters["pull_request_number"] = None if pr_number is None else int(pr_number)
