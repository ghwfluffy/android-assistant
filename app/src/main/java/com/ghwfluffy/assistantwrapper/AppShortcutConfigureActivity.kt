package com.ghwfluffy.assistantwrapper

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class AppShortcutConfigureActivity : Activity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        renderChoices()
    }

    private fun renderChoices() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 36, 36, 36)
        }
        root.addView(TextView(this).apply {
            text = "Choose widget app"
            textSize = 20f
        })
        MobileConfig.appShortcuts().forEach { shortcut ->
            root.addView(Button(this).apply {
                text = shortcut.label
                setOnClickListener { choose(shortcut) }
            })
        }
        setContentView(root)
    }

    private fun choose(shortcut: AppShortcut) {
        WidgetPrefs.saveShortcut(this, appWidgetId, shortcut)
        val manager = AppWidgetManager.getInstance(this)
        AppShortcutWidgetProvider.updateWidget(this, manager, appWidgetId)
        val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_OK, result)
        finish()
    }
}

