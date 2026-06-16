package com.ghwfluffy.assistantwrapper

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

data class VoicePromptResult(
    val transcript: String,
    val responseText: String?,
    val runId: String?,
    val status: String?,
    val selectedAction: String?,
    val toolStatus: String?,
    val failureMessage: String?
) {
    fun displayText(): String {
        if (!failureMessage.isNullOrBlank()) {
            return "I could not complete that: $failureMessage"
        }
        if (!responseText.isNullOrBlank()) {
            return responseText
        }
        if (!selectedAction.isNullOrBlank() && status == "completed") {
            return "Done. I completed ${selectedAction.replace('_', ' ')}."
        }
        if (!status.isNullOrBlank()) {
            return "The voice request finished with status $status."
        }
        return "No response text returned."
    }
}

object MobileHttpClient {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(WebViewCookieJar())
        .connectTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .readTimeout(600, TimeUnit.SECONDS)
        .callTimeout(650, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun warmAgentSession(): Boolean {
        if (isAgentAuthenticated()) {
            return true
        }
        val loginRequest = Request.Builder()
            .url(MobileConfig.agentApi("/auth/login"))
            .get()
            .header("Accept", "text/html,application/json")
            .build()
        client.newCall(loginRequest).execute().use { response ->
            response.body?.string()
        }
        return isAgentAuthenticated()
    }

    fun sendVoicePrompt(audioFile: File, recentContext: String): VoicePromptResult {
        val audioBody = audioFile.asRequestBody("audio/mp4".toMediaType())
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("audio", audioFile.name, audioBody)
            .addFormDataPart("recent_context", recentContext)
            .addFormDataPart("source", "android_voice")
            .build()
        val request = Request.Builder()
            .url(MobileConfig.agentApi("/agent/voice-prompts"))
            .post(body)
            .header("Accept", "application/json")
            .build()
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException("Voice prompt failed with HTTP ${response.code}.")
            }
            val json = JSONObject(text)
            return VoicePromptResult(
                transcript = json.cleanString("transcript") ?: "",
                responseText = json.cleanString("responseText"),
                runId = json.cleanString("runId"),
                status = json.cleanString("status"),
                selectedAction = json.cleanString("selectedAction"),
                toolStatus = json.cleanString("toolStatus"),
                failureMessage = json.cleanString("failureMessage")
            )
        }
    }

    private fun isAgentAuthenticated(): Boolean {
        val request = Request.Builder()
            .url(MobileConfig.agentApi("/auth/me"))
            .get()
            .header("Accept", "application/json")
            .build()
        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@use false
                }
                val json = JSONObject(response.body?.string().orEmpty())
                json.optBoolean("authenticated", false)
            }
        }.getOrDefault(false)
    }
}

private fun JSONObject.cleanString(name: String): String? {
    if (!has(name) || isNull(name)) {
        return null
    }
    return optString(name)
        .trim()
        .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
}
