## Stips+ 0.9: Home your way, a chats widget, and who's who

Everything from issue #8, plus the chats widget from the last round's options.

**What's new**
- **Home your way** (Settings → Home and reading): keep today's layout (the default), switch to one **like the website** (hot questions, then today's questions in sections by topic; tap a topic's heading to open it), or **new questions only**. Also: 3, 5 or 10 hot cards, hot from *now*, *today* or *this week*, the channel chips on or off, and which tab the app opens on.
- **Refresh by hand:** with Live off, a small ⟳ sits beside "New questions". On a question, pull down to reload it and its answers.
- **Who's who:** the asker's own answers say **"שואל השאלה"** ("שואלת השאלה" for "אנונימית"), even when they asked anonymously. Organizations (like מתנדבי עמותת סהר and אבירי הרשת) and Stips' own account get a **blue check**, and their answers a thin blue outline. Profiles of special accounts show Stips' colors and badges, and hide age and numbers where Stips does.
- **Times to the second:** under an hour, times read "5 min 12 s", set by Stips' clock (a phone that's off by a few seconds is corrected). Turn on *Count the seconds* to watch them tick.
- **A welcome screen** on the first launch after installing: sign in, or *Continue as a guest*. It doesn't appear after updates, so you won't see it on your phone unless you reinstall.
- **Expert mode** (bottom of Settings): off, the network log and load times are hidden. On: ids under questions, answers and profiles, ⋮ → *Server details* (everything Stips sent, copyable), load times, and *Diagnostics*.
- **Settings in pages:** Account, Home and reading (also *Order of answers*: as on Stips, newest, oldest, most votes), Appearance, Notifications and widgets, Privacy and safety, Stips, About.
- **Chats widget:** your latest chats on the home screen, with the last message, its time and an unread badge; the title counts unread. Tap a chat to open it.

**Fixed**
- #8 Feature additions: all ten asks.

**To ship**
- Commit "Home layouts, welcome, expert mode, who's who, seconds, settings pages and a chats widget (v0.9)" with "Fixes #8". Pushed by the agent; this release is its build.

**To test**
1. Install over 0.8 and open the app: it opens to Home as before (no welcome screen on an update).
2. Home: turn Live off; a ⟳ appears beside "New questions" and reloads them.
3. Settings → Home and reading → *Like the website*. Home shows the hot cards, then sections such as "#מתמטיקה ›"; tap one to open the topic, scroll down for more. Try *New only* and the hot-card count, then switch back.
4. Open a question, pull down from the top: a spinner comes down under the top bar and the answers reload.
5. Find an answer by the asker (questions where the asker replied): the pill "שואל השאלה" sits after the name. Open profile 253000 (אבירי הרשת): a gradient band, a purple name with a blue ✓, the *מגן מקוון* badge, no age and no numbers.
6. Settings → Home and reading → *Count the seconds*: times under an hour tick every second.
7. Settings → For experts → *Expert mode*: questions and answers show ids; ⋮ → *Server details* shows Stips' JSON; *Diagnostics* appears in Settings.
8. Settings → Notifications and widgets → *Chats widget* (or long-press the home screen → Widgets → Stips+). It lists your chats; send yourself a message from the site and, with the app open, the widget updates within about 10 seconds.
9. Settings → Home and reading → *Open the app on* → Chats. Close the app fully and open it: it starts on Chats.

**Heads-up**
- Nothing was tested on a real phone. The emulator is signed out, so the chats widget's list, verified accounts in answers and the asker's label were checked in code and unit tests only.
- The website-like layout asks Stips for your personal mix when you're signed in, and today's mix otherwise, as the site does.
- The chats widget keeps a copy of your latest chats on the phone for the widget to show. It's left out of backups and deleted when you sign out. While it's placed, it may make a request when your unread count changes.
- The blue check for מתנדבי עמותת סהר comes from a list inside the app: Stips doesn't flag that account. Tell me about others to add.
- Still open: change your Stips password (it was shared for research), and send the permission request to Stips.

**Next options**
1. "Add to home screen" for one question or chat (a pinned shortcut).
2. A preview image for both widgets in the widget picker.
3. The Android 17 emulator in the release check (about 10 more minutes per build).
4. Choosing a cover photo from Unsplash when asking, with our own Unsplash key.
