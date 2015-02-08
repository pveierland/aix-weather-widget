package net.veierland.aix;

import net.veierland.aix.AixProvider.AixPointDataForecastColumns;
import android.database.Cursor;

public class PointData {

	public long mTimeAdded, mTime;
	public float mTemperature, mHumidity, mPressure;
	
	public PointData() {
		
	}
	
	public static PointData buildFromCursor(Cursor c) {
		PointData p = new PointData();
		p.mTimeAdded = c.getLong(AixPointDataForecastColumns.TIME_ADDED_COLUMN);
		p.mTime = c.getLong(AixPointDataForecastColumns.TIME_COLUMN);
		p.mTemperature = c.getFloat(AixPointDataForecastColumns.TEMPERATURE_COLUMN);
		p.mHumidity = c.getFloat(AixPointDataForecastColumns.HUMIDITY_COLUMN);
		p.mPressure = c.getFloat(AixPointDataForecastColumns.PRESSURE_COLUMN);
		return p;
	}
	
}
