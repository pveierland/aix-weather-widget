package net.veierland.aix.widget;

import static net.veierland.aix.AixUtils.*;

import java.util.Date;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;

import net.veierland.aix.AixProvider.AixIntervalDataForecastColumns;
import net.veierland.aix.AixProvider.AixLocations;
import net.veierland.aix.AixProvider.AixLocationsColumns;
import net.veierland.aix.AixProvider.AixPointDataForecastColumns;
import net.veierland.aix.AixProvider.AixSunMoonData;
import net.veierland.aix.AixProvider.AixSunMoonDataColumns;
import net.veierland.aix.AixProvider.AixViews;
import net.veierland.aix.AixProvider.AixWidgetSettings;
import net.veierland.aix.AixProvider.AixWidgets;
import net.veierland.aix.AixProvider.AixWidgetsColumns;
import net.veierland.aix.IntervalData;
import net.veierland.aix.PointData;
import net.veierland.aix.R;
import net.veierland.aix.SunMoonData;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
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
import android.net.Uri;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.util.Log;

public class AixDetailedWidget {
	
	private static final String TAG = "AixDetailedWidget";

	private static final int BORDER_COLOR = 0;
	private static final int BACKGROUND_COLOR = 1;
	private static final int TEXT_COLOR = 2;
	private static final int PATTERN_COLOR = 3;
	private static final int DAY_COLOR = 4;
	private static final int NIGHT_COLOR = 5;
	private static final int GRID_COLOR = 6;
	private static final int GRID_OUTLINE_COLOR = 7;
	private static final int MAX_RAIN_COLOR = 8;
	private static final int MIN_RAIN_COLOR = 9;
	private static final int ABOVE_FREEZING_COLOR = 10;
	private static final int BELOW_FREEZING_COLOR = 11;

	private static final int[] weatherIconsDay = {
			R.drawable.weather_icon_day_sun,
			R.drawable.weather_icon_day_polar_lightcloud,
			R.drawable.weather_icon_day_partlycloud,
			R.drawable.weather_icon_cloud,
			R.drawable.weather_icon_day_lightrainsun,
			R.drawable.weather_icon_day_polar_lightrainthundersun,
			R.drawable.weather_icon_day_polar_sleetsun,
			R.drawable.weather_icon_day_snowsun,
			R.drawable.weather_icon_lightrain,
			R.drawable.weather_icon_rain,
			R.drawable.weather_icon_rainthunder,
			R.drawable.weather_icon_sleet,
			R.drawable.weather_icon_snow,
			R.drawable.weather_icon_snowthunder,
			R.drawable.weather_icon_fog };
	
	private static final int[] weatherIconsNight = {
			R.drawable.weather_icon_night_sun,
			R.drawable.weather_icon_night_lightcloud,
			R.drawable.weather_icon_night_partlycloud,
			R.drawable.weather_icon_cloud,
			R.drawable.weather_icon_night_lightrainsun,
			R.drawable.weather_icon_night_lightrainthundersun,
			R.drawable.weather_icon_night_sleetsun,
			R.drawable.weather_icon_night_snowsun,
			R.drawable.weather_icon_lightrain,
			R.drawable.weather_icon_rain,
			R.drawable.weather_icon_rainthunder,
			R.drawable.weather_icon_sleet,
			R.drawable.weather_icon_snow,
			R.drawable.weather_icon_snowthunder,
			R.drawable.weather_icon_fog };
	
	private static final int[] weatherIconsPolar = {
			R.drawable.weather_icon_polar_sun,
			R.drawable.weather_icon_day_polar_lightcloud,
			R.drawable.weather_icon_polar_partlycloud,
			R.drawable.weather_icon_cloud,
			R.drawable.weather_icon_polar_lightrainsun,
			R.drawable.weather_icon_day_polar_lightrainthundersun,
			R.drawable.weather_icon_day_polar_sleetsun,
			R.drawable.weather_icon_polar_snowsun,
			R.drawable.weather_icon_lightrain,
			R.drawable.weather_icon_rain,
			R.drawable.weather_icon_rainthunder,
			R.drawable.weather_icon_sleet,
			R.drawable.weather_icon_snow,
			R.drawable.weather_icon_snowthunder,
			R.drawable.weather_icon_fog };
	
	private final Context mContext;
	private final Uri mWidgetUri, mViewUri;
	private final int mNumHours, mNumWeatherDataBufferHours;
	
	private int mWidgetRows, mWidgetColumns;
	
	private int[] mColors;
	
	private String mLocationName;
	
	private TimeZone mLocationTimeZone;
	private TimeZone mUtcTimeZone = TimeZone.getTimeZone("UTC");
	
	private long mTimeNow, mTimeFrom, mTimeTo;
	private int mNumHoursBetweenSamples;
	private ArrayList<PointData> mPointData;
	private ArrayList<SunMoonData> mSunMoonData;
	private ArrayList<IntervalData> mIntervalData;
	
	private boolean mUseFahrenheit = false, mUseInches = false, mDrawDayLightEffect = true;
	
	private float mPrecipitationScale = Float.NaN;
	
	private boolean mDrawBorder = true;
	private float mBorderThickness = Float.NaN, mBorderRounding = Float.NaN;
	
	private static final int TOP_TEXT_NEVER = 1;
	private static final int TOP_TEXT_LANDSCAPE = 2;
	private static final int TOP_TEXT_PORTRAIT = 3;
	private static final int TOP_TEXT_ALWAYS = 4;
	
	private int mTopTextVisibility = 0;

	private float mMaxTemperature, mMinTemperature;
	
	private Paint mBorderPaint, mBackgroundPaint, mPatternPaint, mTextPaint, mLabelPaint;
	private Paint mGridPaint, mGridOutlinePaint;
	private Paint mAboveFreezingTemperaturePaint, mBelowFreezingTemperaturePaint;
	private Paint mMinRainPaint, mMaxRainPaint;
	
	private AixDetailedWidget(final Context context, final Uri widgetUri, final Uri viewUri) {
		mContext = context;
		mWidgetUri = widgetUri;
		mViewUri = viewUri;
		
		mNumHours = 24;
		mNumWeatherDataBufferHours = 6;
	}
	
	public static AixDetailedWidget build(final Context context, final Uri widgetUri, final Uri viewUri) throws AixWidgetDrawException {
		AixDetailedWidget widget = new AixDetailedWidget(context, widgetUri, viewUri);
		return widget.initialize();
	}
	
	private AixDetailedWidget initialize() throws AixWidgetDrawException {
		setupWidgetSize();
		setupLocation();
		loadConfiguration();
		setupTimesAndPointData();
		setupIntervalData();
		setupEpochAndTimes();
		setupSampleTimes();
		validatePointData();
		setupSunMoonData();
		setupPaints();

		return this;
	}
	
	public Bitmap render(boolean isLandscape) throws AixWidgetDrawException {
		DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
		final float dp = dm.density;
		
		int widgetWidth = (int)Math.round(((isLandscape
				? (106.0f * mWidgetColumns) : (80.0f * mWidgetColumns)) - 2.0f) * dp);
		int widgetHeight = (int)Math.round(((isLandscape
				? (74.0f * mWidgetRows) : (100.0f * mWidgetRows)) - 2.0f) * dp);

		//Log.d(TAG, "widgetWidth=" + widgetWidth + " widgetHeight=" + widgetHeight);
		
		boolean isWidgetWidthOdd = widgetWidth % 2 == 1;
		boolean isWidgetHeightOdd = widgetHeight % 2 == 1;
		
		Rect widgetBounds = new Rect(
				isWidgetWidthOdd ? 1 : 0,
				isWidgetHeightOdd ? 1 : 0,
				widgetWidth, widgetHeight);
		
		Bitmap bitmap = Bitmap.createBitmap(widgetWidth, widgetHeight, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		
		float borderPadding = 1.0f;
		
		float widgetBorder = Math.round(borderPadding + mBorderRounding + (mBorderThickness - mBorderRounding));
		
		RectF backgroundRect = new RectF(
				(float)widgetBounds.left + widgetBorder,
				(float)widgetBounds.top + widgetBorder,
				(float)widgetBounds.right - widgetBorder,
				(float)widgetBounds.bottom - widgetBorder);
		
		float[] degreesPerCellOptions = { 0.5f, 1.0f, 2.0f, 2.5f, 5.0f, 10.0f,
				20.0f, 25.0f, 50.0f, 100.0f };
		int dPcIndex = 0;

		float textLabelWidth = 0.0f;
		int numHorizontalCells = mNumHours, numVerticalCells = 2;
		float cellSizeX = 10.0f, cellSizeY = 10.0f;
		Rect graphRect = new Rect();

		float iconHeight = 19.0f * dp, iconWidth = 19.0f * dp, iconSpacingY = 2.0f * dp;
		float tempRangeMax = 0.0f, tempRangeMin = 0.0f;

		String[] tempLabels = null;

		final float textSize = 10.0f * dp;
		final float labelTextSize = 9.0f * dp;
		
		setupPaintDimensions(dp, textSize, labelTextSize);
		
		float degreesPerCell;
		
		boolean drawTopText = (mTopTextVisibility == TOP_TEXT_ALWAYS ||
				(isLandscape && mTopTextVisibility == TOP_TEXT_LANDSCAPE) ||
				(!isLandscape && mTopTextVisibility == TOP_TEXT_PORTRAIT));
		
		while (true) {
			if (dPcIndex == degreesPerCellOptions.length) {
				throw AixWidgetDrawException.buildGraphDimensionFailure();
			}
			
			graphRect.left = (int)Math.round(backgroundRect.left + textLabelWidth + 5.0f * dp);
			if (drawTopText) {
				graphRect.top = (int)Math.round(backgroundRect.top + labelTextSize + 4.0f * dp);
			} else {
				graphRect.top = (int)Math.round(backgroundRect.top + labelTextSize / 2.0f + 2.0f * dp);
			}
			graphRect.right = (int)Math.round(backgroundRect.right - 5.0f * dp);
			graphRect.bottom = (int)Math.round(backgroundRect.bottom - labelTextSize - 6.0f * dp);
			
			for (int j = 2; j <= 100; j++) {
				if (mNumHours % j == 0) {
					if ((float)graphRect.width() / (float)j >= 8.0f * dp) {
						numHorizontalCells = j;
					} else {
						break;
					}
				}
			}
			
			cellSizeX = (float)(graphRect.right - graphRect.left) / (float) numHorizontalCells;
			degreesPerCell = degreesPerCellOptions[dPcIndex];
			
			int numCellsReq = (int) Math.ceil(mMaxTemperature / degreesPerCell) - (int) Math.floor(mMinTemperature / degreesPerCell);
			float tCellSizeY = ((float)graphRect.height() - iconHeight - iconSpacingY) / ((mMaxTemperature / degreesPerCell) - (float)Math.floor(mMinTemperature / degreesPerCell));
			numVerticalCells = Math.max((int)Math.ceil((float)graphRect.height() / tCellSizeY), 1);
			
			cellSizeY = (graphRect.bottom - graphRect.top) / (float) numVerticalCells;
			
			if (2.0f * dp > cellSizeY * ((mMinTemperature / degreesPerCell - Math.floor(mMinTemperature / degreesPerCell)))) {
				numVerticalCells++;
			}
			
			if (((float)graphRect.height() / (float) numVerticalCells) < mLabelPaint.getTextSize()) {
				while (isPrime(numVerticalCells)) numVerticalCells++;
			}
			
			cellSizeY = (float)graphRect.height() / (float) numVerticalCells;
			
			if (cellSizeY < 8.0f * dp) {
				dPcIndex++;
				continue;
			}
			
			// Center range as far as possible (need to fix proper centering)
			int startCell = numVerticalCells - numCellsReq;
			while ((startCell > 0) && (iconHeight > cellSizeY// + iconSpacingY > cellSizeY
					* (numVerticalCells - numCellsReq - startCell + (Math
					.ceil(mMaxTemperature / degreesPerCell) - mMaxTemperature / degreesPerCell))))
			{
				startCell--;
			}
			
			tempRangeMin = degreesPerCell * (float)(Math.floor(mMinTemperature / degreesPerCell) - startCell);
			tempRangeMax = tempRangeMin + degreesPerCell * numVerticalCells;
			
			tempLabels = new String[numVerticalCells + 1];

			float newTextLabelWidth = 0.0f;
			
			boolean isAllBelow10 = true;
			
			for (int j = 0; j <= numVerticalCells; j++) {
				float num = tempRangeMin + degreesPerCell * j;
				if (Math.abs(num) >= 10.0f) isAllBelow10 = false;
				String formatting = Math.round(degreesPerCell) != degreesPerCell
						? "%.1f\u00B0" : "%.0f\u00B0";
				tempLabels[j] = String.format(formatting, num);
				newTextLabelWidth = Math.max(newTextLabelWidth,
						mLabelPaint.measureText(tempLabels[j]));
			}
			
			if (isAllBelow10) newTextLabelWidth += dp;

			if (newTextLabelWidth > textLabelWidth) {
				textLabelWidth = newTextLabelWidth;
			} else {
				break;
			}
		}
		
		Rect graphRectInner = new Rect(graphRect.left + 1, graphRect.top + 1, graphRect.right, graphRect.bottom);
		Rect graphRectOuter = new Rect(graphRect.left, graphRect.top, graphRect.right + 1, graphRect.bottom + 1);
		
		drawBackground(canvas, dp, widgetBounds, backgroundRect, borderPadding);
		drawGrid(canvas, graphRect, numHorizontalCells, numVerticalCells, cellSizeX, cellSizeY);

		if (mDrawDayLightEffect) {
			drawDayAndNight(canvas, graphRect);
		}
		
		Path minRainPath = new Path();
		Path maxRainPath = new Path();
		buildRainPaths(minRainPath, maxRainPath, graphRect, numHorizontalCells, numVerticalCells, cellSizeX, cellSizeY);
		drawRainPaths(canvas, dp, minRainPath, maxRainPath, graphRect);
		
		Path temperaturePath = CatmullRom.buildPath(mPointData, mTimeFrom, mTimeTo, tempRangeMax, tempRangeMin, graphRect);
		if (temperaturePath != null) {
			drawTemperature(canvas, dp, temperaturePath, tempRangeMax, tempRangeMin, graphRect, graphRectInner);
		}
		
		drawGridOutline(canvas, graphRect);
		drawTemperatureLabels(canvas, dp, tempLabels, graphRect, numVerticalCells, cellSizeY);
		drawHourLabels(canvas, dp, backgroundRect, graphRect, numHorizontalCells, cellSizeX);
		drawWeatherIcons(canvas, dp, iconHeight, iconWidth, iconSpacingY, graphRect, numHorizontalCells, cellSizeX, tempRangeMin, tempRangeMax);
		
		if (drawTopText) {
			float pressure = Float.NaN, humidity = Float.NaN, temperature = Float.NaN;
			
			long error = Long.MAX_VALUE;
			
			for (PointData p : mPointData) {
				long e = p.mTime - mTimeNow;
				if (e >= 0 && e <= mNumHoursBetweenSamples * DateUtils.HOUR_IN_MILLIS && e < error) {
					pressure = p.mPressure;
					humidity = p.mHumidity;
					temperature = p.mTemperature;
					error = e;
				}
			}
			
			drawInfoText(canvas, dp, pressure, humidity, temperature, backgroundRect, graphRect);
		}
		
		return bitmap;
	}
	
	private void buildRainPaths(Path minRainPath, Path maxRainPath, Rect graphRect,
			int numHorizontalCells, int numVerticalCells, float cellSizeX, float cellSizeY)
	{
		float[] rainValues = new float[numHorizontalCells];
		float[] rainMinValues = new float[numHorizontalCells];
		float[] rainMaxValues = new float[numHorizontalCells];
		
		Arrays.fill(rainValues, Float.NaN);
		Arrays.fill(rainMinValues, Float.NaN);
		Arrays.fill(rainMaxValues, Float.NaN);
		
		int[] pointers = new int[numHorizontalCells];
		int[] precision = new int[numHorizontalCells];
		
		Arrays.fill(pointers, -1);
		
		for (int i = 0; i < mIntervalData.size(); i++) {
			IntervalData d = mIntervalData.get(i);
			if (d.timeFrom == d.timeTo) continue;
			
			int startCell = (int)Math.floor((float)numHorizontalCells *
					(float)(d.timeFrom - mTimeFrom) / (float)(mTimeTo - mTimeFrom));
			
			if (startCell >= numHorizontalCells) continue;
			
			float endCellPos = (float)numHorizontalCells *
					(float)(d.timeTo - mTimeFrom) / (float)(mTimeTo - mTimeFrom);
			int endCell = (endCellPos == Math.round(endCellPos))
					? (int)endCellPos - 1 : (int)Math.ceil(endCellPos);
			
			startCell = lcap(startCell, 0);
			endCell = hcap(endCell, numHorizontalCells - 1);
			
			for (int j = startCell; j <= endCell; j++) {
				if (pointers[j] == -1) {
					rainValues[j] = d.rainValue;
					rainMinValues[j] = d.rainMinValue;
					rainMaxValues[j] = d.rainMaxValue;
					
					precision[j] = d.lengthInHours();
					pointers[j] = i;
				} else if (precision[j] == d.lengthInHours()) {
					boolean modified = false;
					if (d.rainValue > rainValues[j]) {
						rainValues[j] = d.rainValue;
						modified = true;
					}
					if (d.rainMinValue > rainMinValues[j]) {
						rainMinValues[j] = d.rainMinValue;
						modified = true;
					}
					if (d.rainMaxValue > rainMaxValues[j]) {
						rainMaxValues[j] = d.rainMaxValue;
						modified = true;
					}
					if (modified) {
						pointers[j] = -2;
					}
				}
			}
		}
		
		int i = 0;
		while (i < numHorizontalCells) {
			float minVal = Float.NaN, maxVal = Float.NaN;
			
			if (!Float.isNaN(rainMinValues[i]) && !Float.isNaN(rainMaxValues[i])) {
				minVal = rainMinValues[i];
				maxVal = rainMaxValues[i];
			} else if (!Float.isNaN(rainValues[i])) {
				minVal = rainValues[i];
			}
			
			if (Float.isNaN(minVal)) {
				i++;
				continue;
			}
			
			minVal = hcap(minVal / mPrecipitationScale, numVerticalCells);
			
			int endCell = i + 1;
			while (	(endCell < numHorizontalCells) &&
					(pointers[i] == pointers[endCell]) &&
					(pointers[endCell] != -2))
			{
				endCell++;
			}
			
			RectF rainRect = new RectF(
					graphRect.left + Math.round(i * cellSizeX) + 1.0f,
					graphRect.bottom - minVal * cellSizeY,
					graphRect.left + Math.round(endCell * cellSizeX),
					graphRect.bottom);
			minRainPath.addRect(rainRect, Path.Direction.CCW);
			
			if (!Float.isNaN(maxVal) && maxVal > minVal && minVal < numVerticalCells) {
				maxVal = hcap(maxVal / mPrecipitationScale, numVerticalCells);
				rainRect.bottom = rainRect.top;
				rainRect.top = graphRect.bottom - maxVal * cellSizeY;
				maxRainPath.addRect(rainRect, Path.Direction.CCW);
			}
			
			i = endCell;
		}
	}
	
	private void drawBackground(Canvas canvas, final float dp, Rect widgetBounds,
			RectF backgroundRect, float borderPadding)
	{
		if (mBorderThickness > 0.0f) {
			RectF borderRect = new RectF(
					Math.round(widgetBounds.left + borderPadding),
					Math.round(widgetBounds.top + borderPadding),
					Math.round(widgetBounds.right - borderPadding),
					Math.round(widgetBounds.bottom - borderPadding));
			
			canvas.save();
			canvas.clipRect(backgroundRect, Region.Op.DIFFERENCE);
			canvas.drawRoundRect(borderRect, mBorderRounding, mBorderRounding, mBorderPaint);
			canvas.restore();
			
			canvas.drawRect(backgroundRect, mBackgroundPaint);
			canvas.drawRect(backgroundRect, mPatternPaint);
		} else {
			canvas.drawRoundRect(backgroundRect, mBorderRounding, mBorderRounding, mBackgroundPaint);
			canvas.drawRoundRect(backgroundRect, mBorderRounding, mBorderRounding, mPatternPaint);
		}
	}
	
	private void drawDayAndNight(Canvas canvas, Rect graphRect)
	{
		if (mSunMoonData == null) return;
		
		float timeRange = mTimeTo - mTimeFrom;
		float transitionWidthDefault = (float)DateUtils.HOUR_IN_MILLIS / timeRange;
		
		canvas.save();
		canvas.clipRect(graphRect.left + 1, graphRect.top, graphRect.right, graphRect.bottom);
		
		Matrix matrix = new Matrix();
		matrix.setScale(graphRect.width(), graphRect.height());
		matrix.postTranslate(graphRect.left + 1, graphRect.top);
		canvas.setMatrix(matrix);
		
		final int DAY = 1, NIGHT = 2;
		int state = 0; int firstState = NIGHT;
		
		ArrayList<Long> sunToggleTimes = new ArrayList<Long>();
		
		for (SunMoonData s: mSunMoonData) {
			if (s.mSunRise == AixSunMoonData.NEVER_RISE) {
				if (state != NIGHT) {
					sunToggleTimes.add(s.mDate);
					if (state == 0) {
						firstState = NIGHT;
					}
					state = NIGHT;
				}
				continue;
			}
			if (s.mSunSet == AixSunMoonData.NEVER_SET) {
				if (state != DAY) {
					sunToggleTimes.add(s.mDate);
					if (state == 0) {
						firstState = DAY;
					}
					state = DAY;
				}
				continue;
			}
			
			if (s.mSunRise > 0 && state != DAY) {
				sunToggleTimes.add(s.mSunRise);
				if (state == 0) {
					firstState = DAY;
				}
				state = DAY;
			}
			if (s.mSunSet > 0 && state != NIGHT) {
				sunToggleTimes.add(s.mSunSet);
				if (state == 0) {
					firstState = NIGHT;
				}
				state = NIGHT;
			}
		}
		
		sunToggleTimes.add(mSunMoonData.get(mSunMoonData.size() - 1).mDate + DateUtils.DAY_IN_MILLIS);
		
		Paint p = new Paint();
		p.setStyle(Style.FILL);
		
		float marker = Float.NEGATIVE_INFINITY;
		long last = -1, current = -1, next = -1;
		state = 0;
		
		Iterator<Long> iterator = sunToggleTimes.iterator();

		do {
			state = state == 0 ? firstState : (state == DAY ? NIGHT : DAY);
			
			last = current; current = next;
			if (iterator.hasNext()) {
				next = iterator.next();
			} else {
				if (last == -1 || current == -1) {
					break;
				}
				next = -1;
			}
			if (last == -1) continue;
			
			float togglePos = (float)(current - mTimeFrom) / timeRange;
			float transitionWidth = transitionWidthDefault;
			
			if (marker != Float.NEGATIVE_INFINITY) {
				transitionWidth = hcap(transitionWidth, (float)(current - marker) / timeRange);
			}
			if (next != -1) {
				transitionWidth = hcap(transitionWidth, (float)(next - current) / timeRange / 2.0f);
			}
			
			float toggleStart = togglePos - transitionWidth;
			float toggleEnd = togglePos + transitionWidth;
			
			p.setShader(new LinearGradient(toggleStart, 0.0f, toggleEnd, 0.0f,
					mColors[state == DAY ? DAY_COLOR : NIGHT_COLOR],
					mColors[state == DAY ? NIGHT_COLOR : DAY_COLOR],
					Shader.TileMode.CLAMP));
			
			if (marker == Float.NEGATIVE_INFINITY) marker = 0.0f;
			
			canvas.drawRect(marker, 0.0f, toggleEnd, 1.0f, p);
			
			marker = toggleEnd;
		} while (next != -1);
		
		canvas.restore();
	}
	
	private void drawGrid(Canvas canvas, Rect graphRect, int numHorizontalCells,
			int numVerticalCells, float cellSizeX, float cellSizeY)
	{
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
		
		int rightOffset = mDrawDayLightEffect ? 0 : 1;
		
		canvas.save();
		canvas.clipRect(
				graphRect.left + 1,
				graphRect.top,
				graphRect.right + rightOffset,
				graphRect.bottom,
				Region.Op.REPLACE);
		canvas.drawPath(gridPath, mGridPaint);
		canvas.restore();
	}
	
	private void drawGridOutline(Canvas canvas, Rect graphRect) {
		Path gridOutline = new Path();
		gridOutline.moveTo(graphRect.left, graphRect.top);
		gridOutline.lineTo(graphRect.left, graphRect.bottom);
		gridOutline.lineTo(graphRect.right, graphRect.bottom);
		
		if (mDrawDayLightEffect) {
			gridOutline.lineTo(graphRect.right, graphRect.top);
		}
		
		canvas.drawPath(gridOutline, mGridOutlinePaint);
	}
	
	private void drawHourLabels(Canvas canvas, final float dp, RectF backgroundRect, Rect graphRect,
			int numHorizontalCells, float cellSizeX)
	{
		// Draw time stamp labels and horizontal notches
		float notchHeight = 3.5f * dp;
		float labelTextPaddingY = 1.5f * dp;
		
		mLabelPaint.setTextAlign(Paint.Align.CENTER);
		
		Calendar calendar = Calendar.getInstance(mUtcTimeZone);
		calendar.setTimeInMillis(mTimeFrom);
		calendar.setTimeZone(mLocationTimeZone);
		
		int startHour = calendar.get(Calendar.HOUR_OF_DAY);
		float hoursPerCell = (float)mNumHours / (float)numHorizontalCells;
		int numCellsBetweenHorizontalLabels = mNumHoursBetweenSamples < hoursPerCell
				? 1 : (int)Math.floor((float)mNumHoursBetweenSamples / hoursPerCell);
		
		// HARDCODED LIMIT
		if (mNumHours == 24) {
			numCellsBetweenHorizontalLabels = lcap(numCellsBetweenHorizontalLabels, 2);
		}
		
		boolean useShortLabel = false;
		boolean use24hours = DateFormat.is24HourFormat(mContext);
		
		float longLabelWidth = mLabelPaint.measureText(use24hours ? "24" : "12 pm");
		float shortLabelWidth = mLabelPaint.measureText("12p");
		
		while (true) {
			/* Ensure that labels can be evenly spaced, given the number of cells, and make
			 * sure that each label step is a full number of hours.
			 */
			while ((numHorizontalCells % numCellsBetweenHorizontalLabels != 0) ||
					((numCellsBetweenHorizontalLabels * hoursPerCell) !=
							Math.round(numCellsBetweenHorizontalLabels * hoursPerCell)))
			{
				numCellsBetweenHorizontalLabels++;
			}
			
			float spaceBetweenLabels = numCellsBetweenHorizontalLabels * cellSizeX;
			if (spaceBetweenLabels > longLabelWidth * 1.25f) {
				break;
			} else if (!use24hours && spaceBetweenLabels > shortLabelWidth * 1.25f) {
				useShortLabel = true;
				break;
			}
			
			numCellsBetweenHorizontalLabels++;
		}
		
		for (int i = numCellsBetweenHorizontalLabels;
				 i < numHorizontalCells;
				 i+= numCellsBetweenHorizontalLabels)
		{
			float notchX = graphRect.left + Math.round(i * cellSizeX);
			canvas.drawLine(notchX, (float)graphRect.bottom - notchHeight / 2.0f,
					notchX, (float)graphRect.bottom + notchHeight / 2.0f, mGridOutlinePaint);
			
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
			canvas.drawText(hourLabel, notchX, (float)Math.floor(((float)graphRect.bottom + backgroundRect.bottom) / 2.0f + mLabelPaint.getTextSize() / 2.0f), mLabelPaint);
		}
	}

	private void drawInfoText(Canvas canvas, final float dp, float pressure, float humidity,
			float temperature, RectF backgroundRect, Rect graphRect)
	{
		final float locationLabelTextSize = 10.0f * dp;
		
		float topTextSidePadding = 1.0f * dp;
		float topTextBottomPadding = 3.0f * dp;
		
		String pressureString = "";
		if (!Float.isNaN(pressure)) {
			pressureString = mContext.getString(R.string.pressure_top, pressure);
		}
		String humidityString = "";
		if (!Float.isNaN(humidity)) {
			humidityString = mContext.getString(R.string.humidity_top, humidity);
		}
		
		DecimalFormat df = new DecimalFormat(mContext.getString(R.string.rain_scale_format));
		String rainScaleFormatted = df.format(mPrecipitationScale);
			
		String rainScaleUnit = mContext.getString(mUseInches ? R.string.rain_scale_unit_inches
																 : R.string.rain_scale_unit_mm);
		
		String rainScaleString = mContext.getString(R.string.rain_scale_top, rainScaleFormatted, rainScaleUnit);
		
		String temperatureString = "";
		if (!Float.isNaN(temperature)) {
			df = new DecimalFormat(mContext.getString(R.string.temperature_format));
			String temperatureFormatted = df.format(temperature);
			
			String temperatureUnit = mContext.getString(
					mUseFahrenheit ? R.string.temperature_unit_fahrenheit
								   : R.string.temperature_unit_celsius);
			
			temperatureString = ' ' + mContext.getString(
					R.string.temperature_top, temperatureFormatted, temperatureUnit);
		}
		
		float topTextSpace = graphRect.width() - topTextSidePadding * 2.0f;
		
		String ellipsis = "...";
		String spacing = "       ";
		
		boolean isMeasuring = true;
		int measureState = 0;
		
		while ( isMeasuring &&
				topTextSpace < mTextPaint.measureText(pressureString) +
							   mTextPaint.measureText(humidityString) +
							   mTextPaint.measureText(rainScaleString) +
							   mTextPaint.measureText(mLocationName) +
							   mTextPaint.measureText(temperatureString) +
							   mTextPaint.measureText(spacing)) // Spacing between strings
		{
			switch (measureState) {
			case 0:
				if (!Float.isNaN(pressure)) {
					pressureString = mContext.getString(R.string.pressure_top_short, pressure);
				}
				measureState++;
				break;
			case 1:
				if (!Float.isNaN(humidity)) {
					humidityString = mContext.getString(R.string.humidity_top_short, humidity);
				}
				measureState++;
				break;
			case 2:
				if (mLocationName.length() == 0) {
					isMeasuring = false;
					break;
				}
				spacing = spacing + ellipsis;
				measureState++;
			default:
				mLocationName = mLocationName.substring(0, mLocationName.length() - 1);
				if (measureState > mLocationName.length() + 2) isMeasuring = false;
				break;
			}
		}
		
		if (measureState == 3) {
			mLocationName = mLocationName + ellipsis;
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
		
		mTextPaint.setTextAlign(Align.LEFT);
		canvas.drawText(sb.toString(),
				graphRect.left + topTextSidePadding,
				backgroundRect.top + mTextPaint.getTextSize(),
				mTextPaint);
		
		sb.setLength(0);
		sb.append(mLocationName);

		if (!Float.isNaN(temperature)) {
			df = new DecimalFormat(mContext.getString(R.string.temperature_format));
			String temperatureFormatted = df.format(temperature);
			
			String temperatureUnit = mContext.getString(
					mUseFahrenheit ? R.string.temperature_unit_fahrenheit
								   : R.string.temperature_unit_celsius);
			
			sb.append(' ');
			sb.append(mContext.getString(
					R.string.temperature_top,
					temperatureFormatted,
					temperatureUnit));
		}

		mTextPaint.setTextAlign(Align.RIGHT);
		canvas.drawText(sb.toString(),
				graphRect.right - topTextSidePadding,
				backgroundRect.top + mTextPaint.getTextSize(),
				mTextPaint);
	}
	
	private void drawRainPaths(Canvas canvas, final float dp, Path minRainPath, Path maxRainPath,
			Rect graphRect)
	{
		canvas.drawPath(minRainPath, mMinRainPaint);
		canvas.save();
		canvas.clipPath(maxRainPath);
		
		// TODO Technique works, but too expensive?
		float dimensions = (float)Math.sin(Math.toRadians(45)) *
						   (graphRect.height() + graphRect.width());
		float dimensions2 = dimensions / 2.0f;
		float ggx = graphRect.left + graphRect.width() / 2.0f;
		float ggy = graphRect.top + graphRect.height() / 2.0f;
	
		Matrix transform = new Matrix();
		transform.setRotate(-45.0f, ggx, ggy);
		canvas.setMatrix(transform);
		
		float ypos = graphRect.top - (dimensions - graphRect.height()) / 2.0f;
		float ytest = graphRect.bottom + (dimensions - graphRect.height()) / 2.0f;
		while (ypos < ytest) {
			canvas.drawLine(ggx - dimensions2, ypos, ggx + dimensions2, ypos, mMaxRainPaint);
			ypos += 2.0f * dp;
		}
		canvas.restore();
	}
	
	private void drawTemperature(Canvas canvas, final float dp, Path temperaturePath,
			float tempRangeMax, float tempRangeMin, Rect graphRect, Rect graphRectInner)
	{
		float freezingTemperature = mUseFahrenheit ? 32.0f : 0.0f;
		
		canvas.save();
		if (tempRangeMin >= freezingTemperature) {
			// All positive
			canvas.clipRect(graphRectInner);
			canvas.drawPath(temperaturePath, mAboveFreezingTemperaturePaint);
		} else if (tempRangeMax <= freezingTemperature) {
			// All negative
			canvas.clipRect(graphRectInner);
			canvas.drawPath(temperaturePath, mBelowFreezingTemperaturePaint);
		} else {
			float freezingPosY = (float)Math.floor(graphRect.height() *
					(freezingTemperature - tempRangeMin) / (tempRangeMax - tempRangeMin));
			
			canvas.clipRect(graphRectInner.left, graphRectInner.top,
					graphRectInner.right, graphRectInner.bottom - freezingPosY);
			canvas.drawPath(temperaturePath, mAboveFreezingTemperaturePaint);
			
			canvas.clipRect(graphRectInner.left, graphRectInner.bottom - freezingPosY,
					graphRectInner.right, graphRectInner.bottom, Op.REPLACE);
			canvas.drawPath(temperaturePath, mBelowFreezingTemperaturePaint);
		}
		canvas.restore();
	}
	
	private void drawTemperatureLabels(Canvas canvas, final float dp, String[] labels,
			Rect graphRect, int numVerticalCells, float cellSizeY)
	{
		float labelTextPaddingX = 2.5f * dp;
		float notchWidth = 3.5f * dp;
		
		// Find the minimum number of cells between each label
		int numCellsBetweenVerticalLabels = 1;
		while (numCellsBetweenVerticalLabels * cellSizeY < mLabelPaint.getTextSize()) {
			numCellsBetweenVerticalLabels++;
		}
		// Ensure that labels can be evenly spaced, given the number of cells
		while (numVerticalCells % numCellsBetweenVerticalLabels != 0) {
			numCellsBetweenVerticalLabels++;
		}
		
		if (labels.length < numVerticalCells) return;
		
		mLabelPaint.setTextAlign(Paint.Align.RIGHT);
		
		for (int i = 0; i <= numVerticalCells; i += numCellsBetweenVerticalLabels) {
			float notchY = graphRect.bottom - Math.round(i * cellSizeY);
			canvas.drawLine(
					graphRect.left - notchWidth / 2, notchY,
					graphRect.left + notchWidth / 2, notchY,
					mGridOutlinePaint);
			
			Rect bounds = new Rect();
			mLabelPaint.getTextBounds(labels[i], 0, labels[i].length(), bounds);
			
			canvas.drawText(
					labels[i],
					graphRect.left - labelTextPaddingX,
					graphRect.bottom - bounds.centerY()
							- (float)(i * graphRect.height()) / (float)numVerticalCells,
					mLabelPaint);
		}
	}
	
	private void drawWeatherIcons(Canvas canvas, final float dp, float iconHeight, float iconWidth,
			float iconSpacingY, Rect graphRect, int numHorizontalCells, float cellSizeX,
			float tempRangeMin, float tempRangeMax)
	{
		// Calculate number of cells per icon
		float hoursPerCell = (float)mNumHours / (float)numHorizontalCells;
		int numCellsPerIcon = (int)Math.ceil((float)mNumHoursBetweenSamples / hoursPerCell);
		
		while (	(numCellsPerIcon * cellSizeX < iconWidth) ||
				((float)mNumHours % (numCellsPerIcon * hoursPerCell) != 0.0f))
		{
			numCellsPerIcon++;
		}
		
		int hoursPerIcon = (int)(numCellsPerIcon * hoursPerCell);
		
		float iconWidthValOver4 = iconWidth * (mTimeTo - mTimeFrom) / graphRect.width() / 4.0f;
		
		long loMarker = mTimeFrom + hoursPerIcon * DateUtils.HOUR_IN_MILLIS / 2;
		long hiMarker = mTimeTo - hoursPerIcon * DateUtils.HOUR_IN_MILLIS / 2;
		
		float tempRange = tempRangeMax - tempRangeMin;
		
		Calendar calendar = Calendar.getInstance(mUtcTimeZone);
		
		for (IntervalData dataPoint : mIntervalData) {
			if ((dataPoint.lengthInHours() == hoursPerIcon || dataPoint.lengthInHours() == 0) && dataPoint.weatherIcon >= 1 && dataPoint.weatherIcon <= 15) {
				long iconTimePos = (dataPoint.timeFrom + dataPoint.timeTo) / 2;
				if (iconTimePos < loMarker || iconTimePos > hiMarker) continue;
				
				PointF Z1 = interpolateTemperature(iconTimePos - (long)Math.round(iconWidthValOver4));
				PointF Z2 = interpolateTemperature(iconTimePos + (long)Math.round(iconWidthValOver4));
				
				float val = Float.NEGATIVE_INFINITY;
				if (Z1 != null) val = Math.max(val, Z1.y);
				if (Z2 != null) val = Math.max(val, Z2.y);
				if (val == Float.NEGATIVE_INFINITY) {
					continue;
				}
				
				int iconX = Math.round(graphRect.left - iconWidth / 2.0f +
						Math.round(((float)(iconTimePos - mTimeFrom) /
								((float)DateUtils.HOUR_IN_MILLIS * hoursPerCell)) * cellSizeX));
				int iconY = Math.round((float)graphRect.bottom - iconHeight - iconSpacingY -
						(float)graphRect.height() * (val - tempRangeMin) / tempRange);
				
				iconY = lcap(iconY, graphRect.top);
				iconY = hcap(iconY, graphRect.bottom - (int)Math.ceil(iconHeight));
				
				Rect dest = new Rect(iconX, iconY,
						Math.round(iconX + iconWidth), Math.round(iconY + iconHeight));
				
				calendar.setTimeInMillis(iconTimePos);
				truncateDay(calendar);
				long iconDate = calendar.getTimeInMillis();
				
				int[] weatherIcons = weatherIconsNight;
				
				for (SunMoonData smd : mSunMoonData) {
					if (smd.mDate == iconDate) {
						if (smd.mSunRise == AixSunMoonData.NEVER_RISE) {
							weatherIcons = weatherIconsPolar;
						} else if (smd.mSunSet == AixSunMoonData.NEVER_SET) {
							weatherIcons = weatherIconsDay;
						}
					}
					if (smd.mSunRise < iconTimePos && smd.mSunSet > iconTimePos) {
						weatherIcons = weatherIconsDay;
					}
				}
				
				Bitmap weatherIcon = ((BitmapDrawable)mContext.getResources().
						getDrawable(weatherIcons[dataPoint.weatherIcon - 1])).getBitmap();
				canvas.drawBitmap(weatherIcon, null, dest, null);
				
				loMarker = iconTimePos + hoursPerIcon * DateUtils.HOUR_IN_MILLIS;
			}
		}
		
	}
	
	private PointF interpolateTemperature(long time) {
		PointData before = null, at = null, after = null;
		int beforeIndex = -1, atIndex = -1, afterIndex = -1;
		
		for (int i = 0; i < mPointData.size(); i++) {
			PointData p = mPointData.get(i);
			if (	(p.mTime < time) && // && p.mTime >= mTimeFrom) &&
					(before == null || p.mTime > before.mTime))
			{
				before = p;
				beforeIndex = i;
			}
			if (p.mTime == time) {
				at = p;
				atIndex = i;
			}
			if (	(p.mTime > time) && // && p.mTime <= mTimeTo) &&
					(after == null || p.mTime < after.mTime))
			{
				after = p;
				afterIndex = i;
			}
		}
		
		PointData Q1 = null, Q2 = null, Q3 = null, Q4 = null;
		
		if (beforeIndex != -1) {
			if (atIndex == -1 && afterIndex == -1) {
				return null;
			}
			Q2 = before;
			if (beforeIndex > 0) {
				Q1 = mPointData.get(beforeIndex - 1);
			} else {
				Q1 = Q2;
			}
			
			if (atIndex != -1) {
				Q3 = at;
				if (afterIndex != -1) {
					Q4 = mPointData.get(afterIndex);
				} else {
					Q4 = Q3;
				}
			} else {
				Q3 = after;
				if (afterIndex < mPointData.size() - 1) {
					Q4 = mPointData.get(afterIndex + 1);
				} else {
					Q4 = Q3;
				}
			}
		} else {
			if (atIndex == -1 || afterIndex == -1) {
				return null;
			}
			
			Q1 = Q2 = at;
			Q3 = after;
			
			if (afterIndex < mPointData.size() - 1) {
				Q4 = mPointData.get(afterIndex + 1);
			} else {
				Q4 = Q3;
			}
		}

		double timeRange = (double)(mTimeTo - mTimeFrom);
		double qx = (double)(time - mTimeFrom) / timeRange;
		
		double q1x = (double)(Q1.mTime - mTimeFrom) / timeRange;
		double q2x = (double)(Q2.mTime - mTimeFrom) / timeRange;
		double q3x = (double)(Q3.mTime - mTimeFrom) / timeRange;
		double q4x = (double)(Q4.mTime - mTimeFrom) / timeRange;

		qx = Math.max(qx, q2x);
		qx = Math.min(qx, q3x);
		
		double a = -0.5f * q1x + 1.5f * q2x - 1.5f * q3x + 0.5f * q4x;
		double b = q1x - 2.5f * q2x + 2.0f * q3x - 0.5f * q4x;
		double c = -0.5f * q1x + 0.5f * q3x;
		double d = q2x - qx;
		
		double t = Double.NaN;
		
		if (a != 0.0f) {
			double A = b / a;
			double B = c / a;
			double C = d / a;
			
			double Q = (3.0f * B - A * A) / 9.0f;
			double R = (9.0f * A * B - 27.0f * C - 2.0f * Math.pow(A, 3.0f)) / 54.0f;
			double D = Math.pow(Q, 3.0f) + Math.pow(R, 2.0f);
			
			if (D < 0.0f) {
				double theta = Math.acos(R / Math.sqrt(Math.pow(-Q, 3.0f)));
				double x1 = 2.0f * Math.sqrt(-Q) * Math.cos(theta / 3.0f) - A / 3.0f;
				double x2 = 2.0f * Math.sqrt(-Q) * Math.cos((theta + 2 * Math.PI) / 3.0f) - A / 3.0f;
				double x3 = 2.0f * Math.sqrt(-Q) * Math.cos((theta + 4 * Math.PI) / 3.0f) - A / 3.0f;
				
				double error = Double.MAX_VALUE;
				
				if (Math.abs(x1 - 0.5f) < error) {
					t = x1;
					error = Math.abs(x1 - 0.5f);
				}
				if (Math.abs(x2 - 0.5f) < error) {
					t = x2;
					error = Math.abs(x2 - 0.5f);
				}
				if (Math.abs(x3 - 0.5f) < error) {
					t = x3;
					error = Math.abs(x3 - 0.5f);
				}
			} else {
				double common = 2.0f * Math.pow(b, 3.0f) - 9.0f * a * b * c + 27.0f * Math.pow(a, 2.0f) * d;
				double temp = Math.pow(b, 2.0f) - 3.0f * a * c;
				double inner = Math.sqrt(Math.pow(common, 2.0f) - 4.0f * Math.pow(temp, 3.0f));
				t = (-b - Math.cbrt(0.5f * (common + inner)) - Math.cbrt(0.5f * (common - inner))) / (3.0f * a);
			}
		} else if (b != 0.0f) {
			double discriminant = Math.pow(c, 2.0f) - 4.0f * b * d;
			if (discriminant > 0.0f) {
				double x1 = (-c + Math.sqrt(discriminant)) / (2.0f * b);
				double x2 = (-c - Math.sqrt(discriminant)) / (2.0f * b);
				
				double error = Double.MAX_VALUE;
				
				if (Math.abs(x1 - 0.5f) < error) {
					t = x1;
					error = Math.abs(x1 - 0.5f);
				}
				if (Math.abs(x2 - 0.5f) < error) {
					t = x2;
					error = Math.abs(x2 - 0.5f);
				}
			} else if (discriminant == 0.0f) {
				t = -c / (2.0f * b);
			}
		} else if (c != 0.0f) {
			double x = -d / c;
			if (x >= 0.0f && x <= 1.0f) {
				t = x;
			}
		}
		
		if (Double.isNaN(t)) {
			return null;
		}
		
		t = Math.min(t, 1.0f);
		t = Math.max(t, 0.0f);
		
		double QX = a * Math.pow(t, 3.0f) + b * Math.pow(t, 2.0f) + c * t + q2x;
		double QY = (-0.5f * Q1.mTemperature + 1.5f * Q2.mTemperature - 1.5f * Q3.mTemperature + 0.5f * Q4.mTemperature) * Math.pow(t, 3.0f)
				+ (Q1.mTemperature - 2.5f * Q2.mTemperature + 2.0f * Q3.mTemperature - 0.5f * Q4.mTemperature) * Math.pow(t, 2.0f)
				+ (0.5f * Q3.mTemperature - 0.5f * Q1.mTemperature) * t + Q2.mTemperature;
		
		return new PointF((float)QX, (float)QY);
	}
	
	private void loadConfiguration() {
		final Context context = mContext;
		// Set up default colors
		final Resources resources = context.getResources();
		mColors = new int[12];
		mColors[BORDER_COLOR] = resources.getColor(R.color.border);
		mColors[BACKGROUND_COLOR] = resources.getColor(R.color.background);
		mColors[TEXT_COLOR] = resources.getColor(R.color.text);
		mColors[PATTERN_COLOR] = resources.getColor(R.color.pattern);
		mColors[DAY_COLOR] = resources.getColor(R.color.day);
		mColors[NIGHT_COLOR] = resources.getColor(R.color.night);
		mColors[GRID_COLOR] = resources.getColor(R.color.grid);
		mColors[GRID_OUTLINE_COLOR] = resources.getColor(R.color.grid_outline);
		mColors[MAX_RAIN_COLOR] = resources.getColor(R.color.maximum_rain);
		mColors[MIN_RAIN_COLOR] = resources.getColor(R.color.minimum_rain);
		mColors[ABOVE_FREEZING_COLOR] = resources.getColor(R.color.above_freezing);
		mColors[BELOW_FREEZING_COLOR] = resources.getColor(R.color.below_freezing);
		
		// Get widget settings
		Cursor widgetSettingsCursor = mContext.getContentResolver().query(
				Uri.withAppendedPath(mWidgetUri, AixWidgets.TWIG_SETTINGS),
				null, null, null, null);
		
		Map<String, Integer> colorNameMap = new HashMap<String, Integer>() {{
			put(context.getString(R.string.border_color_int), BORDER_COLOR);
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
		
		if (widgetSettingsCursor != null) {
			if (widgetSettingsCursor.moveToFirst()) {
				do {
					String key = widgetSettingsCursor.getString(AixWidgetSettings.KEY_COLUMN);
					String value = widgetSettingsCursor.getString(AixWidgetSettings.VALUE_COLUMN);
					
					if (colorNameMap.containsKey(key)) {
						mColors[colorNameMap.get(key)] = Integer.parseInt(value);
					} else if (key.equals(context.getString(R.string.temperature_units_string))) {
						mUseFahrenheit = value.equals("2");
					} else if (key.equals(context.getString(R.string.precipitation_units_string))) {
						mUseInches = value.equals("2");
					} else if (key.equals(context.getString(R.string.day_effect_bool))) {
						mDrawDayLightEffect = Boolean.parseBoolean(value);
					} else if (key.equals(context.getString(R.string.precipitation_scaling_string))) {
						try {
							mPrecipitationScale = Float.parseFloat(value);
						} catch (NumberFormatException e) { }
					} else if (key.equals(context.getString(R.string.top_text_visibility_string))) {
						try {
							mTopTextVisibility = Integer.parseInt(value);
						} catch (NumberFormatException e) { }
					} else if (key.equals(context.getString(R.string.border_enabled_bool))) {
						mDrawBorder = Boolean.parseBoolean(value);
					} else if (key.equals(context.getString(R.string.border_thickness_string))) {
						try {
							mBorderThickness = Float.parseFloat(value);
						} catch (NumberFormatException e) { }
					} else if (key.equals(context.getString(R.string.border_rounding_string))) {
						try {
							mBorderRounding = Float.parseFloat(value);
						} catch (NumberFormatException e) { }
					} else {
						//Log.d(TAG, "Unused property key=" + key + ", value=" + value);
					}
				} while (widgetSettingsCursor.moveToNext());
			}
			widgetSettingsCursor.close();
		}
		
		if (Float.isNaN(mPrecipitationScale)) {
			String precipitationDefault = mUseInches
					? context.getString(R.string.precipitation_scaling_inches_default)
					: context.getString(R.string.precipitation_scaling_mm_default);
			try {
				mPrecipitationScale = Float.parseFloat(precipitationDefault);
			} catch (NumberFormatException e) { }
		}
		if (mTopTextVisibility == 0) {
			try {
				mTopTextVisibility = Integer.parseInt(
						context.getString(R.string.top_text_visibility_default));
			} catch (NumberFormatException e) { }
		}
		if (Float.isNaN(mBorderThickness)) {
			try {
				mBorderThickness = Float.parseFloat(
						context.getString(R.string.border_thickness_default));
			} catch (NumberFormatException e) { }
		}
		if (Float.isNaN(mBorderRounding)) {
			try {
				mBorderRounding = Float.parseFloat(
						context.getString(R.string.border_rounding_default));
			} catch (NumberFormatException e) { }
		}
		
		if (!mDrawBorder) {
			mBorderThickness = 0.0f;
		}
	}
	
	private void setupIntervalData() {
		ArrayList<IntervalData> intervalData = new ArrayList<IntervalData>();
		
		Uri.Builder builder = mViewUri.buildUpon();
		builder.appendPath(AixViews.TWIG_INTERVALDATAFORECASTS);
		builder.appendQueryParameter("pu", mUseInches ? "i" : "m");

		Cursor cursor = mContext.getContentResolver().query(
				builder.build(), null,
				AixIntervalDataForecastColumns.TIME_TO + ">? AND " +
				AixIntervalDataForecastColumns.TIME_FROM + "<?",
				new String[] { Long.toString(mTimeFrom), Long.toString(mTimeTo) },
				'(' + AixIntervalDataForecastColumns.TIME_TO + '-' +
				AixIntervalDataForecastColumns.TIME_FROM + ") ASC," +
				AixIntervalDataForecastColumns.TIME_FROM + " ASC");
		if (cursor != null) {
			if (cursor.moveToFirst()) {
				do {
					try {
						IntervalData d = IntervalData.buildFromCursor(cursor);
						intervalData.add(d);
					} catch (Exception e) { Log.d(TAG, "setupIntervalData(): Adding IntervalData from cursor failed: " + e.getMessage()); }
				} while (cursor.moveToNext());
			}
			cursor.close();
		}
		
		mIntervalData = intervalData;
	}
	
	private void setupLocation() throws AixWidgetDrawException {
		String locationName = null;
		TimeZone locationTimeZone = null;
		
		Uri locationUri = Uri.withAppendedPath(mViewUri, AixViews.TWIG_LOCATION); 
		Cursor locationCursor = mContext.getContentResolver().query(locationUri, null, null, null, null);
		
		if (locationCursor != null) {
			if (locationCursor.moveToFirst()) {
				locationName = locationCursor.getString(AixLocationsColumns.TITLE_COLUMN);
				String timeZone = locationCursor.getString(AixLocations.TIME_ZONE_COLUMN);
				try {
					locationTimeZone = TimeZone.getTimeZone(timeZone);
				} catch (Exception e) {
					throw AixWidgetDrawException.buildInvalidTimeZoneException(timeZone);
				}
			}
			locationCursor.close();
		}
		
		if (locationTimeZone == null) {
			throw AixWidgetDrawException.buildInvalidTimeZoneException("null");
		}
		
		mLocationName = (locationName != null) ? locationName : "";
		mLocationTimeZone = locationTimeZone;
	}
	
	private void setupPaintDimensions(final float dp,
			final float textSize, final float labelTextSize)
	{
		mTextPaint.setTextSize(textSize);
		mLabelPaint.setTextSize(labelTextSize);
		mAboveFreezingTemperaturePaint.setStrokeWidth(2.0f * dp);
		mBelowFreezingTemperaturePaint.setStrokeWidth(2.0f * dp);
		mMaxRainPaint.setStrokeWidth(1.0f * dp);
	}
	
	private void setupPaints() {
		mBorderPaint = new Paint() {{
			setAntiAlias(true);
			setColor(mColors[BORDER_COLOR]);
			setStyle(Paint.Style.FILL);
		}};
		mBackgroundPaint = new Paint() {{
			setColor(mColors[BACKGROUND_COLOR]);
			setStyle(Paint.Style.FILL);
		}};
		final Bitmap bgPattern = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.pattern);
		mPatternPaint = new Paint() {{
			setShader(new BitmapShader(bgPattern, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT));
			setColorFilter(new PorterDuffColorFilter(mColors[PATTERN_COLOR], PorterDuff.Mode.SRC_IN));
		}};
		
		mTextPaint = new Paint() {{
			setAntiAlias(true);
			setColor(mColors[TEXT_COLOR]);
		}};
		mLabelPaint = new Paint() {{
			setAntiAlias(true);
			setColor(mColors[TEXT_COLOR]);
		}};
		
		mGridPaint = new Paint() {{
			setColor(mColors[GRID_COLOR]);
			setStyle(Paint.Style.STROKE);
		}};
		mGridOutlinePaint = new Paint() {{
			setColor(mColors[GRID_OUTLINE_COLOR]);
			setStyle(Paint.Style.STROKE);
		}};
		
		mAboveFreezingTemperaturePaint = new Paint() {{
			setAntiAlias(true);
			setColor(mColors[ABOVE_FREEZING_COLOR]);
			setStrokeCap(Paint.Cap.ROUND);
			setStyle(Paint.Style.STROKE);
		}};
		mBelowFreezingTemperaturePaint = new Paint() {{
			setAntiAlias(true);
			setColor(mColors[BELOW_FREEZING_COLOR]);
			setStrokeCap(Paint.Cap.ROUND);
			setStyle(Paint.Style.STROKE);
		}};
		
		mMinRainPaint = new Paint() {{
			setAntiAlias(false);
			setColor(mColors[MIN_RAIN_COLOR]);
			setStyle(Paint.Style.FILL);
		}};
		mMaxRainPaint = new Paint() {{
			setAntiAlias(true);
			setColor(mColors[MAX_RAIN_COLOR]);
			setStrokeCap(Paint.Cap.SQUARE);
			setStyle(Paint.Style.STROKE);
		}};
	}
	
	private void setupSunMoonData() {
		Calendar calendar = Calendar.getInstance(mUtcTimeZone);
		
		calendar.setTimeInMillis(mTimeFrom);
		truncateDay(calendar);
		calendar.add(Calendar.DAY_OF_YEAR, -1);
		long dateFrom = calendar.getTimeInMillis();
		
		calendar.setTimeInMillis(mTimeTo);
		truncateDay(calendar);
		calendar.add(Calendar.DAY_OF_YEAR, +1);
		long dateTo = calendar.getTimeInMillis();
		
		ArrayList<SunMoonData> sunMoonData = new ArrayList<SunMoonData>();
		Cursor c = mContext.getContentResolver().query(
				Uri.withAppendedPath(mViewUri, AixViews.TWIG_SUNMOONDATA),
				null,
				AixSunMoonDataColumns.DATE + ">=? AND " +
				AixSunMoonDataColumns.DATE + "<=?",
				new String[] { Long.toString(dateFrom), Long.toString(dateTo) },
				AixSunMoonDataColumns.DATE + " ASC");
		if (c != null) {
			if (c.moveToFirst()) {
				do {
					sunMoonData.add(SunMoonData.buildFromCursor(c));
				} while (c.moveToNext());
			}
			c.close();
		}
		
		mSunMoonData = sunMoonData;
	}
	
	private void setupTimesAndPointData() throws AixWidgetDrawException {
		// Set up time variables
		Calendar calendar = Calendar.getInstance(mUtcTimeZone);
		mTimeNow = calendar.getTimeInMillis();
		truncateHour(calendar);
		calendar.add(Calendar.HOUR_OF_DAY, 1);
		calendar.add(Calendar.HOUR_OF_DAY, -mNumWeatherDataBufferHours);
		mTimeFrom = calendar.getTimeInMillis();
		calendar.add(Calendar.HOUR_OF_DAY, mNumWeatherDataBufferHours * 2 + mNumHours);
		mTimeTo = calendar.getTimeInMillis();
		
		// Get temperature values
		ArrayList<PointData> pointData = new ArrayList<PointData>();
		
		Uri.Builder builder = mViewUri.buildUpon();
		builder.appendPath(AixViews.TWIG_POINTDATAFORECASTS);
		builder.appendQueryParameter("tu", mUseFahrenheit ? "f" : "c");

		Cursor cursor = mContext.getContentResolver().query(
				builder.build(),
				null,
				AixPointDataForecastColumns.TIME + ">=? AND " +
				AixPointDataForecastColumns.TIME + "<=?",
				new String[] { Long.toString(mTimeFrom), Long.toString(mTimeTo) },
				AixPointDataForecastColumns.TIME + " ASC");
		
		if (cursor != null) {
			if (cursor.moveToFirst()) {
				do {
					PointData p = PointData.buildFromCursor(cursor);
					pointData.add(p);
				} while (cursor.moveToNext());
			}
			cursor.close();
		}

		mPointData = pointData;
	}
	
	private void setupEpochAndTimes() {
		Calendar calendar = Calendar.getInstance(mUtcTimeZone);
		truncateHour(calendar);
		calendar.add(Calendar.HOUR_OF_DAY, 1);
		
		long iconEpoch = 0;
		long compare = Long.MAX_VALUE;
		
		for (IntervalData inter : mIntervalData) {
			if (inter.weatherIcon != 0) {
				if ((inter.timeTo != inter.timeFrom && inter.timeTo > calendar.getTimeInMillis()) ||
						(inter.timeFrom == inter.timeTo && inter.timeTo >= calendar.getTimeInMillis())) {
					long diff = inter.timeFrom - calendar.getTimeInMillis();//Math.abs(
					if (diff < compare) {
						compare = diff;
						iconEpoch = inter.timeFrom;
					}
				}
			}
		}
		
		long pointEpoch = 0;
		compare = Long.MAX_VALUE;
		
		if (pointEpoch == 0) {
			for (PointData p : mPointData) {
				if (p.mTime >= calendar.getTimeInMillis()) {
					long diff = p.mTime - calendar.getTimeInMillis();
					if (diff < compare) {
						compare = diff;
						pointEpoch = p.mTime;
					}
				}
			}
		}
		
		long epoch = Math.max(iconEpoch, pointEpoch);
		
		if (epoch == 0) {
			epoch = calendar.getTimeInMillis();
		}
		
		// Update timeFrom and timeTo to correct values given the epoch
		calendar.setTimeInMillis(epoch);
		//calendar.setTimeInMillis(timeTemp);
		mTimeFrom = calendar.getTimeInMillis();
		calendar.add(Calendar.HOUR_OF_DAY, mNumHours);
		mTimeTo = calendar.getTimeInMillis();
	}
	
	private void setupSampleTimes() throws AixWidgetDrawException {
		long lastPointPos = -1;
		long lastIntervalPos = -1;
		
		long sampleResolutionHrs = Long.MAX_VALUE;
		
		for (IntervalData inter : mIntervalData) {
			if (inter.weatherIcon != 0) {
				if (inter.timeFrom == inter.timeTo) {
					if (lastPointPos != -1) {
						sampleResolutionHrs = Math.min(sampleResolutionHrs, Math.abs(inter.timeFrom - lastPointPos));
					}
					lastPointPos = inter.timeFrom;
				} else {
					long intervalPos = (inter.timeFrom + inter.timeTo) / 2;
					if (lastIntervalPos != -1) {
						sampleResolutionHrs = Math.min(sampleResolutionHrs, Math.abs(intervalPos - lastIntervalPos));
					}
					lastIntervalPos = intervalPos;
				}
			}
		}
		
		sampleResolutionHrs = Math.round(sampleResolutionHrs / DateUtils.HOUR_IN_MILLIS);
		
		if (sampleResolutionHrs > 6) {
			lastPointPos = -1;
			for (PointData p : mPointData) {
				if (lastPointPos != -1) {
					sampleResolutionHrs = Math.min(sampleResolutionHrs, Math.round((p.mTime - lastPointPos) / DateUtils.HOUR_IN_MILLIS));
				}
				lastPointPos = p.mTime;
			}
		}
		
		if ((sampleResolutionHrs < 1) || (sampleResolutionHrs > mNumHours / 2)) {
			throw AixWidgetDrawException.buildMissingWeatherDataException();
		}
		
		mNumHoursBetweenSamples = (int)sampleResolutionHrs;
	}
	
	private void setupWidgetSize() throws AixWidgetDrawException {
		// Get widget size
		int widgetSize = 0;
		Cursor widgetCursor = mContext.getContentResolver().query(mWidgetUri, null, null, null, null);
		if (widgetCursor != null) {
			if (widgetCursor.moveToFirst()) {
				widgetSize = widgetCursor.getInt(AixWidgetsColumns.SIZE_COLUMN);
			}
			widgetCursor.close();
		}
		
		if (widgetSize == AixWidgetsColumns.SIZE_INVALID) {
			throw AixWidgetDrawException.buildInvalidWidgetSizeException();
		}
		
		mWidgetColumns = (widgetSize - 1) / 4 + 1;
		mWidgetRows = (widgetSize - 1) % 4 + 1;
	}
	
	private void validatePointData() throws AixWidgetDrawException {
		float maxTemperature = Float.NEGATIVE_INFINITY;
		float minTemperature = Float.POSITIVE_INFINITY;
		
		int validPointDataSamples = 0;
		// Find maximum, minimum and range of temperatures
		for (PointData p : mPointData) {
			// Check if temperature is within viewable range
			if (p.mTime >= mTimeFrom && p.mTime <= mTimeTo)
			{
				if (p.mTemperature > maxTemperature) maxTemperature = p.mTemperature;
				if (p.mTemperature < minTemperature) minTemperature = p.mTemperature;
				validPointDataSamples++;
			}
		}
		
		if (Float.isInfinite(mMaxTemperature) || Float.isInfinite(minTemperature)) {
			throw AixWidgetDrawException.buildMissingWeatherDataException();
		}
		
		// Ensure that there are enough point data samples within the time period
		if (validPointDataSamples < 2) {
			throw AixWidgetDrawException.buildMissingWeatherDataException();
		}
		
		mMaxTemperature = maxTemperature;
		mMinTemperature = minTemperature;
	}
	
	private static class CatmullRom {

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
		
		private static Path buildPath(ArrayList<PointData> pointData, long startTime, long endTime,
				float tempRangeMax, float tempRangeMin, Rect graphRect)
		{
			if (pointData.size() <= 0) return null;
			Path path = new Path();
			float tempRange = tempRangeMax - tempRangeMin;
			long timeRange = endTime - startTime;
			PointF[] points = new PointF[pointData.size()];
			
			for (int i = 0; i < pointData.size(); i++) {
				PointData p = pointData.get(i);
				points[i] = new PointF((float)(p.mTime - startTime) / (float)timeRange,
						(float)(1.0f - (p.mTemperature - tempRangeMin) / tempRange));
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
	
}
