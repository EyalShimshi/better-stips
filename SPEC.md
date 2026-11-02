# Stips+ — spec index

*An independent, native Android client for the Israeli Q&A site [stips.co.il](https://stips.co.il), in the spirit of [better-rail](https://github.com/better-rail/app). Not affiliated with Stips. How we work: [BUILD-PLAYBOOK.md](BUILD-PLAYBOOK.md). How it looks: [DESIGN-PHILOSOPHY.md](DESIGN-PHILOSOPHY.md).*

## Documents

| File | Contents |
|---|---|
| [SPEC-01-product.md](SPEC-01-product.md) | Problem, goals, personas, scope, functional requirements (F-01…) |
| [SPEC-02-design.md](SPEC-02-design.md) | Information architecture, screens, localization, accessibility |
| [SPEC-03-architecture.md](SPEC-03-architecture.md) | Stack and versions, packages, subsystems, security, spikes |
| [SPEC-04-data.md](SPEC-04-data.md) | The Stips API (verified calls), data model, pipeline, legal |
| [SPEC-05-quality-release.md](SPEC-05-quality-release.md) | Tests, CI, versioning, signing, distribution, privacy |
| [SPEC-06-roadmap-risks.md](SPEC-06-roadmap-risks.md) | Milestones, risks, open questions, the permission request |

## At a glance

| | |
|---|---|
| **What** | Read Stips questions and answers, sign in, answer, chat — in a fast, native Material 3 app |
| **Who** | Stips readers and answerers who find the official app (a WebView wrapper) slow and clunky |
| **Platform** | Android 8.0+ · Kotlin · Jetpack Compose · Material 3 |
| **Data** | Stips' own JSON API (`https://stips.co.il/api`), called directly from the phone. No server of ours |
| **Status** | Private. Personal use until Stips gives written permission (D-02) |
| **Distribution** | GitHub pre-releases in the private repo, installed by sideloading |
| **License** | GPL-3.0-or-later |

## Decision log

| ID | Decision | Why | By |
|---|---|---|---|
| D-01 | Native Android: Kotlin, Jetpack Compose, Material 3 | The owner's ask ("more native and fluent"); DESIGN-PHILOSOPHY's reference platform | Agent |
| D-02 | **Private for now; ask Stips for written permission before going public** | Stips' terms forbid copying/distributing content without written permission (SPEC-04 §6) | Owner |
| D-03 | v0.1 = reading **+ sign-in + answering** | The owner wants the participation loop in the first build | Owner |
| D-04 | **Native sign-in form.** The password goes only to Stips, by HTTPS POST; the app keeps only Stips' session token, never the password | Smoother than a web page. POST keeps the password out of URLs and logs | Owner (form), agent (token-only storage) |
| D-05 | Bottom tabs **Home · Chats · Profile**. Channels are a chip row on Home; Saved, my questions/answers and Settings live in Profile | Three destinations, per DESIGN-PHILOSOPHY §3.1 | Owner, agent |
| D-06 | Colors: **wallpaper (dynamic) colors**; when off, a scheme generated from **Stips green `#09C286`**. A question in a channel is tinted with that channel's color | Familiar, calm; content tints the hero | Owner, agent |
| D-07 | Languages: **Hebrew and English**, following the phone | Stips is Hebrew; English helps contributors | Owner |
| D-08 | Name **Stips+**; package id `io.github.eyalshimshi.betterstips` (permanent) | Owner's pick; the id is neutral and stays even if the name changes | Owner, agent |
| D-09 | License **GPL-3.0-or-later** | Keeps forks open, like better-rail's AGPL | Owner |
| D-10 | The agent commits and pushes; the owner creates the repo and the signing secret once | Agent's GitHub access is read-only | Owner |
| D-11 | minSdk 26 (Android 8.0), targetSdk 36, compileSdk 37 | `java.time` without desugaring; ~all active devices | Agent |
| D-12 | Toolchain and library versions copied from Google's CI-tested [compose-samples](https://github.com/android/compose-samples) (Nov 2026) | Compile the first time (playbook §4) | Agent |
| D-13 | **Respectful traffic:** an honest User-Agent; requests only on user action (plus the next page while scrolling); no crawling, no background polling, no bulk export | Stips' terms forbid automated copying; be transparent and light | Agent |
| D-14 | No ads, analytics or trackers. Crash reports stay on the phone until the user sends one | DESIGN-PHILOSOPHY §11 | Agent |
| D-15 | Search = **topic search** (opens the topic's questions) | Stips' API has no free-text search (SPEC-04 §2.3) | Agent |
| D-16 | Tolerant hand-written JSON parsing (kotlinx-serialization's `JsonElement`), no code generation | Unofficial API with loose types; compile-first-time rule | Agent |
| D-17 | Release builds are shrunk and optimized by R8 but **not obfuscated** | Compose needs R8 to feel fast; readable crash reports | Agent |
| D-18 | One signing key from day 1, from the `SIGNING_KEYSTORE_BASE64` secret; backup kept on the owner's computer | Updates install over each other | Agent |
| D-19 | CI uses the open-source Gradle cache (`cache-provider: basic`) and writes each result to a `ci-status` branch | No commercial cache service; the agent can read results through git | Agent |
| D-20 | OkHttp **4.12.0** instead of the sample's 5.5.0 | Coil 2.7.0 is built against OkHttp 4; avoids a binary mismatch at run time | Agent |
| D-21 | Feeds send `safe_filter: true` when signed out, and the account's own setting when signed in | That's what the website does (measured: it hides some questions); show only what the site would show the same person | Agent |

## Next steps

1. Owner: send the permission request to Stips (SPEC-06 §4).
2. Owner: install the first build and test sign-in and answering with a real account (they couldn't be tested without one).
3. Next round candidates: notifications, asking a question, voting polish, home-screen widget (SPEC-06 §1).

## Implementation notes

### First build (v0.1)

| Spec says | v0.1 does | Why / next step |
|---|---|---|
| F-20 Notifications, F-21 Ask a question | Not yet | v0.2; needs the account APIs verified first (S-01…S-05) |
| Per-app language (DESIGN §9) | Follows the phone's language | Add `localeConfig` in v0.2 |
| Reduced motion follows the system | The in-app *Animations* switch only | Also read the system animator scale in v0.2 |
| Color crossfade when the scheme changes (DESIGN §2.2) | Instant switch | Polish round |
| Schemes computed off the main thread | Computed in composition and remembered | Measured cost is under 1 ms per scheme |
| Drafts survive process death (DESIGN §13) | Survive rotation only | `SavedStateHandle` in a later round |
| Medium/expanded layouts (rail, two panes) | Phone layout everywhere | M3 |
| Chats: paging through all conversations | First page only | Add when someone has more than one page |
| Long-press menus on rows (DESIGN §3.3) | Not yet | v0.2 |
| Android Lint in CI | Not yet | M3, once the base is stable |
| Gradle wrapper in the repo | CI installs Gradle 9.5.0 | Add the wrapper when someone builds locally |
| Read settings off the main thread | SharedPreferences read at start-up (a few hundred bytes) | DataStore if it ever shows up in timings |

## Glossary

| Term | Meaning |
|---|---|
| Question (שאלה) | A post on Stips (`objType: "ask"`). The app's main item |
| Answer (תשובה) | A reply to a question (`objType: "ans"`) |
| Channel (ערוץ) | One of 6 curated sections (music, studies…) with its own colors |
| Topic / tag (תגית) | A free-form label on a question; Stips calls it "topic" in URLs |
| Flower (פרח) | A "best answer" award given by the asker |
| Hot (חמות) | Stips' trending list, within a window: now, today, week (or week, month) |
| Session token (`a_token`) | What Stips returns after sign-in; it restores the session later without the password |
| Cover | A question's image: a Stips upload or an Unsplash photo picked by the asker |
