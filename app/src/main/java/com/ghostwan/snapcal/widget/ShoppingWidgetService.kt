package com.ghostwan.snapcal.widget

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.ghostwan.snapcal.R
import com.ghostwan.snapcal.data.local.AppDatabase
import com.ghostwan.snapcal.data.local.ShoppingItemEntity

class ShoppingWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return ShoppingWidgetFactory(applicationContext)
    }
}

class ShoppingWidgetFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {

    private var items: List<ShoppingItemEntity> = emptyList()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        val dao = AppDatabase.getInstance(context).shoppingItemDao()
        items = dao.getAllSync()
    }

    override fun onDestroy() {
        items = emptyList()
    }

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews {
        val item = items[position]
        val views = RemoteViews(context.packageName, R.layout.widget_shopping_item)

        views.setTextViewText(R.id.widget_shopping_item_name, item.name)

        if (item.isChecked) {
            views.setBoolean(R.id.widget_shopping_item_checkbox, "setChecked", true)
            views.setInt(R.id.widget_shopping_item_name, "setPaintFlags",
                Paint.STRIKE_THRU_TEXT_FLAG or Paint.ANTI_ALIAS_FLAG)
            views.setTextColor(R.id.widget_shopping_item_name, 0xFF999999.toInt())
        } else {
            views.setBoolean(R.id.widget_shopping_item_checkbox, "setChecked", false)
            views.setInt(R.id.widget_shopping_item_name, "setPaintFlags", Paint.ANTI_ALIAS_FLAG)
            views.setTextColor(R.id.widget_shopping_item_name, 0xDE000000.toInt())
        }

        val fillInIntent = Intent().apply {
            putExtra(ShoppingWidgetProvider.EXTRA_ITEM_ID, item.id)
        }
        views.setOnClickFillInIntent(R.id.widget_shopping_item_root, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = items[position].id

    override fun hasStableIds(): Boolean = true
}
