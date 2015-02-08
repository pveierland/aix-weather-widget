package widget;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import net.veierland.aix.AixProvider.AixWidgetSettings;
import net.veierland.aix.R;
import net.veierland.aix.Rain;
import net.veierland.aix.Temperature;
import net.veierland.aix.AixProvider.AixForecasts;
import net.veierland.aix.AixProvider.AixForecastsColumns;
import net.veierland.aix.AixProvider.AixLocations;
import net.veierland.aix.AixProvider.AixLocationsColumns;
import net.veierland.aix.AixProvider.AixViews;
import net.veierland.aix.AixProvider.AixWidgets;
import net.veierland.aix.AixProvider.AixWidgetsColumns;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.NinePatch;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Shader;
import android.graphics.Paint.Style;
import android.graphics.Region.Op;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
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
	private static final int LOCATION_BACKGROUND_COLOR = 2;
	private static final int LOCATION_TEXT_COLOR = 3;
	private static final int GRID_COLOR = 4;
	private static final int GRID_OUTLINE_COLOR = 5;
	private static final int MAX_RAIN_COLOR = 6;
	private static final int MIN_RAIN_COLOR = 7;
	private static final int ABOVE_FREEZING_COLOR = 8;
	private static final int BELOW_FREEZING_COLOR = 9;
	private static final int DAY_COLOR = 10;
	private static final int NIGHT_COLOR = 11;
	
//	private static void getColorFromCursor(Cursor widgetCursor, Resources resources, int[] colors, int colorColumnId, int colorId, int defaultColor) {
//		int color = widgetCursor.getInt(colorColumnId);
//		if (color == 0) {
//			color = resources.getColor(defaultColor);
//		}
//		colors[colorId] = color;
//	}
	
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
        // jump through hoops to avoid scaling the tiny ninepatch, which would skew the results
        // depending on device density
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
	
	public static Bitmap buildView(final Context context, Uri widgetUri, Uri viewUri) throws Exception {
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
			throw new Exception("AixDetailedWidget.buildView(): Widget has invalid size!");
		}
		
		// Set up default colors
		final Resources resources = context.getResources();
		final int[] colors = new int[12];
		colors[BACKGROUND_COLOR] = resources.getColor(R.color.background_default);
		colors[TEXT_COLOR] = resources.getColor(R.color.text_default);
		colors[LOCATION_BACKGROUND_COLOR] = resources.getColor(R.color.location_background_default);
		colors[LOCATION_TEXT_COLOR] = resources.getColor(R.color.location_text_default);
		colors[GRID_COLOR] = resources.getColor(R.color.grid_default);
		colors[GRID_OUTLINE_COLOR] = resources.getColor(R.color.grid_outline_default);
		colors[MAX_RAIN_COLOR] = resources.getColor(R.color.maximum_rain_default);
		colors[MIN_RAIN_COLOR] = resources.getColor(R.color.minimum_rain_default);
		colors[ABOVE_FREEZING_COLOR] = resources.getColor(R.color.above_freezing_default);
		colors[BELOW_FREEZING_COLOR] = resources.getColor(R.color.below_freezing_default);
		colors[DAY_COLOR] = resources.getColor(R.color.day_default);
		colors[NIGHT_COLOR] = resources.getColor(R.color.night_default);
		
		// Get widget settings
		Cursor widgetSettingsCursor = resolver.query(
				Uri.withAppendedPath(widgetUri, AixWidgets.TWIG_SETTINGS),
				null, null, null, null);
		
		Map<String, Integer> colorNameMap = new HashMap<String, Integer>() {{
			put(context.getString(R.string.background_color_int), BACKGROUND_COLOR);
			put(context.getString(R.string.text_color_int), TEXT_COLOR);
			put(context.getString(R.string.location_background_color_int), LOCATION_BACKGROUND_COLOR);
			put(context.getString(R.string.location_text_color_int), LOCATION_TEXT_COLOR);
			put(context.getString(R.string.grid_color_int), GRID_COLOR);
			put(context.getString(R.string.grid_outline_color_int), GRID_OUTLINE_COLOR);
			put(context.getString(R.string.max_rain_color_int), MAX_RAIN_COLOR);
			put(context.getString(R.string.min_rain_color_int), MIN_RAIN_COLOR);
			put(context.getString(R.string.above_freezing_color_int), ABOVE_FREEZING_COLOR);
			put(context.getString(R.string.below_freezing_color_int), BELOW_FREEZING_COLOR);
			put(context.getString(R.string.day_color_int), DAY_COLOR);
			put(context.getString(R.string.night_color_int), NIGHT_COLOR);
		}};
		
		boolean isFahrenheit = false;
		boolean isInches = false;
		
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
					} else {
						Log.d(TAG, "Unused property key=" + key + ", value=" + value);
					}
				} while (widgetSettingsCursor.moveToNext());
			}
			widgetSettingsCursor.close();
		}
		
		// Get location settings
		
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
					throw new Exception("AixDetailedWidget.buildView(): Invalid Time Zone (" +
							timeZone + ")");
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
		
		boolean isLandscape = (context.getResources().getConfiguration().orientation ==
							   Configuration.ORIENTATION_LANDSCAPE);
		
		Log.d(TAG, "isLandscape=" + isLandscape);
		
		int columns = (widgetSize - 1) / 4 + 1;
		int rows = (widgetSize - 1) % 4 + 1;
		
//		int widgetWidth = (int)Math.round(isLandscape ? (106.0f * columns - 2.0f) * dp : (80.0f * columns - 2.0f) * dp);
//		int widgetHeight = (int)Math.round(isLandscape ? (74.0f * rows - 2.0f) * dp : (100.0f * rows - 2.0f) * dp);
		int widgetWidth = (int)Math.round(isLandscape ? (106.0f * columns) * dp: (80.0f * columns) * dp) - 2;
		int widgetHeight = (int)Math.round(isLandscape ? (74.0f * rows) * dp : (100.0f * rows) * dp) - 2;
		
		Bitmap bitmap = Bitmap.createBitmap(widgetWidth, widgetHeight, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);

		Log.d(TAG, "width=" + widgetWidth + " height=" + widgetHeight);
		
		// MODIFIED
		
		//Rect destX = new Rect(5, 5, widgetWidth - 6, widgetHeight - 6);
		
		//float bgRectPaddingVertical = 2.0f * dp;
		//float bgRectPaddingHorizontal = 1.0f * dp;
		float bgRectRounding = 2.0f * dp;
		
		//float bgRectPaddingVertical = 2.0f * dp;
		//float bgRectPaddingHorizontal = 1.0f * dp;

		RectF bgRect = new RectF(6.0f, 6.0f, widgetWidth - 7.0f, widgetHeight - 7.0f);
		
//		RectF bgRect = new RectF(bgRectPaddingHorizontal,
//				bgRectPaddingVertical, widgetWidth
//						- bgRectPaddingHorizontal, widgetHeight
//						- bgRectPaddingVertical);
		
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
		
		float[] degreesPerCellOptions = { 0.5f, 1.0f, 2.0f, 2.5f, 5.0f, 10.0f,
				20.0f, 25.0f, 50.0f, 100.0f };
		int dPcIndex = 0;

		float textLabelWidth = 0.0f;
		int numHorizontalCells = numHours, numVerticalCells = 2;
		float cellSizeX = 10.0f, cellSizeY = 10.0f;
		RectF graphRect = new RectF();

		float iconHeight = 19.0f * dp;
		float iconSpacingY = 5.0f * dp;
		
		float tempRangeMax = 0.0f, tempRangeMin = 0.0f, degreesPerCell = 0.0f;

		String[] tempLabels = null;

		final float labelTextSize = 9.0f * dp;
		Paint temperatureLabels = new Paint() {{
			setColor(colors[TEXT_COLOR]);
			setTextAlign(Paint.Align.RIGHT);
			setAntiAlias(true);
			setTextSize(labelTextSize);
		}};
		
		int ggState = 0;
		boolean upTheGame = false;
		
		while (dPcIndex < degreesPerCellOptions.length) {
			if (upTheGame && ggState == 0) {
				dPcIndex++;
			}

			float dPc = degreesPerCellOptions[dPcIndex];

			graphRect.left = bgRect.left + textLabelWidth + 5.0f * dp;
			graphRect.top = bgRect.top + labelTextSize + 4.0f * dp;
			graphRect.right = bgRect.right - 5.0f * dp;
			graphRect.bottom = bgRect.bottom - labelTextSize - 4.0f * dp;

			cellSizeX = (graphRect.right - graphRect.left)
					/ (float) numHorizontalCells;
			
			if (upTheGame && ggState == 2 && numVerticalCells > 2) {
				numVerticalCells--;
				ggState = 1;
				upTheGame = false;
			} else if (upTheGame && ggState > 0) {
				numVerticalCells += 2;
				ggState = 0;
				upTheGame = false;
			} else {
				numVerticalCells = (int) Math
						.round((graphRect.bottom - graphRect.top) / cellSizeX);
				
				if (isPrime(numVerticalCells)) {
					ggState = 2;
					upTheGame = true;
					continue;
				}
			}
			
			cellSizeY = (graphRect.bottom - graphRect.top)
					/ (float) numVerticalCells;

			int numCellsReq = (int) Math.ceil(maxTemperature / dPc)
					- (int) Math.floor(minTemperature / dPc);
			if (numCellsReq > numVerticalCells) {
				//dPcIndex++;
				upTheGame = true;
				continue;
			}
			
			// Check that the weather icons will fit above the graph
			if (iconHeight + iconSpacingY > cellSizeY
					* (numVerticalCells - numCellsReq + (Math
							.ceil(maxTemperature / dPc) - maxTemperature
							/ dPc)))
			{
				//dPcIndex++;
				upTheGame = true;
				continue;
			}

			int startCell = (int)Math.ceil(numVerticalCells / 2.0f - numCellsReq / 2.0f);
			
			// Center range
			while ((startCell > 0) && (iconHeight + iconSpacingY > cellSizeY
					* (numVerticalCells - numCellsReq - startCell + (Math
					.ceil(maxTemperature / dPc) - maxTemperature / dPc))))
			{
				startCell--;
			}

			// Ensure that there is enough space below temperature graph
			if (5 * dp > cellSizeY * (startCell + (minTemperature / dPc - Math.floor(minTemperature / dPc)))) {
				//dPcIndex++;
				upTheGame = true;
				continue;
			}
			
			tempRangeMin = dPc * (float)(Math.floor(minTemperature / dPc) - startCell);
			tempRangeMax = tempRangeMin + dPc * numVerticalCells;

			tempLabels = new String[numVerticalCells + 1];

			float newTextLabelWidth = 0.0f;
			
			for (int i = 0; i <= numVerticalCells; i++) {
				float num = tempRangeMin + dPc * i;
				String formatting = Math.round(dPc) != dPc ? "%.1f\u00B0" : "%.0f\u00B0";
				tempLabels[i] = String.format(formatting, num);
				float tempLabelWidth = temperatureLabels
						.measureText(tempLabels[i]);
				if (tempLabelWidth > newTextLabelWidth) {
					newTextLabelWidth = tempLabelWidth;
				}
			}

			if (newTextLabelWidth > textLabelWidth) {
				textLabelWidth = newTextLabelWidth;
			} else {
				degreesPerCell = dPc;
				break;
			}
		}
		
		// Calculate number of cells between vertical labels
		int numCellsBetweenVerticalLabels = 0;
		while (numCellsBetweenVerticalLabels * cellSizeY < labelTextSize) {
			numCellsBetweenVerticalLabels++;
		}
		
		float iconWidth = 19.0f * dp;
		// Calculate number of cells per icon
		int numCellsPerIcon = 0;
		while ((numCellsPerIcon * cellSizeX < iconWidth) || (numHours % numCellsPerIcon != 0)) {
			numCellsPerIcon++;
		}

		Path graphLines = new Path();
		graphLines.moveTo(graphRect.left, graphRect.top);
		graphLines.lineTo(graphRect.left, graphRect.bottom);
		graphLines.lineTo(graphRect.right, graphRect.bottom);
		graphLines.lineTo(graphRect.right, graphRect.top);

		Path path = buildPath(temperatureValues, tempRangeMax, tempRangeMin, timeFrom, timeTo);
		
		Matrix scaleMatrix = new Matrix();
		scaleMatrix.setScale(graphRect.right - graphRect.left, 
				graphRect.bottom - graphRect.top);
		if (path != null) {
			path.transform(scaleMatrix);
			path.offset(graphRect.left, graphRect.top);
		}
		
		// Create rain graphics
		Path rainBarPath = new Path();
		Path rainBarPath2 = new Path();
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
			rainBarPath.addRect(rainRect, Path.Direction.CCW);
			if (highValue > lowValue && lowValue != numVerticalCells) {
				highValue = Math.min(highValue, numVerticalCells);
				rainRect.bottom = rainRect.top;
				rainRect.top = graphRect.bottom - highValue * cellSizeY;
				rainBarPath2.addRect(rainRect, Path.Direction.CCW);
			}
		}
		
		Paint rectPaint = new Paint() {{
			setColor(colors[BACKGROUND_COLOR]);
			setStyle(Paint.Style.FILL);
			setAntiAlias(true);
		}};
		
		
		NinePatchDrawable niner = getNinePatchDrawable(resources, R.drawable.widget_bg_holo);
		niner.setBounds(0, 0, widgetWidth, widgetHeight);
		//niner.setColorFilter(new PorterDuffColorFilter(colors[BACKGROUND_COLOR], PorterDuff.Mode.MULTIPLY));
		niner.draw(canvas);
		
		final Bitmap gg = getBitmapUnscaled(resources, R.drawable.pattern);
		Rect destX = new Rect(5, 5, widgetWidth - 5, widgetHeight - 5);
		
		Paint backgroundPaint = new Paint() {{
			setStyle(Style.FILL);
			setColor(Color.argb(200, 34, 34, 34));
		}};
		
		canvas.drawRect(destX, backgroundPaint);
		
		Paint bitPaint = new Paint() {{
			setShader(new BitmapShader(gg, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT));
			setColorFilter(new PorterDuffColorFilter(Color.argb(255, 255, 255, 255), PorterDuff.Mode.MULTIPLY));
		}};
		
		canvas.save();
		//canvas.clipRect(graphRect.left + 1.0f, graphRect.top - 1.0f, graphRect.right, graphRect.bottom - 1.0f, Region.Op.DIFFERENCE);
		canvas.drawRect(destX, bitPaint);
		//canvas.drawRoundRect(bgRect, bgRectRounding, bgRectRounding, rectPaint);
		canvas.restore();
		
		// Find weather icon resolution (cells / icon)
		long rtp = timeFrom + sampleResolutionHrs * DateUtils.HOUR_IN_MILLIS / 2;
		
		ArrayList<Rain> sunValues = new ArrayList<Rain>();
		Cursor sunCursor = context.getContentResolver().query(
				Uri.withAppendedPath(viewUri, AixViews.TWIG_FORECASTS),
				null,
				AixForecastsColumns.SUN_RISE + " IS NOT NULL",
				null, null);
		if (sunCursor != null) {
			if (sunCursor.moveToFirst()) {
				do {
					Rain sun = new Rain();
					sun.timeFrom = sunCursor.getLong(AixForecastsColumns.SUN_RISE_COLUMN);
					sun.timeTo = sunCursor.getLong(AixForecastsColumns.SUN_SET_COLUMN);
					sunValues.add(sun);
				} while (sunCursor.moveToNext());
			}
			sunCursor.close();
		}
		
		//Collections.sort(sunValues);
		Collections.sort(sunValues, new Comparator<Rain>() {
			@Override
			public int compare(Rain object1, Rain object2) {
				long diffA = object1.timeTo - object1.timeFrom;
				long diffB = object2.timeTo - object2.timeFrom;
				if (diffA == diffB) return 0;
				return diffA > diffB ? 1 : -1;
			}
		});
		
		//long currentTime = timeFrom;
		
		float transitionWidth = 1.0f / numHours;
		
		float timeRange = timeTo - timeFrom;
		
		canvas.save();
		canvas.clipRect(graphRect.left, graphRect.top - 1.0f, graphRect.right, graphRect.bottom - 1.0f);
		
		Matrix matrix = new Matrix();
		matrix.setScale(graphRect.right - graphRect.left, graphRect.bottom - graphRect.top);
		matrix.postTranslate(graphRect.left, graphRect.top - 1.0f);
		canvas.setMatrix(matrix);
		
		Paint p = new Paint();
		p.setStyle(Style.FILL);
		
//		int day = Color.argb(25 * 255 / 100, 222, 222, 222);
//		int night = Color.argb(25 * 255 / 100, 33, 33, 33);
		
		float marker = Float.NEGATIVE_INFINITY;
		
		for (int i = 0; i < sunValues.size(); i++) {
			Rain sun = sunValues.get(i);
			
			float sunRiseTime = (float)(sun.timeFrom - timeFrom) / timeRange;
			float sunRiseStart = sunRiseTime - transitionWidth / 2.0f;
			float sunRiseEnd = sunRiseTime + transitionWidth / 2.0f;
			
			float midDay = ((sun.timeFrom + sun.timeTo) / 2.0f - timeFrom) / timeRange;
			
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
		
		
		
		// Draw grid
		Paint gridPaint = new Paint() {{
			setColor(colors[GRID_COLOR]);
			setAntiAlias(false);
			setStyle(Paint.Style.STROKE);
			setStrokeWidth(1.0f);
			setStrokeCap(Cap.SQUARE);
		}};
		
		for (int i = 1; i < numHorizontalCells; i++) {
			float xPos = graphRect.left + Math.round(i * cellSizeX);
			//if ((startHour + i) % 24 == 0) {
				//gridPaint.setAlpha(65 * 255 / 100);
				//canvas.drawLine(xPos, graphRect.bottom, xPos, graphRect.top, gridPaint);
				//gridPaint.setAlpha(20 * 255 / 100);
			//} else {
				canvas.drawLine(xPos, graphRect.bottom, xPos, graphRect.top, gridPaint);
			//}
		}

		for (int i = 1; i <= numVerticalCells; i++) {
			float yPos = graphRect.bottom - Math.round(i * cellSizeY);
			canvas.drawLine(graphRect.left, yPos, graphRect.right, yPos, gridPaint);
		}

		// Draw rain bars
		Paint rainBarPaint = new Paint() {{
			setColor(colors[MIN_RAIN_COLOR]);
			setStyle(Paint.Style.FILL);
			setAntiAlias(false);
		}};
		
		canvas.drawPath(rainBarPath, rainBarPaint);
		
		// Draw light rain bars
		Paint lightRainPaint = new Paint() {{
			setColor(colors[MAX_RAIN_COLOR]);
			setStyle(Paint.Style.STROKE);
			setAntiAlias(true);
			setStrokeWidth(1.30f);
			setStrokeCap(Cap.SQUARE);
		}};
		
		canvas.save();
		canvas.clipPath(rainBarPath2);
		
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

		// Old way of drawing light rain
		//Bitmap lightRainTexture = BitmapFactory.decodeResource(context.getResources(), 
		//		   R.drawable.rain_pattern);
		//BitmapShader rainShader = new BitmapShader(lightRainTexture, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
		//rainBarPaint.setShader(rainShader);
		
		// Draw temperature graph
		Paint pLine = new Paint() {{
			setColor(colors[ABOVE_FREEZING_COLOR]);
			setStyle(Paint.Style.STROKE);
			setStrokeCap(Cap.SQUARE);
			setAntiAlias(true);
			setStrokeWidth(2.0f * dp);
		}};
		
		if (path != null) {
			canvas.save();
			
			float freezingTemperature = isFahrenheit ? 32.0f : 0.0f;
			
			if (tempRangeMin >= freezingTemperature) {
				// All positive
				canvas.clipRect(graphRect.left, graphRect.top, graphRect.right, graphRect.bottom);
				canvas.drawPath(path, pLine);
			} else if (tempRangeMax <= freezingTemperature) {
				// All negative
				canvas.clipRect(graphRect.left, graphRect.top, graphRect.right, graphRect.bottom);
				pLine.setColor(colors[BELOW_FREEZING_COLOR]);
				canvas.drawPath(path, pLine);
			} else {
				float q = (float)Math.floor((graphRect.bottom - graphRect.top) * (freezingTemperature - tempRangeMin) / (tempRangeMax - tempRangeMin));
				canvas.clipRect(graphRect.left, graphRect.top, graphRect.right, graphRect.bottom - q);
				canvas.drawPath(path, pLine);
				canvas.clipRect(graphRect.left, graphRect.bottom - q, graphRect.right, graphRect.bottom, Op.REPLACE);
				pLine.setColor(colors[BELOW_FREEZING_COLOR]);
				canvas.drawPath(path, pLine);
			}
			canvas.restore();
		}
		
		// Draw graph border lines
		Paint graphLinesPaint = new Paint() {{
			setColor(colors[GRID_OUTLINE_COLOR]);
			setStyle(Paint.Style.STROKE);
			setAntiAlias(false);
			setStrokeWidth(1.0f);
			setStrokeCap(Cap.SQUARE);
		}};
		canvas.drawPath(graphLines, graphLinesPaint);

		// Draw temperature labels and vertical notches
		int lowLabelWatermark = 0;
		int highLabelWatermark = numVerticalCells;
		float labelTextPaddingX = 2.5f * dp;
		float notchWidth = 3.0f * dp;
		
		if (tempLabels != null) {
			while (true) {
				if (lowLabelWatermark > highLabelWatermark) break;
				// Draw low notch
				float notchY = graphRect.bottom - Math.round(lowLabelWatermark * cellSizeY);
				canvas.drawLine(graphRect.left - notchWidth / 2, notchY, graphRect.left + notchWidth / 2, notchY, graphLinesPaint);
				Rect bounds = new Rect();
				temperatureLabels.getTextBounds(tempLabels[lowLabelWatermark], 0, tempLabels[lowLabelWatermark].length(), bounds);
				// Draw low label
				canvas.drawText(
						tempLabels[lowLabelWatermark],
						graphRect.left - labelTextPaddingX,
						graphRect.bottom - bounds.centerY() - (float)lowLabelWatermark * (graphRect.bottom - graphRect.top) / (float)numVerticalCells,
						temperatureLabels);
				lowLabelWatermark += numCellsBetweenVerticalLabels;
				if (highLabelWatermark < lowLabelWatermark) break;
				// Draw high notch
				notchY = graphRect.bottom - Math.round(highLabelWatermark * cellSizeY);
				canvas.drawLine(graphRect.left - notchWidth / 2, notchY, graphRect.left + notchWidth / 2, notchY, graphLinesPaint);
				temperatureLabels.getTextBounds(tempLabels[highLabelWatermark], 0, tempLabels[highLabelWatermark].length(), bounds);
				canvas.drawText(
						tempLabels[highLabelWatermark],
						graphRect.left - labelTextPaddingX,
						graphRect.bottom - bounds.centerY() - (float)highLabelWatermark * (graphRect.bottom - graphRect.top) / (float)numVerticalCells,
						temperatureLabels);
				highLabelWatermark -= numCellsBetweenVerticalLabels;
			}
		}
		
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
		
		sampleResolutionHrs = Math.max(sampleResolutionHrs, 2);
		
		float iconWidthValOver2 = iconWidth * (timeTo - timeFrom) / (graphRect.right - graphRect.left) / 2.0f;
		
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
							dest.left = (int)Math.round(graphRect.left + Math.round(((float)(iconTimePos - timeFrom) / (float)DateUtils.HOUR_IN_MILLIS) * cellSizeX) - iconWidth / 2.0f);
							dest.top = Math.round(graphRect.bottom - (graphRect.bottom - graphRect.top) * (val - tempRangeMin) / (tempRangeMax - tempRangeMin) - iconHeight - iconSpacingY);
							dest.right = Math.round(dest.left + iconWidth);
							dest.bottom = Math.round(dest.top + iconHeight);
							
							boolean isDay = false;
							
							for (Rain sun : sunValues) {
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
		
		// Draw time stamp labels and horizontal notches
		float notchHeight = 3.0f * dp;
		float labelTextPaddingY = 2.0f * dp;
		
		temperatureLabels.setTextAlign(Paint.Align.CENTER);
		
		boolean use24hours = DateFormat.is24HourFormat(context);
		
		// Set local time zone for drawing labels
		calendar.setTimeInMillis(timeFrom);
		calendar.setTimeZone(locationTime);
		int startHour = calendar.get(Calendar.HOUR_OF_DAY);
		
		for (int i = sampleResolutionHrs; i < numHours; i+= sampleResolutionHrs) {
			float notchX = graphRect.left + Math.round(i * cellSizeX);
			canvas.drawLine(notchX, graphRect.bottom - notchHeight / 2, notchX, graphRect.bottom + notchHeight / 2, graphLinesPaint);
			String hourLabel;
			if (use24hours) {
				hourLabel = String.format("%02d", (startHour + i) % 24);
			} else {
				int hour = (startHour + i) % 12;
				if (0 == hour) hour = 12;
				boolean am = ((startHour + i) % 24) < 12;
				if (sampleResolutionHrs == 2) {
					hourLabel = String.format("%2d%s", hour, am ? "a" : "p" );
				} else {
					hourLabel = String.format("%2d %s", hour, am ? "am" : "pm" );
				}
			}
			canvas.drawText(hourLabel, notchX, graphRect.bottom + labelTextSize + labelTextPaddingY, temperatureLabels);
		}
		
		/*
		if (locationName != null) {
			Paint locationRectPaint = new Paint() {{
				setColor(colors[LOCATION_BACKGROUND_COLOR]);
				setStyle(Paint.Style.FILL);
				setAntiAlias(true);
			}};
			final float locationLabelTextSize = 10.0f * dp;
			Paint locationLabelPaint = new Paint() {{
				setColor(colors[LOCATION_TEXT_COLOR]);
				setAntiAlias(true);
				setTextSize(locationLabelTextSize);
			}};
			
			float locationLabelPaddingX = 4.0f * dp;
			float locationLabelPaddingY = 3.0f * dp;
			float locationLabelPositionX = 7.77f * dp;
			
			Rect locationNameRect = new Rect();
			locationLabelPaint.getTextBounds(locationName, 0, locationName.length(), locationNameRect);
			
			float textLeft = Math.round(graphRect.right - locationLabelPositionX - locationNameRect.width());
			float textBottom = Math.round(graphRect.top + locationNameRect.height() / 3.0f);
			
//			RectF labelBgRect = new RectF(
//					textLeft + locationNameRect.left - 1.0f,
//					textBottom + locationNameRect.top - 1.0f,
//					textLeft + locationNameRect.right,
//					textBottom + locationNameRect.bottom);
			
			RectF labelBgRect = new RectF(
					Math.round(textLeft + locationNameRect.left - locationLabelPaddingX),
					Math.round(textBottom + locationNameRect.top - 1.0f - locationLabelPaddingY),
					Math.round(textLeft + locationNameRect.right + locationLabelPaddingX),
					Math.round(textBottom + locationNameRect.bottom + locationLabelPaddingY));

			canvas.drawRoundRect(labelBgRect, bgRectRounding, bgRectRounding, locationRectPaint);
			canvas.drawText(locationName, textLeft, textBottom, locationLabelPaint);
		}
		*/
		
		
		// Top bar stuff
		final float locationLabelTextSize = 10.0f * dp;
		Paint locationLabelPaint = new Paint() {{
			setColor(colors[LOCATION_TEXT_COLOR]);
			setAntiAlias(true);
			setTextSize(locationLabelTextSize);
		}};
		
		canvas.drawText("MSLP: 101.325 kPa  RH: 45%", graphRect.left + 2.0f, bgRect.top + locationLabelTextSize - 1.0f, locationLabelPaint);
		locationLabelPaint.setTextAlign(Align.RIGHT);
		canvas.drawText(locationName + " (24\u00B0C)", graphRect.right - 2.0f, bgRect.top + locationLabelTextSize - 1.0f, locationLabelPaint);
		
		
		Log.d(TAG, "Drawing widget took: " + (timerStart - System.currentTimeMillis()) + " ms");
		
		return bitmap;
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
	
	private static Path buildPath(ArrayList<Temperature> temperatureValues, float tempRangeMax,
			float tempRangeMin, long startTime, long endTime) {
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
		
		return path;
	}
	
}
