package net.veierland.aix.data;

import java.io.InputStream;
import java.util.Locale;

import net.veierland.aix.AixSettings;
import net.veierland.aix.AixUpdate;
import net.veierland.aix.AixUtils;
import net.veierland.aix.util.AixLocationInfo;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

public class AixGeoNamesData implements AixDataSource {

	public final static String TAG = "AixGeoNamesData";
	
	private Context mContext;
	private AixSettings mAixSettings;
	private AixUpdate mAixUpdate;
	
	private AixGeoNamesData(Context context, AixUpdate aixUpdate, AixSettings aixSettings)
	{
		mContext = context;
		mAixUpdate = aixUpdate;
		mAixSettings = aixSettings;
	}
	
	public static AixGeoNamesData build(Context context, AixUpdate aixUpdate, AixSettings aixSettings)
	{
		return new AixGeoNamesData(context, aixUpdate, aixSettings);
	}
	
	public void update(AixLocationInfo aixLocationInfo, long currentUtcTime) throws AixDataUpdateException
	{
		String timeZone = aixLocationInfo.getTimeZone();
		String countryCode = mAixSettings.getLocationCountryCode(aixLocationInfo.getId());
		
		mAixUpdate.updateWidgetRemoteViews("Getting timezone data...", false);
		
		Double latitude = aixLocationInfo.getLatitude();
		Double longitude = aixLocationInfo.getLongitude();
		
		if (latitude == null || longitude == null)
		{
			throw new AixDataUpdateException("Missing location information. Latitude/Longitude was null");
		}
		
		if (timeZone != null) timeZone = timeZone.trim();
		if (countryCode != null) countryCode = countryCode.trim();
		
		if (timeZone == null || timeZone.length() == 0 || timeZone.equalsIgnoreCase("null") ||
		    countryCode == null || countryCode.length() == 0 || countryCode.equalsIgnoreCase("null"))
		{
			String url = String.format(
					Locale.US,
					"http://api.geonames.org/timezoneJSON?lat=%.5f&lng=%.5f&username=aix_widget",
					latitude.doubleValue(),
					longitude.doubleValue());
			
			Log.d(TAG, "Retrieving timezone data from URL=" + url);
			
			try
			{
				HttpClient httpClient = AixUtils.setupHttpClient(mContext);
				HttpGet httpGet = new HttpGet(url);
				HttpResponse response = httpClient.execute(httpGet);

				if (response.getStatusLine().getStatusCode() == 429)
				{
					throw new AixDataUpdateException(url, AixDataUpdateException.Reason.RATE_LIMITED);
				}

				InputStream content = response.getEntity().getContent();
				
				String input = AixUtils.convertStreamToString(content);
				
				JSONObject jObject = new JSONObject(input);
				
				timeZone = jObject.getString("timezoneId");
				countryCode = jObject.getString("countryCode");
				
				Log.d(TAG, "Parsed TimeZone='" + timeZone + "' CountryCode='" + countryCode + "'");
				
				mAixSettings.setLocationCountryCode(aixLocationInfo.getId(), countryCode);
				
				aixLocationInfo.setTimeZone(timeZone);
				aixLocationInfo.commit(mContext);
			}
			catch (Exception e)
			{
				Log.d(TAG, "Failed to retrieve timezone data. (" + e.getMessage() + ")");
				throw new AixDataUpdateException();
			}
		}
	}
	
}
