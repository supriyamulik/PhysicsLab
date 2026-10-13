package com.physicslab.app.domain

import com.physicslab.app.data.Quantity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale
import java.util.TimeZone

class NameValidatorTest {

    private val fixedUtcZone = TimeZone.getTimeZone("UTC")
    private val locale = Locale.US
    // 2026-10-13 14:32:00 UTC = 1791901920000L
    private val fixedTimestamp = 1791901920000L

    @Test
    fun generateDefaultName_formatsCorrectPattern() {
        val name = NameValidator.generateDefaultName(
            quantity = Quantity.MAGNETIC_FIELD,
            timestampEpochMillis = fixedTimestamp,
            timeZone = fixedUtcZone,
            locale = locale
        )
        assertEquals("Magnetic Field · 13 Oct 2026, 14:32", name)
    }

    @Test
    fun sanitizeOrGenerate_nullInput_returnsDefaultName() {
        val result = NameValidator.sanitizeOrGenerate(
            inputName = null,
            quantity = Quantity.ACCELERATION,
            timestampEpochMillis = fixedTimestamp,
            timeZone = fixedUtcZone,
            locale = locale
        )
        assertEquals("Acceleration · 13 Oct 2026, 14:32", result)
    }

    @Test
    fun sanitizeOrGenerate_blankInput_returnsDefaultName() {
        val result = NameValidator.sanitizeOrGenerate(
            inputName = "    ",
            quantity = Quantity.LIGHT,
            timestampEpochMillis = fixedTimestamp,
            timeZone = fixedUtcZone,
            locale = locale
        )
        assertEquals("Light · 13 Oct 2026, 14:32", result)
    }

    @Test
    fun sanitizeOrGenerate_validInput_trimmedAndPreserved() {
        val result = NameValidator.sanitizeOrGenerate(
            inputName = "  Hall Effect Lab 1  ",
            quantity = Quantity.MAGNETIC_FIELD,
            timestampEpochMillis = fixedTimestamp,
            timeZone = fixedUtcZone,
            locale = locale
        )
        assertEquals("Hall Effect Lab 1", result)
    }

    @Test
    fun sanitizeOrGenerate_exceeds60Chars_truncatedTo60() {
        val longName = "A".repeat(80)
        val result = NameValidator.sanitizeOrGenerate(
            inputName = longName,
            quantity = Quantity.ACCELERATION,
            timestampEpochMillis = fixedTimestamp,
            timeZone = fixedUtcZone,
            locale = locale
        )
        assertEquals(60, result.length)
        assertEquals("A".repeat(60), result)
    }

    @Test
    fun validateForRename_validName_trimmed() {
        val renamed = NameValidator.validateForRename("  My New Sensor Run  ")
        assertEquals("My New Sensor Run", renamed)
    }

    @Test
    fun validateForRename_emptyOrBlankName_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            NameValidator.validateForRename("")
        }
        assertThrows(IllegalArgumentException::class.java) {
            NameValidator.validateForRename("     ")
        }
    }

    @Test
    fun validateForRename_exceeds60Chars_truncated() {
        val longName = "B".repeat(75)
        val renamed = NameValidator.validateForRename(longName)
        assertEquals(60, renamed.length)
        assertEquals("B".repeat(60), renamed)
    }
}
