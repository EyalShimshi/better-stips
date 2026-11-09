## Stips+ 0.17, build 29: the DM page, notes to self, and a bell you can trust

- **Search on the DM page (#20).** Chats has a search field on top: type a name and your chats with that person show at once, then people from all of Stips. Tap someone and the chat opens (not their profile). Back clears the search. Home's search is still there.
- **Who's online (#20).** Under the search, a shelf like Instagram's shows the people from your recent chats who are online now, the most recent on the right. It scrolls sideways when they don't fit. Tap one to open the chat.
- **Notes to self (#20).** The first circle on the shelf is your photo with a bookmark: a chat with yourself for drafts, links and reminders. Stips keeps it like any chat, so it's on the site too. It reads as notes, not as a chat with someone: *Notes to self* on top, no report or block, no "online", no card about a person, and the box says *Note…*. It never sends you a notification and stays out of the chats widget.
- **Dates in chats.** Each day starts with its date, as in WhatsApp: *Today*, *Yesterday*, the weekday within the week, then the date.
- **The bell's red number shows up.** Stips stops counting notifications as unread as soon as any app on your account opens the list: the official app, or the site. So Stips+ was often told there was nothing new, and you only found out by opening the bell. Now Stips+ keeps its own record of what you've seen in its bell, and reads the bell's first page itself: when Stips counts something new, and every minute while Home is open.
- **Phone notifications about answers arrive.** Same cause: the phone check only looked when Stips counted something unread, so it never posted answers. Now it reads the bell's first page on every check and decides what's new by what you've seen in Stips+.
- **A question's updates are one row.** In the bell, all the updates about one question share a row where the newest is: its words, and "3 updates · 7 new answers". Tap it to open the question at the first new answer; the arrow shows each update. On the phone, one notification per question, replaced when more answers come.
- **Stips' system account (#21).** "מערכת סטיפס" (user 50916) gets Stips' blue check.
- **Emoji codes in chats (#22).** People on older Stips apps sent codes like `היי:nerd:`. The site shows them as emoji in chats, and so does Stips+ now: `היי🤓`. Stips+ never sends codes; real emoji are the way.

**To test**
1. Chats: type part of a name in the search, then tap the person. The chat opens.
2. The shelf: who's online right now, with your notes first. Write yourself a note, and check that the chat has no ⋮.
3. A chat that goes back a few days: a date above each day.
4. The bell: when new answers come in (with the official app on the phone too), the red number shows on Home. Open the bell: a busy question is one row, and the arrow shows its updates.
5. Leave the app and wait for an answer on a question you follow: a phone notification. Another answer on the same question updates the same notification.

**Heads-up**
- After you update, the bell's number may count notifications you saw in the official app or on the site but not in Stips+'s bell. Open the bell once and it starts fresh.
- The first phone check after updating only takes note, so nothing old pops up. Notifications start with the check after it.
- Reading the bell's first page makes Stips count those notifications as seen, as opening the bell on the site does. So the official app's bell may stop showing a number for things Stips+ already read.
- This release waits for your approval: in the GitHub app, open the run → *Review deployments* → *Approve*.
- Nothing was tested on a real phone. The CI emulator is signed out.