package net.veierland.aix;

import net.veierland.aix.AixProvider.AixIntervalDataForecastColumns;
import android.database.Cursor;
import android.text.format.DateUtils;

public class IntervalData {
	
	public long timeAdded, timeFrom, timeTo;
	public int weatherIcon;
	public float rainValue, rainMinValue, rainMaxValue;
	
	public IntervalData() {
		
	}
	
	public IntervalData(long timeAdded, long timeFrom, long timeTo, int weatherIcon,
			float rainValue, float rainMinValue, float rainMaxValue)
	{
		this.timeAdded = timeAdded;
		this.timeFrom = timeFrom;
		this.timeTo = timeTo;
		this.weatherIcon = weatherIcon;
		this.rainValue = rainValue;
		this.rainMinValue = rainMinValue;
		this.rainMaxValue = rainMaxValue;
	}
	
	public static IntervalData buildFromCursor(Cursor c) throws Exception {
		long timeAdded = c.getLong(AixIntervalDataForecastColumns.TIME_ADDED_COLUMN);
		long timeFrom = c.getLong(AixIntervalDataForecastColumns.TIME_FROM_COLUMN);
		long timeTo = c.getLong(AixIntervalDataForecastColumns.TIME_TO_COLUMN);
		
		int weatherIcon = c.getInt(AixIntervalDataForecastColumns.WEATHER_ICON_COLUMN);
		
		float rainValue, rainMinValue, rainMaxValue;
		
		String rainValueString = c.getString(AixIntervalDataForecastColumns.RAIN_VALUE_COLUMN);
		try {
			rainValue = Float.parseFloat(rainValueString);
		} catch (Exception e) {
			//throw new Exception("Rain() constructor: Invalid rain value (" + rainValueString + ")");
			rainValue = Float.NaN;
		}
		
		try {
			rainMinValue = Float.parseFloat(
					c.getString(AixIntervalDataForecastColumns.RAIN_MINVAL_COLUMN));
		} catch (Exception e) {
			rainMinValue = Float.NaN;
		}
		
		try {
			rainMaxValue = Float.parseFloat(
					c.getString(AixIntervalDataForecastColumns.RAIN_MAXVAL_COLUMN));
		} catch (Exception e) {
			rainMaxValue = Float.NaN;
		}
		
		return new IntervalData(
				timeAdded, timeFrom, timeTo, weatherIcon, rainValue, rainMinValue, rainMaxValue);
	}
	
	public int lengthInHours() {
		return (int)((timeTo - timeFrom) / DateUtils.HOUR_IN_MILLIS);
	}

}
