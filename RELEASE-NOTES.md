## Stips+ 0.16: swipes that feel right, sorting in the question, Pen pals Live, and a Stips+ badge

**Swiping**
- **Questions now swipe like answers.** Questions in lists, answers and chat messages share one swipe, built to Material's standard:
  - The line is at 64 dp, the answers' distance (questions needed 96 before).
  - A quick flick runs the swipe without reaching the line, and a flick back cancels it.
  - A question's row slides further, so you can read the action behind it, and the icon pops at the line.
- **Messages at the bottom swipe away:** swipe one down or to either side and it's gone at once. Everything else about them is the same.

**Questions**
- **A sort button** by the number of answers: *Default*, *Newest first*, *Most upvotes*, *Least upvotes*. It's only for that visit: leave the question and it's back to *Default*. The order setting is gone from Settings.
  - **Is Default the same as oldest first?** Yes. Stips lists answers by id, which is the order they were posted; I measured this on 1,203 answers in 24 busy questions. Flowered answers stay where they were posted. Carets count in this order too, so replies and quotes stay right whatever you sort by.
  - The upvote orders go by the score shown on each answer (ups minus downs, the only number Stips sends).
- **Carets are hidden by default,** in answers and in chats; the quote above shows what they point at. Settings can show them again.

**Pen pals and refreshing**
- **Live on Pen pals:** the chip at the top, as on Home. New posts come in by themselves, only while Pen pals is on screen and the phone is awake. It uses the same rate as Home's Live (long-press the chip to change it).
- **Tabs refresh when you switch to them:** Home, Pen pals and Chats, and the open one when you come back to the app.
  - New questions and posts come in on top, so what you were reading stays put.
  - It doesn't happen on Profile, when you come back from a question, or if the tab loaded in the last 10 seconds.

**Settings and the intro**
- **Less animations is the default**, and **Count the seconds** is on.
- Settings → Questions and answers shows a **picture of swiping answers**: someone's, and one under your own nickname.
- The intro has a **Swipes** page, with questions and answers together, and the carets on the replies page are in the accent color.

**The Stips+ badge**
- Profiles whose bio has the *I use Stips+* line (its words or its link) show a **Stips+** badge in the button's colors. It costs nothing, since the profile is already loaded. It isn't on answers, which would cost a request for each person who answered.
- **Is there a better way to know who uses Stips+?** Not without a server of our own.
  - Only Stips sees that a request comes from Stips+ (the app says so in every request). Stips could show it, so it's worth asking in the permission email.
  - A list of our own would break the "no account of ours" promise, would list mostly teens' accounts, and couldn't confirm anyone.
  - A hidden mark in posts would be secret tracking that changes people's words.

**To test**
1. Home: flick a question right (it's pinned) and left (the report dialog; cancel it). Then drag one slowly past the line and back: nothing happens.
2. Pin something, then swipe the message at the bottom down or to the side.
3. In a busy question, try each order from the sort button by "N answers", then leave and come back (it's *Default* again).
4. Settings → Questions and answers: the new picture, with your nickname.
5. Settings → About → *Show the intro again*: the Swipes page, and the colored carets on the replies page.
6. Pen pals: tap the Live chip, and long-press it for the rate.
7. Switch between Home, Pen pals and Chats; leave the app and come back.
8. Open a profile whose bio has the Stips+ line (yours, if you added it).

**Heads-up**
- **Some of your settings changed, once:**
  - *Full animations* became *Less*.
  - *Count the seconds* is on.
  - Carets are hidden in answers and chats.

  Stips+ saves every setting whenever one changes, so it can't tell a default from a choice. If you picked the old ones on purpose, set them again (Settings → Appearance, Questions and answers, DM settings).
- A quick flick now runs a swipe on answers and chat messages too. If replying by swipe feels too eager now, tell me.
- This release waits for your approval: in the GitHub app, open the run → *Review deployments* → *Approve*.
- Nothing was tested on a real phone. The CI emulator is signed out.