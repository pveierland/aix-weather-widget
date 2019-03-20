package net.veierland.aix.data;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import net.veierland.aix.AixProvider.AixLocations;
import net.veierland.aix.AixProvider.AixSunMoonData;
import net.veierland.aix.AixProvider.AixSunMoonDataColumns;
import net.veierland.aix.AixSettings;
import net.veierland.aix.AixUpdate;
import net.veierland.aix.AixUtils;
import net.veierland.aix.util.AixLocationInfo;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.util.Xml;

public class AixMetSunTimeData implements AixDataSource {

	public final static String TAG = "AixMetSunTimeData";

	private final static int NUM_DAYS_MINIMUM = 5;
	private final static int NUM_DAYS_REQUEST = 15;

	@SuppressWarnings("serial")
	private Map<String, Integer> moonPhaseMap = new HashMap<String, Integer>() {{
		put("new moon", AixSunMoonData.NEW_MOON);
		put("waxing crescent", AixSunMoonData.WAXING_CRESCENT);
		put("first quarter", AixSunMoonData.FIRST_QUARTER);
		put("waxing gibbous", AixSunMoonData.WAXING_GIBBOUS);
		put("full moon", AixSunMoonData.FULL_MOON);
		put("waning gibbous", AixSunMoonData.WANING_GIBBOUS);
		put("third quarter", AixSunMoonData.LAST_QUARTER);
		put("waning crescent", AixSunMoonData.WANING_CRESCENT);
	}};

	private Context mContext;

	//private AixSettings mAixSettings;
	private AixUpdate mAixUpdate;

	private SimpleDateFormat mDateFormat;
	private SimpleDateFormat mTimeFormat;

	private TimeZone mUtcTimeZone;

	private long mStartDate;
	private long mEndDate;
	
	private AixMetSunTimeData(Context context, AixUpdate aixUpdate, AixSettings aixSettings)
	{
		mContext = context;
		mAixUpdate = aixUpdate;
		//mAixSettings = aixSettings;
		
		mUtcTimeZone = TimeZone.getTimeZone("UTC");
		
		mDateFormat = new SimpleDateFormat("yyyy-MM-dd");
		mDateFormat.setTimeZone(mUtcTimeZone);
		
		mTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		mTimeFormat.setTimeZone(mUtcTimeZone);
	}
	
	public static AixMetSunTimeData build(Context context, AixUpdate aixUpdate, AixSettings aixSettings)
	{
		return new AixMetSunTimeData(context, aixUpdate, aixSettings);
	}
	
	private int getNumExistingDataSets(long locationId)
	{
		ContentResolver contentResolver = mContext.getContentResolver();
		
		Cursor cursor = null;
		
		try {
			final Uri uri = AixLocations.CONTENT_URI.buildUpon()
					.appendPath(Long.toString(locationId))
					.appendPath(AixLocations.TWIG_SUNMOONDATA)
					.appendQueryParameter("start", Long.toString(mStartDate))
					.appendQueryParameter("end", Long.toString(mEndDate)).build();

			cursor = contentResolver.query(uri, null, null, null, null);
			
			return cursor.getCount();
		}
		finally
		{
			if (cursor != null)
			{
				cursor.close();
			}
		}
	}

	private List<ContentValues> parseData(
			InputStream content, long locationId, long currentUtcTime, int maxDays)
			throws ParseException, XmlPullParserException, IOException
	{
		List<ContentValues> contentValuesList = new ArrayList<ContentValues>();
		ContentValues contentValues = null;

		float solarnoonElevationValue = 0.0f;
		Float moonElevationAtStartOfDay = null;
		
		XmlPullParser parser = Xml.newPullParser();
		parser.setInput(content, null);
		
		int eventType = parser.getEventType();
		
		while (eventType != XmlPullParser.END_DOCUMENT) {
			switch (eventType) {
			case XmlPullParser.END_TAG:
				if (parser.getName().equalsIgnoreCase("time") && contentValues != null)
				{
					if (!contentValues.containsKey(AixSunMoonDataColumns.SUN_RISE))
					{
						contentValues.put(AixSunMoonDataColumns.SUN_RISE, solarnoonElevationValue >= 0.0f ? 0 : AixSunMoonData.NEVER_RISE);
					}

					if (!contentValues.containsKey(AixSunMoonDataColumns.SUN_SET))
					{
						contentValues.put(AixSunMoonDataColumns.SUN_SET, AixSunMoonData.NEVER_SET);
					}

					if (!contentValues.containsKey(AixSunMoonDataColumns.MOON_RISE))
					{
						contentValues.put(
							AixSunMoonDataColumns.MOON_RISE,
							(moonElevationAtStartOfDay != null && moonElevationAtStartOfDay >= 0.0f) ? 0 : AixSunMoonData.NEVER_RISE);
					}

					if (!contentValues.containsKey(AixSunMoonDataColumns.MOON_SET))
					{
						contentValues.put(AixSunMoonDataColumns.MOON_SET, AixSunMoonData.NEVER_SET);
					}

					contentValuesList.add(contentValues);
					contentValues = null;
					moonElevationAtStartOfDay = null;
				}
				break;
			case XmlPullParser.START_TAG:
				if (parser.getName().equalsIgnoreCase("time"))
				{
					if (contentValuesList.size() < maxDays)
					{
						String dateString = parser.getAttributeValue(null, "date");
						long date = mDateFormat.parse(dateString).getTime();

						contentValues = new ContentValues();
						contentValues.put(AixSunMoonDataColumns.LOCATION, locationId);
						contentValues.put(AixSunMoonDataColumns.TIME_ADDED, currentUtcTime);
						contentValues.put(AixSunMoonDataColumns.DATE, date);
					}
				}
				else if (contentValues != null)
				{
					String time = parser.getAttributeValue(null, "time");

					if (time != null)
					{
						long timeValue = mTimeFormat.parse(time.substring(0, 19)).getTime();

						if (parser.getName().equalsIgnoreCase("sunrise"))
						{
							contentValues.put(AixSunMoonDataColumns.SUN_RISE, timeValue);
						}
						else if (parser.getName().equalsIgnoreCase("sunset"))
						{
							contentValues.put(AixSunMoonDataColumns.SUN_SET, timeValue);
						}
						else if (parser.getName().equalsIgnoreCase("solarnoon"))
						{
							solarnoonElevationValue = Float.parseFloat(
								parser.getAttributeValue(null, "elevation"));
						}
						else if (parser.getName().equalsIgnoreCase("moonrise"))
						{
							contentValues.put(AixSunMoonDataColumns.MOON_RISE, timeValue);
						}
						else if (parser.getName().equalsIgnoreCase("moonset"))
						{
							contentValues.put(AixSunMoonDataColumns.MOON_SET, timeValue);
						}
						else if (parser.getName().equalsIgnoreCase("moonposition"))
						{
							moonElevationAtStartOfDay = Float.parseFloat(
									parser.getAttributeValue(null, "elevation"));

							float moonphaseValue = Float.parseFloat(
									parser.getAttributeValue(null, "phase"));

							contentValues.put(
									AixSunMoonDataColumns.MOON_PHASE,
									parseMoonPhaseValue(moonphaseValue, parser.getLineNumber()));
						}
					}
				}
				break;
			}
			eventType = parser.next();
		}
		
		return contentValuesList;
	}

	private int parseMoonPhaseValue(float value, int parserLineNumber) throws ParseException
	{
		if (value >= 0.0 && value < 0.5) {
			return AixSunMoonData.NEW_MOON;
		}
		else if (value >= 0.5 && value < 20.0) {
			return AixSunMoonData.WAXING_CRESCENT;
		}
		else if (value >= 20.0 && value < 30.0) {
			return AixSunMoonData.FIRST_QUARTER;
		}
		else if (value >= 30.0 && value < 49.5) {
			return AixSunMoonData.WAXING_GIBBOUS;
		}
		else if (value >= 49.5 && value < 50.5) {
			return AixSunMoonData.FULL_MOON;
		}
		else if (value >= 50.5 && value < 70.0) {
			return AixSunMoonData.WANING_GIBBOUS;
		}
		else if (value >= 70.0 && value < 80.0) {
			return AixSunMoonData.LAST_QUARTER;
		}
		else if (value >= 80.0 && value < 99.5) {
			return AixSunMoonData.WANING_CRESCENT;
		}
		else if (value > 99.5 && value <= 100.0) {
			return AixSunMoonData.NEW_MOON;
		}
		else {
			throw new ParseException(
				String.format("parseMoonPhaseValue: value %f out of range", value),
				parserLineNumber);
		}
	}

	private void setupDateParameters(long time)
	{
		Calendar calendar = Calendar.getInstance(mUtcTimeZone);
		
		calendar.setTimeInMillis(time);
		AixUtils.truncateDay(calendar);
		
		calendar.add(Calendar.DAY_OF_YEAR, -1);
		mStartDate = calendar.getTimeInMillis();
		
		calendar.add(Calendar.DAY_OF_YEAR, NUM_DAYS_REQUEST - 1);
		mEndDate = calendar.getTimeInMillis();
	}
	
	public void update(AixLocationInfo aixLocationInfo, long currentUtcTime)
			throws AixDataUpdateException
	{
		try {
			mAixUpdate.updateWidgetRemoteViews("Getting sun time data...", false);
			
			Double latitude = aixLocationInfo.getLatitude();
			Double longitude = aixLocationInfo.getLongitude();
			
			if (latitude == null || longitude == null)
			{
				throw new AixDataUpdateException("Missing location information. Latitude/Longitude was null");
			}
			
			setupDateParameters(currentUtcTime);
			
			int numExistingDataSets = getNumExistingDataSets(aixLocationInfo.getId());
			
			Log.d(TAG, String.format("update(): For location %s (%d), there are %d existing datasets.",
					aixLocationInfo.getTitle(), aixLocationInfo.getId(), numExistingDataSets));
			
			if (numExistingDataSets < NUM_DAYS_MINIMUM)
			{
				String url = String.format(
						Locale.US,
						"https://aa033wckd2azu8v41.api.met.no/weatherapi/sunrise/2.0/?lat=%.1f&lon=%.1f&date=%s&offset=+00:00&days=%d",
						latitude.doubleValue(),
						longitude.doubleValue(),
						mDateFormat.format(mStartDate),
						NUM_DAYS_REQUEST);

				HttpClient httpClient = AixUtils.setupHttpClient(mContext);
				HttpGet httpGet = AixUtils.buildGzipHttpGet(url);
				HttpResponse httpResponse = httpClient.execute(httpGet);

				if (httpResponse.getStatusLine().getStatusCode() == 429)
				{
					throw new AixDataUpdateException(url, AixDataUpdateException.Reason.RATE_LIMITED);
				}

				InputStream content = AixUtils.getGzipInputStream(httpResponse);

				List<ContentValues> contentValuesList = parseData(
						content, aixLocationInfo.getId(), currentUtcTime, NUM_DAYS_REQUEST);

				if (contentValuesList != null && contentValuesList.size() > 0)
				{
					updateDatabase(contentValuesList.toArray(new ContentValues[contentValuesList.size()]));
				}

				Log.d(TAG, String.format("update(): %d datasets were added to location %s (%d).",
						contentValuesList.size(), aixLocationInfo.getTitle(), aixLocationInfo.getId()));
			}
		}
		catch (Exception e)
		{
			if (aixLocationInfo != null)
			{
				Log.d(TAG, String.format("update(): " + e.getMessage() + " thrown for location %s (%d).",
						aixLocationInfo.getTitle(), aixLocationInfo.getId()));
			}
			throw new AixDataUpdateException();
		}
	}
	
	private void updateDatabase(ContentValues[] contentValuesArray)
	{
		ContentResolver contentResolver = mContext.getContentResolver();
		
		for (ContentValues contentValues : contentValuesArray)
		{
			contentResolver.insert(AixSunMoonData.CONTENT_URI, contentValues);
		}
	}
}
