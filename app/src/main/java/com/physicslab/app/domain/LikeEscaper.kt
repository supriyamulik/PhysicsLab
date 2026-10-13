package com.physicslab.app.domain

/**
 * Utility for escaping special characters in SQLite `LIKE ... ESCAPE '\'` clauses.
 *
 * In SQLite `LIKE` queries:
 * - `%` matches any sequence of zero or more characters.
 * - `_` matches any single character.
 * - `\` is configured as our ESCAPE character.
 *
 * Without proper escaping, user input like "100%" or "sensor_1" or "C:\" alters
 * the wildcard search behavior or causes syntax errors.
 */
object LikeEscaper {

    /**
     * Escapes `\`, `%`, and `_` with a leading backslash.
     * Backslash MUST be replaced first to avoid escaping newly added backslashes.
     */
    fun escape(input: String): String {
        return input
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
    }

    /**
     * Builds a substring pattern `"%<escaped>%"`.
     * Suitable for passing to queries with `LIKE :pattern ESCAPE '\'`.
     */
    fun buildContainsPattern(query: String): String {
        return "%${escape(query)}%"
    }
}
