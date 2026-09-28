package com.doitsh.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DoitshApiClient @Inject constructor() {

    private var apiClient: HttpClient? = null
    private var baseUrl: String? = null

    fun configure(baseUrl: String, authToken: String? = null) {
        this.baseUrl = baseUrl
        apiClient?.close()
        apiClient = HttpClient(Android) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                    isLenient = true
                })
            }
            install(DefaultRequest) {
                url(baseUrl)
                contentType(ContentType.Application.Json)
                if (authToken != null) {
                    header("Authorization", "Bearer " + authToken)
                }
            }
        }
    }

    suspend fun testConnection(): Boolean {
        return try {
            val response: HttpResponse = apiClient?.get("/api/health") ?: return false
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            false
        }
    }

    fun getClient(): HttpClient {
        return apiClient ?: throw IllegalStateException("API client not configured")
    }

    fun getBaseUrl(): String {
        return baseUrl ?: throw IllegalStateException("Base URL not configured")
    }

    fun close() {
        apiClient?.close()
        apiClient = null
    }
}
