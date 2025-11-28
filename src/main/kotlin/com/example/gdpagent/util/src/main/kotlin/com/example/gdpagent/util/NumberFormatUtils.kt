package com.example.gdpagent.util

import kotlin.math.ln
import kotlin.math.pow

object NumberFormatUtils {

    fun formatValue(key: String, value: Double?): String {
        if (value == null) return "-"
        val absValue = kotlin.math.abs(value)

        return when {
            // GDP — in billions, millions, thousands
            key.lowercase() == "gdp" -> when {
                absValue >= 1e9 -> "${"%.2f".format(value / 1e9)}B"
                absValue >= 1e6 -> "${"%.2f".format(value / 1e6)}M"
                absValue >= 1e3 -> "${"%.0f".format(value / 1e3)}K"
                else -> "%.0f".format(value)
            }

            // Population — round to millions/thousands
            key.lowercase() == "population" -> when {
                absValue >= 1e6 -> "${"%.2f".format(value / 1e6)}M"
                absValue >= 1e3 -> "${"%.0f".format(value / 1e3)}K"
                else -> "%.0f".format(value)
            }

            // Agriculture — show as percentage with one decimal place
            key.lowercase().contains("agriculture") -> "${"%.1f".format(value)}%"

            // Fallback
            else -> "%.2f".format(value)
        }
    }
}
