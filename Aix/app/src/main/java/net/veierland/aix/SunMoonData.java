package net.veierland.aix;

import net.veierland.aix.AixProvider.AixSunMoonDataColumns;
import android.database.Cursor;

public class SunMoonData {
	
	public long timeAdded;
	public long date;
	public long sunRise, sunSet;
	public long moonRise, moonSet;
	public int moonPhase;
	
	public SunMoonData() {
		
	}
	
	public static SunMoonData buildFromCursor(Cursor c) {
		SunMoonData smd = new SunMoonData();
		smd.timeAdded = c.getLong(AixSunMoonDataColumns.TIME_ADDED_COLUMN);
		smd.date = c.getLong(AixSunMoonDataColumns.DATE_COLUMN);
		smd.sunRise = c.getLong(AixSunMoonDataColumns.SUN_RISE_COLUMN);
		smd.sunSet = c.getLong(AixSunMoonDataColumns.SUN_SET_COLUMN);
		smd.moonRise = c.getLong(AixSunMoonDataColumns.MOON_RISE_COLUMN);
		smd.moonSet = c.getLong(AixSunMoonDataColumns.MOON_SET_COLUMN);
		smd.moonPhase = c.getInt(AixSunMoonDataColumns.MOON_PHASE_COLUMN);
		return smd;
	}
	
}
