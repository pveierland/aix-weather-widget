package net.veierland.aix;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Rain implements Comparable<Rain> {
	
	public int weatherIcon;
	public long timeFrom, timeTo;
	public float value, minValue, maxValue;
	
	public Rain() {
		
	}
	
	public Rain(long timeFrom, long timeTo, float value, float minValue, float maxValue,
			boolean isInches, float scale, int weatherIcon)
	{
		this.timeFrom = timeFrom;
		this.timeTo = timeTo;
		if (isInches) {
			this.value = value / 25.4f / scale;
			this.minValue = minValue / 25.4f / scale;
			this.maxValue = maxValue / 25.4f / scale;
		} else {
			this.value = value / scale;
			this.minValue = minValue / scale;
			this.maxValue = maxValue / scale;
		}
		this.weatherIcon = weatherIcon;
	}
	
	public Rain(long timeFrom, long timeTo, String value, String minValue, String maxValue,
			boolean isInches, float scale, String weatherIcon) throws Exception
	{
		if (timeFrom <= 0 || timeTo <= 0) {
			throw new Exception("Rain() constructor: Invalid timeFrom or timeTo value (" +
					timeFrom + ", " + timeTo + ")");
		}
		
		this.timeFrom = timeFrom;
		this.timeTo = timeTo;
		
		// Value is a required field
		try {
			float f = Float.parseFloat(value);
			if (isInches) {
				this.value = f / 25.4f / scale;
			} else {
				this.value = f / scale;
			}
		} catch (Exception e) {
			throw new Exception("Rain() constructor: Invalid rain value (" + value + ")");
		}
		
		try {
			float f = Float.parseFloat(minValue);
			if (isInches) {
				this.minValue = f / 25.4f / scale;
			} else {
				this.minValue = f / scale;
			}
		} catch (Exception e) {
			this.minValue = Float.NaN;
		}
		try {
			float f = Float.parseFloat(maxValue);
			if (isInches) {
				this.maxValue = f / 25.4f / scale;
			} else {
				this.maxValue = f / scale;
			}
		} catch (Exception e) {
			this.maxValue = Float.NaN;
		}
		
		try {
			this.weatherIcon = Integer.parseInt(weatherIcon);
		} catch (Exception e) {
			this.weatherIcon = -1;
		}
	}

	public long getDiff() {
		return this.timeTo - this.timeFrom;
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
