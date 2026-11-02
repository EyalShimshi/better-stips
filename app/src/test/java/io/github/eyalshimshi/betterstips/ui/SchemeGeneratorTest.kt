package io.github.eyalshimshi.betterstips.ui

import io.github.eyalshimshi.betterstips.ui.theme.ColorMath
import io.github.eyalshimshi.betterstips.ui.theme.SchemeGenerator
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class SchemeGeneratorTest {
    private val seeds = listOf(
        0xFF09C286, // Stips green
        0xFF6750A4, // Material purple
        0xFF808080, // grey
        0xFFFF5900, // music orange
        0xFF0038B8, // deep blue
        0xFFFFEC27, // yellow, hard to keep in gamut
    ).map { it.toInt() }

    @Test
    fun tonesLandOnTheirLightness() {
        seeds.forEach { seed ->
            val p = SchemeGenerator.palettes(seed)
            listOf(p.primary, p.secondary, p.tertiary, p.neutral, p.neutralVariant, p.error).forEach { palette ->
                listOf(10, 20, 30, 40, 50, 60, 70, 80, 90, 95).forEach { tone ->
                    val l = ColorMath.lStar(palette.tone(tone))
                    assertTrue("seed ${Integer.toHexString(seed)} tone $tone gave L*=$l", abs(l - tone) < 1.0)
                }
            }
        }
    }

    @Test
    fun textRolesHaveReadableContrast() {
        seeds.forEach { seed ->
            val p = SchemeGenerator.palettes(seed)
            val hex = Integer.toHexString(seed)
            assertTrue("light primary/onPrimary $hex", ColorMath.contrast(p.primary.tone(40), p.primary.tone(100)) >= 4.5)
            assertTrue("dark primary/onPrimary $hex", ColorMath.contrast(p.primary.tone(80), p.primary.tone(20)) >= 4.5)
            assertTrue("light surface/onSurface $hex", ColorMath.contrast(p.neutral.tone(98), p.neutral.tone(10)) >= 7.0)
            assertTrue("dark surface/onSurface $hex", ColorMath.contrast(p.neutral.tone(6), p.neutral.tone(90)) >= 7.0)
            assertTrue("light secondary text $hex", ColorMath.contrast(p.neutral.tone(98), p.neutralVariant.tone(30)) >= 4.5)
            assertTrue("containers $hex", ColorMath.contrast(p.primary.tone(90), p.primary.tone(10)) >= 4.5)
        }
    }

    @Test
    fun greySeedsStayCalm() {
        val grey = SchemeGenerator.palettes(0xFF808080.toInt())
        assertTrue("primary chroma ${grey.primary.chroma}", grey.primary.chroma < 4.0)
        assertTrue("error keeps its color", grey.error.chroma > 50.0)
    }

    @Test
    fun stipsGreenStaysGreen() {
        val hue = ColorMath.toLch(0xFF09C286.toInt()).h
        assertTrue("hue $hue", hue in 130.0..175.0)
    }
}
