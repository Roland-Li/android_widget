package com.services

import android.widget.RemoteViews
import android.app.PendingIntent
import android.content.Intent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import com.R
import com.activity.VoiceAddActivity
import com.analytics.EventLoggingService
import com.analytics.events.VoiceAddWidgetAnalyticEvents

/*
 *  Used by Android's OS to track the widget creation/removal/updating 
 *  on the user homescreen
 */
class VoiceAddWidgetProvider : AppWidgetProvider() {

  /**
   * Called when the first instance of the widget is added
   * Sends out an analytic event to track when the widget is added
   */
  override fun onEnabled(context: Context?) {
    super.onEnabled(context)

    // Send out an event to track
    EventLoggingService.logEvent(
        context,
        VoiceAddWidgetAnalyticEvents.addWidgetAnalyticEvent(VoiceAddWidgetAnalyticEvents.ADDED)
    )
  }

  /**
   * Called when the last instance of the widget is deleted
   * Sends out an analytic event to track when the widget is removed
  */
  override fun onDisabled(context: Context?) {
    super.onDisabled(context)

    // Send out an event to track
    EventLoggingService.logEvent(
        context,
        VoiceAddWidgetAnalyticEvents.addWidgetAnalyticEvent(VoiceAddWidgetAnalyticEvents.REMOVED)
    )
  }

  /**
   * Called by the OS to redraw the widget
   * Set up view and make it so a click fires a start intent for the activity
   */
  override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
    val N = appWidgetIds.size

    // Perform this loop procedure for each App Widget that belongs to this provider
    for (i in 0 until N) {
      val appWidgetId = appWidgetIds[i]

      val intent = Intent(context, VoiceAddActivity::class.java)
       intent.flags = (
         Intent.FLAG_ACTIVITY_SINGLE_TOP or   // Makes it so there is only a single instance of the activity
         Intent.FLAG_ACTIVITY_MULTIPLE_TASK   // Allows the activity to be wiped from history properly
       )

      val pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)
      val views = RemoteViews(context.packageName, R.layout.widget_voice_add_button)

      // Get the layout for the App Widget and attach an on-click listener to the button
      views.setOnClickPendingIntent(R.id.widget_icon, pendingIntent)

      // Tell the AppWidgetManager to perform an update on the current app widget
      appWidgetManager.updateAppWidget(appWidgetId, views)
    }
  }
}
