package net.veierland.aix;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Rain implements Comparable<Rain> {
	
	public int weatherIcon;
	public long timeFrom, timeTo;
	public float value, minValue, maxValue;
	
	public Rain(long timeFrom, long timeTo, float value, float minValue, float maxValue, int weatherIcon) {
		this.timeFrom = timeFrom;
		this.timeTo = timeTo;
		this.value = value;
		this.minValue = minValue;
		this.maxValue = maxValue;
		this.weatherIcon = weatherIcon;
	}	
	
	public Rain(long timeFrom, long timeTo, String value, String minValue, String maxValue, String weatherIcon)
			throws Exception {
		if (timeFrom <= 0 || timeTo <= 0) {
			throw new Exception("Rain() constructor: Invalid timeFrom or timeTo value (" +
					timeFrom + ", " + timeTo + ")");
		}
		
		this.timeFrom = timeFrom;
		this.timeTo = timeTo;
		
		// Value is a required field
		try {
			this.value = Float.parseFloat(value);
		} catch (Exception e) {
			throw new Exception("Rain() constructor: Invalid rain value (" + value + ")");
		}
		
		try {
			this.minValue = Float.parseFloat(minValue);
		} catch (Exception e) {
			this.minValue = Float.NaN;
		}
		try {
			this.maxValue = Float.parseFloat(maxValue);
		} catch (Exception e) {
			this.maxValue = Float.NaN;
		}
		
		try {
			this.weatherIcon = Integer.parseInt(weatherIcon);
		} catch (Exception e) {
			this.weatherIcon = -1;
		}
	}

	@Override
	public String toString() {
		SimpleDateFormat sdf = new SimpleDateFormat();
		return	(timeTo - timeFrom) + " " +
				sdf.format(new Date(timeFrom)) + " " +
				sdf.format(new Date(timeTo)) + " " +
				value + " " + minValue + " " + maxValue + " " + weatherIcon;
	}

	@Override
	public int compareTo(Rain another) {
		long diffA = this.timeTo - this.timeFrom;
		long diffB = another.timeTo - another.timeFrom;
		if (diffA == diffB) return 0;
		return diffA < diffB ? 1 : -1;
	}
	
}
