package net.veierland.aix;

import net.veierland.aix.AixProvider.AixSunMoonDataColumns;
import android.database.Cursor;

public class SunMoonData {
	
	public long mTimeAdded;
	public long mDate;
	public long mSunRise, mSunSet;
	public long mMoonRise, mMoonSet;
	public int mMoonPhase;
	
	public SunMoonData() {
		
	}
	
	public static SunMoonData buildFromCursor(Cursor c) {
		SunMoonData smd = new SunMoonData();
		smd.mTimeAdded = c.getLong(AixSunMoonDataColumns.TIME_ADDED_COLUMN);
		smd.mDate = c.getLong(AixSunMoonDataColumns.DATE_COLUMN);
		smd.mSunRise = c.getLong(AixSunMoonDataColumns.SUN_RISE_COLUMN);
		smd.mSunSet = c.getLong(AixSunMoonDataColumns.SUN_SET_COLUMN);
		smd.mMoonRise = c.getLong(AixSunMoonDataColumns.MOON_RISE_COLUMN);
		smd.mMoonSet = c.getLong(AixSunMoonDataColumns.MOON_SET_COLUMN);
		smd.mMoonPhase = c.getInt(AixSunMoonDataColumns.MOON_PHASE_COLUMN);
		return smd;
	}
	
}
