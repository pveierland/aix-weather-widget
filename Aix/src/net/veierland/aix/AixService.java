package net.veierland.aix;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Queue;
import java.util.TimeZone;

import net.veierland.aix.AixProvider.AixForecasts;
import net.veierland.aix.AixProvider.AixForecastsColumns;
import net.veierland.aix.AixProvider.AixLocations;
import net.veierland.aix.AixProvider.AixLocationsColumns;
import net.veierland.aix.AixProvider.AixViews;
import net.veierland.aix.AixProvider.AixWidgets;
import net.veierland.aix.AixProvider.AixWidgetsColumns;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
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
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.Xml;
import android.view.View;
import android.widget.RemoteViews;

public class AixService extends Service implements Runnable {
	private static final String TAG = "AixService";
	
	public static final String ACTION_UPDATE_ALL = "net.veierland.aix.UPDATE_ALL";
	
	private static Object sLock = new Object();
	private static boolean sThreadRunning = false;
	private static Queue<Integer> sAppWidgetIds = new LinkedList<Integer>();
	
	public static void requestUpdate(int appWidgetId) {
		synchronized (sLock) {
			sAppWidgetIds.add(appWidgetId);
		}
	}
	
	public static void requestUpdate(int[] appWidgetIds) {
		synchronized (sLock) {
			for (int appWidgetId : appWidgetIds) {
				sAppWidgetIds.add(appWidgetId);
			}
		}
	}
	
	private static boolean hasMoreUpdates() {
		synchronized (sLock) {
			boolean hasMore = !sAppWidgetIds.isEmpty();
			if (!hasMore) {
				sThreadRunning = false;
			}
			return hasMore;
		}
	}
	
	private static int getNextUpdate() {
		synchronized (sLock) {
			if (sAppWidgetIds.peek() == null) {
				return AppWidgetManager.INVALID_APPWIDGET_ID;
			} else {
				return sAppWidgetIds.poll();
			}
		}
	}
	
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		
		if (ACTION_UPDATE_ALL.equals(intent.getAction())) {
			Log.d(TAG, "Requested UPDATE ALL action");
			AppWidgetManager manager = AppWidgetManager.getInstance(this);
			requestUpdate(manager.getAppWidgetIds(new ComponentName(this, AixWidget.class)));
		}
		
		synchronized (sLock) {
			if (!sThreadRunning) {
				sThreadRunning = true;
				new Thread(this).start();
			}
		}
	}

	private final static int STATE_INITIAL = 0;
	private final static int STATE_WIDGET_NOT_FOUND = 1;
	private final static int STATE_VIEW_NOT_FOUND = 2;
	private final static int STATE_DOWNLOAD_FAILED = 3;
	
	
	@Override
	public void run() {
		Log.d(TAG, "Processing thread started");
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
		ContentResolver resolver = getContentResolver();
		
		long utcTime = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis();
		long updateTime = Long.MAX_VALUE;
		
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		
		int updateHours = 0;
		String updateRateString = settings.getString(getString(R.string.preference_update_rate), "0");
		try {
			updateHours = Integer.parseInt(updateRateString);
		} catch (NumberFormatException e) { }
		
		boolean wifiOnly = settings.getBoolean(getString(R.string.preference_wifi_only), false);
		
		while (hasMoreUpdates()) {
			// Get widget data
			int appWidgetId = getNextUpdate();
			Uri appWidgetUri = ContentUris.withAppendedId(AixWidgets.CONTENT_URI, appWidgetId);			
			
			boolean widgetFound = false;
			String widgetTitle = null, widgetViews = null;
			int widgetSize = -1;
			
			Cursor widgetCursor = resolver.query(appWidgetUri, null, null, null, null);
			if (widgetCursor != null) {
				if (widgetCursor.moveToFirst()) {
					widgetTitle = widgetCursor.getString(AixWidgetsColumns.TITLE_COLUMN);
					widgetSize = widgetCursor.getInt(AixWidgetsColumns.SIZE_COLUMN);
					widgetViews = widgetCursor.getString(AixWidgetsColumns.VIEWS_COLUMN);
					widgetFound = true;
				}
				widgetCursor.close();
			}
			
			Uri viewUri = null;
			boolean weatherUpdated = false;
			boolean locationFound = false;
			long lastForecastUpdate = -1, forecastValidTo = -1, nextForecastUpdate = -1;
			boolean shouldUpdate = false;
			
			if (!widgetFound) {
				// Draw error widget
				Log.d(TAG, "Error: Could not find widget in database. uri=" + appWidgetUri);
			} else {
				// Iterate through widget views and update as appropriate
				//for (String view : widgetViews.split(":")) {
					//Uri viewUri = ContentUris.withAppendedId(AixViews.CONTENT_URI, Long.parseLong(view));
					viewUri = ContentUris.withAppendedId(AixViews.CONTENT_URI, Long.parseLong(widgetViews));
					
					Cursor locationCursor = resolver.query(
							Uri.withAppendedPath(viewUri, AixViews.TWIG_LOCATION),
							null, null, null, null);
					
					if (locationCursor != null) {
						if (locationCursor.moveToFirst()) {
							lastForecastUpdate = locationCursor.getLong(AixLocationsColumns.LAST_FORECAST_UPDATE_COLUMN);
							forecastValidTo = locationCursor.getLong(AixLocationsColumns.FORECAST_VALID_TO_COLUMN);
							nextForecastUpdate = locationCursor.getLong(AixLocationsColumns.NEXT_FORECAST_UPDATE_COLUMN);
							locationFound = true;
						}
						locationCursor.close();
					}

					if (locationFound) {
						if (updateHours == 0) {
							if (utcTime >= lastForecastUpdate + DateUtils.HOUR_IN_MILLIS &&
									(utcTime >= nextForecastUpdate || forecastValidTo < utcTime))
							{
								shouldUpdate = true;
							}
						} else {
							if (utcTime >= lastForecastUpdate + updateHours * DateUtils.HOUR_IN_MILLIS) {
								shouldUpdate = true;
							}
						}
						
						if (shouldUpdate) {
							if (wifiOnly) {
								ConnectivityManager connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
								NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
								if (mWifi.isConnected()) {
									try {
										updateWeather(resolver, viewUri);
										weatherUpdated = true;
										Log.d(TAG, "updateWeather() successful!");
									} catch (Exception e) {
										Log.d(TAG, "updateWeather() failed! Scheduling update in 5 minutes");
										updateTime = calcUpdateTime(updateTime, System.currentTimeMillis() + 5 * DateUtils.MINUTE_IN_MILLIS);
									}
								} else {
									Editor editor = settings.edit();
									editor.putBoolean("needwifi", true);
									editor.commit();
									Log.d(TAG, "WiFi needed, but not connected!");
								}
							} else {
								try {
									updateWeather(resolver, viewUri);
									weatherUpdated = true;
									Log.d(TAG, "updateWeather() successful!");
								} catch (Exception e) {
									Log.d(TAG, "updateWeather() failed! Scheduling update in 5 minutes");
									updateTime = calcUpdateTime(updateTime, System.currentTimeMillis() + 5 * DateUtils.MINUTE_IN_MILLIS);
								}
							}
						}
					}
				//}
			}
			
			RemoteViews updateView = new RemoteViews(getPackageName(), R.layout.widget);
			if (!widgetFound || !locationFound || (!weatherUpdated && shouldUpdate && forecastValidTo < utcTime)) {
				if (widgetFound && !locationFound) {
					updateView.setViewVisibility(R.id.widgetTextContainer, View.VISIBLE);
					updateView.setViewVisibility(R.id.widgetImage, View.GONE);
					updateView.setTextViewText(R.id.widgetText, "Error. Please recreate widget.");
				} else if (widgetFound && locationFound && !weatherUpdated) {
					updateView.setViewVisibility(R.id.widgetTextContainer, View.VISIBLE);
					updateView.setViewVisibility(R.id.widgetImage, View.GONE);
					updateView.setTextViewText(R.id.widgetText, "No weather data. Retrying in 5 minutes.");
				}
			} else {
				Bitmap bitmap = AixWidget.buildView(this, viewUri, false);
				
				if (bitmap == null) {
					updateView.setViewVisibility(R.id.widgetTextContainer, View.VISIBLE);
					updateView.setViewVisibility(R.id.widgetImage, View.GONE);
					updateView.setTextViewText(R.id.widgetText, "Error. Please recreate widget.");
				} else {
					updateView.setViewVisibility(R.id.widgetTextContainer, View.GONE);
					updateView.setViewVisibility(R.id.widgetImage, View.VISIBLE);
					updateView.setImageViewBitmap(R.id.widgetImage, bitmap);
				}
			}
			
			Intent intent = new Intent(Intent.ACTION_EDIT, appWidgetUri, AixService.this, AixConfigure.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
			updateView.setOnClickPendingIntent(R.id.widgetContainer, pendingIntent);

			appWidgetManager.updateAppWidget(appWidgetId, updateView);
		}
		
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		calendar.add(Calendar.HOUR, 1);
		
		updateTime = calcUpdateTime(updateTime, calendar.getTimeInMillis());
		
		Intent updateIntent = new Intent(ACTION_UPDATE_ALL);
		updateIntent.setClass(this, AixService.class);
		
		PendingIntent pendingIntent = PendingIntent.getService(this, 0, updateIntent, 0);
		
		boolean awakeOnly = settings.getBoolean(getString(R.string.preference_awake_only), false);
		
		AlarmManager alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
		alarmManager.set(awakeOnly ? AlarmManager.RTC : AlarmManager.RTC_WAKEUP, updateTime, pendingIntent);
		Log.d(TAG, "Scheduling next update for: " + (new SimpleDateFormat().format(updateTime)) + " AwakeOnly=" + awakeOnly);
		
		stopSelf();
	}
	
	private long calcUpdateTime(long updateTime, long newTime) {
		return newTime < updateTime ? newTime : updateTime;
	}

	public String convertStreamToString(InputStream is) throws IOException {
		if (is != null) {
			Writer writer = new StringWriter();
			char[] buffer = new char[1024];
			try {
				Reader reader = new BufferedReader(
						new InputStreamReader(is, "UTF-8"));
				int n;
				while ((n = reader.read(buffer)) != -1) {
					writer.write(buffer, 0, n);
				}
			} finally {
				is.close();
			}
			return writer.toString();
		} else {
			return "";
		}
	}
	
	private String getTimezone(ContentResolver resolver, long locationId, String latitude, String longitude) throws IOException, Exception {
		// Check if timezone info needs to be retrieved
		Cursor timezoneCursor = resolver.query(
				AixLocations.CONTENT_URI,
				null,
				BaseColumns._ID + '=' + Long.toString(locationId) + " AND " + AixLocationsColumns.TIME_ZONE + " IS NOT NULL",
				null,
				null);
		String timezoneId = null;
		
		if (timezoneCursor != null) {
			if (timezoneCursor.moveToFirst()) {
				timezoneId = timezoneCursor.getString(AixLocationsColumns.TIME_ZONE_COLUMN);
			}
			timezoneCursor.close();
		}
		
		if (TextUtils.isEmpty(timezoneId)) {
			// Don't have the timezone. Retrieve!
			String url = "http://api.geonames.org/timezoneJSON?lat=" + latitude + "&lng=" + longitude + "&username=aix_widget";
			HttpGet httpGet = new HttpGet(url);
			HttpClient httpclient = new DefaultHttpClient();
			HttpResponse response = httpclient.execute(httpGet);
			InputStream content = response.getEntity().getContent();
			
			String input = convertStreamToString(content);
			
			try {
				JSONObject jObject = new JSONObject(input);
				timezoneId = jObject.getString("timezoneId");
				//ContentValues values = new ContentValues();
				//values.put(BaseColumns._ID, locationId);
				//values.put(AixLocationsColumns.TIME_ZONE, timezoneId);
				//resolver.insert(ContentUris.withAppendedId(AixLocations.CONTENT_URI, locationId), values);
			} catch (JSONException e) {
				Log.d(TAG, "Failed to retrieve timezone data. " + e.getMessage());
				throw new Exception("Failed to retrieve timezone data");
			}
		}
		
		return timezoneId;
	}
	
	private void getSunTimes(ContentResolver resolver, long locationId, String latitude, String longitude) throws IOException {
		TimeZone utc = TimeZone.getTimeZone("UTC");
		Calendar utcCalendar = Calendar.getInstance(utc);
		utcCalendar.set(Calendar.HOUR_OF_DAY, 0);
		utcCalendar.set(Calendar.MINUTE, 0);
		utcCalendar.set(Calendar.SECOND, 0);
		utcCalendar.set(Calendar.MILLISECOND, 0);

		int numDaysBuffered = 5;
		
		utcCalendar.add(Calendar.DAY_OF_YEAR, -1);
		long dateFrom = utcCalendar.getTimeInMillis();
		utcCalendar.add(Calendar.DAY_OF_YEAR, numDaysBuffered - 1);
		long dateTo = utcCalendar.getTimeInMillis();
		
		int numExists = 0;
		Cursor sunCursor = null;
		
		sunCursor = resolver.query(
				AixForecasts.CONTENT_URI,
				null,
				AixForecastsColumns.LOCATION + "=" + locationId + " AND " +
				AixForecastsColumns.TIME_FROM + ">=" + dateFrom + " AND " +
				AixForecastsColumns.TIME_TO + "<=" + dateTo + " AND " +
				AixForecastsColumns.SUN_RISE + " IS NOT NULL", null, null);

		if (sunCursor != null) {
			numExists = sunCursor.getCount();
			sunCursor.close();
		}
		if (numExists >= numDaysBuffered) return;
		
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		
		dateFormat.setTimeZone(utc);
		timeFormat.setTimeZone(utc);

		String dateFromString = dateFormat.format(dateFrom);
		String dateToString = dateFormat.format(dateTo);
		
		// http://api.met.no/weatherapi/sunrise/1.0/?lat=60.10;lon=9.58;from=2009-04-01;to=2009-04-15
		HttpGet httpGet = new HttpGet(
				"http://api.met.no/weatherapi/sunrise/1.0/?lat="
				+ latitude + ";lon=" + longitude + ";from="
				+ dateFromString + ";to=" + dateToString);
		HttpClient httpclient = new DefaultHttpClient();
		HttpResponse response = httpclient.execute(httpGet);
		InputStream content = response.getEntity().getContent();
		
		XmlPullParser parser = Xml.newPullParser();
		
		try {
			parser.setInput(content, null);
			int eventType = parser.getEventType();
			ContentValues contentValues = new ContentValues();
			
			while (eventType != XmlPullParser.END_DOCUMENT) {
				switch (eventType) {
				case XmlPullParser.END_TAG:
					if (parser.getName().equals("time") && contentValues != null) {
						resolver.insert(AixForecasts.CONTENT_URI, contentValues);
					}
					break;
				case XmlPullParser.START_TAG:
					if (parser.getName().equals("time")) {
						String date = parser.getAttributeValue(null, "date");
						try {
							long dateParsed = dateFormat.parse(date).getTime();
							contentValues.clear();
							contentValues.put(AixForecastsColumns.LOCATION, locationId);
							contentValues.put(AixForecastsColumns.TIME_FROM, dateParsed);
							contentValues.put(AixForecastsColumns.TIME_TO, dateParsed);
						} catch (Exception e) {
							contentValues = null;
						}
					} else if (parser.getName().equals("sun")) {
						if (contentValues != null) {
							try {
								long sunRise = timeFormat.parse(parser.getAttributeValue(null, "rise")).getTime();
								long sunSet = timeFormat.parse(parser.getAttributeValue(null, "set")).getTime();
								contentValues.put(AixForecastsColumns.SUN_RISE, sunRise);
								contentValues.put(AixForecastsColumns.SUN_SET, sunSet);
							} catch (Exception e) {
								contentValues = null;
							}
						}
					}
					break;
				}
				eventType = parser.next();
			}
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void updateWeather(ContentResolver resolver, Uri viewUri)
			throws BadDataException, IOException, Exception {
		Log.d(TAG, "updateWeather started uri=" + viewUri);
		
		long locationId = -1;
		String latString = null, lonString = null;
		
		boolean found = false;
		Cursor cursor = resolver.query(Uri.withAppendedPath(viewUri, AixViews.TWIG_LOCATION), null, null, null, null);
		if (cursor != null) {
			if (cursor.moveToFirst()) {
				found = true;
				locationId = cursor.getLong(AixLocationsColumns.LOCATIONS_ID_COLUMN);
				latString = cursor.getString(AixLocationsColumns.LATITUDE_COLUMN);
				lonString = cursor.getString(AixLocationsColumns.LONGITUDE_COLUMN);
			}
			cursor.close();
		}
		
		if (!found) {
			throw new BadDataException();
		}
		
		float latitude, longitude;
		try {
			latitude = Float.parseFloat(latString);
			longitude = Float.parseFloat(lonString);
		} catch (NumberFormatException e) {
			Log.d(TAG, "Error parsing lat/lon! lat=" + latString + " lon=" + lonString);
			throw new BadDataException();
		}
		
		// Set a calendar up to the current time
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		
		calendar.add(Calendar.HOUR_OF_DAY, -48);
		resolver.delete(
				AixForecasts.CONTENT_URI,
				AixForecastsColumns.LOCATION + '=' + locationId + " AND " + AixForecastsColumns.TIME_TO + "<" + calendar.getTimeInMillis(),
				null);
		calendar.add(Calendar.HOUR_OF_DAY, 48);
		
		String timeZoneId = null;
		
		timeZoneId = getTimezone(resolver, locationId, latString, lonString);
		
		try {
			getSunTimes(resolver, locationId, latString, lonString);
		} catch (Exception e) {
			Log.d(TAG, e.getMessage());
		}
		
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		
		// Parse weather data
		XmlPullParser parser = Xml.newPullParser();
		
		long nextUpdate = -1;
		long forecastValidTo = -1;
		
		ContentValues contentValues = new ContentValues();
		
		// Retrieve weather data
		HttpGet httpGet = new HttpGet(
				"http://api.met.no/weatherapi/locationforecast/1.8/?lat="
				+ latitude + ";lon=" + longitude);
		HttpClient httpclient = new DefaultHttpClient();
		HttpResponse response = httpclient.execute(httpGet);
		InputStream content = response.getEntity().getContent();
		
		try {
			parser.setInput(content, null);
			int eventType = parser.getEventType();
			
			while (eventType != XmlPullParser.END_DOCUMENT) {
				switch (eventType) {
				case XmlPullParser.END_TAG:
					if (parser.getName().equals("time") && contentValues != null) {
						resolver.insert(AixForecasts.CONTENT_URI, contentValues);
					}
					break;
				case XmlPullParser.START_TAG:
					if (parser.getName().equals("time")) {
						String fromString = parser.getAttributeValue(null, "from");
						String toString = parser.getAttributeValue(null, "to");
						
						try {
    						long from = dateFormat.parse(fromString).getTime();
							long to = dateFormat.parse(toString).getTime();
							contentValues.clear();
    						contentValues.put(AixForecastsColumns.LOCATION, locationId);
    						contentValues.put(AixForecastsColumns.TIME_FROM, from);
    						contentValues.put(AixForecastsColumns.TIME_TO, to);
						} catch (Exception e) {
							Log.d(TAG, "Error parsing from & to values. from="
									+ fromString + " to=" + toString);
							contentValues = null;
						}
					} else if (parser.getName().equals("temperature")) {
						if (contentValues != null) {
							contentValues.put(AixForecastsColumns.TEMPERATURE,
									Float.parseFloat(parser.getAttributeValue(null, "value")));
						}
					} else if (parser.getName().equals("symbol")) {
						if (contentValues != null) {
							contentValues.put(AixForecastsColumns.WEATHER_ICON,
									Integer.parseInt(parser.getAttributeValue(null, "number")));
						}
					} else if (parser.getName().equals("precipitation")) {
						if (contentValues != null) {
							contentValues.put(AixForecastsColumns.RAIN_VALUE,
									Float.parseFloat(
											parser.getAttributeValue(null, "value")));
							try {
								contentValues.put(AixForecastsColumns.RAIN_LOWVAL,
    									Float.parseFloat(
    											parser.getAttributeValue(null, "minvalue")));
							} catch (Exception e) {
								/* LOW VALUE IS OPTIONAL */
							}
							try {
    							contentValues.put(AixForecastsColumns.RAIN_HIGHVAL,
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
			contentValues.put(AixLocationsColumns.LAST_FORECAST_UPDATE, calendar.getTimeInMillis());
			contentValues.put(AixLocationsColumns.FORECAST_VALID_TO, forecastValidTo);
			contentValues.put(AixLocationsColumns.NEXT_FORECAST_UPDATE, nextUpdate);
			resolver.update(Uri.withAppendedPath(viewUri, AixViews.TWIG_LOCATION), contentValues, null, null);
		} catch (XmlPullParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

}
