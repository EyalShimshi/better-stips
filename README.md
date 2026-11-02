# Stips+

*A fast, native Android app for [Stips](https://stips.co.il), Israel's Hebrew Q&A community. An independent app, not affiliated with Stips.*

> **Status:** private, for personal use, while we ask Stips for permission to publish it ([SPEC-06 §4](SPEC-06-roadmap-risks.md#4-the-permission-request)).

## Features (v0.1)

- **Feeds:** New and Hot (now · today · this week), with the six channels as color chips.
- **Topics:** search a topic, or tap one on a question.
- **Questions:** cover photo, links you can tap, text you can copy, all answers with scores and flowers. Questions in a channel take that channel's colors.
- **Sign in** with your Stips account, then **answer** (also anonymously), **vote**, and **chat** in private messages.
- **Profile:** your questions, questions you answered, and questions you saved on the phone.
- **Native feel:** Material 3, your wallpaper's colors (or Stips green), dark mode with pure black, right-to-left Hebrew, predictive back.
- **Fast:** the last feed shows instantly on launch; load times are visible in Settings → Performance.

## Install

1. On the phone, open the repository's **Releases** page (signed in to GitHub) and tap the newest `stipsplus-<number>.apk`.
2. Allow your browser to install unknown apps when Android asks.
3. If Play Protect warns, choose **Install anyway**. Every build is signed with the same key, so new builds install over old ones.

**From a computer (optional, needs Android's platform tools):** `adb install -r stipsplus-<number>.apk`

**Open Stips links in the app (optional):** Android Settings → Apps → Stips+ → Open by default → add `stips.co.il`.

## Privacy

- **No ads, analytics or trackers.** No account of ours.
- **Your password** goes only to Stips, once, when you sign in. The app keeps only the sign-in token Stips returns, in app-private storage that's excluded from backups.
- **Saved questions, settings and load times** stay on the phone.
- **Crash reports** stay on the phone until you choose *Copy details* or *Report on GitHub*.

**Every network call:**

| Host | When |
|---|---|
| `stips.co.il` | Loading feeds, questions and chats; signing in; posting answers, votes and messages |
| `cdn.stips.co.il` | Question photos and profile pictures (turn off: Settings → Show images) |
| `images.unsplash.com` | Stock cover photos that askers picked (same switch) |

**Permissions:** Internet only.

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

**Stips+** היא אפליקציית אנדרואיד מהירה וטבעית לסטיפס: פיד חדשות וחמות, ערוצים ונושאים, שאלות ותשובות, התחברות, מענה (גם באנונימיות), הצבעות ושיחות פרטיות. בלי פרסומות ובלי מעקב. הסיסמה נשלחת רק לסטיפס, והאפליקציה שומרת רק את אסימון ההתחברות. אפליקציה עצמאית, ללא קשר לסטיפס. כרגע היא פרטית, עד שנקבל אישור מסטיפס לפרסם אותה.
