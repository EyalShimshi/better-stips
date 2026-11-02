# SPEC-05: Quality and release

## 1. Test strategy

| Layer | Tests (JUnit 4, JVM, run in CI) |
|---|---|
| Parsing | Recorded, anonymized fixtures in `app/src/test/resources/fixtures/`: feed, question, answers, channels config, errors, odd types (ids as strings, nulls, missing fields) |
| API helpers | The `+` encoding; envelope errors → `ApiException` codes |
| Time | Israel-time parsing across DST; relative formatting buckets |
| Validation | The site's repeated-letter rule; length limits |
| Colors | Scheme generator: tones land within ±2 of target, text/background contrast ≥ 4.5:1, grey seeds stay calm |
| Data files | `assets/channels.json`: parses, unique ids, non-empty names, valid colors |

UI and account flows are tested by the owner on the phone (SPEC-03 §11 spikes).

## 2. Static analysis

- Kotlin compiler warnings are visible in the build log; Android Lint runs later (backlog) once the base is stable.
- Quick local checks before every push (playbook Appendix D): XML well-formed, JSON parses, string apostrophes escaped.

## 3. CI/CD

`.github/workflows/build.yml`, on every push to `main` and on demand:

1. Checkout, JDK 17, Gradle 9.5.0 (`gradle/actions/setup-gradle@v6`, `cache-provider: basic`).
2. Restore the signing key from `SIGNING_KEYSTORE_BASE64` (warns in the summary if missing).
3. `gradle assembleRelease` → on failure, the error lines go into a box on the run's Summary page.
4. Publish a pre-release `build-<run>` with `stipsplus-<run>.apk`.
5. `gradle testDebugUnitTest` → failures into the Summary box.
6. Write the outcome to the `ci-status` branch (`status.md`), so the agent can read results with git.

## 4. Versioning and signing

- `versionCode` = CI run number; `versionName` = `0.1.<run>` (the minor goes up each round).
- **One key for every build:** the release build signs with the debug-style keystore restored from the secret (alias `androiddebugkey`, standard debug password). Without the secret, CI falls back to a throwaway key and says so; such a build won't install over the others.
- **Backup:** the keystore file lives at `%USERPROFILE%\.android\stipsplus-signing.keystore` on the owner's computer. Losing it means users must uninstall to update.

## 5. Distribution

| Channel | Status |
|---|---|
| GitHub pre-releases (private repo) | Every green build |
| Obtainium | Works with the private repo only with a GitHub token; simpler to open the release page on the phone |
| Google Play / F-Droid | Not before Stips' permission (D-02) |

**Install on the phone:** open the release page in the phone's browser (signed in to GitHub), tap the APK, allow "Install unknown apps" for the browser, and choose *Install anyway* if Play Protect asks.

## 6. Crash reporting

- On a crash, a report (app version, Android version, device, stack trace) is saved on the phone.
- The next launch offers *Copy details*, *Report on GitHub* (opens a pre-filled issue, trimmed to fit a URL) or *Close*.
- Nothing is sent automatically. Release builds aren't obfuscated, so stack traces are readable.

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
