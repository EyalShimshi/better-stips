# SPEC-04: Data

*Where the content comes from, what it looks like, and how the app handles it. Everything in §2 was measured against the live service on 2026-11-02; re-check it when something breaks (§5).*

## 1. Source evaluation

| Source | What it gives | Access | Terms | Verdict |
|---|---|---|---|---|
| `https://stips.co.il/api` (the site's own JSON API) | Questions, answers, channels, topics, profiles, notifications, messages | HTTPS. Reads work without an account; writes need a signed-in session cookie | No public API, docs or license. Site terms apply (§6) | **Use.** The only source. It's the same API the website and the official app call |
| Official app `com.stipsapp` | A Cordova wrapper around the same website (`window.plugins.socialsharing` in the web bundle) | Google Play | — | Not a data source. A reference for features and pain points |
| `https://cdn.stips.co.il` | User-uploaded question photos and profile photos (JPEG) | Anonymous HTTPS | Stips and its users | **Use** for artwork and avatars |
| `https://images.unsplash.com` | Stock cover photos that askers pick for their question | Anonymous HTTPS | Unsplash License: free to use; credit the photographer | **Use**, with the photographer's credit on the question screen |
| HTML scraping, search engines | — | — | — | **Not needed.** The site is a single-page app; the API returns clean JSON |

## 2. API reference

### 2.1 Request format

- **One endpoint:** `https://stips.co.il/api`, GET with query parameters:

| Parameter | Value |
|---|---|
| `name` | The call: `objectlist`, `omniobj`, `homepage.content`, `smartdata.get`, `user.login`, … |
| `api_params` | A JSON object, serialized to a string |
| `rest_action` | For `omniobj` only: `GET` · `PUT` · `POST` · `DELETE` |
| `omniobj` | For `omniobj` only: `{"objType":"…","data":{…}}` as a string |

- **The plus-sign quirk:** the server decodes `+` as a space, so the web app replaces every `+` inside the JSON strings with `~MODU_PLUS_SIGN~`. The app must do the same. *Verified:* topic `C++` returns C++ questions only when encoded this way.
- **Writes** (the web app's rule): POST the same fields as a form body when `rest_action` is `PUT`/`POST`, or when the JSON is longer than 200 characters.
- **Session:** a classic ASP cookie (`ASPSESSIONID…`). The web app restores a signed-in session at start-up by calling `user.login` with `auth_token` (kept in the browser's localStorage under a key ending in `UserService.appUser_a_token`).
- **No special headers** are needed. Responses are gzip-compressed JSON, labeled `text/html; Charset=UTF-8`, behind Cloudflare (`cf-cache-status: DYNAMIC`, not cached).

### 2.2 Response envelope

```json
{ "status": "ok", "error_code": "", "error_msg": "", "data": …, "exec_time": "297ms", "sqls": [ … ] }
```

| `error_code` | Meaning |
|---|---|
| `API_ERROR_GENERAL` | Generic failure; `error_msg` is Hebrew text (for example, missing permission) |
| `API_ERROR_EOF` | The item doesn't exist (deleted question) |
| `API_ERROR_BANNED` | The signed-in user is banned |

### 2.3 Calls

**Read calls (no account needed), all verified:**

| Purpose | `name` | `api_params` / `omniobj` | Returns |
|---|---|---|---|
| Newest questions | `objectlist` | `{"method":"ask.new","page":1}` | `data`: array of 30 questions. An empty array means the end |
| Hot questions | `objectlist` | `{"method":"ask.hot","page":1,"filter_section_name":"now"}` (`now` · `today` · `week`) | Same |
| A channel | `objectlist` | `{"method":"ask.new","channel":"לימודים","page":1}`; hot adds `"filter_section_name":"week"` (`week` · `month`) | Same, all from that channel. **The channel is passed by its Hebrew name**, not its id |
| A topic (tag) | `objectlist` | `{"method":"ask.new","topic":"מתמטיקה","page":1}`; hot as for channels | Same |
| Pinned questions | `homepage.content` | — | `data.pinnedItems`: 5 questions |
| One question | `omniobj` | `rest_action=GET`, `omniobj={"objType":"ask","data":{"id":19745077}}` | `data.omniOmniObj`: one question |
| Its answers | `objectlist` | `{"method":"ans.for_item","askid":19745077}` | `data`: all answers at once (no paging) |
| Channels | `smartdata.get` | `{"namespace":"item","unit":"RemoteConfig"}` | `data.results.itemChannels.channels` |

**Not available:** a free-text search over questions. The web app's `search_query` parameter is ignored by `ask.new`/`ask.hot` (it's used only in an admin screen). Topics (tags) are the closest thing.

**Write and account calls (seen in the web bundle, not yet called; verify in the sign-in round):**

| Purpose | `name` | Parameters seen |
|---|---|---|
| Sign in / restore session | `user.login` | `email`, `password`, `auth_token`, `auth_cookie_password` |
| Who am I | `user.get_app_user` | — (fails with `API_ERROR_GENERAL` when signed out) |
| Vote on an answer | `item.vote` | `objType`, `id`, `vote` (1, 0, −1) |
| Give a flower (best answer) | `item.send_flower` | — |
| Post an answer | `omniobj` (create) | `objType:"ans"`, `data:{askid, a, name, anonflg, link1, link1name}` |
| Ask a question | `omniobj` (create) | `objType:"ask"`, `data:{q, text_content, tagslist, photo, anonflg, name, ask_type:11, channel_id}` |
| My questions / answers / pins | `objectlist` | `{"method":"ask.user_activity","for_current_user":true,"filter_section_name":"asked"}` (`asked` · `ansed` · `pinned`) |
| Notifications | `notifications.list` | — |
| Private messages | `messages.list`, `messages.from_user`, `messages.send`, `messages.count` | `page`, `userid`, `msgid` |
| Profile | `profile.page_data` | — |
| Report | `reports.send` | — |

**Account calls used since v0.2** (read in the web bundle and watched on the site with the owner's account, D-22; write calls were watched, not sent, unless noted):

| Purpose | `name` | Parameters | Returns / notes |
|---|---|---|---|
| A user | `omniobj` GET | `{"objType":"user","data":{"id":…}}` | `data.points` (credibility 0–100), `data.gender` (1 male, 2 female), `extra.photostamp`, `extra.item_profile.online` |
| Profile page | `profile.page_data` | `{"userid":…}` | `questions`, `answers`, `flowers`, `age`, `hebrew_active_since` ("6 שנים ו-9 חודשים"), `badges[{name}]`, `user_profile_page.data.text_status` (bio), `user_profile_page.meta.permissions.edit`; older profiles: `profile_quote` |
| Activity graph | `profile.actions_chart` | `{"userid":…,"action":"ans"}` (`ans` · `ask` · `flowers`) | `chartData[{time:"2026/10/25 00:00:00", actionsCount}]`: 26 weekly points, newest first |
| Answers that got a flower | `objectlist` | `{"method":"ans.flower_for_user","userid":…,"page":1}` | Answers with `extra.parent_item_title` |
| Thanks wall | `objectlist` | `{"objType":"thanksmsg","userid":…,"page":1}` | 30 per page: `data{id, fromuserid, touserid, msg, time}`, `extra.item_profile` (the writer), `meta.permissions.deleteWithoutMsg` (the wall's owner) |
| Delete a thanks message | `omniobj` DELETE | `{"objType":"thanksmsg","data":{"id":…}}` | — |
| Notifications | `notifications.list` | `{"page":1}` | `[{id, contentHtml, msgText, linkPageName ("ask"·"profile"), linkItemId, viewed, time, type}]`; types are words like `new_ask_ans_registered` |
| Unread counts | `messages.count` | `{}` | `{messagesCount, notificationsCount}` |
| Pin / unpin | `item.add_pin` | `{"id":…,"minus":false}` (`true` unpins) | The question's `data.user_pinned` is 1 when pinned (−1 when never pinned); pinned list: `ask.user_activity` with `pinned`. *Sent once by mistake during research and undone at once* |
| Is following | `smartdata.get` | `{"namespace":"ask","unit":"AskAdditionalData","id":…}` | `results.notificationsRegistered` |
| Follow / unfollow | `item.notifications_register` | `{"objType":"ask","id":…,"add":true}` | The site does this after an answer when its "קבלת עדכונים" toggle is on (the default) |
| Edit an answer | `omniobj` POST (`rest_action`) | `{"objType":"ans","data":{"id", "a", "link1", "link1name"}}` | Only while the answer's `meta.permissions.edit` is true (it expires) |
| Detach an answer from your account | `item.set_anonymous` | `{"objType":"ans","id":…}` | Offered when `meta.permissions.itemOwner`; the site confirms first |
| A link's title | `smartdata.get` | `{"namespace":"utils","unit":"GetUrlMeta","url":…}` | `results.title` (may be empty); names the one link an answer can carry (`link1`, `link1name`) |
| Safe mode | `user.update.settings` | `{"userid":…,"settings_name":"safeFilter","value":true}` | Stored on the account |
| Bio | `omniobj` POST | `{"objType":"user_profile_page","data":{"userid","text_status"}}` with `api_params {"createIfNotExist":true}` | — |
| Profile photo | multipart POST to `https://stips.co.il/scripts/upload/photo.asp?photo_type=profile_photo` (field `file`), then `profile.set_thumbnail` | `{"userid","x0","x1","y0","y1"}` (the crop square) | Upload answers `{success, error}`. The site adds `&flip=yes` / `&rotateR=yes` for camera photos; the app straightens the photo itself |

Credibility bands on the site's gauge ("דירוג אמינות"): 0 red `#f50057`, 30 yellow `#FDD835`, 50 indigo `#3f51b5`, 80 green `#09c286`. Badge codes: `moderator` (נאמן/ת סטיפס), `senior_moderator` (נאמן/ה בכיר/ה), `old_expert` (מומחה/ית סטיפס), `senior_advisor` (עונה בכיר/ה), `custom_rank`.


**Calls used since v0.3** (from the site's code, checked live where public; write calls not sent during research):

| Purpose | `name` | Parameters | Returns / notes |
|---|---|---|---|
| Report | `reports.send` | `{"itemid","itemtype":"ask"/"ans"/"penfriendsitem","reason","reason_id":0}` | The user's words, 5–500 characters. Users are reported from chats with `itemtype:"usermsgs"`, `itemid` = their id and `reason_id` 2, 4, 8, 16 or 1024 (not in the app yet) |
| Block content | `smartdata.get` | `{"namespace":"ask","unit":"BlockContent","id","objType","blockUser"?:true}` | Answer ignored. Hiding is done by the client; the site's app keeps `bc-item-{type}-{id}` / `bc-user-{id}` flags locally, and "remove all" just clears them |
| Block messages | `messages.block_user` | `{"userid","action":"add"/"remove"}` | `messages.from_user` returns `userBlocked` |
| Delete your item | `omniobj` DELETE | `{"objType","data":{"id"}}` | When `meta.permissions.delete` or `deleteWithoutMsg` |
| Edit your question | `omniobj` POST | `{"objType":"ask","data":{"id","q","text_content","tagslist":[…],"q_link"}}` | When `meta.permissions.edit` |
| Detach your question | `item.set_anonymous` | `{"objType":"ask","id"}` | As for answers |
| Send a flower | `item.send_flower` | `{"id": answerId}` | `{flowerSent: true}`. Only the asker, to someone else's answer up to 14 days old, 5 per question; not to the asker's own reply (`extra.parentItemCreatorResponse`) |
| Write on a thanks wall | `omniobj` PUT | `{"objType":"thanksmsg","data":{"msg","touserid"}}` | — |
| Find people | `smartdata.get` | `{"namespace":"users","unit":"Search","q"}` | `results.profiles[{userid,nickname,photostamp,online,anonflg}]` (verified, public) |
| Daily question limit | `smartdata.get` | `{"namespace":"ask","unit":"DailyAskCount"}` | `results{allowed,maxPerDay}` |
| Tag suggestions | `item.autotags` / `item.tag_suggest` | `{"title"}` / `{"q"}` | `data.tags[]` (verified, public) |
| Cover photos | `photos.tag_gallery` | `{"tags":["…"]}` | `data.photos[{photo_id,original_url}]`; the site falls back to the tag "רקעי פריטים" |
| Ask | `omniobj` PUT | `{"objType":"ask","data":{"q","q_link","text_content","tagslist":[…],"photo","anonflg"?,"name"?,"ask_type":11,"channel_id"?}}` | `data.newid`. Title 10–120, details ≤2000, 1–5 tags, nickname 2–18; a photo unless the link is YouTube |
| Pen pals: list | `objectlist` | `{"method":"penfriendsitem.new","page"}` | 30 per page, newest first: `data{id,msg,time,active}`, `extra.item_profile` (verified, public) |
| Pen pals: may I post? | `smartdata.get` | `{"namespace":"penfriends","unit":"PostAllowed"}` | `results{allowed,postInterval,nextTimeInMinutes}`. Posting also needs 5 flowers (`user.get_app_user` → `flowersCount`) |
| Pen pals: my last post | `smartdata.get` | `{"namespace":"penfriends","unit":"LastMessageForUser"}` | `results.messageText` |
| Pen pals: post | `omniobj` PUT | `{"objType":"penfriendsitem","data":{"msg"}}` | Up to 255 characters; ages 13–21 (the site's rule) |

**Sessions and errors (measured in v0.3):**
- Account calls made without a live session return `status: "ok"` with **empty data** (`messages.count` → 0/0, `notifications.list` → `[]`, `AskAdditionalData` → false). Only `user.get_app_user` fails, with "המשתמש אינו מחובר למערכת".
- `user.login` with a token Stips no longer accepts returns `{"status":"ok","data":{"logged":false}}`.
- Stips is served through **Cloudflare** (`Server: cloudflare`, `CF-RAY`) from one ASP.NET origin (one `ASPSESSIONID…` cookie name across requests). A Cloudflare limit shows as HTTP 429 or 403.

**Request limit:** an address that sends about 100 calls without the site's Referer is refused with `IP_RATE_LIMIT`; calls with it (every call from the website and the official app) aren't limited that way (measured in v0.4–0.5, §2.10).

**Image sizes (measured):** question photos `s` (75×75), `w400`, `z` (about 500 px, the largest); Unsplash covers accept any `w=` (1080 used for full view); profile photos `t` (50×50), `a` (200×200, square crop), `l` (the whole upload, about 300×530).
### 2.4 Question (`objType: "ask"`)

| Field | Example | Use |
|---|---|---|
| `data.id` | `19745126` | Identity; links `https://stips.co.il/ask/<id>` |
| `data.q` | "איך התליון שלי?…" | Title (the question itself) |
| `data.text_content` | "https://www.reddit.com/…" | Body, plain text with `\n`; may contain URLs |
| `data.tagslist` | `["כסף","מחירים"]` | Topics |
| `data.time` | `2026/11/02 16:59:56` | Posted time, **Israel local time, no zone** |
| `data.anonflg` | `true` | Asked anonymously (no profile link) |
| `data.photo` | `4791385491` or `unsplash:StifWyrXbdc` | Cover image (§2.6) |
| `data.answer_count`, `data.pin_count` | `13`, `0` | Counts |
| `data.q_link` | `https://example.com/…` | A link the asker attached (shown under the title) |
| `data.user_pinned` | `1`, `0`, `-1` | 1 = pinned by the signed-in user |
| `data.channel_id` | `4` | `0` = no channel |
| `extra.hebrew_time` | "33 שניות" | The server's relative time (Hebrew only; the app formats its own) |
| `extra.url` | `ask/19745126/<slug>` | Share link path |
| `extra.content_image` | `url_small`, `url_medium`, `creator_name`, `creator_username` | Unsplash cover and credit |
| `extra.item_profile` | `nickname`, `userid`, `photostamp`, `online`, `anonflg` | The asker |
| `meta.permissions` | `edit`, `delete`, `report`, … | What the current user may do |

### 2.5 Answer (`objType: "ans"`)

| Field | Use |
|---|---|
| `data.id`, `data.askid` | Identity, parent question |
| `data.a` | Text, plain with `\n` |
| `data.name`, `data.anonflg` | Display name; anonymous answers carry a chosen nickname (for example "אנונימית") |
| `data.time` | Israel local time |
| `data.votes_score`, `data.votes_count` | Score |
| `data.link1…3`, `data.link1name…3name` | Optional links |
| `extra.userVote` | The current user's vote |
| `extra.flowered` | Picked as a best answer (a "flower") |
| `extra.item_profile` | The answerer |
| `extra.parent_item_title` | The question's title (in lists of a user's answers) |
| `meta.permissions.edit`, `meta.permissions.itemOwner` | The signed-in user may edit it (time-limited) / wrote it |

### 2.6 Images

| Image | URL | Measured size |
|---|---|---|
| Question photo, thumbnail | `https://cdn.stips.co.il/photos/s/<photo>.jpg` | 3.6 KB |
| Question photo, 400 px wide | `https://cdn.stips.co.il/photos/w400/<photo>.jpg` | 35 KB |
| Question photo, large | `https://cdn.stips.co.il/photos/z/<photo>.jpg` | — |
| Unsplash cover | `extra.content_image.url_small` (400 px) / `url_medium` (680 px) | ~30–60 KB |
| Profile photo, small / large | `https://cdn.stips.co.il/photos/user_profile/t|l/<userid>/<photostamp>.jpg` | 4 KB / 115 KB |
| Channel icons | SVG on the site | **Not used:** Android can't decode SVG; the app uses its own icons |

A numeric `photo` is a Stips upload; `unsplash:<id>` means the cover is in `content_image`. A question with neither gets a generated monogram (DESIGN-PHILOSOPHY §2.8).

### 2.7 Channels (snapshot 2026-11-02)

| id | Name | Meaning | Site icon | Gradient (site) |
|---|---|---|---|---|
| 1 | מוסיקה | Music | `music` | `#ff5900 → #ffec27` |
| 2 | אינטרנט וטכנולוגיה | Internet and technology | `technology` | `#1697b1 → #00074e` |
| 4 | לימודים | Studies | `dna` | `#00b165 → #26c1f8` |
| 7 | סדרות וסרטים | Series and movies | `movies` | `#af00c9 → #a30606` |
| 8 | בעלי חיים | Animals | `animals` | `#7bd5fd → #3b58e3` |
| 9 | פילוסופיה | Philosophy | `philosophy` | `#800080 → #ea4fc9` |

The snapshot, with every channel's full gradient, ships as `app/src/main/assets/channels.json` (§4). Each channel's first strong color seeds its tinted scheme (DESIGN-PHILOSOPHY §2.2).

### 2.8 Measured costs and speed

| Request | Transfer (gzip) | Raw | Time |
|---|---|---|---|
| Feed page (30 questions) | 9.5 KB | 45 KB | 0.4–0.55 s |
| Answers (13) | 1.6 KB | 9.4 KB | 0.2 s |
| Channels config | < 1 KB | 1.4 KB | 0.1 s |

Browsing 100 questions with thumbnails costs roughly 0.5–1 MB; opening questions with large covers adds about 50 KB each.

Background costs (all opt-in or only while the app is open): **Live** reloads the first New page every 30 s by default (5 s at the fastest, ≈6.7 MB an hour) while Home is on screen, ≈120 × 9.5 KB ≈ 1.1 MB an hour. **Unread counts** cost under 1 KB a minute while the app is open. **Phone notifications** cost ≈96 small checks a day, plus a list (a few KB) only when something is new: well under 1 MB a day.

### 2.10 Request limit (`IP_RATE_LIMIT`, #5)

- **What it looks like:** HTTP 200 with `{"status":"failed","error_code":"IP_RATE_LIMIT","msg":"ip <address> reached rate limit"}` (130 bytes), from Stips' server (`cf-cache-status: DYNAMIC`, no `cf-mitigated`), not from Cloudflare. Once it starts, every call from that address without the site's Referer gets it, whatever the method. (The app reads `error_msg`, not `msg`, so the address never reaches its network log.)
- **Who it applies to: calls without the site's Referer.** Measured in v0.5 with test clients on GitHub's runners, one address each, all asking for the first New page once a second, anonymously ([tools/probe_rate_limit.py](tools/probe_rate_limit.py)):

  | Client | Referer | Result |
  |---|---|---|
  | Stips+'s headers | none | refused at call #102, after 145–150 s (twice) |
  | Stips+'s headers + `Sec-Fetch-Site/Mode/Dest` (`same-origin`/`cors`/`empty`) | none | refused at #102, after 150 s |
  | Stips+'s headers at v0.4's budget (30, then one per 5 s; at most 90 in any 5 minutes) | none | refused at #102, after 6 minutes |
  | Stips+'s headers + `Referer: https://stips.co.il/` | yes | 150 calls in 211 s, never refused |
  | A browser's headers, copied by a script | yes | 200 in 285 s, never refused |
  | A real Chrome page on https://stips.co.il calling `fetch()` | yes (the page's URL) | 200 in 283 s, never refused |

- **So:** about 100 calls without the Referer, at any pace from 150 s to 6 minutes (the window is 6 minutes or longer), then every call without it is refused, for over 20 minutes (one try a minute). Calls with the Referer from the same address still pass during that refusal (tested right after it). The User-Agent doesn't matter; the Referer does.
- **Stips+ 0.3 (IPv6, HTTP/1.1, no Referer):** 103 calls answered, the 104th refused, 3 m 38 s after start (51 `objectlist`, 13 `omniobj`, 10 `smartdata.get`, 9 `profile.actions_chart`, 7 `messages.count`, 5 `profile.page_data` and a few others). Stips+ 0.2 (IPv4, #3) was cut off the same way within 3 m 21 s, and only a new address (a VPN, airplane mode) brought it back.
- **The official app** is the site in a WebView: same `https://stips.co.il/api`, same parameters (GET, or a form POST over 200 characters), so every call carries the site's Referer. PCAPdroid recordings while refreshing frantically, one HTTP/2 connection each: 102 calls in 2 m 17 s (11:08), and 185–245 in 4 m 34 s (13:00; 59–76 in its busiest minute, 145–191 in three). None were refused. v0.4 read the first recording as one limit for both apps; the second recording and the test clients disproved that.
- **Stips+ 0.5 sends the site's Referer** (D-49) with its honest User-Agent, and paces itself near the official app (40 at once, then 30 a minute: at most 190 in any 5 minutes, D-46).
- **The site's profile graph** loads only the selected action (`ask`, `ans` or `flowers`) and reloads when the selection changes.
- **Open:** whether calls with the Referer have a limit of their own (none seen up to 245 in 4½ minutes), the exact window for calls without it, and the refusal's full length. The probe workflow re-measures all three (MAINTAINING.md).

## 3. Data model (in the app)

```kotlin
data class Question(
    val id: Long, val title: String, val body: String, val topics: List<String>,
    val postedAt: Instant, val answerCount: Int, val pinCount: Int, val channelId: Int,
    val author: Author, val cover: Cover?, val sharePath: String,
)
data class Answer(
    val id: Long, val questionId: Long, val text: String, val postedAt: Instant,
    val score: Int, val flowered: Boolean, val author: Author, val links: List<Link>,
)
data class Author(val nickname: String, val userId: Long?, val photoStamp: String?, val anonymous: Boolean, val online: Boolean)
sealed interface Cover { data class Stips(val photoId: String) : Cover; data class Unsplash(val small: String, val medium: String, val credit: String) : Cover }
data class Channel(val id: Int, val name: String, val icon: String, val colors: List<Color>)
enum class Feed { New, Hot }   // with HotWindow: Now, Today, Week, Month
```

- **The parser is tolerant:** a missing or mistyped field gets a safe default instead of failing the whole list. Ids may arrive as numbers or strings.
- **Times** are parsed as `Asia/Jerusalem` local time and shown relative in the user's language ("5 min", "לפני 5 דק׳").

## 4. Pipeline

```mermaid
flowchart LR
  API[stips.co.il/api] -->|OkHttp, gzip| P[Tolerant JSON parser]
  P --> R[Repository]
  R --> M[(Memory cache)]
  R --> D[(Last-seen file cache)]
  A[assets/channels.json] --> R
  CFG[smartdata RemoteConfig] -->|refresh| R
  R --> UI[Compose UI]
```

- **Bundled snapshot:** `channels.json` makes the Channels screen instant and offline-ready. Unit tests validate it (ids unique, names non-empty, colors parse).
- **Cache what you've shown:** the last feed page of each list is saved to the app's private files, so the next launch shows it at once and refreshes quietly.
- **Updatable without a release:** at start-up the app refreshes channels from `smartdata.get` and keeps the snapshot as a fallback.

## 5. Automation and maintenance

- No server of our own, no background polling in v0.x.
- **When the site changes** (renamed calls, new fields): the parser tests (with recorded fixtures in `app/src/test/resources/`) show what still parses. Re-run the probes in MAINTAINING.md to compare.
- **Fixtures** are trimmed real responses with personal data replaced (nicknames → "משתמש/ת", user ids → 1000+).

## 6. Governance and legal

**Stips' terms of use** ([stips.co.il/about/terms](https://stips.co.il/about/terms), read 2026-11-02; the page renders only in a browser):

| Clause | Text (Hebrew) | Meaning for us |
|---|---|---|
| Operator | "האתר מנוהל ומופעל על-ידי נדב דפני - אינטרנט ותוכנה בע\"מ" | Who to ask (Google Play lists the developer as Futurebit Media Ltd., `contact@stips.co.il`) |
| Intellectual property | "אין להעתיק, לשכפל, להפיץ, למכור, לשווק ולתרגם מידע כלשהו לרבות סימני מסחר, תמונות, צילומים, טקסטים וקוד מחשב מן האתר בלא קבלת רשותה המפורשת של הנהלת האתר מראש ובכתב." | Copying or distributing content or trademarks needs **prior written permission** → private until Stips agrees (D-02) |
| Commercial use | "אין לעשות כל שימוש מסחרי במידע, בתכנים ובנתונים המתפרסמים באתר… בלא קבלת הסכמתה של הנהלת האתר מראש ובכתב." | The app is free, with no ads or payments |
| Automated access | "חל איסור חמור להפעיל כל יישום מחשב… לרבות תוכנות מסוג Crawlers, Robots וכדומה, שנועדו לשם חיפוש סריקה, העתקה או אחזור באופן אוטומטי של מידע ותכנים מהאתר לשם יצירת העתק, מלא או חלקי…" | No crawling, mirroring, bulk prefetch or export. The app fetches only what the user opens, like a browser (D-13) |
| User content | Users grant Stips a free, worldwide, perpetual license to what they post | Answers posted through the app are ordinary Stips posts |
| Law | Israeli law; courts in Jerusalem and Tel Aviv | — |

**Other sources:**
- `robots.txt` allows everything except one profile page.
- Unsplash photos: free under the Unsplash License; the app shows the photographer's name (from `content_image.creator_name`).
- Prior art: open-source wrappers of the same API exist (for example `stipspy`, Chrome extensions such as StipsX under Apache-2.0); none is endorsed by Stips. We don't reuse their code.

**Our rules:**
- No Stips logo or artwork in the repo; the icon is our own.
- Show only what the site shows. Profile fields the website hides (some tools expose birth dates and gender) are never displayed.
- Disclaimer in About and on the sign-in screen: "An independent app, not affiliated with Stips."
- If Stips asks us to stop or blocks the app, we stop; we don't work around it.
