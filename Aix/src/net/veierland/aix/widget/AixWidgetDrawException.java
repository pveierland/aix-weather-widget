package net.veierland.aix.widget;

public class AixWidgetDrawException extends Exception {

	private int mErrorCode;
	
	public static final int MISSING_WEATHER_DATA = 22;
	public static final int INVALID_WIDGET_SIZE = 23;
	public static final int INVALID_TIMEZONE = 24;
	public static final int GRAPH_DIMENSION_FAIL = 25;
	
	public AixWidgetDrawException(String message, int errorCode) {
		super(message);
		mErrorCode = errorCode;
	}
	
	public static AixWidgetDrawException buildMissingWeatherDataException() {
		return new AixWidgetDrawException(
				"Not enough weather data to draw widget",
				MISSING_WEATHER_DATA);
	}
	
	public static AixWidgetDrawException buildInvalidWidgetSizeException() {
		return new AixWidgetDrawException(
				"Invalid widget size",
				INVALID_WIDGET_SIZE);
	}
	
	public static AixWidgetDrawException buildInvalidTimeZoneException(String timeZone) {
		return new AixWidgetDrawException(
				"Invalid timezone: " + timeZone,
				INVALID_TIMEZONE);
	}
	
	public static AixWidgetDrawException buildGraphDimensionFailure() {
		return new AixWidgetDrawException(
				"Failed to dimension graph",
				GRAPH_DIMENSION_FAIL);
	}
	
	public int getErrorCode() {
		return mErrorCode;
	}
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return getMessage() + " (" + mErrorCode + ")";
	}
	
}
