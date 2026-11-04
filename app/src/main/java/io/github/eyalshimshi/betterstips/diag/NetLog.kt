package io.github.eyalshimshi.betterstips.diag

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * The last few hundred network and session events, kept in memory for issue reports (#3). It never holds request
 * parameters, cookies, tokens, passwords or message text: only API method names, hosts, status codes, timings,
 * connection facts and error types.
 */
object NetLog {
    data class Entry(val timeMs: Long, val kind: String, val text: String)

    private const val MAX_ENTRIES = 600
    private const val MAX_TEXT = 300
    private val entries = ArrayDeque<Entry>()
    private val _version = MutableStateFlow(0L)

    /** Changes whenever the log does, so an open log screen can refresh. */
    val version: StateFlow<Long> = _version.asStateFlow()

    fun event(kind: String, text: String) {
        add(Entry(System.currentTimeMillis(), kind, text.take(MAX_TEXT)))
    }

    @Synchronized
    private fun add(entry: Entry) {
        entries.addLast(entry)
        while (entries.size > MAX_ENTRIES) entries.removeFirst()
        _version.value = _version.value + 1
    }

    @Synchronized
    fun snapshot(): List<Entry> = entries.toList()

    @Synchronized
    fun clear() {
        entries.clear()
        _version.value = _version.value + 1
    }

    private val clock = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

    /** One line per event, oldest first, under [header] (app version, device). Pure, for tests and sharing. */
    fun format(entries: List<Entry>, header: String, zone: ZoneId = ZoneId.systemDefault()): String = buildString {
        appendLine(header.trim())
        appendLine("Events: ${entries.size}, oldest first. Times are ${zone.id}.")
        appendLine()
        for (e in entries) {
            append(Instant.ofEpochMilli(e.timeMs).atZone(zone).format(clock))
            append("  ")
            append(e.kind.padEnd(8))
            append(' ')
            appendLine(e.text)
        }
    }
}
