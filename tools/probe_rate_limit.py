"""
Re-measures how Stips' IP_RATE_LIMIT treats Stips+ (issue #5, SPEC-04 §2.10). Run it with the "Probe Stips'
rate limit" workflow (Actions > Run workflow): each variant gets its own runner, so its own address. Anonymous
requests only (no account, no credentials): the first page of New questions, the same request a refresh makes.

  app    Stips+ as shipped: its User-Agent and the site's Referer, at the request budget's fastest pace
         (40 at once, then one per 2 s) for 5 minutes. It should never be refused.
  limit  Stips+'s User-Agent without the Referer, one request a second until refused (Stips' limit for clients
         outside the site), then one try a minute to time how long the refusal lasts. Triggers the limit on purpose.
  site   A real Chrome page on https://stips.co.il calling fetch() as the site does (what the official app gets).
"""
import gzip
import http.client
import json
import sys
import time
import urllib.parse
import urllib.request
import zlib

HOST = "stips.co.il"
PARAMS = json.dumps({"method": "ask.new", "page": 1, "safe_filter": True}, separators=(",", ":"))
PATH = "/api?" + urllib.parse.urlencode({"name": "objectlist", "api_params": PARAMS})

STIPSPLUS_UA = "StipsPlus/0.5 (Android 17; unofficial; +https://github.com/EyalShimshi/better-stips)"
WEBVIEW_UA = (
    "Mozilla/5.0 (Linux; Android 14; SM-S948B Build/UP1A.231005.007; wv) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Version/4.0 Chrome/130.0.6723.102 Mobile Safari/537.36"
)
STIPSPLUS = {"User-Agent": STIPSPLUS_UA, "Accept-Encoding": "gzip"}
REFERER = {"Referer": "https://stips.co.il/"}

# name: (client, headers, pace, cap); "budget" mirrors RateGuard (CAPACITY 40, REFILL_MS 2000) for 5 minutes.
VARIANTS = {
    "app": ("direct", {**STIPSPLUS, **REFERER}, "budget", 0),
    "limit": ("direct", STIPSPLUS, "fast", 150),
    "site": ("browser", None, "fast", 200),
}
CAPACITY, REFILL_S, BUDGET_RUN_S = 40.0, 2.0, 300

START = time.time()


def log(msg):
    print(f"{time.strftime('%H:%M:%S')} +{time.time() - START:6.1f}s  {msg}", flush=True)


class Direct:
    """One keep-alive HTTPS connection, cookies kept like OkHttp's jar."""

    def __init__(self, headers):
        self.headers = headers
        self.cookies = {}
        self.conn = None

    def get(self, path):
        for attempt in range(2):
            try:
                if self.conn is None:
                    self.conn = http.client.HTTPSConnection(HOST, timeout=25)
                headers = dict(self.headers)
                if self.cookies:
                    headers["Cookie"] = "; ".join(f"{k}={v}" for k, v in self.cookies.items())
                self.conn.request("GET", path, headers=headers)
                r = self.conn.getresponse()
                body = r.read()
                for k, v in r.getheaders():
                    if k.lower() == "set-cookie":
                        pair = v.split(";", 1)[0]
                        if "=" in pair:
                            name, value = pair.split("=", 1)
                            self.cookies[name.strip()] = value.strip()
                encoding = (r.getheader("Content-Encoding") or "").lower()
                if encoding == "gzip":
                    body = gzip.decompress(body)
                elif encoding == "deflate":
                    body = zlib.decompress(body)
                return r.status, body.decode("utf-8", "replace"), r.getheader("cf-ray")
            except (http.client.HTTPException, OSError) as e:
                self.conn = None
                if attempt:
                    return 0, f"{type(e).__name__}: {e}", None
        return 0, "unreachable", None


class Browser:
    """A real Chrome page whose origin is https://stips.co.il (served locally, nothing loaded from Stips)."""

    def __init__(self):
        from playwright.sync_api import sync_playwright

        self.pw = sync_playwright().start()
        try:
            self.browser = self.pw.chromium.launch(channel="chrome", headless=True)
        except Exception as e:
            log(f"system Chrome unavailable ({type(e).__name__}); using Playwright's Chromium")
            self.browser = self.pw.chromium.launch(headless=True)
        ctx = self.browser.new_context(user_agent=WEBVIEW_UA, locale="he-IL")
        self.page = ctx.new_page()
        page_url = "https://stips.co.il/probe.html"
        self.page.route(page_url, lambda route: route.fulfill(
            status=200, content_type="text/html; charset=utf-8",
            body="<!doctype html><meta charset=utf-8><title>probe</title>"))
        self.page.goto(page_url)

    def get(self, path):
        try:
            status, text, ray = self.page.evaluate(
                """async (p) => {
                    const r = await fetch(p, {credentials: 'include'});
                    return [r.status, await r.text(), r.headers.get('cf-ray')];
                }""",
                path,
            )
            return status, text, ray
        except Exception as e:
            return 0, f"{type(e).__name__}: {str(e)[:200]}", None


def outcome(status, text):
    if status == 0:
        return "network: " + text[:120]
    if status != 200:
        return f"HTTP {status}"
    try:
        j = json.loads(text)
    except ValueError:
        return "not JSON: " + text[:80].replace("\n", " ")
    code = str(j.get("error_code") or "")
    if "RATE" in code.upper() or "LIMIT" in code.upper():
        # The message names the runner's address, not a user's.
        return "LIMITED " + code + " " + text[:160]
    if j.get("status") == "ok":
        return "ok"
    return "error " + code + " " + text[:120]


def max_in(times, window):
    best = j = 0
    for i in range(len(times)):
        while times[i] - times[j] > window:
            j += 1
        best = max(best, i - j + 1)
    return best


def main(name):
    kind, headers, pace, cap = VARIANTS[name]
    try:
        trace = urllib.request.urlopen("https://stips.co.il/cdn-cgi/trace", timeout=15).read().decode()
        info = dict(line.split("=", 1) for line in trace.strip().splitlines() if "=" in line)
        log(f"variant {name}: address {info.get('ip')} ({info.get('loc')}), Cloudflare {info.get('colo')}")
    except Exception as e:
        log(f"variant {name}: trace failed: {e}")
    client = Direct(headers) if kind == "direct" else Browser()

    sent = []
    first = None
    tokens, refilled = CAPACITY, time.time()
    t0 = time.time()
    while True:
        now = time.time()
        if pace == "fast":
            if len(sent) >= cap:
                break
        else:
            if now - t0 > BUDGET_RUN_S:
                break
            tokens = min(CAPACITY, tokens + (now - refilled) / REFILL_S)
            refilled = now
            if tokens < 1:
                time.sleep((1 - tokens) * REFILL_S)
                continue
            tokens -= 1
        if sent and time.time() - sent[-1] < 1.0:
            time.sleep(1.0 - (time.time() - sent[-1]))
        status, text, ray = client.get(PATH)
        sent.append(time.time())
        result = outcome(status, text)
        n = len(sent)
        if result != "ok":
            log(f"#{n}: {result} (ray {ray})")
            if result.startswith("LIMITED") or result.startswith("HTTP 4"):
                first = (n, time.time() - t0, result)
                break
        elif n % 25 == 0:
            log(f"#{n}: ok ({len(text)} chars, ray {ray})")

    windows = ", ".join(f"{w}s:{max_in(sent, w)}" for w in (60, 120, 180, 300, 600))
    if first:
        log(f"RESULT {name}: REFUSED at request #{first[0]} after {first[1]:.0f}s; busiest {windows}")
    else:
        log(f"RESULT {name}: never refused; {len(sent)} requests in {time.time() - t0:.0f}s; busiest {windows}")

    if first and name == "limit":
        for minute in range(1, 31):
            time.sleep(60)
            status, text, ray = client.get(PATH)
            result = outcome(status, text)
            log(f"try after {minute} min: {result[:60]}")
            if result == "ok":
                log(f"RESULT {name}: the refusal lasted between {minute - 1} and {minute} minutes")
                break
        else:
            log(f"RESULT {name}: still refused after 30 minutes")
    if name == "app" and first:
        sys.exit(1)


if __name__ == "__main__":
    main(sys.argv[1])
