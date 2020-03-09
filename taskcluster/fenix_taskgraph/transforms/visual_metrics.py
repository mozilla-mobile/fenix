# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
"""
Generate labels for tasks without names, consistently.
Uses attributes from `primary-dependency`.
"""
from __future__ import absolute_import, print_function, unicode_literals

from taskgraph.transforms.base import TransformSequence

transforms = TransformSequence()

SYMBOL = "%(groupSymbol)s(%(symbol)s-vismet)"
# the test- prefix makes the task SETA-optimized.
LABEL = "test-vismet-%(platform)s-%(raptor_try_name)s"


@transforms.add
def make_label(config, jobs):
	""" Generate a sane label for a new task constructed from a dependency
	Using attributes from the dependent job and the current task kind"""
	for job in jobs:
		dep_job = job['primary-dependency']
		attr = dep_job.attributes.get

		if attr('locale', job.get('locale')):
			template = "{kind}-{locale}-{build_platform}/{build_type}"
		elif attr('l10n_chunk'):
			template = "{kind}-{build_platform}-{l10n_chunk}/{build_type}"
		elif config.kind.startswith("release-eme-free") or \
				config.kind.startswith("release-partner-repack"):
			suffix = job.get("extra", {}).get("repack_suffix", None) or \
					 job.get("extra", {}).get("repack_id", None)
			template = "{kind}-{build_platform}"
			if suffix:
				template += "-{}".format(suffix.replace('/', '-'))
		else:
			template = "{kind}-{build_platform}/{build_type}"
		job['label'] = template.format(
			kind=config.kind,
			build_platform=attr('build_platform'),
			build_type=attr('build_type'),
			locale=attr('locale', job.get('locale', '')),  # Locale can be absent
			l10n_chunk=attr('l10n_chunk', '')  # Can be empty
		)

		yield job


@transforms.add
def run_visual_metrics(config, jobs):
    for job in jobs:
        dep_job = job.pop('primary-dependency', None)
        print("here1")
        if dep_job is not None:
        	print("here2")
            platform = dep_job.task['extra']['treeherder-platform']
            job['dependencies'] = {dep_job.label: dep_job.label}
            job['fetches'][dep_job.label] = ['/public/test_info/browsertime-results.tgz']
            attributes = dict(dep_job.attributes)
            attributes['platform'] = platform
            job['label'] = LABEL % attributes
            treeherder_info = dict(dep_job.task['extra']['treeherder'])
            job['treeherder']['symbol'] = SYMBOL % treeherder_info

            # vismet runs on Linux but we want to have it displayed
            # alongside the job it was triggered by to make it easier for
            # people to find it back.
            job['treeherder']['platform'] = platform

            # run-on-projects needs to be set based on the dependent task
            job['run-on-projects'] = attributes['run_on_projects']

            yield job
