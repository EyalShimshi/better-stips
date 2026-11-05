package io.github.eyalshimshi.betterstips.util

import io.github.eyalshimshi.betterstips.data.model.AnswerLink
import io.github.eyalshimshi.betterstips.data.model.AnswerRevision
import io.github.eyalshimshi.betterstips.data.parse.Parsers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure rules added in v0.13 (#10): an answer's edit history, as the site's "גרסאות קודמות" reads it. */
class EditHistoryTest {

    private val revisions = Parsers.answerRevisions(
        Parsers.envelope(
            """{"status":"ok","data":[""" +
                """{"objType":"omniobj_revision","data":{"id":3,"editor":"moderator","jsonstring":"{\"a\":\"second &gt; text\"}"},"extra":{"hebrew_time":"לפני שעה"}},""" +
                """{"objType":"omniobj_revision","data":{"id":2,"editor":"owner","jsonstring":"{\"a\":\"first\",\"link1\":\"https://a.example\",\"link1name\":\"A\"}"},"extra":{"hebrew_time":"לפני יומיים"}},""" +
                """{"objType":"omniobj_revision","data":{"id":1,"jsonstring":"not json"}}""" +
                """]}""",
        ),
    )

    @Test
    fun parsers_readRevisions() {
        assertEquals(3, revisions.size)
        assertEquals("the old text, decoded once", "second > text", revisions[0].text)
        assertEquals("moderator", revisions[0].editor)
        assertNull("the edit didn't touch the links", revisions[0].links)
        assertEquals("לפני שעה", revisions[0].timeText)
        assertEquals("first", revisions[1].text)
        assertEquals(listOf(AnswerLink("https://a.example", "A")), revisions[1].links)
        assertEquals("owner", revisions[1].editor)
        assertNull("unreadable JSON keeps nothing", revisions[2].text)
        assertNull(revisions[2].editor)
        assertEquals(emptyList<AnswerRevision>(), Parsers.answerRevisions(Parsers.envelope("""{"status":"ok","data":[]}""")))
    }

    @Test
    fun parsers_answersSayWhetherTheyWereEdited() {
        val answers = Parsers.answers(
            Parsers.envelope(
                """{"status":"ok","data":[""" +
                    """{"objType":"ans","data":{"id":10,"a":"x","revision_flg":1}},""" +
                    """{"objType":"ans","data":{"id":11,"a":"y","revision_flg":"0"}},""" +
                    """{"objType":"ans","data":{"id":12,"a":"z"}}""" +
                    """]}""",
            ),
        )
        assertEquals(listOf(true, false, false), answers.map { it.edited })
    }

    @Test
    fun editHistory_versionsNewestFirst() {
        val versions = EditHistory.versions("third", emptyList(), revisions.take(2))
        assertEquals(listOf("third", "second > text", "first"), versions.map { it.text })
        assertTrue(versions[0].current)
        assertEquals("the newest edit made the current text", EditHistory.Editor.Moderator, versions[0].editedBy)
        assertEquals("לפני שעה", versions[0].editedAtText)
        assertEquals(EditHistory.Editor.Owner, versions[1].editedBy)
        assertEquals("לפני יומיים", versions[1].editedAtText)
        assertEquals("links the edit didn't change are today's", emptyList<AnswerLink>(), versions[1].links)
        assertTrue(versions[2].original)
        assertNull(versions[2].editedBy)
        assertEquals(listOf(AnswerLink("https://a.example", "A")), versions[2].links)
        assertFalse(versions[1].current)

        val only = EditHistory.versions("only", emptyList(), emptyList()).single()
        assertTrue(only.current)
        assertTrue(only.original)

        val linkOnly = EditHistory.versions("same", emptyList(), listOf(AnswerRevision(null, emptyList(), "owner", null, null)))
        assertEquals("an edit of the link alone keeps the text", listOf("same", "same"), linkOnly.map { it.text })
    }

    @Test
    fun editHistory_editors() {
        assertEquals(EditHistory.Editor.Owner, EditHistory.editor("owner"))
        assertEquals(EditHistory.Editor.Moderator, EditHistory.editor("moderator"))
        assertEquals(EditHistory.Editor.Stips, EditHistory.editor("platformAdmin"))
        assertEquals(EditHistory.Editor.Unknown, EditHistory.editor("someone"))
        assertEquals(EditHistory.Editor.Unknown, EditHistory.editor(null))
    }
}
