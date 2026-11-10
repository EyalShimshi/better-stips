package io.github.eyalshimshi.betterstips.util

import io.github.eyalshimshi.betterstips.SpecialAccounts
import io.github.eyalshimshi.betterstips.data.model.Answer
import io.github.eyalshimshi.betterstips.data.model.AnswerLink
import io.github.eyalshimshi.betterstips.data.model.Author
import io.github.eyalshimshi.betterstips.data.model.Organizations
import io.github.eyalshimshi.betterstips.data.model.Question
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

/**
 * Pure rules added in v0.14 (#12, #14, #15, #16): no emojis in questions and answers, the least scrolling for a
 * jump, the special accounts file, the asker's nickname, the bio line, picture links, and exporting a thread.
 */
class RulesV14Test {

    // --- Emojis (#12) ---

    @Test
    fun emoji_leftOutButTextStays() {
        assertEquals("שלום  עולם", EmojiFilter.strip("שלום 😀 עולם"))
        assertEquals("a heart (❤ with the emoji selector) goes whole", "I  it", EmojiFilter.strip("I ❤️ it"))
        assertEquals("a keycap goes whole", "", EmojiFilter.strip("1️⃣"))
        assertEquals("a family joined with ZWJ goes whole", "x", EmojiFilter.strip("x👨‍👩‍👧"))
        assertEquals("an emoji-by-default symbol", "", EmojiFilter.strip("⭐"))
        assertEquals("a letter can't be emoji-styled: only the selector goes", "x", EmojiFilter.strip("x\uFE0F"))
        val plain = "♥ ✓ ★ → ^^ 4^ ש\"ס"
        assertEquals("symbols written as text stay", plain, EmojiFilter.strip(plain))
        assertFalse(EmojiFilter.hasEmoji(plain))
        assertTrue(EmojiFilter.hasEmoji("ok 👍"))
    }

    @Test
    fun emoji_cursorFollowsTheText() {
        // "ab😀cd": the emoji is two chars (2, 3); a cursor after it (4) lands at 2.
        val result = EmojiFilter.strip("ab😀cd", 4, 6)
        assertEquals("abcd", result.text)
        assertEquals(2, result.selectionStart)
        assertEquals(4, result.selectionEnd)
        assertTrue(result.removed)
        val untouched = EmojiFilter.strip("abc", 1, 2)
        assertEquals(EmojiFilter.Result("abc", 1, 2, removed = false), untouched)
    }

    // --- Jumps (#12) ---

    @Test
    fun jump_staysWhenWholeAndMovesTheLeast() {
        fun plan(item: JumpScroll.Item?, before: Boolean = false, index: Int = 5, reversed: Boolean = false) =
            JumpScroll.plan(index, item, before, lastIndex = 10, windowStart = 0, windowEnd = 1000, margin = 100, readsFromEnd = reversed, spacing = 8)
        assertEquals(JumpScroll.Move.Stay, plan(JumpScroll.Item(100, 50)))
        assertEquals("cut at the bottom: up just enough, a margin from the edge", JumpScroll.Move.By(150), plan(JumpScroll.Item(950, 100)))
        assertEquals("cut at the top", JumpScroll.Move.By(-130), plan(JumpScroll.Item(-30, 100)))
        assertEquals("taller than the window: its start shows", JumpScroll.Move.By(-600), plan(JumpScroll.Item(-500, 2000)))
        assertEquals("reversed and tall: its top (far edge) shows", JumpScroll.Move.By(600), plan(JumpScroll.Item(-500, 2000), reversed = true))
        assertEquals(JumpScroll.Move.ToItem(5, -100), plan(null, before = true))
        assertEquals("past the far edge: the next item starts a margin inside", JumpScroll.Move.ToItem(6, -908), plan(null))
        assertEquals("the last item", JumpScroll.Move.ToItem(10, -100), plan(null, index = 10))
        assertEquals(100, JumpScroll.margin(0, 1000))
        assertEquals(0, JumpScroll.margin(500, 400))
    }

    // --- Special accounts (#12, #14) ---

    @Test
    fun specialAccounts_listIsSound() {
        val creator = SpecialAccounts.badge(2042L)!!
        assertEquals("יוצר סטיפס", creator.badgeHe)
        assertEquals("Stips' creator", creator.badgeEn)
        assertEquals(0xFFD99A00.toInt(), creator.color)
        assertNull("an organization has no badge", SpecialAccounts.badge(10486L))
        assertTrue(SpecialAccounts.entry(10486L)!!.verified)
        assertTrue(Organizations.isKnown(253000L))
        assertFalse(Organizations.isKnown(2042L))
        assertFalse(Organizations.isKnown(null))
        assertNull(SpecialAccounts.entry(null))
        // The list is edited by hand: one entry per account, short one-line badges, opaque colors.
        val ids = SpecialAccounts.ALL.map { it.userId }
        assertEquals(ids.size, ids.toSet().size)
        for (entry in SpecialAccounts.ALL) {
            assertTrue(entry.userId > 0)
            assertTrue("an entry without a badge is an organization or Stips' own (#21)", entry.hasBadge || entry.verified || entry.official)
            for (label in listOfNotNull(entry.badgeHe, entry.badgeEn)) {
                assertTrue(label, label.isNotBlank() && label.length <= 24 && label.none { it.isISOControl() })
            }
            entry.color?.let { assertEquals("opaque", 0xFF, it ushr 24) }
        }
    }
    // --- The asker's nickname (#12) ---

    @Test
    fun askerNickname_onlyOnTheirOwnAnonymousQuestion() {
        val asked = author("שואל סקרן", anonymous = true)
        val mine = answer(1, author("אנונימי", anonymous = true), byAsker = true)
        val other = answer(2, author("אנונימי", anonymous = true))
        val named = answer(3, author("Noa"), byAsker = true)
        val shown = AnswerDisplay.withAskerNickname(question(asked), listOf(mine, other, named))
        assertEquals("שואל סקרן", shown[0].shownAuthor.nickname)
        assertTrue("still anonymous", shown[0].shownAuthor.anonymous)
        assertEquals("אנונימי", shown[1].shownAuthor.nickname)
        assertEquals("Noa", shown[2].shownAuthor.nickname)
        // Asked by name: an anonymous answer by the asker stays anonymous.
        val byName = AnswerDisplay.withAskerNickname(question(author("Dan")), listOf(mine))
        assertEquals("אנונימי", byName[0].shownAuthor.nickname)
        assertEquals(listOf(mine), AnswerDisplay.withAskerNickname(null, listOf(mine)))
    }

    // --- The bio line (#12) ---

    @Test
    fun bioPromo_addsBelowAndKeepsTheBio() {
        assertTrue(BioPromo.line(female = false).startsWith("גם אני משתמש בסטיפס+\n"))
        assertTrue(BioPromo.line(female = true).startsWith("גם אני משתמשת בסטיפס+\n"))
        assertTrue(BioPromo.line(false).endsWith(BioPromo.LINK))
        assertEquals("שלום\n\n" + BioPromo.line(false), BioPromo.appended("שלום  \n", female = false))
        assertEquals(BioPromo.line(true), BioPromo.appended(null, female = true))
        assertEquals(BioPromo.line(true), BioPromo.appended("   ", female = true))
        assertTrue(BioPromo.has(BioPromo.appended("hi", false)))
        assertFalse(BioPromo.has("hi"))
        assertFalse(BioPromo.has(null))
        assertTrue(BioPromo.fits("short", false))
        assertFalse(BioPromo.fits("x".repeat(BioPromo.BIO_MAX), false))
    }

    // --- Picture links (#15) ---

    @Test
    fun imageLinks_knowsImgbbAndImageFiles() {
        val page = ImageLinks.classify(" https://ibb.co/NnphwxbM ")
        assertEquals(ImageLinks.Source.ImgbbPage("https://ibb.co/NnphwxbM", "NnphwxbM"), page)
        assertEquals("https://ibb.co/NnphwxbM/oembed.json", (page as ImageLinks.Source.ImgbbPage).oembedUrl)
        assertEquals(
            ImageLinks.Source.Direct("https://i.ibb.co/vcyrdSQ/IMG-1113.jpg", "https://i.ibb.co/vcyrdSQ/IMG-1113.jpg"),
            ImageLinks.classify("https://i.ibb.co/vcyrdSQ/IMG-1113.jpg"),
        )
        assertEquals(
            "made https, without the query",
            "https://i.imgur.com/abc.png",
            (ImageLinks.classify("http://i.imgur.com/abc.png?x=1") as ImageLinks.Source.Direct).imageUrl,
        )
        assertTrue(ImageLinks.isImageLink("https://ibb.co/6y9ptk1/"))
        assertTrue(ImageLinks.isImageLink("https://i.postimg.cc/abc/photo.WEBP"))
    }

    @Test
    fun imageLinks_leavesEverythingElseToTheBrowser() {
        for (link in listOf(
            "https://ibb.co/album/abc12",
            "https://ibb.co/upload",
            "https://ibb.co/",
            "https://example.com/photo.jpg",
            "https://i.ibb.co/x/readme.txt",
            "https://i.ibb.co@evil.example/x.jpg",
            "https://i.ibb.co:8443/x/y.jpg",
            "javascript:alert(1)",
            "ftp://i.ibb.co/x/y.jpg",
            "not a link",
            "",
            null,
        )) {
            assertNull(link, ImageLinks.classify(link))
        }
    }

    @Test
    fun imageLinks_oembedTakesOnlyImgbbFiles() {
        // imgbb's real answer for https://ibb.co/NnphwxbM (slashes escaped, as it sends them).
        val real = """{"version":"1.0","type":"photo","provider_name":"ImgBB","title":"Screenshot",""" +
            """"url":"https:\/\/i.ibb.co\/mFtsQJg3\/Screenshot-2025-02-23-at-4-33-40-PM.png","width":1600,"height":900,""" +
            """"thumbnail_url":"https:\/\/i.ibb.co\/NnphwxbM\/Screenshot-2025-02-23-at-4-33-40-PM.png","thumbnail_width":"180"}"""
        assertEquals(
            ImageLinks.Picture(
                "https://i.ibb.co/mFtsQJg3/Screenshot-2025-02-23-at-4-33-40-PM.png",
                "https://i.ibb.co/NnphwxbM/Screenshot-2025-02-23-at-4-33-40-PM.png",
            ),
            ImageLinks.fromOembed(real),
        )
        assertNull(ImageLinks.fromOembed("""{"url":"https://evil.example/x.png"}"""))
        assertNull("only https", ImageLinks.fromOembed("""{"url":"http://i.ibb.co/a/b.png"}"""))
        assertNull(ImageLinks.fromOembed("""{"url":"https://i.ibb.co/a/page.html"}"""))
        assertNull(ImageLinks.fromOembed("""{"url":12}"""))
        assertNull(ImageLinks.fromOembed("[]"))
        assertNull(ImageLinks.fromOembed("<html>"))
        assertEquals(
            "a bad thumbnail is only left out",
            ImageLinks.Picture("https://i.ibb.co/a/b.jpg", null),
            ImageLinks.fromOembed("""{"url":"https://i.ibb.co/a/b.jpg","thumbnail_url":"https://evil.example/t.jpg"}"""),
        )
    }

    @Test
    fun linksHost_isTheRealHost() {
        assertEquals("login-stips.example", Links.host("https://stips.co.il@login-stips.example/x"))
        assertEquals("host.com", Links.host("https://user:pass@host.com:80/"))
        assertEquals("evil.example", Links.host("https://evil.example\\@stips.co.il/"))
        assertEquals("Example.com", Links.host("https://WWW.Example.com:8080/a"))
        assertEquals("not a link", Links.host("not a link"))
    }

    // --- Exporting a thread (#16) ---

    @Test
    fun export_markdownNumbersRepliesAndKeepsHiddenOut() {
        val q = question(author("Dan"), title = "Which\nlibrary?", body = "Open late?", link = "https://ibb.co/abc1234")
        val first = answer(1, author("Noa"), text = "Herzl Street, until 10", links = listOf(AnswerLink("https://example.com", "Hours")))
        val second = answer(2, author("אנונימי", anonymous = true), text = "^\nThanks!", byAsker = true, score = 3, flowered = true)
        val third = answer(3, author("Troll"), text = "secret words")
        val text = ThreadExport.markdown(q, listOf(first, second, third), hidden = setOf(3L), channel = "Studies", zone = ZoneOffset.UTC)
        assertTrue(text, text.startsWith("# Which library?\n"))
        assertTrue(text.contains("- Asked by Dan on 2026-11-07 10:00"))
        assertTrue(text.contains("- Channel: Studies"))
        assertTrue(text.contains("- Attached link: https://ibb.co/abc1234"))
        assertTrue(text.contains("- Source: https://stips.co.il/ask/123"))
        assertTrue(text.contains("## Answers (3, oldest first)"))
        assertTrue(text.contains("### #1 · Noa · 2026-11-07 10:05"))
        assertTrue(text.contains("Link: Hours <https://example.com>"))
        assertTrue(text.contains("### #2 · אנונימי (anonymous)"))
        assertTrue(text.contains("_replies to #1 · score 3 · got a flower · by the asker_"))
        assertTrue("the words stay as written", text.contains("^\nThanks!"))
        assertTrue(text.contains("### #3 · hidden"))
        assertFalse("a hidden answer's words stay out", text.contains("secret words"))
        assertFalse("and so does its author", text.contains("Troll"))
    }

    @Test
    fun export_jsonIsStructured() {
        val q = question(author("Dan"))
        val first = answer(1, author("Noa"), text = "one")
        val second = answer(2, author("Dan"), text = "^^ two", byAsker = true)
        val third = answer(3, author("Eli"), text = "^ three")
        val hidden = answer(4, author("Troll"), text = "secret")
        val root = Json.parseToJsonElement(ThreadExport.json(q, listOf(first, second, third, hidden), hidden = setOf(4L))).jsonObject
        assertEquals("https://stips.co.il/ask/123", root["source"]!!.jsonPrimitive.content)
        val question = root["question"]!!.jsonObject
        assertEquals("Title", question["title"]!!.jsonPrimitive.content)
        assertEquals("Dan", question["author"]!!.jsonObject["name"]!!.jsonPrimitive.content)
        val answers = root["answers"]!!.jsonArray
        assertEquals(4, answers.size)
        assertEquals(1, answers[0].jsonObject["number"]!!.jsonPrimitive.int)
        assertNull("^^ from #2 points above the first answer", answers[1].jsonObject["repliesTo"])
        assertEquals(JsonArray(listOf(JsonPrimitive(2))), answers[2].jsonObject["repliesTo"])
        assertEquals(JsonPrimitive(true), answers[1].jsonObject["byAsker"])
        assertEquals(setOf("number", "hidden"), (answers[3] as JsonObject).keys)
        assertFalse(ThreadExport.json(q, listOf(hidden), hidden = setOf(4L)).contains("secret"))
        assertEquals(listOf(1), ThreadExport.replies(third, 1))
    }

    private fun author(name: String, anonymous: Boolean = false) =
        Author(nickname = name, userId = if (anonymous) null else name.hashCode().toLong(), photoStamp = null, anonymous = anonymous, online = false)

    private fun question(author: Author, title: String = "Title", body: String = "Body", link: String? = null) = Question(
        id = 123,
        title = title,
        body = body,
        topics = listOf("study"),
        postedAt = Instant.parse("2026-11-07T10:00:00Z"),
        answerCount = 3,
        pinCount = 1,
        channelId = 1,
        author = author,
        cover = null,
        link = link,
    )

    private fun answer(
        id: Long,
        author: Author,
        text: String = "text",
        byAsker: Boolean = false,
        score: Int = 0,
        flowered: Boolean = false,
        links: List<AnswerLink> = emptyList(),
    ) = Answer(
        id = id,
        questionId = 123,
        text = text,
        postedAt = Instant.parse("2026-11-07T10:00:00Z").plusSeconds(300 * id),
        score = score,
        userVote = 0,
        flowered = flowered,
        author = author,
        links = links,
        byAsker = byAsker,
    )
}
