package com.ironlog.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [CsvExporter.escape] covering CSV-injection guards. These call the REAL
 * production function (now on the companion object, so no Context is required) — a regression
 * in the escaping logic will actually fail these tests.
 */
class CsvExporterTest {

    private fun escape(value: String, separator: Char = ','): String =
        CsvExporter.escape(value, separator)

    @Test
    fun `plain value is returned as-is`() {
        assertEquals("Bench Press", escape("Bench Press"))
    }

    @Test
    fun `value with comma is quoted`() {
        assertEquals("\"Push, Pull, Legs\"", escape("Push, Pull, Legs"))
    }

    @Test
    fun `embedded quote is escaped and wrapped`() {
        assertEquals("\"He said \"\"go\"\"\"", escape("He said \"go\""))
    }

    @Test
    fun `formula injection equals is neutralized`() {
        assertEquals("'=cmd|'/c calc'!A1", escape("=cmd|'/c calc'!A1"))
    }

    @Test
    fun `formula injection plus is neutralized`() {
        // Value also contains a comma, so after the `'+` prefix the result must be quoted.
        assertEquals("\"'+SUM(1,2)\"", escape("+SUM(1,2)"))
    }

    @Test
    fun `formula injection plus without comma needs no quotes`() {
        assertEquals("'+1", escape("+1"))
    }

    @Test
    fun `formula injection minus is neutralized`() {
        assertEquals("'-1+1", escape("-1+1"))
    }

    @Test
    fun `formula injection at sign is neutralized`() {
        assertEquals("'@import", escape("@import"))
    }

    @Test
    fun `formula injection plus comma both apply`() {
        // After neutralization the value contains a comma → must be wrapped in quotes too.
        assertEquals("\"'+1,2\"", escape("+1,2"))
    }

    @Test
    fun `semicolon separator quotes semicolon but not comma`() {
        assertEquals("a,b", escape("a,b", separator = ';'))
        assertEquals("\"a;b\"", escape("a;b", separator = ';'))
    }

    @Test
    fun `empty string is preserved`() {
        assertEquals("", escape(""))
    }

    @Test
    fun `newline forces quoting`() {
        assertEquals("\"a\nb\"", escape("a\nb"))
        assertEquals("\"a\rb\"", escape("a\rb"))
    }
}
