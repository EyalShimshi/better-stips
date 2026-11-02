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
| v0.2 | Notifications, ask a question, per-app language, polish from owner feedback |
| v0.3 | Home-screen widget, shortcuts, link handling polish |
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
| F-12 | Chats | P1 | Signed in: conversations (name, last message, time, unread count); a thread with bubbles; send a message; load earlier messages |
| F-13 | Profile | P1 | Signed in: photo, nickname, points; My questions, My answers. Signed out: an invitation to sign in. Always: Saved, Settings |
| F-14 | Settings | P0 | Colors (dynamic color, accent, theme, pure black), interface (images, animations), performance, about |
| F-15 | About and disclaimer | P0 | Version, license, source link, report link, "An independent app, not affiliated with Stips" |
| F-16 | Crash catcher | P0 | After a crash, the next launch offers Copy details / Report on GitHub / Close; nothing sent automatically |
| F-17 | Open Stips links | P2 | `https://stips.co.il/ask/<id>` links can open in the app (when the user allows it in Android settings) |
| F-18 | Timings | P1 | The question screen shows "Loaded in 0.4 s"; Settings → Performance lists recent timings |
| F-19 | Instant start | P1 | The last-seen first page of the Home feed shows at once on launch, then refreshes |
| F-20 | Notifications | P2 | v0.2 |
| F-21 | Ask a question | P2 | v0.2 |

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
| Respect | User-initiated requests only; honest User-Agent `StipsPlus/<version> (Android; unofficial)` |

## 8. Backlog (beyond v0.2)

- Home-screen widget with hot questions; launcher shortcuts (Hot, Chats).
- Follow topics as shelves on Home.
- Image viewer with pinch-zoom for question photos.
- Draft answers saved while typing.
- Per-question "notify me of new answers" (`item.notifications_register`).

## 9. Assumptions

- Stips keeps its current API (`objectlist`, `omniobj`, …) stable enough; parsing is tolerant so small changes don't break the app.
- Account calls behave as the website's code shows (SPEC-04 §2.3); they're verified on the owner's device in this round.
- Stips doesn't object to a private, personal-use client while permission is pending.
