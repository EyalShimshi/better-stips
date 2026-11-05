# Stips+

*A fast, native Android app for [Stips](https://stips.co.il), Israel's Hebrew Q&A community. An independent app, not affiliated with Stips.*

> **Status:** private, for personal use, while we ask Stips for permission to publish it ([SPEC-06 §4](SPEC-06-roadmap-risks.md#4-the-permission-request)).

## Features (v0.9)

- **Home:** the five hottest questions as big cards, then the newest questions. **Live** brings new questions in by themselves, every 5 to 30 seconds (long-press the Live chip to set it); with Live off, ⟳ beside "New questions" reloads them. Search finds topics and people.
- **Home your way** (Settings → Home and reading): the default, a layout **like the website** (hot questions, then today's questions by topic), or **new questions only**; 3, 5 or 10 hot cards, from now, today or this week; the channel chips on or off; which tab the app opens on.
- **Home-screen widgets:** "Hot right now" shows the hottest questions, and **Chats** shows your latest chats with unread counts. Tap a question or a chat to open it, or ⟳ to refresh. Add them from your launcher's widgets, or from Settings → Notifications and widgets.
- **Launcher shortcuts:** touch and hold the app icon for **Hot** and **Chats**.
- **Ask a question** in three steps, like the site: the question (anonymous with your own nickname, and a link if you like), its tags (suggested by Stips), and a cover photo from Stips' gallery.
- **Questions:** the cover photo behind the title (tap ⋮ → View image to see it whole, with zoom), answers 💬 and pins 📌 as symbols, the asker's link, a header that folds into a small title bar as you scroll, and pull down to reload. The asker's own answers say **"שואל השאלה"**, even when anonymous. Organizations and Stips' own account have a **blue check**. Answers in Stips' order, newest, oldest or by votes.
- **Times to the second** under an hour ("5 min 12 s"), set by Stips' clock; turn on *Count the seconds* to watch them tick.
- **Answer** (anonymously too, with one link), **edit**, **delete** or **detach** your answers; **vote**, **pin**, **get updates**. If you answered a question anonymously, your next answer there starts anonymous. As the asker, **send flowers** to good answers.
- **Anonymous mode** (the masks on Home): everything you post goes out anonymously for 30 minutes, a few hours, or until you turn it off.
- **Report and hide:** report a question, answer or pen-pal post to Stips' moderators; hide an item or everything from one user (Settings → Privacy and safety → Hidden content brings it all back).
- **Pen pals (חברים לעט):** read the board, post your own message, and tap a post to start a private chat.
- **Profiles** for everyone: photo (tap to view it big), questions · answers · flowers, bio, badges, age, how long they've been on Stips, the **credibility** gauge, the **thanks wall** first (write on others', delete on yours), and activity graphs. Special accounts show Stips' colors and badges.
- **Chats:** a green dot shows who's online, and the top bar shows who you're talking to (tap it for their profile). New messages arrive within seconds. Bubbles are compact: tap one for its time, long-press to copy it. Scroll up and earlier messages load by themselves, back to a card about the other person where the chat began; a ↓ button counts new messages and jumps back down. ⋮ reports someone to Stips' team or blocks their messages.
- **Notifications:** Stips' list with new ones highlighted until you open them, a badge on the bell and on Chats, and optional **phone notifications**.
- **Welcome:** the first launch offers to sign in, or to look around as a guest.
- **Settings** in pages: account, Home and reading, appearance, notifications and widgets, privacy and safety, Stips' own pages, about. **Expert mode** adds ids, *Server details* (what Stips sent, copyable), load times, and **Diagnostics → Network log** for bug reports.
- **Native feel:** Material 3, your wallpaper's colors (or Stips green), dark mode with pure black, right-to-left Hebrew, haptic feedback, and transitions that follow your finger on predictive back.

## Install

1. On the phone, open the repository's **Releases** page (signed in to GitHub) and tap the newest `stipsplus-<number>.apk`. Each release's notes say what changed and how it was checked: every APK is opened on an Android emulator before it's published. Builds 7–9 crash on launch (#7); don't install them.
2. Allow your browser to install unknown apps when Android asks.
3. If Play Protect warns, choose **Install anyway**. Every build is signed with the same key, so new builds install over old ones.

**From a computer (optional, needs Android's platform tools):** `adb install -r stipsplus-<number>.apk`

**Open Stips links in the app (optional):** Android Settings → Apps → Stips+ → Open by default → add `stips.co.il`. Question, profile and messages links then open in Stips+.

## Privacy

- **No ads, analytics or trackers.** No account of ours.
- **Your password** goes only to Stips, once, when you sign in. The app keeps only the sign-in token Stips returns, in app-private storage that's excluded from backups.
- **Saved questions, settings, hidden content, anonymous mode and load times** stay on the phone. So does the chats widget's copy of your latest chats: it's left out of backups and deleted when you sign out.
- **Crash reports** stay on the phone until you choose *Copy details* or *Report on GitHub*.

**Every network call:**

| Host | When |
|---|---|
| `stips.co.il` | Loading feeds, questions, profiles, pen pals, notifications and chats; signing in; posting questions, answers, votes, pins, flowers, reports and messages; uploading a new profile photo |
| `cdn.stips.co.il` | Question photos and profile pictures (turn off: Settings → Appearance → Show images) |
| `images.unsplash.com` | Stock cover photos that askers picked (same switch) |

**In the background:**

| What | When | Cost |
|---|---|---|
| Unread chats and notifications | Every 10 seconds, only while the app is open and you're signed in | Under 1 KB each |
| An open chat | New messages every 3 seconds, and whether the other person is online once a minute, only while the chat is on screen | Under 1 KB for each check with nothing new, about 1 MB an hour |
| **Live** new questions | Every 5–30 seconds (you choose; 30 by default), only while Home is on screen, and only if you turn it on | About 1 MB an hour at 30 s, 6.7 MB at 5 s |
| **Phone notifications** | About every 15 minutes, only if you turn it on (Settings → Notifications and widgets) | Under 1 MB a day |
| **Home-screen widget** | When Android wakes it (hourly), only if its list is over 30 minutes old; nothing extra when the app loaded the same list recently. Only if you add the widget | About 9 KB each, at most 0.2 MB a day |
| **Chats widget** | When your unread count changes (checked by the app while it's open, or by phone notifications), or hourly when its list is over 30 minutes old; nothing extra when the Chats screen loaded the list. Only if you add the widget | A few KB each |

**Stips' request limit:** Stips stops answering an address after about 100 requests that don't come from its own site, for 20 minutes or more. Requests from the site and the official app carry the site's Referer and aren't limited that way, even from an address Stips is refusing; Stips+ sends the same Referer, and still identifies itself honestly. It also keeps to about the official app's pace: 40 requests at once, then 30 a minute, and the checks above wait while you're busy. If Stips stops answering anyway, a banner counts down, nothing is sent except one try a minute, and everything resumes as soon as Stips answers.

**Network log:** the app keeps its last few hundred requests in memory (which call, the answer code, timing, errors; never passwords, cookies or what you wrote) so you can attach them to a bug report from Settings → Diagnostics (turn on expert mode to see it). Nothing is sent unless you share it.

**Permissions:** Internet and network state (to notice network changes and to schedule the background check); notifications (asked only when you turn phone notifications on, Android 13+); "run at startup", so the 15-minute check survives a restart. Picking a profile photo uses Android's photo picker, so the app never gets access to your other photos.

## Building

There's nothing to install locally: GitHub Actions builds every push to `main` (JDK 17, Gradle 9.5.0, Android Gradle Plugin 9.3.1), runs the unit tests, checks the APK, opens it on an Android emulator, and only then publishes a pre-release with the APK. See [MAINTAINING.md](MAINTAINING.md). To build on your own machine, install Gradle 9.5 and the Android SDK, then run `gradle assembleRelease`.

## How it's made

- Spec and decisions: [SPEC.md](SPEC.md) (with SPEC-01…06).
- Process: [BUILD-PLAYBOOK.md](BUILD-PLAYBOOK.md). Look: [DESIGN-PHILOSOPHY.md](DESIGN-PHILOSOPHY.md).
- Inspired by [better-rail](https://github.com/better-rail/app). No code or assets are copied from it or from Stips.

## License

[GPL-3.0-or-later](LICENSE). "Stips" is a trademark of its owners; this app isn't affiliated with or endorsed by them.

---

## בעברית

**Stips+** היא אפליקציית אנדרואיד מהירה וטבעית לסטיפס: חמות ושאלות חדשות (גם בלייב), ווידג'ט "חם עכשיו" למסך הבית, שאילת שאלות, חברים לעט, ערוצים ונושאים, שאלות ותשובות, התחברות, מענה (גם באנונימיות ועם קישור), מצב אנונימי, עריכת תשובות, נעיצות, פרחים, דיווח וחסימת תוכן, קבלת עדכונים, פרופילים עם נקודות אמינות וקיר תודות, התראות ושיחות פרטיות חיות (מי מחובר/ת, והודעות חדשות תוך שניות, עם דיווח וחסימה), וקיצורי דרך ל"חם עכשיו" ולשיחות מסמל האפליקציה. בלי פרסומות ובלי מעקב. הסיסמה נשלחת רק לסטיפס, והאפליקציה שומרת רק את אסימון ההתחברות. אפליקציה עצמאית, ללא קשר לסטיפס. כרגע היא פרטית, עד שנקבל אישור מסטיפס לפרסם אותה.
