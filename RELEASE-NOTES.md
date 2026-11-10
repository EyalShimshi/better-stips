## Stips+ 0.20: no more Play Protect warning, a fresh start on reinstalling

**Play Protect**
- Play Protect called builds 31 and 32 harmful. They were the first that could install their own updates, and to Play Protect a new app that downloads and installs apps looks like malware. From this build, Stips+ can't install apps at all, and doesn't ask for that permission.
- *Update* (on Home, and in Settings → About) now opens the new version in your browser, which downloads it. Open the file and Android asks you to confirm the update, as on the first install.
- Every build is now checked for this before it's published: it fails if it asks to install apps.

**Reinstalling**
- Nothing is backed up or moved to a new phone any more. Uninstalling removes everything, and installing again starts fresh, with the intro. Until now Android brought the old settings back, so the intro was skipped and you were signed out.

**The site**
- www.shimshii.com has its security certificate now, so browsers don't warn about it any more.

**To test**
1. Install this build over 0.19, from this page or with Home's update card. Play Protect may offer to scan it, but it shouldn't call it harmful.
2. Uninstall Stips+ and install this APK again. The intro should show.
3. Settings → About → *Check for updates* says you have the newest version. When the next build is out, *Update* opens it in the browser.
4. Open www.shimshii.com/stips-plus on your phone. There should be no warning.

**Heads-up**
- 0.18 and 0.19 can still install this build with their update card: it's the last update they install themselves.
- If Play Protect still flags this build, you can ask Google to review it (MAINTAINING.md, "Play Protect").
- Tick **Enforce HTTPS** in the `EyalShimshi.github.io` repository → Settings → Pages.
- This release waits for your approval: in the GitHub app, open the run → *Review deployments* → *Approve*.
