package io.github.eyalshimshi.betterstips.data

import io.github.eyalshimshi.betterstips.data.api.ApiException
import io.github.eyalshimshi.betterstips.data.model.AskAllowance
import io.github.eyalshimshi.betterstips.data.model.Cover
import io.github.eyalshimshi.betterstips.data.model.GalleryPhoto
import io.github.eyalshimshi.betterstips.data.model.PenPalAllowance
import io.github.eyalshimshi.betterstips.data.parse.Parsers
import io.github.eyalshimshi.betterstips.util.StipsTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Parsers added in v0.3 (moderation, asking, pen pals, people search), in the shapes Stips sends (SPEC-04). */
class ParsersV3Test {

    private fun data(json: String) = Parsers.envelope(json)

    @Test
    fun login_onlyLoggedFalseMeansTheTokenIsGone() {
        assertTrue(Parsers.loginRejected(data("""{"status":"ok","data":{"logged":false}}""")))
        assertFalse(Parsers.loginRejected(data("""{"status":"ok","data":{"logged":true,"appUser":{"id":5},"a_token":"t"}}""")))
        assertFalse("odd answers keep the session", Parsers.loginRejected(data("""{"status":"ok","data":{}}""")))
        assertFalse(Parsers.loginRejected(null))
    }

    @Test
    fun question_readsWhatTheUserMayDo() {
        val q = Parsers.question(
            data(
                """{"status":"ok","data":{"objType":"ask","data":{"id":7,"q":"t"},
                "meta":{"permissions":{"edit":true,"delete":false,"deleteWithoutMsg":true,"itemOwner":true,"report":false}}}}""",
            ),
        )!!
        assertTrue(q.canEdit)
        assertTrue("deleteWithoutMsg also allows deleting", q.canDelete)
        assertTrue(q.isOwner)
        assertFalse(q.canReport)
        val other = Parsers.question(data("""{"status":"ok","data":{"objType":"ask","data":{"id":8,"q":"t"},"meta":{"permissions":{"report":true}}}}"""))!!
        assertFalse(other.isOwner)
        assertTrue(other.canReport)
    }

    @Test
    fun answer_knowsTheAskersOwnReply() {
        val a = Parsers.answer(
            data(
                """{"status":"ok","data":{"objType":"ans","data":{"id":9,"a":"x"},"extra":{"parentItemCreatorResponse":true},
                "meta":{"permissions":{"delete":true,"report":true}}}}""",
            ),
        )!!
        assertTrue(a.byAsker)
        assertTrue(a.canDelete)
        assertTrue(a.canReport)
    }

    @Test
    fun tagsAndGallery() {
        assertEquals(listOf("מתמטיקה", "בגרות"), Parsers.tags(data("""{"status":"ok","data":{"tags":[" מתמטיקה ","בגרות","","מתמטיקה"]}}""")))
        assertTrue(Parsers.tags(data("""{"status":"ok","data":{}}""")).isEmpty())
        assertEquals(
            listOf(GalleryPhoto("4791385491"), GalleryPhoto("12")),
            Parsers.galleryPhotos(
                data("""{"status":"ok","data":{"photos":[{"photo_id":"4791385491"},{"photo_id":12},{"photo_id":"x1"},{"photo_id":"12"}]}}"""),
            ),
        )
        assertEquals("https://cdn.stips.co.il/photos/w400/12.jpg", GalleryPhoto("12").thumbnailUrl)
    }

    @Test
    fun allowances() {
        assertEquals(AskAllowance(false, 10), Parsers.askAllowance(data("""{"status":"ok","data":{"results":{"allowed":false,"maxPerDay":10}}}""")))
        assertEquals("unknown answers allow asking", AskAllowance(true, null), Parsers.askAllowance(data("""{"status":"ok","data":{}}""")))
        assertEquals(
            PenPalAllowance(false, 60, 17),
            Parsers.penPalAllowance(data("""{"status":"ok","data":{"results":{"allowed":false,"postInterval":60,"nextTimeInMinutes":"17"}}}""")),
        )
        assertEquals("hi there", Parsers.lastPenPalMessage(data("""{"status":"ok","data":{"results":{"messageText":" hi there "}}}""")))
        assertNull(Parsers.lastPenPalMessage(data("""{"status":"ok","data":{"results":{"messageText":""}}}""")))
        assertTrue(Parsers.flowerSent(data("""{"status":"ok","data":{"flowerSent":true}}""")))
        assertFalse(Parsers.flowerSent(data("""{"status":"ok","data":{}}""")))
    }

    @Test
    fun penPals_skipInactivePostsAndReadPermissions() {
        val list = Parsers.penPals(
            data(
                """{"status":"ok","data":[
                {"objType":"penfriendsitem","data":{"id":1,"msg":" hey ","time":"2026/11/03 10:00:00","active":true},
                 "extra":{"item_profile":{"userid":5,"nickname":"Dana","online":true}},"meta":{"permissions":{"delete":true}}},
                {"objType":"penfriendsitem","data":{"id":2,"msg":"gone","active":false}},
                {"objType":"penfriendsitem","data":{"msg":"no id"}}]}""",
            ),
        )
        assertEquals(1, list.size)
        val post = list.single()
        assertEquals("hey", post.text)
        assertEquals(5L, post.author.userId)
        assertTrue(post.author.online)
        assertTrue(post.canDelete)
        assertEquals(StipsTime.parse("2026/11/03 10:00:00"), post.time)
    }

    @Test
    fun peopleSearch_keepsRealUsersOnce() {
        val people = Parsers.userSearch(
            data(
                """{"status":"ok","data":{"results":{"profiles":[
                {"nickname":"Cool","userid":1,"photostamp":"9","anonflg":false},
                {"nickname":"Cool","userid":1},
                {"nickname":"","userid":2},
                {"nickname":"Anon","anonflg":true}]}}}""",
            ),
        )
        assertEquals(listOf(1L), people.map { it.userId })
        assertEquals("https://cdn.stips.co.il/photos/user_profile/a/1/9.jpg", people.single().avatarUrl(large = true))
        assertEquals("https://cdn.stips.co.il/photos/user_profile/l/1/9.jpg", people.single().fullPhotoUrl())
    }

    @Test
    fun notifications_keepTheTeamNoteAndRemovedPoints() {
        val n = Parsers.notifications(
            data(
                """{"status":"ok","data":[{"id":5,"contentHtml":"Your answer was removed","msgText":"Spam","pointsRemoved":"3","viewed":0}]}""",
            ),
        ).single()
        assertEquals("Your answer was removed", n.text)
        assertEquals("Spam", n.note)
        assertEquals(3, n.pointsRemoved)
        assertFalse(n.viewed)
    }

    @Test
    fun covers_haveAFullSizeVersion() {
        assertEquals("https://cdn.stips.co.il/photos/z/42.jpg", Cover.Stips("42").fullUrl)
        assertEquals(
            "https://images.unsplash.com/photo-1?ixid=a&w=1080&fit=crop",
            Cover.Unsplash("s", "https://images.unsplash.com/photo-1?ixid=a&w=680&fit=crop", null).fullUrl,
        )
        assertEquals("https://x.example/p?w=1080", Cover.widen("https://x.example/p", 1080))
        assertEquals("https://x.example/p?q=1&w=1080", Cover.widen("https://x.example/p?q=1", 1080))
    }

    @Test
    fun httpErrorsAreExplainedAndNeverSignOut() {
        assertEquals(ApiException.RATE_LIMITED, ApiException.forHttp(429).code)
        assertEquals(ApiException.BLOCKED, ApiException.forHttp(403).code)
        assertEquals(ApiException.SERVER, ApiException.forHttp(522).code)
        assertEquals(ApiException.HTTP, ApiException.forHttp(404).code)
        listOf(429, 403, 503, 404).forEach { assertTrue(ApiException.forHttp(it).isTransient) }
        assertTrue(ApiException(ApiException.BAD_RESPONSE).isTransient)
        assertFalse(ApiException(ApiException.GENERAL, "x").isTransient)
        assertTrue(ApiException(ApiException.NETWORK).isNetwork)
        assertFalse("an HTTP error is an answer, not a dead network", ApiException.forHttp(429).isNetwork)
        assertTrue(ApiException(ApiException.GENERAL, "המשתמש אינו מחובר למערכת").looksSignedOut)
        assertTrue(ApiException(ApiException.GENERAL, "יש להתחבר כדי לענות").looksSignedOut)
        assertFalse(ApiException(ApiException.GENERAL, "התשובה קצרה מדי").looksSignedOut)
        assertTrue(ApiException(ApiException.NOT_SIGNED_IN).looksSignedOut)
    }
}
