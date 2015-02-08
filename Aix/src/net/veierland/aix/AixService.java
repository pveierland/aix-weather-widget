package net.veierland.aix;

import static net.veierland.aix.AixSettings.CALIBRATION_STATE_FINISHED;
import static net.veierland.aix.AixSettings.CALIBRATION_STATE_VERTICAL;
import static net.veierland.aix.AixSettings.LANDSCAPE_HEIGHT;
import static net.veierland.aix.AixSettings.LANDSCAPE_WIDTH;
import static net.veierland.aix.AixSettings.PORTRAIT_HEIGHT;
import static net.veierland.aix.AixSettings.PORTRAIT_WIDTH;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import net.veierland.aix.AixProvider.AixWidgets;
import net.veierland.aix.util.AixWidgetInfo;
import net.veierland.aix.util.Pair;
import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;

public class AixService extends IntentService {
	
	private static final String TAG = "AixService";
	
	public final static String APPWIDGET_ID = "appWidgetId";
	
	public final static String ACTION_DELETE_WIDGET = "aix.intent.action.DELETE_WIDGET";
	public final static String ACTION_UPDATE_ALL = "aix.intent.action.UPDATE_ALL";
	public final static String ACTION_UPDATE_ALL_MINIMAL_DIMENSIONS = "aix.intent.action.UPDATE_WIDGET_MINIMAL_DIMENSIONS";
	public final static String ACTION_UPDATE_ALL_PROVIDER_AUTO = "aix.intent.action.UPDATE_WIDGET_PROVIDER_AUTO";
	public final static String ACTION_UPDATE_ALL_PROVIDER_CHANGE = "aix.intent.action.UPDATE_WIDGET_PROVIDER_CHANGE";
	public final static String ACTION_UPDATE_WIDGET = "aix.intent.action.UPDATE_WIDGET";
	
	public final static String ACTION_DECREASE_LANDSCAPE_HEIGHT = "aix.intent.action.DECREASE_LANDSCAPE_HEIGHT";
	public final static String ACTION_DECREASE_LANDSCAPE_WIDTH = "aix.intent.action.DECREASE_LANDSCAPE_WIDTH";
	public final static String ACTION_DECREASE_PORTRAIT_HEIGHT = "aix.intent.action.DECREASE_PORTRAIT_HEIGHT";
	public final static String ACTION_DECREASE_PORTRAIT_WIDTH = "aix.intent.action.DECREASE_PORTRAIT_WIDTH";
	public final static String ACTION_INCREASE_LANDSCAPE_HEIGHT = "aix.intent.action.INCREASE_LANDSCAPE_HEIGHT";
	public final static String ACTION_INCREASE_LANDSCAPE_WIDTH = "aix.intent.action.INCREASE_LANDSCAPE_WIDTH";
	public final static String ACTION_INCREASE_PORTRAIT_HEIGHT = "aix.intent.action.INCREASE_PORTRAIT_HEIGHT";
	public final static String ACTION_INCREASE_PORTRAIT_WIDTH = "aix.intent.action.INCREASE_PORTRAIT_WIDTH";
	
	public final static String ACTION_ACCEPT_PORTRAIT_HORIZONTAL_CALIBRATION = "aix.intent.action.ACCEPT_PORTRAIT_HORIZONTAL_CALIBRATION";
	public final static String ACTION_ACCEPT_PORTRAIT_VERTICAL_CALIBRATION = "aix.intent.action.ACCEPT_PORTRAIT_VERTICAL_CALIBRATION";
	public final static String ACTION_ACCEPT_LANDSCAPE_HORIZONTAL_CALIBRATION = "aix.intent.action.ACCEPT_LANDSCAPE_HORIZONTAL_CALIBRATION";
	public final static String ACTION_ACCEPT_LANDSCAPE_VERTICAL_CALIBRATION = "aix.intent.action.ACCEPT_LANDSCAPE_VERTICAL_CALIBRATION";
	
	@SuppressWarnings("serial")
	Map<String, Pair<String, Integer>> mCalibrationAdjustmentsMap = new HashMap<String, Pair<String, Integer>>() {{
		put(ACTION_DECREASE_LANDSCAPE_HEIGHT, new Pair<String, Integer>(LANDSCAPE_HEIGHT, -1));
		put(ACTION_INCREASE_LANDSCAPE_HEIGHT, new Pair<String, Integer>(LANDSCAPE_HEIGHT, +1));
		put(ACTION_DECREASE_LANDSCAPE_WIDTH,  new Pair<String, Integer>(LANDSCAPE_WIDTH,  -1));
		put(ACTION_INCREASE_LANDSCAPE_WIDTH,  new Pair<String, Integer>(LANDSCAPE_WIDTH,  +1));
		put(ACTION_DECREASE_PORTRAIT_HEIGHT,  new Pair<String, Integer>(PORTRAIT_HEIGHT,  -1));
		put(ACTION_INCREASE_PORTRAIT_HEIGHT,  new Pair<String, Integer>(PORTRAIT_HEIGHT,  +1));
		put(ACTION_DECREASE_PORTRAIT_WIDTH,   new Pair<String, Integer>(PORTRAIT_WIDTH,   -1));
		put(ACTION_INCREASE_PORTRAIT_WIDTH,   new Pair<String, Integer>(PORTRAIT_WIDTH,   +1));
	}};
	
	@SuppressWarnings("serial")
	Map<String, String> mCalibrationAcceptActionsMap = new HashMap<String, String>() {{
		put(ACTION_ACCEPT_PORTRAIT_HORIZONTAL_CALIBRATION, PORTRAIT_WIDTH);
		put(ACTION_ACCEPT_PORTRAIT_VERTICAL_CALIBRATION, PORTRAIT_HEIGHT);
		put(ACTION_ACCEPT_LANDSCAPE_HORIZONTAL_CALIBRATION, LANDSCAPE_WIDTH);
		put(ACTION_ACCEPT_LANDSCAPE_VERTICAL_CALIBRATION, LANDSCAPE_HEIGHT);
	}};
	
	public AixService() {
		super("net.veierland.aix.AixService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d(TAG, "onHandleIntent() " + intent.getAction() + " " + intent.getData());
		
		String action = intent.getAction();
		Uri widgetUri = intent.getData();
		
		if (	action.equals(ACTION_UPDATE_WIDGET) ||
				mCalibrationAdjustmentsMap.containsKey(action) ||
				mCalibrationAcceptActionsMap.containsKey(action))
		{
			updateWidget(action, widgetUri);
		}
		else if (action.equals(ACTION_UPDATE_ALL)) {
			updateAllWidgets(widgetUri);
		}
		else if (action.equals(ACTION_UPDATE_ALL_PROVIDER_CHANGE))
		{
			AixUtils.clearProviderData(getContentResolver());
			updateAllWidgets(widgetUri);
		}
		else if (action.equals(ACTION_UPDATE_ALL_PROVIDER_AUTO))
		{
			SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
			Editor editor = sharedPreferences.edit();
			editor.putInt(getString(R.string.provider_string), AixUtils.PROVIDER_AUTO);
			editor.commit();
			
			AixUtils.clearProviderData(getContentResolver());
			
			updateAllWidgets(widgetUri);
		}
		else if (action.equals(ACTION_UPDATE_ALL_MINIMAL_DIMENSIONS))
		{
			SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
			Editor editor = sharedPreferences.edit();
			editor.putBoolean(getString(R.string.useDeviceSpecificDimensions_bool), false);
			editor.commit();
			action = ACTION_UPDATE_WIDGET;
			
			updateAllWidgets(widgetUri);
		}
		else if (action.equals(ACTION_DELETE_WIDGET))
		{
			int appWidgetId = (int)ContentUris.parseId(widgetUri);
			AixUtils.deleteWidget(this, appWidgetId);
		}
		else {
			Log.d(TAG, "onHandleIntent() called with unhandled action (" + action + ")");
		}
	}
	
	private void updateWidget(String action, Uri widgetUri)
	{
		int appWidgetId = (int)ContentUris.parseId(widgetUri);
		
		AixWidgetInfo widgetInfo = null;
		try {
			widgetInfo = AixWidgetInfo.build(this, widgetUri);
			widgetInfo.loadSettings(this);
		} catch (Exception e) {
			PendingIntent pendingIntent = AixUtils.buildConfigurationIntent(this, widgetUri);
			AixUtils.updateWidgetRemoteViews(this, appWidgetId, "Failed to get widget information", true, pendingIntent);
			Log.d(TAG, "onHandleIntent() failed: Could not retrieve widget information (" + e.getMessage() + ")");
			return;
		}
		
		AixSettings aixSettings = AixSettings.build(this, widgetInfo);
		aixSettings.loadSettings();
		
		int calibrationTarget = aixSettings.getCalibrationTarget();
		
		if (calibrationTarget == widgetInfo.getAppWidgetId()) {
			calibrationMethod(widgetInfo, aixSettings, action);
		} else {
			try {
				AixUpdate aixUpdate = AixUpdate.build(this, widgetInfo, aixSettings);
				aixUpdate.process();
			} catch (Exception e) {
				PendingIntent pendingIntent = AixUtils.buildConfigurationIntent(this, widgetUri);
				AixUtils.updateWidgetRemoteViews(this, appWidgetId, "Failed to update widget", true, pendingIntent);
				Log.d(TAG, "AixUpdate of " + widgetUri + " failed! (" + e.getMessage() + ")");
				e.printStackTrace();
			}
		}
	}
	
	private void updateAllWidgets(Uri widgetUri)
	{
		AixSettings.clearAllWidgetStates(PreferenceManager.getDefaultSharedPreferences(this));
		
		// Update all widgets except widgetUri
		int widgetIdExclude = AppWidgetManager.INVALID_APPWIDGET_ID;
		if (widgetUri != null)
		{
			widgetIdExclude = (int)ContentUris.parseId(widgetUri);
			
			
			updateWidget(ACTION_UPDATE_WIDGET, widgetUri);
		}
		
		AppWidgetManager manager = AppWidgetManager.getInstance(this);
		int[] appWidgetIds = manager.getAppWidgetIds(new ComponentName(this, AixWidget.class));
		for (int appWidgetId : appWidgetIds) {
			if (appWidgetId != widgetIdExclude)
			{
				Intent updateIntent = new Intent(
						ACTION_UPDATE_WIDGET,
						ContentUris.withAppendedId(AixWidgets.CONTENT_URI, appWidgetId),
						this, AixService.class);
				startService(updateIntent);
			}
		}
	}
	
	private void calibrationMethod(AixWidgetInfo widgetInfo, AixSettings aixSettings, String action) {
		Log.d(TAG, "calibrationMethod() " + action);
		
		if (mCalibrationAcceptActionsMap.containsKey(action)) {
			String property = mCalibrationAcceptActionsMap.get(action);
			
			aixSettings.saveCalibratedDimension(property);
			
			if (	action.equals(ACTION_ACCEPT_PORTRAIT_HORIZONTAL_CALIBRATION) ||
					action.equals(ACTION_ACCEPT_LANDSCAPE_HORIZONTAL_CALIBRATION))
			{
				aixSettings.setCalibrationState(CALIBRATION_STATE_VERTICAL);
				
				// Update relevant widget only, still calibrating
				Intent updateIntent = new Intent(ACTION_UPDATE_WIDGET, widgetInfo.getWidgetUri(), this, AixService.class);
				startService(updateIntent);
			} else {
				aixSettings.setCalibrationState(CALIBRATION_STATE_FINISHED);
				aixSettings.exitCalibrationMode();
				
				PendingIntent pendingIntent = AixUtils.buildConfigurationIntent(this, widgetInfo.getWidgetUri());
				AixUtils.updateWidgetRemoteViews(this, widgetInfo.getAppWidgetId(), getString(R.string.widget_loading), true, pendingIntent);
				
				// Update all widgets after ended calibration
				Intent updateIntent = new Intent(ACTION_UPDATE_ALL, widgetInfo.getWidgetUri(), this, AixService.class);
				startService(updateIntent);
			}
		} else {
			Pair<String, Integer> adjustParams = mCalibrationAdjustmentsMap.get(action);
			
			if (adjustParams != null) {
				aixSettings.adjustCalibrationDimension(adjustParams.first, adjustParams.second);
			}

			try {
				setupCalibrationWidget(widgetInfo, aixSettings);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private Bitmap renderCalibrationBitmap(int width, int height, boolean vertical) {
		Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		canvas.drawColor(Color.WHITE);
		
		Paint p = new Paint() {{
			setColor(Color.BLACK);
			setStrokeWidth(0);
		}};
		
		if (vertical) {
			for (int y = 0; y < height; y += 2) {
				canvas.drawLine(0.0f, (float)y, (float)width, (float)y, p);
			}
		} else {
			for (int x = 0; x < width; x += 2) {
				canvas.drawLine((float)x, 0.0f, (float)x, (float)height, p);
			}
		}
		
		return bitmap;
	}
	
	private void setupCalibrationWidget(AixWidgetInfo aixWidgetInfo, AixSettings aixSettings) throws IOException {
		int calibrationState = aixSettings.getCalibrationState();
		boolean vertical = (calibrationState == AixSettings.CALIBRATION_STATE_VERTICAL);
		
		int appWidgetId = aixWidgetInfo.getAppWidgetId();
		Uri widgetUri = aixWidgetInfo.getWidgetUri();
		
		Point portraitDimensions = aixSettings.getCalibrationPixelDimensionsOrStandard(false);
		Point landscapeDimensions = aixSettings.getCalibrationPixelDimensionsOrStandard(true);
		
		Bitmap portraitBitmap = renderCalibrationBitmap(portraitDimensions.x, portraitDimensions.y, vertical);
		Bitmap landscapeBitmap = renderCalibrationBitmap(landscapeDimensions.x, landscapeDimensions.y, vertical);
		
		long now = System.currentTimeMillis();
		
		Uri portraitUri = AixUtils.storeBitmap(this, portraitBitmap, appWidgetId, now, false);
		Uri landscapeUri = AixUtils.storeBitmap(this, landscapeBitmap, appWidgetId, now, true);
		
		RemoteViews updateView = new RemoteViews(getPackageName(), R.layout.aix_calibrate);
		
		setupPendingIntent(updateView, widgetUri, R.id.landscape_decrease, vertical ? ACTION_DECREASE_LANDSCAPE_HEIGHT : ACTION_DECREASE_LANDSCAPE_WIDTH);
		setupPendingIntent(updateView, widgetUri, R.id.landscape_increase, vertical ? ACTION_INCREASE_LANDSCAPE_HEIGHT : ACTION_INCREASE_LANDSCAPE_WIDTH);
		setupPendingIntent(updateView, widgetUri, R.id.portrait_decrease,  vertical ? ACTION_DECREASE_PORTRAIT_HEIGHT  : ACTION_DECREASE_PORTRAIT_WIDTH);
		setupPendingIntent(updateView, widgetUri, R.id.portrait_increase,  vertical ? ACTION_INCREASE_PORTRAIT_HEIGHT  : ACTION_INCREASE_PORTRAIT_WIDTH);
		
		setupPendingIntent(updateView, widgetUri, R.id.landscape_accept,   vertical ? ACTION_ACCEPT_LANDSCAPE_VERTICAL_CALIBRATION : ACTION_ACCEPT_LANDSCAPE_HORIZONTAL_CALIBRATION);
		setupPendingIntent(updateView, widgetUri, R.id.portrait_accept,    vertical ? ACTION_ACCEPT_PORTRAIT_VERTICAL_CALIBRATION  : ACTION_ACCEPT_PORTRAIT_HORIZONTAL_CALIBRATION);
		
		updateView.setTextViewText(R.id.portraitText, "Portrait/" + (vertical ? "Vertical" : "Horizontal") + "\n" + portraitDimensions.x + "x" + portraitDimensions.y);
		updateView.setTextViewText(R.id.landscapeText, "Landscape/" + (vertical ? "Vertical" : "Horizontal") + "\n" + landscapeDimensions.x + "x" + landscapeDimensions.y);
		
		updateView.setImageViewUri(R.id.landscapeCalibrationImage, landscapeUri);
		updateView.setImageViewUri(R.id.portraitCalibrationImage, portraitUri);
		
		AppWidgetManager.getInstance(this).updateAppWidget(appWidgetId, updateView);
	}
	
	private void setupPendingIntent(RemoteViews remoteViews, Uri widgetUri, int resource, String action) {
		Intent intent = new Intent(action, widgetUri, this, AixService.class);
		remoteViews.setOnClickPendingIntent(resource, PendingIntent.getService(this, 0, intent, 0));
	}

}
