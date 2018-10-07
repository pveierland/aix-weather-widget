package net.veierland.aixd.data;

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

import net.veierland.aixd.AixProvider.AixSunMoonData;
import net.veierland.aixd.AixProvider.AixSunMoonDataColumns;
import net.veierland.aixd.AixSettings;
import net.veierland.aixd.AixUpdate;
import net.veierland.aixd.AixUtils;
import net.veierland.aixd.util.AixLocationInfo;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.util.Xml;

public class AixMetSunTimeData implements AixDataSource {

	public final static String TAG = "AixMetSunTimeData";

	private final static int NUM_DAYS_BUFFERED = 5;

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
		
		mTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
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
			cursor = contentResolver.query(
					AixSunMoonData.CONTENT_URI,
					null,
					AixSunMoonDataColumns.LOCATION + "=" + locationId + " AND " +
					AixSunMoonDataColumns.DATE + ">=" + mStartDate + " AND " +
					AixSunMoonDataColumns.DATE + "<=" + mEndDate,
					null, null);
			
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
	
	private List<ContentValues> parseData(InputStream content, long locationId, long currentUtcTime)
			throws ParseException, XmlPullParserException, IOException
	{
		List<ContentValues> contentValuesList = new ArrayList<ContentValues>();
		ContentValues contentValues = null;
		
		XmlPullParser parser = Xml.newPullParser();
		parser.setInput(content, null);
		
		int eventType = parser.getEventType();
		
		while (eventType != XmlPullParser.END_DOCUMENT) {
			switch (eventType) {
			case XmlPullParser.END_TAG:
				if (parser.getName().equalsIgnoreCase("time") && contentValues != null)
				{
					contentValuesList.add(contentValues);
					contentValues = null;
				}
				break;
			case XmlPullParser.START_TAG:
				if (parser.getName().equalsIgnoreCase("time"))
				{
					String dateString = parser.getAttributeValue(null, "date");
					long date = mDateFormat.parse(dateString).getTime();
					
					contentValues = new ContentValues();
					contentValues.put(AixSunMoonDataColumns.LOCATION, locationId);
					contentValues.put(AixSunMoonDataColumns.TIME_ADDED, currentUtcTime);
					contentValues.put(AixSunMoonDataColumns.DATE, date);
				}
				else if (parser.getName().equalsIgnoreCase("sun") && contentValues != null)
				{
					String neverRise = parser.getAttributeValue(null, "never_rise");
					String rise      = parser.getAttributeValue(null, "rise");
					String neverSet  = parser.getAttributeValue(null, "never_set");
					String set       = parser.getAttributeValue(null, "set");

					long riseValue = 0;
					long setValue  = 0;

					if (neverRise != null && neverRise.equalsIgnoreCase("true"))
					{
						// Polar Night
						riseValue = AixSunMoonData.NEVER_RISE;
						setValue  = AixSunMoonData.NEVER_SET;
					}
					else if (neverSet != null && neverSet.equalsIgnoreCase("true"))
					{
						// Midnight Sun
						setValue = AixSunMoonData.NEVER_SET;
					}
					else
					{
						if (rise != null)
						{
							try
							{
								riseValue = mTimeFormat.parse(rise).getTime();
							}
							catch (Exception e)
							{
								Log.e(TAG, "Sun rise time parse failed: " + e.getMessage());
							}
						}

						if (set != null)
						{
							try
							{
								setValue = mTimeFormat.parse(set).getTime();
							}
							catch (Exception e)
							{
								Log.e(TAG, "Sun set time parse failed: " + e.getMessage());
							}
						}
					}

					contentValues.put(AixSunMoonDataColumns.SUN_RISE, riseValue);
					contentValues.put(AixSunMoonDataColumns.SUN_SET, setValue);
				}
				else if (parser.getName().equals("moon") && contentValues != null)
				{
					String neverRise = parser.getAttributeValue(null, "never_rise");
					String rise      = parser.getAttributeValue(null, "rise");
					String neverSet  = parser.getAttributeValue(null, "never_set");
					String set       = parser.getAttributeValue(null, "set");

					long riseValue = 0;
					long setValue  = 0;

					if (neverRise != null && neverRise.equalsIgnoreCase("true"))
					{
						riseValue = AixSunMoonData.NEVER_RISE;
						setValue  = AixSunMoonData.NEVER_SET;
					}
					else if (neverSet != null && neverSet.equalsIgnoreCase("true"))
					{
						setValue = AixSunMoonData.NEVER_SET;
					}
					else
					{
						if (rise != null)
						{
							try
							{
								riseValue = mTimeFormat.parse(rise).getTime();
							}
							catch (Exception e)
							{
								Log.e(TAG, "Moon rise time parse failed: " + e.getMessage());
							}
						}

						if (set != null)
						{
							try
							{
								setValue = mTimeFormat.parse(set).getTime();
							}
							catch (Exception e)
							{
								Log.e(TAG, "Moon set time parse failed: " + e.getMessage());
							}
						}
					}

					contentValues.put(AixSunMoonDataColumns.MOON_RISE, riseValue);
					contentValues.put(AixSunMoonDataColumns.MOON_SET, setValue);

					String moonPhase = parser.getAttributeValue(null, "phase");
					int moonPhaseData = AixSunMoonData.NO_MOON_PHASE_DATA;
					
					if (moonPhase != null) {
						moonPhase = moonPhase.toLowerCase();
						if (moonPhaseMap.containsKey(moonPhase.toLowerCase())) {
							moonPhaseData = moonPhaseMap.get(moonPhase);
						}
					}
					
					contentValues.put(AixSunMoonDataColumns.MOON_PHASE, moonPhaseData);
				}
				break;
			}
			eventType = parser.next();
		}
		
		return contentValuesList;
	}

	private void setupDateParameters(long time)
	{
		Calendar calendar = Calendar.getInstance(mUtcTimeZone);
		
		calendar.setTimeInMillis(time);
		AixUtils.truncateDay(calendar);
		
		calendar.add(Calendar.DAY_OF_YEAR, -1);
		mStartDate = calendar.getTimeInMillis();
		
		calendar.add(Calendar.DAY_OF_YEAR, NUM_DAYS_BUFFERED - 1);
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
			
			if (numExistingDataSets < NUM_DAYS_BUFFERED)
			{
				String url = String.format(
						Locale.US,
						"http://api.met.no/weatherapi/sunrise/1.1/?lat=%.5f;lon=%.5f;from=%s;to=%s",
						latitude.doubleValue(),
						longitude.doubleValue(),
						mDateFormat.format(mStartDate),
						mDateFormat.format(mEndDate));
				
				HttpClient httpClient = AixUtils.setupHttpClient(mContext);
				
				HttpGet httpGet = AixUtils.buildGzipHttpGet(url);
				HttpResponse httpResponse = httpClient.execute(httpGet);
				InputStream content = AixUtils.getGzipInputStream(httpResponse);

				List<ContentValues> contentValuesList = parseData(content, aixLocationInfo.getId(), currentUtcTime);

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
