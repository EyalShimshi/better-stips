# Stips+

*A fast, native Android app for [Stips](https://stips.co.il), Israel's Hebrew Q&A community. An independent app, not affiliated with Stips.*

> **Status:** the code is open; the app isn't published anywhere yet while we ask Stips for permission ([SPEC-06 §4](SPEC-06-roadmap-risks.md#4-the-permission-request)).

## Features (v0.18)

- **Home:** the five hottest questions as big cards, then the newest questions. **Live** brings new questions in by themselves, every 5 to 30 seconds (long-press the Live chip to set it); pull down to reload. Switching to Home, or coming back to the app on it, brings new questions in too. Search finds topics and people.
- **Home your way** (Settings → Home): the default, a layout **like the website** (hot questions, then today's questions by topic), or **new questions only**; 3, 5 or 10 hot cards, from now, today or this week; the channel chips on or off; which tab the app opens on.
- **Home-screen widgets:** "Hot right now" shows the hottest questions, and **Chats** shows your latest chats with unread counts. Tap a question or a chat to open it, or ⟳ to refresh. Add them from your launcher's widgets, or from Settings → Widgets.
- **Launcher shortcuts:** touch and hold the app icon for **Hot** and **Chats**.
- **Ask a question** in three steps, like the site: the question (anonymous with your own nickname, and a link if you like), its tags (suggested by Stips), and a cover photo from Stips' gallery.
- **Questions:** the cover photo behind the title (tap ⋮ → View image to see it whole, with zoom), answers 💬 and pins 📌 as symbols, the asker's link, a header that folds into a small title bar as you scroll, and pull down to reload. The asker's own answers say **"שואל השאלה"**, even when anonymous. Organizations and Stips' own account have a **blue check**, moderators a **"נאמן סטיפס"** badge, and Stips' creator a badge of his own (Stips+'s creator's shows on his profile only). A **sort** chip by the number of answers shows them newest first or by upvotes, for that visit; every visit starts in Stips' order (oldest first).
- **Pictures in the app:** Stips can't hold pictures, so people link to them on imgbb. A picture link (imgbb, and image files on Imgur or Postimages) opens in the app's viewer, with zoom; an attached one gets a **Show image** button next to it. The viewer has *Open in browser*.
- **Export a thread:** ⋮ → *Export thread* copies or shares the question and its answers as text (Markdown, easy to paste into an AI chat) or JSON, numbered, with who replied to whom. Answers you hid stay out.
- **Replies, Stips style:** swipe an answer to the right to reply to it. Stips+ starts your answer with the carets that point to it (^^, or 4^ from four up) and fixes them after posting if other answers came in first (from 50 credibility points, since Stips allows editing only from there). Answers that point up show the answer they reply to as a quote above their text, which stays as written; tap the quote or the carets to go there. The opening carets hide behind the quote (Settings can show them). If it's already in sight, it only lights up; otherwise the thread moves just enough to show it. Stips keeps no emojis in questions and answers, so they're left out as you type (chats keep them).
- **Notifications open in place:** new answers open at the first new one under a thin **"unread" bar**, as in WhatsApp; a flower opens at your answer that got it, and it lights up for a moment.
- **Notices from Stips' team:** when a moderator removes your question or answer, a report you made is handled, or the team leaves you a note, the notification says so, shows the team's reason and the credibility points taken, and opens in full with how to appeal.
- **Credibility, explained:** the gauge shows Stips' rating (from *מושלם* to *מזיק מאוד*) and how fast it rises. Tap it for what your points allow, **when they'll reach any number you pick**, the milestones ahead, Stips' table, and your goals (senior answerer, what Stips asks of moderators). If Stips blocks the account, or the points fall under 30, a screen says why and until when, and reading goes on.
- **Times to the second** under a minute ("12 s"), set by Stips' clock, ticking while you look (*Count the seconds* turns that off).
- **Answer** (anonymously too, with one link), **edit**, **delete** or **detach** your answers; **vote**, **pin**, **get updates**. If you answered a question anonymously, your next answer there starts anonymous. As the asker, **send flowers** to good answers.
- **Anonymous mode** (the masks on Home): everything you post goes out anonymously for 30 minutes, a few hours, or until you turn it off.
- **Report and hide:** report a question or an answer to Stips' moderators; hide an item or everything from one user, with *Undo* for a few seconds (Settings → Privacy and safety → Hidden content brings it all back).
- **Swipe actions, as in Gmail:** swipe a question in any list to the right to pin it and to the left to report it (a short flick is enough), or pick other actions for each side in Settings → Home (get updates, share, copy the link, open on Stips, export, hide). Answers swipe right to reply, and left for an action you pick in Settings → Questions and answers: nothing on others' answers and *Edit* on your own, by default.
- **Pen pals (חברים לעט):** read the board, post your own message, and tap a post to start a private chat. **Live** (the chip at the top) brings new posts in by themselves, and so does switching to the tab or coming back to the app on it.
- **Profiles** for everyone: photo (tap to view it big), questions · answers · flowers, bio, badges, age, how long they've been on Stips, the **credibility** gauge, the **thanks wall** first (write on others', delete on yours), and activity graphs. Special accounts show Stips' colors and badges. Links in bios open. Bios hold up to 1000 characters, centered, and a last line in tildes (`~your words~`) shows under them as your quote, as on Stips. Activity graphs read left to right, newest on the right. On your own profile, **I use Stips+** adds a line with a link to the app's page below your bio, and profiles whose bio has it show a **Stips+** badge.
- **Chats:** search on top finds a chat by name, or anyone on Stips, and a tap opens the chat with them. Under it, a shelf shows who from your recent chats is online now, as on Instagram, with **notes to self** first: a chat with yourself for drafts, links and reminders, kept on Stips. Scroll down and older chats keep coming, all the way to your first. A green dot shows who's online, and the top bar shows who you're talking to (tap it for their profile). New messages arrive within seconds, and each day starts with its date, as in WhatsApp. Bubbles are compact: tap one for its time; **long-press** for *Reply*, *Copy*, *Forward* (to up to five chats at once) and *Share* (to other apps), which leave out the reply carets. **Swipe a message to the right to reply to it** with Stips' carets, as on answers, and messages that point up show the one they reply to as a quote. A thin **"unread" bar** marks where the unread messages start. Emoji codes like `:nerd:` from older Stips apps show as emoji. **Links to Stips questions and profiles show as cards** (the question's photo, title and answers; the person's photo, credibility and counts), and a tap opens them in the app. **Reply in private:** a question's ⋮ menu (or a swipe you pick for questions or answers) opens a chat with the asker about the question; it goes with the question's link, which shows as its card. Scroll up and earlier messages load by themselves, back to a card about the other person where the chat began; a ↓ button counts new messages and jumps back down. ⋮ reports someone to Stips' team or blocks their messages, and ⚙ opens **DM settings** (also in Settings): replies, message notifications and their sound, the chats widget.
- **Back to the top:** tap the open tab (Home, Pen pals, Chats, Profile) again; it reloads too, as if you'd just opened it.
- **Edit history:** an edited answer says "· edited" next to its time. Tap it (or ⋮ → Edit history) to see every version, who edited it (the answerer, a moderator or Stips) and when, as on the site.
- **Long threads:** a small ↓ button jumps to the last answer, and turns into ↑ (back to the question) once you've scrolled down. Scores are green above zero and red below.
- **Notifications in order:** chips split the bell's list into *Updates*, *Flowers and thanks* and *System messages*, each counting what's new; notices from Stips' team stay pinned on top until you open them, and open in full, from their phone notification too; the rest is grouped by day, and **a question's updates are one row** that reads as one sentence with their total ("התקבלו 6 תשובות חדשות לשאלה: …"; a tap opens at the first new answer). Flowers show in Stips' own tulip colors, and older notifications load as you scroll. Long-press a row to mark it read or stop a question's updates. Optionally, rows show the question's title and photo (Settings → Notifications). A badge on the bell and on Chats (the bell's counts what's new even when the official app or the site saw it first), and optional **phone notifications** that pop up within minutes while you use your phone: new answers (one notification per question), flowers and thanks, Stips' team's notices, and one per chat with who wrote and what; tapping one marks it read. Each kind has its own Android channel, so you can give it a sound or silence it. Settings → Notifications shows the last check and anything in Android that holds them back, with *Check now* and *Send a test*.
- **Intro:** the first launch sets the app up in a few steps: the look, Home's layout, how replies show and what swipes do, each with a picture that changes as you choose; then sign in or look around as a guest, and phone notifications. Settings → About shows it again.
- **Tips:** small hints show what Stips+ adds as you meet it: an answer slides to show the reply swipe, your new answer shows what swiping it left does, a chat message shows the reply swipe, Home's rows show their two swipes (as you set them), and Live and your notes get a word each. Each shows until you use it, three times at most (Settings → About → *Show the tips again*).
- **Updates inside the app:** once a day Stips+ asks GitHub whether a newer build is out, and Home offers it: *Update* opens it in your browser, which downloads it; open the file and Android installs it after you confirm. Stips+ itself never downloads or installs apps. Settings → About checks now, or turns the daily check off.
- **Share to chats:** share text from any app (WhatsApp, the browser) to Stips+ and send it to up to five chats at once, as in WhatsApp: your notes to self on top, then your recent chats, and a search for anyone on Stips. Text only; five is the most, so it can't be used to spam.
- **Settings** in three groups: *Make it yours* (appearance, Home, questions and answers, DM settings), *On your phone* (notifications, widgets) and *More* (privacy and safety, Stips, about), each row saying what's chosen now. Pages with something to see start with a picture of it that changes with the switches. **Expert mode** (in About) adds ids, *Server details* (what Stips sent, copyable), load times, and **Diagnostics → Network log** for bug reports.
- **Native feel:** Material 3, your wallpaper's colors (or Stips green), dark mode with pure black, right-to-left Hebrew, haptic feedback, and quick transitions that follow your finger on predictive back. **Animations your way** (Settings → Appearance): less (the default: no screen transitions, everything else quicker), full, none, or a custom mix. Messages at the bottom swipe away down or to the side.

## Install

1. On the phone, open **[www.shimshii.com/stips-plus](https://www.shimshii.com/stips-plus)** and tap *Download*, or open the repository's **Releases** page and tap the newest `stipsplus-<number>.apk`. Each release's notes say what changed and how it was checked: every APK is opened on an Android emulator before it's published. Builds 7–9 crash on launch (#7); don't install them.
2. Allow your browser to install unknown apps when Android asks.
3. Play Protect may offer to scan it first; that's fine. Every build is signed with the same key, so new builds install over old ones. (Play Protect flagged builds 31 and 32, the ones that could install their own updates. From build 33, Stips+ can't install apps at all.)
4. From then on, Stips+ tells you about new builds (Home, or Settings → About), and *Update* downloads them in your browser.

**From a computer (optional, needs Android's platform tools):** `adb install -r stipsplus-<number>.apk`

**Open Stips links in the app (optional):** share a link to Stips+ from any app, or set Android Settings → Apps → Stips+ → Open by default → add `stips.co.il`. Question, profile and messages links then open in Stips+.

## Privacy

- **No ads, analytics or trackers.** No account of ours.
- **Your password** goes only to Stips, once, when you sign in. The app keeps only the sign-in token Stips returns, in app-private storage that's excluded from backups.
- **Saved questions, settings, hidden content, anonymous mode and load times** stay on the phone. So does the chats widget's copy of your latest chats: it's left out of backups and deleted when you sign out. If you reinstall, Android's backup brings your settings back, and the intro shows with them already chosen.
- **Crash reports** stay on the phone until you choose *Copy details* or *Report on GitHub*.

**Every network call:**

| Host | When |
|---|---|
| `stips.co.il` | Loading feeds, questions, profiles, pen pals, notifications and chats; signing in; posting questions, answers, votes, pins, flowers, reports and messages; uploading a new profile photo |
| `cdn.stips.co.il` | Question photos and profile pictures (turn off: Settings → Appearance → Show images) |
| `images.unsplash.com` | Stock cover photos that askers picked (same switch) |
| `ibb.co`, `i.ibb.co`, `i.imgur.com`, `i.postimg.cc` | Only when you tap a picture link or *Show image*: the picture, and for an imgbb page first its short description (under 1 KB) to find the picture. No cookies |
| `api.github.com` | Once a day as the app opens, to see whether a newer Stips+ is out (turn off: Settings → About → *Check once a day*). No cookies, nothing about you |
| `github.com` | Only when you tap *Update*: the new APK |

**In the background:**

| What | When | Cost |
|---|---|---|
| Unread chats and notifications | Every 10 seconds, only while the app is open and you're signed in; the bell's first page too when Stips counts something new, and every minute while Home is on screen (since other apps on your account can mark notifications seen before Stips+ counts them) | Under 1 KB each; about 1 KB for the bell's page |
| An open chat | New messages every 3 seconds, and whether the other person is online once a minute, only while the chat is on screen | Under 1 KB for each check with nothing new, about 1 MB an hour |
| **Live** new questions | Every 5–30 seconds (you choose; 30 by default), only while Home is on screen, and only if you turn it on | About 1 MB an hour at 30 s, 6.7 MB at 5 s |
| **Live** pen pals | At the same rate, only while Pen pals is on screen, and only if you turn it on (the chip at its top) | The board's newest 30 posts each time |
| **Entering a tab** | Switching to Home, Pen pals or Chats, or coming back to the app on one: its first page once, unless it loaded in the last 10 seconds (plus the hot questions on Home) | About the same as one Live check |
| **Phone notifications** | Every 1, 2 or 5 minutes while your phone is awake (you choose; 2 by default) and about every 15 minutes otherwise, only if you turn it on (Settings → Notifications). Not while the app is open | Under 1 KB for the counts and about 1 KB for the bell's first page each time; the chats list (a few KB) only when something may be new |
| **Home-screen widget** | When Android wakes it (hourly), only if its list is over 30 minutes old; nothing extra when the app loaded the same list recently. Only if you add the widget | About 9 KB each, at most 0.2 MB a day |
| **Chats widget** | When your unread count changes (checked by the app while it's open, or by phone notifications), or hourly when its list is over 30 minutes old; nothing extra when the Chats screen loaded the list. Only if you add the widget | A few KB each |
| **Moderator badges** | While a question is open: the profile badges of people whose answers are on screen, once a day per person, at most 30 per question and one every 1.5 seconds | About 1–2 KB each |
| **Link cards in chats** | While a chat is on screen: each Stips question or profile linked in a message on screen, once per app session, one at a time, and only while the request limit has room (nothing for a question a list already showed) | A few KB each; two calls for a profile |
| **Questions' titles in notifications** | While the bell's list is on screen, only if you turn on *Show questions' titles and photos* (Settings → Notifications): each question its rows are about, once, from the app's memory when it has it, one every 1.5 seconds, and only while the request limit has room | A few KB each |

**Stips' request limit:** Stips can stop answering a phone that sends too many requests. Stips+ makes its requests the way the site's own pages do, identifies itself honestly, and keeps to about the official app's pace: 40 requests at once, then 30 a minute, and the checks above wait while you're busy. If Stips stops answering, a banner counts down, nothing is sent except one try a minute, and everything resumes as soon as Stips answers.

**Network log:** the app keeps its last few hundred requests in memory (which call, the answer code, timing, errors; never passwords, cookies or what you wrote) so you can attach them to a bug report from Settings → Diagnostics (turn on expert mode to see it). Nothing is sent unless you share it.

**Permissions:** Internet and network state (to notice network changes and to schedule the background check); notifications (asked only when you turn phone notifications on, Android 13+); "run at startup", so the 15-minute check survives a restart; asking to run in the background (Android shows its own question when you turn phone notifications on, and you decide), so battery saving doesn't hold the checks back; and installing apps, only for Stips+'s own updates after you tap *Update* (Android asks you once to allow it, and installs only a build signed like yours). Picking a profile photo uses Android's photo picker, so the app never gets access to your other photos.

## Building

There's nothing to install locally: GitHub Actions builds every push to `main` (JDK 17, Gradle 9.5.0, Android Gradle Plugin 9.3.1), runs the unit tests, checks the APK, opens it on an Android emulator, and then publishes a pre-release with the APK once the owner approves it. See [MAINTAINING.md](MAINTAINING.md). To build on your own machine, install Gradle 9.5 and the Android SDK, then run `gradle assembleRelease`.

## How it's made

- Spec and decisions: [SPEC.md](SPEC.md) (with SPEC-01…06).
- Process: [BUILD-PLAYBOOK.md](BUILD-PLAYBOOK.md). Look: [DESIGN-PHILOSOPHY.md](DESIGN-PHILOSOPHY.md).
- Inspired by [better-rail](https://github.com/better-rail/app). No code or assets are copied from it or from Stips.

## License

[GPL-3.0-or-later](LICENSE). "Stips" is a trademark of its owners; this app isn't affiliated with or endorsed by them.

---

## בעברית

**Stips+** היא אפליקציית אנדרואיד מהירה וטבעית לסטיפס: חמות ושאלות חדשות (גם בלייב), ווידג'ט "חם עכשיו" למסך הבית, שאילת שאלות, חברים לעט, ערוצים ונושאים, שאלות ותשובות, התחברות, מענה (גם באנונימיות ועם קישור), מצב אנונימי, עריכת תשובות, נעיצות, פרחים, דיווח וחסימת תוכן, קבלת עדכונים, פרופילים עם נקודות אמינות וקיר תודות, התראות ושיחות פרטיות חיות (מי מחובר/ת, והודעות חדשות תוך שניות, עם דיווח וחסימה, חיפוש משתמשים לשיחה, מדף של מי שמחובר/ת עכשיו, הערות לעצמי ותאריך מעל כל יום), קיצורי דרך ל"חם עכשיו" ולשיחות מסמל האפליקציה, תמונות מקישורים (imgbb ועוד) בתוך האפליקציה, ייצוא שרשור כטקסט, התראות מסודרות לפי סוגים (עדכונים, פרחים ותודות, הודעות מערכת) ושורה אחת לכל שאלה, במשפט אחד עם סך התשובות, החלקות לפעולות מהירות על שאלות ותשובות, מיון תשובות בתוך השאלה, חברים לעט בלייב, תג Stips+ בפרופיל, כרטיסים לקישורי שאלות ופרופילים בשיחות ו"תגובה בפרטי" לשואל, עדכוני גרסה מתוך האפליקציה, טיפים קטנים על ההחלקות, ובחירה כמה אנימציות יהיו. בלי פרסומות ובלי מעקב. הסיסמה נשלחת רק לסטיפס, והאפליקציה שומרת רק את אסימון ההתחברות. אפליקציה עצמאית, ללא קשר לסטיפס. הקוד פתוח, אבל האפליקציה עוד לא מפורסמת, עד שנקבל אישור מסטיפס.
