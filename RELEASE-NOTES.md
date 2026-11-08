## Stips+ 0.15: notifications in order, animations your way, and swipe actions (#13, #18, #19)

**#13: the notifications list, as you decided**
- **Chips on top:** *All · Updates · Flowers and thanks · System messages*, each with a count of what's new. Tap one to see only that kind (older ones of that kind come with *Load older notifications*); tap it again for everything.
- **A summary line** in *All*: "New since your last visit: 12 new answers on 3 questions · 2 flowers · a thanks message".
- **Stips' team's notices stay on top** (a removal, a report's outcome, a note) as cards, until you open them or mark them read. Only notices from the last 30 days stay pinned.
- **Sections by day:** Today, Yesterday, This week, Earlier. New ones are still bold, with a dot.
- **Long-press a row** for *Mark as read*, and on an update for *Stop updates* on that question (with *Undo*).
- **Optional, off by default:** Settings → Notifications → *Show questions' titles and photos*. Rows about a question then show its title and photo. The app reads each question from Stips once (about 2 KB), one every 1.5 seconds.
- **Phone notifications by kind:** Android now has a channel for each kind: *Updates*, *Flowers and thanks*, *System messages*, and *Messages* as before. Settings → Notifications → *Kinds of notifications* opens each one, so you can give it its own sound or silence it.

**#18: more tweaks**
- **Animations** (Settings → Appearance → Animations):
  - *Full animations*: everything, with screen changes about twice as fast as before.
  - *Less animations*: no sliding between screens. Jumping to an answer, the glow, scrolling up on refresh and the moving elements stay, and they're quicker.
  - *No animations*.
  - *Customized*: a switch for each kind (screens, jumps, scrolling to the top, glow, moving elements, quick). It starts from what you had.
  - If you had animations turned off, you're now on *No animations*.
- **Swiping questions, like Gmail:** in every list (Home, topics, channels, your lists), swipe a question right to pin it (or unpin it) and left to report it. Settings → Home → *Swiping a question* changes each side: nothing, pin, get updates, share, copy link, open on Stips, export, report, hide the question, or hide everything from its asker. The action's color, icon and name show behind the row, and the phone ticks once you've swiped far enough.
- **Swiping answers left:** swiping right still replies. Swiping left does what you pick in Settings → Questions and answers → *Swiping an answer*: on others' answers nothing by default (or report, hide, hide everything from them, edit history), and on your own *Edit* by default (or detach, delete, edit history). A swipe only offers what the answer's ⋮ menu offers.
- **Hiding can be undone:** a hidden question or answer disappears at once, and its message offers *Undo* for a few seconds. Stips is told only after that.
- **The *I use Stips+* button** now lines up with *Edit bio*.

**#19: how does Stips know who's online?**
- Stips' server decides, and sends a plain yes or no (`online`) with each profile, answer and chat. The site draws it as the pulsing green ring; Stips+ shows the same value as its green dot. The server never says what counts as online, or when someone was last seen, so neither the site nor the app can show "last seen".
- "Active" is something else: in Stips' data it means a question or answer that wasn't removed, or an account that wasn't closed. "On Stips for" is Stips' own phrase for how old the account is.
- My guess is that any signed-in request in the last few minutes makes you online. That would include Stips+'s background checks for phone notifications. It's not measured yet; [SPEC-04 §2.14](SPEC-04-data.md#214-online-active-and-on-stips-for-19-from-the-sites-code-2026-11) has the details and a simple test with a second account.

**#17: closed**, as you decided. The iPhone plan stays in [SPEC-07 §3](SPEC-07-proposals.md#3-an-iphone-version-17), marked discarded.

**To test**
1. The bell: tap each chip, read the summary line, long-press a row → *Mark as read*, and long-press an update → *Stop updates* → *Undo*.
2. Settings → Notifications → *Show questions' titles and photos*, then open the bell again.
3. Settings → Notifications → *Kinds of notifications*: each row opens Android's settings for that kind.
4. Settings → Appearance → Animations: try *Less animations*, then *Customized*.
5. Home: swipe a question right (pinned), swipe it right again (unpinned), and swipe one left (the report dialog; cancel it). Then change the swipes in Settings → Home.
6. In a question, swipe your own answer left: the editor opens.
7. Hide a question from ⋮ or a swipe, then tap *Undo*.
8. Your profile: *I use Stips+* next to *Edit bio*.

**Heads-up**
- This release waits for your approval: in the GitHub app, open the run → *Review deployments* → *Approve*. It appears on the Releases page only after that.
- Android's old *Answers and activity* channel is replaced by the three new ones. If you gave it a sound, set it again on the new ones.
- Nothing was tested on a real phone. The CI emulator is signed out, so it never sees the bell, swipes on your own answers, or pins.