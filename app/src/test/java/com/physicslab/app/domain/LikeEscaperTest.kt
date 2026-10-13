package com.physicslab.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class LikeEscaperTest {

    @Test
    fun escape_plainString_unchanged() {
        assertEquals("normal text", LikeEscaper.escape("normal text"))
    }

    @Test
    fun escape_percentSign_escaped() {
        assertEquals("100\\%", LikeEscaper.escape("100%"))
    }

    @Test
    fun escape_underscore_escaped() {
        assertEquals("sensor\\_test", LikeEscaper.escape("sensor_test"))
    }

    @Test
    fun escape_backslash_escaped() {
        assertEquals("C:\\\\path", LikeEscaper.escape("C:\\path"))
    }

    @Test
    fun escape_allSpecialCharsCombined_escapedProperly() {
        val input = "100%_pure\\sample"
        val expected = "100\\%\\_pure\\\\sample"
        assertEquals(expected, LikeEscaper.escape(input))
    }

    @Test
    fun buildContainsPattern_wrapsWithPercentWildcards() {
        val query = "50%_off\\deal"
        val expected = "%50\\%\\_off\\\\deal%"
        assertEquals(expected, LikeEscaper.buildContainsPattern(query))
    }
}
