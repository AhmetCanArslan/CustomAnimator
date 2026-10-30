package com.arslan.customanimator.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandSuggestionsTest {

    private fun tokens(text: String, cursor: Int = text.length, packages: List<String> = emptyList()) =
        CommandSuggestions.suggest(text, cursor, packages).map { it.token }

    @Test
    fun emptyInputOffersTopLevelCommands() {
        val suggestions = tokens("")
        assertTrue(suggestions.isNotEmpty())
        assertTrue(suggestions.contains("settings"))
    }

    @Test
    fun prefixFiltersAndKeepsMatchesFirst() {
        val suggestions = tokens("sett")
        assertTrue(suggestions.isNotEmpty())
        assertTrue(suggestions.first().startsWith("sett"))
    }

    @Test
    fun secondLevelIsContextual() {
        val suggestions = tokens("settings ")
        assertTrue(suggestions.contains("put"))
        assertTrue(suggestions.contains("get"))
    }

    @Test
    fun installedPackagesArePlacedWhereTheyBelong() {
        val suggestions = tokens("am force-stop ", packages = listOf("com.example.app"))
        assertTrue(suggestions.contains("com.example.app"))
    }

    @Test
    fun unknownCommandsSuggestNothing() {
        assertTrue(tokens("definitelynotacommand ").isEmpty())
    }

    @Test
    fun cursorOutOfRangeIsClamped() {
        assertEquals(tokens("settings", 8), CommandSuggestions.suggest("settings", 999).map { it.token })
    }

    @Test
    fun applyInsertsSuggestionAtCursor() {
        val (text, cursor) = CommandSuggestions.apply("sett", 4, "settings")
        assertEquals("settings ", text)
        assertEquals(text.length, cursor)
    }
}
