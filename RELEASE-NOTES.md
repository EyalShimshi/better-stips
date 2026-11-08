## Stips+ 0.16, build 27: two touches

- **The sort control is a chip** by the number of answers: the sort icon, the order the answers are in, and ▾. It's much easier to hit than the small icon was. When the answers are in another order than the default, the chip fills in.
- **The question mark faces the right way.** Android turns its question-mark icon around in right-to-left, the way Arabic writes "؟", and in Hebrew that reads backwards. *My questions* on your profile, question notifications in the bell, and the credibility sheet now use a question mark that doesn't turn.

Everything else in 0.16 is in [build 26's notes](https://github.com/EyalShimshi/better-stips/releases/tag/build-26).

**To test**
1. In a question with a few answers, tap the chip by "N answers" and pick an order: the chip fills in. Leave and come back: it's *Default* again.
2. On your profile, the "?" next to *My questions*.

**Heads-up**
- This release waits for your approval: in the GitHub app, open the run → *Review deployments* → *Approve*.
- Nothing was tested on a real phone. The CI emulator is signed out.