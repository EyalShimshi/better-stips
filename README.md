# Stips+

*A fast, native Android app for [Stips](https://stips.co.il), Israel's Hebrew Q&A community. An independent app, not affiliated with Stips.*

> **Status:** private, for personal use, while we ask Stips for permission to publish it ([SPEC-06 §4](SPEC-06-roadmap-risks.md#4-the-permission-request)).

## Features (v0.2)

- **Home:** the five hottest questions as big cards, then the newest questions. Turn on **Live** and new questions slide in by themselves.
- **Feeds:** New and Hot (now · today · this week), with the six channels as color chips. **Topics:** search one, or tap one on a question.
- **Questions:** the cover photo sits behind the title; the question stays pinned on top, small, as you scroll the answers. The asker's link, links you can tap, text you can copy, all answers with scores and flowers.
- **Sign in** with your Stips account, then **answer** (also anonymously, with one attached link), **edit** or **detach** your answers, **vote**, **pin** questions, **get updates** on a question, and **chat** in private messages.
- **Profiles** for everyone: photo, questions · answers · flowers, bio, badges, age, how long they've been on Stips, the **credibility** gauge, activity graphs, answers that got a flower, and the **thanks wall** (delete messages on yours). Change your photo and bio from the app.
- **Notifications:** Stips' notification list with a badge on the bell, an unread badge on Chats, and optional **phone notifications**.
- **Settings:** safe mode, and the Stips account pages (details, password, contact, rules).
- **Native feel:** Material 3, your wallpaper's colors (or Stips green), dark mode with pure black, right-to-left Hebrew, and screen transitions that follow your finger on predictive back.
- **Fast:** the last feed shows instantly on launch; load times are visible in Settings → Performance.

## Install

1. On the phone, open the repository's **Releases** page (signed in to GitHub) and tap the newest `stipsplus-<number>.apk`.
2. Allow your browser to install unknown apps when Android asks.
3. If Play Protect warns, choose **Install anyway**. Every build is signed with the same key, so new builds install over old ones.

**From a computer (optional, needs Android's platform tools):** `adb install -r stipsplus-<number>.apk`

**Open Stips links in the app (optional):** Android Settings → Apps → Stips+ → Open by default → add `stips.co.il`. Question, profile and messages links then open in Stips+.

## Privacy

- **No ads, analytics or trackers.** No account of ours.
- **Your password** goes only to Stips, once, when you sign in. The app keeps only the sign-in token Stips returns, in app-private storage that's excluded from backups.
- **Saved questions, settings and load times** stay on the phone.
- **Crash reports** stay on the phone until you choose *Copy details* or *Report on GitHub*.

**Every network call:**

| Host | When |
|---|---|
| `stips.co.il` | Loading feeds, questions, profiles, notifications and chats; signing in; posting answers, votes, pins and messages; uploading a new profile photo |
| `cdn.stips.co.il` | Question photos and profile pictures (turn off: Settings → Show images) |
| `images.unsplash.com` | Stock cover photos that askers picked (same switch) |

**In the background:**

| What | When | Cost |
|---|---|---|
| Unread chats and notifications | Once a minute, only while the app is open and you're signed in | Under 1 KB each |
| **Live** new questions | Every 30 seconds, only while Home is on screen, and only if you turn it on | About 1 MB an hour |
| **Phone notifications** | About every 15 minutes, only if you turn it on (Settings → Updates) | Under 1 MB a day |

**Permissions:** Internet; notifications (asked only when you turn phone notifications on, Android 13+); "run at startup", so the 15-minute check survives a restart. Picking a profile photo uses Android's photo picker, so the app never gets access to your other photos.

## Building

There's nothing to install locally: GitHub Actions builds every push to `main` (JDK 17, Gradle 9.5.0, Android Gradle Plugin 9.3.1) and publishes a pre-release with the APK. See [MAINTAINING.md](MAINTAINING.md). To build on your own machine, install Gradle 9.5 and the Android SDK, then run `gradle assembleRelease`.

## How it's made

- Spec and decisions: [SPEC.md](SPEC.md) (with SPEC-01…06).
- Process: [BUILD-PLAYBOOK.md](BUILD-PLAYBOOK.md). Look: [DESIGN-PHILOSOPHY.md](DESIGN-PHILOSOPHY.md).
- Inspired by [better-rail](https://github.com/better-rail/app). No code or assets are copied from it or from Stips.

## License

[GPL-3.0-or-later](LICENSE). "Stips" is a trademark of its owners; this app isn't affiliated with or endorsed by them.

---

## בעברית

**Stips+** היא אפליקציית אנדרואיד מהירה וטבעית לסטיפס: חמות ושאלות חדשות (גם בלייב), ערוצים ונושאים, שאלות ותשובות, התחברות, מענה (גם באנונימיות ועם קישור), עריכת תשובות, נעיצות, קבלת עדכונים, פרופילים עם נקודות אמינות וקיר תודות, התראות ושיחות פרטיות. בלי פרסומות ובלי מעקב. הסיסמה נשלחת רק לסטיפס, והאפליקציה שומרת רק את אסימון ההתחברות. אפליקציה עצמאית, ללא קשר לסטיפס. כרגע היא פרטית, עד שנקבל אישור מסטיפס לפרסם אותה.
