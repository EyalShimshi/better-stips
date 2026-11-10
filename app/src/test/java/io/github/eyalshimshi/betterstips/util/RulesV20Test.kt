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
 * new APK in the browser (D-145). And nothing is backed up, so reinstalling starts fresh, with the intro (D-146).
 * CI checks the APK as built for both too.
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

    // --- No backups (D-146) ---

    @Test
    fun backups_nothingLeavesThePhone() {
        val manifest = source("AndroidManifest.xml").readText()
        assertTrue("no cloud backup", manifest.contains("android:allowBackup=\"false\""))
        assertFalse("Android 11's backup rules would be moot", manifest.contains("android:fullBackupContent"))
        assertTrue(manifest.contains("android:dataExtractionRules=\"@xml/data_extraction_rules\""))
        val rules = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(source("res/xml/data_extraction_rules.xml")).documentElement
        val everywhere = setOf(
            "root", "file", "database", "sharedpref", "external",
            "device_root", "device_file", "device_database", "device_sharedpref",
        )
        // allowBackup="false" doesn't stop Android 12+'s phone-to-phone transfer: the rules must, for both.
        for (section in listOf("cloud-backup", "device-transfer")) {
            val element = rules.getElementsByTagName(section).item(0) as? Element
            assertNotNull(section, element)
            assertEquals("$section includes nothing", 0, element!!.getElementsByTagName("include").length)
            val excludes = element.getElementsByTagName("exclude")
            val excluded = (0 until excludes.length).map { excludes.item(it) as Element }
                .filter { it.getAttribute("path") == "." }
                .map { it.getAttribute("domain") }
                .toSet()
            assertEquals(section, everywhere, excluded)
        }
    }
}
