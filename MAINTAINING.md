# Maintaining Stips+

*For the owner and maintainers: how builds are made, installed and kept healthy.*

## Release flow

1. Push to `main` (the agent does it, or you push from your editor).
2. GitHub Actions ([build.yml](.github/workflows/build.yml)) compiles and signs the APK, checks it ([tools/check_apk.py](tools/check_apk.py)), runs the unit tests, then opens it on an Android 15 emulator ([tools/launch_test.sh](tools/launch_test.sh)): over the newest earlier release that opens, as a fresh install (which must show the welcome screen), a second time, and from each launcher shortcut. Only then does it publish a pre-release `build-<run>` with `stipsplus-<run>.apk`. The release notes are [RELEASE-NOTES.md](RELEASE-NOTES.md) (update it every round) followed by the launch test's results.
3. A build takes about 15 minutes of your monthly Actions minutes (GitHub Free includes 2,000 a month for private repos): about 5 to build and test, 10 on the emulator.
4. Errors appear in a box on the run's **Summary** page, and in `status.md` on the `ci-status` branch. A failed launch test lists each step's verdict and the crash's stack trace; screenshots, logcats and the screen's text are in the run's `launch-test` artifact.
5. A red build is fixed before any new work. No pre-release is published for it.

**Versions:** `versionCode` is the CI run number. `versionName` is `<major>.<minor>.<run>`; raise `versionMajorMinor` in [app/build.gradle.kts](app/build.gradle.kts) every round.

### Opening a release on another Android version

Actions → **Launch test** → Run workflow ([launch-test.yml](.github/workflows/launch-test.yml)): a release tag (for example `build-10`), an emulator API level (`35` Android 15, `36` Android 16, `37.0` Android 17), and optionally an earlier release to update from. The Android 17 image took over 10 minutes to boot on GitHub's runners, so that workflow waits up to 20.

### Shrinking rules (#7)

Release builds are shrunk by R8. Libraries that create classes by name need their constructors kept. Since AGP 9, R8 reads library rules written as `-keep class X` strictly and drops the constructor. That crashed builds 7–9 at launch (WorkManager's database, through the widget's Glance). [gradle.properties](gradle.properties) restores AGP 8's reading (`android.r8.strictFullModeForKeepRules=false`), [app/proguard-rules.pro](app/proguard-rules.pro) keeps the known constructors, and `check_apk.py` fails the build if one goes missing. When adding a library that creates classes by name, add its base class to `check_apk.py`.

## Reading CI results without the website

The agent can't read Actions in a private repo, so every run also writes its outcome to the `ci-status` branch:

```powershell
git fetch origin ci-status
git --no-pager show origin/ci-status:status.md
```

`status.md` holds the run number, commit, the result of each job (`build-and-tests`, `launch-test`, `release`), which key signed the APK (with its certificate digest), the APK size, and the error lines (build errors, the APK check, test failures, the launch test). It never contains secrets.

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
$p = [uri]::EscapeDataString('{"userid":275580,"action":"ans"}')
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
- **`IP_RATE_LIMIT` and `limit` lines:** Stips refused this address (#5, SPEC-04 §2.10). Stips+ sends the site's Referer (D-49), which Stips' limit lets through, so first run the probe workflow's `app` variant (below): if it's refused too, Stips changed its rule. The first `limit` line says how many requests went out in the last 1, 5 and 10 minutes; "Stips answers again, m:ss after it started refusing" says how long it lasted (over 20 minutes when measured without the Referer). During a pause the app sends nothing but one try a minute.
- **`-> 429` or `-> 403`** (often with `mitigated=…`): Cloudflare is limiting this phone's address; the app pauses the same way.
- **Comparing with the official app:** record it with PCAPdroid and export both the pcap and the CSV (the CSV lists the connections: the API and `cdn.stips.co.il` share one). The traffic is encrypted, but each request's size and timing tell API calls from images: Stips' server answers a feed in 180–430 ms with about 8.7 KB (small calls can be under 100 ms, under 2 KB), while Cloudflare answers cached images in under 80 ms with bigger bodies. PCAPdroid captures IPv4 only by default, so the recording runs on a different address than the phone's IPv6. The official app is the website in a WebView, so its calls carry the site's Referer.
- **`FAILED … Timeout` or `ConnectException` on new connections:** the network (or Cloudflare dropping our address) — compare with `network` lines.
- **`FAILED` on `reused` connections only, then fine on `new` ones:** a stale connection; the app drops idle connections after any failure and on network changes.
- **`session` lines** show sign-ins with the saved token; only "rejected the saved sign-in (logged: false)" signs the user out.
- **`api … BAD_RESPONSE (body starts: <!DOCTYPE …)`:** Stips sent a web page instead of JSON (a challenge page or an error page).

The log never contains request parameters, cookies, tokens or text the user wrote.

### Re-measuring Stips' limit

Actions → **Probe Stips' rate limit** → Run workflow ([probe.yml](.github/workflows/probe.yml), [tools/probe_rate_limit.py](tools/probe_rate_limit.py)). It takes a JSON list of variants; each runs on its own runner, so its own address, with anonymous requests only (the first New page, as a refresh sends):

- `["app"]` (the default): Stips+'s own pattern (its User-Agent and the site's Referer, at the budget's fastest pace for 5 minutes, about 190 requests). Its `RESULT` line should say "never refused"; the job fails if Stips refuses.
- `["limit"]`: without the Referer, one request a second until Stips refuses (about the 102nd), then one try a minute for up to 30 minutes to time the refusal. It triggers the limit on the runner's address on purpose, so run it rarely.
- `["site"]`: a real Chrome page on stips.co.il calling the API as the site does, for reference.

Test from CI, not from a corporate network: a proxy there may isolate web pages or share one address with many people.

## Phone notifications

- Off by default; signed-in users are offered them once (`NotificationsPrompt`). When turned on (Settings → Notifications and widgets, the card on the notifications screen, or the offer), `Notifier.schedule` registers job **2104201000** with Android's JobScheduler (every 15 minutes, any network, kept across reboots) and sets an alarm for every *How often* minutes while the phone is awake (`NotificationAlarmReceiver`, `ELAPSED_REALTIME`: it never wakes the phone). Turning it off cancels both and forgets what was seen. v0.2–v0.10 used job id 4201; WorkManager (the widgets) numbers its own jobs from 0, so ours moved far above, and the old one is cancelled if it's ours (D-72).
- Each run asks `messages.count`; it reads `notifications.list` only when notifications are waiting (Stips then treats them as seen, as when the bell is opened on the site), and `messages.list` when the unread count changed or every 30 minutes. `NotifyRules` decides what's new by signature (D-73). Checks skip while the app is on screen (`Notifier.appVisible`) or the request budget is low; skips and results go to the network log ("notifications").
- Turning them on also asks Android to let Stips+ run in the background (`ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`). Without that, Samsung and others hold jobs back for hours. Settings shows the state, the last check and the fixes (D-74).
- Debug on a connected phone: `adb shell cmd jobscheduler run -f io.github.eyalshimshi.betterstips 2104201000` runs the job now; `adb shell dumpsys jobscheduler | Select-String betterstips` shows when it last ran; `adb shell dumpsys alarm | Select-String betterstips` shows the alarm; `adb shell am get-standby-bucket io.github.eyalshimshi.betterstips` shows Android's standby group (also in expert mode).

## Widgets

- Two Glance widgets: "Hot right now" (`HotWidget`, `HotWidgetFeed`) and Chats (`ChatsWidget`, `ChatsWidgetFeed`). Each feed listens to the repository (`onFirstPage`, `onConversations`, `onUnreadCounts`), so lists that screens load anyway reach the widgets at no cost.
- The chats widget asks Stips only while it's placed (`AppWidgetManager.getAppWidgetIds`), when the unread count changes, hourly when stale, or on ⟳. Its copy (`files/chats_widget.json`) holds private message previews: keep it in the backup exclusions (`res/xml/backup_rules.xml`, `data_extraction_rules.xml`) and deleted on sign-out.
- A widget row opens a chat through the private action `…action.CHAT` with a `stipsplus://chat/<userId>?name=` address (`Shortcuts.chatUri`); each chat's address differs so Android keeps the taps apart. Message notifications open chats the same way (v0.11).

## The website-like Home

`feed.get` (SPEC-04 §2.11) is the site's home. If its sections stop loading, open `https://stips.co.il/` in a browser's developer tools, find the `feed.get` call and compare its parameters (`random_order_number`, `feed_type`) with `StipsRepository.homeSections`. Organizations without a Stips flag are listed in `Organizations.KNOWN_USER_IDS` (`data/model/Models.kt`).

## Replies, notifications and moderators (v0.10)

- **Caret pointers** are read and written in one place, `util/ReplyRefs.kt`, tested on real answers in `RulesV10Test` and `RulesV11Test`. If people start writing pointers another way, add the form there with an example. Counting relies on Stips listing answers oldest first (SPEC-04 §2.12). The answer's text is shown as written; only the *Hide the carets* setting (`hideLeading`) drops the opening pointers.
- **Where a notification opens** is decided in `util/JumpRules.kt`. Stips' notification texts aren't in the site's code, so the "N new answers" count is a guess at their wording. With expert mode on, { } on a notification shows its type, its raw JSON and where the app opens it; fix the patterns there when a text is read wrong.
- **Moderator marks** come from people's profile badges (`profile.page_data`), cached in the `user_badges` prefs for two weeks. If the marks cost too many requests, lower `MAX_BADGE_LOOKUPS` in `QuestionViewModel`, or turn the setting's default off.

## Data files

| File | What | Rule |
|---|---|---|
| `app/src/main/assets/channels.json` | Channel snapshot (ids, Hebrew and English names, colors) | Refresh from `smartdata.get` RemoteConfig when channels change; the unit test checks it |

## Quick local checks before pushing

From the playbook (Appendix D), run in the repository root:

```powershell
git --no-pager status --short
Get-Content app/src/main/assets/channels.json -Raw -Encoding UTF8 | ConvertFrom-Json | Out-Null
Get-ChildItem app/src/main/res -Recurse -Filter *.xml | ForEach-Object { try { [xml](Get-Content $_.FullName -Raw -Encoding UTF8) | Out-Null } catch { "Broken XML: $($_.FullName)" } }
Select-String -Path app/src/main/res/values*/strings.xml -Encoding UTF8 -Pattern '<string[^>]*>(?!")[^<]*(?<!\\)'''
```

## Before going public

- Stips' written permission (SPEC-06 §4) and a decision on the name.
- The playbook §7 checklist: no secrets in history, the signing key only in the encrypted secret, approval required for workflows from outside contributors.
