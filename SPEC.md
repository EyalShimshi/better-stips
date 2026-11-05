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
| D-25 | Unread chats and notifications are fetched every 10 s (v0.2–v0.6: once a minute; D-53) while the app is on screen and signed in (`messages.count`, under 1 KB) | Badges must be current; the site polls the same call. Refines D-13 | Agent |
| D-26 | **Phone notifications are opt-in.** A JobScheduler job checks every 15 minutes (≈96 tiny requests a day) and fetches the list only when the count says something is new. The first check only records where things stand. No Firebase, no WorkManager | Stips has no push service for other apps; JobScheduler is built in. Refines D-13 for users who turn it on | Owner (feature), agent (design) |
| D-27 | **Pins** (Stips' own "save", with a count) replace the local bookmark on the question screen. The local Saved list stays for signed-out reading | Pins sync with the site and show how many people saved a question | Owner |
| D-28 | Questions have a **Get updates** toggle (`item.notifications_register`); answering turns it on unless the composer's chip is off, like the site | The owner's ask; same behavior as the site | Owner |
| D-29 | **One profile screen for everyone**, social-style: photo, counts, bio, badges, age, account age, the credibility gauge, 26-week activity graphs, flowered answers and the thanks wall. When signed in, the Profile tab is your own profile | The owner's ask: the same look for me and for others | Owner |
| D-30 | Profile photo: picked with the system photo picker (no storage permission), straightened, cropped to a centered square and sent as a 720 px JPEG; then the thumbnail is set to the whole square (the site's two steps) | No cropping UI needed; the site's own flow | Agent |
| D-31 | Settings: safe mode, profile photo and bio are native; account details, password, contact, rules, about and account deletion open the Stips site in the browser | Rarely used, and the site handles them; the browser keeps its own Stips sign-in | Agent |
| D-32 | Motion: a new screen slides in over the current one, which drifts back by a quarter; Back reverses it; tabs fade through; during the predictive back gesture the screen follows the finger | Replaces Navigation's default shrink-and-vanish, which the owner disliked | Owner (problem), agent (motion) |
| D-33 | Stips question, profile and messages links open in the app: from other apps (when allowed under "Open by default"), from phone notifications, and from links inside answers | One way in, wherever the link comes from | Agent |
| D-34 | Answers can be edited only while Stips allows it (`meta.permissions.edit`, which expires after a while). Detaching an answer from your account (`item.set_anonymous`) asks for confirmation first | Same rules as the site | Owner (ask), agent |
| D-35 | Issues are the backlog (playbook §8). Round 3 fixes #1–#4; the commit closes them | The owner's instruction for this round | Owner |
| D-36 | **Images get their own client (#3).** Idle connections are dropped after a network error and on every network change. (v0.3 also moved API calls to HTTP/1.1, reading #3 as one stalled HTTP/2 connection; #5 showed it was Stips' request limit, so v0.4 reverses that: D-48) | The only evidence (a PCAPdroid capture) showed one long-lived connection carrying everything | Agent |
| D-37 | **Never sign out on errors (#3).** Only `user.login` answering `{"logged": false}` (measured) signs the user out. Other failures keep the session. A server session older than 10 minutes is checked with `user.get_app_user` (the one call that fails when signed out; the others return empty data) | v0.2 signed people out whenever re-login hit an error, and missed silently expired sessions | Agent |
| D-38 | **Back off when Stips limits us (#3; v0.4 replaces it with D-46).** HTTP 429/403 pause the background checks (Live, badges, phone notifications) for 1, 2, 4, 8 then 15 minutes, or Stips' `Retry-After`; the first success lifts it. Errors say what happened ("Stips is limiting this phone") instead of "can't reach" | Stips sits behind Cloudflare; a block by IP (cured by a new IP: VPN or airplane mode) fits the report | Agent |
| D-39 | **Network log (#3):** the last 600 requests and session/network events in memory, shareable from Settings → Diagnostics. Method names, status codes, timings, Cloudflare ray ids and error types only: never parameters, cookies, tokens or text | The owner asked for a way to send evidence; the capture couldn't show HTTP-level detail | Owner (ask), agent |
| D-40 | Live's rate is adjustable from 5 to 30 seconds (default 30; v0.3 allowed 2, v0.4 10), in Settings and by long-pressing the Live chip; the cost is shown live (5 s ≈ 6.7 MB an hour). Failed checks slow it down; it waits while the request budget is low (D-46) | The owner's ask and pick; honest cost. At 2 s Live alone sent 30 requests a minute (#5) | Owner (ask), agent |
| D-41 | **Asking questions** follows the site's three steps and limits (title 10–120, details ≤2000, 1–5 tags, the daily cap). Photos come from **Stips' own gallery** (`photos.tag_gallery`). The site's Unsplash search uses Stips' private API key, which the app doesn't borrow | Same rules as the site; no third-party keys | Agent |
| D-42 | **Moderation like Stips' own app:** report questions, answers and pen-pal posts (`reports.send`, 5–500 characters); "block content" hides an item or everything by a user **on the phone** and tells Stips (`ask/BlockContent`); Settings has "Remove all content blocks" | The site's app keeps blocks locally too (`bc-item-…`/`bc-user-…`); same behavior | Owner (ask), agent |
| D-43 | **Anonymous mode**: a global switch (30 minutes to "until I turn it off") makes every answer and question anonymous; a question you answered anonymously defaults to anonymous next time (from a local list, and from Stips marking your own anonymous answers) | The owner's ask | Owner |
| D-44 | **Pen pals (חברים לעט)** get a bottom tab, like the site. Replies are private messages. The site's rules apply: 5 flowers and a cooldown to post, 255 characters, ages 13–21 only | The owner's ask; the site's rules | Owner (ask), agent |
| D-45 | Also from the site scan: the asker can **send flowers**, anyone can **write on a thanks wall**, Home's search also finds **people** (`users/Search`), and you can **edit, delete or detach your own question** | "Scan the site for features" (#4) | Agent |
| D-46 | **Pace requests like the official app, and pause if Stips refuses (#5).** A budget: 40 requests at once, then 30 a minute (at most 190 in any five minutes; v0.4 had 30, then 12 a minute); Live, badges and phone checks run only while half of it is left, and taps wait a few seconds when it's empty. If Stips refuses anyway: nothing is sent but one try a minute, a banner counts down, and the first normal answer ends it. Refusals that arrive together count once | The official app's busiest stretches: 59–76 calls in a minute, 185–245 in 4½ minutes, never refused; test clients with the site's Referer passed 200 in under 5 minutes (SPEC-04 §2.10). v0.4's tighter budget assumed one limit for all clients, which v0.5 disproved (D-49) | Owner (report), agent |
| D-47 | **Fewer requests per screen, like the site (#5).** Profile graphs load in the Activity section, one at a time (the site's chart does the same); the thanks wall and flowered answers load when scrolled to; a question opened from a list reuses the list's copy (under 3 minutes old; same fields). A profile costs 3 requests instead of 7–8, a question 2 instead of 3 | The #5 log: 9 graph requests in 18 seconds from three profile visits | Agent |
| D-48 | **API calls share one HTTP/2 connection again**, as the official app's do (reverses v0.3's HTTP/1.1 from D-36) | #3 was the request limit, not a stalled connection; HTTP/1.1 only added a TLS handshake per extra connection (about 20 in four minutes in #5) | Agent |
| D-49 | **Every request to Stips carries the site's Referer (`https://stips.co.il/`)**, as every call from the website and the official app does. The User-Agent still says "StipsPlus … unofficial", so Stips can tell Stips+ apart and block it if they choose; if they do, we stop and ask them (R-02). This replaces v0.4's "never work around the limit" | Stips refuses an address after about 100 calls without the Referer, at any pace from 150 s to 6 minutes, and not calls with it, even while it refuses the others (SPEC-04 §2.10). Without it Stips+ couldn't support normal use: even v0.4's budget was refused after 6 minutes | Owner (decision), agent (measurement) |
| D-50 | A manual workflow, "Probe Stips' rate limit" (`tools/probe_rate_limit.py`), re-measures the limit from GitHub's runners: `app` checks Stips+'s own pattern, `limit` triggers and times the limit without the Referer, `site` is a real browser page | Stips can change the rule. Each runner has its own address, and the owner's office network isolates web pages | Agent |
| D-51 | **A "Hot right now" home-screen widget** (Jetpack Glance 1.2.0, the version in Google's CI-tested samples catalog): up to 10 hot questions with rank, title, answers and pins; tap a question to open it, the title to open the app, ⟳ to refresh. It shows the list any screen last loaded (Home's top cards load the same list), and by itself asks Stips only when Android wakes it (hourly), its copy is over 30 minutes old and the budget allows (D-46). Wallpaper colors when the app uses them (Android 12+), else the app's accent; the system's corner radius. Settings → Updates can ask the launcher to place it | The owner's ask; SPEC-01's backlog item; DESIGN-PHILOSOPHY §10. No times on rows: a widget may redraw only hourly, so "5 min" would mislead | Owner (ask), agent |
| D-52 | **Opening the app warms up; waking it doesn't.** Refreshing the channels and restoring the session moved from app start to the first screen. A process started for the widget or the notification check makes only its own requests; account calls restore the session when they need it | Each background wake-up cost about 3 extra requests (channels, sign-in, profile) | Agent |
| D-53 | **Chats keep up by themselves (#6).** An open chat asks for new messages every 3 s while it's on screen (`messages.from_user` after the last id), slowing to 24 s after failures, and asks for the other person's profile (photo, online status) once a minute. The unread counts are asked every 10 s while the app is open (D-25), and the chats list reloads quietly when they change. All of these wait while the request budget is low (D-46). Phone notifications in the background stay at every 15 minutes (D-26) | The owner's ask: messages faster, and faster still inside a chat. The site's own chat asks every 1.6 s, its chat list every 5 s and its notifications every 10 s. At these rates an open chat costs about 27 requests a minute, under the budget's 30 a minute; 15 minutes is the shortest Android allows for a background job | Owner (ask), agent (rates) |
| D-54 | **Chats look like a messaging app (#6).** The chat's top bar shows the other person's photo, name and "Online now", and opens their profile. The chats list and pen pals show the same green dot as profiles. Bubbles are compact and show no time: a tap shows it ("14:05", "Yesterday, 14:05" or the date), a long-press copies the text, and each visit starts with every time folded. Earlier messages load by themselves when the top comes into view, without a "Load earlier" button. At the very start, a card sums up the other person (photo, credibility, answers, flowers, time on Stips). An opened notification leaves "New" at once | The owner's six asks in #6. The site loads history the same way, on scrolling up (`history: true`), and marks "online" on the photo. Messaging apps show a contact card where a chat begins | Owner (ask), agent (design) |
| D-55 | **A release must open before it's published (#7).** Builds 7–9 crashed on every launch: under AGP 9, R8 no longer keeps a class's default constructor for old-style library rules (`-keep class * extends …`), and removed `WorkDatabase_Impl()` and WorkManager's input mergers, which WorkManager (brought by Glance) creates by name at app start. Fixed with explicit keep rules plus `android.r8.strictFullModeForKeepRules=false` (AGP 8's reading of library rules). CI now publishes only after an APK check (those constructors exist in the dex) and a launch test on an Android 15 emulator (over the newest earlier release that opens, fresh, a second time, and from each shortcut). The crash catcher installs before the libraries start | A build that compiles and passes unit tests crashed on the phone, and the crash happened before the in-app crash catcher ran. The emulator reproduced it exactly, so it would have stopped builds 7–9 | Owner (report), agent |
| D-56 | **Report and block from a chat.** ⋮ → *Report* sends `reports.send` with `itemtype: "usermsgs"` and one of the site's five reasons (`reason_id` 2 spam, 4 sexual, 8 abusive, 16 personal details, 1024 other) plus optional details; ⋮ → *Block messages* (confirmed) sends `messages.block_user`. While blocked (the site's `userBlocked` on the first page), a bar with *Unblock* replaces the composer and the chat stops checking for new messages | The site's chat menu offers the same; R-09. The site hides the whole chat while blocked; Stips+ keeps the history readable | Owner (picked from options), agent |
| D-57 | **Two launcher shortcuts: Hot and Chats** (static, `res/xml/shortcuts.xml`), with Material glyphs on the light circle of the shortcut icon guidelines. Each sends MainActivity its own action, mapped to a screen like a link; opening those screens in the app reports the shortcut as used, for the launcher's suggestions | The owner picked them from the options; DESIGN-PHILOSOPHY §10. The two places reached most often besides Home | Owner (picked), agent |
| D-58 | **A "jump to newest" button in chats:** a round ↓ appears once the reader scrolls up more than a message or two, with a badge counting their messages that arrived meanwhile; a tap jumps (animated when close, at once when far) | The owner picked it; with the 3 s checks (D-53), messages arrive while reading older ones | Owner (picked), agent |
| D-59 | **Home's layout is a choice (#8), the default unchanged:** *Hot, then new* (D-23), *Like the website* (the hot cards, then today's questions in sections by topic from `feed.get`, with no New list and no Live), or *New only*. Also: how many hot cards (3, 5 or 10), which hot list (now, today or this week; inside a channel Stips offers the week's), and whether the channel chips show. Picking a channel in the website layout shows that channel's hot and new questions. A list that isn't shown isn't loaded | The owner's ask: "a website-like layout… add many options, but keep the current layout as the default". The site's home is `feed.get`: signed in `userRelated`, else (or when that's empty) `todayItems`, with `random_order_number` = 1000 × weekday + 100 × hour + minute / 5, kept for every page of one visit (SPEC-04 §2.11) | Owner (ask), agent |
| D-60 | **Refreshing by hand:** pull-to-refresh on a question, which always asks Stips (never the 3-minute copy of D-47). v0.9 also put a ⟳ beside "New questions"; v0.10 removed it, since pulling Home down does the same | The owner's ask (#8); then "it's redundant" | Owner |
| D-61 | **A welcome screen on the first launch after installing:** sign in, or *Continue as a guest*. It never shows after an update, and links and shortcuts skip it. Settings → Home and reading → *Open the app on* picks the start tab (Home, Pen pals, Chats or Profile), from the next launch | The owner's ask (#8). A welcome over the app (not a separate start screen) keeps links and back navigation simple | Owner (ask), agent |
| D-62 | **Expert mode** (off by default). Off, the network log and load times are hidden. On: *Diagnostics* in Settings, load times on questions, and ids under questions, answers and profiles, plus ⋮ → *Server details*: the fields the app read and the JSON Stips sent, selectable and copyable. The JSON is kept in memory with each item, never written anywhere new | The owner's ask (#8): debug items only for those who want them, and "the full server details" | Owner (ask), agent |
| D-63 | **The asker's own answers are marked** "שואל השאלה" (Stips' `parentItemCreatorResponse`), also when the asker posted anonymously; "שואלת השאלה" when the nickname is "אנונימית", as on the site | The owner's ask (#8); the site's label | Owner |
| D-64 | **Verified accounts get a blue check** (#1D9BF0): organizations — accounts Stips brands (`specialConfig.itemSpecialRankBranding`, measured on Net Knights) and a short list of known ones (Sahar volunteers 10486, Net Knights 253000) — and Stips' own system account (`isPlatfromAdmin`, Stips' spelling). Their answers get a thin blue outline. Names Stips highlights show in its purple; profiles show Stips' gradient background as a band behind the photo, `custom_rank` badges ("מגן מקוון"), an *Organization* pill, and hide age and numbers where Stips does (`hideAge`, `hideStats`) | The owner's ask (#8). Sahar's account has no special flag on Stips, so the app keeps its own list | Owner (ask), agent |
| D-65 | **Times show seconds under a minute** ("12 s"), then minutes as before ("5 min"); v0.9 showed seconds up to an hour ("5 min 12 s"). Measured against Stips' clock: when the phone's clock is 2 s or more off the `Date` header of Stips' answers, the app corrects for it. *Count the seconds* (off by default) makes them tick every second while the app is on screen; it makes no requests | The owner's ask (#8), then "not more than a minute" (v0.10). A seconds display shows a wrong phone clock plainly | Owner (ask), agent |
| D-66 | **Settings in pages (#8):** the main page lists Account, Home and reading, Appearance, Notifications and widgets, Privacy and safety, Stips, About, then expert mode (and Diagnostics when it's on). *Order of answers* (as on Stips, newest, oldest, most votes) joins Home and reading | The owner's ask: "organize the settings". Pages keep each one short | Owner (ask), agent |
| D-67 | **A chats widget:** up to 10 latest chats with name, last message, time and an unread badge; the title counts unread; tap a chat to open it, the title to open Chats, ⟳ to refresh. It shows the list the Chats screen last loaded; by itself it asks Stips (one `messages.list`) only while it's on the home screen, and only when the unread count changes (the app's 10 s check, D-25, or the background check, D-26), Android wakes it hourly and its copy is over 30 minutes old, or on ⟳, within the budget (D-46). Its copy of your chats stays on the phone: backups skip it and signing out deletes it. Signed out it says to sign in | The owner picked it from the options. Times are clock times ("14:05"), not "5 min ago", because a widget may redraw only hourly (as D-51) | Owner (picked), agent |
| D-68 | **Easy reply** (on by default; Settings → Home and reading). Stips can't reply to an answer, so people open theirs with carets that count the answers up to the one they mean ("^^" = two up). Swiping an answer from left to right (in Hebrew too; also ⋮ → *Reply*, and TalkBack's *Reply* action) opens the draft with that pointer and a line break: "^", "^^", "^^^", then a number, "4^". Answers are counted in Stips' order, oldest first, wherever the reader's order puts them (SPEC-04 §2.12). Sending counts again; afterwards the app reloads the answers and, if answers posted while the user wrote landed in between, edits the answer's opening pointer while Stips still allows editing (once per answer and visit, only if the answer still opens with a pointer). Which answer each reply points at is kept on the phone (`reply_targets`, the last 300) | The owner's ask, including "retroactively edit my answer" and the "4^" form. Stips orders answers by time, so the count can only be right once the answer exists | Owner (ask), agent |
| D-69 | **Reply quotes** (on by default): an answer that points up shows the answer it replies to above its text, like a quoted message in WhatsApp (author, two lines; a tap scrolls there and lights it up). The pointers are read in any answer: runs of carets, "4^", "4 ^" and "^4"; at the start (on their own line or straight before the words, stacked to answer several, up to 3 quotes) or further on. Opening pointers that resolve are replaced by the quote; the others stay in the text, bold and tappable. Not pointers: carets inside words or numbers ("x^2", "2^3") and caret smileys ("^_^", "^.^"). A pointer past the first answer points nowhere. A quoted answer that's hidden shows as "A hidden answer" | The owner's ask. In a probe of 1,203 answers, 213 had carets: 173 at the start, 25 further on (SPEC-04 §2.12); the forms above all occurred | Owner (ask), agent |
| D-70 | **Notifications open where they point:** new answers on a question open at the first new one (the third from the bottom for "3 new"), a flower at the user's answer that got it; the answer "breathes" (two slow pulses of the primary container, 2 s; static with animations off). The count comes from the notification's text when it has one, counted back from the notification's time so later answers don't shift it; the app also remembers the newest answer it showed on each question (`read_marks`, the last 500) and uses the later of the two; the user's own answers are skipped. Phone notifications carry the same as extras. In expert mode each notification shows its type and *Server details*, including where it opens | The owner's ask. Stips' notifications link only to the question (`linkItemId`), and their texts aren't in the site's code; the count rule is a best guess until real texts are checked in expert mode | Owner (ask), agent |
| D-71 | **Moderators are marked on answers:** "נאמן סטיפס" / "נאמנת סטיפס" (and "נאמן בכיר" / "נאמנה בכירה") with a shield in Stips' purple, as on profiles. Answers don't carry badges (SPEC-04 §2.12), so the app reads each answerer's profile badges (`profile.page_data`, plus the profile for a moderator's gender) and keeps them two weeks (`user_badges`, up to 3,000 people; profile visits fill it too). Only for answers on screen and the next three, one lookup every 1.5 s at most, at most 30 per visit, only when the budget has room (D-46). A setting turns it off | The owner's ask ("so I won't need to go into the profile"). The probe's 70 most active answerers: 43 senior answerers, no moderators, so moderators are few; the per-visit cap keeps a busy question from costing more than a minute of background requests | Owner (ask), agent |

## Next steps

1. Owner: send the permission request to Stips (SPEC-06 §4). It now also says that Stips+ sends the site's Referer (D-49) and asks whether that's acceptable.
2. Owner: change the Stips password that was shared for research (D-22).
3. Owner: if the "Stips paused this phone" banner still appears, report the network log on GitHub (Settings → Diagnostics). It says how many requests went out before the refusal and how long it lasted.
4. Owner: test the v0.3 account features: asking a question, pen-pal posts, reports, flowers, thanks messages, deleting your own question.
5. Owner: test the v0.7–v0.8 chats with someone who answers: the online dot, the top bar, messages arriving within a few seconds, tap and long-press on bubbles, scrolling up to the start, the ↓ button, block and unblock.
6. Owner: try the v0.9 options (Settings → Home and reading) and the chats widget with real unread messages.
7. Owner: try easy reply on a busy new question, and open a few notifications about new answers and flowers. If one opens at the wrong answer, turn on expert mode, tap { } on that notification, and send me what it shows (D-70).
8. Next round candidates: the Android 17 emulator in the release check (about 10 more minutes per build), "Add to home screen" for one question or chat (pinned shortcuts), choosing an Unsplash cover with our own Unsplash key, a generated widget preview for the widget picker, stacking two easy replies in one answer.

## Implementation notes

### Tenth build (v0.10, the owner's round of requests)

| Spec says / the site does | v0.10 does | Why / next step |
|---|---|---|
| The site has no replies; people point with carets (^^, 4^) | Easy reply writes them, reply quotes read them (D-68, D-69) | The count is fixed after posting, since answers posted meanwhile land in between |
| The site marks nothing on a moderator's answers | "נאמן סטיפס" by their name, from their profile badges (D-71) | Answers carry no badges; one lookup per answerer, cached two weeks |
| The site opens a notification at the top of its question | Stips+ opens at the first new answer, or at the user's flowered answer, and lights it up (D-70) | The count rule is a guess until real notification texts are seen; expert mode shows them |
| v0.9: a ⟳ beside "New questions" | Removed (D-60) | Pulling Home down does the same |
| v0.9: seconds up to an hour | Seconds only under a minute (D-65) | The owner's ask |

### Ninth build (v0.9, #8 and the chats widget)

| Spec says / the site does | v0.9 does | Why / next step |
|---|---|---|
| The site's home: hot questions, then topic sections (`feed.get`) | *Like the website* layout (D-59); a topic's heading opens the topic, an item list's is plain, as on the site | The site's "load more" pages by scrolling; so does Stips+ |
| The site sends `item.send_view` when a Net Knights answer is shown | Not sent | An impression count for Stips' campaign, not needed to show the answer |
| The site animates highlighted names (#5e5e5e ↔ #9069d7) | A still purple, at a tone that reads on the theme (D-64) | Nothing moves in a list (DESIGN-PHILOSOPHY §4) |
| The site outlines Net Knights answers in purple | A thin blue outline for every verified account, matching the check | One look for all verified accounts |
| "Show seconds" | Under an hour only; after that minutes, hours, days or a date (D-65; under a minute from v0.10) | Seconds on a two-day-old answer carry nothing |
| The site's settings page | Pages in the app; the site's own pages still open in the browser (D-31, D-66) | — |
| Unread chats on the home screen | The chats widget (D-67) | A generated widget preview can come later |
| CI checks where the app lands | The fresh install must show the welcome screen ("Welcome") | The shortcut checks still land on Hot and Chats: they skip the welcome |

### Eighth build (v0.8, #7 and the round's options)

| Spec says / the site does | v0.8 does | Why / next step |
|---|---|---|
| Playbook §3: CI builds, publishes, then runs the unit tests | Builds, checks the APK, runs the unit tests, opens the APK on an emulator, and only then publishes (D-55) | Builds 7–9 compiled, passed their tests and crashed on every launch |
| Library keep rules keep what the library needs | Explicit rules for Room databases and WorkManager input mergers, and AGP 8's reading of library rules | AGP 9's R8 dropped constructors that libraries call by name (#7) |
| Test on the owner's phone (Android 17) | The release check uses an Android 15 emulator; Android 17 can be tried by hand (`launch-test.yml`) | The Android 17 image didn't finish booting within 10 minutes on GitHub's runners |
| The site's chat menu: report, block | ⋮ → Report (five reasons + details), Block messages; a bar with Unblock (D-56) | The site hides the chat while blocked; Stips+ keeps the history readable |
| DESIGN-PHILOSOPHY §10: the top 4 favorites as launcher shortcuts | Two static shortcuts, Hot and Chats (D-57) | Stips+ has no favorites list; per-item pinned shortcuts are a candidate |
| Messaging apps' ↓ button | A round ↓ with a badge for new messages (D-58) | — |

### Seventh build (v0.7, #6 chats)

| Spec says / the site does | v0.7 does | Why / next step |
|---|---|---|
| The site's open chat asks for new messages every 1.6 s | Every 3 s while the chat is on screen, slower after failures (D-53) | About half the site's rate keeps an open chat within the request budget |
| The site's chat list refreshes every 5 s, its bell every 10 s | The unread counts every 10 s; the chats list reloads when they change | One small request covers both badges and the list |
| The site marks "online" with a pulsing green ring around the photo | A still green dot at the photo's corner, as Stips+'s profiles already showed | Readable at 36 dp; nothing moves in a list (DESIGN-PHILOSOPHY §4) |
| The site loads older messages on scrolling up | The same, with a small spinner, down to a summary card at the start (D-54) | The "Load earlier" button is gone |
| A time under every message | Times on tap only; a long-press copies (D-54) | The owner's ask; the text stays selectable in answers, not in bubbles |
| Background phone notifications every 15 minutes | Unchanged | Android's minimum for a background job; anything faster needs a push service, which Stips doesn't offer other apps |

### Sixth build (v0.6, the widget)

| Spec says / the site does | v0.6 does | Why / next step |
|---|---|---|
| A home-screen widget with hot questions (SPEC-01 backlog, F-42) | "Hot right now": a list widget with the app's mark, ⟳ and up to 10 questions (D-51) | Shortcuts are next |
| DESIGN-PHILOSOPHY §10: dynamic colors and the system's widget corner radius | Glance's `Scaffold` (system radius on Android 12+) and `GlanceTheme`: wallpaper colors, or the app's accent when dynamic color is off or unavailable | Android 8–11 show square corners |
| A preview in the widget picker | The app icon and the name only | A generated preview (Android 15) can come later |
| Background work starts the whole app | Only the first screen warms up (D-52) | Fewer requests for the notification check too |

### Fifth build (v0.5, #5 follow-up)

| Spec says / the site does | v0.5 does | Why / next step |
|---|---|---|
| The official app "never halts" (a second recording: 185–245 calls in 4½ minutes, never refused) | Found why with six test clients: Stips limits only calls without its site's Referer. Stips+ sends it (D-49); the budget matches the official app's pace (D-46); Live from 5 s (D-40) | v0.4's "one limit for both apps" was wrong (SPEC-04 §2.10) |
| v0.4's budget (at most 90 in any 5 minutes) | Replaced | A client at that pace, without the Referer, was still refused after 6 minutes |
| Re-measuring the limit | Manual probe workflow (D-50) | Run `app` after any report of the banner |

### Fourth build (v0.4, issue #5)

| Spec says / the site does | v0.4 does | Why / next step |
|---|---|---|
| #5: "the same network thing" | Found the cause in the network log: Stips' `IP_RATE_LIMIT`, at the 104th request in 3 m 38 s. Request budget, pause with one try a minute, banner (D-46); fewer requests per screen (D-47) | Whether the window slides or follows the clock, and how long a refusal lasts, are still open; the log now records the counts and the length |
| The official app "handles frantic refreshing" | Compared with a PCAPdroid capture and its CSV: about 102 API requests in 2 m 17 s (51 of them feed refreshes), all user-triggered, no refusal. Same `/api`, same parameters, one connection | Read as one limit for both apps, right at it. Wrong: v0.5 showed the official app's calls aren't limited that way (D-49) |
| One connection, like the site | HTTP/2 again (D-48) | v0.3's HTTP/1.1 was a wrong guess about #3 |

### Third build (v0.3, issues #1–#4)

| Spec says / the site does | v0.3 does | Why / next step |
|---|---|---|
| #3: find out why Stips becomes unreachable | Removed the likely causes (D-36–D-38) and added the network log (D-39) | Not reproduced: the only capture showed no failed connection. The next failure's log will say what Stips answered |
| Question photos: Stips gallery or Unsplash search | Stips gallery only | Unsplash needs an API key; Stips' is private (D-41). Could add our own key later |
| Asking: drafts, editing tags, YouTube preview | No drafts; editing your question changes title, details and link (tags stay) | Small follow-ups |
| Report users from chats (`usermsgs`, reasons 2/4/8/16/1024), block messages (`messages.block_user`) | Not yet | Next round |
| Blocking also personalizes on Stips (unverified) | Hiding is local, Stips is told | Same as the site's app |
| Pen pals: "one active post per user", expiry | Unknown; the app follows the server | Unverified on the site |
| Send flower: the site counts `owner_flower_count` | Counts flowered answers in the list (max 5) | The field wasn't seen in responses |
| Notifications: Stips marks items viewed when listed | "New" = Stips' flag or newer than the last item the app showed (per account) | Keeps new items marked after the phone check read them |

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
| Pen pals (חברים לעט) | A board of short posts by people looking for someone to talk to; replies are private messages |
| Block content (חסימת תוכן) | Hiding an item, or everything by one user, on this phone; Stips is told so it can show less |
| Anonymous mode | A timed switch that makes everything you post anonymous |
| Back-off | A pause of the app's automatic checks after Stips says "too many requests" |
