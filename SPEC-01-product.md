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
| v0.9 | Issue #8: Home layouts and options, refresh by hand, a welcome screen, expert mode, the asker's label, verified accounts, seconds, settings in pages (F-45–F-49); the chats widget (F-50) |
| v0.10 | Easy reply and reply quotes (F-51, F-52), notifications that open at the new answer or the flowered one (F-53), moderators marked on answers (F-54); seconds only under a minute, no ⟳ beside "New questions" |
| v0.11 | Phone notifications that arrive: checks every few minutes while the phone is awake, one notification per chat, notifications that pop up, a status panel with fixes, and a one-time offer to turn them on (F-55); reply quotes keep the answer's text as written (F-52) |
| v0.12 | Easy reply and quotes in chats (F-56), DM settings (F-57), tapping the open tab goes back to the top (F-58); text with `<`, `>`, `&` or quotes shows as written; #9: chats no longer come back empty after reopening the app |
| v0.13 | "Unread" bars in questions and chats (F-59, F-60), notices from Stips' team in full (F-61), block screens (F-62), no easy reply on answers below 50 points (F-63), what the credibility points mean and when they'd reach more (F-64), messages need 5 flowers (F-65); #10: an answer's edit history (F-66), tapping the open tab reloads it (F-58), scores in green and red (F-67), a scroll button on threads (F-68) |
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
| F-20 | Notifications | P1 | A bell on Home with an unread badge opens Stips' notification list (newest first, unread highlighted, tap opens the question or profile); Chats shows an unread badge; optional phone notifications (opt-in; every few minutes while the phone is awake since v0.11, F-55) |
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
| F-45 | Home your way (#8) | P1 | Three layouts: *Hot, then new* (the default), *Like the website* (hot cards, then today's questions by topic; a topic's heading opens it) and *New only*; 3, 5 or 10 hot cards from now, today or this week; channel chips on or off; the tab the app opens on; the order of a question's answers. Pull to refresh on a question |
| F-46 | Welcome (#8) | P1 | The first launch after installing offers sign-in or *Continue as a guest*; updates, links and shortcuts skip it |
| F-47 | Expert mode (#8) | P2 | Off: no network log or load times. On: Diagnostics, load times, ids, and *Server details* (the fields read and Stips' JSON, copyable) on questions, answers and profiles |
| F-48 | Who's who (#8) | P1 | "שואל השאלה" / "שואלת השאלה" on the asker's answers, also anonymous ones; a blue check and outline for organizations and Stips' own account; Stips' highlighted names, profile backgrounds and custom badges; age and numbers hidden where Stips hides them |
| F-49 | Exact times (#8) | P2 | Under a minute, times show seconds, corrected to Stips' clock; *Count the seconds* makes them tick |
| F-50 | Chats widget | P2 | Up to 10 latest chats (name, last message, time, unread count) on the home screen; a tap opens the chat. It follows the unread count and the Chats screen; its copy stays on the phone |
| F-51 | Easy reply (v0.10) | P1 | Swiping an answer to the right (or ⋮ → *Reply*) starts a reply to it: the draft opens with the carets that point to it ("^^", "4^") and a line break, and a bar above the composer says who it's to, with ✕. After posting, the carets are corrected if answers came in between, while Stips allows editing. A setting turns it off |
| F-52 | Reply quotes (v0.10) | P1 | Answers that point up with carets show the answer they reply to as a quote (author, two lines) above their text; a tap goes there and lights it up. The text stays as written, with every pointer tappable (v0.11). Settings turn the quotes off, or hide the opening carets |
| F-53 | Notifications open in place (v0.10) | P1 | A notification about new answers opens its question at the first new one; one about a flower, at the user's answer that got it; the answer lights up for two seconds (new answers get an "unread" bar instead since v0.13, F-59). From the bell and from phone notifications |
| F-54 | Moderators on answers (v0.10) | P2 | "נאמן סטיפס" / "נאמנת סטיפס" (and senior moderators) by the name on their answers, as on their profiles. A setting turns it off |
| F-55 | Phone notifications that arrive (v0.11) | P1 | New answers, flowers, thanks and messages reach the phone within minutes while it's in use (every 1, 2 or 5 minutes; 2 by default), and about every 15 minutes otherwise; they pop up. One notification per chat, with who wrote and what; tapping opens the chat. A status panel in Settings shows the last check and what holds checks back (Android's notification settings, restricted background use, battery saving), with *Check now* and *Send a test*. Signed-in users are offered, once, to turn them on and to let Stips+ run in the background |
| F-56 | Easy reply in chats (v0.12) | P1 | As on answers: swipe a message to the right to reply; the draft opens with the carets and a bar says to whom. The count is checked again, with new messages fetched, just before sending (Stips can't edit messages). Messages that point up show the one they reply to as a quote; their text stays as written. Chat-only switches |
| F-57 | DM settings (v0.12) | P1 | A ⚙ at the top of the chats list and of every chat opens the chat options in one sheet: easy reply, quotes, hiding their carets, phone and message notifications, Android's sound settings for messages, the chats widget. Also a page in Settings |
| F-58 | Back to the top (v0.12) | P2 | Tapping the open tab (Home, Pen pals, Chats, Profile) scrolls it back to the top, and since v0.13 (#10) reloads it at the same time |
| F-59 | Unread answers (v0.13) | P1 | A notification about new answers opens the question with a thin "N unread answers" bar above the first new one (no glow; a flower notification still glows). A refresh, the user's own answer, or opening the question again removes it |
| F-60 | Unread messages (v0.13) | P1 | A chat opens with a thin "N unread messages" bar above the first unread message, near the top when they fill the screen; reopening the chat or writing in it removes it |
| F-61 | Notices from Stips' team (v0.13) | P1 | A removed question or answer, a report's outcome and team notes say who they're from, show the team's words in a box and the credibility points taken; a tap shows them in full with the user's credibility now and, for a removal, how to appeal. Phone notifications are titled "Stips' team" |
| F-62 | Block screens (v0.13) | P1 | When Stips blocks the account, or it has under 30 credibility points, a full screen says why and until when (Stips' reason and end date, or 30 − points days; below 20 for good), with *Continue reading*, *Contact Stips*, *Stips' rules* and *Sign out*; once per run and again when Stips refuses an action |
| F-63 | Easy reply needs editing (v0.13) | P1 | Below 50 credibility points easy reply on answers is off (Stips allows no editing), and Settings says why; reply quotes and chats' easy reply stay |
| F-64 | Credibility, explained (v0.13) | P1 | The gauge shows Stips' rating and how fast it rises; a tap shows what the points allow, a slider for "when will the points reach N" (a date, or a range from 90), the milestones ahead, Stips' table, and on one's own profile the senior answerer and moderator goals |
| F-65 | Messages need 5 flowers (v0.13) | P2 | Below 5 flowers (moderators exempt) a chat shows the site's explanation instead of the composer |
| F-66 | Edit history (#10, v0.13) | P1 | An edited answer says "· edited" after its time; it and ⋮ → *Edit history* open every version, newest first, with who edited it (the answerer, a moderator, Stips) and when |
| F-67 | Scores in color (#10, v0.13) | P2 | An answer's score is green above zero and red below |
| F-68 | Scroll button on threads (#10, v0.13) | P2 | A small round button goes down to the last answer; once the question's header has folded away it turns into "back to the top". Hidden when the thread fits on the screen |
| F-69 | Pen pals, simpler (#12, v0.14) | P2 | A pen-pal post only opens a chat with its writer; your own keeps *Delete* |
| F-70 | The asker's nickname (#12, v0.14) | P2 | On an anonymous question asked under a nickname, the asker's anonymous answers show that nickname |
| F-71 | Gentle jumps (#12, v0.14) | P1 | A reply's quote or carets light up what they point at without moving the list when it's in sight, and move it the least otherwise |
| F-72 | No emojis where Stips drops them (#12, v0.14) | P1 | Questions and answers leave emojis out as they're typed, with a line saying why; chats keep them |
| F-73 | Bio links and "I use Stips+" (#12, v0.14) | P2 | Links in bios open; your profile offers to add a line with the app's Google Play link below your bio |
| F-74 | Special accounts (#12, v0.14) | P2 | Stips' creator has a badge by his name on his profile, questions and answers; the list is in the code |
| F-75 | Settings rethought and an intro (#12, v0.14) | P1 | Settings in three groups with summaries and pictures; a first-run intro to choose the look, Home and replies, then sign in; shown again from About |
| F-76 | Pictures from links (#15, v0.14) | P1 | imgbb links and image files open in the app's zoomable viewer; attached ones get *Show image* |
| F-77 | Export a thread (#16, v0.14) | P2 | ⋮ → *Export thread*: Markdown or JSON, copied or shared |
| F-78 | Notifications in categories (#13, v0.15) | P1 | The bell's list: category chips, a summary of what's new, Stips' team's notices pinned until opened, sections by day, long-press *Mark as read* and *Stop updates*; optional titles and photos; one phone channel per category |
| F-79 | Animation presets (#18, v0.15) | P1 | Full (with short transitions), Less (no screen transitions, the rest faster), None, or each kind chosen |
| F-80 | Swipe actions (#18, v0.15) | P1 | Questions in lists swipe both ways (pin and report by default, each configurable); answers swipe left too (nothing on others' by default, edit on your own) |## 7. Non-functional requirements

| Area | Requirement |
|---|---|
| Speed | Cached feed visible < 300 ms after launch; a network feed page < 1 s on 4G |
| Size | APK < 10 MB |
| Data | About 10 KB per feed page and 2 KB per question's answers (plus images, which can be turned off) |
| Privacy | No analytics, ads or trackers. Network calls only to stips.co.il, cdn.stips.co.il, images.unsplash.com and the user's own browser/share targets |
| Security | HTTPS only; session token in app-private storage, excluded from backups |
| Accessibility | 48 dp targets, labels on icon buttons, 200% text, TalkBack reads rows as one sentence |
| Locale | Hebrew (RTL) and English; content direction follows the text |
| Respect | User-initiated requests, plus small checks while the app is open (unread counts every 10 s; new messages every 3 s in an open chat) and four opt-in ones (Live, phone notifications, the two widgets; SPEC D-24…D-26, D-51, D-53, D-67); honest User-Agent `StipsPlus/<version> (Android; unofficial)` |

## 8. Backlog (beyond v0.2)

- Pinned shortcuts for a single question or chat ("Add to home screen").
- Follow topics as shelves on Home.
- Draft answers saved while typing.

## 9. Assumptions

- Stips keeps its current API (`objectlist`, `omniobj`, …) stable enough; parsing is tolerant so small changes don't break the app.
- Account calls behave as the website's code shows (SPEC-04 §2.3); they're verified on the owner's device in this round.
- Stips doesn't object to a private, personal-use client while permission is pending.
