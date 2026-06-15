package com.ghwfluffy.assistantwrapper

import org.json.JSONArray

data class AppShortcut(val label: String, val path: String)

object MobileConfig {
    const val EXTRA_START_PATH = "start_path"

    val siteOrigin: String = BuildConfig.SITE_ORIGIN.trim().trimEnd('/')
    val authBasePath: String = normalizePath(BuildConfig.AUTH_BASE_PATH)
    val agentBasePath: String = normalizePath(BuildConfig.AGENT_BASE_PATH)
    val defaultStartPath: String = normalizePath(BuildConfig.DEFAULT_START_PATH.ifBlank { authBasePath })

    fun urlForPath(path: String): String {
        val trimmed = path.trim()
        if (trimmed.startsWith("https://")) {
            return trimmed
        }
        return siteOrigin + normalizePath(trimmed)
    }

    fun agentApi(path: String): String {
        return siteOrigin + agentBasePath + "/api/v1" + normalizePath(path)
    }

    fun appShortcuts(): List<AppShortcut> {
        val parsed = runCatching { JSONArray(BuildConfig.APP_SHORTCUTS_JSON) }.getOrNull() ?: return listOf(
            AppShortcut("Directory", authBasePath)
        )
        val shortcuts = mutableListOf<AppShortcut>()
        for (index in 0 until parsed.length()) {
            val item = parsed.optJSONObject(index) ?: continue
            val label = item.optString("label").trim()
            val path = item.optString("path").trim()
            if (label.isNotEmpty() && path.isNotEmpty()) {
                shortcuts += AppShortcut(label, path)
            }
        }
        return shortcuts.ifEmpty { listOf(AppShortcut("Directory", authBasePath)) }
    }

    private fun normalizePath(path: String): String {
        val trimmed = path.trim()
        if (trimmed.isEmpty() || trimmed == "/") {
            return "/"
        }
        val prefixed = if (trimmed.startsWith("/")) trimmed else "/$trimmed"
        return if (prefixed.length > 1) prefixed.trimEnd('/') else prefixed
    }
}

