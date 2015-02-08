package net.veierland.aix.widget;

import static net.veierland.aix.AixUtils.ABOVE_FREEZING_COLOR;
import static net.veierland.aix.AixUtils.BACKGROUND_COLOR;
import static net.veierland.aix.AixUtils.BELOW_FREEZING_COLOR;
import static net.veierland.aix.AixUtils.BORDER_COLOR;
import static net.veierland.aix.AixUtils.DAY_COLOR;
import static net.veierland.aix.AixUtils.GRID_COLOR;
import static net.veierland.aix.AixUtils.GRID_OUTLINE_COLOR;
import static net.veierland.aix.AixUtils.MAX_RAIN_COLOR;
import static net.veierland.aix.AixUtils.MIN_RAIN_COLOR;
import static net.veierland.aix.AixUtils.NIGHT_COLOR;
import static net.veierland.aix.AixUtils.PATTERN_COLOR;
import static net.veierland.aix.AixUtils.TEXT_COLOR;
import static net.veierland.aix.AixUtils.TOP_TEXT_ALWAYS;
import static net.veierland.aix.AixUtils.TOP_TEXT_LANDSCAPE;
import static net.veierland.aix.AixUtils.TOP_TEXT_PORTRAIT;
import static net.veierland.aix.AixUtils.WEATHER_ICONS_DAY;
import static net.veierland.aix.AixUtils.WEATHER_ICONS_NIGHT;
import static net.veierland.aix.AixUtils.WEATHER_ICONS_POLAR;
import static net.veierland.aix.AixUtils.hcap;
import static net.veierland.aix.AixUtils.isPrime;
import static net.veierland.aix.AixUtils.lcap;
import static net.veierland.aix.AixUtils.truncateDay;
import static net.veierland.aix.AixUtils.truncateHour;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
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
import net.veierland.aix.AixProvider.AixWidgetSettings;
import net.veierland.aix.AixProvider.AixWidgets;
import net.veierland.aix.IntervalData;
import net.veierland.aix.PointData;
import net.veierland.aix.R;
import net.veierland.aix.SunMoonData;
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
	
	/* Initial Properties */
	
	private final int mNumHours;
	private final int mNumWeatherDataBufferHours;
	
	private final Context mContext;
	
	private final Uri mLocationUri;
	private final Uri mWidgetUri;
	
	/* Settings */
	
	private boolean mDrawBorder = true;
	private boolean mDrawDayLightEffect = true;
	private boolean mUseFahrenheit = false;
	private boolean mUseInches = false;
	
	private int mTopTextVisibility = 0;
	private int[] mColors;
	
	private float mBorderRounding = Float.NaN;
	private float mBorderThickness = Float.NaN;
	private float mPrecipitationScale = Float.NaN;
	
	/* Common Properties */
	
	private float mDP;
	
	private float mIconHeight;
	private float mIconSpacingY;
	private float mIconWidth;
	private float mLabelTextSize;
	private float mTextSize;
	
	private int mNumHoursBetweenSamples;
	private int mWidgetColumns;
	private int mWidgetHeight;
	private int mWidgetRows;
	private int mWidgetWidth;
	
	private long mTimeFrom;
	private long mTimeNow;
	private long mTimeTo;	
	
	private ArrayList<IntervalData> mIntervalData;
	private ArrayList<PointData> mPointData;
	private ArrayList<SunMoonData> mSunMoonData;
	
	private ContentResolver mResolver;
	
	private Paint mAboveFreezingTemperaturePaint;
	private Paint mBackgroundPaint;
	private Paint mBelowFreezingTemperaturePaint;
	private Paint mBorderPaint;
	private Paint mGridOutlinePaint;
	private Paint mGridPaint;
	private Paint mLabelPaint;
	private Paint mPatternPaint;
	private Paint mTextPaint;
	
	private Paint mMinRainPaint, mMaxRainPaint;
	
	private String mLocationName;
	
	private TimeZone mLocationTimeZone;
	private TimeZone mUtcTimeZone = TimeZone.getTimeZone("UTC");
	
	/* Render Properties */

	private float mTemperatureValueMax, mTemperatureValueMin;
	private float mTemperatureRangeMax, mTemperatureRangeMin;
	
	private int mNumHorizontalCells, mNumVerticalCells;
	private float mCellSizeX, mCellSizeY;
	
	private String[] mTemperatureLabels;

	private Rect mGraphRect = new Rect();
	private Rect mWidgetBounds = new Rect();
	
	private RectF mBackgroundRect = new RectF();
	private RectF mBorderRect = new RectF();
	
	private AixDetailedWidget(final Context context, final Uri widgetUri, final Uri locationUri, final int widgetSize) {
		mContext = context;
		mWidgetUri = widgetUri;
		mLocationUri = locationUri;
		
		mNumHours = 24;
		mNumWeatherDataBufferHours = 6;
		
		mWidgetColumns = (widgetSize - 1) / 4 + 1;
		mWidgetRows = (widgetSize - 1) % 4 + 1;
	}
	
	public static AixDetailedWidget build(final Context context, final Uri widgetUri, final Uri locationUri, final int widgetSize) throws AixWidgetDrawException {
		AixDetailedWidget widget = new AixDetailedWidget(context, widgetUri, locationUri, widgetSize);
		return widget.initialize();
	}
	
	private AixDetailedWidget initialize() throws AixWidgetDrawException {
		mResolver = mContext.getContentResolver();
		
		setupLocation();
		loadConfiguration();
		setupTimesAndPointData();
		setupIntervalData();
		setupSampleTimes();
		setupEpochAndTimes();
		validatePointData();
		setupSunMoonData();
		setupPaints();
		
		DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
		mDP = dm.density;
		
		mTextSize = 10.0f * mDP;
		mLabelTextSize = 9.0f * mDP;
		mIconHeight = 19.0f * mDP;
		mIconWidth = 19.0f * mDP;
		mIconSpacingY = 2.0f * mDP;
		
		setupPaintDimensions();
		
		return this;
	}
	
	public Bitmap render(boolean isLandscape) throws AixWidgetDrawException {
		setupWidgetDimensions(isLandscape);
		Bitmap bitmap = Bitmap.createBitmap(mWidgetWidth, mWidgetHeight, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		
		float borderPadding = 1.0f;
		mBorderRect.set(
				Math.round(mWidgetBounds.left + borderPadding),
				Math.round(mWidgetBounds.top + borderPadding),
				Math.round(mWidgetBounds.right - borderPadding),
				Math.round(mWidgetBounds.bottom - borderPadding));
		
		float widgetBorder = Math.round(borderPadding + mBorderRounding + (mBorderThickness - mBorderRounding));
		mBackgroundRect.set(
				Math.round(mWidgetBounds.left + widgetBorder),
				Math.round(mWidgetBounds.top + widgetBorder),
				Math.round(mWidgetBounds.right - widgetBorder),
				Math.round(mWidgetBounds.bottom - widgetBorder));
		
		boolean drawTopText = (mTopTextVisibility == TOP_TEXT_ALWAYS ||
				(isLandscape && mTopTextVisibility == TOP_TEXT_LANDSCAPE) ||
				(!isLandscape && mTopTextVisibility == TOP_TEXT_PORTRAIT));
		
		calculateDimensions(isLandscape, drawTopText);
		
		drawBackground(canvas);
		drawGrid(canvas);

		if (mDrawDayLightEffect) {
			drawDayAndNight(canvas);
		}
		
		Path minRainPath = new Path();
		Path maxRainPath = new Path();
		buildRainPaths(minRainPath, maxRainPath);
		drawRainPaths(canvas, minRainPath, maxRainPath);
		
		Path temperaturePath = CatmullRom.buildPath(mPointData, mTimeFrom, mTimeTo, mTemperatureRangeMax, mTemperatureRangeMin);
		if (temperaturePath != null) {
			Matrix scaleMatrix = new Matrix();
			scaleMatrix.setScale(mGraphRect.width(), mGraphRect.height());
			temperaturePath.transform(scaleMatrix);
			temperaturePath.offset(mGraphRect.left, mGraphRect.top);
			drawTemperature(canvas, temperaturePath);
		}
		
		drawGridOutline(canvas);
		drawTemperatureLabels(canvas);
		drawHourLabels(canvas);
		drawWeatherIcons(canvas);
		
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
			
			drawInfoText(canvas, pressure, humidity, temperature);
		}
		
		return bitmap;
	}

	private void buildRainPaths(Path minRainPath, Path maxRainPath) {
		float[] rainValues = new float[mNumHorizontalCells];
		float[] rainMinValues = new float[mNumHorizontalCells];
		float[] rainMaxValues = new float[mNumHorizontalCells];
		
		Arrays.fill(rainValues, Float.NaN);
		Arrays.fill(rainMinValues, Float.NaN);
		Arrays.fill(rainMaxValues, Float.NaN);
		
		int[] pointers = new int[mNumHorizontalCells];
		int[] precision = new int[mNumHorizontalCells];
		
		Arrays.fill(pointers, -1);
		
		for (int i = 0; i < mIntervalData.size(); i++) {
			IntervalData d = mIntervalData.get(i);
			if (d.timeFrom == d.timeTo) continue;
			
			int startCell = (int)Math.floor((float)mNumHorizontalCells *
					(float)(d.timeFrom - mTimeFrom) / (float)(mTimeTo - mTimeFrom));
			
			if (startCell >= mNumHorizontalCells) continue;
			
			float endCellPos = (float)mNumHorizontalCells *
					(float)(d.timeTo - mTimeFrom) / (float)(mTimeTo - mTimeFrom);
			int endCell = (endCellPos == Math.round(endCellPos))
					? (int)endCellPos - 1 : (int)Math.ceil(endCellPos);
			
			startCell = lcap(startCell, 0);
			endCell = hcap(endCell, mNumHorizontalCells - 1);
			
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
		
		RectF rainRect = new RectF();
		
		int i = 0;
		while (i < mNumHorizontalCells) {
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
			
			minVal = hcap(minVal / mPrecipitationScale, mNumVerticalCells);
			
			int endCell = i + 1;
			while (	(endCell < mNumHorizontalCells) &&
					(pointers[i] == pointers[endCell]) &&
					(pointers[endCell] != -2))
			{
				endCell++;
			}
			
			rainRect.set(
					mGraphRect.left + Math.round(i * mCellSizeX) + 1.0f,
					mGraphRect.bottom - minVal * mCellSizeY,
					mGraphRect.left + Math.round(endCell * mCellSizeX),
					mGraphRect.bottom);
			minRainPath.addRect(rainRect, Path.Direction.CCW);
			
			if (!Float.isNaN(maxVal) && maxVal > minVal && minVal < mNumVerticalCells) {
				maxVal = hcap(maxVal / mPrecipitationScale, mNumVerticalCells);
				rainRect.bottom = rainRect.top;
				rainRect.top = mGraphRect.bottom - maxVal * mCellSizeY;
				maxRainPath.addRect(rainRect, Path.Direction.CCW);
			}
			
			i = endCell;
		}
	}
	
	private void calculateDimensions(final boolean isLandscape, final boolean drawTopText) {
		float[] degreesPerCellOptions = { 0.5f, 1.0f, 2.0f, 2.5f, 5.0f, 10.0f,
				20.0f, 25.0f, 50.0f, 100.0f };
		int dPcIndex = 0;

		float textLabelWidth = 0.0f;
		
		mNumHorizontalCells = mNumHours;
		mNumVerticalCells = 2;
		
		float degreesPerCell;
		
		while (true) {
			mGraphRect.left = (int)Math.round(mBackgroundRect.left + textLabelWidth + 5.0f * mDP);
			if (drawTopText) {
				mGraphRect.top = (int)Math.round(mBackgroundRect.top + mLabelTextSize + 4.0f * mDP);
			} else {
				mGraphRect.top = (int)Math.round(mBackgroundRect.top + mLabelTextSize / 2.0f + 2.0f * mDP);
			}
			mGraphRect.right = (int)Math.round(mBackgroundRect.right - 5.0f * mDP);
			mGraphRect.bottom = (int)Math.round(mBackgroundRect.bottom - mLabelTextSize - 6.0f * mDP);
			
			for (int j = 2; j <= 100; j++) {
				if (mNumHours % j == 0) {
					if ((float)mGraphRect.width() / (float)j >= 8.0f * mDP) {
						mNumHorizontalCells = j;
					} else {
						break;
					}
				}
			}
			
			mCellSizeX = (float)(mGraphRect.right - mGraphRect.left) / (float) mNumHorizontalCells;
			degreesPerCell = degreesPerCellOptions[dPcIndex];
			
			int numCellsReq = (int) Math.ceil(mTemperatureValueMax / degreesPerCell) - (int) Math.floor(mTemperatureValueMin / degreesPerCell);
			float tCellSizeY = ((float)mGraphRect.height() - mIconHeight - mIconSpacingY) / ((mTemperatureValueMax / degreesPerCell) - (float)Math.floor(mTemperatureValueMin / degreesPerCell));
			mNumVerticalCells = Math.max((int)Math.ceil((float)mGraphRect.height() / tCellSizeY), 1);
			
			mCellSizeY = (mGraphRect.bottom - mGraphRect.top) / (float) mNumVerticalCells;
			
			if (2.0f * mDP > mCellSizeY * ((mTemperatureValueMin / degreesPerCell - Math.floor(mTemperatureValueMin / degreesPerCell)))) {
				mNumVerticalCells++;
			}
			
			if (((float)mGraphRect.height() / (float) mNumVerticalCells) < mLabelPaint.getTextSize()) {
				while (isPrime(mNumVerticalCells)) mNumVerticalCells++;
			}
			
			mCellSizeY = (float)mGraphRect.height() / (float) mNumVerticalCells;
			
			if (mCellSizeY < 8.0f * mDP) {
				dPcIndex++;
				continue;
			}
			
			// Center range as far as possible (need to fix proper centering)
			int startCell = mNumVerticalCells - numCellsReq;
			while ((startCell > 0) && (mIconHeight > mCellSizeY// + iconSpacingY > cellSizeY
					* (mNumVerticalCells - numCellsReq - startCell + (Math
					.ceil(mTemperatureValueMax / degreesPerCell) - mTemperatureValueMax / degreesPerCell))))
			{
				startCell--;
			}
			
			mTemperatureRangeMin = degreesPerCell * (float)(Math.floor(mTemperatureValueMin / degreesPerCell) - startCell);
			mTemperatureRangeMax = mTemperatureRangeMin + degreesPerCell * mNumVerticalCells;
			
			mTemperatureLabels = new String[mNumVerticalCells + 1];

			float newTextLabelWidth = 0.0f;
			
			boolean isAllBelow10 = true;
			
			for (int j = 0; j <= mNumVerticalCells; j++) {
				float num = mTemperatureRangeMin + degreesPerCell * j;
				if (Math.abs(num) >= 10.0f) isAllBelow10 = false;
				String formatting = Math.round(degreesPerCell) != degreesPerCell
						? "%.1f\u00B0" : "%.0f\u00B0";
				mTemperatureLabels[j] = String.format(formatting, num);
				newTextLabelWidth = Math.max(newTextLabelWidth,
						mLabelPaint.measureText(mTemperatureLabels[j]));
			}
			
			if (isAllBelow10) newTextLabelWidth += mDP;

			if (newTextLabelWidth > textLabelWidth) {
				textLabelWidth = newTextLabelWidth;
			} else {
				break;
			}
		}
	}
	
	private void drawBackground(Canvas canvas)
	{
		if (mBorderThickness > 0.0f) {
			canvas.save();
			canvas.clipRect(mBackgroundRect, Region.Op.DIFFERENCE);
			canvas.drawRoundRect(mBorderRect, mBorderRounding, mBorderRounding, mBorderPaint);
			canvas.restore();
			
			canvas.drawRect(mBackgroundRect, mBackgroundPaint);
			canvas.drawRect(mBackgroundRect, mPatternPaint);
		} else {
			canvas.drawRoundRect(mBackgroundRect, mBorderRounding, mBorderRounding, mBackgroundPaint);
			canvas.drawRoundRect(mBackgroundRect, mBorderRounding, mBorderRounding, mPatternPaint);
		}
	}
	
	private void drawDayAndNight(Canvas canvas)
	{
		if (mSunMoonData == null) return;
		
		float timeRange = mTimeTo - mTimeFrom;
		float transitionWidthDefault = (float)DateUtils.HOUR_IN_MILLIS / timeRange;
		
		canvas.save();
		canvas.clipRect(mGraphRect.left + 1, mGraphRect.top, mGraphRect.right, mGraphRect.bottom);
		
		Matrix matrix = new Matrix();
		matrix.setScale(mGraphRect.width(), mGraphRect.height());
		matrix.postTranslate(mGraphRect.left + 1, mGraphRect.top);
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
	
	private void drawGrid(Canvas canvas) {
		Path gridPath = new Path();
		
		for (int i = 1; i <= mNumHorizontalCells; i++) {
			float xPos = mGraphRect.left + Math.round(i * mCellSizeX);
			gridPath.moveTo(xPos, mGraphRect.bottom);
			gridPath.lineTo(xPos, mGraphRect.top);
		}

		for (int i = 1; i <= mNumVerticalCells; i++) {
			float yPos = mGraphRect.bottom - Math.round(i * mCellSizeY);
			gridPath.moveTo(mGraphRect.left, yPos);
			gridPath.lineTo(mGraphRect.right, yPos);
		}
		
		int rightOffset = mDrawDayLightEffect ? 0 : 1;
		
		canvas.save();
		canvas.clipRect(
				mGraphRect.left + 1,
				mGraphRect.top,
				mGraphRect.right + rightOffset,
				mGraphRect.bottom,
				Region.Op.REPLACE);
		canvas.drawPath(gridPath, mGridPaint);
		canvas.restore();
	}
	
	private void drawGridOutline(Canvas canvas) {
		Path gridOutline = new Path();
		gridOutline.moveTo(mGraphRect.left, mGraphRect.top);
		gridOutline.lineTo(mGraphRect.left, mGraphRect.bottom);
		gridOutline.lineTo(mGraphRect.right + 1, mGraphRect.bottom);
		
		if (mDrawDayLightEffect) {
			gridOutline.moveTo(mGraphRect.right, mGraphRect.top);
			gridOutline.lineTo(mGraphRect.right, mGraphRect.bottom);
		}
		
		canvas.drawPath(gridOutline, mGridOutlinePaint);
	}
	
	private void drawHourLabels(Canvas canvas) {
		// Draw time stamp labels and horizontal notches
		float notchHeight = 3.5f * mDP;
		float labelTextPaddingY = 1.5f * mDP;
		
		mLabelPaint.setTextAlign(Paint.Align.CENTER);
		
		Calendar calendar = Calendar.getInstance(mLocationTimeZone);
		calendar.setTimeInMillis(mTimeFrom);
		
		int startHour = calendar.get(Calendar.HOUR_OF_DAY);

		float hoursPerCell = (float)mNumHours / (float)mNumHorizontalCells;
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
			while ((mNumHorizontalCells % numCellsBetweenHorizontalLabels != 0) ||
					((numCellsBetweenHorizontalLabels * hoursPerCell) !=
							Math.round(numCellsBetweenHorizontalLabels * hoursPerCell)))
			{
				numCellsBetweenHorizontalLabels++;
			}
			
			float spaceBetweenLabels = numCellsBetweenHorizontalLabels * mCellSizeX;
			if (spaceBetweenLabels > longLabelWidth * 1.25f) {
				break;
			} else if (!use24hours && spaceBetweenLabels > shortLabelWidth * 1.25f) {
				useShortLabel = true;
				break;
			}
			
			numCellsBetweenHorizontalLabels++;
		}
		
		for (int i = numCellsBetweenHorizontalLabels;
				 i < mNumHorizontalCells;
				 i+= numCellsBetweenHorizontalLabels)
		{
			float notchX = mGraphRect.left + Math.round(i * mCellSizeX);
			canvas.drawLine(notchX, (float)mGraphRect.bottom - notchHeight / 2.0f,
					notchX, (float)mGraphRect.bottom + notchHeight / 2.0f, mGridOutlinePaint);
			
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
			canvas.drawText(hourLabel, notchX, (float)Math.floor(((float)mGraphRect.bottom + mBackgroundRect.bottom) / 2.0f + mLabelPaint.getTextSize() / 2.0f), mLabelPaint);
		}
	}

	private void drawInfoText(Canvas canvas, float pressure, float humidity, float temperature)
	{
		final float locationLabelTextSize = 10.0f * mDP;
		
		float topTextSidePadding = 1.0f * mDP;
		float topTextBottomPadding = 3.0f * mDP;
		
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
		
		float topTextSpace = mGraphRect.width() - topTextSidePadding * 2.0f;
		
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
				mGraphRect.left + topTextSidePadding,
				mBackgroundRect.top + mTextPaint.getTextSize(),
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
				mGraphRect.right - topTextSidePadding,
				mBackgroundRect.top + mTextPaint.getTextSize(),
				mTextPaint);
	}
	
	private void drawRainPaths(Canvas canvas, Path minRainPath, Path maxRainPath)
	{
		canvas.drawPath(minRainPath, mMinRainPaint);
		canvas.save();
		canvas.clipPath(maxRainPath);
		
		// TODO Technique works, but too expensive?
		float dimensions = (float)Math.sin(Math.toRadians(45)) *
						   (mGraphRect.height() + mGraphRect.width());
		float dimensions2 = dimensions / 2.0f;
		float ggx = mGraphRect.left + mGraphRect.width() / 2.0f;
		float ggy = mGraphRect.top + mGraphRect.height() / 2.0f;
	
		Matrix transform = new Matrix();
		transform.setRotate(-45.0f, ggx, ggy);
		canvas.setMatrix(transform);
		
		float ypos = mGraphRect.top - (dimensions - mGraphRect.height()) / 2.0f;
		float ytest = mGraphRect.bottom + (dimensions - mGraphRect.height()) / 2.0f;
		while (ypos < ytest) {
			canvas.drawLine(ggx - dimensions2, ypos, ggx + dimensions2, ypos, mMaxRainPaint);
			ypos += 2.0f * mDP;
		}
		canvas.restore();
	}
	
	private void drawTemperature(Canvas canvas, Path temperaturePath)
	{
		float freezingTemperature = mUseFahrenheit ? 32.0f : 0.0f;
		
		Rect graphRectInner = new Rect(mGraphRect.left + 1, mGraphRect.top + 1, mGraphRect.right, mGraphRect.bottom);
		
		canvas.save();
		if (mTemperatureRangeMin >= freezingTemperature) {
			// All positive
			canvas.clipRect(graphRectInner);
			canvas.drawPath(temperaturePath, mAboveFreezingTemperaturePaint);
		} else if (mTemperatureRangeMax <= freezingTemperature) {
			// All negative
			canvas.clipRect(graphRectInner);
			canvas.drawPath(temperaturePath, mBelowFreezingTemperaturePaint);
		} else {
			float freezingPosY = (float)Math.floor(mGraphRect.height() *
					(freezingTemperature - mTemperatureRangeMin) / (mTemperatureRangeMax - mTemperatureRangeMin));
			
			canvas.clipRect(graphRectInner.left, graphRectInner.top,
					graphRectInner.right, graphRectInner.bottom - freezingPosY);
			canvas.drawPath(temperaturePath, mAboveFreezingTemperaturePaint);
			
			canvas.clipRect(graphRectInner.left, graphRectInner.bottom - freezingPosY,
					graphRectInner.right, graphRectInner.bottom, Op.REPLACE);
			canvas.drawPath(temperaturePath, mBelowFreezingTemperaturePaint);
		}
		canvas.restore();
	}
	
	private void drawTemperatureLabels(Canvas canvas) {
		float labelTextPaddingX = 2.5f * mDP;
		float notchWidth = 3.5f * mDP;
		
		// Find the minimum number of cells between each label
		int numCellsBetweenVerticalLabels = 1;
		while (numCellsBetweenVerticalLabels * mCellSizeY < mLabelPaint.getTextSize()) {
			numCellsBetweenVerticalLabels++;
		}
		// Ensure that labels can be evenly spaced, given the number of cells
		while (mNumVerticalCells % numCellsBetweenVerticalLabels != 0) {
			numCellsBetweenVerticalLabels++;
		}
		
		if (mTemperatureLabels.length < mNumVerticalCells) return;
		
		mLabelPaint.setTextAlign(Paint.Align.RIGHT);
		
		Rect bounds = new Rect();
		
		for (int i = 0; i <= mNumVerticalCells; i += numCellsBetweenVerticalLabels) {
			float notchY = mGraphRect.bottom - Math.round(i * mCellSizeY);
			canvas.drawLine(
					mGraphRect.left - notchWidth / 2, notchY,
					mGraphRect.left + notchWidth / 2, notchY,
					mGridOutlinePaint);
			
			mLabelPaint.getTextBounds(mTemperatureLabels[i], 0, mTemperatureLabels[i].length(), bounds);
			
			canvas.drawText(
					mTemperatureLabels[i],
					mGraphRect.left - labelTextPaddingX,
					mGraphRect.bottom - bounds.centerY()
							- (float)(i * mGraphRect.height()) / (float)mNumVerticalCells,
					mLabelPaint);
		}
	}
	
	private void drawWeatherIcons(Canvas canvas)
	{
		// Calculate number of cells per icon
		float hoursPerCell = (float)mNumHours / (float)mNumHorizontalCells;
		int numCellsPerIcon = (int)Math.ceil((float)mNumHoursBetweenSamples / hoursPerCell);
		
		while (	(numCellsPerIcon * mCellSizeX < mIconWidth) ||
				((float)mNumHours % (numCellsPerIcon * hoursPerCell) != 0.0f))
		{
			numCellsPerIcon++;
		}
		
		int hoursPerIcon = (int)(numCellsPerIcon * hoursPerCell);
		
		float iconWidthValOver4 = mIconWidth * (mTimeTo - mTimeFrom) / mGraphRect.width() / 4.0f;
		
		long loMarker = mTimeFrom + hoursPerIcon * DateUtils.HOUR_IN_MILLIS / 2;
		long hiMarker = mTimeTo - hoursPerIcon * DateUtils.HOUR_IN_MILLIS / 2;
		
		float tempRange = mTemperatureRangeMax - mTemperatureRangeMin;
		
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
				
				int iconX = Math.round(mGraphRect.left - mIconWidth / 2.0f +
						Math.round(((float)(iconTimePos - mTimeFrom) /
								((float)DateUtils.HOUR_IN_MILLIS * hoursPerCell)) * mCellSizeX));
				int iconY = Math.round((float)mGraphRect.bottom - mIconHeight - mIconSpacingY -
						(float)mGraphRect.height() * (val - mTemperatureRangeMin) / tempRange);
				
				iconY = lcap(iconY, mGraphRect.top);
				iconY = hcap(iconY, mGraphRect.bottom - (int)Math.ceil(mIconHeight));
				
				Rect dest = new Rect(iconX, iconY,
						Math.round(iconX + mIconWidth), Math.round(iconY + mIconHeight));
				
				calendar.setTimeInMillis(iconTimePos);
				truncateDay(calendar);
				long iconDate = calendar.getTimeInMillis();
				
				int[] weatherIcons = WEATHER_ICONS_NIGHT;
				
				for (SunMoonData smd : mSunMoonData) {
					if (smd.mDate == iconDate) {
						if (smd.mSunRise == AixSunMoonData.NEVER_RISE) {
							weatherIcons = WEATHER_ICONS_POLAR;
						} else if (smd.mSunSet == AixSunMoonData.NEVER_SET) {
							weatherIcons = WEATHER_ICONS_DAY;
						}
					}
					if (smd.mSunRise < iconTimePos && smd.mSunSet > iconTimePos) {
						weatherIcons = WEATHER_ICONS_DAY;
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
		Cursor widgetSettingsCursor = mResolver.query(
				Uri.withAppendedPath(mWidgetUri, AixWidgets.TWIG_SETTINGS),
				null, null, null, null);
		
		@SuppressWarnings("serial")
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
		
		Uri.Builder builder = mLocationUri.buildUpon();
		builder.appendPath(AixLocations.TWIG_INTERVALDATAFORECASTS);
		builder.appendQueryParameter("pu", mUseInches ? "i" : "m");

		Cursor cursor = mResolver.query(
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
		 
		Cursor locationCursor = mResolver.query(mLocationUri, null, null, null, null);
		
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
	
	private void setupPaintDimensions()
	{
		mTextPaint.setTextSize(mTextSize);
		mLabelPaint.setTextSize(mLabelTextSize);
		mAboveFreezingTemperaturePaint.setStrokeWidth(2.0f * mDP);
		mBelowFreezingTemperaturePaint.setStrokeWidth(2.0f * mDP);
		mMaxRainPaint.setStrokeWidth(1.0f * mDP);
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
		Cursor c = mResolver.query(
				Uri.withAppendedPath(mLocationUri, AixLocations.TWIG_SUNMOONDATA),
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
	
	private void setupEpochAndTimes() {
		Calendar calendar = Calendar.getInstance(mUtcTimeZone);
		calendar.setTimeInMillis(mTimeNow);
		truncateHour(calendar);
		calendar.add(Calendar.HOUR_OF_DAY, 1);
		
		long nextHour = calendar.getTimeInMillis();
		
		long firstIntervalSampleAfter = Long.MAX_VALUE;
		
		for (IntervalData inter : mIntervalData) {
			if (inter.weatherIcon == 0) continue;
			if (inter.timeFrom > nextHour) {
				firstIntervalSampleAfter = Math.min(firstIntervalSampleAfter, inter.timeFrom);
			}
		}
		
		long epoch = Math.min(nextHour, firstIntervalSampleAfter - mNumHoursBetweenSamples * DateUtils.HOUR_IN_MILLIS);
		
		long firstPointSample = Long.MAX_VALUE;
		
		for (PointData p : mPointData) {
			firstPointSample = Math.min(firstPointSample, p.mTime);
		}
		
		if (firstPointSample != Long.MAX_VALUE) {
			epoch = Math.max(epoch, firstPointSample);
		}
				
		// Update timeFrom and timeTo to correct values given the epoch
		calendar.setTimeInMillis(epoch);
		//calendar.setTimeInMillis(timeTemp);
		mTimeFrom = calendar.getTimeInMillis();
		calendar.add(Calendar.HOUR_OF_DAY, mNumHours);
		mTimeTo = calendar.getTimeInMillis();
		
		SimpleDateFormat sdf = new SimpleDateFormat();
		sdf.setTimeZone(mLocationTimeZone);
		
		Log.d(TAG, mLocationName + " (" + mLocationTimeZone.getDisplayName() + "): " + sdf.format(new Date(mTimeNow)) +
				" nextHour=" + sdf.format(new Date(nextHour)) +
				" -> firstIntervalSampleAfter=" + sdf.format(new Date(firstIntervalSampleAfter)) +
				",firstPointSample=" + sdf.format(new Date(firstPointSample)) +
				",numHoursBetweenSamples=" + mNumHoursBetweenSamples +
				" -> timeFrom=" + sdf.format(new Date(mTimeFrom)) +
				",timeTo=" + sdf.format(new Date(mTimeTo))); 
	}
	
	private void setupSampleTimes() throws AixWidgetDrawException {
		long sampleResolutionHrs = Long.MAX_VALUE;
		
		long lastIntervalPos = -1;
		for (IntervalData inter : mIntervalData) {
			if (inter.weatherIcon != 0) {
				long intervalPos = (inter.timeFrom + inter.timeTo) / 2;
				if (lastIntervalPos != -1 && lastIntervalPos != intervalPos) {
					sampleResolutionHrs = Math.min(sampleResolutionHrs, Math.abs(intervalPos - lastIntervalPos));
				}
				lastIntervalPos = intervalPos;
			}
		}
		
		sampleResolutionHrs = Math.round((double)sampleResolutionHrs / (double)DateUtils.HOUR_IN_MILLIS);

		if (sampleResolutionHrs > 6) {
			sampleResolutionHrs = Long.MAX_VALUE;
			long lastPointPos = -1;
			for (PointData p : mPointData) {
				if (lastPointPos != -1 && lastPointPos != p.mTime) {
					sampleResolutionHrs = Math.min(sampleResolutionHrs, Math.abs(p.mTime - lastPointPos));
				}
				lastPointPos = p.mTime;
			}
			sampleResolutionHrs = Math.round((double)sampleResolutionHrs / (double)DateUtils.HOUR_IN_MILLIS);
		}
		
		if ((sampleResolutionHrs < 1) || (sampleResolutionHrs > mNumHours / 2)) {
			throw AixWidgetDrawException.buildMissingWeatherDataException();
		}
		
		mNumHoursBetweenSamples = (int)sampleResolutionHrs;
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
		
		Uri.Builder builder = mLocationUri.buildUpon();
		builder.appendPath(AixLocations.TWIG_POINTDATAFORECASTS);
		builder.appendQueryParameter("tu", mUseFahrenheit ? "f" : "c");

		Cursor cursor = mResolver.query(
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
	
	private void setupWidgetDimensions(final boolean isLandscape) {
		mWidgetWidth = (int)Math.round(((isLandscape
				? (106.0f * mWidgetColumns) : (80.0f * mWidgetColumns)) - 2.0f) * mDP);
		mWidgetHeight = (int)Math.round(((isLandscape
				? (74.0f * mWidgetRows) : (100.0f * mWidgetRows)) - 2.0f) * mDP);
		
		boolean isWidgetWidthOdd = mWidgetWidth % 2 == 1;
		boolean isWidgetHeightOdd = mWidgetHeight % 2 == 1;
		
		mWidgetBounds.set(
				isWidgetWidthOdd ? 1 : 0,
				isWidgetHeightOdd ? 1 : 0,
				mWidgetWidth, mWidgetHeight);
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
		
		if (Float.isInfinite(mTemperatureValueMax) || Float.isInfinite(minTemperature)) {
			throw AixWidgetDrawException.buildMissingWeatherDataException();
		}
		
		// Ensure that there are enough point data samples within the time period
		if (validPointDataSamples < 2) {
			throw AixWidgetDrawException.buildMissingWeatherDataException();
		}
		
		mTemperatureValueMax = maxTemperature;
		mTemperatureValueMin = minTemperature;
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
				float tempRangeMax, float tempRangeMin)
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
			
			return path;
		}
		
	}
	
}
