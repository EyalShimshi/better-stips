# Maintaining Stips+

*For the owner and maintainers: how builds are made, installed and kept healthy.*

## Release flow

1. Push to `main` (the agent does it, or you push from your editor).
2. GitHub Actions ([build.yml](.github/workflows/build.yml)) compiles and signs the APK, checks it ([tools/check_apk.py](tools/check_apk.py)), runs the unit tests, then opens it on an Android 15 emulator ([tools/launch_test.sh](tools/launch_test.sh)): over the newest earlier release that opens, as a fresh install (which must show the intro's first page, "Welcome"), a second time, and from each launcher shortcut. Then the release job waits for your approval (#14): GitHub notifies you, and in the GitHub app you open the run → *Review deployments* → *release* → *Approve and deploy*. Only then does it publish a pre-release `build-<run>` with `stipsplus-<run>.apk`. The release notes are [RELEASE-NOTES.md](RELEASE-NOTES.md) (update it every round) followed by the launch test's results.
   - **One-time setup:** the first run creates the `release` environment. In the repository's Settings → Environments → **release** → *Required reviewers*, add yourself and save (optionally, *Deployment branches*: `main` only). Until then releases publish without waiting.
3. A build takes about 15 minutes of runner time (free for a public repository): about 5 to build and test, 10 on the emulator. A hotfix you ask to rush can skip the emulator with `[skip launch]` in its commit message (build 30): the release still waits for your approval, and its notes and `status.md` say the launch test was skipped. Don't use it for changes to startup, libraries or R8's rules.
4. Errors appear in a box on the run's **Summary** page, and in `status.md` on the `ci-status` branch. A failed launch test lists each step's verdict and the crash's stack trace; screenshots, logcats and the screen's text are in the run's `launch-test` artifact, kept only when the test fails (for 5 days): they show Stips' live posts, and a public repository's artifacts are public (#14). The summary that goes into release notes and `status.md` holds only verdicts and the app's own words.
5. A red build is fixed before any new work. No pre-release is published for it.

**Versions:** `versionCode` is the CI run number. `versionName` is `<major>.<minor>.<run>`; raise `versionMajorMinor` in [app/build.gradle.kts](app/build.gradle.kts) every round.

### Opening a release on another Android version

Actions → **Launch test** → Run workflow ([launch-test.yml](.github/workflows/launch-test.yml)): a release tag (for example `build-10`), an emulator API level (`35` Android 15, `36` Android 16, `37.0` Android 17), and optionally an earlier release to update from. The Android 17 image took over 10 minutes to boot on GitHub's runners, so that workflow waits up to 20.

### Shrinking rules (#7)

Release builds are shrunk by R8. Libraries that create classes by name need their constructors kept. Since AGP 9, R8 reads library rules written as `-keep class X` strictly and drops the constructor. That crashed builds 7–9 at launch (WorkManager's database, through the widget's Glance). [gradle.properties](gradle.properties) restores AGP 8's reading (`android.r8.strictFullModeForKeepRules=false`), [app/proguard-rules.pro](app/proguard-rules.pro) keeps the known constructors, and `check_apk.py` fails the build if one goes missing. When adding a library that creates classes by name, add its base class to `check_apk.py`.

## Reading CI results without the website

Every run also writes its outcome to the `ci-status` branch, which git can read without the website:

```powershell
git fetch origin ci-status
git --no-pager show origin/ci-status:status.md
```

`status.md` holds the run number, commit, the result of each job (`build-and-tests`, `launch-test`; `release` says "waiting for approval" when both passed, or the launch test was skipped with `[skip launch]`, since the report doesn't wait for you), which key signed the APK (with its certificate digest), the APK size, and the error lines (build errors, the APK check, test failures, the launch test). It never contains secrets.

## The signing key

- Every build is signed with one key, so updates install over each other.
- CI restores it from the repository secret `SIGNING_KEYSTORE_BASE64` (Settings → Secrets and variables → Actions) into `~/.android/debug.keystore`. The release build type signs with that keystore (alias `androiddebugkey`, the standard debug password).
- **Expected certificate SHA-256:** `c0118f1a84a763a7bc13ae6f9c89803c49978745d74bb931b0bb9e16521e352c`. Every run's `status.md` shows `signing:` and `cert-sha256:`; a different digest means the APK won't install over the others.
- **Backup:** `%USERPROFILE%\.android\stipsplus-signing.keystore` on the owner's computer. Keep a second copy somewhere safe. If it's lost, a new key means uninstalling the app (losing saved questions) to update.
- Without the secret, CI signs with a throwaway key and warns in the summary; such builds don't install over the others.
- To recreate the secret from the backup:

  ```powershell
  [Convert]::ToBase64String([IO.File]::ReadAllBytes("$env:USERPROFILE\.android\stipsplus-signing.keystore")) | Set-Clipboard
  ```

## Installing builds

See [README.md](README.md#install). Expect Android's "unknown apps" prompt and Play Protect's *Install anyway*.

## When Stips changes something

Symptoms: feeds or questions stop loading, or show "Stips answered in a way the app doesn't understand".

1. Probe the API from PowerShell (read-only, no account needed):

   ```powershell
   $p = [uri]::EscapeDataString('{"method":"ask.new","page":1}')
   (Invoke-WebRequest "https://stips.co.il/api?name=objectlist&api_params=$p" -UseBasicParsing).Content.Substring(0, 600)
   $o = [uri]::EscapeDataString('{"objType":"ask","data":{"id":19745077}}')
   (Invoke-WebRequest "https://stips.co.il/api?name=omniobj&rest_action=GET&omniobj=$o" -UseBasicParsing).Content.Substring(0, 600)
   ```

2. Compare with [SPEC-04 §2](SPEC-04-data.md#2-api-reference). If the website's bundle changed, download `main.*.js` (linked from `https://stips.co.il/`) and search it for `name:"` to list the API calls.
3. Fix the parser in [Parsers.kt](app/src/main/java/io/github/eyalshimshi/betterstips/data/parse/Parsers.kt), add a trimmed, anonymized fixture under `app/src/test/resources/fixtures/`, and a test.
4. **If Stips blocks the app** (for example by its User-Agent), stop and talk to them. Don't work around it.

**Account calls** (profiles, pins, notifications, thanks wall; SPEC-04 §2.3) can't be probed without signing in. Check them on the phone: open your profile (counts, graphs, thanks wall), the bell on Home, and pin and unpin a question. A profile is public, so `profile.page_data` and `profile.actions_chart` can still be probed from PowerShell with any user id:

```powershell
$p = [uri]::EscapeDataString('{"userid":2042,"action":"ans"}')
(Invoke-WebRequest "https://stips.co.il/api?name=profile.actions_chart&api_params=$p" -UseBasicParsing).Content.Substring(0, 300)
```

## When Stips stops answering (issue #3)

The app keeps a network log: Settings → Diagnostics → Network log (Diagnostics appears once expert mode is on, at the bottom of Settings; Copy, Share, or Report on GitHub, which copies it for pasting). Each line is one request or event:

```
10:01:02.345  http     objectlist -> 200 | h2 reused #1a2b 104.26.13.211 | 312 ms | 9 KB | ray=… cache=DYNAMIC
10:01:05.120  http     messages.count FAILED SocketTimeoutException: timeout | h2 new #3c4d 104.26.13.211 | 20011 ms
10:01:06.410  api      profile.actions_chart: IP_RATE_LIMIT …
10:01:06.410  limit    before this refusal: 31 requests in the last minute, 97 in 5 minutes, 102 in 10 minutes (102 since 3:38 ago)
10:01:06.411  limit    Stips said IP_RATE_LIMIT: no requests for up to 60 s, one try a minute (refusal 1)
10:01:09.000  session  signing in again with the saved token failed: RATE_LIMITED; keeping the session
10:01:30.000  network  now on cellular
```

How to read it:
- **`IP_RATE_LIMIT` and `limit` lines:** Stips refused this address (#5, SPEC-04 §2.10). The first `limit` line says how many requests went out in the last 1, 5 and 10 minutes; "Stips answers again, m:ss after it started refusing" says how long it lasted. During a pause the app sends nothing but one try a minute. If it happens at the app's normal pace, Stips may have changed its rules: compare with the official app (next line).
- **`-> 429` or `-> 403`** (often with `mitigated=…`): Cloudflare is limiting this phone's address; the app pauses the same way.
- **Comparing with the official app:** record it with PCAPdroid and export both the pcap and the CSV (the CSV lists the connections: the API and `cdn.stips.co.il` share one). The traffic is encrypted, but each request's size and timing tell API calls from images: Stips' server answers a feed in 180–430 ms with about 8.7 KB (small calls can be under 100 ms, under 2 KB), while Cloudflare answers cached images in under 80 ms with bigger bodies. PCAPdroid captures IPv4 only by default, so the recording runs on a different address than the phone's IPv6. The official app is the website in a WebView.
- **`FAILED … Timeout` or `ConnectException` on new connections:** the network (or Cloudflare dropping our address) — compare with `network` lines.
- **`FAILED` on `reused` connections only, then fine on `new` ones:** a stale connection; the app drops idle connections after any failure and on network changes.
- **`session` lines** show sign-ins with the saved token; only "rejected the saved sign-in (logged: false)" signs the user out. "kept the signed-in session: … from before the sign-in brought another" is the #9 race being stopped (below); it's harmless.
- **`api … BAD_RESPONSE (body starts: <!DOCTYPE …)`:** Stips sent a web page instead of JSON (a challenge page or an error page).

The log never contains request parameters, cookies, tokens or text the user wrote.

## Phone notifications

- Off by default; signed-in users are offered them once (`NotificationsPrompt`). When turned on (Settings → Notifications and widgets, the card on the notifications screen, or the offer), `Notifier.schedule` registers job **2104201000** with Android's JobScheduler (every 15 minutes, any network, kept across reboots) and sets an alarm for every *How often* minutes while the phone is awake (`NotificationAlarmReceiver`, `ELAPSED_REALTIME`: it never wakes the phone). Turning it off cancels both and forgets what was seen. v0.2–v0.10 used job id 4201; WorkManager (the widgets) numbers its own jobs from 0, so ours moved far above, and the old one is cancelled if it's ours (D-72).
- Each run asks `messages.count` and reads the bell's first page (`notifications.list`) every time since v0.17 (D-122, D-123): other apps on the account zero Stips' count of unseen notifications, so it can't say when to look. Reading it makes Stips treat them as seen, as when the bell is opened on the site; what's new goes to `SeenNotifications.learn` for the bell's badge. `messages.list` is read when the unread count changed or every 30 minutes. `NotifyRules.pick` decides what's new by signature and by Stips+'s own record (D-73, D-123), and `RULES_VERSION` makes the first check after a change of rules only take note. Updates on one question share one phone notification (tag `question`, the question's id). Checks skip while the app is on screen (`Notifier.appVisible`) or the request budget is low; skips and results go to the network log ("notifications").
- **Channels (v0.15):** `stips_answers` (updates), `stips_flowers` (flowers and thanks), `stips_system` (Stips' team and anything unknown) and `stips_messages`; `NotificationCategories.of` picks the channel, the same rule as the bell's chips. `ensureChannels` deletes v0.2–v0.10's `stips_updates` and v0.11–v0.14's `stips_activity`. Android keeps a channel's sound and importance once the user touches them, so changing a channel's defaults means a new id (and deleting the old one).
- Turning them on also asks Android to let Stips+ run in the background (`ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`). Without that, Samsung and others hold jobs back for hours. Settings shows the state, the last check and the fixes (D-74).
- Debug on a connected phone: `adb shell cmd jobscheduler run -f io.github.eyalshimshi.betterstips 2104201000` runs the job now; `adb shell dumpsys jobscheduler | Select-String betterstips` shows when it last ran; `adb shell dumpsys alarm | Select-String betterstips` shows the alarm; `adb shell am get-standby-bucket io.github.eyalshimshi.betterstips` shows Android's standby group (also in expert mode).

## Widgets

- Two Glance widgets: "Hot right now" (`HotWidget`, `HotWidgetFeed`) and Chats (`ChatsWidget`, `ChatsWidgetFeed`). Each feed listens to the repository (`onFirstPage`, `onConversations`, `onUnreadCounts`), so lists that screens load anyway reach the widgets at no cost.
- The chats widget asks Stips only while it's placed (`AppWidgetManager.getAppWidgetIds`), when the unread count changes, hourly when stale, or on ⟳. Its copy (`files/chats_widget.json`) holds private message previews: keep it in the backup exclusions (`res/xml/backup_rules.xml`, `data_extraction_rules.xml`) and deleted on sign-out.
- A widget row opens a chat through the private action `…action.CHAT` with a `stipsplus://chat/<userId>?name=` address (`Shortcuts.chatUri`); each chat's address differs so Android keeps the taps apart. Message notifications open chats the same way (v0.11).

## The website-like Home

`feed.get` (SPEC-04 §2.11) is the site's home. If its sections stop loading, open `https://stips.co.il/` in a browser's developer tools, find the `feed.get` call and compare its parameters (`random_order_number`, `feed_type`) with `StipsRepository.homeSections`. Organizations without a Stips flag are listed in [SpecialAccounts.kt](app/src/main/java/io/github/eyalshimshi/betterstips/SpecialAccounts.kt) (`verified = true`; see "Special accounts" below).

## Replies, notifications and moderators (v0.10)

- **Caret pointers** are read and written in one place, `util/ReplyRefs.kt`, tested on real answers in `RulesV10Test` and `RulesV11Test`. If people start writing pointers another way, add the form there with an example. Counting relies on Stips listing answers oldest first (SPEC-04 §2.12). The answer's text is shown as written; only the *Hide the carets* setting (`hideLeading`) drops the opening pointers.
- **Where a notification opens** is decided in `util/JumpRules.kt`. Stips' notification texts aren't in the site's code, so the "N new answers" count is a guess at their wording. With expert mode on, { } on a notification shows its type, its raw JSON and where the app opens it; fix the patterns there when a text is read wrong.
- **Moderator marks** come from people's profile badges (`profile.page_data`), cached in the `user_badges` prefs for two weeks. If the marks cost too many requests, lower `MAX_BADGE_LOOKUPS` in `QuestionViewModel`, or turn the setting's default off.

## Chats, text and the session (v0.12)

- **Easy reply in chats** uses the same `ReplyRefs` as answers; `ChatRules.replyCount` counts from the bottom of the loaded chat. Stips has no way to edit or delete a message (the site's code has `messages.list`, `from_user`, `get_single_message`, `send`, `count`, `block_user`), so the count is redone once just before sending, after fetching new messages, and never after. The DM settings live in one composable, `ui/chats/ChatSettings.kt`, shown both as the ⚙ sheet on the chats screens and as Settings → DM settings: add DM options there, not in two places.
- **Text arrives HTML-escaped.** Parsers read people's text with `text(key)` / `decodedText()` (`data/parse/JsonAccess.kt`), which decode entities once (`decodeEntities` in `util/TextUtils.kt`). Use them for any new text field; don't decode again later (`&amp;lt;` must stay `&lt;`). Unknown entity names are left as they are; add one to the table when a new one shows up raw.
- **The session cookie (#9).** Stips gives any request without a session cookie a new anonymous one. `CookieGenerationStamp` tags each call with the jar's sign-in generation; `StaleCookieGuard` (a network interceptor, so it runs before OkHttp saves cookies) starts a new generation on each `user.login` answer and drops `Set-Cookie` from answers to calls that started in an older one. Keep both in the client (`StipsApp.httpClient`), in that order. Lists that come back empty and pull-to-refresh on chats and notifications check the session with `user.get_app_user` (`ensureSession(verify = true)`) and fetch again if it had to sign in. `SessionCookieRaceTest` replays the race with MockWebServer.
- **Back to the top:** the bottom bar sends a reselected tab's route through `LocalTabReselects`; a screen joins with `ScrollToTopOnReselect(route, listState)`.

## Credibility, Stips' team and unread bars (v0.13)

- **Stips' rules live in `util/CredRules.kt`** (pure, `RulesV13Test`): the rating table and its rates, editing from 50, blocks under 30 and 20, the projection and the milestones. If Stips changes its rule book, change the numbers there; the profile's gauge and sheet (`ui/user/CredibilitySheet.kt`), the block screen (`ui/account/Account.kt`) and the 50-point gate on easy reply follow. The rating names are strings (`cred_rating_*`).
- **The account** is `SessionStore.account`, read from `user.get_app_user` after the session is restored (`AppContainer.warmUp`), after signing in, and when `StipsApi.onBanned` fires (`API_ERROR_BANNED`, at most once a minute). Points or flowers it leaves out stay null: never treat them as 0, or every account would look blocked. `knownCredibility()` is the screens' way in.
- **The team's notices** are told apart in `util/NotificationKinds.kt`. Their types aren't in the site's code, so the rules read `msgText`, `pointsRemoved`, type words and phrases ("נמחק", "הוסר", "דיווחת"); words count only for the team's notices and unknown types, since ordinary notifications quote question titles. When a real notice is labeled wrong, expert mode's { } shows its type and kind: add the type word or phrase and a test.
- **Unread bars:** questions get theirs from `JumpRules.unread` when a new-answers notification opens them (`QuestionUiState.unread`, dropped on refresh and on posting). Chats get theirs from `ChatRules.firstUnread`: the chats list's `newCount` kept in memory (`StipsRepository.unreadInChat`), else `ChatReadMarks` (the newest message shown per chat). Stips' messages have no read flag; if one appears, prefer it.
- **Edit history (#10):** `omniobj_revision.for_item` lists an item's earlier versions newest first; `Parsers.answerRevisions` reads each one's `jsonstring` (the fields as they were before the next edit) and `editor`, and `EditHistory.versions` (pure, `EditHistoryTest`) lines them up as the site does, filling unchanged fields from the current answer. Questions have the same list (`itemObjType: "ask"`, fields `q`, `text_content`, `tagslist`) if their history is wanted too. If versions ever come out in the wrong order, check the order Stips returns (the site assumes newest first).
- **Reloading on re-tapping a tab:** `ScrollToTopOnReselect(route, listState, onRefresh)` refreshes as it scrolls up; each tab passes its own refresh.

## Special accounts, pictures, export and the settings (v0.14)

- **Special accounts** are in [SpecialAccounts.kt](app/src/main/java/io/github/eyalshimshi/betterstips/SpecialAccounts.kt), at the top of the code: a badge by someone's name (Hebrew and English words, a color) or an organization's blue check (`verified = true`). Add a line, push, and it reaches phones with the release. It's code on purpose (#14): a list downloaded at run time could be changed by anyone who manages to write to the repository, and would put a badge on any account in every installed app. `RulesV14Test` checks the list (unique ids, short one-line badges, opaque colors).
- **Picture links (#15):** [ImageLinks.kt](app/src/main/java/io/github/eyalshimshi/betterstips/util/ImageLinks.kt) knows imgbb pages (`ibb.co/<id>`) and image files on `i.ibb.co`, `i.imgur.com` and `i.postimg.cc`; any other link stays a browser link. An imgbb page is read through its oEmbed answer (`https://ibb.co/<id>/oembed.json`, under 1 KB; `ImageLinkResolver`, at most 16 KB read, no cookies), and only an https image on `i.ibb.co` is taken from it. If imgbb changes that answer, pictures fall back to "couldn't be loaded" with *Open in browser*; check the answer in a browser and fix `ImageLinks.fromOembed`. To add a host, add it to `DIRECT_HOSTS` (image files only) and to the README's network table.
- **Emojis (#12):** questions and answers drop emojis as they're typed ([EmojiFilter.kt](app/src/main/java/io/github/eyalshimshi/betterstips/util/EmojiFilter.kt)): Stips' server cuts their text at the first one. Chats keep them. If Stips ever stores them, remove the filter from the composer, the ask screen and the question editor.
- **Jumps (#12):** a reply's quote or carets move the list as little as possible ([JumpScroll.kt](app/src/main/java/io/github/eyalshimshi/betterstips/util/JumpScroll.kt), pure; [Reveal.kt](app/src/main/java/io/github/eyalshimshi/betterstips/ui/components/Reveal.kt) reads the list's layout): nothing when the item is in sight whole, otherwise just enough to show it a tenth of the screen from the edge. Chats are reversed lists; the plan works in the list's own coordinates.
- **Exporting a thread (#16):** [ThreadExport.kt](app/src/main/java/io/github/eyalshimshi/betterstips/util/ThreadExport.kt) (pure) writes Markdown or JSON; the dialog copies or shares it. Very long threads can be over Android's limit for the clipboard or sharing: the dialog says so instead of crashing.
- **The bio line (#12):** [BioPromo.kt](app/src/main/java/io/github/eyalshimshi/betterstips/util/BioPromo.kt) holds the words and the Google Play link (from the application id). The link works once the app is on Google Play.
- **Settings and the intro (#12):** the pages are `SettingsPage`; their pictures are in [Previews.kt](app/src/main/java/io/github/eyalshimshi/betterstips/ui/settings/Previews.kt), drawn with the theme and the current settings (the DM settings sheet shows `ChatPreview` too). The intro is [IntroScreen.kt](app/src/main/java/io/github/eyalshimshi/betterstips/ui/signin/IntroScreen.kt); its first page must keep the line "Welcome" (`welcome_title`), which the launch test looks for. Settings → About shows it again through `LocalShowIntro`.
- **Signing out (#14)** clears the account's notifications (`Notifier.forgetAccount`) and its local lists (anonymous answers, read marks, chat read marks, reply targets, seen notifications) through `SessionStore.onSignedOut`. A new per-account store should be cleared there too.

## Notifications, motion and swipes (v0.15)

- **The bell's categories (#13)** are in [NotificationCategories.kt](app/src/main/java/io/github/eyalshimshi/betterstips/util/NotificationCategories.kt) (pure, `RulesV15Test`): the category of each `NotificationKinds.Kind`, the day sections, the summary line (it counts answers with `JumpRules.newAnswerCount`) and which team notices pin (`PIN_DAYS`). A new kind goes to *System messages* until it's given a category there. "Opened" marks for pinned notices are kept per account in `SeenNotifications` (newest 300) and cleared on sign-out with the rest.
- **Rows and the badge (v0.17):** `NotificationCategories.rows` puts a question's updates in one row (`rowKey`: `q<question>` for an update, `n<id>` otherwise; the bell, its badge and the phone's notifications all use it), and `focusFor` opens a row at all its new answers (`RulesV17Test`). The badge is `SeenNotifications.waitingRows`: what `learn` found new in a page read outside the bell (`fresh`: past both the bell's `lastSeenId` and the newest id checks read, so a read that crossed the bell's own can't bring back what it showed), until `markSeen` passes it. When to read the page while the app is open is `BellRules` (AppRoot's 10 s loop). If Stips ever sends a per-app or per-item "seen" call, prefer it.
- **Titles and photos on rows** (the *Show questions' titles and photos* option) come from `StipsRepository.cachedQuestion`, else one `omniobj` fetch per question, queued in `NotificationsViewModel` (`GLANCE_GAP_MS` apart, only while `RateGuard.allowBackground()`). The option is off by default because each new question costs a request.
- **Animations (#18)** are `Motion` in `SettingsStore.kt`: six kinds (screens, jumps, scrolling to the top, glow, moving elements, quick) and the presets. Read `LocalAppSettings.current.motion` and check the kind your animation belongs to; don't add a separate switch. Durations that should follow *quick* multiply by `Motion.pace`; the glow is `glowOnce` in [Motion.kt](app/src/main/java/io/github/eyalshimshi/betterstips/ui/components/Motion.kt). Screen transitions are `NavMotion` in `AppRoot.kt`.
- **Swipe actions (#18):** [SwipeActions.kt](app/src/main/java/io/github/eyalshimshi/betterstips/ui/components/SwipeActions.kt) draws a row that slides both ways; [QuestionActions.kt](app/src/main/java/io/github/eyalshimshi/betterstips/ui/question/QuestionActions.kt) runs a question's actions anywhere in the app (`QuestionActionsHost` in `AppRoot` gives it the snackbar and its dialogs) and `SwipeableQuestionRow` is what lists use. Which actions apply to whom is [SwipeRules.kt](app/src/main/java/io/github/eyalshimshi/betterstips/util/SwipeRules.kt). An answer's left swipe runs the entry of its ⋮ menu tagged with the same `AnswerSwipe` (`MenuEntry.swipe` in `QuestionScreen.kt`), so a swipe can't do what the menu wouldn't offer; a new menu entry that should be swipeable needs a tag, a value in `AnswerSwipe` and a place in `SwipeRules`.
- **Hiding with Undo (#18):** `BlockStore.block` hides at once and `unblock` takes it back on *Undo*; Stips hears of it (`StipsRepository.tellStipsHidden`) only after the message goes, through `tellStipsHiddenLater` on the app's scope, so leaving the screen doesn't lose it. Stips has no call to unhide a single item, which is why it waits.
- **Online (#19):** the green ring is Stips' `online` boolean (SPEC-04 §2.14); the app never guesses it.

## Swipes, messages, sorting and refreshing (v0.16)

- **One swipe (D-110):** [SwipeActions.kt](app/src/main/java/io/github/eyalshimshi/betterstips/ui/components/SwipeActions.kt) has the engine: `rememberSwipeDrag` and `Modifier.swipeToAct(drag, onRight, onLeft)`, with `SWIPE_TRIGGER` (64 dp) and `SWIPE_FLING` (125 dp/s); how a release ends is `SwipeRules.release` (pure, `RulesV16Test`). Questions in lists use `SwipeActionsBox`; `AnswerCard` and the chat `Bubble` draw their own icons from `drag.rightProgress`/`leftProgress`. Change how swipes feel there, not in the screens.
- **Messages at the bottom (D-111):** screens use `SwipeAwaySnackbarHost` ([SwipeAwaySnackbar.kt](app/src/main/java/io/github/eyalshimshi/betterstips/ui/components/SwipeAwaySnackbar.kt)), not Material's `SnackbarHost`. The drag is read on a box that doesn't move while the snackbar is drawn moved, so the speed is right; where it goes is `SwipeRules.snackbarExit`.
- **Defaults (D-112):** `SettingsStore.update` saves every setting, so a new default doesn't reach anyone who ever saved one. To move people still on an old default, add it to a migration like `v16DefaultChanges` and raise `DEFAULTS_VERSION`.
- **Sorting answers (D-113):** `AnswerSort` in [AnswerDisplay.kt](app/src/main/java/io/github/eyalshimshi/betterstips/util/AnswerDisplay.kt), kept in `QuestionUiState.sort`, so it lasts as long as the visit. Carets always count in Stips' order (`stipsOrder`), whatever is shown.
- **Refreshing tabs (D-115):** AppRoot marks a tab in `TabRefreshes` ([TabRefresh.kt](app/src/main/java/io/github/eyalshimshi/betterstips/ui/components/TabRefresh.kt)) when the bottom bar switches to it, and the open tab when the app comes back (`ON_START` after `ON_STOP`). A screen joins with `RefreshOnEnter(route) { … }`; merge new items on top (`pollNew`) rather than reload, and skip when its view model loaded less than `AutoRefresh.FRESH_MS` ago. Pen pals' Live is Home's loop: `repeatOnLifecycle(RESUMED)`, `RateGuard.allowBackground()`, `AppSettings.liveDelayMs`; both share `liveIntervalSeconds`.
- **"?" icons (D-117):** use `QuestionIcon` (Components.kt), not `Icons.AutoMirrored.*.HelpOutline`: the auto-mirrored one turns around in right-to-left and reads backwards in Hebrew.
- **The Stips+ badge (D-116):** `BioPromo.has` decides, from the profile already loaded. Keep it off answers: it would cost a profile request per answerer.

## The DM page, notes, emoji codes and the bell's own count (v0.17)

- **Search and the shelf (#20):** in `ChatsScreen` (`ChatsScreens.kt`): the search is the list's first item; chats are matched locally (`ChatRules.matching`), people with `StipsRepository.searchUsers`, the same call as Home's. The shelf is `ChatRules.onlineShelf` over the chats list already loaded: no extra request, and "online" is as fresh as that list.
- **All the chats (build 30):** `messages.list` comes a page at a time (`{page}`), as the site's list reads it; until build 30 Stips+ read only page 1. `ChatsViewModel.loadMore` asks for the next page when the list is `PREFETCH_ROWS` from its end and stops at a page with no one new (`ChatRules.newOnPage`; that also stops it if Stips ever ignores `page`). Quiet refreshes put page 1 on top of what's loaded (`ChatRules.withFirstPage`); pulling to refresh starts again from page 1. Previews of the last message go through `ReplyRefs.preview` (no opening carets, one line) in the list, the widget and message notifications.
- **Notes to self (#20):** `ChatRules.isNotes(userId, myId)`; the screens branch on it (no ⋮, no person's card, `NotesAvatar`). Stips accepts and lists the chat with yourself (SPEC-04 §2.15). Phone notifications skip it already (`pickChats` ignores chats whose last message is your own); the widget leaves it out (`ChatsWidgetFeed.chatsOf(list, myId)`).
- **Stips' system account (#21):** `SpecialAccounts` entries with `official = true` get Stips' blue check through `Parsers.author` and `Parsers.profile` (`isOfficial`).
- **Emoji codes (#22):** [StipsEmoji.kt](app/src/main/java/io/github/eyalshimshi/betterstips/util/StipsEmoji.kt) reads `assets/stips_emoji.txt` once, off the main thread (`StipsApp.warmUp`). `Parsers.conversations` and `messages` and the sent message's echo call `inChat`; nothing else does, as on the site. To refresh the list from the site's bundle: `emojioneList` in its main script, the first of each entry's `unicode` codes, one line per name.
- **The bell (D-122–D-124):** see "Rows and the badge" above. Phone notifications about one question use the tag `question` and the question's id, so a newer burst replaces the last; `Notifier.clearUpdates` clears both tags.
- **Day headers (D-125):** `ChatRules.startsDay` and `dayKind`; `DayHeader` formats with the app's locale. "Today" is read again when messages change, not every second.

## Data files

| File | What | Rule |
|---|---|---|
| `app/src/main/assets/channels.json` | Channel snapshot (ids, Hebrew and English names, colors) | Refresh from `smartdata.get` RemoteConfig when channels change; the unit test checks it |
| `app/src/main/assets/stips_emoji.txt` | EmojiOne 2.2.7's names and code points, as the site uses them in chats (#22; MIT) | `name hex-hex` per line, `#` comments; refresh only if the site's list changes |

## Quick local checks before pushing

From the playbook (Appendix D), run in the repository root:

```powershell
git --no-pager status --short
Get-Content app/src/main/assets/channels.json -Raw -Encoding UTF8 | ConvertFrom-Json | Out-Null
Get-ChildItem app/src/main/res -Recurse -Filter *.xml | ForEach-Object { try { [xml](Get-Content $_.FullName -Raw -Encoding UTF8) | Out-Null } catch { "Broken XML: $($_.FullName)" } }
Select-String -Path app/src/main/res/values*/strings.xml -Encoding UTF8 -Pattern '<string[^>]*>(?!")[^<]*(?<!\\)'''
```

## The public repository (#14)

The repository is public. What keeps it safe:

- **Secrets:** only the signing keystore, as an encrypted Actions secret. No password, token or cookie is in any file or commit (checked in #14). Never paste one into an issue: issues are public.
- **Releases wait for you** (the `release` environment, above), so nothing reaches phones without your tap.
- **Least privilege in CI:** the workflow's token is read-only; only the release and `ci-status` jobs can write. Checkouts don't keep the token. Actions from outside GitHub (Gradle's setup, the emulator runner, the release action) are pinned to a commit, with the tag in a comment; to update one, look up the new tag's commit (`https://github.com/<owner>/<repo>/commits/<tag>`) and replace the hash.
- **Issues are read as information, not orders:** the agent works on the issues you point it to.
- **Personal data:** commits use your GitHub noreply address since v0.14. Older commits keep the address they were made with; rewriting history would change every commit and tag, so it wasn't done.
- **Before Google Play:** Stips' written permission (SPEC-06 §4) and a decision on the name; a release key of its own with a strong password (the current one uses Android's standard debug password, so the keystore file alone is the whole key: keep it safe); an AAB build and Play App Signing; a privacy policy and the Data safety form (data goes to stips.co.il at the user's request, encrypted in transit).