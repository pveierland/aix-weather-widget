package net.veierland.aix;

import static net.veierland.aix.AixUtils.WEATHER_ICON_CLOUD;
import static net.veierland.aix.AixUtils.WEATHER_ICON_DAY_PARTLYCLOUD;
import static net.veierland.aix.AixUtils.WEATHER_ICON_DAY_POLAR_LIGHTCLOUD;
import static net.veierland.aix.AixUtils.WEATHER_ICON_DAY_POLAR_LIGHTRAINTHUNDERSUN;
import static net.veierland.aix.AixUtils.WEATHER_ICON_DAY_POLAR_SLEETSUN;
import static net.veierland.aix.AixUtils.WEATHER_ICON_DAY_SNOWSUN;
import static net.veierland.aix.AixUtils.WEATHER_ICON_DAY_SUN;
import static net.veierland.aix.AixUtils.WEATHER_ICON_FOG;
import static net.veierland.aix.AixUtils.WEATHER_ICON_LIGHTRAIN;
import static net.veierland.aix.AixUtils.WEATHER_ICON_NIGHT_LIGHTCLOUD;
import static net.veierland.aix.AixUtils.WEATHER_ICON_NIGHT_LIGHTRAINSUN;
import static net.veierland.aix.AixUtils.WEATHER_ICON_NIGHT_LIGHTRAINTHUNDERSUN;
import static net.veierland.aix.AixUtils.WEATHER_ICON_NIGHT_PARTLYCLOUD;
import static net.veierland.aix.AixUtils.WEATHER_ICON_NIGHT_SLEETSUN;
import static net.veierland.aix.AixUtils.WEATHER_ICON_NIGHT_SNOWSUN;
import static net.veierland.aix.AixUtils.WEATHER_ICON_NIGHT_SUN;
import static net.veierland.aix.AixUtils.WEATHER_ICON_RAIN;
import static net.veierland.aix.AixUtils.WEATHER_ICON_RAINTHUNDER;
import static net.veierland.aix.AixUtils.WEATHER_ICON_SLEET;
import static net.veierland.aix.AixUtils.WEATHER_ICON_SNOW;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;

import net.veierland.aix.AixProvider.AixIntervalDataForecastColumns;
import net.veierland.aix.AixProvider.AixIntervalDataForecasts;
import net.veierland.aix.AixProvider.AixLocations;
import net.veierland.aix.AixProvider.AixLocationsColumns;
import net.veierland.aix.AixProvider.AixPointDataForecastColumns;
import net.veierland.aix.AixProvider.AixPointDataForecasts;
import net.veierland.aix.AixProvider.AixSunMoonData;
import net.veierland.aix.AixProvider.AixSunMoonDataColumns;
import net.veierland.aix.AixProvider.AixViews;
import net.veierland.aix.AixProvider.AixWidgetsColumns;
import net.veierland.aix.widget.AixDetailedWidget;
import net.veierland.aix.widget.AixWidgetDrawException;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.Xml;
import android.view.View;
import android.widget.RemoteViews;

public class AixUpdate {
	
	private final static String TAG = "AixUpdate";
	
	public static final int WIDGET_STATE_MESSAGE = 1;
	public static final int WIDGET_STATE_RENDER = 2;
	
	private boolean mAwakeOnly;
	private boolean mWifiOnly;
	
	private int mAppWidgetId;
	private int mLayout;
	private int mUpdateHours;
	private int mWidgetSize;
	
	private int mProvider;
	
	private final static int PROVIDER_AUTO = 1;
	private final static int PROVIDER_NMET = 2;
	private final static int PROVIDER_NWS = 3;
	
	private int[] mViews;
	private int[] mViewLocations;
	private int[] mViewTypes;
	
	private long mCurrentLocalTime;
	private long mCurrentUtcTime;
	private long mForecastValidTo;
	private long mLastForecastUpdate;
	private long mNextForecastUpdate;
	
	private ContentResolver mResolver;
	
	private Context mContext;
	
	private String mWidgetTitle;
	
	private Uri mLandscapeUri;
	private Uri mPortraitUri;
	private Uri mWidgetUri;
	private Uri mLocationUri;
	
	private float mLatitude;
	private float mLongitude;
	private String mLatitudeString;
	private String mLongitudeString;
	
	private long mLocationId = -1;
	
	private String mPortraitFileName;
	private String mLandscapeFileName;
	
	private TimeZone mLocationTimeZone;
	private TimeZone mUtcTimeZone = TimeZone.getTimeZone("UTC");
	
	public AixUpdate(Context context, Uri widgetUri) {
		mContext = context;
		mWidgetUri = widgetUri;
		
		mAppWidgetId = (int)ContentUris.parseId(widgetUri);
		
		mLayout = R.layout.widget;
	}
	
	public void process() throws Exception {
		Log.d(TAG, "process() started!");
		
		mCurrentLocalTime = System.currentTimeMillis();
		mCurrentUtcTime = Calendar.getInstance(mUtcTimeZone).getTimeInMillis();
		
		mResolver = mContext.getContentResolver();
		
		if (!getWidgetInfo()) {
			Log.d(TAG, "Error: No widget found!");
			updateWidgetRemoteViews("Error: Please recreate widget", true);
			return;
		}
		getViewInfo();
		if (!getLocationInfo()) {
			Log.d(TAG, "No location info found!");
			updateWidgetRemoteViews("No location data!", true);
			return;
		}
		
		long updateTime = Long.MAX_VALUE;
		boolean shouldUpdate = false;
		boolean weatherUpdated = false;
		
		getPreferences();
		
		shouldUpdate = isDataUpdateNeeded();
		
		if (shouldUpdate) {
			if (mWifiOnly) {
				ConnectivityManager connManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
				NetworkInfo wifiInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
				if (wifiInfo.isConnected()) {
					try {
						updateData();
						weatherUpdated = true;
						Log.d(TAG, "updateWeather() successful!");
					} catch (Exception e) {
						Log.d(TAG, "updateWeather() failed! Scheduling update in 5 minutes");
						clearUpdateTimestamps();
						updateTime = Math.min(updateTime, System.currentTimeMillis() + 5 * DateUtils.MINUTE_IN_MILLIS);
					}
				} else {
					SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
					Editor editor = settings.edit();
					editor.putBoolean("global_needwifi", true);
					editor.commit();
					Log.d(TAG, "WiFi needed, but not connected!");
				}
			} else {
				try {
					updateData();
					weatherUpdated = true;
					Log.d(TAG, "updateWeather() successful!");
				} catch (Exception e) {
					Log.d(TAG, "updateWeather() failed! Scheduling update in 5 minutes");
					clearUpdateTimestamps();
					updateTime = Math.min(updateTime, System.currentTimeMillis() + 5 * DateUtils.MINUTE_IN_MILLIS);
				}
			}
		}
		
		updateWidgetRemoteViews("Drawing widget...", false);
		
		try {
			renderWidget();
			updateWidgetRemoteViews();
		} catch (AixWidgetDrawException e) {
			Log.d(TAG, e.toString());
			e.printStackTrace();
			clearUpdateTimestamps();
			updateTime = Math.min(updateTime, System.currentTimeMillis() + 5 * DateUtils.MINUTE_IN_MILLIS);
			
			switch (e.getErrorCode()) {
			case AixWidgetDrawException.INVALID_WIDGET_SIZE:
				updateWidgetRemoteViews(mContext.getString(R.string.widget_load_error_please_recreate), true);
				return;
			case AixWidgetDrawException.GRAPH_DIMENSION_FAIL:
				updateWidgetRemoteViews("Failed to calculate dimensions.", true);
				return;
			default:
				updateWidgetRemoteViews(mContext.getString(R.string.widget_no_weather_data, 5), true);
				break;
			}
		} catch (Exception e) {
			updateWidgetRemoteViews(mContext.getString(R.string.widget_failed_to_draw), true);
			e.printStackTrace();
		}
		
		try {
			AixService.deleteCacheFiles(mContext, mAppWidgetId);
		} catch (Exception e) {
			Log.d(TAG, "Error occured when clearing cache files. " + e.getMessage());
			e.printStackTrace();
		}
		
//		try {
//			AixService.deleteTemporaryFiles(mContext, mAppWidgetId, mPortraitFileName, mLandscapeFileName);
//		} catch (Exception e) {
//			Log.d(TAG, "Error occured when clearing temporary files. " + e.getMessage());
//			e.printStackTrace();
//		}
		
		scheduleUpdate(updateTime);
		
		Log.d(TAG, "process() ended!");
	}

	private boolean isLocationInUS() {
		String widgetCountryCode = "global_country_" + mAppWidgetId;
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
		boolean isUS = settings.getString(widgetCountryCode, "").toLowerCase().equals("us");
		return isUS;

		//return mLatitude >= 18.75f && mLatitude <= 71.55f && (mLongitude <= -66.85 || mLongitude >= 170.55f);
		
		/*
        "bounds" : {
        "northeast" : {
           "lat" : 71.53879999999999,
           "lng" : -66.88507489999999
        },
        "southwest" : {
           "lat" : 18.77630,
           "lng" : 170.59570
        }
     },
     */
	}
	
	private String trimWeatherIconLink(String iconLink) {
		iconLink = iconLink.trim();
		int slashPos = iconLink.lastIndexOf("/") + 1;
		if (slashPos > iconLink.length()) return null;
		String gg = iconLink.substring(slashPos).split("\\d*[.]jpg")[0];
		return gg;
	}
	
	private void getPreferences() {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
		String updateRateString = settings.getString(mContext.getString(R.string.update_rate_string), "0");
		try {
			mUpdateHours = Integer.parseInt(updateRateString);
		} catch (NumberFormatException e) { }
		
		mWifiOnly = settings.getBoolean(mContext.getString(R.string.wifi_only_bool), false);
		mAwakeOnly = settings.getBoolean(mContext.getString(R.string.awake_only_bool), false);
		mProvider = Integer.parseInt(settings.getString(mContext.getString(R.string.provider_string), "1"));
	}
	
	private boolean getWidgetInfo() throws Exception {
		boolean isWidgetFound = false;
		Cursor widgetCursor = mResolver.query(mWidgetUri, null, null, null, null);
		String widgetViews = null;
		if (widgetCursor != null) {
			if (widgetCursor.moveToFirst()) {
				mWidgetSize = widgetCursor.getInt(AixWidgetsColumns.SIZE_COLUMN);
				widgetViews = widgetCursor.getString(AixWidgetsColumns.VIEWS_COLUMN);
				isWidgetFound = true;
			}
			widgetCursor.close();
		}
		
		if (isWidgetFound) {
			String[] views = widgetViews.split(":");
			mViews = new int[views.length];
			for (int i = 0; i < views.length; i++) {
				mViews[i] = Integer.parseInt(views[i]);
			}
			
			if (mViews.length < 1) {
				throw new Exception("Widget has invalid view properties");
			}
		}
		
		return isWidgetFound;
	}
	
	private void getViewInfo() throws Exception {
		mViewLocations = new int[mViews.length];
		mViewTypes = new int[mViews.length];
		
		for (int i = 0; i < mViews.length; i++) {
			Uri viewUri = ContentUris.withAppendedId(AixViews.CONTENT_URI, mViews[i]);
			Cursor viewCursor = mResolver.query(viewUri, null, null, null, null);
			if (viewCursor != null) {
				if (viewCursor.moveToFirst()) {
					mViewLocations[i] = viewCursor.getInt(AixViews.LOCATION_COLUMN);
					mViewTypes[i] = viewCursor.getInt(AixViews.TYPE_COLUMN);
				}
				viewCursor.close();
			}
		}
	}
	
	private boolean getLocationInfo() {
		boolean isLocationFound = false;
		
		mLocationUri = ContentUris.withAppendedId(AixLocations.CONTENT_URI, mViewLocations[0]);
		Cursor locationCursor = mResolver.query(mLocationUri, null, null, null, null);
		
		if (locationCursor != null) {
			if (locationCursor.moveToFirst()) {
				mLocationId = locationCursor.getLong(AixLocationsColumns.LOCATION_ID_COLUMN);
				mLatitudeString = locationCursor.getString(AixLocationsColumns.LATITUDE_COLUMN);
				mLongitudeString = locationCursor.getString(AixLocationsColumns.LONGITUDE_COLUMN);
				mLastForecastUpdate = locationCursor.getLong(AixLocationsColumns.LAST_FORECAST_UPDATE_COLUMN);
				mForecastValidTo = locationCursor.getLong(AixLocationsColumns.FORECAST_VALID_TO_COLUMN);
				mNextForecastUpdate = locationCursor.getLong(AixLocationsColumns.NEXT_FORECAST_UPDATE_COLUMN);
				isLocationFound = true;
			}
			locationCursor.close();
		}
		
		if (isLocationFound) {
			try {
				mLatitude = Float.parseFloat(mLatitudeString);
				mLongitude = Float.parseFloat(mLongitudeString);
				isLocationFound = true;
			} catch (NumberFormatException e) {
				Log.d(TAG, "Error parsing lat/lon! lat=" + mLatitudeString + " lon=" + mLongitudeString);
			}
		}
		return isLocationFound;
	}
	
	private void scheduleUpdate(long updateTime) {
		Calendar calendar = Calendar.getInstance();
		AixUtils.truncateHour(calendar);
		calendar.add(Calendar.HOUR, 1);
		
		// Add random interval to spread traffic
		calendar.add(Calendar.SECOND, (int)(120.0f * Math.random()));
		
		updateTime = Math.min(updateTime, calendar.getTimeInMillis());

		Intent updateIntent = new Intent(
				AixService.ACTION_UPDATE_WIDGET, mWidgetUri, mContext, AixService.class);
		PendingIntent pendingUpdateIntent =
				PendingIntent.getService(mContext.getApplicationContext(), 0, updateIntent, 0);
		
		AlarmManager alarmManager = (AlarmManager)mContext.getSystemService(Context.ALARM_SERVICE);
		alarmManager.set(mAwakeOnly ? AlarmManager.RTC : AlarmManager.RTC_WAKEUP, updateTime, pendingUpdateIntent);
		Log.d(TAG, "Scheduling next update for: " + (new SimpleDateFormat().format(updateTime)) + " AwakeOnly=" + mAwakeOnly);
	}
	
	private void renderWidget() throws AixWidgetDrawException, IOException {
		mPortraitFileName = "aix_" + mAppWidgetId + "_" + mCurrentLocalTime + "_portrait.png";
		mLandscapeFileName = "aix_" + mAppWidgetId + "_" + mCurrentLocalTime + "_landscape.png";
		
		AixDetailedWidget widget = AixDetailedWidget.build(mContext.getApplicationContext(), mWidgetUri, mLocationUri, mWidgetSize);
		
		mPortraitUri = renderWidget(widget, mPortraitFileName, false);
		mLandscapeUri = renderWidget(widget, mLandscapeFileName, true);
	}
	
	private Uri renderWidget(AixDetailedWidget widget, String fileName, boolean isLandscape) throws AixWidgetDrawException, IOException {
		Bitmap bitmap = widget.render(isLandscape);
		FileOutputStream out = mContext.openFileOutput(fileName, Context.MODE_WORLD_READABLE);
		bitmap.setDensity(Bitmap.DENSITY_NONE);
		bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
		out.flush();
		out.close();
		
		String orientation = isLandscape ? "/landscape" : "/portrait";
		return Uri.parse("content://net.veierland.aix/aixrender/" +
				mAppWidgetId + '/' + mCurrentLocalTime + orientation);
	}

	private void updateWidgetRemoteViews() {
		String widgetProperty = "global_widget_" + mAppWidgetId;
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
		Editor editor = settings.edit();
		editor.putInt(widgetProperty, WIDGET_STATE_RENDER);
		editor.commit();
		
		RemoteViews updateView = new RemoteViews(mContext.getPackageName(), mLayout);
		
		updateView.setViewVisibility(R.id.widgetTextContainer, View.GONE);
		updateView.setViewVisibility(R.id.widgetImageContainer, View.VISIBLE);
		
		updateView.setImageViewUri(R.id.widgetImagePortrait, mPortraitUri);
		updateView.setImageViewUri(R.id.widgetImageLandscape, mLandscapeUri);
		
		Intent editWidgetIntent = new Intent(
				Intent.ACTION_EDIT, mWidgetUri, mContext.getApplicationContext(), AixConfigure.class);
		editWidgetIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		editWidgetIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent pendingIntent = PendingIntent.getActivity(
				mContext.getApplicationContext(), 0, editWidgetIntent, 0);
		
		updateView.setOnClickPendingIntent(R.id.widgetContainer, pendingIntent);
		
		AppWidgetManager.getInstance(mContext).updateAppWidget(mAppWidgetId, updateView);
	}
	
	private void updateWidgetRemoteViews(String message, boolean overwrite) {
		String widgetProperty = "global_widget_" + mAppWidgetId;
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
		int widgetState = settings.getInt(widgetProperty, WIDGET_STATE_MESSAGE);
		
		if (widgetState == WIDGET_STATE_RENDER) {
			if (overwrite) {
				Editor editor = settings.edit();
				editor.putInt(widgetProperty, WIDGET_STATE_MESSAGE);
				editor.commit();
			} else {
				return;
			}
		}
		
		Intent editWidgetIntent = new Intent(
				Intent.ACTION_EDIT, mWidgetUri, mContext.getApplicationContext(), AixConfigure.class);
		editWidgetIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		editWidgetIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent pendingIntent = PendingIntent.getActivity(
				mContext.getApplicationContext(), 0, editWidgetIntent, 0);
		
		RemoteViews updateView = new RemoteViews(mContext.getPackageName(), mLayout);
		updateView.setViewVisibility(R.id.widgetTextContainer, View.VISIBLE);
		updateView.setViewVisibility(R.id.widgetImageContainer, View.GONE);
		updateView.setTextViewText(R.id.widgetText, message);
		updateView.setOnClickPendingIntent(R.id.widgetContainer, pendingIntent);
		
		AppWidgetManager.getInstance(mContext).updateAppWidget(mAppWidgetId, updateView);
	}
	
	private boolean isDataUpdateNeeded() {
		boolean shouldUpdate = false;
		if (mUpdateHours == 0) {
			if ((mCurrentUtcTime >= mLastForecastUpdate + 5 * DateUtils.MINUTE_IN_MILLIS)
					&& (mCurrentUtcTime >= mNextForecastUpdate || mForecastValidTo < mCurrentUtcTime))
			{
				shouldUpdate = true;
			}
		} else {
			if (mCurrentUtcTime >= mLastForecastUpdate + mUpdateHours * DateUtils.HOUR_IN_MILLIS) {
				shouldUpdate = true;
			}
		}
		return shouldUpdate;
	}
	
	private void clearUpdateTimestamps() {
		ContentValues values = new ContentValues();
		values.put(AixLocationsColumns.LAST_FORECAST_UPDATE, 0);
		values.put(AixLocationsColumns.FORECAST_VALID_TO, 0);
		values.put(AixLocationsColumns.NEXT_FORECAST_UPDATE, 0);
		mResolver.update(mLocationUri, values, null, null);
	}
	
	private String getTimezone() throws IOException, Exception {
		// Check if timezone info needs to be retrieved
		Cursor timezoneCursor = mResolver.query(
				AixLocations.CONTENT_URI,
				null,
				BaseColumns._ID + '=' + Long.toString(mLocationId),
				null, null);
		
		String timezoneId = null;
		if (timezoneCursor != null) {
			if (timezoneCursor.moveToFirst()) {
				timezoneId = timezoneCursor.getString(AixLocationsColumns.TIME_ZONE_COLUMN);
			}
			timezoneCursor.close();
		}
		
		if (TextUtils.isEmpty(timezoneId)) {
			// Don't have the timezone. Retrieve!
			String url = "http://api.geonames.org/timezoneJSON?lat=" + mLatitudeString + "&lng=" + mLongitudeString + "&username=aix_widget";
			HttpGet httpGet = new HttpGet(url);
			
			try {
				HttpResponse response = setupHttpClient().execute(httpGet);
				InputStream content = response.getEntity().getContent();
				
				String input = AixUtils.convertStreamToString(content);
				
				JSONObject jObject = new JSONObject(input);
				timezoneId = jObject.getString("timezoneId");
				
				String countryCode = jObject.getString("countryCode");
				Log.d(TAG, "timezoneId=" + timezoneId + ",countryCode=" + countryCode);
				if (!TextUtils.isEmpty(countryCode)) {
					String widgetCountryCode = "global_country_" + mAppWidgetId;
					SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
					Editor editor = settings.edit();
					editor.putString(widgetCountryCode, countryCode);
					editor.commit();
				}
			} catch (Exception e) {
				Log.d(TAG, "Failed to retrieve timezone data. " + e.getMessage());
				e.printStackTrace();
				throw new Exception("Failed to retrieve timezone data");
			}
		}
		
		return timezoneId;
	}
	
	private void getSunTimes(ContentResolver resolver, long locationId,
			String latitude, String longitude) throws IOException, Exception
	{
		Calendar calendar = Calendar.getInstance(mUtcTimeZone);
		long timeNow = calendar.getTimeInMillis();

		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);

		int numDaysBuffered = 5;
		
		calendar.add(Calendar.DAY_OF_YEAR, -1);
		long dateFrom = calendar.getTimeInMillis();
		calendar.add(Calendar.DAY_OF_YEAR, numDaysBuffered - 1);
		long dateTo = calendar.getTimeInMillis();
		
		int numExists = -1;
		Cursor cursor = null;
		
		cursor = resolver.query(
				AixSunMoonData.CONTENT_URI,
				null,
				AixSunMoonDataColumns.LOCATION + "=" + locationId + " AND " +
				AixSunMoonDataColumns.DATE + ">=" + dateFrom + " AND " +
				AixSunMoonDataColumns.DATE + "<=" + dateTo,
				null, null);
		if (cursor != null) {
			numExists = cursor.getCount();
			cursor.close();
		}
		
		Log.d(TAG, "getSunTimes(): For location " + locationId + " there are " + numExists +
				" existing data sets");
		
		if (numExists == -1) {
			throw new Exception("getSunTimes(): Failed to query database for pre-existing sun/moon data.");
		} else if (numExists > numDaysBuffered) {
			Log.d(TAG, "getSunTimes(): Enough data sets exists");
			return;
		}
		
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		
		dateFormat.setTimeZone(mUtcTimeZone);
		timeFormat.setTimeZone(mUtcTimeZone);

		String dateFromString = dateFormat.format(dateFrom);
		String dateToString = dateFormat.format(dateTo);
		
		// http://api.met.no/weatherapi/sunrise/1.0/?lat=60.10;lon=9.58;from=2009-04-01;to=2009-04-15
		HttpGet httpGet = new HttpGet(
				"http://api.met.no/weatherapi/sunrise/1.0/?lat="
				+ latitude + ";lon=" + longitude + ";from="
				+ dateFromString + ";to=" + dateToString);
		httpGet.addHeader("Accept-Encoding", "gzip");
		HttpResponse response = setupHttpClient().execute(httpGet);
		InputStream content = response.getEntity().getContent();
		
		Header contentEncoding = response.getFirstHeader("Content-Encoding");
		if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
			content = new GZIPInputStream(content);
		}
		
		XmlPullParser parser = Xml.newPullParser();
		parser.setInput(content, null);
		int eventType = parser.getEventType();
		
		ContentValues contentValues = new ContentValues();
		
		while (eventType != XmlPullParser.END_DOCUMENT) {
			switch (eventType) {
			case XmlPullParser.END_TAG:
				if (	parser.getName().equals("time") &&
						contentValues.containsKey(AixSunMoonDataColumns.LOCATION))
				{
					resolver.insert(AixSunMoonData.CONTENT_URI, contentValues);
				}
				break;
			case XmlPullParser.START_TAG:
				if (parser.getName().equals("time")) {
					contentValues.clear();
					try {
						String date = parser.getAttributeValue(null, "date");
						long dateParsed = dateFormat.parse(date).getTime();
						
						contentValues.put(AixSunMoonDataColumns.LOCATION, locationId);
						contentValues.put(AixSunMoonDataColumns.TIME_ADDED, timeNow);
						contentValues.put(AixSunMoonDataColumns.DATE, dateParsed);
					} catch (Exception e) {
					}
				} else if (parser.getName().equals("sun")) {
					String neverRiseValue = parser.getAttributeValue(null, "never_rise");
					String neverSetValue = parser.getAttributeValue(null, "never_set");
					if (neverRiseValue != null && neverRiseValue.equalsIgnoreCase("true")) {
						contentValues.put(AixSunMoonDataColumns.SUN_RISE, AixSunMoonData.NEVER_RISE);
						contentValues.putNull(AixSunMoonDataColumns.SUN_SET);
					} else if (neverSetValue != null && neverSetValue.equalsIgnoreCase("true")) {
						contentValues.put(AixSunMoonDataColumns.SUN_SET, AixSunMoonData.NEVER_SET);
						contentValues.putNull(AixSunMoonDataColumns.SUN_RISE);
					} else {
						try {
							long sunRise = timeFormat.parse(parser.getAttributeValue(null, "rise")).getTime();
							long sunSet = timeFormat.parse(parser.getAttributeValue(null, "set")).getTime();
							contentValues.put(AixSunMoonDataColumns.SUN_RISE, sunRise);
							contentValues.put(AixSunMoonDataColumns.SUN_SET, sunSet);
						} catch (Exception e) {
							Log.d(TAG, "getSunTimes(): Exception thrown when parsing sun data (" +
									e.getMessage() + ")");
						}
					}
				} else if (parser.getName().equals("moon")) {
					String neverRiseValue = parser.getAttributeValue(null, "never_rise");
					String neverSetValue = parser.getAttributeValue(null, "never_set");
					if (neverRiseValue != null && neverRiseValue.equalsIgnoreCase("true")) {
						contentValues.put(AixSunMoonDataColumns.MOON_RISE, AixSunMoonData.NEVER_RISE);
						contentValues.putNull(AixSunMoonDataColumns.MOON_SET);
					} else if (neverSetValue != null && neverSetValue.equalsIgnoreCase("true")) {
						contentValues.put(AixSunMoonDataColumns.MOON_SET, AixSunMoonData.NEVER_SET);
						contentValues.putNull(AixSunMoonDataColumns.MOON_RISE);
					} else {
						try {
							long moonRise = timeFormat.parse(parser.getAttributeValue(null, "rise")).getTime();
							long moonSet = timeFormat.parse(parser.getAttributeValue(null, "set")).getTime();
							contentValues.put(AixSunMoonDataColumns.MOON_RISE, moonRise);
							contentValues.put(AixSunMoonDataColumns.MOON_SET, moonSet);
						} catch (Exception e) {
							Log.d(TAG, "getSunTimes(): Exception thrown when parsing moon data (" +
									e.getMessage() + ")");
						}
					}
					
					Map<String, Integer> moonPhaseMap = new HashMap<String, Integer>() {{
						put("new moon", AixSunMoonData.NEW_MOON);
						put("waxing crescent", AixSunMoonData.WAXING_CRESCENT);
						put("first quarter", AixSunMoonData.FIRST_QUARTER);
						put("waxing gibbous", AixSunMoonData.WAXING_GIBBOUS);
						put("full moon", AixSunMoonData.FULL_MOON);
						put("waning gibbous", AixSunMoonData.WANING_GIBBOUS);
						put("third quarter", AixSunMoonData.LAST_QUARTER);
						put("waning crescent", AixSunMoonData.WANING_CRESCENT);
					}};
					
					String moonPhase = parser.getAttributeValue(null, "phase");
					int moonPhaseData = AixSunMoonData.NO_MOON_PHASE_DATA;
					
					if (moonPhase != null) {
						moonPhase = moonPhase.toLowerCase();
						if (moonPhaseMap.containsKey(moonPhase)) {
							moonPhaseData = moonPhaseMap.get(moonPhase);
						}
					}
					
					contentValues.put(AixSunMoonDataColumns.MOON_PHASE, moonPhaseData);
				}
				break;
			}
			eventType = parser.next();
		}
	}
	
	private void updateData() throws BadDataException, IOException, Exception {
		Log.d(TAG, "updateData() started uri=" + mLocationUri);
		
		Calendar calendar = Calendar.getInstance(mUtcTimeZone);
		long timeNow = calendar.getTimeInMillis();
		AixUtils.truncateHour(calendar);
		
		calendar.add(Calendar.HOUR_OF_DAY, -12);
		mResolver.delete(
				AixPointDataForecasts.CONTENT_URI,
				AixPointDataForecastColumns.TIME + "<=" + calendar.getTimeInMillis(),
				null);
		mResolver.delete(
				AixIntervalDataForecasts.CONTENT_URI,
				AixIntervalDataForecastColumns.TIME_TO + "<=" + calendar.getTimeInMillis(),
				null);
		calendar.add(Calendar.HOUR_OF_DAY, -36);
		mResolver.delete(
				AixSunMoonData.CONTENT_URI,
				AixSunMoonDataColumns.DATE + "<=" + calendar.getTimeInMillis(),
				null);
		String timeZoneId = null;
		
		updateWidgetRemoteViews("Getting timezone data...", false);
		
		timeZoneId = getTimezone();
		
		updateWidgetRemoteViews("Getting sun time data...", false);
		
		try {
			getSunTimes(mResolver, mLocationId, mLatitudeString, mLongitudeString);
		} catch (Exception e) {
			e.printStackTrace();
			Log.d(TAG, e.getMessage());
		}
		
		if ((mProvider == PROVIDER_AUTO && isLocationInUS()) || mProvider == PROVIDER_NWS) {
			updateWeatherDataNOAA(timeNow, timeZoneId);
		} else {
			updateWeatherDataMET(timeNow, timeZoneId);
		}
	}
	
	private void updateWeatherDataMET(long timeNow, String timeZoneId) throws BadDataException, IOException, Exception {
		Log.d(TAG, "updateWeatherDataMET(" + timeNow + "," + timeZoneId + "): started uri=" + mLocationUri);
		
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		dateFormat.setTimeZone(mUtcTimeZone);
		
		updateWidgetRemoteViews("Downloading weather data...", false);
		
		// Parse weather data
		XmlPullParser parser = Xml.newPullParser();
		
		long nextUpdate = -1;
		long forecastValidTo = -1;
		
		ContentValues contentValues = new ContentValues();
		
		// Retrieve weather data
		HttpGet httpGet = new HttpGet(
				"http://api.met.no/weatherapi/locationforecast/1.8/?lat="
				+ mLatitude + ";lon=" + mLongitude);
		httpGet.addHeader("Accept-Encoding", "gzip");
		
		HttpResponse response = setupHttpClient().execute(httpGet);
		InputStream content = response.getEntity().getContent();
		
		Header contentEncoding = response.getFirstHeader("Content-Encoding");
		if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
			content = new GZIPInputStream(content);
		}
		
		Uri contentProviderUri = null;
		
		updateWidgetRemoteViews("Parsing weather data...", false);
		
		try {
			parser.setInput(content, null);
			int eventType = parser.getEventType();
			
			while (eventType != XmlPullParser.END_DOCUMENT) {
				switch (eventType) {
				case XmlPullParser.END_TAG:
					if (parser.getName().equals("time") && contentValues != null && contentProviderUri != null) {
						mResolver.insert(contentProviderUri, contentValues);
					}
					break;
				case XmlPullParser.START_TAG:
					if (parser.getName().equals("time")) {
						contentValues.clear();
						
						String fromString = parser.getAttributeValue(null, "from");
						String toString = parser.getAttributeValue(null, "to");
						
						try {
    						long from = dateFormat.parse(fromString).getTime();
							long to = dateFormat.parse(toString).getTime();
							
							if (from != to) {
								contentProviderUri = AixIntervalDataForecasts.CONTENT_URI;
								contentValues.put(AixIntervalDataForecastColumns.LOCATION, mLocationId);
								contentValues.put(AixIntervalDataForecastColumns.TIME_ADDED, timeNow);
								contentValues.put(AixIntervalDataForecastColumns.TIME_FROM, from);
								contentValues.put(AixIntervalDataForecastColumns.TIME_TO, to);
							} else {
								contentProviderUri = AixPointDataForecasts.CONTENT_URI;
								contentValues.put(AixPointDataForecastColumns.LOCATION, mLocationId);
								contentValues.put(AixPointDataForecastColumns.TIME_ADDED, timeNow);
								contentValues.put(AixPointDataForecastColumns.TIME, from);
							}
						} catch (Exception e) {
							Log.d(TAG, "Error parsing from & to values. from="
									+ fromString + " to=" + toString);
							contentValues = null;
						}
					} else if (parser.getName().equals("temperature")) {
						if (contentValues != null) {
							contentValues.put(AixPointDataForecastColumns.TEMPERATURE,
									Float.parseFloat(parser.getAttributeValue(null, "value")));
						}
					} else if (parser.getName().equals("humidity")) {
						if (contentValues != null) {
							contentValues.put(AixPointDataForecastColumns.HUMIDITY,
									Float.parseFloat(parser.getAttributeValue(null, "value")));
						}
					} else if (parser.getName().equals("pressure")) {
						if (contentValues != null) {
							contentValues.put(AixPointDataForecastColumns.PRESSURE,
									Float.parseFloat(parser.getAttributeValue(null, "value")));
						}
					} else if (parser.getName().equals("symbol")) {
						if (contentValues != null) {
							contentValues.put(AixIntervalDataForecastColumns.WEATHER_ICON,
									Integer.parseInt(parser.getAttributeValue(null, "number")));
						}
					} else if (parser.getName().equals("precipitation")) {
						if (contentValues != null) {
							contentValues.put(AixIntervalDataForecastColumns.RAIN_VALUE,
									Float.parseFloat(
											parser.getAttributeValue(null, "value")));
							try {
								contentValues.put(AixIntervalDataForecastColumns.RAIN_MINVAL,
    									Float.parseFloat(
    											parser.getAttributeValue(null, "minvalue")));
							} catch (Exception e) {
								/* LOW VALUE IS OPTIONAL */
							}
							try {
    							contentValues.put(AixIntervalDataForecastColumns.RAIN_MAXVAL,
    									Float.parseFloat(
    											parser.getAttributeValue(null, "maxvalue")));
							} catch (Exception e) {
								/* HIGH VALUE IS OPTIONAL */
							}
						}
					} else if (parser.getName().equals("model")) {
						String model = parser.getAttributeValue(null, "name");
						if (model.toLowerCase().equals("yr")) {
							try {
								nextUpdate = dateFormat.parse(parser.getAttributeValue(null, "nextrun")).getTime();
							} catch (ParseException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							try {
								forecastValidTo = dateFormat.parse(parser.getAttributeValue(null, "to")).getTime();
							} catch (ParseException e) {
								e.printStackTrace();
							}
						}
					}
					break;
				}
				eventType = parser.next();
			}
			
			// Successfully retrieved weather data. Update lastUpdated parameter
			contentValues.clear();
			if (timeZoneId != null) {
				contentValues.put(AixLocationsColumns.TIME_ZONE, timeZoneId);
			}
			contentValues.put(AixLocationsColumns.LAST_FORECAST_UPDATE, timeNow);
			contentValues.put(AixLocationsColumns.FORECAST_VALID_TO, forecastValidTo);
			contentValues.put(AixLocationsColumns.NEXT_FORECAST_UPDATE, nextUpdate);
			mResolver.update(mLocationUri, contentValues, null, null);

			// Remove duplicates from weather data
			Log.d(TAG, "Removed " + mResolver.update(AixPointDataForecasts.CONTENT_URI, null, null, null) + " redundant points from db.");
			Log.d(TAG, "Removed " + mResolver.update(AixIntervalDataForecasts.CONTENT_URI, null, null, null) + " redundant intervals from db.");
		} catch (XmlPullParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static final Map<String, Integer> weatherIconMapping = new HashMap<String, Integer>() {{
		/* Night icons */
		put("ntsra", WEATHER_ICON_RAINTHUNDER);
		put("nscttsra", WEATHER_ICON_NIGHT_LIGHTRAINTHUNDERSUN);
		put("ip", WEATHER_ICON_SLEET);
		put("nraip", WEATHER_ICON_SLEET);
		put("mix", WEATHER_ICON_NIGHT_SLEETSUN);
		put("nrasn", WEATHER_ICON_NIGHT_SLEETSUN);
		put("nsn", WEATHER_ICON_NIGHT_SNOWSUN);
		put("fzra", WEATHER_ICON_SLEET);
		put("ip", WEATHER_ICON_SLEET);
		put("nra", WEATHER_ICON_RAIN);
		put("hi_nshwrs", WEATHER_ICON_NIGHT_LIGHTRAINSUN);
		put("nra", WEATHER_ICON_LIGHTRAIN);
		put("blizzard", WEATHER_ICON_SNOW);
		put("du", WEATHER_ICON_NIGHT_PARTLYCLOUD);
		put("fu", WEATHER_ICON_NIGHT_PARTLYCLOUD);
		put("nfg", WEATHER_ICON_FOG);
		put("nwind", WEATHER_ICON_NIGHT_PARTLYCLOUD);
		put("novc", WEATHER_ICON_CLOUD);
		put("nbkn", WEATHER_ICON_NIGHT_PARTLYCLOUD);
		put("nsct", WEATHER_ICON_NIGHT_LIGHTCLOUD);
		put("nfew", WEATHER_ICON_NIGHT_LIGHTCLOUD);
		put("nskc", WEATHER_ICON_NIGHT_SUN);
		
		/* Day icons */
		put("tsra", WEATHER_ICON_RAINTHUNDER);
		put("scttsra", WEATHER_ICON_DAY_POLAR_LIGHTRAINTHUNDERSUN);
		//put("ip", WEATHER_ICON_SLEET);
		put("raip", WEATHER_ICON_SLEET);
		//put("mix", WEATHER_ICON_DAY_POLAR_SLEETSUN);
		put("rasn", WEATHER_ICON_DAY_POLAR_SLEETSUN);
		put("sn", WEATHER_ICON_DAY_SNOWSUN);
		//put("fzra", WEATHER_ICON_SLEET);
		//put("ip", WEATHER_ICON_SLEET);
		put("shra", WEATHER_ICON_RAIN);
		//put("hi_shwrs", WEATHER_ICON_DAY_LIGHTRAINSUN);
		put("ra", WEATHER_ICON_LIGHTRAIN);
		//put("blizzard", WEATHER_ICON_SNOW);
		//put("du", WEATHER_ICON_DAY_PARTLYCLOUD);
		//put("fu", WEATHER_ICON_DAY_PARTLYCLOUD);
		put("fg", WEATHER_ICON_FOG);
		put("cold", WEATHER_ICON_DAY_PARTLYCLOUD);
		put("hot", WEATHER_ICON_DAY_SUN);
		put("wind", WEATHER_ICON_DAY_PARTLYCLOUD);
		put("ovc", WEATHER_ICON_CLOUD);
		put("bkn", WEATHER_ICON_DAY_PARTLYCLOUD);
		put("sct", WEATHER_ICON_DAY_POLAR_LIGHTCLOUD);
		put("few", WEATHER_ICON_DAY_POLAR_LIGHTCLOUD);
		put("skc", WEATHER_ICON_DAY_SUN);
	}};
	
	private static final int PARSE_TIME_LAYOUT_FLAG				= 1<<0;
	private static final int PARSE_TIME_LAYOUT_KEY_FLAG			= 1<<1;
	private static final int PARSE_TIME_START_FLAG				= 1<<2;
	private static final int PARSE_TIME_END_FLAG				= 1<<3;
	private static final int PARSE_TEMPERATURE_FLAG				= 1<<4;
	private static final int PARSE_PRECIPITATION_FLAG			= 1<<5;
	private static final int PARSE_HUMIDITY_FLAG				= 1<<6;
	private static final int PARSE_VALUE_FLAG					= 1<<7;
	private static final int PARSE_WEATHER_ICONS_FLAG			= 1<<8;
	private static final int PARSE_WEATHER_ICONS_LINK_FLAG		= 1<<9;
	
	private static final int PARSE_TIME_LAYOUT_MASK = 0x00;
	private static final int PARSE_TIME_LAYOUT_KEY_MASK = PARSE_TIME_LAYOUT_FLAG;
	private static final int PARSE_TIME_START_MASK = PARSE_TIME_LAYOUT_FLAG;
	private static final int PARSE_TIME_END_MASK = PARSE_TIME_LAYOUT_FLAG;
	private static final int PARSE_TEMPERATURE_MASK = 0x00;
	private static final int PARSE_PRECIPITATION_MASK = 0x00;
	private static final int PARSE_HUMIDITY_MASK = 0x00;
	private static final int PARSE_VALUE_MASK = ~(1<<7);
	private static final int PARSE_WEATHER_ICONS_MASK = 0x00;
	private static final int PARSE_WEATHER_ICONS_LINK_MASK = PARSE_WEATHER_ICONS_FLAG;
	
	private void updateWeatherDataNOAA(long timeNow, String timeZoneId) throws BadDataException, IOException, Exception {
		Log.d(TAG, "updateWeatherDataNOAA(" + timeNow + "," + timeZoneId + "): started uri=" + mLocationUri);
		
		updateWidgetRemoteViews("Downloading weather data...", false);
		
		String url = "http://www.weather.gov/forecasts/xml/sample_products/browser_interface/ndfdXMLclient.php?lat="
				+ mLatitudeString + "&lon=" + mLongitudeString + "&product=time-series";
		
		Log.d(TAG, url);
		
		// Retrieve weather data
		HttpGet httpGet = new HttpGet(url);//&temp=temp&qpf=qpf&icons=icons&rh=rh");
		httpGet.addHeader("Accept-Encoding", "gzip");

		HttpResponse response = setupHttpClient().execute(httpGet);
		InputStream content = response.getEntity().getContent();
		
		Header contentEncoding = response.getFirstHeader("Content-Encoding");
		if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
			content = new GZIPInputStream(content);
		}
		
		updateWidgetRemoteViews("Parsing weather data...", false);
		
		boolean isFahrenheit = false;
		boolean isInches = false;
		
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		dateFormat.setTimeZone(mUtcTimeZone);
		
		Map<String, ArrayList<Long>> startTimeMap = new HashMap<String, ArrayList<Long>>();
		Map<String, ArrayList<Long>> endTimeMap = new HashMap<String, ArrayList<Long>>();
		
		ArrayList<Long> tempStartTimes = null;
		ArrayList<Long> tempEndTimes = null;
		String tempTimeKey = null;
		
		// Point Data
		String humidityTimeKey = null;
		ArrayList<Float> humidityData = new ArrayList<Float>();
		
		String temperatureTimeKey = null;
		ArrayList<Float> temperatureData = new ArrayList<Float>();
		
		// Interval Data
		String weatherIconTimeKey = null;
		ArrayList<String> weatherIconData = new ArrayList<String>();
		
		String precipitationTimeKey = null;
		ArrayList<Float> precipitationData = new ArrayList<Float>();
		
		int state = 0;
		
		XmlPullParser parser = Xml.newPullParser();
		try {
			parser.setInput(content, null);
			int eventType = parser.getEventType();
			
			while (eventType != XmlPullParser.END_DOCUMENT) {
				String tagName = parser.getName();
				switch (eventType) {
				case XmlPullParser.TEXT:
					String text = parser.getText();
					if (text == null) continue;
					if ((state & PARSE_TIME_START_FLAG) != 0) {
						tempStartTimes.add(dateFormat.parse(text).getTime());
					} else if ((state & PARSE_TIME_END_FLAG) != 0) {
						tempEndTimes.add(dateFormat.parse(text).getTime());
					} else if ((state & PARSE_TIME_LAYOUT_KEY_FLAG) != 0) {
						tempTimeKey = text;
					} else if ((state & PARSE_WEATHER_ICONS_LINK_FLAG) != 0) {
						weatherIconData.add(trimWeatherIconLink(text));
					} else if ((state & PARSE_VALUE_FLAG) != 0) {
						if ((state & PARSE_TEMPERATURE_FLAG) != 0) {
							float t = Float.parseFloat(text);
							temperatureData.add(isFahrenheit ? (t - 32.0f) * 5.0f / 9.0f : t);
							//temperatureData.add(t);
						} else if ((state & PARSE_PRECIPITATION_FLAG) != 0) {
							float p = Float.parseFloat(text);
							precipitationData.add(isInches ? p * 25.4f : p);
							//precipitationData.add(p);
						} else if ((state & PARSE_HUMIDITY_FLAG) != 0) {
							humidityData.add(Float.parseFloat(text));
						}
					}
					break;
				case XmlPullParser.END_TAG:
					if (tagName.equals("time-layout")) {
						if (tempTimeKey != null) {
							if (tempStartTimes != null && tempStartTimes.size() > 0) {
								startTimeMap.put(tempTimeKey, tempStartTimes);
							}
							if (tempEndTimes != null && tempEndTimes.size() > 0) {
								endTimeMap.put(tempTimeKey, tempEndTimes);
							}
						}
						state &= PARSE_TIME_LAYOUT_MASK;
					} else if (tagName.equals("start-valid-time")) {
						state &= PARSE_TIME_START_MASK;
					} else if (tagName.equals("end-valid-time")) {
						state &= PARSE_TIME_END_MASK;
					} else if (tagName.equals("layout-key")) {
						state &= PARSE_TIME_LAYOUT_KEY_MASK;
					} else if (tagName.equals("temperature")) {
						state &= PARSE_TEMPERATURE_MASK;
					} else if (tagName.equals("precipitation")) {
						state &= PARSE_PRECIPITATION_MASK;
					} else if (tagName.equals("humidity")) {
						state &= PARSE_HUMIDITY_MASK;
					} else if (tagName.equals("conditions-icon")) {
						state &= PARSE_WEATHER_ICONS_MASK;
					} else if (tagName.equals("icon-link")) {
						state &= PARSE_WEATHER_ICONS_LINK_MASK;
					} else if (tagName.equals("value")) {
						state &= PARSE_VALUE_MASK;
					}
					break;
				case XmlPullParser.START_TAG:
					if (tagName.equals("time-layout")) {
						state |= PARSE_TIME_LAYOUT_FLAG;
						tempStartTimes = new ArrayList<Long>();
						tempEndTimes = new ArrayList<Long>();
					} else if (tagName.equals("start-valid-time")) {
						state |= PARSE_TIME_START_FLAG;
					} else if (tagName.equals("end-valid-time")) {
						state |= PARSE_TIME_END_FLAG;
					} else if (tagName.equals("layout-key")) {
						state |= PARSE_TIME_LAYOUT_KEY_FLAG;
					} else if (tagName.equals("temperature")) {
						String type = parser.getAttributeValue(null, "type");
						if (type != null && type.equals("hourly")) {
							temperatureTimeKey = parser.getAttributeValue(null, "time-layout");
							String unit = parser.getAttributeValue(null, "units");
							if (unit != null) {
								isFahrenheit = unit.toLowerCase().startsWith("f");
							}
							state = PARSE_TEMPERATURE_FLAG;
						}
					} else if (tagName.equals("precipitation")) {
						String type = parser.getAttributeValue(null, "type");
						if (type != null && type.equals("liquid")) {
							precipitationTimeKey = parser.getAttributeValue(null, "time-layout");
							String unit = parser.getAttributeValue(null, "units");
							if (unit != null) {
								isInches = unit.toLowerCase().startsWith("i");
							}
							state = PARSE_PRECIPITATION_FLAG;
						}
					} else if (tagName.equals("humidity")) {
						String type = parser.getAttributeValue(null, "type");
						if (type != null && type.equals("relative")) {
							humidityTimeKey = parser.getAttributeValue(null, "time-layout");
							state = PARSE_HUMIDITY_FLAG;
						}
					} else if (tagName.equals("conditions-icon")) {
						weatherIconTimeKey = parser.getAttributeValue(null, "time-layout");
						state = PARSE_WEATHER_ICONS_FLAG;
					} else if (tagName.equals("icon-link")) {
						state |= PARSE_WEATHER_ICONS_LINK_FLAG;
					} else if (tagName.equals("value")) {
						state |= PARSE_VALUE_FLAG;
					}
					break;
				default:
				}
				eventType = parser.next();
			}
			
			Map<Long, PointData> pointData = new HashMap<Long, PointData>();
			Map<MultiKey, IntervalData> intervalData = new HashMap<MultiKey, IntervalData>();
			
			if (temperatureTimeKey != null) {
				ArrayList<Long> temperatureTimes = startTimeMap.get(temperatureTimeKey);
				for (int i = 0; i < temperatureData.size(); i++) {
					updatePoint(pointData, temperatureTimes.get(i), timeNow).mTemperature = temperatureData.get(i);
				}
			}
			
			if (humidityTimeKey != null) {
				ArrayList<Long> humidityTimes = startTimeMap.get(humidityTimeKey);
				for (int i = 0; i < humidityData.size(); i++) {
					updatePoint(pointData, humidityTimes.get(i), timeNow).mHumidity = humidityData.get(i);
				}
			}
			
			ArrayList<Long> precipitationFromTimes = startTimeMap.get(precipitationTimeKey);
			ArrayList<Long> precipitationToTimes = endTimeMap.get(precipitationTimeKey);
			
			dateFormat.setTimeZone(TimeZone.getTimeZone(timeZoneId));
			
			for (int i = 0; i < precipitationData.size(); i++) {
				updateInterval(intervalData,
						precipitationFromTimes.get(i),
						precipitationToTimes.get(i),
						timeNow).rainValue = precipitationData.get(i);
			}
			
			ArrayList<Long> weatherIconTimes = startTimeMap.get(weatherIconTimeKey);
			for (int i = 0; i < weatherIconData.size(); i++) {
				long time = weatherIconTimes.get(i);
				Integer z = weatherIconMapping.get(weatherIconData.get(i));
				updateInterval(intervalData,
						time, time, timeNow).weatherIcon = (z != null) ? z : 0;
			}
			
			// Add merged data to provider
				
			ContentValues contentValues = new ContentValues();
			
			for (PointData p : pointData.values()) {
				contentValues.clear();
				contentValues.put(AixPointDataForecasts.LOCATION, mLocationId);
				contentValues.put(AixPointDataForecasts.TIME, p.mTime);
				contentValues.put(AixPointDataForecasts.TIME_ADDED, p.mTimeAdded);
				if (temperatureTimeKey != null) {
					contentValues.put(AixPointDataForecasts.TEMPERATURE, p.mTemperature);
				}
				if (humidityTimeKey != null) {
					contentValues.put(AixPointDataForecasts.HUMIDITY, p.mHumidity);
				}
				mResolver.insert(AixPointDataForecasts.CONTENT_URI, contentValues);
			}
			
			for (IntervalData inter : intervalData.values()) {
				contentValues.clear();
				contentValues.put(AixIntervalDataForecasts.LOCATION, mLocationId);
				contentValues.put(AixIntervalDataForecasts.TIME_FROM, inter.timeFrom);
				contentValues.put(AixIntervalDataForecasts.TIME_TO, inter.timeTo);
				contentValues.put(AixIntervalDataForecasts.TIME_ADDED, inter.timeAdded);
				contentValues.put(AixIntervalDataForecasts.RAIN_VALUE, inter.rainValue);
				contentValues.put(AixIntervalDataForecasts.WEATHER_ICON, inter.weatherIcon);
				mResolver.insert(AixIntervalDataForecasts.CONTENT_URI, contentValues);
			}
			
			// Successfully retrieved weather data. Update lastUpdated parameter
			contentValues.clear();
			if (timeZoneId != null) {
				contentValues.put(AixLocationsColumns.TIME_ZONE, timeZoneId);
			}
			contentValues.put(AixLocationsColumns.LAST_FORECAST_UPDATE, timeNow);
			contentValues.put(AixLocationsColumns.FORECAST_VALID_TO, timeNow + 18 * DateUtils.HOUR_IN_MILLIS);
			contentValues.put(AixLocationsColumns.NEXT_FORECAST_UPDATE, timeNow + 2 * DateUtils.HOUR_IN_MILLIS);
			
			mResolver.update(mLocationUri, contentValues, null, null);
			
			// Remove duplicates from weather data
			Log.d(TAG, "Removed " + mResolver.update(AixPointDataForecasts.CONTENT_URI, null, null, null) + " redundant points from db.");
			Log.d(TAG, "Removed " + mResolver.update(AixIntervalDataForecasts.CONTENT_URI, null, null, null) + " redundant intervals from db.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private HttpClient setupHttpClient() {
		HttpParams httpParameters = new BasicHttpParams();
		int timeoutConnection = 5000;
		HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
		int timeoutSocket = 10000;
		HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
		return new DefaultHttpClient(httpParameters);
	}
	
	private PointData updatePoint(Map<Long, PointData> pointData, long pointTime, long now) {
		PointData p = pointData.get(pointTime);
		if (p == null) {
			p = new PointData();
			p.mTime = pointTime;
			p.mTimeAdded = now;
			pointData.put(pointTime, p);
		}
		return p;
	}

	private IntervalData updateInterval(Map<MultiKey, IntervalData> intervalData, long fromTime, long toTime, long now) {
		MultiKey m = new MultiKey(fromTime, toTime);
		IntervalData inter = intervalData.get(m);
		if (inter == null) {
			inter = new IntervalData();
			inter.timeFrom = fromTime;
			inter.timeTo = toTime;
			inter.timeAdded = now;
			intervalData.put(m, inter);
		}
		return inter;
	}
	
}
