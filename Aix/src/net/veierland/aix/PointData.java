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
		
		try {
			p.mTemperature = Float.parseFloat(c.getString(AixPointDataForecastColumns.TEMPERATURE_COLUMN));
		} catch (Exception e) {
			p.mTemperature = Float.NaN;
		}
		try {
			p.mHumidity = Float.parseFloat(c.getString(AixPointDataForecastColumns.HUMIDITY_COLUMN));
		} catch (Exception e) {
			p.mHumidity = Float.NaN;
		}
		try {
			p.mPressure = Float.parseFloat(c.getString(AixPointDataForecastColumns.PRESSURE_COLUMN));
		} catch (Exception e) {
			p.mPressure = Float.NaN;
		}

		return p;
	}
	
}
