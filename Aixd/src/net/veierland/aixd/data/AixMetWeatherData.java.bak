package net.veierland.aixd.data;

import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TimeZone;

import net.veierland.aixd.AixSettings;
import net.veierland.aixd.AixUpdate;
import net.veierland.aixd.AixUtils;
import net.veierland.aixd.AixProvider.AixIntervalDataForecastColumns;
import net.veierland.aixd.AixProvider.AixIntervalDataForecasts;
import net.veierland.aixd.AixProvider.AixPointDataForecastColumns;
import net.veierland.aixd.AixProvider.AixPointDataForecasts;
import net.veierland.aixd.util.AixLocationInfo;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.xmlpull.v1.XmlPullParser;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.util.Log;
import android.util.Xml;

public class AixMetWeatherData implements AixDataSource {

	public static final String TAG = "AixMetWeatherData";
	
	private Context mContext;
	//private AixSettings mAixSettings;
	private AixUpdate mAixUpdate;
	
	private AixMetWeatherData(Context context, AixUpdate aixUpdate, AixSettings aixSettings)
	{
		mContext = context;
		mAixUpdate = aixUpdate;
		//mAixSettings = aixSettings;
	}
	
	public static AixMetWeatherData build(Context context, AixUpdate aixUpdate, AixSettings aixSettings)
	{
		return new AixMetWeatherData(context, aixUpdate, aixSettings);
	}
	
	public void update(AixLocationInfo aixLocationInfo, long currentUtcTime)
			throws AixDataUpdateException
	{
		try {
			Log.d(TAG, "update(): Started update operation. (aixLocationInfo=" + aixLocationInfo + ",currentUtcTime=" + currentUtcTime + ")");
			
			mAixUpdate.updateWidgetRemoteViews("Downloading NMI weather data...", false);
			
			Double latitude = aixLocationInfo.getLatitude();
			Double longitude = aixLocationInfo.getLongitude();
			
			if (latitude == null || longitude == null)
			{
				throw new AixDataUpdateException("Missing location information. Latitude/Longitude was null");
			}
			
			String url = String.format(
					Locale.US,
					"http://api.met.no/weatherapi/locationforecast/1.8/?lat=%.5f;lon=%.5f",
					latitude.doubleValue(),
					longitude.doubleValue());
			
			Log.d(TAG, "Attempting to download weather data from URL=" + url);
			
			HttpClient httpClient = AixUtils.setupHttpClient();
			HttpGet httpGet = AixUtils.buildGzipHttpGet(url);
			HttpResponse httpResponse = httpClient.execute(httpGet);
			InputStream content = AixUtils.getGzipInputStream(httpResponse);
			
			mAixUpdate.updateWidgetRemoteViews("Parsing NMI weather data...", false);
			
			TimeZone utcTimeZone = TimeZone.getTimeZone("UTC");
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
			dateFormat.setTimeZone(utcTimeZone);
			
			ArrayList<ContentValues> pointDataValues = new ArrayList<ContentValues>();
			ArrayList<ContentValues> intervalDataValues = new ArrayList<ContentValues>();
			ArrayList<ContentValues> currentList = null;
			
			long nextUpdate = -1;
			long forecastValidTo = -1;
			
			ContentValues contentValues = null;
			
			XmlPullParser parser = Xml.newPullParser();
			
			long startTime = System.currentTimeMillis();
			
			parser.setInput(content, null);
			int eventType = parser.getEventType();
			
			while (eventType != XmlPullParser.END_DOCUMENT) {
				switch (eventType) {
				case XmlPullParser.END_TAG:
					if (parser.getName().equals("time") && contentValues != null)
					{
						if (currentList != null && contentValues != null) {
							currentList.add(contentValues);
						}
					}
					break;
				case XmlPullParser.START_TAG:
					if (parser.getName().equals("time")) {
						contentValues = new ContentValues();
						
						String fromString = parser.getAttributeValue(null, "from");
						String toString = parser.getAttributeValue(null, "to");
						
						try {
    						long from = dateFormat.parse(fromString).getTime();
							long to = dateFormat.parse(toString).getTime();
							
							if (from != to) {
								currentList = intervalDataValues;
								contentValues.put(AixIntervalDataForecastColumns.LOCATION, aixLocationInfo.getId());
								contentValues.put(AixIntervalDataForecastColumns.TIME_ADDED, currentUtcTime);
								contentValues.put(AixIntervalDataForecastColumns.TIME_FROM, from);
								contentValues.put(AixIntervalDataForecastColumns.TIME_TO, to);
							} else {
								currentList = pointDataValues;
								contentValues.put(AixPointDataForecastColumns.LOCATION, aixLocationInfo.getId());
								contentValues.put(AixPointDataForecastColumns.TIME_ADDED, currentUtcTime);
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
			
			ContentResolver resolver = mContext.getContentResolver();
			resolver.bulkInsert(AixPointDataForecasts.CONTENT_URI, pointDataValues.toArray(new ContentValues[pointDataValues.size()]));
			resolver.bulkInsert(AixIntervalDataForecasts.CONTENT_URI, intervalDataValues.toArray(new ContentValues[intervalDataValues.size()]));

			// Remove duplicates from weather data
			int numRedundantPointDataEntries = resolver.update(AixPointDataForecasts.CONTENT_URI, null, null, null);
			int numRedundantIntervalDataEntries = resolver.update(AixIntervalDataForecasts.CONTENT_URI, null, null, null);
			
			Log.d(TAG, String.format("update(): %d new PointData entries! %d redundant entries removed.", pointDataValues.size(), numRedundantPointDataEntries));
			Log.d(TAG, String.format("update(): %d new IntervalData entries! %d redundant entries removed.", intervalDataValues.size(), numRedundantIntervalDataEntries));
			
			aixLocationInfo.setLastForecastUpdate(currentUtcTime);
			aixLocationInfo.setForecastValidTo(forecastValidTo);
			aixLocationInfo.setNextForecastUpdate(nextUpdate);
			aixLocationInfo.commit(mContext);
			
			long endTime = System.currentTimeMillis();
			
			Log.d(TAG, "Time spent parsing MET data = " + (endTime - startTime) + " ms");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new AixDataUpdateException();
		}
	}
	
}
