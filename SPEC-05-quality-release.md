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
| v0.9 rules (`ParsersV9Test`, `RulesV9Test`) | Special accounts (Net Knights, Sahar volunteers, the system account, anonymous answers), profile backgrounds, hidden age and numbers, custom badges; `feed.get` sections merged by heading and paged without repeats; the home feed's order number; the order of answers and the asker's label; chat links from the widget; the chats widget's list, its saved copy and clock times; expert mode's id lines and JSON; seconds only under a minute |
| v0.10 rules (`RulesV10Test`) | Caret pointers in the forms the probe found (runs, "4^", "4 ^", "^4", stacked, after punctuation, at the end; not smileys, maths or carets inside words), writing and correcting the opening pointer, resolving pointers to answers and quotes; reading notifications (type, count in Hebrew and English), where they open (count back from their time, the last visit, the user's own answers skipped, flowers), the route values; moderator ranks and the badge store's format |
| v0.11 rules (`RulesV11Test`) | Which notifications reach the phone: the first check only records, new ones and ones Stips updated in place post, read ones leave, an upgrade from v0.10 doesn't replay, at most 5 (the newest), 200 remembered; chats one by one (a new message posts again, the user's own last message and read chats don't, newest first, read ones leave); when the chats list is read; the alarm's interval; the stores' formats; reply pointers kept in the text by default (tappable, at the start too) and hidden only on request |
| v0.12 rules (`RulesV12Test`) | HTML entities decoded once (named, numeric, hex; `&amp;lt;` stays `&lt;`; unknown ones and plain ampersands untouched) and in the parsers (answers, questions, tags, chats); easy reply in chats: the carets counted from the bottom, and read back to the same message |
| #9 (`SessionCookieRaceTest`, MockWebServer) | A slow answer from before the sign-in, arriving after it, doesn't replace the signed-in session cookie; answers after the sign-in still set cookies |
| v0.13 rules (`RulesV13Test`) | Credibility by the rule book: each rating's range and rate, editing from 50, blocks (23 points → 7 days, below 20 for good), the block screen's state from Stips' block or the points (unknown points never block), days to a target (3–7 a point from 90), milestones, months on Stips from the profile's phrase; the team's notices told apart (a question title saying "נמחק" doesn't make one) and opening at the top; the "unread" marks in questions (the user's own answers not counted) and chats (Stips' count, else the last visit); `appUser` with a block and a moderator rank, and missing points left unknown |
| #10 (`EditHistoryTest`) | Earlier versions read from `jsonstring` (decoded once; links only when the edit changed them; unreadable JSON keeps nothing); answers marked edited by `revision_flg`; the versions newest first, each with the edit that made it, the oldest the original, unchanged fields as they are now; Stips' editor names |
| v0.14 rules (`RulesV14Test`) | Emojis left out (surrogates, emoji-by-default symbols, ❤️ and keycaps with their selectors, ZWJ families; Hebrew and text symbols kept) with the cursor kept in place; jumps (stay when in sight, the least move, tall items, reversed lists, items not laid out); the special accounts list (Stips' creator, organizations, unique ids, short badges, opaque colors); the asker's nickname only on their own anonymous question; the bio line (gendered, below the bio, 250 characters); picture links (imgbb pages and files, Imgur and Postimages files; not albums, other sites, `user@host`, odd ports or schemes) and imgbb's oEmbed answer (only https files on `i.ibb.co`); link labels with the real host; the thread export (numbers, replies, hidden answers, Markdown and JSON) |
| v0.15 rules (`RulesV15Test`) | The animation presets (Full, Less: no screens and quick, None, Customized) and their saved bits, the old switch's mapping; which question swipes apply (no reporting or hiding your own, no hiding an anonymous asker or yourself) and the answer swipe choices; notification categories (updates, flowers and thanks, the rest: removals, notes, unknown kinds), day sections (a clock ahead is today, 7 days back is earlier), the summary (answers read from the sentence, distinct questions), and pinned team notices (unopened, under 30 days) |**On the built APK (CI, before every release, D-55):**
| v0.16 rules (`RulesV16Test`) | How a swipe ends (past the line, a flick that moved far enough, a flick back cancels); which way a swiped snackbar goes (a share of its size, or a flick the same way); when entering a tab refreshes it (not within 10 s, a clock gone back counts as old); the Stips+ badge from a bio (the line, its words in either form, its link); the intro's carets are exactly the pointers Stips+ reads (English and Hebrew); the new defaults and moving the old ones once. `RulesV9Test` covers the four sorts and their ties |
| v0.17 rules (`RulesV17Test`) | Emoji codes in chats (the list's lines, `:nerd:` after text and after a time, two in a row, capitals and unknown names left, `{ic:N}` dropped, nothing without the list); Stips' system account through a chats list (the check, and codes in the last line); notes to self, the shelf (online, most recent first, without you), chat search by name and notes by theirs, the widget without notes; day headers (the oldest shown, midnight, no time) and what they say; the bell's rows (a question's updates together where the newest is, flowers apart, sections by the newest), where a row opens (all its new answers), its badge in rows, what counts as waiting (past what the bell showed and what checks read; nothing the bell showed comes back), when the page is read; phone notifications new by Stips+'s record (what other apps marked seen, done once the bell showed it, changed after, old ones never). `RulesV11Test`'s first check and "read" follow the new rules |
| Check | What it catches |
|---|---|
| `tools/check_apk.py` | Reads the APK's dex files: every class that a library creates by name (Room databases, WorkManager input mergers and workers, start-up initializers, Glance action callbacks) still has the constructor it's called with. R8 removed some in builds 7–9 (#7) |
| `tools/launch_test.sh` on an Android 15 emulator | Opens the app over the newest earlier release that opens, as a fresh install (which must show the intro's "Welcome", v0.14), a second time, and from each launcher shortcut (checking the screen's text). Fails on a crash, "not responding" or a process that quits. Its summary has only verdicts; screenshots, logcats and the screen's text are kept only when it fails (they show Stips' live posts, #14) |

Account flows (signed in) are still tested by the owner on the phone (SPEC-03 §11 spikes): the emulator is signed out.

## 2. Static analysis

- Kotlin compiler warnings are visible in the build log; Android Lint runs later (backlog) once the base is stable.
- Quick local checks before every push (playbook Appendix D): XML well-formed, JSON parses, string apostrophes escaped.

## 3. CI/CD

`.github/workflows/build.yml`, on every push to `main` and on demand. A pre-release appears only when everything passes and the owner approves it (#14). The workflow's token is read-only; only the release and report jobs can write. Checkouts don't keep the token, and actions from outside GitHub are pinned to commits:

1. **Build and test** (one runner): checkout, JDK 17, Gradle 9.5.0 (`gradle/actions/setup-gradle`, v6 pinned to its commit, `cache-provider: basic`); restore the signing key from `SIGNING_KEYSTORE_BASE64` (warns in the summary if missing); `gradle assembleRelease` (errors into a box on the run's Summary page); name the APK and record its certificate digest; `tools/check_apk.py`; `gradle testDebugUnitTest` (failures into the Summary box).
2. **Open on an emulator** (a second runner with KVM, `reactivecircus/android-emulator-runner` v2, pinned, API 35 `google_apis` x86_64): installs the emulator and the image first with retries (sdkmanager's downloads sometimes break, android-emulator-runner#496), downloads the four newest releases and runs `tools/launch_test.sh` (§1). If the emulator never starts, it tries once more; a failing app is never retried. Screenshots and logs are kept as the run's `launch-test` artifact for 5 days, only when it fails.
3. **Publish the pre-release** `build-<run>` with `stipsplus-<run>.apk`, in the `release` environment: it waits for the owner's approval. The notes are `RELEASE-NOTES.md` (the round's summary) followed by the launch test's results.
4. **A hotfix the owner asks to rush** (build 30) can skip step 2 with `[skip launch]` in its commit message: the release still needs the build, the APK check and the unit tests, and the owner's approval, and its notes say the launch test was skipped. Never for a change to startup, libraries or R8's rules, which only the emulator catches (#7).
4. **Report** (always, without waiting for the approval): `status.md` on the `ci-status` branch with each job's result, the signing key and digest, the APK size, and the error lines.

`.github/workflows/launch-test.yml` opens any release by hand, on any emulator API level (for example 37.0, Android 17, which is slow to boot on these runners).

## 4. Versioning and signing

- `versionCode` = CI run number; `versionName` = `0.1.<run>` (the minor goes up each round).
- **One key for every build:** the release build signs with the debug-style keystore restored from the secret (alias `androiddebugkey`, standard debug password). Without the secret, CI falls back to a throwaway key and says so; such a build won't install over the others.
- **Backup:** the keystore file lives at `%USERPROFILE%\.android\stipsplus-signing.keystore` on the owner's computer. Losing it means users must uninstall to update.

## 5. Distribution

| Channel | Status |
|---|---|
| GitHub pre-releases (public repository since v0.14) | Every build that passes the checks above, once the owner approves it |
| Obtainium | Can follow the public repository's pre-releases |
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
