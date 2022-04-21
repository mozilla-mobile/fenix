#!/usr/bin/python3

from __future__ import print_function

import sys
import argparse
from pathlib import Path
import json
import yaml

def parse_args(cmdln_args):
    parser = argparse.ArgumentParser(description="Parse UI test logs an results")
    parser.add_argument(
        "--output-md",
        type=argparse.FileType("w", encoding="utf-8"),
        help="Output markdown file.",
        required=True,
    )
    parser.add_argument(
        "--log",
        type=argparse.FileType("r", encoding="utf-8"),
        help="Log output of flank.",
        required=True,
    )
    parser.add_argument(
        "--results", type=Path, help="Directory containing flank results", required=True
    )
    parser.add_argument(
        "--exit-code", type=int, help="Exit code of flank.", required=True
    )
    parser.add_argument("--device-type", help="Type of device ", required=True)
    return parser.parse_args(args=cmdln_args)


def extract_android_args(log):
    return yaml.safe_load(log.split("AndroidArgs\n")[1].split("RunTests\n")[0])


def main():
    args = parse_args(sys.argv[1:])

    log = args.log.read()
    matrix_ids = json.loads(args.results.joinpath("matrix_ids.json").read_text())
    #with args.results.joinpath("flank.yml") as f:
    #    flank_config = yaml.safe_load(f)

    android_args = extract_android_args(log)

    print = args.output_md.write

    print("# Devices\n")
    print(yaml.safe_dump(android_args["gcloud"]["device"]))

    print("# Results\n")
    print("| matrix | result | logs | details \n")
    print("| --- | --- | --- | --- |\n")
    for matrix, matrix_result in matrix_ids.items():
        print("| {matrixId} | {outcome} | [logs]({webLink}) | {axes[0][details]}\n".format(**matrix_result))
    print("---\n")
    print("# References & Documentation\n")
    print("* [Automated UI Testing Documentation](https://github.com/mozilla-mobile/shared-docs/blob/main/android/ui-testing.md)\n")
    print("* Mobile Test Engineering on [Mana](https://mana.mozilla.org/wiki/display/MTE/Mobile+Test+Engineering) | [Slack](https://mozilla.slack.com/archives/C02KDDS9QM9) | [Alerts](https://mozilla.slack.com/archives/C0134KJ4JHL)\n")


if __name__ == "__main__":
    main()

