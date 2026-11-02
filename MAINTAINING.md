# Maintaining Stips+

*For the owner and maintainers: how builds are made, installed and kept healthy.*

## Release flow

1. Push to `main` (the agent does it, or you push from your editor).
2. GitHub Actions ([build.yml](.github/workflows/build.yml)) compiles, signs and publishes a pre-release `build-<run>` with `stipsplus-<run>.apk`, then runs the unit tests. A build takes about 8–12 minutes of your monthly Actions minutes (GitHub Free includes 2,000 a month for private repos).
3. Errors appear in a box on the run's **Summary** page, and in `status.md` on the `ci-status` branch.
4. A red build is fixed before any new work.

**Versions:** `versionCode` is the CI run number. `versionName` is `<major>.<minor>.<run>`; raise `versionMajorMinor` in [app/build.gradle.kts](app/build.gradle.kts) every round.

## Reading CI results without the website

The agent can't read Actions in a private repo, so every run also writes its outcome to the `ci-status` branch:

```powershell
git fetch origin ci-status
git --no-pager show origin/ci-status:status.md
```

`status.md` holds the run number, commit, build and test outcomes, and the error lines. It never contains secrets.

## The signing key

- Every build is signed with one key, so updates install over each other.
- CI restores it from the repository secret `SIGNING_KEYSTORE_BASE64` (Settings → Secrets and variables → Actions) into `~/.android/debug.keystore`. The release build type signs with that keystore (alias `androiddebugkey`, the standard debug password).
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
