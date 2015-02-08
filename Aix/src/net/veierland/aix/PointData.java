package net.veierland.aix;

import net.veierland.aix.AixProvider.AixPointDataForecastColumns;
import net.veierland.aix.AixProvider.AixPointDataForecasts;
import android.content.ContentValues;
import android.database.Cursor;

public class PointData {

	public Long timeAdded = null;
	public Long time = null;
	
	public Float temperature = null;
	public Float humidity = null;
	public Float pressure = null;
	
	public PointData() { }
	
	public ContentValues buildContentValues(long locationId)
	{
		ContentValues contentValues = new ContentValues();
		
		contentValues.put(AixPointDataForecasts.LOCATION, locationId);
		
		if (timeAdded != null) contentValues.put(AixPointDataForecasts.TIME_ADDED, timeAdded);
		if (time != null) contentValues.put(AixPointDataForecasts.TIME, time); 
		if (temperature != null) contentValues.put(AixPointDataForecastColumns.TEMPERATURE, temperature);
		if (humidity != null) contentValues.put(AixPointDataForecastColumns.HUMIDITY, humidity);
		if (pressure != null) contentValues.put(AixPointDataForecastColumns.PRESSURE, pressure);
		
		return contentValues;
	}
	
	public static PointData buildFromCursor(Cursor c) {
		PointData pointData = new PointData();
		
		int columnIndex = c.getColumnIndex(AixPointDataForecastColumns.TIME_ADDED);
		if (columnIndex != -1 && !c.isNull(columnIndex)) pointData.timeAdded = c.getLong(columnIndex);
		
		columnIndex = c.getColumnIndex(AixPointDataForecastColumns.TIME);
		if (columnIndex != -1 && !c.isNull(columnIndex)) pointData.time = c.getLong(columnIndex);
		
		columnIndex = c.getColumnIndex(AixPointDataForecastColumns.TEMPERATURE);
		if (columnIndex != -1 && !c.isNull(columnIndex)) pointData.temperature = c.getFloat(columnIndex);
		
		columnIndex = c.getColumnIndex(AixPointDataForecastColumns.HUMIDITY);
		if (columnIndex != -1 && !c.isNull(columnIndex)) pointData.humidity = c.getFloat(columnIndex);
		
		columnIndex = c.getColumnIndex(AixPointDataForecastColumns.PRESSURE);
		if (columnIndex != -1 && !c.isNull(columnIndex)) pointData.pressure = c.getFloat(columnIndex);

		return pointData;
	}
	
}
