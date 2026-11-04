## Stips+ 0.8: opens again, and chats get report, block and "jump to newest"

**Fixed: the app closed the moment you opened it (#7).** Every build since the widget (v0.6–v0.7, builds 7–9) crashed before its first screen. The widget's library brings Android's WorkManager, which opens a small database each time the app starts. The build tool that shrinks the APK (R8) removed that database's constructor, because WorkManager only calls it by name. Your phone's bug report and an Android 15 emulator showed the same error: *Failed to create an instance of androidx.work.impl.WorkDatabase*. The fix keeps those constructors, and nothing in the app had to change.

**Why it won't happen again:** a build that compiles and passes its tests can still crash like this, so now every build is opened before it's released:
- an APK check reads the app's code and fails the build if a constructor that a library needs is missing;
- the APK is installed on an Android 15 emulator in GitHub Actions and opened four ways: over the newest earlier release that still opens, as a fresh install, a second time, and from each launcher shortcut;
- the pre-release is published only if all of that passes. The results are at the bottom of these notes.

**What's new**
- **Report and block in chats:** ⋮ in a chat's top bar → *Report* (the site's five reasons, plus optional details) or *Block messages*. While messages from someone are blocked, the composer is replaced by a bar with *Unblock*.
- **Jump to the newest message:** scroll up in a chat and a round ↓ button appears, with a badge counting their messages that arrived meanwhile. Tap it to jump down.
- **Launcher shortcuts:** touch and hold the Stips+ icon for **Hot** (the full hot list) and **Chats**. Drag one to the home screen to keep it there.

**To test on the phone**
1. Install this APK over the old one, then open Stips+. It should open to Home.
2. Open a chat → ⋮ → *Block messages* → *Block*. The composer turns into a "blocked" bar; tap *Unblock* to get it back.
3. In the same chat, ⋮ → *Report*. Pick a reason and tap *Cancel* (sending reaches Stips' moderators).
4. Scroll far up in a chat with someone who's writing to you: the ↓ button counts their new messages; tap it.
5. Touch and hold the app icon: *Hot* opens the hot questions, *Chats* opens your chats.

**Heads-up**
- Builds 7–9 on the Releases page crash on launch. Install this one or a later one.
- Nothing was tested on a real phone. On the emulator the app is signed out, so a signed-in chat, report and block were checked only in code and unit tests.
- Each build now takes about 15 minutes of Actions time instead of 5, because of the emulator.
- Still open: change your Stips password (it was shared for research), and send the permission request to Stips.

**Next options**
1. Open the Android 17 emulator (your phone's version) in the release check too. It's slower to start, so it would add about 10 minutes per build.
2. "Add to home screen" for a single question or chat, as a pinned shortcut.
3. A home-screen widget for unread chats.
