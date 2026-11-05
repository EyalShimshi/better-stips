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
| M3.7 v0.11 | Phone notifications that arrive: checks every few minutes while the phone is awake, per-chat notifications that pop up, a status panel and a one-time offer | The owner gets new answers and messages on the phone within minutes |
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
| R-12 | Phone notifications come late or not at all: Stips has no push for other apps, and Android (Samsung's especially) holds background work back to save battery | Medium | Medium | Checks every few minutes while the phone is awake; Android's "run in the background?" asked when turning them on; a status panel that names what's holding them back, with *Check now* and a test (D-72–D-74). Can't be tested on CI's signed-out emulator: the owner's phone is the test |

## 4. The permission request

**Q-01:** Will Stips allow an independent client? The owner sends this to `contact@stips.co.il` (the address in Stips' Google Play listing). The terms name the operator as "נדב דפני - אינטרנט ותוכנה בע\"מ"; the Play listing names Futurebit Media Ltd.

Rewritten before the launch (v0.10): the highlights so far, and a link to the open code. It still discloses the Referer (R-02).

> **נושא:** Stips+ – אפליקציית אנדרואיד לסטיפס, רגע לפני ההשקה
>
> שלום לצוות סטיפס,
>
> שמי אייל שמשי, ואני משתמש פעיל בסטיפס. מתוך אהבה לאתר ולקהילה בניתי את Stips+, אפליקציית אנדרואיד עצמאית וחינמית שמביאה את סטיפס לטלפון בממשק טבעי, מהיר ונוח. האפליקציה כבר בליטושים האחרונים לקראת השקה, ואני ממש מתרגש לשתף אתכם!
>
> כמה דברים שאני גאה בהם במיוחד:
> - **דף בית חי:** השאלות הכי חמות בכרטיסים גדולים, ומתחתן השאלות החדשות, עם מצב Live שבו הן קופצות לפיד בזמן אמת בלי לרענן.
> - **תשובה בהחלקה:** החלקה על תשובה מוסיפה לבד את ה-^^ לפי המוסכמה של סטיפס, ואם בינתיים נכנסו עוד תשובות, הספירה מתוקנת אוטומטית.
> - **ציטוטים כמו בוואטסאפ:** תשובה שמפנה ב-^^ לתשובה קודמת מציגה ציטוט שלה, ולחיצה עליו קופצת אליה.
> - **התראות שפותחות בדיוק במקום:** התראה על תשובות חדשות פותחת ישר בתשובה החדשה הראשונה, והתראה על פרח פותחת בתשובה שקיבלה אותו.
> - **פרופילים בסגנון רשת חברתית:** נקודות אמינות, פרחים, גרפים של פעילות לאורך זמן וקיר התודות, ותג "נאמן סטיפס" ליד התשובות של הנאמנים.
> - **ווידג'טים למסך הבית** לשאלות החמות ולשיחות שלא נקראו, מצב כהה ועברית מלאה מימין לשמאל.
>
> חשוב לי שהאפליקציה תהיה טובה גם לסטיפס, לא רק למשתמשים, ולכן:
> - **התוכן נשאר אצלכם.** האפליקציה פונה ישירות מהטלפון של המשתמש לסטיפס, כמו דפדפן, ורק בעקבות מה שהמשתמש עושה או מפעיל בה. אין שרת ביניים, ואין איסוף, העתקה או סריקה של תוכן.
> - **שקיפות מלאה.** האפליקציה מזדהה בגלוי בכל בקשה (StipsPlus, unofficial, עם קישור לפרויקט), בכל שאלה יש קישור "פתיחה בסטיפס", והקוד פתוח לעיון: https://github.com/EyalShimshi/better-stips
> - **בקצב של האפליקציה הרשמית.** כדי ששימוש רגיל לא ייחסם במגבלת הקצב שלכם (IP_RATE_LIMIT), כל בקשה נשלחת עם ה-Referer של האתר (https://stips.co.il/), כמו הבקשות של האתר ושל האפליקציה הרשמית, והקצב מוגבל בערך לזה של האפליקציה הרשמית. אם תעדיפו שאעשה את זה אחרת, אשנה.
> - **פרטיות ובלי פרסומות.** ההתחברות היא עם חשבון סטיפס רגיל: הסיסמה נשלחת רק אליכם, והאפליקציה לא שומרת אותה. אין בה פרסומות, אנליטיקה או מעקב.
>
> עד עכשיו לא הפצתי את האפליקציה לציבור, ואני משתמש בה בעצמי. לפני שאשיק אותה רשמית, ואולי גם אעלה אותה ל-Google Play, אשמח לקבל את אישורכם בכתב, כפי שנדרש בתנאי השימוש. אם יש לכם בקשות או תנאים, כמו שם אחר, קצב פניות, ייחוס או כל דבר אחר, אשמח להתאים. אשמח גם לשלוח לכם גרסה לנסות, או להראות לכם אותה.
>
> המטרה שלי פשוטה: שיהיה עוד יותר כיף להשתמש בסטיפס.
>
> תודה רבה, ומחכה לשמוע מכם!
> אייל שמשי
> eyalshimshi1@gmail.com

*(English summary: an active user built a free, independent Android client out of love for the site, now in final polish before launch. Highlights: a live Home, swipe-to-reply with Stips' carets (corrected if answers land in between), WhatsApp-style reply quotes, notifications that open at the right answer, social profiles with the thanks wall and moderator badges, widgets and dark mode. Good faith: direct requests like a browser, only from what the user does or turns on; no middle server, collecting, copying or crawling; an honest User-Agent, "Open on Stips" and open code; the site's Referer on every request at about the official app's pace, changed if Stips prefers; the password goes only to Stips; no ads or tracking. Not distributed yet; asks for written permission and any conditions before launch, and offers a test build or a demo.)*

## 5. Open questions

| ID | Question | Blocks |
|---|---|---|
| Q-01 | Does Stips permit an independent client, and on what conditions? | Going public |
| Q-02 | Public name: keep "Stips+" or rename? | Going public |
| Q-03 | Google Play, or GitHub/Obtainium only? | Going public |
| Q-04 | ~~Notifications: in-app only, or a periodic background check?~~ **Both:** the in-app list always; the background check is opt-in (SPEC D-26) | Answered in v0.2 |
