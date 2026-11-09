package io.github.eyalshimshi.betterstips.util

/**
 * The bell's badge on Home (v0.17). Stips counts unread notifications, but the count drops to 0 as soon as any app on
 * the account reads the list (the official app, the site, Stips+'s phone check), so the badge often stayed empty while
 * the bell had new ones. Now Stips+ reads the bell's first page itself now and then, keeps what's new until the bell
 * shows it, and counts rows as the bell shows them: a question's updates are one. Pure.
 */
object BellRules {
    /** On Home, where the bell is, its first page is read this often while the app is on screen. */
    const val LIST_EVERY_MS = 60_000L

    /** Whether to read the bell's first page now: Stips' count went up since [before], or on Home a minute went by. */
    fun listNow(stips: Int, before: Int, sinceListedMs: Long, onHome: Boolean): Boolean =
        stips > before || (onHome && sinceListedMs >= LIST_EVERY_MS)

    /**
     * The badge: the [rows] waiting for the bell. Stips' own count only fills in until the page is read: right after
     * reading it ([listed]) Stips counts those as seen, and the rows say it all.
     */
    fun badge(rows: Int, stips: Int, listed: Boolean): Int = if (listed) rows else maxOf(rows, stips)
}
