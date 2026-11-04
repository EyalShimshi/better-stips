# SPEC-01: Product

## 1. Summary

Stips+ is a fast, native Android app for reading and answering questions on [stips.co.il](https://stips.co.il), Israel's biggest Hebrew Q&A community, mostly used by teenagers. It talks to Stips' own API directly from the phone, so it shows the same content as the site, without the site's ads and web-page lag.

## 2. Problem

- The official Android app (`com.stipsapp`, 4.1★) is the website inside a WebView (a Cordova wrapper). Scrolling, navigation and back don't feel like Android.
- The site carries ads and trackers (Taboola, AdSense, Google Analytics).
- No dark mode that follows the phone, no dynamic color, no predictive back, poor large-text support.

## 3. Goals and non-goals

| Goals | Non-goals (for now) |
|---|---|
| Read the feed, channels and topics fluently | Replacing Stips or hosting any content |
| Open a question and its answers instantly | Mirroring, exporting or archiving Stips content |
| Sign in, answer (also anonymously), chat | Moderation tools, admin features |
| Feel native: Material 3, dynamic color, RTL, dark, large text | iOS (Stips has no iOS app either; later, maybe) |
| Respect the site: light traffic, honest identification, links back to the site | Circumventing any block or limit Stips sets |

## 4. Personas

| Persona | Needs |
|---|---|
| **Noa, 16, reader** | Scrolls hot questions on the bus; wants it smooth and dark at night |
| **Itay, 17, answerer** | Answers questions in "Studies"; wants to post fast and see replies |
| **Dana, 15, asker** | Asks anonymously, chats privately with people who answered |

## 5. Release scope

| Version | Scope |
|---|---|
| **v0.1** (this round) | Feeds (new, hot + window), channel chips, topic search and topic feeds, question + answers, links, share / open on site, save questions on the phone, native sign-in, post an answer (also anonymously), vote, chats (list, thread, send), profile (my questions, my answers), settings, about, crash catcher, timings |
| **v0.2** (owner feedback round) | Home with the top 5 hot questions and Live new questions; a compact question header with the cover behind the title and a collapsing title bar; the asker's link; pins; Get updates; answer links, editing and detaching; notifications (in-app, plus optional phone notifications); social profiles for everyone (credibility, graphs, flowered answers, thanks wall); profile photo and bio; Stips account settings; new screen motion |
| **v0.3** (issues #1–#4) | Fix the notifications crash and the "can't reach Stips" disconnects (network log, back-off, no false sign-outs); Live 2–30 s; ask a question; pen pals; report and block content; anonymous mode; flowers; thanks-wall writing; people search; own-question actions; image viewer; compact notifications and question meta; haptics |
| v0.4 | Issue #5: a request budget, a pause with one try a minute and a banner when Stips refuses; fewer requests per screen |
| v0.5 | Send the site's Referer, as the official app's calls do (D-49); budget at the official app's pace; Live from 5 s |
| v0.6 | The "Hot right now" home-screen widget (F-42) |
| v0.7 | Issue #6: live chats: online dots, a top bar with the other person, new messages every 3 s, compact bubbles with times on tap, history that loads by itself down to a summary card (F-12, F-43) |
| v0.8 | Issue #7: the launch crash since v0.6 fixed, and every release opened on an emulator before it's published; report and block in chats, a ↓ button for new messages (F-12), launcher shortcuts (F-44) |
| Later | Per-app language, pinned shortcuts for single questions and chats |
| Public | Only after Stips' written permission (D-02) |

## 6. Functional requirements

| ID | Requirement | Pri | Acceptance criteria |
|---|---|---|---|
| F-01 | Home feed: **New** and **Hot** | P0 | Shows 30 questions per page; scrolling near the end loads the next page; pull to refresh |
| F-02 | Hot window | P0 | Now · Today · Week on Home; Week · Month inside a channel |
| F-03 | Channel chips on Home | P0 | "All" + 6 channels with their colors; picking one filters the feed in place |
| F-04 | Topic search and topic feeds | P1 | The search pill takes a word and opens that topic's questions; tapping a topic chip opens it too; `+` in topics works ("C++") |
| F-05 | Question screen | P0 | Cover, title, body, topics, asker, time, channel; all answers with author, time, score, flower; tinted by the channel |
| F-06 | Rich text basics | P1 | URLs are tappable; question and answer text can be selected and copied |
| F-07 | Share and open on site | P0 | Share sends `https://stips.co.il/ask/<id>`; "Open on Stips" opens the browser |
| F-08 | Save questions | P1 | A bookmark on the question screen; Profile → Saved lists them, newest first; stored only on the phone |
| F-09 | Native sign-in | P0 | Email + password; errors in plain words; the password is never stored; the session survives app restarts; sign out clears everything |
| F-10 | Post an answer | P0 | Signed in: a composer under the question; "Answer anonymously" switch; send shows progress in the button; the new answer appears; the site's spam rule (no letter 3× in a row) is checked first |
| F-11 | Vote on answers | P1 | Up/down when signed in; the score updates at once; tapping the same vote again clears it |
| F-12 | Chats | P1 | Signed in: conversations (name, photo with the online dot, last message, time, unread count); a thread with compact bubbles (tap shows the time, long-press copies); send a message; earlier messages load by themselves while scrolling up, down to a summary of the other person at the start (#6); ⋮ → report them or block their messages, with *Unblock* in place of the composer while blocked; a ↓ button with a count of new messages while reading older ones (v0.8) |
| F-13 | Profile | P1 | Signed in: photo, nickname, points; My questions, My answers. Signed out: an invitation to sign in. Always: Saved, Settings |
| F-14 | Settings | P0 | Colors (dynamic color, accent, theme, pure black), interface (images, animations), performance, about |
| F-15 | About and disclaimer | P0 | Version, license, source link, report link, "An independent app, not affiliated with Stips" |
| F-16 | Crash catcher | P0 | After a crash, the next launch offers Copy details / Report on GitHub / Close; nothing sent automatically |
| F-17 | Open Stips links | P2 | Question, profile and messages links open in the app: from other apps (when the user allows it in Android settings), from phone notifications and from links inside answers |
| F-18 | Timings | P1 | The question screen shows "Loaded in 0.4 s"; Settings → Performance lists recent timings |
| F-19 | Instant start | P1 | The last-seen first page of the Home feed shows at once on launch, then refreshes |
| F-20 | Notifications | P1 | A bell on Home with an unread badge opens Stips' notification list (newest first, unread highlighted, tap opens the question or profile); Chats shows an unread badge; optional phone notifications (opt-in, ~15 min) |
| F-21 | Ask a question | P1 | Three steps like the site: question (10–120, details ≤2000, anonymous nickname, link), 1–5 tags (suggested and typed), a cover from Stips' gallery (skipped for YouTube links); the daily cap is respected; the new question opens after posting |
| F-22 | Home: hot first | P0 | The top 5 hot questions as large cards in a snapping row (with "See all"), then the New feed; a **Live** chip makes new questions appear by themselves every 30 s while Home is on screen |
| F-23 | Compact question header | P0 | Title, meta and tags take at most half the screen; the cover is a background the title fades out of; the body is clamped with "Show more"; scrolling collapses the header into a top bar that shows only the question title |
| F-24 | Pins | P1 | A pin with the pin count on every question; signed in, tapping pins or unpins on Stips; Profile lists pinned questions |
| F-25 | Get updates | P1 | A bell on the question turns updates on or off; answering turns them on unless the composer's "Get updates" chip is off |
| F-26 | Answer links, editing, detaching | P1 | The composer can attach one link (a dialog, like the site); your answers have Edit (while Stips allows it) and Detach from my account (with confirmation) |
| F-27 | Profiles | P0 | Tapping a name or photo opens that user's profile: photo, questions · answers · flowers, bio, badges, age, account age, the credibility gauge, Answers/Questions/Flowers graphs for 26 weeks, answers that got a flower, the thanks wall. Signed in, the Profile tab is your own profile |
| F-28 | Thanks wall | P1 | Paged messages with the writer's photo and name; on your own wall each message has Delete (with confirmation) |
| F-29 | Profile photo and bio | P1 | On your profile: a camera button picks a photo (system picker), which is straightened, cropped square and uploaded; "Edit bio" saves the text on Stips |
| F-30 | Account settings | P1 | Safe mode (saved on the account), Profile photo and bio, and links to the site's account details, password, contact, rules, about and account deletion; Sign out |
| F-31 | Motion | P1 | Screens slide over each other (the old one drifts back); tabs fade through; predictive back follows the finger; everything respects the Animations switch |
| F-32 | Live rate (#4) | P1 | 2–30 s, set in Settings or by long-pressing the Live chip; the data cost is shown as it changes |
| F-33 | Reliable connection (#3) | P0 | No sign-out on errors; HTTP 429/403 explained and backed off; a stalled connection never blocks every request; the app recovers without a network change |
| F-34 | Network log (#3) | P1 | Settings → Diagnostics shows recent requests and events; copy, share or report on GitHub; no private data |
| F-35 | Report and block (#4) | P1 | ⋮ on questions, answers and pen-pal posts: report (5–500 characters), hide the item, hide everything from the user; Settings → Hidden content removes all blocks |
| F-36 | Anonymous mode (#4) | P1 | The masks on Home or Settings: 30 min, 1/2/4/8/24 h or until turned off; everything posted meanwhile is anonymous; an anonymous answer makes the next one on that question anonymous by default |
| F-37 | Pen pals (#4) | P1 | A tab with the board (pull to refresh, paging), tap to message the author, post with the site's rules (5 flowers, cooldown, 255 characters, ages 13–21), report, hide, delete your own |
| F-38 | Image viewer (#4) | P2 | Question covers (⋮ → View image) and profile photos (tap) open full screen at the largest size, with pinch and double-tap zoom |
| F-39 | Compact meta and notifications (#4) | P1 | Under questions: the asker at the start, answers/pins/time as symbols at the end; notifications in uniform two-line rows, new ones highlighted in their own section |
| F-40 | Tactile editing (#4) | P2 | Editing an answer puts the cursor at the end and opens the keyboard; toggles, votes, sending and sliders give haptic feedback |
| F-41 | More from the site (#4) | P2 | Send flowers (the asker), write on thanks walls, find people from Home's search, edit/delete/detach your own question, thanks wall first on profiles |
| F-42 | Home-screen widget | P2 | "Hot right now" lists up to 10 hot questions (rank, title, answers and pins) in the wallpaper's colors (Android 12+) or the app's accent, with the system's corner radius. Tapping a question opens it in the app, the title opens the app, ⟳ refreshes. It shows the list the app last loaded, and by itself asks Stips at most every 30 minutes (Android wakes it hourly), within the request budget. Settings → Updates → Home-screen widget asks the launcher to place it |
| F-43 | Live chats (#6) | P1 | The chat's top bar shows the other person's photo, name and online status, and opens their profile. An open chat checks for new messages every 3 s; the unread badges every 10 s while the app is open; the chats list updates when they change. Every visit starts with all bubble times folded. An opened notification stops being highlighted at once |
| F-44 | Launcher shortcuts | P2 | Touching and holding the app icon offers *Hot* (the full hot list) and *Chats*; each opens its screen, also from a cold start and when pinned to the home screen |

## 7. Non-functional requirements

| Area | Requirement |
|---|---|
| Speed | Cached feed visible < 300 ms after launch; a network feed page < 1 s on 4G |
| Size | APK < 10 MB |
| Data | About 10 KB per feed page and 2 KB per question's answers (plus images, which can be turned off) |
| Privacy | No analytics, ads or trackers. Network calls only to stips.co.il, cdn.stips.co.il, images.unsplash.com and the user's own browser/share targets |
| Security | HTTPS only; session token in app-private storage, excluded from backups |
| Accessibility | 48 dp targets, labels on icon buttons, 200% text, TalkBack reads rows as one sentence |
| Locale | Hebrew (RTL) and English; content direction follows the text |
| Respect | User-initiated requests, plus small checks while the app is open (unread counts every 10 s; new messages every 3 s in an open chat) and three opt-in ones (Live, phone notifications, the widget; SPEC D-24…D-26, D-51, D-53); honest User-Agent `StipsPlus/<version> (Android; unofficial)` |

## 8. Backlog (beyond v0.2)

- Pinned shortcuts for a single question or chat ("Add to home screen").
- Follow topics as shelves on Home.
- Draft answers saved while typing.

## 9. Assumptions

- Stips keeps its current API (`objectlist`, `omniobj`, …) stable enough; parsing is tolerant so small changes don't break the app.
- Account calls behave as the website's code shows (SPEC-04 §2.3); they're verified on the owner's device in this round.
- Stips doesn't object to a private, personal-use client while permission is pending.
