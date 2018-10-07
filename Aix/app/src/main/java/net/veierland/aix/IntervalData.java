package net.veierland.aix;

import net.veierland.aix.AixProvider.AixIntervalDataForecastColumns;
import net.veierland.aix.AixProvider.AixIntervalDataForecasts;
import android.content.ContentValues;
import android.database.Cursor;
import android.text.format.DateUtils;

public class IntervalData {
	
	public Long timeAdded = null;
	public Long timeFrom = null;
	public Long timeTo = null;
	
	public Float rainValue = null;
	public Float rainMinValue = null;
	public Float rainMaxValue = null;
	
	public Integer weatherIcon = null;
	
	public IntervalData() { }
	
	
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
	
	public ContentValues buildContentValues(long locationId)
	{
		ContentValues contentValues = new ContentValues();
		
		contentValues.put(AixIntervalDataForecasts.LOCATION, locationId);
		
		if (timeAdded != null) contentValues.put(AixIntervalDataForecastColumns.TIME_ADDED, timeAdded);
		if (timeFrom != null) contentValues.put(AixIntervalDataForecastColumns.TIME_FROM, timeFrom);
		if (timeTo != null) contentValues.put(AixIntervalDataForecastColumns.TIME_TO, timeTo);
		if (rainValue != null) contentValues.put(AixIntervalDataForecastColumns.RAIN_VALUE, rainValue);
		if (rainMinValue != null) contentValues.put(AixIntervalDataForecastColumns.RAIN_MINVAL, rainMinValue);
		if (rainMaxValue != null) contentValues.put(AixIntervalDataForecastColumns.RAIN_MAXVAL, rainMaxValue);
		if (weatherIcon != null) contentValues.put(AixIntervalDataForecastColumns.WEATHER_ICON, weatherIcon);
		
		return contentValues;
	}
	
	public static IntervalData buildFromCursor(Cursor c) {
		IntervalData intervalData = new IntervalData();
		
		int columIndex = c.getColumnIndex(AixIntervalDataForecastColumns.TIME_ADDED);
		if (columIndex != -1 && !c.isNull(columIndex)) intervalData.timeAdded = c.getLong(columIndex);
		
		columIndex = c.getColumnIndex(AixIntervalDataForecastColumns.TIME_FROM);
		if (columIndex != -1 && !c.isNull(columIndex)) intervalData.timeFrom = c.getLong(columIndex);
		
		columIndex = c.getColumnIndex(AixIntervalDataForecastColumns.TIME_TO);
		if (columIndex != -1 && !c.isNull(columIndex)) intervalData.timeTo = c.getLong(columIndex);
		
		columIndex = c.getColumnIndex(AixIntervalDataForecastColumns.WEATHER_ICON);
		if (columIndex != -1 && !c.isNull(columIndex)) intervalData.weatherIcon = c.getInt(columIndex);
		
		columIndex = c.getColumnIndex(AixIntervalDataForecastColumns.RAIN_VALUE);
		if (columIndex != -1 && !c.isNull(columIndex)) intervalData.rainValue = c.getFloat(columIndex);
		
		columIndex = c.getColumnIndex(AixIntervalDataForecastColumns.RAIN_MINVAL);
		if (columIndex != -1 && !c.isNull(columIndex)) intervalData.rainMinValue = c.getFloat(columIndex);
		
		columIndex = c.getColumnIndex(AixIntervalDataForecastColumns.RAIN_MAXVAL);
		if (columIndex != -1 && !c.isNull(columIndex)) intervalData.rainMaxValue = c.getFloat(columIndex);
		
		return intervalData;
	}
	
	public int getLengthInHours() {
		return (int)((timeTo - timeFrom) / DateUtils.HOUR_IN_MILLIS);
	}

}
