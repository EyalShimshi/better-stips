package io.github.eyalshimshi.betterstips.data

import io.github.eyalshimshi.betterstips.data.api.ApiException
import io.github.eyalshimshi.betterstips.data.model.Cover
import io.github.eyalshimshi.betterstips.data.parse.Parsers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.Instant

class ParsersTest {

    private fun fixture(name: String): String =
        requireNotNull(javaClass.getResourceAsStream("/fixtures/$name")) { "missing fixture $name" }
            .bufferedReader(Charsets.UTF_8).use { it.readText() }

    @Test
    fun feed_parsesValidQuestionsAndSkipsBrokenOnes() {
        val questions = Parsers.questions(Parsers.envelope(fixture("feed_new.json")))
        assertEquals(2, questions.size)

        val first = questions[0]
        assertEquals(19745126L, first.id)
        assertEquals("איך התליון שלי? נראה טוב?", first.title)
        assertTrue(first.body.startsWith("https://www.example.com/pendant"))
        assertEquals(listOf("כסף", "מחירים"), first.topics)
        assertEquals(Instant.parse("2026-11-02T14:59:56Z"), first.postedAt)
        assertEquals(0, first.answerCount)
        assertEquals(0, first.channelId)
        assertEquals(Cover.Stips("4791385491"), first.cover)
        assertEquals("https://cdn.stips.co.il/photos/s/4791385491.jpg", first.cover?.thumbnailUrl)
        assertEquals(1001L, first.author.userId)
        assertFalse(first.author.anonymous)
        assertEquals(
            "https://cdn.stips.co.il/photos/user_profile/t/1001/5174315841858.jpg",
            first.author.avatarUrl(),
        )
        assertEquals("https://stips.co.il/ask/19745126", first.shareUrl)
    }

    @Test
    fun feed_toleratesStringNumbersAndAnonymousAuthors() {
        val second = Parsers.questions(Parsers.envelope(fixture("feed_new.json")))[1]
        assertEquals(19745077L, second.id)
        assertEquals(13, second.answerCount)
        assertEquals(4, second.channelId)
        assertTrue(second.author.anonymous)
        assertNull(second.author.userId)
        assertNull(second.author.avatarUrl())
        assertEquals("אנונימית", second.author.nickname)
        val cover = second.cover as Cover.Unsplash
        assertEquals("https://images.unsplash.com/photo-1?w=400", cover.thumbnailUrl)
        assertEquals("https://images.unsplash.com/photo-1?w=680", cover.largeUrl)
        assertEquals("Damien Dufour", cover.creatorName)
    }

    @Test
    fun questionItem_readsOmniOmniObj() {
        val question = Parsers.questionItem(Parsers.envelope(fixture("question_item.json")))
        assertNotNull(question)
        assertEquals(19745077L, question!!.id)
        assertEquals("פרידה לפני חודש וחצי", question.body)
        assertEquals(listOf("אהבה", "זוגיות"), question.topics)
    }

    @Test
    fun answers_parseScoresVotesFlowersAndLinks() {
        val answers = Parsers.answers(Parsers.envelope(fixture("answers.json")))
        assertEquals(2, answers.size)

        val first = answers[0]
        assertEquals(105021432L, first.id)
        assertEquals(19745077L, first.questionId)
        assertEquals("לא צריך לחפש\nזה מגיע לבד", first.text)
        assertEquals(2, first.score)
        assertEquals(1, first.userVote)
        assertTrue(first.flowered)
        assertTrue(first.author.anonymous)
        assertEquals(1, first.links.size)
        assertEquals("https://example.com/a", first.links[0].url)
        assertEquals("מקור", first.links[0].title)

        val second = answers[1]
        assertNull("an unparseable time becomes null, not a crash", second.postedAt)
        assertEquals(-1, second.score)
        assertEquals(1002L, second.author.userId)
        assertTrue(second.author.online)
        assertNull("a blank photo stamp means no avatar URL", second.author.avatarUrl())
        assertTrue("only http(s) links are kept", second.links.isEmpty())
    }

    @Test
    fun channelsFromConfig_parsesGradientsAndSkipsNamelessChannels() {
        val channels = Parsers.channelsFromConfig(Parsers.envelope(fixture("remote_config.json")))
        assertEquals(listOf(1, 8, 4), channels.map { it.id })
        assertEquals("לימודים", channels[2].name)
        assertEquals(3, channels[2].colors.size)
        assertEquals(0xFF00B165.toInt(), channels[2].seedColor)
        assertEquals(5, channels[0].colors.size)
    }

    @Test
    fun envelope_mapsFailuresToApiExceptions() {
        try {
            Parsers.envelope("""{"status":"failed","error_code":"API_ERROR_EOF","data":{},"error_msg":"this item does not exits"}""")
            fail("expected ApiException")
        } catch (e: ApiException) {
            assertEquals(ApiException.NOT_FOUND, e.code)
            assertEquals("this item does not exits", e.serverMessage)
        }
        try {
            Parsers.envelope("""{"status":"failed"}""")
            fail("expected ApiException")
        } catch (e: ApiException) {
            assertEquals(ApiException.GENERAL, e.code)
        }
        try {
            Parsers.envelope("<html>Cloudflare</html>")
            fail("expected ApiException")
        } catch (e: ApiException) {
            assertEquals(ApiException.BAD_RESPONSE, e.code)
        }
    }

    @Test
    fun login_readsTokenUserAndSafeFilter() {
        val result = Parsers.login(
            Parsers.envelope("""{"status":"ok","data":{"appUser":{"id":1001,"anonflg":false,"safeFilter":false},"a_token":"tok-123"}}"""),
        )
        assertNotNull(result)
        assertEquals("tok-123", result!!.token)
        assertEquals(1001L, result.userId)
        assertEquals(false, result.safeFilter)
        assertNull("no appUser means the sign-in failed", Parsers.login(Parsers.envelope("""{"status":"ok","data":{}}""")))
    }

    @Test
    fun profile_keepsOnlyWhatTheSiteShowsItsOwner() {
        val profile = Parsers.profile(
            Parsers.envelope(
                """{"status":"ok","data":{"omniOmniObj":{"objType":"user","data":{"id":1001,"nickname":"איתי","points":"1240","gender":2,"birthDate":"2010-01-01"},"extra":{"photostamp":"999"}}}}""",
            ),
        )
        assertNotNull(profile)
        assertEquals(1001L, profile!!.userId)
        assertEquals("איתי", profile.nickname)
        assertEquals(1240, profile.points)
        assertTrue(profile.female)
        assertEquals("999", profile.photoStamp)
    }

    @Test
    fun conversations_identifyTheOtherPersonAndWhoWroteLast() {
        val conversations = Parsers.conversations(Parsers.envelope(fixture("messages_list.json")), myUserId = 1001L)
        assertEquals(2, conversations.size)
        assertEquals(2002L, conversations[0].other.userId)
        assertEquals("יעל", conversations[0].other.nickname)
        assertEquals(2, conversations[0].unread)
        assertFalse(conversations[0].lastFromMe)
        assertTrue(conversations[1].lastFromMe)
        assertNull(conversations[1].other.avatarUrl())
    }

    @Test
    fun messages_andNewId() {
        val messages = Parsers.messages(Parsers.envelope(fixture("messages_list.json")))
        assertEquals(listOf(5001L, 5000L, 4999L), messages.map { it.id })
        assertEquals(2002L, messages[0].fromUserId)
        assertEquals(42L, Parsers.newId(Parsers.envelope("""{"status":"ok","data":{"newid":"42"}}""")))
        assertNull(Parsers.newId(Parsers.envelope("""{"status":"ok","data":{}}""")))
    }

    @Test
    fun parseColors_readsCssHexColors() {
        val colors = Parsers.parseColors("linear-gradient(to left top, #00b165, #00bfc2,#26c1f8)")
        assertEquals(listOf(0xFF00B165.toInt(), 0xFF00BFC2.toInt(), 0xFF26C1F8.toInt()), colors)
        assertTrue(Parsers.parseColors(null).isEmpty())
    }
}
