## Stips+ 0.10: easy reply, reply quotes, and notifications that open in place

**What's new**
- **Easy reply:** swipe an answer from left to right (also ⋮ → *Reply*). Your draft starts with the carets that point to it, then a new line: "^", "^^", "^^^", and from four up a number, "4^". A bar above the box says who you're replying to; ✕ cancels. After you send, the app reloads the answers. If others came in while you wrote, it edits your answer so the carets point to the right one. A short message tells you when that happens. You can turn it off in Settings → Home and reading.
- **Reply quotes:** any answer that points up with carets now shows the answer it replies to, as a quote like in WhatsApp: the author and the first two lines. Tap the quote to go there; the answer lights up. This works for everyone's answers and every way people write it: "^^" on its own line, "^^text", "4^", "^4", stacked lines replying to two answers, or a caret later in the text (that one is bold and tappable).
- **Notifications open in place:** a notification about new answers opens the question at the first new one (the third from the bottom when there are three). A flower opens it at your answer that got the flower. The answer breathes for two seconds, then goes back to normal. This works from the bell and from phone notifications.
- **"נאמן סטיפס" on answers:** moderators have a purple shield badge by their name, as on their profiles ("נאמנת סטיפס", "נאמן בכיר"…).
- **Smaller changes:** the ⟳ beside "שאלות חדשות" is gone, since pulling down does the same. Seconds now show only in the first minute ("לפני 12 שניות"), then minutes as before.

**Fixed**
- Your requests in chat after 0.9: everything listed above.

**To ship**
- Commit "Easy reply, reply quotes, notifications that open in place, moderator badges (v0.10)", pushed by the agent; this release is its build.

**To test**
1. Install over 0.9. Home: the ⟳ next to "שאלות חדשות" is gone, and a brand-new question says "לפני 12 שניות" and then "לפני דקה".
2. Open a busy question and drag an answer to the right. A ↩ appears, and the phone ticks once you've pulled far enough. Let go: the box opens with "^^" (or whatever count fits) on the first line and the keyboard up. Write something and send.
3. If answers came in while you wrote, a message says your reply was updated; the carets on your answer now point to the right one.
4. Scroll through answers that start with "^" or "^^": each shows a quote of the answer it replies to. Tap a quote: the list jumps there and that answer lights up.
5. Open the bell and tap "new answers" on one of your questions: it opens at the first new answer, which breathes. Do the same with a flower notification.
6. Look for a moderator answering anywhere: the purple "נאמן סטיפס" badge sits by the name. Badges are looked up the first time each person shows up, so one may appear a second or two after the answer.

**Heads-up**
- **Where notifications open is partly a guess.** Stips sends only the question with a notification, and its texts are written on Stips' server, so I couldn't see one. The app reads a count from the text ("3 תשובות חדשות"), and otherwise uses the newest answer it showed you there last time. If a notification opens at the wrong answer: Settings → For experts → Expert mode, then the { } button on that notification. Send me what it shows and I'll fit the rule to the real text.
- **The moderator badge costs requests.** Stips doesn't mark moderators on answers, so the app reads each answerer's profile badges once every two weeks. That's at most 30 lookups per question you open, one every 1.5 seconds, and only when the request budget has room. It can be turned off in Settings → Home and reading.
- **The caret correction edits your answer**, and Stips keeps the previous version in the answer's history (like any edit). It only happens while Stips still lets you edit, and only if your answer still starts with carets.
- Nothing was tested on a real phone. On the emulator the app is signed out, so easy reply, the correction and the notification jumps were checked only in code and unit tests. The caret reading was tested on real answers (213 with carets out of 1,203).
- Still open: change your Stips password (it was shared for research), and send the permission request to Stips.

**Next options**
1. Two replies in one answer: swipe a second answer to add its line ("^^^" then "^"), as people already do by hand.
2. "Add to home screen" for one question or chat (a pinned shortcut).
3. A preview image for both widgets in the widget picker.
4. The Android 17 emulator in the release check (about 10 more minutes per build).
