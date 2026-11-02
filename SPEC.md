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
| **What** | Read Stips questions and answers, sign in, answer, chat, pin and follow questions, profiles and notifications — in a fast, native Material 3 app |
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
| D-22 | For v0.2 research the owner shared their Stips sign-in. The agent used it only in its own browser, mostly to read; it is not stored anywhere in the repo or the app | Learn the signed-in features (profiles, pins, follow, edit, notifications) from the real site. One pin was set by mistake and removed at once | Owner, agent |
| D-23 | Home = the **top 5 hot questions as big cards**, then the New feed. A bell for notifications sits in Home's search bar; Settings moved to Profile | The owner's layout | Owner |
| D-24 | **Live** new questions (chip on Home, switch in Settings) is opt-in: the New feed is checked every 30 s, only while Home is on screen (≈1 MB an hour) | The owner asked; refines D-13: it runs only while the user is looking | Owner |
| D-25 | Unread chats and notifications are fetched once a minute while the app is on screen and signed in (`messages.count`, under 1 KB) | Badges must be current; the site polls the same call. Refines D-13 | Agent |
| D-26 | **Phone notifications are opt-in.** A JobScheduler job checks every 15 minutes (≈96 tiny requests a day) and fetches the list only when the count says something is new. The first check only records where things stand. No Firebase, no WorkManager | Stips has no push service for other apps; JobScheduler is built in. Refines D-13 for users who turn it on | Owner (feature), agent (design) |
| D-27 | **Pins** (Stips' own "save", with a count) replace the local bookmark on the question screen. The local Saved list stays for signed-out reading | Pins sync with the site and show how many people saved a question | Owner |
| D-28 | Questions have a **Get updates** toggle (`item.notifications_register`); answering turns it on unless the composer's chip is off, like the site | The owner's ask; same behavior as the site | Owner |
| D-29 | **One profile screen for everyone**, social-style: photo, counts, bio, badges, age, account age, the credibility gauge, 26-week activity graphs, flowered answers and the thanks wall. When signed in, the Profile tab is your own profile | The owner's ask: the same look for me and for others | Owner |
| D-30 | Profile photo: picked with the system photo picker (no storage permission), straightened, cropped to a centered square and sent as a 720 px JPEG; then the thumbnail is set to the whole square (the site's two steps) | No cropping UI needed; the site's own flow | Agent |
| D-31 | Settings: safe mode, profile photo and bio are native; account details, password, contact, rules, about and account deletion open the Stips site in the browser | Rarely used, and the site handles them; the browser keeps its own Stips sign-in | Agent |
| D-32 | Motion: a new screen slides in over the current one, which drifts back by a quarter; Back reverses it; tabs fade through; during the predictive back gesture the screen follows the finger | Replaces Navigation's default shrink-and-vanish, which the owner disliked | Owner (problem), agent (motion) |
| D-33 | Stips question, profile and messages links open in the app: from other apps (when allowed under "Open by default"), from phone notifications, and from links inside answers | One way in, wherever the link comes from | Agent |
| D-34 | Answers can be edited only while Stips allows it (`meta.permissions.edit`, which expires after a while). Detaching an answer from your account (`item.set_anonymous`) asks for confirmation first | Same rules as the site | Owner (ask), agent |

## Next steps

1. Owner: send the permission request to Stips (SPEC-06 §4).
2. Owner: change the Stips password that was shared for research (D-22).
3. Owner: test the v0.2 features that change the account: photo upload, editing and detaching an answer, pins, Get updates, deleting a thanks message, phone notifications.
4. Next round candidates: asking a question, writing on a thanks wall, searching users, home-screen widget (SPEC-06 §1).

## Implementation notes

### Second build (v0.2)

| Spec says / the site does | v0.2 does | Why / next step |
|---|---|---|
| F-21 Ask a question | Not yet | Next round |
| User search (the site's header autocomplete, `smartdata users/Search`) | Not yet | Next round |
| The site labels answers by the asker ("שואל/ת השאלה") | No label | Small follow-up |
| Upvoting an answer also pins the question when you never pinned it (`user_pinned == -1`) | Not copied | Pins change only when you tap the pin |
| The site shows activity as a line chart | Weekly bars; tap a bar for its week | Easier to read on a phone |
| Custom rank badges (`custom_rank`) | Not shown | The label text isn't in the response we saw |
| Writing on someone's thanks wall | Read and delete only | Next round |
| Whether opening the notification list marks items seen | Assumed yes: the site clears its badge after loading the list, with no other call | Confirm on the phone; the background check reads the list only when the count is above zero |
| Phone notifications arrive within ~15 minutes | JobScheduler's minimum period; Android may stretch it in battery saver | Stips has no push for other apps |
| "Play a sound for new messages" (site setting) | Not offered | Web only; Android's channel settings cover sounds |

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
| Pin (נעיצה) | Stips' way to save a question to your account; questions show how many people pinned them |
| Get updates (קבלת עדכונים) | Following a question: Stips notifies you about new answers |
| Credibility (נקודות אמינות) | A 0–100 score Stips gives each user; the site draws it as a gauge in four color bands |
| Thanks wall (קיר התודות) | Short thank-you messages other users leave on a profile; the owner can delete them |
| Live | Home's opt-in mode where new questions appear without pulling to refresh |
