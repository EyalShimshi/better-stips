package io.github.eyalshimshi.betterstips.data

import io.github.eyalshimshi.betterstips.data.parse.Parsers
import io.github.eyalshimshi.betterstips.ui.home.HomeSectionsMerge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Parsers added in v0.9 (#8), in the shapes Stips sends (measured on the live site). */
class ParsersV9Test {

    private fun data(json: String) = Parsers.envelope(json)

    private fun answerBy(itemProfile: String, anonflg: Boolean = false) = Parsers.answer(
        data("""{"status":"ok","data":{"objType":"ans","data":{"id":1,"a":"x","anonflg":$anonflg},"extra":{"item_profile":$itemProfile}}}"""),
    )!!

    @Test
    fun author_readsStipsSpecialAccounts() {
        val knights = answerBy(
            """{"userid":253000,"nickname":"אבירי הרשת","specialConfig":{"profileBackground":"linear-gradient(135deg, #f64f59, #b45ae0, #109cbb)",
            "profileLightColorNickname":true,"itemNicknameSpecialRankColor":true,"itemSpecialRankBranding":"RANK_NET_KNIGHT"}}""",
        ).author
        assertEquals("RANK_NET_KNIGHT", knights.branding)
        assertTrue(knights.highlightName)
        assertTrue(knights.organization)
        assertTrue(knights.verified)
        assertFalse(knights.platform)

        val sahar = answerBy("""{"userid":10486,"nickname":"מתנדבי עמותת סהר"}""").author
        assertNull("no special config", sahar.branding)
        assertTrue("on the app's own list", sahar.organization)
        assertTrue(sahar.verified)

        val system = answerBy("""{"nickname":"מערכת סטיפס","anonflg":true,"isPlatfromAdmin":true}""", anonflg = true).author
        assertTrue("Stips spells it isPlatfromAdmin", system.platform)
        assertTrue(system.verified)
        assertFalse(system.organization)
        assertNull(system.userId)

        val someone = answerBy("""{"userid":5,"nickname":"דני","specialConfig":[]}""").author
        assertNull(someone.branding)
        assertFalse(someone.highlightName)
        assertFalse(someone.verified)

        val hiddenKnight = answerBy(
            """{"userid":253000,"nickname":"אנונימי","anonflg":true,"specialConfig":{"itemSpecialRankBranding":"RANK_NET_KNIGHT","itemNicknameSpecialRankColor":true}}""",
            anonflg = true,
        ).author
        assertNull("an anonymous answer shows nothing of its author", hiddenKnight.branding)
        assertFalse(hiddenKnight.highlightName)
        assertFalse(hiddenKnight.verified)
    }

    @Test
    fun questionsAndAnswers_keepWhatStipsSent() {
        val answer = answerBy("""{"userid":5,"nickname":"דני"}""")
        assertNotNull(answer.raw)
        assertTrue(answer.raw!!.contains("\"item_profile\""))
        val question = Parsers.question(data("""{"status":"ok","data":{"objType":"ask","data":{"id":7,"q":"t"}}}"""))!!
        assertTrue(question.raw!!.contains("\"id\":7"))
    }

    @Test
    fun profile_readsTheSpecialBackgroundAndName() {
        val profile = Parsers.profile(
            data(
                """{"status":"ok","data":{"omniOmniObj":{"objType":"user","data":{"id":253000,"nickname":"אבירי הרשת","points":10},
                "extra":{"item_profile":{"userid":253000,"specialConfig":{"profileBackground":"linear-gradient(135deg, #f64f59, #b45ae0, #109cbb)",
                "itemNicknameSpecialRankColor":true,"itemSpecialRankBranding":"RANK_NET_KNIGHT"}}}}}}""",
            ),
        )!!
        assertEquals(listOf(0xFFF64F59.toInt(), 0xFFB45AE0.toInt(), 0xFF109CBB.toInt()), profile.background)
        assertTrue(profile.highlightName)
        assertEquals("RANK_NET_KNIGHT", profile.branding)
        assertTrue(profile.avatar.verified)
        assertNotNull(profile.raw)

        val plain = Parsers.profile(data("""{"status":"ok","data":{"omniOmniObj":{"objType":"user","data":{"id":5,"nickname":"דני"}}}}"""))!!
        assertTrue(plain.background.isEmpty())
        assertFalse(plain.avatar.verified)
    }

    @Test
    fun profilePage_readsHiddenNumbersAndCustomBadges() {
        val knights = Parsers.profilePage(
            data(
                """{"status":"ok","data":{"questions":0,"answers":12,"flowers":3,"age":30,"hideAge":true,"hideStats":true,
                "badges":[{"name":"custom_rank","label":"מגן מקוון"},{"name":"senior_advisor"}]}}""",
            ),
        )!!
        assertTrue(knights.hideAge)
        assertTrue(knights.hideStats)
        assertEquals(listOf("מגן מקוון"), knights.customBadges)
        assertEquals(listOf("custom_rank", "senior_advisor"), knights.badges)
        assertNotNull(knights.raw)

        val plain = Parsers.profilePage(data("""{"status":"ok","data":{"questions":1,"answers":2,"flowers":0}}"""))!!
        assertFalse(plain.hideAge)
        assertFalse(plain.hideStats)
        assertTrue(plain.customBadges.isEmpty())
    }

    @Test
    fun homeSections_mergeRepeatedHeadingsAndKeepTopics() {
        val sections = Parsers.homeSections(
            data(
                """{"status":"ok","data":{"results":[
                {"elementType":"topic","name":"מתמטיקה","title":"מתמטיקה","items":[{"objType":"ask","data":{"id":1,"q":"a"}},{"objType":"ask","data":{"id":2,"q":"b"}}]},
                {"elementType":"itemList","title":"אולי פספסת","items":[{"objType":"ask","data":{"id":3,"q":"c"}}]},
                {"elementType":"topic","name":"מתמטיקה","items":[{"objType":"ask","data":{"id":2,"q":"b"}},{"objType":"ask","data":{"id":4,"q":"d"}}]},
                {"elementType":"topic","name":"ריק","items":[]},
                {"elementType":"topic","items":[{"objType":"ask","data":{"id":5,"q":"e"}}]}
                ]}}""",
            ),
        )
        assertEquals(listOf("מתמטיקה", "אולי פספסת"), sections.map { it.title })
        assertEquals(listOf(1L, 2L, 4L), sections[0].questions.map { it.id })
        assertEquals("a topic heading opens the topic", "מתמטיקה", sections[0].topic)
        assertNull("an item list is only a title", sections[1].topic)
        assertTrue(Parsers.homeSections(data("""{"status":"ok","data":{}}""")).isEmpty())
    }

    @Test
    fun homeSections_nextPagesAddWithoutRepeats() {
        val first = Parsers.homeSections(
            data(
                """{"status":"ok","data":{"results":[{"elementType":"topic","name":"א","items":[{"objType":"ask","data":{"id":1,"q":"a"}}]}]}}""",
            ),
        )
        val second = Parsers.homeSections(
            data(
                """{"status":"ok","data":{"results":[
                {"elementType":"topic","name":"ב","items":[{"objType":"ask","data":{"id":2,"q":"b"}}]},
                {"elementType":"topic","name":"א","items":[{"objType":"ask","data":{"id":1,"q":"a"}},{"objType":"ask","data":{"id":3,"q":"c"}}]}
                ]}}""",
            ),
        )
        val merged = HomeSectionsMerge.append(first, second)
        assertEquals(listOf("א", "ב"), merged.map { it.title })
        assertEquals(listOf(1L, 3L), merged[0].questions.map { it.id })
        assertEquals(listOf(2L), merged[1].questions.map { it.id })
        assertEquals(first, HomeSectionsMerge.append(first, emptyList()))
    }

    @Test
    fun homeFeedOrder_asTheSiteComputesIt() {
        val zone = java.time.ZoneId.of("Asia/Jerusalem")
        // Sunday is 0: 1000 × weekday + 100 × hour + minute / 5.
        assertEquals(1309, StipsRepository.homeFeedOrder(java.time.ZonedDateTime.of(2026, 11, 1, 13, 47, 0, 0, zone)))
        assertEquals(1900, StipsRepository.homeFeedOrder(java.time.ZonedDateTime.of(2026, 11, 2, 9, 3, 0, 0, zone)))
        assertEquals(8311, StipsRepository.homeFeedOrder(java.time.ZonedDateTime.of(2026, 11, 7, 23, 59, 0, 0, zone)))
    }
}
