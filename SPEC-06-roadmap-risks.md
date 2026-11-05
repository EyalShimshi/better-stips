# SPEC-06: Roadmap and risks

## 1. Milestones

| Milestone | Contents | Exit criteria |
|---|---|---|
| **M0 Pipeline** | Repo, CI, signing, pre-releases | A green build installs on the owner's phone |
| **M1 v0.1** | SPEC-01 v0.1 scope | Spikes S-01…S-06 pass on the owner's phone |
| M2 v0.2 | Owner feedback: hot-first Home with Live, compact question header, pins, Get updates, answer links/editing/detaching, notifications, social profiles, photo and bio, account settings, motion | Owner uses it daily instead of the official app |
| M2.5 v0.3 | Issues #1–#4: crash and connection fixes, network log, Live rate, ask a question, pen pals, report/block, anonymous mode, flowers, thanks writing, people search | The owner uses posting and moderation daily |
| M3 v0.4 | Widget (shipped in v0.6), shortcuts (shipped in v0.8), tablet layout, lint in CI | DESIGN-PHILOSOPHY §13 checklist passes on every screen |
| M3.5 v0.9 | Issue #8: Home layouts and options, welcome, expert mode, who's who (asker, verified accounts), seconds, settings in pages; the chats widget | The owner sets Home the way they like it |
| M3.6 v0.10 | Easy reply and reply quotes, notifications that open at the new answer, moderators on answers | Replying in busy threads takes one swipe |
| M4 Public | Only with Stips' written permission | Playbook §7 checklist, README in Hebrew and English |

## 2. Spikes

See SPEC-03 §11 (S-01…S-06). All need the owner's Stips account on the phone; the agent never handles the password.

## 3. Risks

| ID | Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|---|
| R-01 | Stips objects: their terms forbid copying/distributing content and trademarks without written permission, and automated copying | Medium | High | Private until permission (D-02); requests only on user action; no mirroring or export; "Open on Stips" everywhere; take down on request |
| R-02 | Stips changes or blocks the API | Medium | High | Tolerant parser, fixtures and probes (MAINTAINING.md). An honest User-Agent; requests carry the site's Referer like the site's own (D-49), which the permission request discloses. If Stips blocks Stips+ itself, we stop and talk to them — no evasion |
| R-03 | Stips penalizes accounts that use a third-party client | Low | High | Ask in the permission request; the owner tests with their own account first |
| R-04 | The name "Stips+" uses Stips' trademark | Medium | Medium | Fine while private; decide the public name with Stips' answer (Q-02) |
| R-05 | Account calls behave differently from the website's code | High | Medium | Tolerant parsing, clear error messages, spikes on the device, fix in the next round |
| R-06 | Compile errors cost CI round trips (no local compiler) | Medium | Low | Versions from a CI-tested sample; signatures checked in source jars; `ci-status` branch for fast feedback |
| R-07 | The signing key is lost | Low | High | CI secret + a backup file on the owner's computer |
| R-08 | Users are mostly minors; their content is personal | Medium | Medium | Nothing stored beyond caches and the user's own saved list; hidden profile fields are never shown; no analytics |
| R-09 | Unsafe content or harassment | Medium | Medium | Report and block content in the app since v0.3 (`reports.send`, `ask/BlockContent`); report users and block their messages from a chat since v0.8 (D-56) |
| R-10 | Stips limits the phone's address (`IP_RATE_LIMIT` after about 100 calls without the site's Referer) and seems "down" (#3, #5) | Low since v0.5 | Medium | The site's Referer (D-49), a budget at the official app's pace (D-46), a pause with one try a minute and a banner, the network log, and the probe workflow to re-measure (D-50) |
| R-11 | A release crashes on launch although it compiles and passes its tests (builds 7–9, #7: R8 removed constructors that a library calls by name) | Low since v0.8 | High | Explicit keep rules and AGP 8's reading of library rules; CI checks those constructors in the APK and opens every build on an emulator before publishing it (D-55) |

## 4. The permission request

**Q-01:** Will Stips allow an independent client? The owner sends this to `contact@stips.co.il` (the address in Stips' Google Play listing). The terms name the operator as "נדב דפני - אינטרנט ותוכנה בע\"מ"; the Play listing names Futurebit Media Ltd.

> **נושא:** בקשת אישור – אפליקציית אנדרואיד עצמאית לסטיפס
>
> שלום לצוות סטיפס,
>
> שמי אייל שמשי, ואני משתמש בסטיפס. אני בונה אפליקציית אנדרואיד חינמית בשם Stips+, שמציגה את התוכן של סטיפס בממשק אנדרואיד טבעי: גלילה חלקה, מצב כהה, טקסט גדול ותמיכה מלאה בעברית. ההשראה היא Better Rail, אפליקציה עצמאית לרכבת ישראל.
>
> כמה דברים חשובים לי לומר מראש:
> - האפליקציה פונה ישירות מהטלפון של המשתמש לאתר שלכם, כמו דפדפן, ורק כשהמשתמש מבקש. אין שרת שלנו, אין העתקה או שמירה של תוכן, ואין סריקה אוטומטית.
> - האפליקציה מזדהה בגלוי כ-Stips+ בכל בקשה, ויש בה קישור "פתיחה בסטיפס" לכל שאלה.
> - כדי שמגבלת הקצב שלכם (IP_RATE_LIMIT) לא תעצור שימוש רגיל, כל בקשה נשלחת עם ה-Referer של האתר (https://stips.co.il/), כמו הבקשות של האתר ושל האפליקציה הרשמית, וקצב הפניות מוגבל לקצב של האפליקציה הרשמית בערך. אם תעדיפו שנפעל אחרת, נשנה.
> - ההתחברות היא עם חשבון סטיפס רגיל. הסיסמה נשלחת רק אליכם, והאפליקציה לא שומרת אותה.
> - אין בה פרסומות, אנליטיקה או מעקב, והיא לא מציגה מידע שלא מוצג באתר.
>
> כרגע האפליקציה פרטית ומשמשת אותי בלבד. לפני שאפרסם אותה (בקוד פתוח, ואולי ב-Google Play), אני מבקש את אישורכם בכתב, כפי שנדרש בתנאי השימוש. אשמח לשמוע אם יש לכם תנאים: שם אחר, הגבלת קצב פניות, ייחוס, שיתוף פרסומות או כל דבר אחר. אשמח גם להראות לכם אותה.
>
> תודה,
> אייל שמשי
> eyalshimshi1@gmail.com

*(English summary: an independent, free Android client; direct, user-initiated requests like a browser; honest identification; the site's Referer on every request so the rate limit treats it like the site, at about the official app's pace, and we'll change that if Stips prefers; no ads, tracking, copying or crawling; private for now; asking for written permission and any conditions before publishing.)*

## 5. Open questions

| ID | Question | Blocks |
|---|---|---|
| Q-01 | Does Stips permit an independent client, and on what conditions? | Going public |
| Q-02 | Public name: keep "Stips+" or rename? | Going public |
| Q-03 | Google Play, or GitHub/Obtainium only? | Going public |
| Q-04 | ~~Notifications: in-app only, or a periodic background check?~~ **Both:** the in-app list always; the background check is opt-in (SPEC D-26) | Answered in v0.2 |
