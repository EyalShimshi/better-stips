package io.github.eyalshimshi.betterstips.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * v0.20: Stips+ never installs apps itself, so Play Protect has no reason to take it for malware; *Update* opens the
 * new APK in the browser (D-145), and CI checks the APK as built for it too. Backups keep the settings across a
 * reinstall but never the sign-in, and the intro shows anyway (D-148).
 */
class RulesV20Test {

    private fun source(path: String): File =
        listOf(File("src/main/$path"), File("app/src/main/$path")).first { it.exists() }

    private val release = UpdateRules.Release(
        build = 33,
        versionName = "0.20.33",
        apkUrl = "https://github.com/EyalShimshi/stips-plus/releases/download/build-33/stipsplus-33.apk",
        apkBytes = 4_000_000,
        pageUrl = "https://github.com/EyalShimshi/stips-plus/releases/tag/build-33",
    )

    // --- Updates in the browser (D-145) ---

    @Test
    fun update_opensTheApkInTheBrowser() {
        assertEquals(release.apkUrl, UpdateRules.downloadUrl(release))
        assertEquals(
            "a link that isn't one of the releases' opens the release's page",
            release.pageUrl,
            UpdateRules.downloadUrl(release.copy(apkUrl = "https://example.com/stipsplus-33.apk")),
        )
        assertEquals(release.pageUrl, UpdateRules.downloadUrl(release.copy(apkUrl = "")))
    }

    @Test
    fun app_neverInstallsAppsItself() {
        val manifest = source("AndroidManifest.xml").readText()
        assertFalse("asks to install apps", Regex("<uses-permission[^>]*REQUEST_INSTALL_PACKAGES").containsMatchIn(manifest))
        assertFalse("hands files to an installer", manifest.contains("androidx.core.content.FileProvider"))
        val code = source("java").walkTopDown().filter { it.isFile && it.extension == "kt" }.joinToString("\n") { it.readText() }
        for (sign in listOf("application/vnd.android.package-archive", "PackageInstaller", "ACTION_INSTALL_PACKAGE", "REQUEST_INSTALL_PACKAGES")) {
            assertFalse(sign, code.contains(sign))
        }
    }

    // --- Backups keep the settings, never the sign-in; the intro shows anyway (D-148) ---

    @Test
    fun backups_keepTheSettingsButNeverTheSignIn() {
        val manifest = source("AndroidManifest.xml").readText()
        assertTrue(manifest.contains("android:allowBackup=\"true\""))
        assertTrue(manifest.contains("android:fullBackupContent=\"@xml/backup_rules\""))
        assertTrue(manifest.contains("android:dataExtractionRules=\"@xml/data_extraction_rules\""))
        val neverBackedUp = setOf("sharedpref:session.xml", "sharedpref:anonymous_answers.xml", "file:chats_widget.json")
        fun excluded(section: Element): Set<String> {
            assertEquals("everything else is backed up", 0, section.getElementsByTagName("include").length)
            val excludes = section.getElementsByTagName("exclude")
            return (0 until excludes.length).map { excludes.item(it) as Element }
                .map { it.getAttribute("domain") + ":" + it.getAttribute("path") }
                .toSet()
        }
        fun parse(name: String) = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(source("res/xml/$name")).documentElement
        // Android 11 and older.
        assertEquals(neverBackedUp, excluded(parse("backup_rules.xml")))
        // Android 12 and newer: cloud backups, and moving to a new phone.
        val rules = parse("data_extraction_rules.xml")
        for (name in listOf("cloud-backup", "device-transfer")) {
            val section = rules.getElementsByTagName(name).item(0) as? Element
            assertNotNull(name, section)
            assertEquals(name, neverBackedUp, excluded(section!!))
        }
    }
}