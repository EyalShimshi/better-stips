# SPEC-06: Roadmap and risks

## 1. Milestones

| Milestone | Contents | Exit criteria |
|---|---|---|
| **M0 Pipeline** | Repo, CI, signing, pre-releases | A green build installs on the owner's phone |
| **M1 v0.1** | SPEC-01 v0.1 scope | Spikes S-01…S-06 pass on the owner's phone |
| M2 v0.2 | Owner feedback: hot-first Home with Live, compact question header, pins, Get updates, answer links/editing/detaching, notifications, social profiles, photo and bio, account settings, motion | Owner uses it daily instead of the official app |
| M2.5 v0.3 | Issues #1–#4: crash and connection fixes, network log, Live rate, ask a question, pen pals, report/block, anonymous mode, flowers, thanks writing, people search | The owner uses posting and moderation daily |
| M3 v0.4 | Widget (shipped in v0.6), shortcuts (shipped in v0.8), tablet layout, lint in CI | DESIGN-PHILOSOPHY §13 checklist passes on every screen |
| M3.5 v0.9 | Issue #8: Home layouts and options, welcome, expert mode, who's who (asker, verified accounts), seconds, settings in pages; the chats widget | The owner sets Home the way they like it |
| M3.6 v0.10 | Easy reply and reply quotes, notifications that open at the new answer, moderators on answers | Replying in busy threads takes one swipe |
| M3.7 v0.11 | Phone notifications that arrive: checks every few minutes while the phone is awake, per-chat notifications that pop up, a status panel and a one-time offer | The owner gets new answers and messages on the phone within minutes |
| M3.8 v0.12 | Easy reply in chats, DM settings, back to the top, text as written; #9 | Chats work as answers do, and stay signed in |
| M3.9 v0.13 | "Unread" bars, notices from Stips' team, block screens, credibility explained and projected, the 50-point and 5-flower limits; #10: edit history, reload on re-tapping a tab, colored scores, a scroll button on threads | The app says what Stips' rules mean for the user before they hit them |
| M3.10 v0.14 | #12: settings rethought with pictures, a first-run intro, gentle jumps, no emojis where Stips drops them, the asker's nickname, bio links and "I use Stips+", Stips' creator's badge; #15: pictures from links; #16: exporting a thread; #14: the public repository made safe (releases on approval, least-privilege CI, personal data out); proposals for #13, #15 and #17 (SPEC-07) | The owner sets the app up in a minute, and a public repository can't change what's on phones without the owner |
| M3.11 v0.15 | #13: the bell's list in categories, with the team's notices pinned and channels per category; #18: animation presets and a custom set, short transitions, swipe actions on questions and answers, the bio button aligned; #19 answered; #17 closed | The owner finds what matters in the bell at a glance, and the app moves as much as they like |
| M3.12 v0.16 | The owner's list: one swipe engine with Material's flick, snackbars that swipe away, less animation, ticking seconds and hidden carets by default, sorting in the question, the Swipes intro step and answer picture, Pen pals Live, tabs that refresh on entry and on coming back, the Stips+ badge | Swiping and refreshing feel like the platform; the app stays close to Stips' own look |
| M3.13 v0.17 | #20: search and a shelf of who's online on the DM page, notes to self; #21: Stips' system account; #22: legacy emoji codes in chats; the bell's badge and phone notifications by Stips+'s own record; one row and one phone notification per question; dates in chats | The bell's number and phone notifications show up even when the official app is used too; the DM page works like Instagram's |
| M3.14 v0.18 | #27: the bell in sentences, flowers in color, older notifications by themselves, tapped notifications read, team notices in full; #29: bios as on Stips; #31: moderator marks daily; #30: a calm refresh; #26: credit; #24: the app's page; #25: updates inside the app; Share → Stips+; link cards and *Reply in private* in chats; #28: tips | Friends install and update Stips+ without GitHub; links in chats read like the rest of the app |
| M3.15 v0.19 | Share to chats (up to five, text only) in place of opening shared links; a message's menu with *Forward* and *Share*; bigger flowers on answers; the creator's badge on his profile only; *Buy me a coffee*; the app's page moves to the owner's site, www.shimshii.com | Passing something on to friends on Stips works as in WhatsApp |
| M4 Public | Only with Stips' written permission | Playbook §7 checklist, README in Hebrew and English |

## 2. Spikes

See SPEC-03 §11 (S-01…S-06). All need the owner's Stips account on the phone; the agent never handles the password.

## 3. Risks

| ID | Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|---|
| R-01 | Stips objects: their terms forbid copying/distributing content and trademarks without written permission, and automated copying | Medium | High | Private until permission (D-02); requests only on user action; no mirroring or export; "Open on Stips" everywhere; take down on request |
| R-02 | Stips changes or blocks the API | Medium | High | Tolerant parser, fixtures and probes (MAINTAINING.md). An honest User-Agent; requests carry the site's Referer like the site's own (D-49), which the permission request discloses. If Stips blocks Stips+ itself, we stop and talk to them — no evasion |
| R-03 | Stips penalizes accounts that use a third-party client | Low | High | Ask in the permission request; the owner tests with their own account first |
| R-04 | The name "Stips+" uses Stips' trademark | Medium | Medium | Fine while private; decide the public name with Stips' answer (Q-02) |
| R-05 | Account calls behave differently from the website's code | High | Medium | Tolerant parsing, clear error messages, spikes on the device, fix in the next round |
| R-06 | Compile errors cost CI round trips (no local compiler) | Medium | Low | Versions from a CI-tested sample; signatures checked in source jars; `ci-status` branch for fast feedback |
| R-07 | The signing key is lost | Low | High | CI secret + a backup file on the owner's computer |
| R-08 | Users are mostly minors; their content is personal | Medium | Medium | Nothing stored beyond caches and the user's own saved list; hidden profile fields are never shown; no analytics |
| R-09 | Unsafe content or harassment | Medium | Medium | Report and block content in the app since v0.3 (`reports.send`, `ask/BlockContent`); report users and block their messages from a chat since v0.8 (D-56) |
| R-10 | Stips limits the phone's address (`IP_RATE_LIMIT`) and seems "down" (#3, #5) | Low since v0.5 | Medium | Requests made as the site's pages make them (D-49), a budget at the official app's pace (D-46), a pause with one try a minute and a banner, and the network log |
| R-11 | A release crashes on launch although it compiles and passes its tests (builds 7–9, #7: R8 removed constructors that a library calls by name) | Low since v0.8 | High | Explicit keep rules and AGP 8's reading of library rules; CI checks those constructors in the APK and opens every build on an emulator before publishing it (D-55) |
| R-12 | Phone notifications come late or not at all: Stips has no push for other apps, and Android (Samsung's especially) holds background work back to save battery | Medium | Medium | Checks every few minutes while the phone is awake; Android's "run in the background?" asked when turning them on; a status panel that names what's holding them back, with *Check now* and a test (D-72–D-74). Can't be tested on CI's signed-out emulator: the owner's phone is the test |
| R-13 | A lost session looks like "nothing there": Stips answers account calls without one with empty data, not an error (#9) | Low since v0.12 | High | Cookies from answers to calls older than the latest sign-in are ignored; empty first pages and pull-to-refresh check the session with Stips and sign in again (D-79); a MockWebServer test covers the race |
| R-14 | The team's notices and blocks can't be seen on a healthy account: their notification types and texts aren't in the site's code, and the block screens need under 30 points or a block (v0.13) | High | Low | Classified by `msgText`, `pointsRemoved` and their words, never by a question title (D-82); unknown points never block (D-83); expert mode shows each notification's type and kind, so a real notice can correct the rules |
| R-15 | A public repository: anyone can read the code and file issues; a tricked agent, a hijacked action or a stolen account could change a release (#14) | Medium | High | Releases wait for the owner's approval; the CI token is read-only except for publishing; outside actions pinned to commits; the agent works on the issues the owner points to; nothing on phones is downloaded at run time (special accounts are code, D-99) |
| R-16 | Picture links lead to someone else's pictures (#15) | Low | Medium | Only on the user's tap, only image files (decoded at screen size) from four known hosts, no cookies; *Open in browser* as before |
| R-17 | Other apps on the account (the official one, the site) read the bell first, so Stips' "unread" says nothing new (v0.17) | High | Medium | Stips+ keeps its own record of what the bell showed and reads the first page itself (D-122, D-123); reading it marks those as seen on Stips, as opening the bell on the site does |
| R-18 | An update from inside the app installs something it shouldn't: a broken build, a changed file (v0.18) | Low | High | Releases are published only after the owner's approval and the checks; the app downloads only this repository's release files, and only when the user taps *Update*; the file must match GitHub's size and SHA-256; Android installs it only over an app signed with the same key, and asks to confirm (D-134) |
| R-19 | Link cards in chats add requests to Stips (v0.18) | Low | Low | One read per link per app session, only for cards on screen, one at a time, and only while the request budget has room; questions a list showed cost nothing (D-136) |

## 4. The permission request

**Q-01:** Will Stips allow an independent client? The owner sends this to `contact@stips.co.il` (the address in Stips' Google Play listing). The terms name the operator as "נדב דפני - אינטרנט ותוכנה בע\"מ"; the Play listing names Futurebit Media Ltd.

The owner sends the request from their own address. Its text isn't kept in this public repository (#14): it carries the owner's name and contact details, and how Stips+ makes its requests is for Stips to read, not for other clients (D-49, D-50). What it says:

- **Who and why:** an active user built a free, independent Android client out of love for the site and its community, now in its final polish before launch.
- **Highlights:** a live Home (the hottest questions as big cards, new questions arriving by themselves); swipe-to-reply with Stips' carets, corrected when answers land in between; WhatsApp-style reply quotes; notifications that open at the right answer; social profiles with credibility, flowers, activity graphs, the thanks wall and moderator badges; widgets, dark mode and full right-to-left Hebrew.
- **Good faith:** requests go straight from the user's phone to Stips, like a browser, only from what the user does or turns on; no middle server, collecting, copying or crawling. An honest User-Agent with a link to the project, "Open on Stips" on every question, and the open code. Requests are made as the site's own pages make them, at about the official app's pace, and that changes if Stips prefers. The password goes only to Stips and isn't stored; no ads, analytics or tracking.
- **The ask:** written permission before launch (and before Google Play), as the terms require, and any conditions (a different name, a request rate, attribution). It offers a test build or a demo.
## 5. Open questions

| ID | Question | Blocks |
|---|---|---|
| Q-01 | Does Stips permit an independent client, and on what conditions? | Going public |
| Q-02 | Public name: keep "Stips+" or rename? | Going public |
| Q-03 | Google Play, or GitHub/Obtainium only? | Going public |
| Q-04 | ~~Notifications: in-app only, or a periodic background check?~~ **Both:** the in-app list always; the background check is opt-in (SPEC D-26) | Answered in v0.2 |
