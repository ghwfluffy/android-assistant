package com.ghwfluffy.assistantwrapper

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class AppShortcutWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id ->
            updateWidget(context, appWidgetManager, id)
        }
    }

    companion object {
        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val shortcut = WidgetPrefs.shortcut(context, appWidgetId)
                ?: MobileConfig.appShortcuts().first()
            val intent = Intent(context, MainActivity::class.java).apply {
                putExtra(MobileConfig.EXTRA_START_PATH, shortcut.path)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val views = RemoteViews(context.packageName, R.layout.widget_app_shortcut).apply {
                setTextViewText(R.id.widget_label, shortcut.label)
                setOnClickPendingIntent(R.id.widget_label, pendingIntent)
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}

