package com.arslan.customanimator.resources

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

class StringResourceIntegrityTest {

    private val resDir = File("src/main/res")
    private val formatPattern = Regex("%(\\d+\\$)?[-#+ 0,(]*\\d*(\\.\\d+)?[a-zA-Z]")

    private data class Strings(val locale: String, val values: Map<String, String>)

    private fun parse(file: File): Map<String, String> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val nodes = document.getElementsByTagName("string")
        val values = LinkedHashMap<String, String>()
        for (index in 0 until nodes.length) {
            val element = nodes.item(index) as Element
            values[element.getAttribute("name")] = element.textContent
        }
        return values
    }

    private fun localeFiles(): List<Strings> {
        return resDir.listFiles()
            .orEmpty()
            .filter { it.isDirectory && it.name.startsWith("values") }
            .mapNotNull { dir ->
                val file = File(dir, "strings.xml")
                if (file.exists()) Strings(dir.name, parse(file)) else null
            }
            .sortedBy { it.locale }
    }

    private fun placeholders(value: String): List<String> =
        formatPattern.findAll(value.replace("%%", "")).map { it.value.takeLast(1) }.toList()

    private fun baseStrings(): Strings = localeFiles().first { it.locale == "values" }

    @Test
    fun baseStringsExistAndAreNotEmpty() {
        val base = baseStrings()
        assertTrue("base strings.xml is empty", base.values.size > 100)
        val blank = base.values.filterValues { it.isBlank() }.keys
        assertTrue("blank strings: $blank", blank.isEmpty())
    }

    @Test
    fun noDuplicateStringNames() {
        localeFiles().forEach { strings ->
            val file = File(resDir, "${strings.locale}/strings.xml")
            val names = Regex("<string name=\"([^\"]+)\"").findAll(file.readText()).map { it.groupValues[1] }.toList()
            val duplicates = names.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
            assertTrue("duplicate names in ${strings.locale}: $duplicates", duplicates.isEmpty())
        }
    }

    @Test
    fun translationsUseTheSameFormatPlaceholders() {
        val base = baseStrings()
        val problems = mutableListOf<String>()
        localeFiles().filter { it.locale != "values" }.forEach { strings ->
            strings.values.forEach { (name, value) ->
                val expected = base.values[name] ?: return@forEach
                val basePlaceholders = placeholders(expected).sorted()
                val localePlaceholders = placeholders(value).sorted()
                if (basePlaceholders != localePlaceholders) {
                    problems += "${strings.locale}/$name expected $basePlaceholders got $localePlaceholders"
                }
            }
        }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    @Test
    fun translationsNeverIntroduceUnknownStringNames() {
        val base = baseStrings()
        val problems = mutableListOf<String>()
        localeFiles().filter { it.locale != "values" }.forEach { strings ->
            val unknown = strings.values.keys - base.values.keys
            if (unknown.isNotEmpty()) problems += "${strings.locale}: $unknown"
        }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    @Test
    fun stringsEscapeApostrophesAndAmpersands() {
        val problems = mutableListOf<String>()
        localeFiles().forEach { strings ->
            val text = File(resDir, "${strings.locale}/strings.xml").readText()
            Regex("<string name=\"([^\"]+)\">(.*?)</string>", RegexOption.DOT_MATCHES_ALL)
                .findAll(text)
                .forEach { match ->
                    val name = match.groupValues[1]
                    val raw = match.groupValues[2]
                    if (Regex("(?<!\\\\)'").containsMatchIn(raw)) {
                        problems += "${strings.locale}/$name has an unescaped apostrophe"
                    }
                    if (Regex("&(?!amp;|lt;|gt;|quot;|apos;|#)").containsMatchIn(raw)) {
                        problems += "${strings.locale}/$name has an unescaped ampersand"
                    }
                }
        }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    @Test
    fun everyStringUsedInCodeExists() {
        val base = baseStrings().values.keys
        val referenced = mutableSetOf<String>()
        File("src/main/java").walkTopDown().filter { it.extension == "kt" }.forEach { file ->
            Regex("R\\.string\\.(\\w+)").findAll(file.readText()).forEach { referenced += it.groupValues[1] }
        }
        val missing = referenced - base
        assertTrue("referenced but undefined strings: $missing", missing.isEmpty())
    }

    @Test
    fun buttonAndTitleStringsStayShortEnoughToFitNarrowScreens() {
        val base = baseStrings()
        val shortKeywords = listOf("_button", "_turn_on", "_turn_off", "_tile_label", "_apply", "_short")
        val tooLong = base.values.filterKeys { name -> shortKeywords.any { name.endsWith(it) } }
            .filterValues { it.length > 40 }
        assertTrue("these labels are too long for small screens: ${tooLong.keys}", tooLong.isEmpty())
    }
}
