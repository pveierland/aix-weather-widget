package net.veierland.aix;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import net.veierland.aix.data.AixDataUpdateException;
import net.veierland.aix.data.AixGeoNamesData;
import net.veierland.aix.data.AixMetSunTimeData;
import net.veierland.aix.data.AixMetWeatherData;
import net.veierland.aix.data.AixNoaaWeatherData;
import net.veierland.aix.util.AixLocationInfo;
import net.veierland.aix.util.AixViewInfo;
import net.veierland.aix.util.AixWidgetInfo;
import net.veierland.aix.widget.AixDetailedWidget;
import net.veierland.aix.widget.AixWidgetDataException;
import net.veierland.aix.widget.AixWidgetDrawException;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.RemoteViews;

public class AixUpdate {
	
	private final static String TAG = "AixUpdate";
	
	public static final int WIDGET_STATE_MESSAGE = 1;
	public static final int WIDGET_STATE_RENDER = 2;
	
	private long mCurrentUtcTime;
	
	private Context mContext;
	
	private Uri mWidgetUri;
	
	private TimeZone mUtcTimeZone = null;
	
	private AixWidgetInfo mAixWidgetInfo = null;
	private AixSettings mAixSettings = null;
	
	private AixUpdate(Context context, AixWidgetInfo aixWidgetInfo, AixSettings aixSettings) {
		mContext = context;
		mAixWidgetInfo = aixWidgetInfo;
		mWidgetUri = aixWidgetInfo.getWidgetUri();
		mAixSettings = aixSettings;
		
		mUtcTimeZone = TimeZone.getTimeZone("UTC");
	}
	
	public static AixUpdate build(Context context, AixWidgetInfo aixWidgetInfo, AixSettings aixSettings)
	{
		return new AixUpdate(context, aixWidgetInfo, aixSettings);
	}
	
	public void process() throws Exception {
		if (mAixWidgetInfo == null)
		{
			Log.d(TAG, "process(): Failed to start update. Missing widget info.");
			return;
		}
		
		AixViewInfo aixViewInfo = mAixWidgetInfo.getViewInfo();
		if (aixViewInfo == null)
		{
			Log.d(TAG, "process(): Failed to start update. Missing view info.");
			return;
		}
		
		AixLocationInfo aixLocationInfo = aixViewInfo.getLocationInfo();
		if (aixLocationInfo == null)
		{
			Log.d(TAG, "process(): Failed to start update. Missing location info.");
			return;
		}
		
		Log.d(TAG, "process(): Started processing. " + mAixWidgetInfo.toString());
		
		mCurrentUtcTime = Calendar.getInstance(mUtcTimeZone).getTimeInMillis(); 

		final int totalNumAttempts = 3;
		int numAttemptsRemaining = totalNumAttempts;
		boolean updateSuccess = false, drawSuccess = false;

		boolean shouldUpdate = isDataUpdateNeeded(aixLocationInfo);
		boolean isWifiConnectionMissing = false;
		boolean isRateLimited = false;
		
		do {
			if (numAttemptsRemaining != totalNumAttempts) {
				updateWidgetRemoteViews("Delay before attempt #" + (totalNumAttempts - numAttemptsRemaining + 1), false);
				Thread.sleep(10000);
			}
			
			if (shouldUpdate) {
				if (!mAixSettings.getCachedWifiOnly() || isWiFiAvailable()) {
					try {
						updateData(aixLocationInfo);
						updateSuccess = true;
						isWifiConnectionMissing = false;
					}
					catch (AixDataUpdateException e) {
						if (e.reason == AixDataUpdateException.Reason.RATE_LIMITED) {
							isRateLimited = true;
							numAttemptsRemaining = 0;
						}
						Log.d(TAG, String.format("update() failed: %s", e.toString()));
					}
					catch (Exception e) {
						Log.d(TAG, String.format("update() failed: %s", e.toString()));
					}
				} else {
					isWifiConnectionMissing = true;
				}
			}
			
			try {
				AixDetailedWidget widget = AixDetailedWidget.build(mContext, mAixWidgetInfo, aixLocationInfo);
				updateWidgetRemoteViews(widget);
				drawSuccess = true;
			}
			catch (AixWidgetDrawException e) {
				Log.d(TAG, "process(): Failed to draw widget. AixWidgetDrawException=" + e.getMessage());
				e.printStackTrace();
				break;
			}
			catch (AixWidgetDataException e)
			{
				Log.d(TAG, "process(): Failed to draw widget. AixWidgetDataException=" + e.getMessage());
				e.printStackTrace();

				if (updateSuccess)
				{
					updateSuccess = false;
					break;
				}
				else
				{
					shouldUpdate = true;
				}
			}
			catch (Exception e) {
				Log.d(TAG, "process(): Failed to draw widget. Exception=" + e.getMessage());
				e.printStackTrace();
				
				shouldUpdate = true;
			}
		} while (!drawSuccess && (shouldUpdate && !updateSuccess) && (--numAttemptsRemaining > 0));
		
		long updateTime = Long.MAX_VALUE;

		if (shouldUpdate && !updateSuccess) {
			if (mAixSettings.getCachedWifiOnly()) {
				SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
				Editor editor = settings.edit();
				editor.putBoolean("global_needwifi", true);
				editor.commit();

				Log.d(TAG, "WiFi needed, but not connected!");
			}

			clearUpdateTimestamps(aixLocationInfo);
			updateTime = Math.min(updateTime,
				System.currentTimeMillis()
				+ 10 * DateUtils.MINUTE_IN_MILLIS
				+ Math.round((float)(20 * 60) * Math.random()) * DateUtils.SECOND_IN_MILLIS);
		}

		if (!drawSuccess) {
			if (shouldUpdate && !updateSuccess) {
				if (mAixSettings.getCachedProvider() == AixUtils.PROVIDER_NWS && !isLocationInUS(aixLocationInfo)) {
					PendingIntent pendingIntent = AixUtils.buildWidgetProviderAutoIntent(mContext, mWidgetUri);
					AixUtils.updateWidgetRemoteViews(mContext, mAixWidgetInfo.getAppWidgetId(), "NWS source cannot be used outside US.\nTap widget to revert to auto", true, pendingIntent);
				} else if (isRateLimited) {
					updateWidgetRemoteViews("API is currently rate limited", true);
				} else if (isWifiConnectionMissing) {
					updateWidgetRemoteViews("WiFi is required and missing", true);
				} else {
					updateWidgetRemoteViews("Failed to get weather data", true);
				}
			} else {
				if (mAixSettings.getCachedUseSpecificDimensions()) {
					PendingIntent pendingIntent = AixUtils.buildDisableSpecificDimensionsIntent(mContext, mWidgetUri);
					AixUtils.updateWidgetRemoteViews(mContext, mAixWidgetInfo.getAppWidgetId(), "Draw failed!\nTap widget to revert to minimal dimensions", true, pendingIntent);
				} else {
					updateWidgetRemoteViews("Failed to draw widget", true);
				}
			}
		}

		scheduleUpdate(updateTime);
		
		Log.d(TAG, "process() ended!");
	}
	
	private boolean isWiFiAvailable() {
		ConnectivityManager connManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo wifiInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		
		if (wifiInfo != null && wifiInfo.isConnected()) {
			return true;
		} else {
			return false;
		}
	}

	private boolean isLocationInUS(AixLocationInfo locationInfo) {
		String widgetCountryCode = "global_lcountry_" + locationInfo.getId();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
		boolean isUS = settings.getString(widgetCountryCode, "").toLowerCase().equals("us");
		return isUS;
	}
	
	private void scheduleUpdate(long updateTime) {
		Calendar calendar = Calendar.getInstance();
		AixUtils.truncateHour(calendar);
		calendar.add(Calendar.HOUR, 1);
		
		// Add random interval to spread traffic
		calendar.add(Calendar.SECOND, (int)Math.round(180.0f * Math.random()));
		
		updateTime = Math.min(updateTime, calendar.getTimeInMillis());

		Intent updateIntent = new Intent(AixService.ACTION_UPDATE_WIDGET, mWidgetUri, mContext, AixServiceReceiver.class);
		PendingIntent pendingUpdateIntent = PendingIntent.getBroadcast(mContext, 0, updateIntent, 0);
		
		boolean awakeOnly = mAixSettings.getCachedAwakeOnly();
		
		AlarmManager alarmManager = (AlarmManager)mContext.getSystemService(Context.ALARM_SERVICE);
		alarmManager.set(awakeOnly ? AlarmManager.RTC : AlarmManager.RTC_WAKEUP, updateTime, pendingUpdateIntent);
		Log.d(TAG, "Scheduling next update for: " + (new SimpleDateFormat().format(updateTime)) + " AwakeOnly=" + awakeOnly);
	}
	
	private Uri renderWidget(AixDetailedWidget widget, boolean isLandscape) throws AixWidgetDrawException, IOException {		
		Point dimensions;
		
		if (mAixSettings.getCachedUseSpecificDimensions()) {
			dimensions = mAixSettings.getPixelDimensionsOrStandard(isLandscape);
		} else {
			dimensions = mAixSettings.getStandardPixelDimensions(mAixWidgetInfo.getNumColumns(), mAixWidgetInfo.getNumRows(), isLandscape, true);
		}

		Bitmap bitmap = widget.render(dimensions.x, dimensions.y, isLandscape);
		return AixUtils.storeBitmap(mContext, bitmap, mAixWidgetInfo.getAppWidgetId(), mCurrentUtcTime, isLandscape);
	}
	
	private void updateWidgetRemoteViews(AixDetailedWidget aixDetailedWidget) throws AixWidgetDrawException, IOException
	{
		int orientationMode = mAixSettings.getCachedOrientationMode();
		
		RemoteViews updateView = null;
		
		if (orientationMode == AixUtils.ORIENTATION_PORTRAIT_FIXED) {
			Uri portraitUri = renderWidget(aixDetailedWidget, false);
			updateView = new RemoteViews(mContext.getPackageName(),
					mAixSettings.getCachedUseSpecificDimensions()
					? R.layout.widget_custom_fixed
					: R.layout.widget_large_tiny_portrait);
			updateView.setImageViewUri(R.id.widgetImage, portraitUri);
			Log.d(TAG, "Updating portrait mode");
		}
		else if (orientationMode == AixUtils.ORIENTATION_LANDSCAPE_FIXED)
		{
			Uri landscapeUri = renderWidget(aixDetailedWidget, true);
			updateView = new RemoteViews(mContext.getPackageName(),
					mAixSettings.getCachedUseSpecificDimensions()
					? R.layout.widget_custom_fixed
					: R.layout.widget_large_tiny_landscape);
			updateView.setImageViewUri(R.id.widgetImage, landscapeUri);
			Log.d(TAG, "Updating landscape mode");
		}
		else
		{
			Uri portraitUri = renderWidget(aixDetailedWidget, false);
			Uri landscapeUri = renderWidget(aixDetailedWidget, true);
			
			updateView = new RemoteViews(mContext.getPackageName(),
					mAixSettings.getCachedUseSpecificDimensions()
					? R.layout.widget_custom
					: R.layout.widget_large_tiny);
			
			updateView.setImageViewUri(R.id.widgetImagePortrait, portraitUri);
			updateView.setImageViewUri(R.id.widgetImageLandscape, landscapeUri);
			
			Log.d(TAG, "Updating both portrait and landscape mode");
		}
		
		mAixSettings.setWidgetState(WIDGET_STATE_RENDER);
		
		PendingIntent configurationIntent = AixUtils.buildConfigurationIntent(mContext, mAixWidgetInfo.getWidgetUri());
		updateView.setOnClickPendingIntent(R.id.widgetContainer, configurationIntent);
		AppWidgetManager.getInstance(mContext).updateAppWidget(mAixWidgetInfo.getAppWidgetId(), updateView);
	}
	
	private boolean isDataUpdateNeeded(AixLocationInfo locationInfo) {
		if (locationInfo.getLastForecastUpdate() == null ||
				locationInfo.getForecastValidTo() == null ||
				locationInfo.getNextForecastUpdate() == null)
		{
			return true;
		}
		
		boolean shouldUpdate = false;
		int updateHours = mAixSettings.getCachedNumUpdateHours();
		
		if (updateHours == 0) {
			if (	   (mCurrentUtcTime >= locationInfo.getLastForecastUpdate() + DateUtils.MINUTE_IN_MILLIS)
					&& (mCurrentUtcTime >= locationInfo.getNextForecastUpdate() || locationInfo.getForecastValidTo() < mCurrentUtcTime))
			{
				shouldUpdate = true;
			}
		} else {
			if (mCurrentUtcTime >= locationInfo.getLastForecastUpdate() + updateHours * DateUtils.HOUR_IN_MILLIS) {
				shouldUpdate = true;
			}
		}
		return shouldUpdate;
	}
	
	private void clearUpdateTimestamps(AixLocationInfo locationInfo) {
		locationInfo.setLastForecastUpdate(null);
		locationInfo.setForecastValidTo(null);
		locationInfo.setNextForecastUpdate(null);
		locationInfo.commit(mContext);
	}
	
	public void updateWidgetRemoteViews(String message, boolean overwrite)
	{
		PendingIntent configurationIntent = AixUtils.buildConfigurationIntent(mContext, mAixWidgetInfo.getWidgetUri());
		AixUtils.updateWidgetRemoteViews(mContext, mAixWidgetInfo.getAppWidgetId(), message, overwrite, configurationIntent);
	}
	
	private void updateData(AixLocationInfo aixLocationInfo) throws AixDataUpdateException {
		Log.d(TAG, "updateData() started uri=" + aixLocationInfo.getLocationUri());

		AixUtils.clearOldProviderData(mContext.getContentResolver());
		AixGeoNamesData.build(mContext, this, mAixSettings).update(aixLocationInfo, mCurrentUtcTime);
		AixMetSunTimeData.build(mContext, this, mAixSettings).update(aixLocationInfo, mCurrentUtcTime);

		int provider = mAixSettings.getCachedProvider();

		if ((provider == AixUtils.PROVIDER_AUTO && isLocationInUS(aixLocationInfo)) || provider == AixUtils.PROVIDER_NWS) {
			AixNoaaWeatherData.build(mContext, this, mAixSettings).update(aixLocationInfo, mCurrentUtcTime);
		} else {
			AixMetWeatherData.build(mContext, this, mAixSettings).update(aixLocationInfo, mCurrentUtcTime);
		}
	}
	
}
