package com.doitsh.app.data.remote

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DoitshApiClient @Inject constructor() {

    private var client: HttpClient? = null
    private var baseUrl: String? = null

    fun configure(baseUrl: String, authToken: String? = null) {
        this.baseUrl = baseUrl
        client?.close()
        client = HttpClient(Android) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                    isLenient = true
                })
            }
            defaultRequest {
                url(baseUrl)
                contentType(ContentType.Application.Json)
                authToken?.let { header("Authorization", "Bearer $it") }
            }
        }
    }

    suspend fun testConnection(): Boolean {
        return try {
            val response = client?.get("/api/health") ?: return false
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            false
        }
    }

    fun getClient(): HttpClient = client ?: throw IllegalStateException(
        "API client not configured. Call configure() first."
    )

    fun getBaseUrl(): String = baseUrl ?: throw IllegalStateException(
        "Base URL not configured. Call configure() first."
    }

    fun close() {
        client?.close()
        client = null
    }
}
