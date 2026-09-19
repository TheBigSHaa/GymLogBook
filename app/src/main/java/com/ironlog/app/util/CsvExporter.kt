package com.ironlog.app.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.ironlog.app.ui.log.SessionWithDetails
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.DecimalFormatSymbols
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CsvExporter @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun exportWorkoutHistory(sessions: List<SessionWithDetails>): Result<File> {
        return runCatching {
            val dateStamp = java.time.LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
            val file = File(context.cacheDir, "proyou_export_$dateStamp.csv")

            // If the locale's decimal separator is a comma, Excel/Sheets will mis-parse a
            // comma-delimited file; switch to semicolon so users in DE/FR/HE locales get
            // correctly-aligned columns.
            val locale = Locale.getDefault()
            val separator = if (DecimalFormatSymbols.getInstance(locale).decimalSeparator == ',') ';' else ','

            file.outputStream().use { out ->
                // BOM so Excel opens UTF-8 reliably.
                out.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                out.bufferedWriter(Charsets.UTF_8).use { writer ->
                    val header = listOf(
                        "Date", "Workout Day", "Exercise", "Set",
                        "Weight (kg)", "Reps", "RPE", "Rest (s)", "Warmup", "Rep Unit",
                    ).joinToString(separator.toString())
                    writer.appendLine(header)

                    val sorted = sessions.sortedWith(
                        compareBy<SessionWithDetails> { it.session.date }.thenBy { it.session.id },
                    )
                    sorted.forEach { s ->
                        val date = s.session.date.toString()
                        val dayName = s.workoutDay.name
                        s.exercises.forEach { ex ->
                            val exName = ex.exercise.name
                            val repUnit = ex.exercise.repUnit.name.lowercase()
                            ex.sets.forEachIndexed { idx, swp ->
                                val set = swp.set
                                val setNum = set.setNumber.takeIf { it > 0 } ?: (idx + 1)
                                val rpe = set.rpe?.toString().orEmpty()
                                val rest = set.restSeconds?.toString().orEmpty()
                                val row = listOf(
                                    date,
                                    escape(dayName, separator),
                                    escape(exName, separator),
                                    setNum.toString(),
                                    set.weight.toString(),
                                    set.reps.toString(),
                                    rpe,
                                    rest,
                                    set.isWarmup.toString(),
                                    repUnit,
                                ).joinToString(separator.toString())
                                writer.appendLine(row)
                            }
                        }
                    }
                }
            }
            file
        }
    }

    fun fileToShareUri(file: File): Uri {
        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(context, authority, file)
    }

    companion object {
        private val DANGEROUS_PREFIXES = setOf('=', '+', '-', '@')

        /**
         * Quotes cells that contain the field separator, quotes, or newlines.
         * Additionally prefixes a leading `=`, `+`, `-`, or `@` with a single quote so that
         * Excel/Sheets treat the value as text rather than executing it as a formula
         * (CSV injection / "formula injection").
         *
         * On the companion (context-free) so unit tests exercise the REAL implementation
         * rather than a mirror copy.
         */
        internal fun escape(value: String, separator: Char = ','): String {
            val neutralized = if (value.isNotEmpty() && value.first() in DANGEROUS_PREFIXES) {
                "'$value"
            } else {
                value
            }
            val needsQuotes =
                neutralized.contains(separator) ||
                    neutralized.contains('"') ||
                    neutralized.contains('\n') ||
                    neutralized.contains('\r')
            if (!needsQuotes) return neutralized
            return "\"${neutralized.replace("\"", "\"\"")}\""
        }
    }
}
