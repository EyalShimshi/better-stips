# SPEC-07 — Proposals (v0.14)

*Planning answers to issues that asked "what do you think?" rather than "do it". §1 was built in v0.15; §2 waits for the owner; §3 was discarded. Links: [SPEC.md](SPEC.md) (decisions), [SPEC-04](SPEC-04-data.md) (data).*

---

## 1. Stips' notifications inside the app, rethought (#13)

### 1.1 What Stips sends

`notifications.list` (`{"page":1}`, about 20 per page, newest first) returns items like:

| Field | Example | What it is |
|---|---|---|
| `id` | `901` | Grows with time |
| `type` | `new_ask_ans_registered`, `new_flower`, `thanks` | A word, not a number. Only `new_flower` appears in the site's code; the rest are learned from real lists |
| `contentHtml` | "התקבלו 3 תשובות חדשות לשאלה: …" | Stips' own Hebrew sentence, as HTML; the only place a count of new answers appears |
| `msgText` | The team's words | Only on notices from Stips' team (a removal, a report's outcome, a note) |
| `pointsRemoved` | `5` | Credibility points a moderator took away |
| `linkPageName`, `linkItemId` | `"ask"`, `19745077` | Where it leads: a question or a profile. **Never an answer** |
| `viewed` | `false` | Stips sets it as soon as the list is fetched, by anyone: the site, the official app, or Stips+'s phone check |
| `time` | `2026/10/25 12:00:00` | Israel time |

What the rule book lists (p. 10): new answers (on questions you asked, answered or follow), flowers, thanks-wall messages, removals by the team, and the team's notes. Chats are separate (`messages.count`, the Chats tab), and so are pen pals.

### 1.2 What Stips+ does today

- **One list** behind the bell on Home: "New" (what Stips+ hasn't shown before, by its own mark per account, since Stips' `viewed` flips on any fetch) and "Earlier". Each row: an icon by kind, Stips' sentence, the time.
- **Kinds** (`NotificationKinds`): answers, flower, thanks, question, message, badge, removal, report, team note, other. The team's notices are recognized by `msgText`, `pointsRemoved` and their words; they open a dialog with the reason and the points, the rest open where they lead.
- **Opening in place:** new answers open the question at the first new one under an "unread" bar (the count read from the sentence); a flower opens at the answer that got it and lights it up.
- **Phone notifications** use the same list (one per item), plus one per chat.

### 1.3 What's messy

1. **Many rows for one question.** A busy question sends a row per burst ("2 new answers", then "3 new answers", then "1 new answer"): ten rows can be one question. Opening one doesn't clear the others.
2. **Important things drown.** A removal that cost 5 credibility points looks like any answer ping, between dozens of them.
3. **"New" vs "Earlier" says little.** It doesn't say when, and a long "Earlier" list has no shape.
4. **Activity is in three places:** the bell (answers, flowers, thanks, the team), Chats (messages), Pen pals. Each has its own badge.
5. **No actions where you read them:** to stop updates from a noisy question, you open it and tap the bell there.
6. **Stips' sentences are the only data:** a row can't say more than Stips wrote (who gave the flower, which answer) unless the app reads the question.

### 1.4 What we could do

| Idea | What it looks like | Cost | Notes |
|---|---|---|---|
| **A. One row per question** | "שאלה: …" with a summary: "7 new answers · a flower", the newest time, a dot if any is new. Tap opens at the first unread answer; a small arrow shows the separate events | Medium, data we have (group by `linkItemId`) | The biggest cleanup. Phone notifications can group the same way (Android's notification groups) |
| **B. The team's notices on top** | A distinct card ("From Stips' team") pinned above everything until opened, with the points taken | Small | Matches how much they matter: credibility, blocks |
| **C. Time sections** | Today · Yesterday · This week · Earlier, with "new" dots instead of a "New" section | Small | Familiar from every phone app |
| **D. Filters** | Chips: All · Answers · Flowers and thanks · Stips' team | Small | Useful once lists are long |
| **E. Actions in place** | Swipe or ⋮ on a question row: *Stop updates* (Stips' follow switch), *Mark as read*, *Open the question's top* | Medium | Stop updates calls Stips; mark as read is Stips+'s own |
| **F. A "since you were here" line** | At the top: "Since yesterday: 12 answers on 3 questions, 2 flowers, 1 thanks" | Small | Reads the same data |
| **G. One Activity tab** | A bottom-bar tab for notifications, with chats and pen pals as sections or chips, replacing the bell | Large | Changes the app's layout; the bell is where Stips users look today |
| **H. Richer rows** | Read the question (title, cover thumbnail) and, for a flower, which answer | Medium, extra requests | Costs about 1 request per question shown (cached) |

### 1.5 Recommendation

In two steps, so each can be tried on the phone:

1. **v0.15:** A + B + C, with phone notifications grouped per question too. That's most of the mess: one row per question, the team's notices on top, and time sections.
2. **v0.16:** D + E + F if the list still feels long, and H for questions on screen if the rows feel bare.

Leave G for later: an Activity tab is a big change to how the app is organized, and the bell already works the way Stips users expect.

### 1.6 Decided (the owner, after v0.14)

| Question | Decision |
|---|---|
| Group per question (A)? | **No.** Split by **category** instead: **Updates** (new answers on questions you asked, answered or follow), **Flowers and thanks**, **System messages** (removals, reports' outcomes, the team's notes, and anything else that isn't an update or a flower or thanks). **v0.17: yes, too,** at the owner's ask: inside the categories, a question's updates are one row (D-124); **v0.18:** that row reads as one sentence with the total, without an arrow (D-128) |
| The team's notices (B) | **Pinned on top** until opened |
| Sections (C) | **By day:** Today · Yesterday · This week · Earlier, with "new" dots |
| Filters (D) | **Yes** |
| Actions on a row (E) | **Yes:** *Stop updates*, *Mark as read* |
| The summary line (F) | **Yes**; **v0.18: removed** at the owner's ask (#27, D-128) |
| Richer rows (H) | **Yes, as an option** in the notification settings, **off by default** (it costs requests) |
| An Activity tab (G) | **Never:** the bell stays |

### 1.7 Built in v0.15

- **The bell's screen:** chips on top, *All · Updates · Flowers and thanks · System messages* (the categories are the filters; *All* first). Under them, in *All*: the summary line ("New since your last visit: 12 new answers on 3 questions · 2 flowers · a thanks message"), then the team's unopened notices from the last 30 days pinned as cards, then the list in day sections, each row with its category's icon and a "new" dot.
- **Categories** come from `NotificationKinds`: Answers and Question → Updates; Flower and Thanks → Flowers and thanks; Removal, Report, TeamNote, Badge, Message and Other → System messages. Pure, unit-tested, so a new type lands in System messages until it's known.
- **Row actions** (long press): *Mark as read* (Stips+'s own mark, as the "new" dots are) and, on an update, *Stop updates* (Stips' follow switch for that question, with an undo snackbar).
- **Richer rows (option):** Settings → Notifications → *Show questions' titles and photos*: rows on screen read their question (cached for the session, at most one request every 1.5 s, only while the budget has room), adding its title and cover thumbnail.
- **Phone notifications:** unchanged, but their Android channels follow the same three categories, so each can be silenced on its own (*Messages* keeps its channel).

---

## 2. Uploading pictures to imgbb from the app (#15, part 2)

*Part 1 (showing imgbb pictures in the app) shipped in v0.14.*

### 2.1 How it would work

imgbb has an upload API: `POST https://api.imgbb.com/1/upload` with an **API key** and the image; it answers with the picture's page link, its file link and a **delete link**. The app would re-encode the photo (dropping its location and camera data), upload it, and fill the answer's or question's link with the page link.

### 2.2 Why not as it stands

1. **The key would be public.** The app's code is open, so the key would be on GitHub for anyone to use: spam or illegal pictures would be uploaded under the owner's imgbb account, and when imgbb blocks the key, the feature breaks for everyone, in an app that's rarely updated. Hiding it doesn't help: an APK can be opened.
2. **Pictures from minors, made effortless.** Most of Stips is teenagers. One tap would put a photo (a selfie, a screenshot with a name or a phone number) on the open internet, at a link anyone can pass on, with no way to delete it unless the app keeps the delete link. Today the extra step on imgbb's own site at least makes it a choice.
3. **A middle service of ours.** The usual fix for (1) is our own server that holds the key and checks uploads. That ends the promise of "no server, nothing collected", costs money, and makes the owner responsible for what goes through it.
4. **Terms:** imgbb's terms make the key's owner answerable for its use; Google Play asks apps with user content for reporting and blocking, which would then have to cover pictures too.

### 2.3 Safer ways

| Option | How | Risk | Effort |
|---|---|---|---|
| **a. A shortcut to imgbb** | In the "attach a link" dialog: *Upload a picture on imgbb*, opening imgbb's upload page in the browser; the user copies the link back (the dialog offers *Paste*) | None of ours: it's imgbb's own page | Small |
| **b. Your own imgbb key** (an option for the few who want it) | The user makes a free imgbb account and pastes their key once; uploads go to their account; the app strips the photo's data, keeps delete links, and warns that the link is public | Their account, their key | Medium |
| **c. Built-in with a shared key or our server** | As in 2.1 | All of 2.2 | Medium to large |

**Recommendation:** (a) now, since it's cheap and safe; (b) only if people ask for it; not (c).

---

## 3. An iPhone version (#17)

*Discarded by the owner after v0.14 (#17 closed): Stips+ stays an Android app. The analysis is kept for the record.*

### 3.1 What we have

The app is Kotlin with Jetpack Compose. About half of it doesn't depend on Android: the parsers, the rules (`util/`), the models and most of the repository's logic, with their unit tests. The rest is Android's: background checks (JobScheduler, alarms), notifications, widgets (Glance), the photo picker, haptics, resources, and the libraries OkHttp and Coil 2.

### 3.2 The ways to get there

| Way | What it means | Fit |
|---|---|---|
| **Kotlin Multiplatform + Compose Multiplatform** | Move the shared half into a common module (OkHttp → Ktor, Coil 2 → Coil 3, `java.time` → kotlinx-datetime, Android resources → Compose resources, SharedPreferences → a multiplatform store), then run most screens on iOS too. Widgets (WidgetKit) and notifications are written for iOS separately | **Best:** one codebase, the same features, the same tests. Several rounds: first the move (Android keeps working), then the iOS shell |
| **A native SwiftUI app** | Write it again for iOS | The best iOS feel, but two apps to keep alive forever, against "release once, rarely update" |
| **Wrapping the website** | A web view | That's the official app already |

### 3.3 What's hard regardless

- **Building and installing.** iOS apps build only on a Mac. GitHub's macOS runners can build and test (free for a public repository), but an iPhone installs only signed apps: TestFlight or the App Store need an **Apple Developer account ($99 a year)**. There's no "download the APK" path; without the account, a Mac with Xcode re-signs the app for one phone every 7 days.
- **App Store review.** An unofficial client of someone else's service needs the service's permission (guideline 5.2.2), plus the user-content rules (report, block: we have them). Stips' written permission comes first, as for Google Play.
- **Notifications.** iOS doesn't let an app check every few minutes in the background: it runs background refreshes when it chooses, sometimes a few times a day. Timely notifications on iPhone need push from a server, which would have to hold every user's Stips session: against the app's privacy promise. So on iPhone, notifications would be "when you open the app", plus the occasional background refresh.
- **Widgets** are SwiftUI (WidgetKit), a separate small app.

### 3.4 Verdict

Possible, and the shared half makes it cheaper than starting over, but it's a project of its own: the multiplatform move, an iOS shell, the developer account and weaker notifications. **Recommendation:** first Google Play and Stips' permission; then, if iPhone users ask, the Kotlin Multiplatform way, starting by moving the pure code into a shared module (which also helps Android).
