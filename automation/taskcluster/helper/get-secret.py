# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

import argparse
import base64
import os
import taskcluster

def write_secret_to_file(path, data, key, base64decode=False, append=False, prefix=''):
    path = os.path.join(os.path.dirname(__file__), '../../../' + path)
    with open(path, 'a' if append else 'w') as f:
        value = data['secret'][key]
        if base64decode:
            value = base64.b64decode(value)
        f.write(prefix + value)


def fetch_secret_from_taskcluster(name):
    secrets = taskcluster.Secrets({'baseUrl': 'http://taskcluster/secrets/v1'})
    return secrets.get(name)


def main():
    parser = argparse.ArgumentParser(
        description='Fetch a taskcluster secret value and save it to a file.')

    parser.add_argument('-s', dest="secret", action="store", help="name of the secret")
    parser.add_argument('-k', dest='key', action="store", help='key of the secret')
    parser.add_argument('-f', dest="path", action="store", help='file to save secret to')
    parser.add_argument('--decode', dest="decode", action="store_true", default=False, help='base64 decode secret before saving to file')
    parser.add_argument('--append', dest="append", action="store_true", default=False, help='append secret to existing file')
    parser.add_argument('--prefix', dest="prefix", action="store", default="", help='add prefix when writing secret to file')

    result = parser.parse_args()

    secret = fetch_secret_from_taskcluster(result.secret)
    write_secret_to_file(result.path, secret, result.key, result.decode, result.append, result.prefix)


if __name__ == "__main__":
    main()
