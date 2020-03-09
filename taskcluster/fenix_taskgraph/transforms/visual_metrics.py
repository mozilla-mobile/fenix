# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
"""
Generate labels for tasks without names, consistently.
Uses attributes from `primary-dependency`.
"""
from __future__ import absolute_import, print_function, unicode_literals

import os

from taskgraph.transforms.base import TransformSequence

transforms = TransformSequence()

SYMBOL = "%(groupSymbol)s(%(symbol)s-vismet)"
# the test- prefix makes the task SETA-optimized.
LABEL = "test-vismet-%(platform)s-%(label)s"


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
			print(job['fetches'])
			# job['fetches'][dep_job.label] = ['public/test_info/browsertime-results.tgz']
			print(job['fetches'])
			attributes = dict(dep_job.attributes)
			attributes['platform'] = platform
			job['label'] = LABEL % {'platform': platform, 'label': dep_job.label}
			treeherder_info = dict(dep_job.task['extra']['treeherder'])
			job['treeherder']['symbol'] = SYMBOL % treeherder_info

			# vismet runs on Linux but we want to have it displayed
			# alongside the job it was triggered by to make it easier for
			# people to find it back.
			job['treeherder']['platform'] = platform

			# run-on-projects needs to be set based on the dependent task
			job['run-on-projects'] = attributes['run_on_projects']

			### TRY SOMETHING
			worker = job.get('worker', {})
			worker.setdefault('mounts', [])
			worker['mounts'].append({
				'content': {
					'url': script_url(config, 'fetch-content'),
				},
				'file': './fetch-content',
			})
			worker['mounts'].append({
				'content': {
					'url': script_url(config, 'run-task'),
				},
				'file': './run-task',
			})

			job['worker'] = worker

			yield job

def script_url(config, script):
	# This logic is a bit of a hack, and should be replaced by something better.
	# TASK_ID is used as a proxy for running in automation.  In that case, we
	# want to use the run-task/fetch-content corresponding to the taskgraph
	# version we are running, and otherwise, we aren't going to run the task we
	# generate, so the exact version doesn't matter.
	# If we checked out the taskgraph code with run-task in the decision task,
	# we can use TASKGRAPH_* to find the right version, which covers the
	# existing use case.
	if 'TASK_ID' in os.environ:
		if (
			'TASKGRAPH_HEAD_REPOSITORY' not in os.environ
			or 'TASKGRAPH_HEAD_REV' not in os.environ
		):
			raise Exception(
				"Must specify 'TASKGRAPH_HEAD_REPOSITORY' and 'TASKGRAPH_HEAD_REV' "
				"to use run-task on generic-worker."
			)
	taskgraph_repo = os.environ.get(
		'TASKGRAPH_HEAD_REPOSITORY', 'https://hg.mozilla.org/ci/taskgraph'
	)
	taskgraph_rev = os.environ.get('TASKGRAPH_HEAD_REV', 'default')
	return '{}/raw-file/{}/src/taskgraph/run-task/{}'.format(taskgraph_repo, taskgraph_rev, script)


# @run_job_using("docker-worker", "run-task", schema=run_task_schema, defaults=worker_defaults)
# def docker_worker_run_task(config, job, taskdesc):
# 	run = job['run']
# 	worker = taskdesc['worker'] = job['worker']
# 	command = ['/usr/local/bin/run-task']
# 	common_setup(config, job, taskdesc, command)

# 	if run.get('cache-dotcache'):
# 		worker['caches'].append({
# 			'type': 'persistent',
# 			'name': '{project}-dotcache'.format(**config.params),
# 			'mount-point': '{workdir}/.cache'.format(**run),
# 			'skip-untrusted': True,
# 		})

# 	run_command = run['command']

# 	# dict is for the case of `{'task-reference': basestring}`.
# 	if isinstance(run_command, (basestring, dict)):
# 		run_command = ['bash', '-cx', run_command]
# 	command.append('--fetch-hgfingerprint')
# 	if run['run-as-root']:
# 		command.extend(('--user', 'root', '--group', 'root'))
# 	command.append('--')
# 	command.extend(run_command)
# 	worker['command'] = command


# @run_job_using("generic-worker", "run-task", schema=run_task_schema, defaults=worker_defaults)
# def generic_worker_run_task(config, job, taskdesc):
# 	run = job['run']
# 	worker = taskdesc['worker'] = job['worker']
# 	is_win = worker['os'] == 'windows'
# 	is_mac = worker['os'] == 'macosx'
# 	is_bitbar = worker['os'] == 'linux-bitbar'

# 	if is_win:
# 		command = ['C:/mozilla-build/python3/python3.exe', 'run-task']
# 	elif is_mac:
# 		command = ['/tools/python36/bin/python3', 'run-task']
# 	else:
# 		command = ['./run-task']

# 	common_setup(config, job, taskdesc, command)

# 	worker.setdefault('mounts', [])
# 	if run.get('cache-dotcache'):
# 		worker['mounts'].append({
# 			'cache-name': '{project}-dotcache'.format(**config.params),
# 			'directory': '{workdir}/.cache'.format(**run),
# 		})
# 	worker['mounts'].append({
# 		'content': {
# 			'url': script_url(config, 'run-task'),
# 		},
# 		'file': './run-task',
# 	})
# 	if worker.get('env', {}).get('MOZ_FETCHES'):
# 		worker['mounts'].append({
# 			'content': {
# 				'url': script_url(config, 'fetch-content'),
# 			},
# 			'file': './fetch-content',
# 		})

# 	run_command = run['command']

# 	if isinstance(run_command, basestring):
# 		if is_win:
# 			run_command = '"{}"'.format(run_command)
# 		run_command = ['bash', '-cx', run_command]

# 	if run['run-as-root']:
# 		command.extend(('--user', 'root', '--group', 'root'))
# 	command.append('--')
# 	if is_bitbar:
# 		# Use the bitbar wrapper script which sets up the device and adb
# 		# environment variables
# 		command.append('/builds/taskcluster/script.py')
# 	command.extend(run_command)

# 	if is_win:
# 		worker['command'] = [' '.join(command)]
# 	else:
# 		worker['command'] = [
# 			['chmod', '+x', 'run-task'],
# 			command,
# 		]
