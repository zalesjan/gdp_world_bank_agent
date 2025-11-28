package com.example.gdpagent.util

import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

object DataDownloader {

    private val DATA_DIR = File("data")

    fun ensureDataFiles() {
        if (!DATA_DIR.exists()) DATA_DIR.mkdirs()

        // --- Download GDP ---
        downloadAndExtract(
            "https://api.worldbank.org/v2/en/indicator/NY.GDP.MKTP.CD?downloadformat=csv",
            "GDP.zip"
        )

        // --- Download Population ---
        downloadAndExtract(
            "https://api.worldbank.org/v2/en/indicator/SP.POP.TOTL?downloadformat=csv",
            "Population.zip"
        )

        // --- Download Agriculture ---
        downloadAndExtract(
            "https://api.worldbank.org/v2/en/indicator/AG.LND.AGRI.ZS?downloadformat=csv",
            "Agriculture.zip"
        )
    }

    private fun downloadAndExtract(urlStr: String, zipName: String) {
        try {
            val zipFile = File(DATA_DIR, zipName)
            if (zipFile.exists()) {
                println("📦 $zipName already exists, skipping download.")
            } else {
                println("🌾 Downloading $zipName from $urlStr")
                val url = URL(urlStr)
                val conn = url.openConnection() as HttpURLConnection
                conn.instanceFollowRedirects = true
                conn.connectTimeout = 15000
                conn.readTimeout = 30000

                conn.inputStream.use { input ->
                    FileOutputStream(zipFile).use { output ->
                        input.copyTo(output)
                    }
                }
                println("✅ Downloaded $zipName")
            }

            // --- Extract ---
            if (zipFile.exists()) {
                println("📂 Extracting $zipName...")
                ZipInputStream(FileInputStream(zipFile)).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) {
                            val outFile = File(DATA_DIR, entry.name.substringAfterLast('/'))
                            FileOutputStream(outFile).use { output ->
                                zip.copyTo(output)
                            }
                        }
                        entry = zip.nextEntry
                    }
                }
                println("✅ Extracted ${zipFile.name} to ${DATA_DIR.absolutePath}")
            } else {
                println("⚠️ ZIP file not found after download: $zipName")
            }

        } catch (e: Exception) {
            println("❌ Failed to download or extract $zipName: ${e.message}")
        }
    }

    // --- Helper to rename downloaded CSVs ---
    fun renameWorldBankCSVs() {
        val dataDir = File("data")
        if (!dataDir.exists()) return

        val files = dataDir.listFiles() ?: return

        println("➡️ Checking and renaming CSV files...")

        val renameMap = listOf(
            "GDP" to listOf("gdp", "ny.gdp.mktp.cd"),
            "Population" to listOf("pop", "sp.pop.totl"),
            "Agriculture" to listOf("agri", "ag.lnd.agri.zs", "land")
        )

        files.filter { it.extension.equals("csv", ignoreCase = true) }.forEach { file ->
            val name = file.name.lowercase()

            when {
                name.contains("metadata_country") || name.contains("metadata_indicator") -> {
                    println("⚙️ Skipping metadata file: ${file.name}")
                }
                else -> {
                    val match = renameMap.find { (_, patterns) ->
                        patterns.any { name.contains(it) }
                    }
                    if (match != null) {
                        val targetName = "${match.first}.csv"
                        val targetFile = File(dataDir, targetName)
                        if (!targetFile.exists()) {
                            file.renameTo(targetFile)
                            println("🔄 Renamed ${file.name} → ${targetName}")
                        } else {
                            println("✅ Target $targetName already exists, skipping ${file.name}")
                        }
                    }
                }
            }
        }
    }

}
