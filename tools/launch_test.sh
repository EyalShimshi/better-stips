#!/usr/bin/env bash
# Opens Stips+ on the running emulator the way the owner opens it on the phone, and fails if it crashes, stops
# responding or quits (#7). build.yml runs it before every release; launch-test.yml runs it on demand.
#   APK        the build to test (default app.apk)
#   OLD_APKS   earlier builds, newest first, separated by spaces. The newest one that opens is installed first, so the
#              build is also opened over its data. Skipped (with a warning) when none opens.
#   SHORTCUTS  "yes" to also open both launcher shortcuts and check where each one lands
#   LAUNCH_WAIT  seconds to watch each launch (default 25)
# Screenshots, logcats and the screen's text for each step go to launch-test/. The verdicts, any crash's stack trace
# and the screens' text go to the run's summary and to launch-test/summary.md (the release notes include it).
set -u

PKG=io.github.eyalshimshi.betterstips
ACTIVITY="$PKG/.MainActivity"
APK=${APK:-app.apk}
OLD_APKS=${OLD_APKS:-}
SHORTCUTS=${SHORTCUTS:-no}
WAIT=${LAUNCH_WAIT:-25}
OUT=${OUT_DIR:-launch-test}
mkdir -p "$OUT"
REPORT="$OUT/summary.md"
: > "$REPORT"
failures=0

say() {
  echo "$*"
  echo "$*" >> "$REPORT"
}

adb wait-for-device
adb shell svc power stayon true >/dev/null 2>&1 || true
adb shell wm dismiss-keyguard >/dev/null 2>&1 || true
# "App keeps stopping" dialogs from crashing earlier builds would cover the screens checked later.
adb shell settings put global hide_error_dialogs 1 >/dev/null 2>&1 || true
api=$(adb shell getprop ro.build.version.sdk | tr -d '\r')
release=$(adb shell getprop ro.build.version.release | tr -d '\r')

# The app loads feeds as soon as it opens; give the emulator up to a minute to get online (best effort).
online="no"
for _ in $(seq 1 30); do
  if adb shell dumpsys connectivity 2>/dev/null | grep -q "VALIDATED"; then
    online="yes"
    break
  fi
  sleep 2
done
say "### Opened on an emulator (Android $release, API $api; online: $online)"
say ""

# The text on screen, read from the accessibility tree: where the app landed, without looking at a picture.
screen_text() {
  local label=$1
  : > "$OUT/$label-text.txt"
  if timeout 45 adb shell uiautomator dump /sdcard/window.xml >/dev/null 2>&1 \
    && adb shell cat /sdcard/window.xml > "$OUT/$label-window.xml" 2>/dev/null; then
    grep -o ' text="[^"]*"' "$OUT/$label-window.xml" | sed -e 's/^ text="//' -e 's/"$//' \
      | grep -v '^$' | awk '!seen[$0]++' > "$OUT/$label-text.txt" || true
  fi
}

# open_app LABEL MODE [am start arguments]: opens the app (like a tap on its icon, without arguments) and watches it.
# MODE "count": a failure fails the test; "probe": only reported (used for earlier builds). Sets $verdict.
open_app() {
  local label=$1 mode=$2
  shift 2
  adb logcat -c >/dev/null 2>&1 || true
  adb logcat -b crash -c >/dev/null 2>&1 || true
  adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS >/dev/null 2>&1 || true
  if [ $# -eq 0 ]; then
    set -- -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n "$ACTIVITY"
  fi
  # A process that dies while starting can keep "am start -W" waiting for minutes.
  timeout 40 adb shell am start -W "$@" > "$OUT/$label-start.txt" 2>&1 || true
  local waited=0 crashed="no"
  while [ "$waited" -lt "$WAIT" ]; do
    sleep 2
    waited=$((waited + 2))
    if adb logcat -d -b crash 2>/dev/null | grep -q "Process: $PKG"; then
      crashed="yes"
      break
    fi
  done
  adb exec-out screencap -p > "$OUT/$label.png" 2>/dev/null || true
  adb logcat -d -v threadtime > "$OUT/$label-logcat.txt" 2>/dev/null || true
  adb logcat -d -b crash -v threadtime > "$OUT/$label-crash.txt" 2>/dev/null || true
  local pid
  pid=$(adb shell pidof "$PKG" 2>/dev/null | tr -d '\r')
  verdict="ok"
  if [ "$crashed" = "yes" ] || grep -q "Process: $PKG" "$OUT/$label-crash.txt" "$OUT/$label-logcat.txt"; then
    verdict="CRASHED"
  elif grep -q "ANR in $PKG" "$OUT/$label-logcat.txt"; then
    verdict="NOT RESPONDING"
  elif [ -z "$pid" ]; then
    verdict="NOT RUNNING after ${WAIT}s"
  fi
  if [ "$verdict" = "ok" ]; then
    screen_text "$label"
    say "- **$label:** opened, still running after ${WAIT}s"
    if [ -s "$OUT/$label-text.txt" ]; then
      say "  - on screen: $(head -n 14 "$OUT/$label-text.txt" | paste -sd '|' - | sed 's/|/ · /g' | cut -c1-400)"
    fi
    return
  fi
  if [ "$mode" != "count" ]; then
    say "- $label: $verdict (an earlier build, only for reference)"
    return
  fi
  say "- **$label: $verdict**"
  echo "::error::$label: $verdict"
  failures=$((failures + 1))
  {
    echo '```'
    grep -h -A 60 "FATAL EXCEPTION" "$OUT/$label-crash.txt" 2>/dev/null \
      | grep "AndroidRuntime" | sed -E 's/^.*AndroidRuntime: //' | awk '!seen[$0]++' | head -n 40
    grep -h -A 12 "ANR in $PKG" "$OUT/$label-logcat.txt" 2>/dev/null | head -n 15
    echo '```'
  } | tee -a "$REPORT"
}

# expect_text LABEL TEXT: the screen of step LABEL must show TEXT (checks where a shortcut landed).
expect_text() {
  local label=$1 text=$2
  if [ "$verdict" != "ok" ]; then
    return
  fi
  if [ ! -s "$OUT/$label-text.txt" ]; then
    say "  - couldn't read the screen to check for \"$text\""
    echo "::warning::$label: couldn't read the screen to check for \"$text\""
  elif grep -qxF "$text" "$OUT/$label-text.txt"; then
    say "  - shows \"$text\", as expected"
  else
    say "  - **expected \"$text\" on screen**"
    echo "::error::$label: expected \"$text\" on screen"
    failures=$((failures + 1))
  fi
}

# 1. Over an earlier build's data: the newest earlier build that opens, then this one installed over it.
base=""
for old in $OLD_APKS; do
  name=$(basename "$old" .apk)
  adb uninstall "$PKG" >/dev/null 2>&1 || true
  if ! adb install "$old" > "$OUT/install-$name.txt" 2>&1; then
    say "- $name: couldn't install ($(tail -n 1 "$OUT/install-$name.txt"))"
    continue
  fi
  open_app "earlier-$name" probe
  if [ "$verdict" = "ok" ]; then
    base=$name
    break
  fi
done
if [ -n "$base" ]; then
  adb shell am force-stop "$PKG"
  if adb install -r "$APK" > "$OUT/install-upgrade.txt" 2>&1; then
    open_app "update-over-$base" count
  else
    say "- **didn't install over $base:** $(tail -n 1 "$OUT/install-upgrade.txt")"
    echo "::error::the build didn't install over $base"
    failures=$((failures + 1))
  fi
elif [ -n "$OLD_APKS" ]; then
  say "- no earlier build opened, so opening over an earlier build's data wasn't tested"
  echo "::warning::no earlier build opened; the update path wasn't tested"
fi

# 2. A fresh install, opened twice (the second time with its own data).
adb uninstall "$PKG" >/dev/null 2>&1 || true
if ! adb install "$APK" > "$OUT/install.txt" 2>&1; then
  say "- **couldn't install the build:** $(tail -n 1 "$OUT/install.txt")"
  echo "::error::couldn't install the build"
  exit 1
fi
open_app fresh-install count
adb shell am force-stop "$PKG"
open_app second-launch count

# 3. Both launcher shortcuts, as the launcher sends them (signed out, Chats shows its sign-in card).
if [ "$SHORTCUTS" = "yes" ]; then
  adb shell dumpsys shortcut > "$OUT/shortcuts.txt" 2>/dev/null || true
  if grep -q "id=hot" "$OUT/shortcuts.txt" && grep -q "id=chats" "$OUT/shortcuts.txt"; then
    say "- launcher shortcuts registered: hot, chats"
  else
    say "- launcher shortcuts not listed by \`dumpsys shortcut\` (see shortcuts.txt)"
    echo "::warning::launcher shortcuts not found in dumpsys shortcut"
  fi
  adb shell am force-stop "$PKG"
  open_app shortcut-hot count -a "$PKG.action.HOT" -n "$ACTIVITY" -f 0x10008000
  expect_text shortcut-hot "Hot questions"
  adb shell am force-stop "$PKG"
  open_app shortcut-chats count -a "$PKG.action.CHATS" -n "$ACTIVITY" -f 0x10008000
  expect_text shortcut-chats "Sign in to see your chats"
fi

say ""
if [ "$failures" -eq 0 ]; then
  say "Result: passed."
else
  say "Result: **$failures problem(s)**."
fi
if [ -n "${GITHUB_STEP_SUMMARY:-}" ]; then
  cat "$REPORT" >> "$GITHUB_STEP_SUMMARY"
fi
exit "$failures"
