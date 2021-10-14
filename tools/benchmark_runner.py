# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.

import subprocess
import webbrowser
import os
import argparse

DESCRIPTION = """ This script is made to run benchmark tests on Fenix. It'll open
the JSON output file in firefox (or another browser of your choice if you pass the string in)
"""

ff_browser = 'firefox'
output_path = '/storage/emulated/0/benchmark/'
output_file = 'org.mozilla.fenix-benchmarkData.json'
file_url = "file:///"


def parse_args():
    parser = argparse.ArgumentParser(description=DESCRIPTION)
    parser.add_argument("--class_to_test",
                        help="Path to the class to test. Format it as 'org.mozilla.fenix.[path_to_benchmark_test")

    return parser.parse_args()


def run_benchmark(class_to_test):
    args = ['./gradlew', '-Pbenchmark', 'app:connectedCheck']
    if class_to_test:
        args.append("-Pandroid.testInstrumentationRunnerArguments.class={clazz}".format(clazz=class_to_test))
    subprocess.run(args, check=True, text=True)


def fetch_benchmark_results():
    subprocess.run(
        ['adb', 'pull', "{path}{file}".format(path=output_path, file=output_file)],
        check=True, text=True)
    print("The benchmark results can be seen here: {file_path}"
                .format(file_path=os.path.abspath("./{file}".format(file=file_url))))


def open_in_browser():
    abs_path = os.path.abspath("./{file}".format(file=output_file))
    webbrowser.get(ff_browser).open_new(file_url+abs_path)


def main():
    args = parse_args()
    run_benchmark(args.class_to_test)
    fetch_benchmark_results()
    open_in_browser()


if __name__ == '__main__':
    main()
