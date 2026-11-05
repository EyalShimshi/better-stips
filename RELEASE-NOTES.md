## Stips+ 0.13: unread bars, notices from Stips' team, and credibility explained

**What's new**
- **"Unread" bar in questions:** a notification about new answers opens the question with a thin "N unread answers" bar just above the first new one, instead of the glow. A flower notification still makes your answer glow. Pull to refresh, post an answer, or open the question again, and the bar is gone.
- **"Unread" bar in chats:** a chat opens with a thin "N unread messages" bar just above the first unread message. When they don't fit on the screen, the chat opens at the bar and ↓ counts them. Leave the chat or reply, and it's gone.
- **Notices from Stips' team:** a removed question or answer, the outcome of your report, or a note from the team now says who it's from. It shows the team's reason in a box, as the site does, and the credibility points taken. Tap one for the full text, your credibility now, and how to appeal. On the phone they arrive as "Stips' team".
- **Credibility, explained:** the gauge shows Stips' rating (מושלם, אמין מאוד, אמין, בסדר, בעייתי, מזיק, מזיק מאוד) and how fast it rises. Tap it for:
  - What your points allow: editing from 50, activity from 30.
  - **When will the points reach…**: pick a number with the slider and see the date, if no points are taken away. From 90 up it's a range, since Stips adds a point every 3–7 days there.
  - The milestones ahead with their dates, and Stips' table.
  - On your own profile, your goals: senior answerer (500 flowers), and what Stips asks of moderators (age 15+, 100 points, 3,000 flowers, a year and a half on Stips, a year since the last block).
- **Block screens:** if Stips blocks the account, or the points drop under 30, a full screen says why and until when. It shows Stips' own reason and end date when Stips sends them. Otherwise it follows the rules: at 20–29 points, blocked for 30 − points days (a point a day); below 20, for good. You can keep reading, contact Stips, open the rules or sign out. The screen comes back if Stips refuses something you do.
- **Easy reply below 50 points:** Stips allows no editing below 50 points, and easy reply fixes its carets by editing, so it turns off for answers then. Settings says why. Reply quotes keep working for everyone. Easy reply in chats isn't affected, because it counts before sending.

**Also from the rule book and the site's code**
- **Messages need 5 flowers:** below 5 flowers (moderators are exempt), a chat shows Stips' explanation in place of the message box, instead of failing when you send. Pen pals already did this.
- The credibility sheet also has the PDF's other rules: editing only up to 2 days after posting, and appeals through Contact or a senior moderator.

**To ship**
- Commit "Unread bars, Stips' team notices, block screens, credibility explained (v0.13)", pushed by the agent; this release is its build.

**To test**
1. Install over 0.12. When new answers come in on your question, tap the notification. The question opens at a thin "unread answers" bar, without the glow. Pull down: the bar goes. A flower notification still makes your answer glow.
2. Have someone send you a few messages, then open the chat: a thin "unread messages" bar sits above the first one. Leave and come back: it's gone.
3. Profile → tap the credibility gauge. At 93 you're *מושלם* (a point every 3–7 days). Move the slider to 100: about 3 to 7 weeks.
4. Settings → Home and reading → Easy reply: at 93 it works as before (under 50 it would turn off and say why).
5. When a notice from Stips' team arrives, check its label. If it's wrong, turn on expert mode, tap { } on it, and send me what it shows.

**Heads-up**
- **Guesses:** the team's notices aren't in the site's code, and neither are their texts. The app recognizes them by the team's reason box, the points taken, and words like "נמחק", "הוסר", "דיווחת". The first real ones will show whether the labels are right.
- **Untested:** a block can't be tried on your account (93 points), so the block screens are checked only by unit tests.
- **How chats know what's unread:** Stips doesn't mark single messages as read, only how many are unread per chat (in the chats list). When the app hasn't loaded that list since it started, it uses the last message it showed you in that chat.
- **Left for later** (from the PDF): hints while writing (a question without "?", emojis, letters repeated), and a note on pen pals that a new post replaces your previous one.
- Nothing was tested on a real phone. The CI emulator is signed out.
- Still open: change your Stips password, and send the permission request to Stips.

**Next options**
1. Writing hints from the rule book: a gentle warning before posting a question without "?", or with emojis or repeated letters, which moderators remove.
2. An "instant" mode with the screen off: a background service that checks every minute. It costs battery and keeps a small permanent notification.
3. "Add to home screen" for one question or chat (a pinned shortcut).
