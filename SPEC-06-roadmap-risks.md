# SPEC-06: Roadmap and risks

## 1. Milestones

| Milestone | Contents | Exit criteria |
|---|---|---|
| **M0 Pipeline** | Repo, CI, signing, pre-releases | A green build installs on the owner's phone |
| **M1 v0.1** | SPEC-01 v0.1 scope | Spikes S-01…S-06 pass on the owner's phone |
| M2 v0.2 | Notifications (in-app), ask a question, report, per-app language, owner feedback | Owner uses it daily instead of the official app |
| M3 v0.3 | Widget, shortcuts, tablet layout, lint in CI | DESIGN-PHILOSOPHY §13 checklist passes on every screen |
| M4 Public | Only with Stips' written permission | Playbook §7 checklist, README in Hebrew and English |

## 2. Spikes

See SPEC-03 §11 (S-01…S-06). All need the owner's Stips account on the phone; the agent never handles the password.

## 3. Risks

| ID | Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|---|
| R-01 | Stips objects: their terms forbid copying/distributing content and trademarks without written permission, and automated copying | Medium | High | Private until permission (D-02); requests only on user action; no mirroring or export; "Open on Stips" everywhere; take down on request |
| R-02 | Stips changes or blocks the API | Medium | High | Tolerant parser, fixtures and probes (MAINTAINING.md). An honest User-Agent: if Stips blocks it, we stop and talk to them — no evasion |
| R-03 | Stips penalizes accounts that use a third-party client | Low | High | Ask in the permission request; the owner tests with their own account first |
| R-04 | The name "Stips+" uses Stips' trademark | Medium | Medium | Fine while private; decide the public name with Stips' answer (Q-02) |
| R-05 | Account calls behave differently from the website's code | High | Medium | Tolerant parsing, clear error messages, spikes on the device, fix in the next round |
| R-06 | Compile errors cost CI round trips (no local compiler) | Medium | Low | Versions from a CI-tested sample; signatures checked in source jars; `ci-status` branch for fast feedback |
| R-07 | The signing key is lost | Low | High | CI secret + a backup file on the owner's computer |
| R-08 | Users are mostly minors; their content is personal | Medium | Medium | Nothing stored beyond caches and the user's own saved list; hidden profile fields are never shown; no analytics |
| R-09 | Unsafe content or harassment with no report button | Medium | Medium | "Open on Stips" to report there in v0.1; an in-app report action in v0.2 (`reports.send`) |

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
> - ההתחברות היא עם חשבון סטיפס רגיל. הסיסמה נשלחת רק אליכם, והאפליקציה לא שומרת אותה.
> - אין בה פרסומות, אנליטיקה או מעקב, והיא לא מציגה מידע שלא מוצג באתר.
>
> כרגע האפליקציה פרטית ומשמשת אותי בלבד. לפני שאפרסם אותה (בקוד פתוח, ואולי ב-Google Play), אני מבקש את אישורכם בכתב, כפי שנדרש בתנאי השימוש. אשמח לשמוע אם יש לכם תנאים: שם אחר, הגבלת קצב פניות, ייחוס, שיתוף פרסומות או כל דבר אחר. אשמח גם להראות לכם אותה.
>
> תודה,
> אייל שמשי
> eyalshimshi1@gmail.com

*(English summary: an independent, free Android client; direct, user-initiated requests like a browser; honest identification; no ads, tracking, copying or crawling; private for now; asking for written permission and any conditions before publishing.)*

## 5. Open questions

| ID | Question | Blocks |
|---|---|---|
| Q-01 | Does Stips permit an independent client, and on what conditions? | Going public |
| Q-02 | Public name: keep "Stips+" or rename? | Going public |
| Q-03 | Google Play, or GitHub/Obtainium only? | Going public |
| Q-04 | Notifications: in-app only, or a periodic background check (costs battery and data)? | v0.2 |
