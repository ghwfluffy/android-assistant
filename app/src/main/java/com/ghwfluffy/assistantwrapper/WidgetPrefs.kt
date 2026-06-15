package com.ghwfluffy.assistantwrapper

import android.content.Context

object WidgetPrefs {
    private const val PREFS = "widgets"

    fun saveShortcut(context: Context, appWidgetId: Int, shortcut: AppShortcut) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString("label_$appWidgetId", shortcut.label)
            .putString("path_$appWidgetId", shortcut.path)
            .apply()
    }

    fun shortcut(context: Context, appWidgetId: Int): AppShortcut? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val label = prefs.getString("label_$appWidgetId", null)
        val path = prefs.getString("path_$appWidgetId", null)
        if (label.isNullOrBlank() || path.isNullOrBlank()) {
            return null
        }
        return AppShortcut(label, path)
    }
}

