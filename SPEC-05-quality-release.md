# SPEC-05: Quality and release

## 1. Test strategy

| Layer | Tests (JUnit 4, JVM, run in CI) |
|---|---|
| Parsing | Recorded, anonymized fixtures in `app/src/test/resources/fixtures/`: feed, question, answers, channels config, errors, odd types (ids as strings, nulls, missing fields) |
| API helpers | The `+` encoding; envelope errors → `ApiException` codes |
| Time | Israel-time parsing across DST; relative formatting buckets; minutes and seconds under an hour; the server-clock offset and HTTP dates (v0.9) |
| Validation | The site's repeated-letter rule; length limits |
| Colors | Scheme generator: tones land within ±2 of target, text/background contrast ≥ 4.5:1, grey seeds stay calm |
| Data files | `assets/channels.json`: parses, unique ids, non-empty names, valid colors; `res/xml/shortcuts.xml` matches the actions MainActivity handles |
| v0.9 rules (`ParsersV9Test`, `RulesV9Test`) | Special accounts (Net Knights, Sahar volunteers, the system account, anonymous answers), profile backgrounds, hidden age and numbers, custom badges; `feed.get` sections merged by heading and paged without repeats; the home feed's order number; the order of answers and the asker's label; chat links from the widget; the chats widget's list, its saved copy and clock times; expert mode's id lines and JSON |

**On the built APK (CI, before every release, D-55):**

| Check | What it catches |
|---|---|
| `tools/check_apk.py` | Reads the APK's dex files: every class that a library creates by name (Room databases, WorkManager input mergers and workers, start-up initializers, Glance action callbacks) still has the constructor it's called with. R8 removed some in builds 7–9 (#7) |
| `tools/launch_test.sh` on an Android 15 emulator | Opens the app over the newest earlier release that opens, as a fresh install (which must show the welcome screen, v0.9), a second time, and from each launcher shortcut (checking the screen's text). Fails on a crash, "not responding" or a process that quits; keeps screenshots, logcats and the screen's text |

Account flows (signed in) are still tested by the owner on the phone (SPEC-03 §11 spikes): the emulator is signed out.

## 2. Static analysis

- Kotlin compiler warnings are visible in the build log; Android Lint runs later (backlog) once the base is stable.
- Quick local checks before every push (playbook Appendix D): XML well-formed, JSON parses, string apostrophes escaped.

## 3. CI/CD

`.github/workflows/build.yml`, on every push to `main` and on demand. A pre-release appears only when everything passes:

1. **Build and test** (one runner): checkout, JDK 17, Gradle 9.5.0 (`gradle/actions/setup-gradle@v6`, `cache-provider: basic`); restore the signing key from `SIGNING_KEYSTORE_BASE64` (warns in the summary if missing); `gradle assembleRelease` (errors into a box on the run's Summary page); name the APK and record its certificate digest; `tools/check_apk.py`; `gradle testDebugUnitTest` (failures into the Summary box).
2. **Open on an emulator** (a second runner with KVM, `reactivecircus/android-emulator-runner@v2`, API 35 `google_apis` x86_64): installs the emulator and the image first with retries (sdkmanager's downloads sometimes break, android-emulator-runner#496), downloads the four newest releases and runs `tools/launch_test.sh` (§1). If the emulator never starts, it tries once more; a failing app is never retried. Screenshots and logs are kept as the run's `launch-test` artifact for 14 days.
3. **Publish the pre-release** `build-<run>` with `stipsplus-<run>.apk`. The notes are `RELEASE-NOTES.md` (the round's summary) followed by the launch test's results.
4. **Report** (always): `status.md` on the `ci-status` branch with each job's result, the signing key and digest, the APK size, and the error lines.

`.github/workflows/launch-test.yml` opens any release by hand, on any emulator API level (for example 37.0, Android 17, which is slow to boot on these runners).

## 4. Versioning and signing

- `versionCode` = CI run number; `versionName` = `0.1.<run>` (the minor goes up each round).
- **One key for every build:** the release build signs with the debug-style keystore restored from the secret (alias `androiddebugkey`, standard debug password). Without the secret, CI falls back to a throwaway key and says so; such a build won't install over the others.
- **Backup:** the keystore file lives at `%USERPROFILE%\.android\stipsplus-signing.keystore` on the owner's computer. Losing it means users must uninstall to update.

## 5. Distribution

| Channel | Status |
|---|---|
| GitHub pre-releases (private repo) | Every build that passes the checks above |
| Obtainium | Works with the private repo only with a GitHub token; simpler to open the release page on the phone |
| Google Play / F-Droid | Not before Stips' permission (D-02) |

**Install on the phone:** open the release page in the phone's browser (signed in to GitHub), tap the APK, allow "Install unknown apps" for the browser, and choose *Install anyway* if Play Protect asks.

## 6. Crash reporting

- On a crash, a report (app version, Android version, device, stack trace) is saved on the phone. The handler is installed before the libraries' start-up code runs (#7).
- The next launch offers *Copy details*, *Report on GitHub* (opens a pre-filled issue, trimmed to fit a URL) or *Close*.
- Nothing is sent automatically. Release builds aren't obfuscated, so stack traces are readable.
- A crash on every launch never reaches that dialog: the emulator launch test (§1) is what catches it. On the phone, a bug report (Developer options → Take bug report) holds the stack trace.

## 7. Privacy

Every network call the app makes:

| Host | When | What |
|---|---|---|
| `stips.co.il` | Browsing, signing in, posting | API calls; the session cookie and token |
| `cdn.stips.co.il` | Showing questions and profiles (if images are on) | Photos |
| `images.unsplash.com` | Questions with an Unsplash cover (if images are on) | Photos |

No analytics, ads, trackers or crash services. Saved questions, settings and timings stay on the phone.

## 8. Licensing

- Code: GPL-3.0-or-later (`LICENSE`).
- Third-party libraries: Apache-2.0 (AndroidX, Kotlin, OkHttp, Coil) — compatible with GPL-3.0.
- No Stips logos or artwork in the repo; the launcher icon is our own.
- Unsplash photos are shown with the photographer's credit, per the Unsplash License.

## 9. Operations

- **When Stips changes something:** a feed or question fails to load → run the probes in MAINTAINING.md, update the parser, add a fixture.
- **When a build is red:** fix and push again before any new work (playbook §3).
- **Before going public:** the playbook §7 checklist, Stips' written permission, and a decision on the name.
