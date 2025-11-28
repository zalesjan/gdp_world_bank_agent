package com.example.gdpagent

import com.example.gdpagent.db.DatabaseService
import com.example.gdpagent.util.CsvLoader
import com.example.gdpagent.util.DataDownloader
import com.example.gdpagent.util.DataDownloader.renameWorldBankCSVs
import com.example.gdpagent.ai.AIAgent
import com.example.gdpagent.util.NumberFormatUtils


// 2 functions for number formating and printing of results
fun printResults(results: List<Map<String, Any?>>) {
    results.forEach { row ->
        val formatted = row.entries.joinToString(", ") { (key, value) ->
            val formattedValue = when (value) {
                is Number -> NumberFormatUtils.formatValue(key, value.toDouble())
                else -> value.toString()
            }
            "$key=$formattedValue"
        }
        println("{$formatted}")
    }
}


fun main() {
    DataDownloader.ensureDataFiles()
    println("🌍 Loading GDP and Population data...")

    renameWorldBankCSVs()
    println("✅ File check complete, ready to load data.")

    val gdp = CsvLoader.loadCsv("data/GDP.csv", "GDP")
    println("✅ GDP records: ${gdp.size}")

    val pop = CsvLoader.loadCsv("data/Population.csv", "Population")
    println("✅ Population records: ${pop.size}")

    val agri = CsvLoader.loadCsv("data/Agriculture.csv", "Agriculture")
    println("✅ Agriculture records: ${agri.size}")

    val allRecords = gdp + pop + agri
    println("➡️ Total records to insert: ${allRecords.size}")

    val db = DatabaseService()
    db.insertAll(agri)
    db.insertAll(gdp)
    db.insertAll(pop)

    println("✅ Inserted ${gdp.size + pop.size + agri.size} rows into database.")

    //example_query
    val results = db.query("""
    SELECT c.name AS country, e.year,
           MAX(CASE WHEN e.indicator_code='GDP' THEN e.value END) AS gdp,
           MAX(CASE WHEN e.indicator_code='Population' THEN e.value END) AS population,
           (MAX(CASE WHEN e.indicator_code='GDP' THEN e.value END) /
            MAX(CASE WHEN e.indicator_code='Population' THEN e.value END)) AS gdp_per_capita
    FROM economy e
    JOIN countries c ON e.country_id = c.id
    WHERE e.year = 2020
    GROUP BY c.name, e.year
    ORDER BY gdp_per_capita DESC
    LIMIT 5;
    """.trimIndent())

    println("💡 Example question: Top 5 countries by GDP per capita in 2020:")
    if (results.isEmpty()) {
        println("⚠️ No results found for this query. Try a different year (e.g., 2020 or 2022).")
    } else {
    printResults(results)
    }

    println("💬 Ask a question about GDP and population (or type 'exit'):")
    val apiKey = System.getenv("OPENAI_API_KEY") ?: error("❌ Missing OPENAI_API_KEY")

    val agent = AIAgent(apiKey)

    val schemaHint = """
            Tables:
            - countries(id, name, iso_code)
            - indicators(code, description, unit, source)
            - economy(country_id, indicator_code, year, value)
            Relationships:
            economy.country_id → countries.id
            economy.indicator_code → indicators.code
              Notes:
            - Use indicators.code ('GDP', 'Population', 'Agriculture') rather than description in WHERE clauses.
            - Agriculture, GDP and Population data are country-level, by year.
            - 'Agriculture' = percentage of land used for agriculture (% of total land)
            - You can join GDP, Population, and Agriculture by (country_id, year)
            - Example: SELECT ... FROM economy WHERE indicator_code='Agriculture' AND value>20
            """.trimIndent()

    while (true) {
        print("> ")
        val question = readln().trim()
        if (question.lowercase() == "exit") break

        try {
            //val sql =
            //    """SELECT i.code, COUNT(*), MIN(year), MAX(year)
            //FROM economy e
            //JOIN indicators i ON e.indicator_code = i.code
            //GROUP BY i.code;"""

            val sql = agent.askLLM(question, schemaHint)
            println("🤖 Generated SQL:\n$sql\n")

            // clean markdown fences like ```sql ... ```
            val trimsql = sql
                .replace("```sql", "", ignoreCase = true)
                .replace("```", "")
                .trim()

            val results = db.query(trimsql)
            if (results.isEmpty()) {
                println("⚠️ No results found for this query. Try a different year (e.g., 2020 or 2022).")
            } else {
            printResults(results)
            }


            }

        catch (e: Exception) {
            println("❌ Error: ${e.message}") }}}


