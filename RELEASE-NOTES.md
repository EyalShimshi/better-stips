## Stips+ 0.11: phone notifications that arrive, and replies as written

**What's new**
- **Checks every few minutes:** while you use your phone, Stips+ checks Stips every 2 minutes (or 1 or 5: Settings → Notifications and widgets → *How often to check*), and right after you wake the phone. With the screen off, Android runs a check about every 15 minutes. Nothing is checked while Stips+ itself is open; its own badges are live there.
- **Notifications that pop up:** two channels, *Answers and activity* and *Messages*, both shown at the top of the screen. The old *Stips updates* channel is gone.
- **One notification per chat:** who wrote and their last message; tap it to open the chat. Opening the chat clears it, and so does reading the chat anywhere else. Opening the bell clears the notifications about answers and flowers.
- **A status panel** (Settings → Notifications and widgets, while they're on): when the last check ran and what it found, and anything in Android that holds notifications back, each with a button to fix it: notifications turned off for Stips+, restricted background use, or battery saving. **Check now** runs a check on the spot; **Send a test** shows a sample notification.
- **A one-time offer:** signed in with phone notifications off, the app asks once whether to turn them on. If they're on but Android is saving battery on Stips+, it asks once to let it run in the background.
- **Replies keep their carets:** an answer that points up still shows the quote of the answer it replies to, and its own text now stays exactly as written, "^^" included. The carets are tappable too and jump to the same answer. To get the old look back: Settings → Home and reading → *Hide the carets* (off by default).

**Fixed**
- "I'm not getting any push notifications from the app." There was no single proven cause, so each likely one is now handled:
  - Phone notifications were off by default and never offered. A reinstall (for example during the 0.6–0.7 launch crash) turned them off.
  - Android's battery saving, which Samsung applies strongly, could hold the 15-minute check back for hours. Turning notifications on now asks Android to let Stips+ run in the background, and the alarm checks every few minutes whenever the phone is awake.
  - The check missed a notification that Stips updated in place ("3 new answers" with the same id). It also missed a new message when one chat was read as another arrived. It now remembers each notification and chat by its content.
  - A widget update could one day replace the check's background job. Widgets use WorkManager, which numbers its own jobs from 0, and the check used number 4201. The check now uses a number far from those.
- Reply quotes no longer remove the carets from the answer they belong to (your request): the message is shown as its author wrote it.

**To ship**
- Commit "Phone notifications that arrive, and reply quotes that keep the carets (v0.11)", pushed by the agent; this release is its build.

**To test**
1. Install over 0.10 and open the app. If phone notifications are off, a dialog offers to turn them on. Tap **Turn on**, allow notifications, then allow Stips+ to run in the background when Android asks. If they're already on, it asks only about running in the background.
2. Settings → Notifications and widgets. The panel says "No check yet" or when the last check ran. Tap **Send a test**: a notification pops up at the top of the screen. Tap **Check now**: it says "Nothing new" or how many it sent.
3. Leave the app with the home button. Ask someone to message you, or answer one of your questions from the site. Within about 2 minutes a notification pops up. A message shows who wrote and what, and tapping it opens the chat.
4. Open the bell in Stips+: the notifications about answers and flowers leave the phone's shade.
5. If nothing arrives, the panel's status line and warnings say why. Send me those lines. In expert mode the panel also shows Android's standby group.
6. Open a busy question: an answer that starts with "^^" shows its quote above and still starts with "^^" below it. Tap the carets: the list jumps to the answer they point at. Turn on Settings → Home and reading → *Hide the carets*: the "^^" goes, and the quote stays.

**Heads-up**
- **This is still checking, not push.** Stips sends push messages only to its own app. Stips+ checks every 2 minutes while the phone is awake. Once the screen has been off for a while, Android allows a check about every 15 minutes, and only with the background permission; without it, possibly hours. So a notification can come up to about 15 minutes late while the phone sleeps, and right after you pick it up.
- **Data:** each check is one tiny request (under 1 KB), and lists are read only when something may be new. That's about 30 checks an hour while you use the phone.
- **Checking the list counts as seeing it.** As on the site, Stips marks notifications seen when their list is read, so after a phone notification the bell's badge may already be clear. The new ones still show under *New* in the bell.
- **The official Stips app:** if it's on the phone too, you'll get notifications from both apps. You may want to turn off the official app's.
- Nothing was tested on a real phone. The emulator in CI is signed out, so the checks were tested in code and unit tests only. The status panel is there to show what happens on your phone.
- Still open: change your Stips password (it was shared for research), and send the permission request to Stips.

**Next options**
1. An "instant" mode with the screen off: a background service that checks every minute. It costs battery and keeps a small permanent notification.
2. Two replies in one answer: swipe a second answer to add its line, as people already do by hand.
3. "Add to home screen" for one question or chat (a pinned shortcut).
4. The Android 17 emulator in the release check (about 10 more minutes per build).