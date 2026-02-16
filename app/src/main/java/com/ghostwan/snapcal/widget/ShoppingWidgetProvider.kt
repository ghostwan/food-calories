package com.ghostwan.snapcal.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.ghostwan.snapcal.MainActivity
import com.ghostwan.snapcal.R
import com.ghostwan.snapcal.data.local.AppDatabase
import kotlinx.coroutines.runBlocking

class ShoppingWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE_ITEM) {
            val itemId = intent.getLongExtra(EXTRA_ITEM_ID, -1L)
            if (itemId != -1L) {
                val dao = AppDatabase.getInstance(context).shoppingItemDao()
                val items = dao.getAllSync()
                val item = items.find { it.id == itemId }
                if (item != null) {
                    runBlocking { dao.setChecked(itemId, !item.isChecked) }
                }
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val widgetIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, ShoppingWidgetProvider::class.java)
                )
                appWidgetManager.notifyAppWidgetViewDataChanged(widgetIds, R.id.widget_shopping_list)
            }
        }
    }

    companion object {
        const val ACTION_TOGGLE_ITEM = "com.ghostwan.snapcal.widget.TOGGLE_SHOPPING_ITEM"
        const val EXTRA_ITEM_ID = "extra_item_id"

        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_shopping)

            // Set up RemoteViews adapter for ListView
            val serviceIntent = Intent(context, ShoppingWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_shopping_list, serviceIntent)

            // PendingIntent template for item clicks (toggle)
            val toggleIntent = Intent(context, ShoppingWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE_ITEM
            }
            val togglePendingIntent = PendingIntent.getBroadcast(
                context, 0, toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.widget_shopping_list, togglePendingIntent)

            // Header click opens the app on shopping list
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("navigate_to", "shopping")
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context, 1, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_shopping_header, openAppPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val widgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, ShoppingWidgetProvider::class.java)
            )
            for (id in widgetIds) {
                updateWidget(context, appWidgetManager, id)
            }
            appWidgetManager.notifyAppWidgetViewDataChanged(widgetIds, R.id.widget_shopping_list)
        }
    }
}
