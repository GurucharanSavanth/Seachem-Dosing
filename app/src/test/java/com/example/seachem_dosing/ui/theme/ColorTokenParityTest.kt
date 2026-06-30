package com.example.seachem_dosing.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * ADR-009: [Color.kt] is the canonical colour-token source; `res/values/colors.xml` is the
 * active compatibility bridge for the retained XML theme shell (ADR-007). This test fails on
 * any drift between bridged XML entries and Compose tokens.
 *
 * Pure file parsing — deliberately no Compose/Android classpath dependency, so it runs as a
 * plain JVM unit test. XML-name→token mapping is derived so removed XML bridge entries do not
 * force unused resources back into the APK.
 */
class ColorTokenParityTest {

    private val composeRe = Regex("""val\s+(\w+)\s*=\s*Color\(0x([0-9A-Fa-f]{8})\)""")
    private val xmlRe = Regex("""<color\s+name="([^"]+)">#([0-9A-Fa-f]{6,8})</color>""")

    @Test
    fun colorKt_and_colorsXml_stayInLockstep() {
        val colorKt = locate("src/main/java/com/example/seachem_dosing/ui/theme/Color.kt")
        val colorsXml = locate("src/main/res/values/colors.xml")
        assertTrue("Color.kt not found (cwd=${File(".").absolutePath})", colorKt.exists())
        assertTrue("colors.xml not found (cwd=${File(".").absolutePath})", colorsXml.exists())

        val compose = composeRe.findAll(colorKt.readText())
            .associate { it.groupValues[1] to it.groupValues[2].uppercase() }
        val xml = xmlRe.findAll(colorsXml.readText())
            .map { it.groupValues[1] to it.groupValues[2].uppercase() }

        val mismatches = mutableListOf<String>()
        var checked = 0
        xml.forEach { (xmlName, xmlHex) ->
            val composeName = composeNameFor(xmlName)
            if (composeName == null) {
                mismatches += "$xmlName -> no Color.kt token mapping"
                return@forEach
            }
            val composeArgb = compose[composeName]
            if (composeArgb == null) {
                mismatches += "$xmlName -> missing Color.kt token '$composeName'"
                return@forEach
            }
            val expected = if (xmlHex.length == 6) "FF$xmlHex" else xmlHex  // XML opaque -> alpha FF
            if (composeArgb != expected) {
                mismatches += "$composeName: Color.kt=0x$composeArgb vs colors.xml/$xmlName=#$xmlHex"
            }
            checked++
        }

        assertTrue("No bridged colours parsed from colors.xml — structure/regex changed", checked > 0)
        assertEquals(
            "Colour-token drift (ADR-009 bridge):\n${mismatches.joinToString("\n")}",
            emptyList<String>(),
            mismatches,
        )
    }

    /** colors.xml sibling -> Compose token name, or null when the XML name is outside the bridge. */
    private fun composeNameFor(xmlName: String): String? = when {
        xmlName.startsWith("md_theme_light_") -> "Light" + tokenRole(xmlName.removePrefix("md_theme_light_"))
        xmlName.startsWith("md_theme_dark_") -> "Dark" + tokenRole(xmlName.removePrefix("md_theme_dark_"))
        else -> null
    }

    /** "onPrimaryContainer" -> "OnPrimaryContainer". */
    private fun tokenRole(s: String): String = s.replaceFirstChar { it.uppercase() }

    /** Unit-test working dir is the module dir; fall back to repo root just in case. */
    private fun locate(rel: String): File =
        listOf(File(rel), File("app/$rel")).firstOrNull { it.exists() } ?: File(rel)
}
