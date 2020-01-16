#!/usr/bin/python

#  This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

from __future__ import absolute_import, print_function, unicode_literals


# In Focus, this file contained a simple list of release locales. Fenix documents this list in
# '/l10n.toml', so this file instead converts its values to be easily consumed from Python.
#
# Alternatives to custom parsing that were considered:
# - Using standard library module --- none exists to parse TOML
# - Importing a TOML parsing module --- introduces an additional build step, complexity, and
#   security risk
# - Vendoring a TOML module --- large amount of code given the use case. Introduces a security
#   risk
#
# 'l10n.toml' has a very simple structure, and it is reasonable to believe that this (very basic)
# algorithm will continue to work as it is changed.
def get_release_locales():
    with open(r"l10n.toml") as f:
        file = f.read()

        locales_begin = file.find("locales = [") + len("locales = [")
        locales_end = file.find("]", locales_begin)

        locales = file[locales_begin:locales_end]

        return locales.split(",")


RELEASE_LOCALES = get_release_locales()
