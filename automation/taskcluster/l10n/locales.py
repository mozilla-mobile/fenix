#!/usr/bin/python

#  This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

"""This file contains various locale lists consumed by other tools"""

# Sorted list of locales that ship in release builds of Fenix.
#
# Other builds might include more locales.
#
# Note that there are differences in the locale codes used by Pontoon
# and by Android (e.g. Hebrew: he (Pontoon) vs. iw (Android)).
# This list uses the Android notation. A valid entry can be a
# language code (de) or a language plus country (de-DE). Do not use
# the name of the Androidresource folder (de-rDE) here.
#
# Releases should contain all locales that are at 100% or have been
# shipping in previous releases. Ping :delphine in case you want
# to add or remove locales from releases.
RELEASE_LOCALES = [
    "af",
    "am",
    "an",
    "anp",
    "ar",
    "ast",
    "ay",
    "az",
    "bg",
    "bn-BD",
    "bn-IN",
    "bo",
    "bs",
    "ca",
    "cak",
    "co",
    "cs",
    "cy",
    "da",
    "de",
    "dsb",
    "el",
    "en-CA",
    "eo",
    "es-AR",
    "es-CL",
    "es-ES",
    "es-MX",
    "eu",
    "fa",
    "fi",
    "fr",
    "fy-NL",
    "ga-IE",
    "gu-IN",
    "gl",
    "hi-IN",
    "hr",
    "hsb",
    "hu",
    "hus",
    "hy-AM",
    "ia",
    "in",
    "it",
    "ixl",
    "iw",
    "ja",
    "ka",
    "kab",
    "kk",
    "ko",
    "kw",
    "lo",
    "lt",
    "meh",
    "mix",
    "mr",
    "ms",
    "my",
    "nb-NO",
    "ne-NP",
    "nl",
    "nn-NO",
    "oc",
    "pai",
    "pa-IN",
    "pl",
    "ppl",
    "pt-BR",
    "quc",
    "quy",
    "ro",
    "ru",
    "sk",
    "sl",
    "sn",
    "sq",
    "sr",
    "sv-SE",
    "su",
    "ta",
    "te",
    "th",
    "tr",
    "trs",
    "tsz",
    "tt",
    "uk",
    "ur",
    "vi",
    "wo",
    "yua",
    "zam",
    "zh-CN",
    "zh-TW"
]

# This is the list of locales that we want to take automated screenshots of
# in addition to the list of release locales. We want to take screenshots
# of other locales so that translators of not yet completed locales can
# verify their work in progress.

ADDITIONAL_SCREENSHOT_LOCALES = [
    "jv",
    "ace",
    "zh-HK",
    "nv"
]

# Those are locales that we take automated screenshots of.
SCREENSHOT_LOCALES = sorted(RELEASE_LOCALES + ADDITIONAL_SCREENSHOT_LOCALES)