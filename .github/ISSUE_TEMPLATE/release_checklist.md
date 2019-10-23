## Overview ##
Soft code freeze for a release is the *second Wednesday of the sprint*, and most of releng happens between *Wednesday and Friday*, String freeze is the *first Tuesday* of the sprint.

TODO: Delete Sprint Work

| Monday       | Tuesday                   | Wednesday                    | Thursday       | Friday      |
|--------------|---------------------------|------------------------------|----------------|-------------|
| Sprint Start | Sprint Work/String Freeze | Sprint Work                  | Sprint Work    | Sprint Work |
| Sprint Work  | Sprint Work               | Sprint Work/Soft Code Freeze | Sprint Work/QA | Hard Code Freeze/Localization Due 
|              | Release to Play Store     |                              |                |Planning|            |

### Requirements
- Jira account
- Bugzilla account

## Start of sprint [Monday, 1st week of sprint]
- [ ] Create milestone for *upcoming* release. (e.g. if you are doing releng for v2.2, create v2.3 milestone)
- [ ] If this is a major (x.0) release, create an issue in the *upcoming* milestone: "What's New Entry for [release]" to track work for the SUMO page and Google Play release notes. (TODO: Who updates this issue, what's the body?)
TODO: Do we still need the SUMO issue?

## String freeze [Tuesday, 1st week of sprint]
- [ ] Make sure [all issues with label needs:strings](https://github.com/mozilla-mobile/fenix/labels/needs%3Astrings) have strings provided in comment 0 . If there are no strings, ping the Content team to provide them. (Content is @jpfaller)
TODO: What is the strings process? needs:strings > jpfaller > approved:strings? What is the needs:strings label Betty is using (do we need to make another label?) UPDATE BELOW IF CHANGES
- [ ] Open a PR with all new strings in the `strings:approved` lobel that have not yet been merged to master
- [ ] For each string included in the PR, update its issue's `strings:needed` labels from the PR to `approved:strings`

## String freeze follow-up [Wednesday, 1st week of sprint]
- [ ] For any strings still labeled `needs:strings`
    - [ ] If strings have been provided by content team, open a PR with the newly provided strings.
    - [ ] Otherwise, escalate to Product because this may miss the release.

## Soft code freeze [Wednesday, 2nd week of sprint]
- [ ] Merge any remaining [automated L10N string PRs](https://github.com/mozilla-mobile/fenix/pull/6156).
- [ ] Create a branch off of master (DO NOT PUSH YET) for the *current* milestone of format `releases/v2.3` (where 2.3 is the *current* milestone). After that, anything landing in master will be part of the next milestone.
- [ ] On the Release branch, pin the AC version to the stable version ([example](https://github.com/mozilla-mobile/fenix/commit/e413da29f6a7a7d4a765817a9cd5687abbf27619)) with commit message "Issue #`<this releng issue>`: Pin to stable AC `<version>` for release v2.3" (replacing 2.3 with the version)
- [ ] [Create an issue](https://github.com/mozilla-mobile/fenix/issues/new?template=release_checklist.md&title=Releng+for+) in the *upcoming* milestone: "Releng for v[release]".
- [ ] Go through the list of issues closed during this sprint and make sure they all have the correct milestone.
- [ ] Add `eng:qa:needed` flags on each issue that still needs it.
- Note: You will need review to make changes to the release branch after this point.
    - [ ] Push the branch.

### SUMO Verification [Thursday, 2nd week of sprint]
- [ ] Verify with SUMO the contents of the whats new / other sumo pages are accurate with what is in this release.
TODO: Do we need to do this? if so, clarify steps.

### Product Integrity [Thursday, 2nd week of sprint]
TODO: Should PI happen Wednesday?
- [ ] On GitHub, draft a GitHub Release with with a tag of the format `v2.3.0-rc.1` with the release branch as the target. Check the pre-release checkbox. This will kick off a build of the branch. You can see it in the mouseover of the CI badge of the branch in the commits view.

    If you need to trigger a new RC build, you need to draft and publish a new (pre-release) release. Editing an existing release and creating a new tag will not trigger a new RC build.

- [ ] Upload the APK from the Taskcluster signing task to the [releases page](https://github.com/mozilla-mobile/fenix/releases).
- [ ] Create a new PI request in Jira. You can clone [this issue](https://jira.mozilla.com/browse/PI-219).

### During Product Integrity

- [ ] Check Google Play for new crashes. File issues and triage.
- [ ] If bugs are considered release blocker then fix them on master and the milestone branch (cherry-pick / uplift)
- [ ] If needed tag a new RC version (e.g. v1.0-RC2) and follow the submission checklist again.

## Release to Google Play Store [Tuesday, 3rd week]

- [ ] Tag the latest released RC version additionally with the tag of the release (v1.0-RC2 -> v1.0) (This can be done as soon as there are no more release blockers, does not need to be on Release Day.)
- [ ] Upload signed APKs to the [release page](https://github.com/mozilla-mobile/fenix/releases)
- [ ] Create a release request in Bugzilla. You can clone [this issue](https://bugzilla.mozilla.org/show_bug.cgi?id=1571967) and `need-info` someone from release management.

    Release management promotes the build from beta to the release channel. For minor releases this can happen at any time during the day. Major releases often need to be synchronized with other marketing activities (e.g. blog postings). Releases are rolled out to 99% of the population (otherwise the rollout can't be stopped).

## After the release

- [ ] Check whether there are new crashes that need to be filed.
- [ ] Follow up with Product about releasing to 100%.
- [ ] In the Bugzilla bug, ask release management to set the rollout to 100%.

## Room for improvement
- [ ] Automate assigning milestones to closed issues (based on date, etc) #6199
- [ ] Automate assignig `eng:qa:needed` to issues #6199
