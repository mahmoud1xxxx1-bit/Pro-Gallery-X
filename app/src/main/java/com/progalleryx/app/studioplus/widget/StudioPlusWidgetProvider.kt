package com.progalleryx.app.studioplus.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.progalleryx.app.MainActivity
import com.progalleryx.app.R

/**
 * Enterprise-grade Home Screen App Widget for Studio Plus +.
 * Provides 1-tap fast access to Gallery and Creative Studio tools.
 */
class StudioPlusWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, widgetId)
        }
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.studio_plus_widget)

            // Intent to open Pictures tab
            val picturesIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("extra_tab", "PICTURES")
            }
            val picturesPendingIntent = PendingIntent.getActivity(
                context,
                101,
                picturesIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Intent to open Studio + Hub tab
            val toolsIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("extra_tab", "STUDIO_PLUS")
            }
            val toolsPendingIntent = PendingIntent.getActivity(
                context,
                102,
                toolsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Bind actions to widget views
            views.setOnClickPendingIntent(R.id.widget_root, picturesPendingIntent)
            views.setOnClickPendingIntent(R.id.btn_widget_gallery, picturesPendingIntent)
            views.setOnClickPendingIntent(R.id.btn_widget_tools, toolsPendingIntent)

            // Update widget instance
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
