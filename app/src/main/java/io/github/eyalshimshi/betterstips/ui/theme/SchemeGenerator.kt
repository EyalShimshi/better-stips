package io.github.eyalshimshi.betterstips.ui.theme

import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/** sRGB ↔ CIELAB, where L* is Material's "tone" (DESIGN-PHILOSOPHY §2.2). Pure math, no Android. */
object ColorMath {
    data class Lch(val l: Double, val c: Double, val h: Double)

    private const val XN = 0.95047
    private const val YN = 1.0
    private const val ZN = 1.08883
    private const val EPS = 216.0 / 24389.0
    private const val KAPPA = 24389.0 / 27.0

    fun red(argb: Int) = (argb shr 16) and 0xFF
    fun green(argb: Int) = (argb shr 8) and 0xFF
    fun blue(argb: Int) = argb and 0xFF

    fun argb(r: Int, g: Int, b: Int): Int = (0xFF shl 24) or (r shl 16) or (g shl 8) or b

    private fun linearize(channel: Int): Double {
        val v = channel / 255.0
        return if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }

    private fun delinearize(v: Double): Int {
        val c = if (v <= 0.0031308) 12.92 * v else 1.055 * v.pow(1.0 / 2.4) - 0.055
        return (c * 255.0).roundToInt().coerceIn(0, 255)
    }

    private fun f(t: Double): Double = if (t > EPS) cbrt(t) else (KAPPA * t + 16.0) / 116.0

    private fun fInverse(t: Double): Double {
        val cube = t * t * t
        return if (cube > EPS) cube else (116.0 * t - 16.0) / KAPPA
    }

    fun relativeLuminance(argb: Int): Double =
        0.2126 * linearize(red(argb)) + 0.7152 * linearize(green(argb)) + 0.0722 * linearize(blue(argb))

    fun toLch(argb: Int): Lch {
        val r = linearize(red(argb))
        val g = linearize(green(argb))
        val b = linearize(blue(argb))
        val x = 0.4124564 * r + 0.3575761 * g + 0.1804375 * b
        val y = 0.2126729 * r + 0.7151522 * g + 0.0721750 * b
        val z = 0.0193339 * r + 0.1191920 * g + 0.9503041 * b
        val fx = f(x / XN)
        val fy = f(y / YN)
        val fz = f(z / ZN)
        val l = 116.0 * fy - 16.0
        val a = 500.0 * (fx - fy)
        val bb = 200.0 * (fy - fz)
        var h = Math.toDegrees(atan2(bb, a))
        if (h < 0) h += 360.0
        return Lch(l, hypot(a, bb), h)
    }

    fun lStar(argb: Int): Double = toLch(argb).l

    private fun labToLinearRgb(l: Double, a: Double, b: Double): DoubleArray {
        val fy = (l + 16.0) / 116.0
        val fx = fy + a / 500.0
        val fz = fy - b / 200.0
        val x = XN * fInverse(fx)
        val y = YN * fInverse(fy)
        val z = ZN * fInverse(fz)
        return doubleArrayOf(
            3.2404542 * x - 1.5371385 * y - 0.4985314 * z,
            -0.9692660 * x + 1.8760108 * y + 0.0415560 * z,
            0.0556434 * x - 0.2040259 * y + 1.0572252 * z,
        )
    }

    private fun inGamut(rgb: DoubleArray): Boolean = rgb.all { it >= -1e-6 && it <= 1.0 + 1e-6 }

    /** The color at tone [l] with the given hue, keeping as much of [chroma] as sRGB allows. */
    fun fromLch(l: Double, chroma: Double, hueDegrees: Double): Int {
        if (l <= 0.0) return argb(0, 0, 0)
        if (l >= 100.0) return argb(255, 255, 255)
        val rad = Math.toRadians(hueDegrees)
        fun at(c: Double) = labToLinearRgb(l, c * cos(rad), c * sin(rad))
        var rgb = at(chroma)
        if (!inGamut(rgb)) {
            var low = 0.0
            var high = chroma
            repeat(24) {
                val mid = (low + high) / 2.0
                if (inGamut(at(mid))) low = mid else high = mid
            }
            rgb = at(low)
        }
        return argb(delinearize(rgb[0]), delinearize(rgb[1]), delinearize(rgb[2]))
    }

    /** WCAG contrast ratio, 1 to 21. */
    fun contrast(a: Int, b: Int): Double {
        val la = relativeLuminance(a)
        val lb = relativeLuminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }
}

/** Six tonal palettes from one seed color, per DESIGN-PHILOSOPHY §2.2. */
object SchemeGenerator {
    data class TonalPalette(val hue: Double, val chroma: Double) {
        fun tone(t: Int): Int = ColorMath.fromLch(t.toDouble(), chroma, hue)
    }

    data class Palettes(
        val primary: TonalPalette,
        val secondary: TonalPalette,
        val tertiary: TonalPalette,
        val neutral: TonalPalette,
        val neutralVariant: TonalPalette,
        val error: TonalPalette,
    )

    fun palettes(seed: Int): Palettes {
        val lch = ColorMath.toLch(seed)
        val hue = lch.h
        // Grey seeds stay calm: every chroma but error scales down with the seed's own chroma.
        val calm = minOf(1.0, lch.c / 16.0)
        return Palettes(
            primary = TonalPalette(hue, lch.c.coerceIn(32.0, 56.0) * calm),
            secondary = TonalPalette(hue, 18.0 * calm),
            tertiary = TonalPalette((hue + 60.0) % 360.0, 28.0 * calm),
            neutral = TonalPalette(hue, 5.0 * calm),
            neutralVariant = TonalPalette(hue, 10.0 * calm),
            error = TonalPalette(33.0, 62.0),
        )
    }
}
