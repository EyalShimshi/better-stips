package io.github.eyalshimshi.betterstips.util

import io.github.eyalshimshi.betterstips.SpecialAccounts
import io.github.eyalshimshi.betterstips.data.AnswerSwipe
import io.github.eyalshimshi.betterstips.data.QuestionSwipe
import io.github.eyalshimshi.betterstips.data.UserBadges
import io.github.eyalshimshi.betterstips.data.model.ChatMessage
import io.github.eyalshimshi.betterstips.data.model.NotificationTarget
import io.github.eyalshimshi.betterstips.data.model.StipsNotification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * Pure rules added in v0.18: bios with a quote (#29), one sentence for a question's updates (#27), notices' targets in
 * phone notifications, the app's own updates (#25), the creator's badge (#26), the daily moderator check (#31), links
 * and "reply in private" in chats, and the tips (#28). (Opening shared links went in v0.19, for sending to chats.)
 */
class RulesV18Test {

    // --- Bios (#29) ---

    @Test
    fun bio_aLastLineInTildesIsItsQuote() {
        assertEquals(BioText.Parsed("hello", "my quote"), BioText.parse("hello\n~my quote~"))
        assertEquals("~my quote~", BioText.quoteLine("hello\n~my quote~\n"))
        assertNull("one line isn't a quote", BioText.parse("~only~").quote)
        assertNull("nor one that isn't last", BioText.parse("a\n~x~\nb").quote)
        assertNull("nor empty tildes", BioText.parse("a\n~ ~").quote)
        assertNull(BioText.quoteLine("just words"))
    }

    @Test
    fun bioPromo_goesAboveTheQuoteAndLinksTheAppsPage() {
        val male = BioPromo.line(female = false)
        assertTrue(male.endsWith(BioPromo.LINK))
        assertEquals("hello\n\n$male\n~q~", BioPromo.appended("hello\n~q~", female = false))
        assertEquals("the quote stays the quote", "q", BioText.parse(BioPromo.appended("hello\n~q~", female = true)).quote)
        assertEquals("hello\n\n$male", BioPromo.appended("hello", female = false))
        assertTrue(BioPromo.has("see https://www.shimshii.com/stips-plus"))
        assertTrue("the Play link of v0.14–v0.17 still counts", BioPromo.has(BioPromo.PLAY_URL))
        assertFalse(BioPromo.has("hello"))
        assertEquals(1000, BioPromo.BIO_MAX)
    }

    // --- The bell: one sentence per question (#27) ---

    private val t0: Instant = Instant.parse("2026-11-09T10:00:00Z")

    private fun notification(id: Long, text: String, type: String = "new_ask_ans_registered", question: Long? = 100) = StipsNotification(
        id = id,
        text = text,
        type = type,
        viewed = false,
        time = t0.plusSeconds(id * 60),
        target = question?.let { NotificationTarget.OpenQuestion(it) },
    )

    @Test
    fun bell_aQuestionsUpdatesReadAsOneSentenceWithTheirTotal() {
        val newest = notification(10, "התקבלו 3 תשובות חדשות לשאלה: למה?")
        val older = notification(7, "התקבלו שתי תשובות חדשות לשאלה: למה?")
        assertEquals("התקבלו 5 תשובות חדשות לשאלה: למה?", NotificationCategories.combinedText(listOf(newest, older)))
        assertEquals("one says what Stips said", newest.text, NotificationCategories.combinedText(listOf(newest)))
        val one = notification(12, "התקבלה תשובה חדשה לשאלה: מה?")
        val two = notification(11, "התקבלה תשובה חדשה לשאלה: מה?")
        assertEquals("התקבלו 2 תשובות חדשות לשאלה: מה?", NotificationCategories.combinedText(listOf(one, two)))
        assertEquals("", NotificationCategories.combinedText(emptyList()))
    }

    // --- Phone notifications (#27) ---

    @Test
    fun notices_keepWhereTheyLeadInTheirNotification() {
        for (target in listOf(NotificationTarget.OpenQuestion(5), NotificationTarget.OpenProfile(7), null)) {
            assertEquals(target, NotifyRules.decodeTarget(NotifyRules.encodeTarget(target)))
        }
        assertNull(NotifyRules.decodeTarget("chat:1"))
        assertNull(NotifyRules.decodeTarget("ask:x"))
    }

    // --- The app's own updates (#25) ---

    private val downloads = "https://github.com/EyalShimshi/stips-plus/releases/download/"

    private fun release(
        tag: String,
        apk: String?,
        size: Long = 5_000_000,
        digest: String? = null,
        draft: Boolean = false,
        name: String = "Stips+ 0.18.40",
    ): String {
        val digestField = if (digest == null) "" else ",\"digest\":\"$digest\""
        val asset = if (apk == null) "" else "{\"name\":\"$apk\",\"size\":$size,\"browser_download_url\":\"$downloads$tag/$apk\"$digestField}"
        return "{\"tag_name\":\"$tag\",\"name\":\"$name\",\"draft\":$draft," +
            "\"html_url\":\"https://github.com/EyalShimshi/stips-plus/releases/tag/$tag\",\"assets\":[$asset]}"
    }

    @Test
    fun updates_pickTheNewestBuildWithItsApk() {
        val hex = "a".repeat(64)
        val json = "[" + listOf(
            release("build-40", "stipsplus-40.apk", digest = "sha256:$hex"),
            release("build-41", null),
            release("build-42", "stipsplus-42.apk", draft = true),
            release("build-9", "stipsplus-9.apk", name = "Stips+ 0.9.9"),
            release("v1", "stipsplus-1.apk"),
        ).joinToString(",") + "]"
        val newest = UpdateRules.newest(json)!!
        assertEquals(40, newest.build)
        assertEquals("0.18.40", newest.versionName)
        assertEquals(hex, newest.sha256)
        assertEquals(5_000_000L, newest.apkBytes)
        assertTrue(UpdateRules.isNewer(newest, 39))
        assertFalse(UpdateRules.isNewer(newest, 40))
        assertFalse(UpdateRules.isNewer(null, 1))
        assertNull(UpdateRules.newest("not json"))
        assertNull(UpdateRules.newest("[]"))
        assertNull("an APK that's too big isn't ours", UpdateRules.newest("[" + release("build-50", "stipsplus-50.apk", size = UpdateRules.MAX_APK_BYTES + 1) + "]"))
    }

    @Test
    fun updates_onlyFromTheReleasesAndExactlyTheFileListed() {
        assertEquals(31, UpdateRules.buildNumber("build-31"))
        assertNull(UpdateRules.buildNumber("v0.18"))
        assertEquals("build 7", UpdateRules.versionName(null, 7))
        assertEquals("0.18.31", UpdateRules.versionName("Stips+ 0.18.31", 31))
        assertTrue(UpdateRules.isOurDownload("https://github.com/EyalShimshi/stips-plus/releases/download/build-31/stipsplus-31.apk"))
        assertFalse(UpdateRules.isOurDownload("https://github.com/someone/else/releases/download/build-31/stipsplus-31.apk"))
        assertFalse(UpdateRules.isOurDownload("https://github.com/EyalShimshi/stips-plus/releases/download/../../x.apk"))
        assertNull(UpdateRules.sha256Of("md5:abc"))
        assertEquals("b".repeat(64), UpdateRules.sha256Of("sha256:" + "B".repeat(64)))
        val release = UpdateRules.Release(31, "0.18.31", "https://x", 10, "c".repeat(64), "https://y")
        assertTrue(UpdateRules.matches(release, 10, "C".repeat(64)))
        assertFalse(UpdateRules.matches(release, 11, "c".repeat(64)))
        assertFalse(UpdateRules.matches(release, 10, "d".repeat(64)))
        assertTrue("without a digest, the size", UpdateRules.matches(release.copy(sha256 = null), 10, "d".repeat(64)))
        val day = UpdateRules.CHECK_EVERY_MS
        assertTrue(UpdateRules.dueForCheck(0L, day))
        assertFalse(UpdateRules.dueForCheck(1_000L, 1_000L + day - 1))
        assertTrue("the clock went back", UpdateRules.dueForCheck(5_000L, 1_000L))
        val hour = UpdateRules.RETRY_AFTER_FAILURE_MS
        assertFalse("an hour's wait after a check that got no answer", UpdateRules.dueForCheck(0L, day * 3, lastFailureMs = day * 3 - 1_000L))
        assertTrue(UpdateRules.dueForCheck(0L, day * 3, lastFailureMs = day * 3 - hour))
    }

    // --- The creator's badge (#26), moderators checked daily (#31) ---

    @Test
    fun creator_hasTheStipsPlusBadge() {
        val entry = SpecialAccounts.badge(SpecialAccounts.CREATOR_ID)!!
        assertEquals(SpecialAccounts.STIPS_PLUS_COLORS, entry.gradient)
        assertTrue(entry.badgeHe!!.endsWith("\u200E"))
        assertEquals(24L * 60 * 60 * 1000, UserBadges.TTL_MS)
    }

    // --- Links in chats, and reply in private ---

    @Test
    fun chatLinks_aLinkOnItsOwnLineMakesWayForItsCard() {
        val link = "https://stips.co.il/ask/123"
        val read = ChatLinks.read("$link\nwhat do you think?")
        assertEquals(ChatLinks.Target.Question(123, link), read.target)
        assertEquals("what do you think?", read.body)
        assertEquals(0 until link.length + 1, read.removed)

        val inSentence = ChatLinks.read("look at $link now")
        assertEquals("a link in a sentence stays", "look at $link now", inSentence.body)
        assertNull(inSentence.removed)
        assertEquals(ChatLinks.Target.Question(123, link), inSentence.target)

        assertEquals("", ChatLinks.read(link).body)
        assertEquals("the last line takes the line break before it", "hi", ChatLinks.read("hi\n$link").body)
        assertEquals("one in between takes its own", "a\nb", ChatLinks.read("a\n$link\nb").body)
        assertEquals(ChatLinks.Target.Profile(9, "https://www.stips.co.il/profile/9"), ChatLinks.read("https://www.stips.co.il/profile/9").target)
        assertNull(ChatLinks.read("https://example.com/ask/1\nhi").target)
        assertEquals("hi", ChatLinks.read("hi").body)
    }

    @Test
    fun chatLinks_carets_shiftWithTheText() {
        val removed = 0..9
        assertEquals(2..3, ChatLinks.shift(12..13, removed))
        assertNull(ChatLinks.shift(5..6, removed))
        assertEquals(1..2, ChatLinks.shift(1..2, 5..9))
        assertEquals(1..2, ChatLinks.shift(1..2, null))
    }

    @Test
    fun privateReply_writesTheLinkFirstAfterTheCarets() {
        val link = ChatLinks.questionLink(55)
        assertEquals("https://stips.co.il/ask/55", link)
        assertEquals("$link\nhey", ChatLinks.withLink("hey", link))
        assertEquals("^^\n$link\nhey", ChatLinks.withLink("^^\nhey", link))
        assertEquals("only the link", link, ChatLinks.withLink("", link))
        assertEquals("^\n$link", ChatLinks.withLink("^\n", link))
        // What it writes reads back as a card and the words, the carets still opening the message.
        val sent = ChatLinks.withLink("^^\nhey", link)
        assertEquals(2, ReplyRefs.leadingCount(sent))
        assertEquals("hey", ReplyRefs.preview(sent))
    }

    @Test
    fun previews_leaveOutALinkOnItsOwnLine() {
        val link = "https://stips.co.il/ask/1"
        assertEquals("what do you think?", ReplyRefs.preview("$link\nwhat do you think?"))
        assertEquals("a message that's only the link shows it", link, ReplyRefs.preview(link))
        assertEquals(link, ReplyRefs.preview("^\n$link"))
        assertEquals("look at $link", ReplyRefs.preview("look at $link"))
        assertEquals("no link, as before", "hi there", ReplyRefs.preview("^^\nhi there"))
        assertEquals("hi", ChatLinks.withoutLink("$link\nhi"))
        assertEquals("only carets would be left: as written", "^\n$link", ChatLinks.withoutLink("^\n$link"))
    }

    @Test
    fun privateReply_onlyToSomeoneWhoCanBeMessaged() {
        assertTrue(SwipeRules.canMessage(isOwner = false, authorId = 7, anonymous = false, me = 1))
        assertFalse("not your own", SwipeRules.canMessage(isOwner = true, authorId = 1, anonymous = false, me = 1))
        assertFalse("not anonymous", SwipeRules.canMessage(isOwner = false, authorId = null, anonymous = true, me = 1))
        assertFalse("not yourself", SwipeRules.canMessage(isOwner = false, authorId = 1, anonymous = false, me = 1))
        assertTrue(SwipeRules.questionAvailable(QuestionSwipe.PrivateReply, false, 7, false, 1))
        assertFalse(SwipeRules.questionAvailable(QuestionSwipe.PrivateReply, false, 7, true, 1))
        assertTrue("a swipe for others' answers", AnswerSwipe.PrivateReply in SwipeRules.OTHERS_ANSWER_CHOICES)
        assertFalse(AnswerSwipe.PrivateReply in SwipeRules.MY_ANSWER_CHOICES)
    }

    // --- Tips (#28) ---

    @Test
    fun tips_showUntilDoneOrThreeTimes() {
        assertTrue(TutorialRules.due(shown = 0, done = false))
        assertTrue(TutorialRules.due(shown = 2, done = false))
        assertFalse(TutorialRules.due(shown = 3, done = false))
        assertFalse(TutorialRules.due(shown = 0, done = true))
    }

    @Test
    fun tips_findWhatTheyShowOn() {
        fun message(id: Long, from: Long?) = ChatMessage(id, "m$id", null, from, null)
        val messages = listOf(message(1, 7), message(2, 1), message(3, 7), message(4, 1))
        assertEquals(3L, TutorialRules.chatTipTarget(messages, myId = 1)?.id)
        assertNull("they haven't written", TutorialRules.chatTipTarget(listOf(message(1, 1)), myId = 1))
        assertNull(TutorialRules.chatTipTarget(messages, myId = null))
        assertEquals(42L, TutorialRules.questionIdOfKey(42L))
        assertEquals(42L, TutorialRules.questionIdOfKey("section|Hot today|42"))
        assertNull(TutorialRules.questionIdOfKey("new-header"))
        assertNull(TutorialRules.questionIdOfKey(null))
    }
}
