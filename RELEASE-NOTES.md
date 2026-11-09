## Stips+ 0.17, build 30: every chat, and previews without carets

A quick fix on top of build 29.

- **Every chat is in the list.** Stips sends the chats list a page at a time, and Stips+ only ever asked for the first page. That's why you saw 29 chats and your friend about 34: whatever Stips puts on page 1 for each account. Now the next page loads as you scroll, about a screen before the end, so the list just keeps going, all the way to your first chat.
- **Previews show the message, not the carets.** In the chats list, a last message that opens with carets (`^^`, then a line break) now shows its words, whether or not you hide carets inside chats. The chats widget and message notifications do the same.

Everything else in 0.17 is in [build 29's notes](https://github.com/EyalShimshi/stips-plus/releases/tag/build-29).

**To test**
1. Chats: scroll down. Older chats keep coming without stopping.
2. A chat whose last message starts with `^^`: the list shows the message itself.

**Heads-up**
- As you asked, this build skipped the emulator launch test to ship faster. The build, the APK check and the unit tests still ran. The emulator is signed out, so it couldn't have checked the chats list anyway.
- This release waits for your approval: in the GitHub app, open the run → *Review deployments* → *Approve*.