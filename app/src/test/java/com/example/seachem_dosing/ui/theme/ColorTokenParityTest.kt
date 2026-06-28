package com.example.seachem_dosing.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * ADR-009: [Color.kt] is the canonical colour-token source; `res/values/colors.xml` is a
 * compatibility bridge for the retained XML shell (ADR-007). This test fails on any drift
 * between the two so the hand-maintained bridge can never silently diverge.
 *
 * Pure file parsing — deliberately no Compose/Android classpath dependency, so it runs as a
 * plain JVM unit test. Token→XML-name mapping is derived, so new `Light…`, `Dark…` and
 * `Profile…` tokens are covered automatically (a token with no colors.xml sibling fails).
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

        val xml = xmlRe.findAll(colorsXml.readText())
            .associate { it.groupValues[1] to it.groupValues[2].uppercase() }

        val mismatches = mutableListOf<String>()
        var checked = 0
        composeRe.findAll(colorKt.readText()).forEach { m ->
            val name = m.groupValues[1]
            val composeArgb = m.groupValues[2].uppercase()        // 8 hex, ARGB
            val xmlName = xmlNameFor(name) ?: return@forEach        // non-bridged token: skip
            val xmlHex = xml[xmlName]
            if (xmlHex == null) {
                mismatches += "$name -> missing colors.xml entry '$xmlName'"
                return@forEach
            }
            val expected = if (xmlHex.length == 6) "FF$xmlHex" else xmlHex  // XML opaque -> alpha FF
            if (composeArgb != expected) {
                mismatches += "$name: Color.kt=0x$composeArgb vs colors.xml/$xmlName=#$xmlHex"
            }
            checked++
        }

        assertTrue("No colour tokens parsed from Color.kt — structure/regex changed", checked > 0)
        assertEquals(
            "Colour-token drift (ADR-009 bridge):\n${mismatches.joinToString("\n")}",
            emptyList<String>(),
            mismatches,
        )
    }

    /** Compose token name -> colors.xml sibling, or null when it has no bridge counterpart. */
    private fun xmlNameFor(composeName: String): String? = when {
        composeName.startsWith("Light") -> "md_theme_light_" + role(composeName.removePrefix("Light"))
        composeName.startsWith("Dark") -> "md_theme_dark_" + role(composeName.removePrefix("Dark"))
        composeName.startsWith("Profile") -> "profile_" + composeName.removePrefix("Profile").lowercase()
        else -> null
    }

    /** "OnPrimaryContainer" -> "onPrimaryContainer". */
    private fun role(s: String): String = s.replaceFirstChar { it.lowercase() }

    /** Unit-test working dir is the module dir; fall back to repo root just in case. */
    private fun locate(rel: String): File =
        listOf(File(rel), File("app/$rel")).firstOrNull { it.exists() } ?: File(rel)
}
