package net.veierland.aix;

import java.io.File;

import net.veierland.aix.AixProvider.AixSettingsColumns;
import net.veierland.aix.AixProvider.AixViews;
import net.veierland.aix.AixProvider.AixWidgetSettings;
import net.veierland.aix.AixProvider.AixWidgets;
import net.veierland.aix.AixProvider.AixWidgetsColumns;
import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;

public class AixService extends IntentService {
	
	private static final String TAG = "AixService";
	
	public final static String APPWIDGET_ID = "appWidgetId";
	
	public final static String ACTION_DELETE_WIDGET = "aix.intent.action.DELETE_WIDGET";
	public final static String ACTION_UPDATE_ALL = "aix.intent.action.UPDATE_ALL";
	public final static String ACTION_UPDATE_WIDGET = "aix.intent.action.UPDATE_WIDGET";
	
	public AixService() {
		super("net.veierland.aix.AixService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d(TAG, "onHandleIntent() " + intent.getAction() + " " + intent.getData());
		
		String action = intent.getAction();
		
		if (action.equals(ACTION_UPDATE_ALL)) {
			AppWidgetManager manager = AppWidgetManager.getInstance(this);
			int[] appWidgetIds = manager.getAppWidgetIds(new ComponentName(this, AixWidget.class));
			for (int appWidgetId : appWidgetIds) {
				Uri widgetUri = ContentUris.withAppendedId(AixWidgets.CONTENT_URI, appWidgetId);
				try {
					AixUpdate aixUpdate = new AixUpdate(this, widgetUri);
					aixUpdate.process();
				} catch (Exception e) {
					Log.d(TAG, "AixUpdate of " + widgetUri + " failed! (" + e.getMessage() + ")");
					e.printStackTrace();
				}
			}
		} else {
			Uri widgetUri = intent.getData();
			if (widgetUri == null) {
				Log.d(TAG, "onHandleIntent() failed: widgetUri is null");
				return;
			}
			int appWidgetId = (int)ContentUris.parseId(widgetUri);
			if (appWidgetId <= 0) {
				Log.d(TAG, "onHandleIntent() (action=" + action + " ) failed: invalid appWidgetId (" + appWidgetId + ")");
				return;
			}
			
			if (action.equals(ACTION_UPDATE_WIDGET)) {
				try {
					AixUpdate aixUpdate = new AixUpdate(this, widgetUri);
					aixUpdate.process();
				} catch (Exception e) {
					Log.d(TAG, "AixUpdate of " + widgetUri + " failed! (" + e.getMessage() + ")");
					e.printStackTrace();
				}
			} else if (action.equals(ACTION_DELETE_WIDGET)) {
				deleteWidget(widgetUri);
			} else {
				Log.d(TAG, "onHandleIntent() called with unhandled action (" + action + ")");
			}
		}
	}
	
	private void deleteWidget(Uri widgetUri) {
		int appWidgetId = (int)ContentUris.parseId(widgetUri);
		// Delete widget and view entries + setting entries from content provider
		ContentResolver resolver = getApplicationContext().getContentResolver();
		Cursor widgetCursor = resolver.query(widgetUri, null, null, null, null);
		
		// Clear draw state
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		Editor editor = settings.edit();
		editor.remove("global_widget_" + appWidgetId);
		editor.remove("global_country_" + appWidgetId);
		editor.commit();
		
		if (widgetCursor != null) {
			Uri viewUri = null;
			if (widgetCursor.moveToFirst()) {
				long viewRowId = widgetCursor.getLong(AixWidgetsColumns.VIEWS_COLUMN);
				if (viewRowId != -1) {
					viewUri = ContentUris.withAppendedId(AixViews.CONTENT_URI, viewRowId);
				}
			}
			widgetCursor.close();
			if (viewUri != null) {
				resolver.delete(viewUri, null, null);
			}
			resolver.delete(widgetUri, null, null);
		}
		
		resolver.delete(AixWidgetSettings.CONTENT_URI, AixSettingsColumns.ROW_ID + '=' + appWidgetId, null);
		
		// Delete all temporary files made for widget
		deleteCacheFiles(getApplicationContext(), appWidgetId);
		deleteTemporaryFiles(getApplicationContext(), appWidgetId);
	}
	
	public static void deleteCacheFiles(Context context, int appWidgetId) {
		File dir = context.getCacheDir();
		File[] files = dir.listFiles();
		String appWidgetIdString = Integer.toString(appWidgetId);
		for (int i = 0; i < files.length; i++) {
			File f = files[i];
			String filename = f.getName();
			if (filename.startsWith("aix")) {
				String[] s = f.getName().split("_");
				if (s.length > 1 && s[1].equals(appWidgetIdString)) {
					f.delete();
				}
			}
		}
	}
	
	public static void deleteCacheFiles(Context context, int appWidgetId, String portraitFileName, String landscapeFileName) {
		File dir = context.getCacheDir();
		File[] files = dir.listFiles();
		String appWidgetIdString = Integer.toString(appWidgetId);
		for (int i = 0; i < files.length; i++) {
			File f = files[i];
			String filename = f.getName();
			if (filename.startsWith("aix") && !filename.equals(portraitFileName) && !filename.equals(landscapeFileName)) {
				String[] s = f.getName().split("_");
				if (s.length > 1 && s[1].equals(appWidgetIdString)) {
					f.delete();
				}
			}
		}
	}
	
	public static void deleteTemporaryFiles(Context context, int appWidgetId) {
		String appWidgetIdString = Integer.toString(appWidgetId);
		String[] files = context.fileList();
		for (int i = 0; i < files.length; i++) {
			if (files[i].startsWith("aix")) {
				String[] s = files[i].split("_");
				if (s.length > 1 && s[1].equals(appWidgetIdString)) {
					context.deleteFile(files[i]);
				}
			}
		}
	}
	
	public static void deleteTemporaryFiles(Context context, int appWidgetId, String portraitFileName, String landscapeFileName) {
		String appWidgetIdString = Integer.toString(appWidgetId);
		String[] files = context.fileList();
		for (int i = 0; i < files.length; i++) {
			if (files[i].startsWith("aix") && !files[i].equals(portraitFileName) && !files[i].equals(landscapeFileName)) {
				String[] s = files[i].split("_");
				if (s.length > 1 && s[1].equals(appWidgetIdString)) {
					context.deleteFile(files[i]);
				}
			}
		}
	}
	
	public static void deleteTemporaryFiles(Context context, String appWidgetId, long updateTime) {
		String[] files = context.fileList();
		for (int i = 0; i < files.length; i++) {
			String fileName = files[i];
			if (fileName != null && fileName.startsWith("aix")) {
				String[] s = fileName.split("_");
				if (s != null && s.length == 4) {
					try {
						long filetime = Long.parseLong(s[2]);
						if (	(s[1].equals(appWidgetId) && filetime < updateTime) ||
								(filetime < updateTime - 6 * DateUtils.HOUR_IN_MILLIS))
						{
							context.deleteFile(fileName);
						}
					} catch (NumberFormatException e) { }
				}
			}
		}
	}

}
