package com.physicslab.app.domain

import com.physicslab.app.data.Quantity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Validation and default naming rules for [com.physicslab.app.data.SensorReading].
 *
 * Rules:
 * - Trims leading and trailing whitespace.
 * - Maximum 60 characters (longer names are clamped to 60 characters).
 * - If blank or null, generates a default name: `"<DisplayName> · d MMM yyyy, HH:mm"`.
 * - For renames, empty/blank names are rejected (or fallback/clamped).
 * - Duplicate names are explicitly allowed.
 */
object NameValidator {

    const val MAX_NAME_LENGTH = 60

    /**
     * Generates a localized default name for a reading based on its physical quantity and timestamp.
     * Example: "Magnetic Field · 13 Oct 2026, 14:32"
     *
     * Timestamps are stored as UTC epoch millis and formatted in the device locale/timezone
     * when generating the display-level default name.
     */
    fun generateDefaultName(
        quantity: Quantity,
        timestampEpochMillis: Long,
        timeZone: TimeZone = TimeZone.getDefault(),
        locale: Locale = Locale.getDefault()
    ): String {
        val sdf = SimpleDateFormat("d MMM yyyy, HH:mm", locale).apply {
            this.timeZone = timeZone
        }
        val formattedDate = sdf.format(Date(timestampEpochMillis))
        return "${quantity.displayName} · $formattedDate"
    }

    /**
     * Sanitizes user input or falls back to the default name:
     * - Trims whitespace
     * - If empty/blank, returns default name
     * - If exceeds 60 characters, truncates to 60 characters
     */
    fun sanitizeOrGenerate(
        inputName: String?,
        quantity: Quantity,
        timestampEpochMillis: Long,
        timeZone: TimeZone = TimeZone.getDefault(),
        locale: Locale = Locale.getDefault()
    ): String {
        val trimmed = inputName?.trim().orEmpty()
        if (trimmed.isEmpty()) {
            return generateDefaultName(quantity, timestampEpochMillis, timeZone, locale)
        }
        return if (trimmed.length > MAX_NAME_LENGTH) {
            trimmed.substring(0, MAX_NAME_LENGTH)
        } else {
            trimmed
        }
    }

    /**
     * Validates and normalizes a new name during a rename operation.
     *
     * @throws IllegalArgumentException if the provided name is blank.
     */
    fun validateForRename(inputName: String): String {
        val trimmed = inputName.trim()
        require(trimmed.isNotEmpty()) { "Reading name cannot be empty or blank." }
        return if (trimmed.length > MAX_NAME_LENGTH) {
            trimmed.substring(0, MAX_NAME_LENGTH)
        } else {
            trimmed
        }
    }
}
