package com.frostlock.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context

/** Lets the user pin a FrostLock widget without hunting through the launcher picker. */
object WidgetPinning {

    fun isSupported(context: Context): Boolean =
        AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported

    /** Shows the launcher's confirmation sheet for [provider]. Returns false if unsupported. */
    fun request(context: Context, provider: Class<out AppWidgetProvider>): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        if (!manager.isRequestPinAppWidgetSupported) return false
        return manager.requestPinAppWidget(ComponentName(context, provider), null, null)
    }
}
