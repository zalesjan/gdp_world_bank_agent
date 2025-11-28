package com.example.gdpagent.ai

import okhttp3.*
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody

class AIAgent(private val apiKey: String) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    private val mapper = jacksonObjectMapper()

    fun askLLM(question: String, schemaHint: String): String {
        val prompt = """
        You are a data assistant. 
        Translate the following question into an SQL query that can be run against a SQLite database.
        Use the schema:
        $schemaHint

        Question: "$question"

        Respond ONLY with raw SQL, nothing else, and add valid executable SQL — no Markdown formatting, no ``` fences, no commentary.
    """.trimIndent()

        val json = mapper.writeValueAsString(
            mapOf(
                "model" to "gpt-3.5-turbo",
                "messages" to listOf(mapOf("role" to "user", "content" to prompt))
            )
        )

        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(RequestBody.create("application/json".toMediaTypeOrNull(), json))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw RuntimeException("LLM request failed: ${response.code}")
            val bodyText = response.body?.string() ?: throw RuntimeException("Empty LLM response")
            val tree = mapper.readTree(bodyText)
            return tree["choices"][0]["message"]["content"].asText().trim()
        }
    }
}