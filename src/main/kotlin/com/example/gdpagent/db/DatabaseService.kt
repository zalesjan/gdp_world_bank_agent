package com.example.gdpagent.db

import java.sql.Connection
import java.sql.DriverManager
import com.example.gdpagent.model.CountryRecord

class DatabaseService {

    private val connection: Connection = DriverManager.getConnection("jdbc:sqlite::memory:")

    init {
        val stmt = connection.createStatement()
        stmt.execute("PRAGMA foreign_keys = ON;") // enforce relationships

        stmt.execute(
            """
            CREATE TABLE countries (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT UNIQUE NOT NULL,
                iso_code TEXT
            );
            """.trimIndent()
        )

        stmt.execute(
            """
            CREATE TABLE indicators (
                code TEXT PRIMARY KEY,
                description TEXT,
                unit TEXT,
                source TEXT
            );
            """.trimIndent()
        )

        stmt.execute(
            """
            CREATE TABLE economy (
                country_id INTEGER NOT NULL,
                indicator_code TEXT NOT NULL,
                year INTEGER NOT NULL,
                value REAL,
                FOREIGN KEY (country_id) REFERENCES countries(id),
                FOREIGN KEY (indicator_code) REFERENCES indicators(code)
            );
            """.trimIndent()
        )
        stmt.close()

        // Preload indicator definitions
        val indicators = listOf(
            Indicator("GDP", "Gross Domestic Product (current US$)", "US Dollars", "World Bank"),
            Indicator("Population", "Total Population", "People", "World Bank"),
            Indicator("Agriculture", "Agricultural land (% of land area)", "%", "World Bank")

        )
        insertIndicators(indicators)
    }

    data class Indicator(val code: String, val description: String, val unit: String, val source: String)

    private fun insertIndicators(indicators: List<Indicator>) {
        val sql = "INSERT OR IGNORE INTO indicators (code, description, unit, source) VALUES (?, ?, ?, ?)"
        connection.prepareStatement(sql).use { ps ->
            for (ind in indicators) {
                ps.setString(1, ind.code)
                ps.setString(2, ind.description)
                ps.setString(3, ind.unit)
                ps.setString(4, ind.source)
                ps.addBatch()
            }
            ps.executeBatch()
        }
    }

    fun insertAll(records: List<CountryRecord>) {
        val countryMap = mutableMapOf<String, Int>()
        val countryStmt = connection.prepareStatement("INSERT OR IGNORE INTO countries (name) VALUES (?)")
        val getCountryIdStmt = connection.prepareStatement("SELECT id FROM countries WHERE name=?")

        val econStmt = connection.prepareStatement(
            "INSERT INTO economy (country_id, indicator_code, year, value) VALUES (?, ?, ?, ?)"
        )

        for (r in records) {
            val countryId = countryMap.getOrPut(r.country) {
                countryStmt.setString(1, r.country)
                countryStmt.executeUpdate()
                getCountryIdStmt.setString(1, r.country)
                val rs = getCountryIdStmt.executeQuery()
                rs.next()
                val id = rs.getInt("id")
                rs.close()
                id
            }

            econStmt.setInt(1, countryId)
            econStmt.setString(2, r.indicator)
            econStmt.setInt(3, r.year)
            econStmt.setDouble(4, r.value)
            econStmt.addBatch()
        }

        econStmt.executeBatch()
        econStmt.close()
        countryStmt.close()
        getCountryIdStmt.close()
    }

    fun query(sql: String): List<Map<String, Any>> {
        val stmt = connection.createStatement()
        val rs = stmt.executeQuery(sql)
        val meta = rs.metaData
        val columns = (1..meta.columnCount).map { meta.getColumnName(it) }
        val results = mutableListOf<Map<String, Any>>()
        while (rs.next()) {
            val row = columns.associateWith { rs.getObject(it) }
            results.add(row)
        }
        rs.close()
        stmt.close()
        return results
    }
}
