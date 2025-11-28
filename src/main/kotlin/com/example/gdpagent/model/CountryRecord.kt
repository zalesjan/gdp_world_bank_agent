package com.example.gdpagent.model

data class CountryRecord(
    val country: String,
    val year: Int,
    val indicator: String,   // e.g. GDP, Population
    val value: Double
)
