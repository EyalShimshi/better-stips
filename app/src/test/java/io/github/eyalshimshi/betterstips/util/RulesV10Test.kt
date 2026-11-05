package io.github.eyalshimshi.betterstips.util

import io.github.eyalshimshi.betterstips.data.UserBadges
import io.github.eyalshimshi.betterstips.data.model.Answer
import io.github.eyalshimshi.betterstips.data.model.Author
import io.github.eyalshimshi.betterstips.data.model.ModeratorRank
import io.github.eyalshimshi.betterstips.data.model.QuestionFocus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** Pure rules added in v0.10: Stips' caret replies, where notifications open, moderators' marks. */
class RulesV10Test {

    private fun counts(text: String) = ReplyRefs.parse(text).refs.map { it.count }

    // The examples come from real answers (a CI probe of 24 busy questions, 1,203 answers; SPEC-04 §2.12).
    @Test
    fun replyRefs_readTheWaysPeopleWriteThem() {
        val stacked = ReplyRefs.parse("^^^ \n^^ \nאם זה משהו שבני אדם המציאו")
        assertEquals(listOf(3, 2), stacked.refs.map { it.count })
        assertTrue(stacked.refs.all { it.leading })
        assertEquals("אם זה משהו שבני אדם המציאו", "^^^ \n^^ \nאם זה משהו שבני אדם המציאו".substring(stacked.bodyStart))

        assertEquals(listOf(2), counts("^^ 3"))
        assertEquals(listOf(1), counts("^החתיך\nאירופה היא היבשת"))
        assertEquals("caret then a number", listOf(6), counts("^6אוקיי אז מה אתה אומר"))
        assertEquals(listOf(4), counts("4^ זה לא גברי"))
        assertEquals(listOf(4), counts("4 ^ זה לא גברי"))
        assertEquals(listOf(5), counts("^^^^^לא קשור"))
        assertEquals("after punctuation", listOf(3), counts("הגיוני לגמרי.^^^\n\nאני התחלתי"))
        assertEquals("at the end", listOf(3), counts("החיים שלך אינם שלי ^3"))
        assertEquals(listOf(8), counts("I ain't reading all that ^^^^^^^^"))
        assertEquals(listOf(1), counts("מה שהוא מחליט - הוא המוסר. ^"))
        assertFalse(ReplyRefs.parse("יש לי שאלה אלייך: ^^\nלמה").refs.single().leading)
        assertEquals("the opening one and one further on", listOf(3, 2), counts("^^^ את קבעת שכל מוות זה רע.\n\n^^ דור המבול"))

        assertTrue("smileys", counts("^_^ חחח ^.^ ^-^ ^o^").isEmpty())
        assertTrue("maths", counts("x^2 + 2^3").isEmpty())
        assertTrue("inside a word", counts("תודה^^").isEmpty())
        assertTrue(counts("bla bla").isEmpty())
        assertEquals(0, ReplyRefs.parse("שלום ^^").bodyStart)
    }

    @Test
    fun replyRefs_writeAndFixTheOpeningPointer() {
        assertEquals("^", ReplyRefs.prefix(1))
        assertEquals("^^^", ReplyRefs.prefix(3))
        assertEquals("from four, the number", "4^", ReplyRefs.prefix(4))
        assertEquals("12^", ReplyRefs.prefix(12))

        assertEquals("^^\n", ReplyRefs.withReply("", 2))
        assertEquals("5^\nשלום", ReplyRefs.withReply("  שלום", 5))
        assertEquals("^^^\nשלום", ReplyRefs.withReply("^^\nשלום", 3))
        assertEquals("^^ היי", ReplyRefs.withReply("4^ היי", 2))
        assertEquals("only the first of stacked pointers", "4^\n^^\nx", ReplyRefs.withReply("^^^\n^^\nx", 4))

        assertEquals("שלום", ReplyRefs.withoutReply("^^\nשלום"))
        assertEquals("שלום", ReplyRefs.withoutReply("שלום"))
        assertEquals("^^\nx", ReplyRefs.withoutReply("^^^\n^^\nx"))
        assertEquals("", ReplyRefs.withoutReply("^^\n"))

        assertEquals(2, ReplyRefs.leadingCount("^^\nx"))
        assertEquals(12, ReplyRefs.leadingCount("12^ x"))
        assertNull(ReplyRefs.leadingCount("x ^^"))
    }

    @Test
    fun replyRefs_resolveAgainstStipsOrder() {
        assertEquals(3, ReplyRefs.target(5, 2))
        assertEquals(0, ReplyRefs.target(1, 1))
        assertNull("above the first answer", ReplyRefs.target(1, 2))
        assertNull(ReplyRefs.target(0, 1))

        val reply = ReplyRefs.resolve("^^\nשלום", 3)
        assertEquals(listOf(1), reply.targets)
        assertEquals("the pointer becomes a quote", "שלום", reply.body)
        assertTrue(reply.inBody.isEmpty())

        val tooFar = ReplyRefs.resolve("^^\nשלום", 1)
        assertTrue(tooFar.targets.isEmpty())
        assertEquals("unresolved pointers stay as written", "^^\nשלום", tooFar.body)

        val middle = ReplyRefs.resolve("יש לי שאלה ^^ למה", 5)
        assertEquals(listOf(3), middle.targets)
        assertEquals("יש לי שאלה ^^ למה", middle.body)
        assertEquals(listOf((11..12) to 3), middle.inBody)

        val bare = ReplyRefs.resolve("^^", 4)
        assertEquals("nothing but the pointer: it stays", "^^", bare.body)
        assertEquals(listOf((0..1) to 2), bare.inBody)

        val stacked = ReplyRefs.resolve("^^^\n^^\nx", 5)
        assertEquals(listOf(2, 3), stacked.targets)
        assertEquals("x", stacked.body)

        val mixed = ReplyRefs.resolve("^^^ את קבעת\n\n^^ דור המבול", 6)
        assertEquals(listOf(3, 4), mixed.targets)
        assertEquals("את קבעת\n\n^^ דור המבול", mixed.body)
        assertEquals(listOf((9..10) to 4), mixed.inBody)

        assertEquals("שלום עולם שורה", ReplyRefs.snippet("^^\nשלום   עולם\nשורה"))
        assertEquals("abcdefghij\u2026", ReplyRefs.snippet("abcdefghijklmnop", max = 10))
    }

    private val t0: Instant = Instant.parse("2026-11-05T10:00:00Z")

    private fun answer(id: Long, minute: Long, owner: Boolean = false, flowered: Boolean = false) = Answer(
        id = id,
        questionId = 1,
        text = "x",
        postedAt = t0.plusSeconds(minute * 60),
        score = 0,
        userVote = 0,
        flowered = flowered,
        author = Author(nickname = "a", userId = id, photoStamp = null, anonymous = false, online = false),
        links = emptyList(),
        isOwner = owner,
    )

    @Test
    fun jumpRules_readTheNotification() {
        assertEquals(QuestionFocus.NewAnswers(3, t0), JumpRules.focusFor("new_ask_ans_registered", "התקבלו 3 תשובות חדשות לשאלה", t0))
        assertEquals(QuestionFocus.MyFlower, JumpRules.focusFor("ans_flower_received", "קיבלת פרח", t0))
        assertNull(JumpRules.focusFor("thanks", "תודה", t0))
        assertEquals(QuestionFocus.MyFlower, JumpRules.focusFor("", "קיבלת פרח על התשובה שלך", null))

        assertEquals(3, JumpRules.newAnswerCount("התקבלו 3 תשובות חדשות"))
        assertEquals(12, JumpRules.newAnswerCount("12 new answers to your question"))
        assertEquals(2, JumpRules.newAnswerCount("שתי תשובות חדשות לשאלה שלך"))
        assertEquals(1, JumpRules.newAnswerCount("תשובה חדשה לשאלה שלך"))
        assertNull(JumpRules.newAnswerCount("דנה ענתה לשאלה שלך"))

        val focus = QuestionFocus.NewAnswers(4, Instant.ofEpochMilli(1_700_000_000_000L))
        assertEquals(focus, JumpRules.decode(JumpRules.kind(focus), JumpRules.count(focus), JumpRules.at(focus)))
        assertEquals(QuestionFocus.MyFlower, JumpRules.decode(JumpRules.kind(QuestionFocus.MyFlower), -1, -1L))
        assertEquals(QuestionFocus.NewAnswers(null, null), JumpRules.decode("answers", -1, -1L))
        assertNull(JumpRules.decode("", -1, -1L))
        assertNull(JumpRules.decode(null, 3, 5L))
    }

    @Test
    fun jumpRules_openAtTheFirstNewAnswer() {
        val answers = (1L..6L).map { answer(it, it) }
        val at6 = t0.plusSeconds(6 * 60)
        assertEquals("three new: the third from the bottom", 4L, JumpRules.target(answers, QuestionFocus.NewAnswers(3, at6), null))
        val later = answers + answer(7, 40)
        assertEquals("answers after the notification don't shift it", 4L, JumpRules.target(later, QuestionFocus.NewAnswers(3, at6), null))
        assertEquals("what's new since the last visit", 6L, JumpRules.target(answers, QuestionFocus.NewAnswers(null, null), 5L))
        assertEquals("the later of the two", 5L, JumpRules.target(answers, QuestionFocus.NewAnswers(3, at6), 4L))
        assertEquals("all seen: the counted ones", 4L, JumpRules.target(answers, QuestionFocus.NewAnswers(3, at6), 6L))
        assertEquals("nothing to go by: the newest", 6L, JumpRules.target(answers, QuestionFocus.NewAnswers(null, null), null))
        val withMine = answers.map { if (it.id == 4L) it.copy(isOwner = true) else it }
        assertEquals("the user's own answer isn't new to them", 5L, JumpRules.target(withMine, QuestionFocus.NewAnswers(3, at6), null))
        assertEquals("more than there are", 1L, JumpRules.target(answers, QuestionFocus.NewAnswers(40, at6), null))

        val flowers = answers.map { if (it.id == 2L || it.id == 5L) it.copy(isOwner = true, flowered = true) else it }
        assertEquals(5L, JumpRules.target(flowers, QuestionFocus.MyFlower, null))
        assertNull(JumpRules.target(answers, QuestionFocus.MyFlower, null))
        assertNull(JumpRules.target(emptyList(), QuestionFocus.NewAnswers(3, null), null))
    }

    @Test
    fun moderators_fromTheirBadges() {
        assertEquals(ModeratorRank.Moderator, ModeratorRank.of(listOf("moderator")))
        assertEquals(ModeratorRank.Senior, ModeratorRank.of(listOf("moderator", "senior_moderator")))
        assertNull("the common one", ModeratorRank.of(listOf("senior_advisor")))
        assertNull(ModeratorRank.of(emptyList()))

        for (female in listOf(true, false, null)) {
            val entry = UserBadges.Entry(setOf("moderator", "custom_rank"), female, 1_730_000_000_000L)
            assertEquals(entry, UserBadges.decode(UserBadges.encode(entry)))
        }
        val none = UserBadges.Entry(emptySet(), null, 5L)
        assertEquals(none, UserBadges.decode(UserBadges.encode(none)))
        assertNull(UserBadges.decode("moderator"))
        assertNull(UserBadges.decode("moderator|f|soon"))
        assertEquals(ModeratorRank.Moderator, UserBadges.Entry(setOf("moderator"), true, 0L).moderator)
    }
}
