package io.github.eyalshimshi.betterstips.data

import io.github.eyalshimshi.betterstips.data.model.AppUser
import io.github.eyalshimshi.betterstips.data.model.NotificationTarget
import io.github.eyalshimshi.betterstips.data.model.UnreadCounts
import io.github.eyalshimshi.betterstips.data.parse.Parsers
import io.github.eyalshimshi.betterstips.util.StipsTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Parsers added in v0.2 (profiles, thanks wall, notifications, pins, follow, answer editing). Shapes as Stips sends them. */
class ParsersV2Test {

    private fun data(json: String) = Parsers.envelope(json)

    @Test
    fun profilePage_readsCountsBioAndBadges() {
        val page = Parsers.profilePage(
            data(
                """{"status":"ok","data":{"questions":120,"answers":"3816","flowers":1387,
                "hebrew_active_since":"6 שנים ו-9 חודשים","age":17,
                "badges":[{"name":"senior_advisor"},{"name":""}],
                "user_profile_page":{"data":{"text_status":"  hello there  "},"meta":{"permissions":{"edit":true}}},
                "profile_quote":"old quote"}}""",
            ),
        )
        assertNotNull(page)
        page!!
        assertEquals(120, page.questions)
        assertEquals(3816, page.answers)
        assertEquals(1387, page.flowers)
        assertEquals("6 שנים ו-9 חודשים", page.activeSince)
        assertEquals(17, page.age)
        assertEquals(listOf("senior_advisor"), page.badges)
        assertEquals("hello there", page.bio)
        assertTrue(page.bioEditable)
    }

    @Test
    fun profilePage_fallsBackToTheOldQuoteAndHidesAMissingAge() {
        val page = Parsers.profilePage(data("""{"status":"ok","data":{"questions":0,"age":0,"profile_quote":" quote "}}"""))!!
        assertEquals("quote", page.bio)
        assertFalse(page.bioEditable)
        assertNull(page.age)
        assertTrue(page.badges.isEmpty())
        assertNull(page.activeSince)
    }

    @Test
    fun chart_isSortedOldestFirstAndSkipsBadPoints() {
        val points = Parsers.chart(
            data(
                """{"status":"ok","data":{"chartData":[
                {"time":"2026/10/25 00:00:00","actionsCount":0},
                {"time":"2026/10/18 00:00:00","actionsCount":1},
                {"time":"bad","actionsCount":5},
                {"time":"2026/10/11 00:00:00","actionsCount":-3}]}}""",
            ),
        )
        assertEquals(3, points.size)
        assertEquals(StipsTime.parse("2026/10/11 00:00:00"), points[0].time)
        assertEquals(listOf(0, 1, 0), points.map { it.count })
        assertTrue(points.zipWithNext().all { (a, b) -> a.time < b.time })
        assertTrue(Parsers.chart(data("""{"status":"ok","data":{}}""")).isEmpty())
    }

    @Test
    fun thanksWall_readsAuthorsAndDeletePermission() {
        val messages = Parsers.thanksMessages(
            data(
                """{"status":"ok","data":[
                {"objType":"thanksmsg","data":{"id":"77","fromuserid":5,"touserid":123456,"msg":" תודה רבה! ","time":"2026/10/20 10:00:00"},
                 "extra":{"item_profile":{"nickname":"Dana","userid":5,"photostamp":"123","anonflg":false}},
                 "meta":{"permissions":{"deleteWithoutMsg":true}}},
                {"objType":"thanksmsg","data":{"msg":"no id"}},
                {"objType":"thanksmsg","data":{"id":78,"msg":"hi","time":"x"},
                 "extra":{"item_profile":{"nickname":"Avi","userid":6}},"meta":{"permissions":{}}}]}""",
            ),
        )
        assertEquals(2, messages.size)
        val first = messages[0]
        assertEquals(77L, first.id)
        assertEquals("תודה רבה!", first.text)
        assertEquals("Dana", first.author.nickname)
        assertEquals(5L, first.author.userId)
        assertTrue(first.canDelete)
        assertEquals(StipsTime.parse("2026/10/20 10:00:00"), first.time)
        assertFalse(messages[1].canDelete)
        assertNull(messages[1].time)
    }

    @Test
    fun notifications_turnHtmlIntoTextAndFindTheirTarget() {
        val list = Parsers.notifications(
            data(
                """{"status":"ok","data":[
                {"id":901,"contentHtml":"<b>Dana</b> answered your question &quot;why?&quot;","msgText":"fallback",
                 "linkPageName":"ask","linkItemId":"19745077","viewed":false,"time":"2026/10/25 12:00:00","type":"new_ask_ans_registered"},
                {"id":902,"contentHtml":"","msgText":"Avi thanked you","linkPageName":"profile","linkItemId":6,"viewed":1,"type":"thanks"},
                {"id":903,"msgText":"system","linkPageName":"other","linkItemId":1},
                {"msgText":"no id"}]}""",
            ),
        )
        assertEquals(3, list.size)
        assertEquals("Dana answered your question \"why?\"", list[0].text)
        assertEquals(NotificationTarget.OpenQuestion(19745077L), list[0].target)
        assertFalse(list[0].viewed)
        assertEquals("new_ask_ans_registered", list[0].type)
        assertEquals("Avi thanked you", list[1].text)
        assertEquals(NotificationTarget.OpenProfile(6L), list[1].target)
        assertTrue(list[1].viewed)
        assertNull(list[2].target)
        assertTrue("a missing flag counts as seen", list[2].viewed)
    }

    @Test
    fun unreadCounts_toleratesStringsAndNegatives() {
        assertEquals(
            UnreadCounts(messages = 2, notifications = 0),
            Parsers.unreadCounts(data("""{"status":"ok","data":{"messagesCount":"2","notificationsCount":-1}}""")),
        )
        assertEquals(UnreadCounts(), Parsers.unreadCounts(null))
    }

    @Test
    fun appUser_readsWordGender() {
        assertEquals(
            AppUser(id = 123456, points = 88, flowers = 1200, safeFilter = false, female = true),
            Parsers.appUser(
                data("""{"status":"ok","data":{"appUser":{"id":123456,"points":88,"flowersCount":"1200","safeFilter":false,"gender":"female"}}}"""),
            ),
        )
        assertNull(Parsers.appUser(data("""{"status":"ok","data":{}}""")))
    }

    @Test
    fun smallResults_followTitleAndUpload() {
        assertEquals(true, Parsers.followState(data("""{"status":"ok","data":{"results":{"notificationsRegistered":true}}}""")))
        assertNull(Parsers.followState(data("""{"status":"ok","data":{"results":{}}}""")))
        assertEquals("Example", Parsers.urlTitle(data("""{"status":"ok","data":{"results":{"title":"  Example  "}}}""")))
        assertNull(Parsers.urlTitle(data("""{"status":"ok","data":{"results":{"title":""}}}""")))
        assertEquals(true to null, Parsers.uploadResult("""{"success":true}"""))
        assertEquals(false to "too big", Parsers.uploadResult("""{"success":false,"error":"too big"}"""))
        assertEquals(false to null, Parsers.uploadResult("<html>oops</html>"))
    }

    @Test
    fun question_readsItsLinkAndPin() {
        val pinned = Parsers.question(
            Parsers.envelope(
                """{"status":"ok","data":{"objType":"ask","data":{"id":5,"q":"t","time":"2026/11/02 16:10:03",
                "q_link":" https://example.com/x ","user_pinned":1,"pin_count":"4"}}}""",
            ),
        )!!
        assertEquals("https://example.com/x", pinned.link)
        assertTrue(pinned.userPinned)
        assertEquals(4, pinned.pinCount)
        val plain = Parsers.question(
            Parsers.envelope("""{"status":"ok","data":{"objType":"ask","data":{"id":6,"q":"t","q_link":"javascript:x","user_pinned":-1}}}"""),
        )!!
        assertNull(plain.link)
        assertFalse(plain.userPinned)
    }

    @Test
    fun answer_readsPermissionsAndItsQuestionTitle() {
        val answer = Parsers.answer(
            Parsers.envelope(
                """{"status":"ok","data":{"objType":"ans","data":{"id":9,"a":"x","askid":5,"anonflg":false},
                "extra":{"parent_item_title":" Why? ","item_profile":{"nickname":"Me","userid":1}},
                "meta":{"permissions":{"edit":true,"itemOwner":true}}}}""",
            ),
        )!!
        assertTrue(answer.canEdit)
        assertTrue(answer.isOwner)
        assertEquals("Why?", answer.parentTitle)
        assertEquals(5L, answer.questionId)
        val other = Parsers.answers(Parsers.envelope("""{"status":"ok","data":[{"objType":"ans","data":{"id":10,"a":"y"}}]}""")).single()
        assertFalse(other.canEdit)
        assertFalse(other.isOwner)
        assertNull(other.parentTitle)
    }
}
