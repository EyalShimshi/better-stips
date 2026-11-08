package io.github.eyalshimshi.betterstips.util

import io.github.eyalshimshi.betterstips.data.AppSettings
import io.github.eyalshimshi.betterstips.data.Motion
import io.github.eyalshimshi.betterstips.data.MotionPreset
import io.github.eyalshimshi.betterstips.data.SettingsStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure rules added in v0.16: how a swipe ends (the line or a flick), swiping a snackbar away, refreshing a tab on
 * entry, the Stips+ badge from a bio, the carets the intro colors, and the new defaults.
 */
class RulesV16Test {

    // --- Swipes: the answers' distance, and Material's flick ---

    private fun release(offset: Float, velocity: Float) =
        SwipeRules.release(offset, velocity, trigger = 64f, fling = 125f, flingMin = 16f)

    @Test
    fun swipe_runsPastTheLineOrOnAFlick() {
        assertEquals(SwipeSide.Right, release(70f, 0f))
        assertEquals(SwipeSide.Left, release(-64f, 0f))
        assertNull("short of the line, slowly", release(40f, 100f))
        assertEquals("a flick runs it short of the line", SwipeSide.Right, release(30f, 600f))
        assertEquals(SwipeSide.Left, release(-30f, -600f))
        assertNull("a twitch doesn't", release(10f, 2_000f))
        assertNull("a flick back cancels, even past the line", release(90f, -600f))
        assertEquals("easing back slowly past the line still runs", SwipeSide.Right, release(90f, -100f))
        assertNull("heading back toward rest", release(-30f, 600f))
        assertNull(release(0f, 1_000f))
    }

    @Test
    fun snackbar_goesDownOrSideways() {
        val width = 1_000f
        val height = 160f
        val fling = 300f
        fun exit(dx: Float, dy: Float, vx: Float, vy: Float) = SwipeRules.snackbarExit(dx, dy, vx, vy, width, height, fling)
        assertEquals(SnackbarExit.Right, exit(400f, 0f, 0f, 0f))
        assertEquals(SnackbarExit.Left, exit(-360f, 10f, 0f, 0f))
        assertNull("not far enough", exit(200f, 0f, 0f, 0f))
        assertEquals("a flick to the side", SnackbarExit.Right, exit(60f, 5f, 900f, 50f))
        assertNull("a flick back doesn't", exit(60f, 5f, -900f, 0f))
        assertEquals(SnackbarExit.Down, exit(10f, 90f, 0f, 0f))
        assertEquals("a flick down", SnackbarExit.Down, exit(0f, 30f, 0f, 800f))
        assertNull(exit(0f, 30f, 0f, 100f))
        assertNull(exit(0f, 0f, 0f, 0f))
    }

    // --- Refreshing a tab on entry ---

    @Test
    fun autoRefresh_notWhenJustLoaded() {
        assertTrue("never loaded", AutoRefresh.due(0L, 5_000L))
        assertFalse(AutoRefresh.due(100_000L, 105_000L))
        assertTrue(AutoRefresh.due(100_000L, 100_000L + AutoRefresh.FRESH_MS))
        assertTrue("a clock gone back counts as old", AutoRefresh.due(100_000L, 90_000L))
    }

    // --- The Stips+ badge ---

    @Test
    fun stipsPlusBadge_fromTheBio() {
        assertTrue(BioPromo.has(BioPromo.appended("שלום", female = false)))
        assertTrue(BioPromo.has(BioPromo.appended(null, female = true)))
        assertTrue("the words without the link", BioPromo.has("אוהב כדורגל\nגם אני משתמש בסטיפס+"))
        assertTrue(BioPromo.has("גם אני משתמשת בסטיפס+ :)"))
        assertTrue("the link alone", BioPromo.has("סטיפס+ " + BioPromo.PLAY_URL))
        assertFalse(BioPromo.has("אני משתמש בסטיפס"))
        assertFalse(BioPromo.has(""))
        assertFalse(BioPromo.has(null))
    }

    // --- The intro's carets ---

    @Test
    fun introCarets_areThePointersStipsPlusReads() {
        val en = "people point up with carets: ^ for the one above, ^^ for two up."
        assertEquals(listOf("^", "^^"), ReplyRefs.parse(en).refs.map { en.substring(it.range) })
        val iw = "מצביעים למעלה עם ^: ^ לתשובה שמעל, ^^ לשתיים למעלה, ולכתוב את ה־^ בשבילך."
        assertEquals(listOf("^", "^", "^^", "^"), ReplyRefs.parse(iw).refs.map { iw.substring(it.range) })
    }

    // --- Defaults ---

    @Test
    fun defaults_areTheOwners() {
        val defaults = AppSettings()
        assertEquals(MotionPreset.Less, defaults.motionPreset)
        assertTrue(defaults.liveTimes)
        assertTrue(defaults.hideReplyCarets)
        assertTrue(defaults.chatHideReplyCarets)
        assertFalse(defaults.penPalsLive)
        assertEquals(MotionPreset.Less, Motion.presetFrom(null, legacyAnimations = true))
        assertEquals(MotionPreset.Off, Motion.presetFrom(null, legacyAnimations = false))
        assertEquals("a choice stays", MotionPreset.Full, Motion.presetFrom("Full", legacyAnimations = true))
    }

    @Test
    fun defaults_movedOnceFromTheOldOnes() {
        val old = mapOf(
            "motionPreset" to "Full",
            "liveTimes" to false,
            "hideReplyCarets" to false,
            "chatHideReplyCarets" to false,
            "easyReply" to false,
        )
        assertEquals(
            mapOf("motionPreset" to "Less", "liveTimes" to true, "hideReplyCarets" to true, "chatHideReplyCarets" to true),
            SettingsStore.v16DefaultChanges(old),
        )
        assertTrue("other choices stay", SettingsStore.v16DefaultChanges(mapOf("motionPreset" to "Off", "liveTimes" to true)).isEmpty())
        assertTrue("a new install has nothing to move", SettingsStore.v16DefaultChanges(emptyMap<String, Any>()).isEmpty())
    }
}
