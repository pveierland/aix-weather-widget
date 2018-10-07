package net.veierland.aix;

import net.veierland.aix.AixProvider.AixWidgets;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;

public class AixWidget extends AppWidgetProvider {

	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		for (int appWidgetId : appWidgetIds) {
			Intent updateIntent = new Intent(
					AixService.ACTION_DELETE_WIDGET,
					ContentUris.withAppendedId(AixWidgets.CONTENT_URI, appWidgetId),
					context, AixService.class);
			AixService.enqueueWork(context, updateIntent);
		}
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds)
	{
		if (appWidgetIds == null) {
			appWidgetIds = appWidgetManager.getAppWidgetIds(
					new ComponentName(context, AixWidget.class));
		}
		for (int appWidgetId : appWidgetIds) {
			Intent updateIntent = new Intent(
					AixService.ACTION_UPDATE_WIDGET,
					ContentUris.withAppendedId(AixWidgets.CONTENT_URI, appWidgetId),
					context, AixService.class);
			AixService.enqueueWork(context, updateIntent);
		}
	}
	
}
