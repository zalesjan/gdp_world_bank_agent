package com.example.gdpagent.util

import com.example.gdpagent.model.CountryRecord
import com.opencsv.CSVReader
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

object CsvLoader {

    // Skip aggregate "regions" like OECD, World, etc.
    private val nonCountries = listOf(
        "Africa Eastern and Southern", "Africa Western and Central", "blend", "developed",
        "countries", "conflict", "world", "income", "members", "region", "area", "total",
        "union", "latin", "demographic", "only", "europe", "asia", "america",
        "middle east", "sahara"
    )

    fun loadCsv(path: String, indicatorName: String): List<CountryRecord> {
        val file = File(path)
        if (!file.exists()) {
            println("⚠️ File not found: ${file.absolutePath}")
            return emptyList()
        }

        println("📖 Loading $indicatorName data from ${file.name} ...")

        val lines = file.readLines(StandardCharsets.UTF_8)

        // find the header dynamically
        val headerLineIndex = lines.indexOfFirst { it.contains("Country Name") }
        if (headerLineIndex == -1) {
            println("⚠️ Could not find header line in ${file.name}")
            return emptyList()
        }

        val cleanLines = lines.drop(headerLineIndex)
        if (cleanLines.size <= 1) {
            println("⚠️ No valid data rows found in ${file.name}")
            return emptyList()
        }

        // Use OpenCSV for robust CSV parsing (handles quotes/commas)
        val csvText = cleanLines.joinToString("\n")
        val reader = CSVReader(InputStreamReader(csvText.byteInputStream()))
        val allRows = reader.readAll()
        reader.close()

        if (allRows.size <= 1) {
            println("⚠️ No data rows in ${file.name}")
            return emptyList()
        }

        val header = allRows.first()
        val yearColumns = header.drop(4)

        val records = allRows.drop(1).flatMap { row ->
            val country = row.getOrNull(0)?.trim().orEmpty()
            if (country.isBlank() || nonCountries.any { country.lowercase().contains(it) })
                return@flatMap emptyList<CountryRecord>()

            yearColumns.mapIndexedNotNull { i, yearStr ->
                val rawValue = row.getOrNull(i + 4)?.trim().orEmpty()
                if (rawValue.isBlank()) return@mapIndexedNotNull null

                // clean numeric strings
                val cleaned = rawValue.replace(",", "").replace("%", "")
                val numericValue = cleaned.toDoubleOrNull() ?: return@mapIndexedNotNull null

                // keep consistent numeric scaling
                val finalValue = when (indicatorName.lowercase()) {
                    "agriculture" -> numericValue           // percentage
                    "population" -> numericValue            // absolute number
                    "gdp" -> numericValue                   // USD
                    else -> numericValue
                }

                val year = yearStr.toIntOrNull() ?: (1960 + i)
                CountryRecord(country, year, indicatorName, finalValue)
            }
        }

        println("✅ Parsed ${records.size} records for $indicatorName.")
        return records
    }
}
