package io.github.eyalshimshi.betterstips.util

import io.github.eyalshimshi.betterstips.data.model.Answer
import io.github.eyalshimshi.betterstips.data.model.Author
import io.github.eyalshimshi.betterstips.data.model.Question
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * A question and its answers as text to paste elsewhere (#16): Markdown that people and AI chats read well, or JSON
 * for programs. Answers come in Stips' order, oldest first, numbered from 1, and "^" pointers become "replies to
 * #N". The words stay as written. Answers the person hid stay as numbered placeholders (so the numbers still
 * match), without their words. Nothing private goes in: no account numbers, votes or the reader's own details.
 * Pure.
 */
object ThreadExport {
    enum class Format { Markdown, Json }

    private val time = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    private val pretty = Json { prettyPrint = true }

    /**
     * [answers] in Stips' order (as the screen shows names, e.g. with [AnswerDisplay.withAskerNickname]); [hidden]
     * the ids of answers the person hid; [channel] the channel's name when known.
     */
    fun export(
        format: Format,
        question: Question,
        answers: List<Answer>,
        hidden: Set<Long> = emptySet(),
        channel: String? = null,
        zone: ZoneId = ZoneId.systemDefault(),
    ): String = when (format) {
        Format.Markdown -> markdown(question, answers, hidden, channel, zone)
        Format.Json -> json(question, answers, hidden, channel)
    }

    fun markdown(
        question: Question,
        answers: List<Answer>,
        hidden: Set<Long> = emptySet(),
        channel: String? = null,
        zone: ZoneId = ZoneId.systemDefault(),
    ): String = buildString {
        appendLine("# ${oneLine(question.title)}")
        appendLine()
        val asked = buildList {
            add("Asked by ${name(question.author)}")
            question.postedAt?.let { add("on ${local(it, zone)}") }
        }.joinToString(" ")
        appendLine("- $asked")
        if (channel != null) appendLine("- Channel: ${oneLine(channel)}")
        if (question.topics.isNotEmpty()) appendLine("- Tags: " + question.topics.joinToString(", ") { oneLine(it) })
        question.link?.let { appendLine("- Attached link: $it") }
        appendLine("- Source: ${question.shareUrl}")
        if (question.body.isNotBlank()) {
            appendLine()
            appendLine(question.body.trim())
        }
        appendLine()
        appendLine("## Answers (${answers.size}, oldest first)")
        answers.forEachIndexed { index, answer ->
            appendLine()
            val heading = buildList {
                add("#${index + 1}")
                add(if (answer.id in hidden) "hidden" else name(answer.shownAuthor))
                answer.postedAt?.let { add(local(it, zone)) }
            }.joinToString(" · ")
            appendLine("### $heading")
            if (answer.id in hidden) {
                appendLine("(Hidden by the reader.)")
                return@forEachIndexed
            }
            val facts = facts(answer, replies(answer, index))
            if (facts.isNotEmpty()) appendLine("_" + facts.joinToString(" · ") + "_")
            appendLine()
            appendLine(answer.text.trim())
            answer.links.forEach { link ->
                val title = link.title?.let { oneLine(it) + " " }.orEmpty()
                appendLine()
                appendLine("Link: $title<${link.url}>")
            }
        }
    }.trimEnd() + "\n"

    fun json(
        question: Question,
        answers: List<Answer>,
        hidden: Set<Long> = emptySet(),
        channel: String? = null,
    ): String {
        val root = buildJsonObject {
            put("source", question.shareUrl)
            put("site", "stips.co.il")
            putJsonObject("question") {
                put("id", question.id)
                put("title", question.title)
                put("body", question.body)
                author(question.author)
                question.postedAt?.let { put("postedAt", it.toString()) }
                if (channel != null) put("channel", channel)
                putJsonArray("tags") { question.topics.forEach { add(it) } }
                question.link?.let { put("link", it) }
                put("pins", question.pinCount)
            }
            putJsonArray("answers") {
                answers.forEachIndexed { index, answer ->
                    addJsonObject {
                        put("number", index + 1)
                        if (answer.id in hidden) {
                            put("hidden", true)
                            return@addJsonObject
                        }
                        author(answer.shownAuthor)
                        answer.postedAt?.let { put("postedAt", it.toString()) }
                        put("score", answer.score)
                        if (answer.flowered) put("flower", true)
                        if (answer.byAsker) put("byAsker", true)
                        if (answer.edited) put("edited", true)
                        val replies = replies(answer, index)
                        if (replies.isNotEmpty()) putJsonArray("repliesTo") { replies.forEach { add(it) } }
                        put("text", answer.text)
                        if (answer.links.isNotEmpty()) {
                            putJsonArray("links") {
                                answer.links.forEach { link ->
                                    addJsonObject {
                                        put("url", link.url)
                                        link.title?.let { put("title", it) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return pretty.encodeToString(JsonObject.serializer(), root) + "\n"
    }

    /** The numbers (from 1) of the answers this one replies to with "^" pointers. */
    fun replies(answer: Answer, index: Int): List<Int> = ReplyRefs.resolve(answer.text, index).targets.map { it + 1 }

    private fun facts(answer: Answer, replies: List<Int>): List<String> = buildList {
        if (replies.isNotEmpty()) add("replies to " + replies.joinToString(", ") { "#$it" })
        add("score ${answer.score}")
        if (answer.flowered) add("got a flower")
        if (answer.byAsker) add("by the asker")
        if (answer.edited) add("edited")
    }

    private fun JsonObjectBuilder.author(author: Author) {
        putJsonObject("author") {
            put("name", author.nickname)
            if (author.anonymous) put("anonymous", true)
        }
    }

    private fun name(author: Author): String = oneLine(author.nickname) + if (author.anonymous) " (anonymous)" else ""

    private fun local(instant: Instant, zone: ZoneId): String = time.format(instant.atZone(zone))

    private fun oneLine(text: String): String = text.replace(Regex("""\s+"""), " ").trim()
}
