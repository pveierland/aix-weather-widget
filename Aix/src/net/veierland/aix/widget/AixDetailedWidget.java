package net.veierland.aix.widget;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import net.veierland.aix.AixProvider.AixForecasts;
import net.veierland.aix.AixProvider.AixForecastsColumns;
import net.veierland.aix.AixProvider.AixLocations;
import net.veierland.aix.AixProvider.AixLocationsColumns;
import net.veierland.aix.AixProvider.AixViews;
import net.veierland.aix.AixProvider.AixWidgetSettings;
import net.veierland.aix.AixProvider.AixWidgets;
import net.veierland.aix.AixProvider.AixWidgetsColumns;
import net.veierland.aix.R;
import net.veierland.aix.Rain;
import net.veierland.aix.Temperature;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.NinePatch;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Region.Op;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.NinePatchDrawable;
import android.net.Uri;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.util.Log;

public class AixDetailedWidget {
	
	private static final String TAG = "AixDetailedWidget";

	private static final int BACKGROUND_COLOR = 0;
	private static final int TEXT_COLOR = 1;
	private static final int PATTERN_COLOR = 2;
	private static final int DAY_COLOR = 3;
	private static final int NIGHT_COLOR = 4;
	private static final int GRID_COLOR = 5;
	private static final int GRID_OUTLINE_COLOR = 6;
	private static final int MAX_RAIN_COLOR = 7;
	private static final int MIN_RAIN_COLOR = 8;
	private static final int ABOVE_FREEZING_COLOR = 9;
	private static final int BELOW_FREEZING_COLOR = 10;
	
	public static boolean isPrime(long n) {
		boolean prime = true;
		for (long i = 3; i <= Math.sqrt(n); i += 2)
			if (n % i == 0) {
				prime = false;
				break;
			}
		if (( n%2 !=0 && prime && n > 2) || n == 2) {
			return true;
		} else {
			return false;
		}
	}
	
    private static NinePatchDrawable getNinePatchDrawable(Resources resources, int resId) {
        Bitmap bitmap = getBitmapUnscaled(resources, resId);
        NinePatch np = new NinePatch(bitmap, bitmap.getNinePatchChunk(), null);
        return new NinePatchDrawable(resources, np);
    }

    private static Bitmap getBitmapUnscaled(Resources resources, int resId) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inDensity = opts.inTargetDensity = resources.getDisplayMetrics().densityDpi;
        Bitmap bitmap = BitmapFactory.decodeResource(resources, resId, opts);
        return bitmap;
    }
	
	public static Bitmap buildView(final Context context, Uri widgetUri, Uri viewUri, boolean isLandscape) throws AixWidgetDrawException {
		Log.d(TAG, "Building Aix Widget view");
		
		long timerStart = System.currentTimeMillis();
		
		ContentResolver resolver = context.getContentResolver();
		
		// Get widget size
		int widgetSize = 0;
		Cursor widgetCursor = resolver.query(widgetUri, null, null, null, null);
		if (widgetCursor != null) {
			if (widgetCursor.moveToFirst()) {
				widgetSize = widgetCursor.getInt(AixWidgetsColumns.SIZE_COLUMN);
			}
			widgetCursor.close();
		}
		
		if (widgetSize == AixWidgetsColumns.SIZE_INVALID) {
			throw AixWidgetDrawException.buildInvalidWidgetSizeException();
		}
		
		// Set up default colors
		final Resources resources = context.getResources();
		final int[] colors = new int[11];
		colors[BACKGROUND_COLOR] = resources.getColor(R.color.background_default);
		colors[TEXT_COLOR] = resources.getColor(R.color.text_default);
		colors[PATTERN_COLOR] = resources.getColor(R.color.pattern_default);
		colors[DAY_COLOR] = resources.getColor(R.color.day_default);
		colors[NIGHT_COLOR] = resources.getColor(R.color.night_default);
		colors[GRID_COLOR] = resources.getColor(R.color.grid_default);
		colors[GRID_OUTLINE_COLOR] = resources.getColor(R.color.grid_outline_default);
		colors[MAX_RAIN_COLOR] = resources.getColor(R.color.maximum_rain_default);
		colors[MIN_RAIN_COLOR] = resources.getColor(R.color.minimum_rain_default);
		colors[ABOVE_FREEZING_COLOR] = resources.getColor(R.color.above_freezing_default);
		colors[BELOW_FREEZING_COLOR] = resources.getColor(R.color.below_freezing_default);
		
		// Get widget settings
		Cursor widgetSettingsCursor = resolver.query(
				Uri.withAppendedPath(widgetUri, AixWidgets.TWIG_SETTINGS),
				null, null, null, null);
		
		Map<String, Integer> colorNameMap = new HashMap<String, Integer>() {{
			put(context.getString(R.string.background_color_int), BACKGROUND_COLOR);
			put(context.getString(R.string.text_color_int), TEXT_COLOR);
			put(context.getString(R.string.pattern_color_int), PATTERN_COLOR);
			put(context.getString(R.string.day_color_int), DAY_COLOR);
			put(context.getString(R.string.night_color_int), NIGHT_COLOR);
			put(context.getString(R.string.grid_color_int), GRID_COLOR);
			put(context.getString(R.string.grid_outline_color_int), GRID_OUTLINE_COLOR);
			put(context.getString(R.string.max_rain_color_int), MAX_RAIN_COLOR);
			put(context.getString(R.string.min_rain_color_int), MIN_RAIN_COLOR);
			put(context.getString(R.string.above_freezing_color_int), ABOVE_FREEZING_COLOR);
			put(context.getString(R.string.below_freezing_color_int), BELOW_FREEZING_COLOR);
		}};
		
		boolean isFahrenheit = false;
		boolean isInches = false;
		boolean drawDayLightEffect = false;
		
		if (widgetSettingsCursor != null) {
			if (widgetSettingsCursor.moveToFirst()) {
				do {
					String key = widgetSettingsCursor.getString(AixWidgetSettings.KEY_COLUMN);
					String value = widgetSettingsCursor.getString(AixWidgetSettings.VALUE_COLUMN);
					
					if (colorNameMap.containsKey(key)) {
						colors[colorNameMap.get(key)] = Integer.parseInt(value);
					} else if (key.equals(context.getString(R.string.temperature_units_string))) {
						isFahrenheit = value.equals("2");
					} else if (key.equals(context.getString(R.string.precipitation_units_string))) {
						isInches = value.equals("2");
					} else if (key.equals(context.getString(R.string.day_effect_bool))) {
						drawDayLightEffect = Boolean.parseBoolean(value);
					}
					
					else {
						Log.d(TAG, "Unused property key=" + key + ", value=" + value);
					}
				} while (widgetSettingsCursor.moveToNext());
			}
			widgetSettingsCursor.close();
		}
		
		// Get location name for view
		String locationName = null;
		TimeZone locationTime = null;
		
		Uri locationUri = Uri.withAppendedPath(viewUri, AixViews.TWIG_LOCATION); 
		Cursor locationCursor = resolver.query(locationUri, null, null, null, null);
		
		if (locationCursor != null) {
			if (locationCursor.moveToFirst()) {
				locationName = locationCursor.getString(AixLocationsColumns.TITLE_COLUMN);
				String timeZone = locationCursor.getString(AixLocations.TIME_ZONE_COLUMN);
				try {
					locationTime = TimeZone.getTimeZone(timeZone);
				} catch (Exception e) {
					throw AixWidgetDrawException.buildInvalidTimeZoneException(timeZone);
				}
			}
			locationCursor.close();
		}
		
		// Get weather data
		TimeZone utcTime = TimeZone.getTimeZone("UTC");
		Calendar calendar = Calendar.getInstance(utcTime);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		
		long timeNowReally = calendar.getTimeInMillis();
		calendar.add(Calendar.HOUR_OF_DAY, 1);
		long timeNow = calendar.getTimeInMillis();
		calendar.add(Calendar.HOUR_OF_DAY, -12);
		long timeFrom = calendar.getTimeInMillis();
		calendar.add(Calendar.HOUR_OF_DAY, 48);
		long timeTo = calendar.getTimeInMillis();
		
		// Get temperature values
		ArrayList<Temperature> temperatureValues = new ArrayList<Temperature>();
		Cursor cursor = resolver.query(
				Uri.withAppendedPath(viewUri, AixViews.TWIG_FORECASTS),
				null,
				AixForecasts.TIME_FROM + "=" + AixForecasts.TIME_TO + " AND " +
				AixForecasts.TIME_FROM + ">=? AND " +
				AixForecasts.TIME_FROM + "<=?" + " AND " +
				AixForecasts.TEMPERATURE + " IS NOT NULL",
				new String[] { Long.toString(timeFrom), Long.toString(timeTo) },
				AixForecastsColumns.TIME_FROM + " ASC");
		
		int sampleResolutionHrs = Integer.MAX_VALUE;
		long epoch = 0;
		long lastVal = 0;
		
		if (cursor != null) {
			if (cursor.moveToFirst()) {
				do {
					long temperatureTime = cursor.getLong(AixForecastsColumns.TIME_FROM_COLUMN);
					float temperature = cursor.getFloat(AixForecastsColumns.TEMPERATURE_COLUMN);
					if (isFahrenheit) {
						temperature = temperature * 9.0f / 5.0f + 32.0f;
					}
					temperatureValues.add(new Temperature(temperatureTime, temperature));
					
					// Find resolution
					long diff = temperatureTime - lastVal;
					if (diff > 0) {
						int diffInHours = (int)(diff / DateUtils.HOUR_IN_MILLIS);
						if (diffInHours < sampleResolutionHrs) {
							sampleResolutionHrs = diffInHours;
						}
					}
					
					// Set epoch
					if (epoch == 0 && temperatureTime > timeNow) {
						epoch = temperatureTime;
						if (lastVal != 0) {
							epoch -= sampleResolutionHrs * DateUtils.HOUR_IN_MILLIS;
						}
					}
					
					lastVal = temperatureTime;
				} while (cursor.moveToNext());
			}
			cursor.close();
		}
		
		sampleResolutionHrs = Math.max(sampleResolutionHrs, 1);
		sampleResolutionHrs = Math.min(sampleResolutionHrs, 6);
		
		// Update timeFrom and timeTo to correct values given a modified epoch
		int numHours = 24;
		
		calendar.setTimeInMillis(epoch);
		timeFrom = calendar.getTimeInMillis();
		calendar.add(Calendar.HOUR_OF_DAY, numHours);
		timeTo = calendar.getTimeInMillis();
		
		// Get rain values
		ArrayList<Rain> rainValues = new ArrayList<Rain>();
		
		float[] rainDataMinValues = new float[numHours / sampleResolutionHrs];
		float[] rainDataValues = new float[numHours / sampleResolutionHrs];
		float[] rainDataMaxValues = new float[numHours / sampleResolutionHrs];
		
		Arrays.fill(rainDataMinValues, Float.NaN);
		Arrays.fill(rainDataValues, Float.NaN);
		Arrays.fill(rainDataMaxValues, Float.NaN);
		
		cursor = resolver.query(
				Uri.withAppendedPath(viewUri, AixViews.TWIG_FORECASTS),
				null,
				AixForecasts.TIME_TO + ">? AND " +
				AixForecasts.TIME_FROM + "<?" + " AND " +
				AixForecasts.RAIN_VALUE + " IS NOT NULL",
				new String[] { Long.toString(timeFrom), Long.toString(timeTo) },
				AixForecastsColumns.TIME_FROM + " ASC");
		
		if (cursor != null) {
			if (cursor.moveToFirst()) {
				do {
					try {
						Rain rain = new Rain(
								cursor.getLong(AixForecastsColumns.TIME_FROM_COLUMN),
								cursor.getLong(AixForecastsColumns.TIME_TO_COLUMN),
								cursor.getString(AixForecastsColumns.RAIN_VALUE_COLUMN),
								cursor.getString(AixForecastsColumns.RAIN_LOWVAL_COLUMN),
								cursor.getString(AixForecastsColumns.RAIN_HIGHVAL_COLUMN),
								isInches, isInches ? 0.05f : 1.0f,
								cursor.getString(AixForecastsColumns.WEATHER_ICON_COLUMN));
						rainValues.add(rain);
					} catch (Exception e) {
					}
				} while (cursor.moveToNext());
			}
			cursor.close();
		}
		
		Collections.sort(rainValues);
		// Rain data now sorted with longest period data first. Start adding to arrays
		for (Rain r : rainValues) {
			if (r.getDiff() / DateUtils.HOUR_IN_MILLIS == sampleResolutionHrs) {
				/*
				int startIndex = (int)((r.timeFrom - epoch) / DateUtils.HOUR_IN_MILLIS / sampleResolutionHrs);
				int stopIndex = (int)((r.timeTo - epoch) / DateUtils.HOUR_IN_MILLIS / sampleResolutionHrs);
				
				startIndex = Math.max(0, startIndex);
				stopIndex = Math.min(24 / sampleResolutionHrs, stopIndex);
				
				for (int i = startIndex; i < stopIndex; i++) {
					rainDataValues[i] = r.value;
					rainDataMinValues[i] = r.minValue;
					rainDataMaxValues[i] = r.maxValue;
				}
				*/
				int i = (int)((r.timeFrom - epoch) / DateUtils.HOUR_IN_MILLIS / sampleResolutionHrs);
				rainDataValues[i] = r.value;
				rainDataMinValues[i] = r.minValue;
				rainDataMaxValues[i] = r.maxValue;
			}
		}
		
		// Calculate widget dimensions, taking orientation into account
		DisplayMetrics dm = context.getResources().getDisplayMetrics();
		final float dp = dm.density;
		
		int columns = (widgetSize - 1) / 4 + 1;
		int rows = (widgetSize - 1) % 4 + 1;
		
		int widgetWidth = (int)Math.round(((isLandscape ? (106.0f * columns) : (80.0f * columns)) - 2.0f) * dp);
		int widgetHeight = (int)Math.round(((isLandscape ? (74.0f * rows) : (100.0f * rows)) - 2.0f) * dp);

		boolean isWidgetWidthOdd = widgetWidth % 2 == 1;
		boolean isWidgetHeightOdd = widgetHeight % 2 == 1;
		
		Rect widgetBounds = new Rect(
				isWidgetWidthOdd ? 1 : 0,
				isWidgetHeightOdd ? 1 : 0,
				widgetWidth, widgetHeight);
		
		Bitmap bitmap = Bitmap.createBitmap(widgetWidth, widgetHeight, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		
		float widgetBorder = 5.0f;
		
		RectF backgroundRect = new RectF(
				widgetBounds.left + widgetBorder,
				widgetBounds.top + widgetBorder,
				widgetBounds.right - widgetBorder,
				widgetBounds.bottom - widgetBorder);
		
		float maxTemperature = Float.NEGATIVE_INFINITY;
		float minTemperature = Float.POSITIVE_INFINITY;
		// Find maximum, minimum and range of temperatures
		for (Temperature t : temperatureValues) {
			// Check if temperature is within viewable range
			if (t.time >= timeFrom && t.time <= timeTo)
			{
				if (t.value > maxTemperature) maxTemperature = t.value;
				if (t.value < minTemperature) minTemperature = t.value;
			}
		}
		
		if (Float.isInfinite(maxTemperature) || Float.isInfinite(minTemperature)) {
			throw AixWidgetDrawException.buildMissingWeatherDataException();
		}
		
		float[] degreesPerCellOptions = { 0.5f, 1.0f, 2.0f, 2.5f, 5.0f, 10.0f,
				20.0f, 25.0f, 50.0f, 100.0f };
		int dPcIndex = 0;

		float textLabelWidth = 0.0f;
		int numHorizontalCells = numHours, numVerticalCells = 2;
		float cellSizeX = 10.0f, cellSizeY = 10.0f;
		Rect graphRect = new Rect();

		float iconHeight = 19.0f * dp;
		float iconSpacingY = 1.0f * dp;
		
		float tempRangeMax = 0.0f, tempRangeMin = 0.0f;

		String[] tempLabels = null;

		final float labelTextSize = 9.0f * dp;
		Paint labelPaint = new Paint() {{
			setColor(colors[TEXT_COLOR]);
			setTextAlign(Paint.Align.RIGHT);
			setAntiAlias(true);
			setTextSize(labelTextSize);
		}};
		
		float degreesPerCell;
		
		while (true) {
			if (dPcIndex == degreesPerCellOptions.length) {
				throw AixWidgetDrawException.buildGraphDimensionFailure();
			}
			
			graphRect.left = (int)Math.round(backgroundRect.left + textLabelWidth + 5.0f * dp);
			if (isLandscape) {
				graphRect.top = (int)Math.round(backgroundRect.top + labelTextSize / 2.0f + 2.0f * dp);
			} else {
				graphRect.top = (int)Math.round(backgroundRect.top + labelTextSize + 4.0f * dp);
			}
			graphRect.right = (int)Math.round(backgroundRect.right - 5.0f * dp);
			graphRect.bottom = (int)Math.round(backgroundRect.bottom - labelTextSize - 5.0f * dp);
			
			for (int j = 2; j <= 100; j++) {
				if (numHours % j == 0) {
					if ((float)graphRect.width() / (float)j > 8.0f * dp) {
						numHorizontalCells = j;
					} else {
						break;
					}
				}
			}
			
			cellSizeX = (float)(graphRect.right - graphRect.left) / (float) numHorizontalCells;
			degreesPerCell = degreesPerCellOptions[dPcIndex];
			
			int numCellsReq = (int) Math.ceil(maxTemperature / degreesPerCell) - (int) Math.floor(minTemperature / degreesPerCell);
			float tCellSizeY = ((float)graphRect.height() - iconHeight - iconSpacingY) / ((maxTemperature / degreesPerCell) - (float)Math.floor(minTemperature / degreesPerCell));
			numVerticalCells = (int)Math.ceil((float)(graphRect.bottom - graphRect.top) / tCellSizeY);
			
			cellSizeY = (graphRect.bottom - graphRect.top) / (float) numVerticalCells;
			
			if (2.0f * dp > cellSizeY * ((minTemperature / degreesPerCell - Math.floor(minTemperature / degreesPerCell)))) {
				numVerticalCells++;
			}
			if (numVerticalCells > 3) while (isPrime(numVerticalCells)) numVerticalCells++;
			
			cellSizeY = (graphRect.bottom - graphRect.top) / (float) numVerticalCells;
			
			if (cellSizeY < 8.0f * dp) {
				dPcIndex++;
				continue;
			}
			
			int startCell;
			
			// Center range as far as possible (need to fix proper centering)
			startCell = numVerticalCells - numCellsReq;
			while ((startCell > 0) && (iconHeight + iconSpacingY > cellSizeY
					* (numVerticalCells - numCellsReq - startCell + (Math
					.ceil(maxTemperature / degreesPerCell) - maxTemperature / degreesPerCell))))
			{
				startCell--;
			}
			
			tempRangeMin = degreesPerCell * (float)(Math.floor(minTemperature / degreesPerCell) - startCell);
			tempRangeMax = tempRangeMin + degreesPerCell * numVerticalCells;
			
			tempLabels = new String[numVerticalCells + 1];

			float newTextLabelWidth = 0.0f;
			
			boolean isAllBelow10 = true;
			
			for (int j = 0; j <= numVerticalCells; j++) {
				float num = tempRangeMin + degreesPerCell * j;
				if (Math.abs(num) >= 10.0f) isAllBelow10 = false;
				String formatting = Math.round(degreesPerCell) != degreesPerCell ? "%.1f\u00B0" : "%.0f\u00B0";
				tempLabels[j] = String.format(formatting, num);
				newTextLabelWidth = Math.max(newTextLabelWidth,
						labelPaint.measureText(tempLabels[j]));
			}
			
			if (isAllBelow10) newTextLabelWidth += dp;

			if (newTextLabelWidth > textLabelWidth) {
				textLabelWidth = newTextLabelWidth;
			} else {
				break;
			}
		}
		
		float rainScale = isInches ? 0.05f : 1.0f;
		
		Rect graphRectInner = new Rect(graphRect.left + 1, graphRect.top + 1, graphRect.right, graphRect.bottom);
		Rect graphRectOuter = new Rect(graphRect.left, graphRect.top, graphRect.right + 1, graphRect.bottom + 1);
		
		drawBackground(canvas, resources, colors, widgetBounds, backgroundRect);
		drawGrid(canvas, graphRect, colors, drawDayLightEffect, numHorizontalCells, numVerticalCells, cellSizeX, cellSizeY);

		ArrayList<Rain> sunTimes = getSunTimes(resolver, viewUri);
		if (drawDayLightEffect) {
			drawDayAndNight(canvas, context, graphRect, colors, sunTimes, numHours, timeFrom, timeTo);
		}
		
		drawRainBars(canvas, graphRect, dp, colors, numVerticalCells, sampleResolutionHrs, cellSizeX, cellSizeY, rainDataValues, rainDataMinValues, rainDataMaxValues);
		
		Path temperaturePath = buildPath(temperatureValues, graphRect, tempRangeMax, tempRangeMin, timeFrom, timeTo);
		if (temperaturePath != null) {
			drawTemperature(canvas, temperaturePath, graphRect, graphRectInner, colors, dp, isFahrenheit, tempRangeMax, tempRangeMin);
		}
		
		Paint graphOutlinePaint = new Paint() {{
			setColor(colors[GRID_OUTLINE_COLOR]);
			setStyle(Paint.Style.STROKE);
			setAntiAlias(false);
			setStrokeWidth(1.0f);
			setStrokeCap(Cap.SQUARE);
		}};
		
		drawGridOutline(canvas, graphRect, graphOutlinePaint, drawDayLightEffect);
		
		drawTemperatureLabels(canvas, graphRect, tempLabels, dp, cellSizeY, numVerticalCells,
				graphOutlinePaint, labelPaint);

		int[] iconIds = { 0, // Change offset of icons such that sun=1
				R.drawable.weather_icon_day_sun,
				R.drawable.weather_icon_day_lightcloud,
				R.drawable.weather_icon_day_partlycloud,
				R.drawable.weather_icon_day_cloud,
				R.drawable.weather_icon_day_lightrainsun,
				R.drawable.weather_icon_day_lightrainthundersun,
				R.drawable.weather_icon_day_sleetsun,
				R.drawable.weather_icon_day_snowsun,
				R.drawable.weather_icon_day_lightrain,
				R.drawable.weather_icon_day_rain,
				R.drawable.weather_icon_day_rainthunder,
				R.drawable.weather_icon_day_sleet,
				R.drawable.weather_icon_day_snow,
				R.drawable.weather_icon_day_snowthunder,
				R.drawable.weather_icon_day_fog
		};
		
		int[] iconIds_night = { 0, // Change offset of icons such that sun=1
				R.drawable.weather_icon_night_sun,
				R.drawable.weather_icon_night_lightcloud,
				R.drawable.weather_icon_night_partlycloud,
				R.drawable.weather_icon_night_cloud,
				R.drawable.weather_icon_night_lightrainsun,
				R.drawable.weather_icon_night_lightrainthundersun,
				R.drawable.weather_icon_night_sleetsun,
				R.drawable.weather_icon_night_snowsun,
				R.drawable.weather_icon_night_lightrain,
				R.drawable.weather_icon_night_rain,
				R.drawable.weather_icon_night_rainthunder,
				R.drawable.weather_icon_night_sleet,
				R.drawable.weather_icon_night_snow,
				R.drawable.weather_icon_night_snowthunder,
				R.drawable.weather_icon_night_fog
		};
		
		float iconWidth = 19.0f * dp;
		// Calculate number of cells per icon
		int numCellsPerIcon = 0;
		while ((numCellsPerIcon * cellSizeX < iconWidth) || (numHours % numCellsPerIcon != 0)) {
			numCellsPerIcon++;
		}
		
		// Find weather icon resolution (cells / icon)
		sampleResolutionHrs = Math.max(sampleResolutionHrs, 2);
		
		long rtp = timeFrom + sampleResolutionHrs * DateUtils.HOUR_IN_MILLIS / 2;
		
		float hoursPerCell = (float)numHours / (float)numHorizontalCells;
		
		float iconWidthValOver2 = iconWidth * (timeTo - timeFrom) / graphRect.width() / 2.0f;
		
		Rect dest = new Rect();
		for (Rain rain : rainValues) {
			int res = (int)((rain.timeTo - rain.timeFrom) / DateUtils.HOUR_IN_MILLIS);
			if (res == sampleResolutionHrs && rain.timeFrom >= timeFrom && rain.timeTo <= timeTo) {
				long iconTimePos = (rain.timeTo + rain.timeFrom) / 2;
				if (iconTimePos < rtp) continue;

				// Draw this icon
				for (int i = 1; i < temperatureValues.size(); i++) {
					Temperature t1 = temperatureValues.get(i - 1);
					Temperature t2 = temperatureValues.get(i);
					
					float val = Float.NaN;
					
					if (t1.time == iconTimePos) {
						val = t1.value;
						if (i > 1) {
							Temperature tfirst = temperatureValues.get(i - 2);
							float y1 = (float)((iconTimePos - iconWidthValOver2) - tfirst.time) * ((t1.value - tfirst.value) / (float)(t1.time - tfirst.time)) + tfirst.value;
							val = Math.max(val, y1);
						}
						float y2 = (float)((iconTimePos + iconWidthValOver2) - t1.time) * ((t2.value - t1.value) / (float)(t2.time - t1.time)) + t1.value;
						val = Math.max(val, y2);
					} else if (t1.time < iconTimePos && t2.time > iconTimePos) {
						// Use linear interpolation
						val = (float)(iconTimePos - t1.time) * ((t2.value - t1.value) / (float)(t2.time - t1.time)) + t1.value;
						float y1 = (float)((iconTimePos - iconWidthValOver2) - t1.time) * ((t2.value - t1.value) / (float)(t2.time - t1.time)) + t1.value;
						val = Math.max(val, y1);
						float y2 = (float)((iconTimePos + iconWidthValOver2) - t1.time) * ((t2.value - t1.value) / (float)(t2.time - t1.time)) + t1.value;
						val = Math.max(val, y2);
					}
					if (!Float.isNaN(val)) {
						int iconId = rain.weatherIcon;
						if (iconId >= 0 && iconId < iconIds.length) { // Ensure that access is within array
							dest.left = (int)Math.round(graphRect.left + Math.round(((float)(iconTimePos - timeFrom) / ((float)DateUtils.HOUR_IN_MILLIS * hoursPerCell)) * cellSizeX) - iconWidth / 2.0f);
							dest.top = Math.round(graphRect.bottom - (graphRect.bottom - graphRect.top) * (val - tempRangeMin) / (tempRangeMax - tempRangeMin) - iconHeight - iconSpacingY);
							dest.right = Math.round(dest.left + iconWidth);
							dest.bottom = Math.round(dest.top + iconHeight);
							
							boolean isDay = false;
							
							for (Rain sun : sunTimes) {
								if (iconTimePos > sun.timeFrom && iconTimePos < sun.timeTo) {
									isDay = true;
									break;
								}
							}
							
							int[] iconArray = isDay ? iconIds : iconIds_night;
							
							Bitmap weatherIcon = ((BitmapDrawable)context.getResources().
									getDrawable(iconArray[iconId])).getBitmap();
							
							if (weatherIcon != null) {
								canvas.drawBitmap(weatherIcon, null, dest, null);
							}

							rtp = iconTimePos + sampleResolutionHrs * DateUtils.HOUR_IN_MILLIS;
							break;
						}
					}
				}
			}
		}
		
		boolean use24hours = DateFormat.is24HourFormat(context);
		calendar.setTimeInMillis(timeFrom);
		calendar.setTimeZone(locationTime);
		drawHourLabels(canvas, graphRect, calendar, use24hours, numHorizontalCells, numHours,
				sampleResolutionHrs, dp, cellSizeX, labelPaint, graphOutlinePaint);
		
		// Top bar stuff
		
		float pressure = Float.NaN;
		float humidity = Float.NaN;
		float temperature = Float.NaN;
		
		Cursor c = resolver.query(Uri.withAppendedPath(viewUri, AixViews.TWIG_FORECASTS), null,
				AixForecastsColumns.TIME_FROM + ">=" + timeNowReally + " AND " +
				AixForecastsColumns.TEMPERATURE + " IS NOT NULL AND " +
				AixForecastsColumns.PRESSURE + " IS NOT NULL AND " +
				AixForecastsColumns.HUMIDITY + " IS NOT  NULL",
				null,
				AixForecastsColumns.TIME_FROM + " ASC");
		if (c != null) {
			if (c.moveToFirst()) {
				pressure = c.getFloat(AixForecastsColumns.PRESSURE_COLUMN);
				humidity = c.getFloat(AixForecastsColumns.HUMIDITY_COLUMN);
				temperature = c.getFloat(AixForecastsColumns.TEMPERATURE_COLUMN);
			}
			c.close();
		}
		
		if (!isLandscape) {
			if (isFahrenheit && !Float.isNaN(temperature)) {
				temperature = temperature * 9.0f / 5.0f + 32.0f;
			}
			drawInfoText(canvas, context, backgroundRect, graphRect, colors, dp, locationName,
					pressure, humidity, rainScale, isInches, temperature, isFahrenheit);
		}
		
		Log.d(TAG, "Drawing widget took: " + (timerStart - System.currentTimeMillis()) + " ms");
		
		return bitmap;
	}
	
	private static void drawBackground(Canvas canvas, Resources resources, final int[] colors,
			Rect widgetBounds, RectF backgroundRect)
	{
		NinePatchDrawable bgBorderImage = getNinePatchDrawable(
				resources, R.drawable.widget_bg_holo);
		bgBorderImage.setBounds(widgetBounds);
		bgBorderImage.draw(canvas);
		
		Paint backgroundPaint = new Paint() {{
			setStyle(Style.FILL);
			setColor(colors[BACKGROUND_COLOR]);
		}};
		
		final Bitmap bgPattern = getBitmapUnscaled(resources, R.drawable.pattern); 
		
		Paint bitPaint = new Paint() {{
			setShader(new BitmapShader(bgPattern, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT));
			setColorFilter(new PorterDuffColorFilter(colors[PATTERN_COLOR], PorterDuff.Mode.SRC_IN));
		}};
		
		canvas.drawRect(backgroundRect, backgroundPaint);
		canvas.drawRect(backgroundRect, bitPaint);
	}
	
	private static void drawGrid(Canvas canvas, Rect graphRect, final int[] colors,
			boolean drawDayLightEffect, int numHorizontalCells, int numVerticalCells,
			float cellSizeX, float cellSizeY)
	{
		Paint gridPaint = new Paint() {{
			setColor(colors[GRID_COLOR]);
			setAntiAlias(false);
			setStyle(Paint.Style.STROKE);
			setStrokeWidth(1.0f);
			setStrokeCap(Cap.SQUARE);
		}};
		
		Path gridPath = new Path();
		
		for (int i = 1; i <= numHorizontalCells; i++) {
			float xPos = graphRect.left + Math.round(i * cellSizeX);
			gridPath.moveTo(xPos, graphRect.bottom);
			gridPath.lineTo(xPos, graphRect.top);
		}

		for (int i = 1; i <= numVerticalCells; i++) {
			float yPos = graphRect.bottom - Math.round(i * cellSizeY);
			gridPath.moveTo(graphRect.left, yPos);
			gridPath.lineTo(graphRect.right, yPos);
		}
		
		int rightOffset = drawDayLightEffect ? 0 : 1;
		
		canvas.save();
		canvas.clipRect(
				graphRect.left + 1,
				graphRect.top,
				graphRect.right + rightOffset,
				graphRect.bottom,
				Region.Op.REPLACE);
		canvas.drawPath(gridPath, gridPaint);
		canvas.restore();
	}
	
	private static void drawGridOutline(Canvas canvas, Rect graphRect, Paint graphOutlinePaint,
			boolean drawDayLightEffect)
	{
		Path graphLines = new Path();
		graphLines.moveTo(graphRect.left, graphRect.top);
		graphLines.lineTo(graphRect.left, graphRect.bottom);
		graphLines.lineTo(graphRect.right, graphRect.bottom);
		
		if (drawDayLightEffect) {
			graphLines.lineTo(graphRect.right, graphRect.top);
		}
		
		canvas.drawPath(graphLines, graphOutlinePaint);
	}
	
	private static void drawTemperatureLabels(Canvas canvas, Rect graphRect, String[] labels,
			float dp, float cellSizeY, int numVerticalCells, Paint notchPaint, Paint labelPaint)
	{
		float labelTextPaddingX = 2.5f * dp;
		float notchWidth = 3.0f * dp;
		
		// Find the minimum number of cells between each label
		int numCellsBetweenVerticalLabels = 1;
		while (numCellsBetweenVerticalLabels * cellSizeY < labelPaint.getTextSize()) {
			numCellsBetweenVerticalLabels++;
		}
		// Ensure that labels can be evenly spaced, given the number of cells
		while (numVerticalCells % numCellsBetweenVerticalLabels != 0) {
			numCellsBetweenVerticalLabels++;
		}
		
		if (labels.length < numVerticalCells) return;
		
		for (int i = 0; i <= numVerticalCells; i += numCellsBetweenVerticalLabels) {
			float notchY = graphRect.bottom - Math.round(i * cellSizeY);
			canvas.drawLine(
					graphRect.left - notchWidth / 2, notchY,
					graphRect.left + notchWidth / 2, notchY,
					notchPaint);
			
			Rect bounds = new Rect();
			labelPaint.getTextBounds(labels[i], 0, labels[i].length(), bounds);
			
			canvas.drawText(
					labels[i],
					graphRect.left - labelTextPaddingX,
					graphRect.bottom - bounds.centerY()
							- (float)(i * graphRect.height()) / (float)numVerticalCells,
					labelPaint);
		}
	}
	
	private static void drawRainBars(Canvas canvas, Rect graphRect, float dp, final int[] colors,
			int numVerticalCells, int sampleResolutionHrs, float cellSizeX, float cellSizeY,
			float[] rainDataValues, float[] rainDataMinValues, float[] rainDataMaxValues)
	{
		// Build rain paths
		Path minRainPath = new Path();
		Path maxRainPath = new Path();
		for (int i = 0; i < rainDataValues.length; i++) {
			float lowValue =
				(!Float.isNaN(rainDataMinValues[i]))
				? rainDataMinValues[i]
				: ((!Float.isNaN(rainDataValues[i]))
						? rainDataValues[i]
						: 0.0f);
			float highValue =
				(!Float.isNaN(rainDataMaxValues[i]))
				? rainDataMaxValues[i]
				: ((!Float.isNaN(rainDataValues[i]))
						? rainDataValues[i]
						: 0.0f);
			lowValue = Math.min(lowValue, numVerticalCells);

			RectF rainRect = new RectF(
					graphRect.left + Math.round(i * sampleResolutionHrs * cellSizeX) + 1.0f,
					graphRect.bottom - lowValue * cellSizeY,
					graphRect.left + Math.round((i * sampleResolutionHrs + sampleResolutionHrs) * cellSizeX),
					graphRect.bottom);
			minRainPath.addRect(rainRect, Path.Direction.CCW);
			if (highValue > lowValue && lowValue != numVerticalCells) {
				highValue = Math.min(highValue, numVerticalCells);
				rainRect.bottom = rainRect.top;
				rainRect.top = graphRect.bottom - highValue * cellSizeY;
				maxRainPath.addRect(rainRect, Path.Direction.CCW);
			}
		}
		
		// Draw rain paths
		Paint rainBarPaint = new Paint() {{
			setColor(colors[MIN_RAIN_COLOR]);
			setStyle(Paint.Style.FILL);
			setAntiAlias(false);
		}};
		Paint lightRainPaint = new Paint() {{
			setColor(colors[MAX_RAIN_COLOR]);
			setStyle(Paint.Style.STROKE);
			setAntiAlias(true);
			setStrokeWidth(1.30f);
			setStrokeCap(Cap.SQUARE);
		}};
		
		canvas.drawPath(minRainPath, rainBarPaint);

		canvas.save();
		canvas.clipPath(maxRainPath);
		
		// TODO Technique works. But needs cleaning. 
		float dimensions = (float)Math.sin(Math.toRadians(45)) * (graphRect.height() + graphRect.width());
		float dimensions2 = dimensions / 2.0f;
		float ggx = graphRect.left + graphRect.width() / 2.0f;
		float ggy = graphRect.top + graphRect.height() / 2.0f;
	
		Matrix transform = new Matrix();
		transform.setRotate(-45.0f, ggx, ggy);
		canvas.setMatrix(transform);
		
		float ypos = graphRect.top - (dimensions - graphRect.height()) / 2.0f;
		float ytest = graphRect.bottom + (dimensions - graphRect.height()) / 2.0f;
		while (ypos < ytest) {
			canvas.drawLine(ggx - dimensions2, ypos, ggx + dimensions2, ypos, lightRainPaint);
			ypos += 2.0f * dp;
		}
		canvas.restore();
	}
	
	private static void drawTemperature(Canvas canvas, Path temperaturePath,
			Rect graphRect, Rect graphRectInner, final int[] colors, final float dp,
			boolean isFahrenheit, float tempRangeMax, float tempRangeMin)
	{
		Paint temperaturePaint = new Paint() {{
			setColor(colors[ABOVE_FREEZING_COLOR]);
			setStyle(Paint.Style.STROKE);
			setStrokeCap(Cap.SQUARE);
			setAntiAlias(true);
			setStrokeWidth(2.0f * dp);
		}};
		
		float freezingTemperature = isFahrenheit ? 32.0f : 0.0f;
		
		canvas.save();
		if (tempRangeMin >= freezingTemperature) {
			// All positive
			canvas.clipRect(graphRectInner);
			canvas.drawPath(temperaturePath, temperaturePaint);
		} else if (tempRangeMax <= freezingTemperature) {
			// All negative
			canvas.clipRect(graphRectInner);
			temperaturePaint.setColor(colors[BELOW_FREEZING_COLOR]);
			canvas.drawPath(temperaturePath, temperaturePaint);
		} else {
			float freezingPosY = (float)Math.floor(graphRect.height() *
					(freezingTemperature - tempRangeMin) / (tempRangeMax - tempRangeMin));
			
			canvas.clipRect(graphRectInner.left, graphRectInner.top,
					graphRectInner.right, graphRectInner.bottom - freezingPosY);
			canvas.drawPath(temperaturePath, temperaturePaint);
			
			canvas.clipRect(graphRectInner.left, graphRectInner.bottom - freezingPosY,
					graphRectInner.right, graphRectInner.bottom, Op.REPLACE);
			temperaturePaint.setColor(colors[BELOW_FREEZING_COLOR]);
			canvas.drawPath(temperaturePath, temperaturePaint);
		}
		canvas.restore();
	}
	
	private static void drawHourLabels(Canvas canvas, Rect graphRect, Calendar calendar,
			boolean use24hours, int numHorizontalCells, int numHours, int sampleResolutionHours,
			float dp, float cellSizeX, Paint labelPaint, Paint notchPaint)
	{
		// Draw time stamp labels and horizontal notches
		float notchHeight = 3.0f * dp;
		float labelTextPaddingY = 2.0f * dp;
		
		labelPaint.setTextAlign(Paint.Align.CENTER);
		
		int startHour = calendar.get(Calendar.HOUR_OF_DAY);
		float hoursPerCell = (float)numHours / (float)numHorizontalCells;
		int numCellsBetweenHorizontalLabels = sampleResolutionHours < hoursPerCell
				? 1 : (int)Math.floor((float)sampleResolutionHours / hoursPerCell);
		
		boolean useShortLabel = false;
		
		float longLabelWidth = labelPaint.measureText(use24hours ? "24" : "12 pm");
		float shortLabelWidth = labelPaint.measureText("12p");
		
		while (true) {
			/* Ensure that labels can be evenly spaced, given the number of cells, as well making
			 * sure that each label step is a full number of hours.
			 */
			while ((numHorizontalCells % numCellsBetweenHorizontalLabels != 0) ||
					((numCellsBetweenHorizontalLabels * hoursPerCell) !=
							Math.round(numCellsBetweenHorizontalLabels * hoursPerCell)))
			{
				numCellsBetweenHorizontalLabels++;
			}
			
			float spaceBetweenLabels = numCellsBetweenHorizontalLabels * cellSizeX;
			if (spaceBetweenLabels > longLabelWidth) {
				break;
			} else if (!use24hours && spaceBetweenLabels > shortLabelWidth) {
				useShortLabel = true;
				break;
			}
			
			numCellsBetweenHorizontalLabels++;
		}
		
		for (int i = numCellsBetweenHorizontalLabels; i < numHorizontalCells; i+= numCellsBetweenHorizontalLabels) {
			float notchX = graphRect.left + Math.round(i * cellSizeX);
			canvas.drawLine(notchX, graphRect.bottom - notchHeight / 2, notchX, graphRect.bottom + notchHeight / 2, notchPaint);
			
			int hour = startHour + (int)(hoursPerCell * i);
			
			String hourLabel;
			if (use24hours) {
				hourLabel = String.format("%02d", hour % 24);
			} else {
				int hour12 = hour % 12;
				if (0 == hour12) hour12 = 12;
				boolean am = (hour % 24) < 12;
				if (useShortLabel) {
					hourLabel = String.format("%2d%s", hour12, am ? "a" : "p" );
				} else {
					hourLabel = String.format("%2d %s", hour12, am ? "am" : "pm" );
				}
			}
			canvas.drawText(hourLabel, notchX, graphRect.bottom + labelPaint.getTextSize() + labelTextPaddingY, labelPaint);
		}
	}

	private static void drawInfoText(Canvas canvas, Context context,
			RectF backgroundRect, Rect graphRect, 
			final int[] colors, float dp, String locationName, float pressure, float humidity,
			float rainScale, boolean isInches, float temperature, boolean isFahrenheit)
	{
		final float locationLabelTextSize = 10.0f * dp;
		Paint locationLabelPaint = new Paint() {{
			setColor(colors[TEXT_COLOR]);
			setAntiAlias(true);
			setTextSize(locationLabelTextSize);
		}};
		
		float topTextSidePadding = 1.0f * dp;
		float topTextBottomPadding = 3.0f * dp;
		
		String pressureString = "";
		if (!Float.isNaN(pressure)) {
			pressureString = context.getString(R.string.pressure_top, pressure);
		}
		String humidityString = "";
		if (!Float.isNaN(humidity)) {
			humidityString = context.getString(R.string.humidity_top, humidity);
		}
		String rainScaleString = "";
		if (!Float.isNaN(rainScale)) {
			DecimalFormat df = new DecimalFormat(context.getString(R.string.rain_scale_format));
			String rainScaleFormatted = df.format(rainScale);
			
			String rainScaleUnit = context.getString(isInches ? R.string.rain_scale_unit_inches
															  : R.string.rain_scale_unit_mm);
			
			rainScaleString = context.getString(R.string.rain_scale_top, rainScaleFormatted, rainScaleUnit);
		}
		
		if (locationName == null) {
			locationName = "";
		}
		
		String temperatureString = "";
		if (!Float.isNaN(temperature)) {
			DecimalFormat df = new DecimalFormat(context.getString(R.string.temperature_format));
			String temperatureFormatted = df.format(temperature);
			
			String temperatureUnit = context.getString(
					isFahrenheit ? R.string.temperature_unit_fahrenheit
								 : R.string.temperature_unit_celsius);
			
			temperatureString = ' ' + context.getString(
					R.string.temperature_top, temperatureFormatted, temperatureUnit);
		}
		
		float topTextSpace = graphRect.width() - topTextSidePadding * 2.0f;
		
		String ellipsis = "...";
		String spacing = "       ";
		
		boolean isMeasuring = true;
		int measureState = 0;
		
		while ( isMeasuring &&
				topTextSpace < locationLabelPaint.measureText(pressureString) +
							   locationLabelPaint.measureText(humidityString) +
							   locationLabelPaint.measureText(rainScaleString) +
							   locationLabelPaint.measureText(locationName) +
							   locationLabelPaint.measureText(temperatureString) +
							   locationLabelPaint.measureText(spacing)) // Spacing between strings
		{
			switch (measureState) {
			case 0:
				if (!Float.isNaN(pressure)) {
					pressureString = context.getString(R.string.pressure_top_short, pressure);
				}
				measureState++;
				break;
			case 1:
				if (!Float.isNaN(humidity)) {
					humidityString = context.getString(R.string.humidity_top_short, humidity);
				}
				measureState++;
				break;
			case 2:
				if (locationName.length() == 0) {
					isMeasuring = false;
					break;
				}
				spacing = spacing + ellipsis;
				measureState++;
			default:
				locationName = locationName.substring(0, locationName.length() - 1);
				if (measureState > locationName.length() + 2) isMeasuring = false;
				break;
			}
		}
		
		if (measureState == 3) {
			locationName = locationName + ellipsis;
		}

		StringBuilder sb = new StringBuilder();

		if (pressureString.length() != 0) {
			sb.append(pressureString);
			sb.append("  ");
		}
		
		if (humidityString.length() != 0) {
			sb.append(humidityString);
			sb.append("  ");
		}
		
		if (rainScaleString.length() != 0) {
			sb.append(rainScaleString);
		}
		
		canvas.drawText(sb.toString(),
				graphRect.left + topTextSidePadding,
				backgroundRect.top + locationLabelPaint.getTextSize(),
				locationLabelPaint);
		
		sb.setLength(0);
		sb.append(locationName);

		if (!Float.isNaN(temperature)) {
			DecimalFormat df = new DecimalFormat(context.getString(R.string.temperature_format));
			String temperatureFormatted = df.format(temperature);
			
			String temperatureUnit = context.getString(
					isFahrenheit ? R.string.temperature_unit_fahrenheit
								 : R.string.temperature_unit_celsius);
			
			sb.append(' ');
			sb.append(context.getString(
					R.string.temperature_top,
					temperatureFormatted,
					temperatureUnit));
		}

		locationLabelPaint.setTextAlign(Align.RIGHT);
		canvas.drawText(sb.toString(),
				graphRect.right - topTextSidePadding,
				backgroundRect.top + locationLabelPaint.getTextSize(),
				locationLabelPaint);
	}
	
	private static ArrayList<Rain> getSunTimes(ContentResolver resolver, Uri viewUri) {
		ArrayList<Rain> sunTimes = new ArrayList<Rain>();
		Cursor sunCursor = resolver.query(
				Uri.withAppendedPath(viewUri, AixViews.TWIG_FORECASTS),
				null,
				AixForecastsColumns.SUN_RISE + " IS NOT NULL",
				null,
				AixForecastsColumns.TIME_FROM + " ASC");
		if (sunCursor != null) {
			if (sunCursor.moveToFirst()) {
				do {
					Rain day = new Rain();
					day.timeFrom = sunCursor.getLong(AixForecastsColumns.SUN_RISE_COLUMN);
					day.timeTo = sunCursor.getLong(AixForecastsColumns.SUN_SET_COLUMN);
					sunTimes.add(day);
				} while (sunCursor.moveToNext());
			}
			sunCursor.close();
		}
		return sunTimes;
	}
	
	private static void drawDayAndNight(Canvas canvas, Context context,
			Rect graphRect, int[] colors, ArrayList<Rain> sunValues,
			int numHours, long timeFrom, long timeTo)
	{
		float transitionWidth = 1.0f / numHours;
		float timeRange = timeTo - timeFrom;
		
		canvas.save();
		canvas.clipRect(graphRect.left + 1, graphRect.top, graphRect.right, graphRect.bottom);
		
		Matrix matrix = new Matrix();
		matrix.setScale(graphRect.width(), graphRect.height());
		matrix.postTranslate(graphRect.left + 1, graphRect.top);
		canvas.setMatrix(matrix);
		
		Paint p = new Paint();
		p.setStyle(Style.FILL);
		
		float marker = Float.NEGATIVE_INFINITY;
		
		for (int i = 0; i < sunValues.size(); i++) {
			Rain sun = sunValues.get(i);
			
			float sunRiseTime = (float)(sun.timeFrom - timeFrom) / timeRange;
			float sunRiseStart = sunRiseTime - transitionWidth / 2.0f;
			float sunRiseEnd = sunRiseTime + transitionWidth / 2.0f;
			
			float sunSetTime = (float)(sun.timeTo - timeFrom) / timeRange;
			float sunSetStart = sunSetTime - transitionWidth / 2.0f;
			float sunSetEnd = sunSetTime + transitionWidth / 2.0f;
			
			LinearGradient sunRiseGradient = new LinearGradient(
					sunRiseStart, 0.0f, sunRiseEnd, 0.0f,
					colors[NIGHT_COLOR], colors[DAY_COLOR], Shader.TileMode.CLAMP);
			p.setShader(sunRiseGradient);
			
			if (marker == Float.NEGATIVE_INFINITY) marker = sunRiseStart;
			
			RectF sunRiseRect = new RectF(marker, 0.0f, sunRiseEnd, 1.0f);
			
			canvas.drawRect(sunRiseRect, p);
			marker = sunRiseEnd;
			
			LinearGradient sunSetGradient = new LinearGradient(
					sunSetStart, 0.0f, sunSetEnd, 0.0f,
					colors[DAY_COLOR], colors[NIGHT_COLOR], Shader.TileMode.CLAMP);
			p.setShader(sunSetGradient);
			
			RectF sunSetRect = new RectF(marker, 0.0f, sunSetEnd, 1.0f);
			canvas.drawRect(sunSetRect, p);
			marker = sunSetEnd;
		}
		
		canvas.restore();
	}
	
	private static PointF getDerivative(PointF[] points, int i, double tension) {
		PointF ret = new PointF();
		if (i == 0) {
			// First point
			ret.set((float)((points[1].x - points[0].x) / tension),
					(float)((points[1].y - points[0].y) / tension));
		} else if (i == points.length - 1) {
			// Last point
			ret.set((float)((points[i].x - points[i - 1].x) / tension),
					(float)((points[i].y - points[i - 1].y) / tension));
		} else {
			ret.set((float)((points[i + 1].x - points[i - 1].x) / tension),
					(float)((points[i + 1].y - points[i - 1].y) / tension));
		}
		return ret;
	}
	
	private static PointF getB1(PointF[] points, int i, double tension) {
		PointF derivative = getDerivative(points, i, tension);
		return new PointF((float)(points[i].x + derivative.x / 3.0f),
						  (float)(points[i].y + derivative.y / 3.0f));
	}
	
	private static PointF getB2(PointF[] points, int i, double tension) {
		PointF derivative = getDerivative(points, i + 1, tension);
		return new PointF((float)(points[i + 1].x - derivative.x / 3.0f),
						  (float)(points[i + 1].y - derivative.y / 3.0f));
	}
	
	private static Path buildPath(ArrayList<Temperature> temperatureValues, Rect graphRect,
			float tempRangeMax, float tempRangeMin, long startTime, long endTime) {
		if (temperatureValues.size() <= 0) return null;
		Path path = new Path();
		float tempRange = tempRangeMax - tempRangeMin;
		long timeRange = endTime - startTime;
		PointF[] points = new PointF[temperatureValues.size()];
		
		for (int i = 0; i < temperatureValues.size(); i++) {
			Temperature t = temperatureValues.get(i);
			points[i] = new PointF((float)(t.time - startTime) / (float)timeRange,
					(float)(1.0f - (t.value - tempRangeMin) / tempRange));
		}
		
		path.moveTo(points[0].x, points[0].y);
		
		for (int i = 1; i < points.length; i++) {
			PointF b1 = getB1(points, i - 1, 2.0f);
			PointF b2 = getB2(points, i - 1, 2.0f);
			path.cubicTo(b1.x, b1.y, b2.x, b2.y, points[i].x, points[i].y);
		}
		
		Matrix scaleMatrix = new Matrix();
		scaleMatrix.setScale(graphRect.width(), graphRect.height());
		path.transform(scaleMatrix);
		path.offset(graphRect.left, graphRect.top);
		
		return path;
	}
	
}
