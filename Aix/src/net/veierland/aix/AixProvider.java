package net.veierland.aix;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Map.Entry;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;

public class AixProvider extends ContentProvider {

	private static final String TAG = "AixProvider";
	private static final boolean LOGD = false;
	
	public static final String AUTHORITY = "net.veierland.aix";
	
	public interface AixWidgetsColumns {
		/* The size of the widge in the format COLUMNS_ROWS
		 * where TINY=1, SMALL=2, MEDIUM=3, LARGE=4 */
		public static final String SIZE = "size";
		public static final int SIZE_INVALID = 0;
		
		public static final int SIZE_TINY_TINY = 1;
		public static final int SIZE_TINY_SMALL = 2;
		public static final int SIZE_TINY_MEDIUM = 3;
		public static final int SIZE_TINY_LARGE = 4;
		
		public static final int SIZE_SMALL_TINY = 5;
		public static final int SIZE_SMALL_SMALL = 6;
		public static final int SIZE_SMALL_MEDIUM = 7;
		public static final int SIZE_SMALL_LARGE = 8;
		
		public static final int SIZE_MEDIUM_TINY = 9;
		public static final int SIZE_MEDIUM_SMALL = 10;
		public static final int SIZE_MEDIUM_MEDIUM = 11;
		public static final int SIZE_MEDIUM_LARGE = 12;
		
		public static final int SIZE_LARGE_TINY = 13;
		public static final int SIZE_LARGE_SMALL = 14;
		public static final int SIZE_LARGE_MEDIUM = 15;
		public static final int SIZE_LARGE_LARGE = 16;
		
		/* A colon-separated array in string format of the view IDs linked to the widget */
		public static final String VIEWS = "views";
		
		public static final int APPWIDGET_ID_COLUMN = 0;
		public static final int SIZE_COLUMN = 1;
		public static final int VIEWS_COLUMN = 2;
	}
	
	public static class AixWidgets implements BaseColumns, AixWidgetsColumns {
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/aixwidgets");
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/aixwidget";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/aixwidget";
		
		public static final String TWIG_SETTINGS = "settings";
	}
	
	public interface AixViewsColumns {
		/* The location row ID in the AixLocations table for the view location */
		public static final String LOCATION = "location";
		
		/* The type of a specific view, e.g. detailed or long-term */
		public static final String TYPE = "type";
		public static final int TYPE_DETAILED = 1;
		
		public static final int VIEW_ID_COLUMN = 0;
		public static final int LOCATION_COLUMN = 1;
		public static final int TYPE_COLUMN = 2;
	}
	
	public static class AixViews implements BaseColumns, AixViewsColumns {
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/aixviews");
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/aixview";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/aixview";
		
		public static final String TWIG_SETTINGS = "settings";
		public static final String TWIG_LOCATION = "location";
		public static final String TWIG_POINTDATAFORECASTS = "pointdata_forecasts";
		public static final String TWIG_INTERVALDATAFORECASTS = "intervaldata_forecasts";
		public static final String TWIG_SUNMOONDATA = "sunmoondata";
	}
	
	public interface AixLocationsColumns {
		public static final String TITLE = "title";
		public static final String TITLE_DETAILED = "title_detailed";
		public static final String TIME_ZONE = "timeZone";
		
		/* The type of the location; whether it is set statically or updated dynamically */
		public static final String TYPE = "type";
		public static final int LOCATION_STATIC = 1;
		
		/* The time when the current location fix was set */
		public static final String TIME_OF_LAST_FIX = "time_of_last_fix";
		
		public static final String LATITUDE = "latitude";
		
		public static final String LONGITUDE = "longitude";
		
		/* The time when the forecasts for the location was last updated */
		public static final String LAST_FORECAST_UPDATE = "last_forecast_update";
		
		/* The time when the forecasts for the location is no longer valid */
		public static final String FORECAST_VALID_TO = "forecast_valid_to";
		
		/* The time when the forecasts for the location should be updated next */
		public static final String NEXT_FORECAST_UPDATE = "next_forecast_update";
		
		public static final int LOCATION_ID_COLUMN = 0;
		public static final int TITLE_COLUMN = 1;
		public static final int TITLE_DETAILED_COLUMN = 2;
		public static final int TIME_ZONE_COLUMN = 3;
		public static final int TYPE_COLUMN = 4;
		public static final int TIME_OF_LAST_FIX_COLUMN = 5;
		public static final int LATITUDE_COLUMN = 6;
		public static final int LONGITUDE_COLUMN = 7;
		public static final int LAST_FORECAST_UPDATE_COLUMN = 8;
		public static final int FORECAST_VALID_TO_COLUMN = 9;
		public static final int NEXT_FORECAST_UPDATE_COLUMN = 10;
	}
	
	public static class AixLocations implements BaseColumns, AixLocationsColumns {
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/aixlocations");
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/aixlocation";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/aixlocation";
	}
	
	public interface AixPointDataForecastColumns {
		public static final String LOCATION = "location";
		public static final String TIME_ADDED = "timeAdded";
		public static final String TIME = "time";
		public static final String TEMPERATURE = "temperature";
		public static final String HUMIDITY = "humidity";
		public static final String PRESSURE = "pressure";
		
		public static final int LOCATION_COLUMN = 1;
		public static final int TIME_ADDED_COLUMN = 2;
		public static final int TIME_COLUMN = 3;
		public static final int TEMPERATURE_COLUMN = 4;
		public static final int HUMIDITY_COLUMN = 5;
		public static final int PRESSURE_COLUMN = 6;
	};
	
	public static class AixPointDataForecasts implements BaseColumns, AixPointDataForecastColumns {
		public static final Uri CONTENT_URI = Uri.parse(
				"content://" + AUTHORITY + "/aixpointdataforecasts");
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/aixpointdataforecasts";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/aixpointdataforecast";
	}
	
	public interface AixIntervalDataForecastColumns {
		public static final String LOCATION = "location";
		public static final String TIME_ADDED = "timeAdded";
		public static final String TIME_FROM = "timeFrom";
		public static final String TIME_TO = "timeTo";
		public static final String RAIN_VALUE = "rainValue";
		public static final String RAIN_MINVAL = "rainLowVal";
		public static final String RAIN_MAXVAL = "rainMaxVal";
		public static final String WEATHER_ICON = "weatherIcon";
		
		public static final int LOCATION_COLUMN = 1;
		public static final int TIME_ADDED_COLUMN = 2;
		public static final int TIME_FROM_COLUMN = 3;
		public static final int TIME_TO_COLUMN = 4;
		public static final int RAIN_VALUE_COLUMN = 5;
		public static final int RAIN_MINVAL_COLUMN = 6;
		public static final int RAIN_MAXVAL_COLUMN = 7;
		public static final int WEATHER_ICON_COLUMN = 8;
	};
	
	public static class AixIntervalDataForecasts implements BaseColumns, AixIntervalDataForecastColumns {
		public static final Uri CONTENT_URI = Uri.parse(
				"content://" + AUTHORITY + "/aixintervaldataforecasts");
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/aixintervaldataforecasts";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/aixintervaldataforecast";
	}
	
	public interface AixSunMoonDataColumns {
		public static final String LOCATION = "location";
		public static final String TIME_ADDED = "timeAdded";
		public static final String DATE = "date";
		public static final String SUN_RISE = "sunRise";
		public static final String SUN_SET = "sunSet";
		public static final String MOON_RISE = "moonRise";
		public static final String MOON_SET = "moonSet";
		public static final String MOON_PHASE = "moonPhase";
		
		public static final int NO_MOON_PHASE_DATA = -1;
		public static final int NEW_MOON = 1;
		public static final int WAXING_CRESCENT = 2;
		public static final int FIRST_QUARTER = 3;
		public static final int WAXING_GIBBOUS = 4;
		public static final int FULL_MOON = 5;
		public static final int WANING_GIBBOUS = 6;
		public static final int LAST_QUARTER = 7;
		public static final int WANING_CRESCENT = 8;
		public static final int DARK_MOON = 9;
		
		public static final int LOCATION_COLUMN = 1;
		public static final int TIME_ADDED_COLUMN = 2;
		public static final int DATE_COLUMN = 3;
		public static final int SUN_RISE_COLUMN = 4;
		public static final int SUN_SET_COLUMN = 5;
		public static final int MOON_RISE_COLUMN = 6;
		public static final int MOON_SET_COLUMN = 7;
		public static final int MOON_PHASE_COLUMN = 8;
	}
	
	public static class AixSunMoonData implements BaseColumns, AixSunMoonDataColumns {
		public static final Uri CONTENT_URI = Uri.parse(
				"content://" + AUTHORITY + "/aixsunmoondata");
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/aixsunmoondata";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/aixsunmoondata";
		
		public static final long NEVER_RISE = -1;
		public static final long NEVER_SET = -2;
	}
	
	public interface AixSettingsColumns {
		public static final String ROW_ID = "rowId";
		public static final String KEY = "key";
		public static final String VALUE = "value";
		
		public static final int SETTING_ID_COLUMN = 0;
		public static final int ROW_ID_COLUMN = 1;
		public static final int KEY_COLUMN = 2;
		public static final int VALUE_COLUMN = 3;
	}
	
	public static abstract class AixSettings implements BaseColumns, AixSettingsColumns {
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/aixsetting";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/aixsetting";
	}
	
	public static class AixWidgetSettings extends AixSettings {
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/aixwidgetsettings");
		
//		public static final String TEMPERATURE_UNITS = "temperatureUnits";
		public static final int TEMPERATURE_UNITS_CELSIUS = 1;
		public static final int TEMPERATURE_UNITS_FAHRENHEIT = 2;
//		
//		public static final String PRECIPITATION_UNITS = "precipitationUnits";
		public static final int PRECIPITATION_UNITS_MM = 1;
		public static final int PRECIPITATION_UNITS_INCHES = 2;
//		
//		public static final String PRECIPITATION_SCALE = "precipitationScale";
//		
//		public static final String BACKGROUND_COLOR = "backgroundColor";
//		public static final String TEXT_COLOR = "textColor";
//		public static final String LOCATION_BACKGROUND_COLOR = "locationBackgroundColor";
//		public static final String LOCATION_TEXT_COLOR = "locationTextColor";
//		public static final String GRID_COLOR = "gridColor";
//		public static final String GRID_OUTLINE_COLOR = "gridOutlineColor";
//		public static final String MAX_RAIN_COLOR = "maxRainColor";
//		public static final String MIN_RAIN_COLOR = "minRainColor";
//		public static final String ABOVE_FREEZING_COLOR = "aboveFreezingColor";
//		public static final String BELOW_FREEZING_COLOR = "belowFreezingColor";
	}
	
	public static class AixViewSettings extends AixSettings {
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/aixviewsettings");
		
		/* The period of the view; 24 hours / 48 hours / 96 hours */
//		public static final String PERIOD = "period";
//		public static final int PERIOD_24_HOURS = 1;
//		public static final int PERIOD_48_HOURS = 2;
//		public static final int PERIOD_96_HOURS = 3;
	}
	
	private static final String TABLE_AIXWIDGETS = "aixwidgets";
	private static final String TABLE_AIXWIDGETSETTINGS = "aixwidgetsettings";
	private static final String TABLE_AIXVIEWS = "aixviews";
	private static final String TABLE_AIXVIEWSETTINGS = "aixviewsettings";
	private static final String TABLE_AIXLOCATIONS = "aixlocations";
	private static final String TABLE_AIXFORECASTS = "aixforecasts";
	private static final String TABLE_AIXPOINTDATAFORECASTS = "aixpointdataforecasts";
	private static final String TABLE_AIXINTERVALDATAFORECASTS = "aixintervaldataforecasts";
	private static final String TABLE_AIXSUNMOONDATA = "aixsunmoondata";
	
	private DatabaseHelper mOpenHelper;
	
	private static class DatabaseHelper extends SQLiteOpenHelper {
		private static final String DATABASE_NAME = "aix_database.db";
		private static final int DATABASE_VERSION = 8;
		// 0.1.6 = version 8
		// 0.1.5 = version 7
		// 0.1.4 = version 6
		// 0.1.3 = version 5
		
		private Context mContext;
		
		public DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
			mContext = context;
		}
		
		@Override
		public void onCreate(SQLiteDatabase db) {
			Log.d(TAG, "onCreate()");
			createWidgetViewLocationTables(db);
			createForecastTable(db);
			
			ContentValues values = new ContentValues();
			values.put(AixLocationsColumns.TITLE, "Brussels");
			values.put(AixLocationsColumns.TITLE_DETAILED, "Brussels, Belgium");
			values.put(AixLocationsColumns.LATITUDE, 50.85f);
			values.put(AixLocationsColumns.LONGITUDE, 4.35f);
			db.insert(TABLE_AIXLOCATIONS, null, values);
			values.clear();
			values.put(AixLocationsColumns.TITLE, "Luxembourg");
			values.put(AixLocationsColumns.TITLE_DETAILED, "Luxembourg, Europe");
			values.put(AixLocationsColumns.LATITUDE, 49.6f);
			values.put(AixLocationsColumns.LONGITUDE, 6.116667f);
			db.insert(TABLE_AIXLOCATIONS, null, values);
			values.clear();
			values.put(AixLocationsColumns.TITLE, "Oslo");
			values.put(AixLocationsColumns.TITLE_DETAILED, "Oslo, Norway");
			values.put(AixLocationsColumns.LATITUDE, 59.949444f);
			values.put(AixLocationsColumns.LONGITUDE, 10.756389f);
			db.insert(TABLE_AIXLOCATIONS, null, values);
		}

		private void createWidgetViewLocationTables(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + TABLE_AIXWIDGETS + " ("
					+ BaseColumns._ID + " INTEGER PRIMARY KEY,"
					+ AixWidgetsColumns.SIZE + " INTEGER,"
					+ AixWidgetsColumns.VIEWS + " TEXT);");
			
			db.execSQL("CREATE TABLE " + TABLE_AIXWIDGETSETTINGS + " ("
					+ BaseColumns._ID + " INTEGER PRIMARY KEY,"
					+ AixSettingsColumns.ROW_ID + " INTEGER,"
					+ AixSettingsColumns.KEY + " TEXT,"
					+ AixSettingsColumns.VALUE + " TEXT);");
			
			db.execSQL("CREATE TABLE " + TABLE_AIXVIEWS + " ("
					+ BaseColumns._ID + " INTEGER PRIMARY KEY,"
					+ AixViewsColumns.LOCATION + " INTEGER,"
					+ AixViewsColumns.TYPE + " INTEGER);");
			
			db.execSQL("CREATE TABLE " + TABLE_AIXVIEWSETTINGS + " ("
					+ BaseColumns._ID + " INTEGER PRIMARY KEY,"
					+ AixSettingsColumns.ROW_ID + " INTEGER,"
					+ AixSettingsColumns.KEY + " TEXT,"
					+ AixSettingsColumns.VALUE + " TEXT);");
			
			db.execSQL("CREATE TABLE " + TABLE_AIXLOCATIONS + " ("
					+ BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
					+ AixLocationsColumns.TITLE + " TEXT,"
					+ AixLocationsColumns.TITLE_DETAILED + " TEXT,"
					+ AixLocationsColumns.TIME_ZONE + " TEXT,"
					+ AixLocationsColumns.TYPE + " INTEGER,"
					+ AixLocationsColumns.TIME_OF_LAST_FIX + " INTEGER,"
					+ AixLocationsColumns.LATITUDE + " REAL,"
					+ AixLocationsColumns.LONGITUDE + " REAL,"
					+ AixLocationsColumns.LAST_FORECAST_UPDATE + " INTEGER,"
					+ AixLocationsColumns.FORECAST_VALID_TO + " INTEGER,"
					+ AixLocationsColumns.NEXT_FORECAST_UPDATE + " INTEGER);");
		}

		private void createForecastTable(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + TABLE_AIXPOINTDATAFORECASTS + " ("
					+ BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
					+ AixPointDataForecastColumns.LOCATION + " INTEGER,"
					+ AixPointDataForecastColumns.TIME_ADDED + " INTEGER,"
					+ AixPointDataForecastColumns.TIME + " INTEGER,"
					+ AixPointDataForecastColumns.TEMPERATURE + " REAL,"
					+ AixPointDataForecastColumns.HUMIDITY + " REAL,"
					+ AixPointDataForecastColumns.PRESSURE + " REAL);");
			
			db.execSQL("CREATE TABLE " + TABLE_AIXINTERVALDATAFORECASTS + " ("
					+ BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
					+ AixIntervalDataForecastColumns.LOCATION + " INTEGER,"
					+ AixIntervalDataForecastColumns.TIME_ADDED + " INTEGER,"
					+ AixIntervalDataForecastColumns.TIME_FROM + " INTEGER,"
					+ AixIntervalDataForecastColumns.TIME_TO + " INTEGER,"
					+ AixIntervalDataForecastColumns.RAIN_VALUE + " REAL,"
					+ AixIntervalDataForecastColumns.RAIN_MINVAL + " REAL,"
					+ AixIntervalDataForecastColumns.RAIN_MAXVAL + " REAL,"
					+ AixIntervalDataForecastColumns.WEATHER_ICON + " INTEGER);");
			
			db.execSQL("CREATE TABLE " + TABLE_AIXSUNMOONDATA + " ("
					+ BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
					+ AixSunMoonDataColumns.LOCATION + " INTEGER,"
					+ AixSunMoonDataColumns.TIME_ADDED + " INTEGER,"
					+ AixSunMoonDataColumns.DATE + " INTEGER,"
					+ AixSunMoonDataColumns.SUN_RISE + " INTEGER,"
					+ AixSunMoonDataColumns.SUN_SET + " INTEGER,"
					+ AixSunMoonDataColumns.MOON_RISE + " INTEGER,"
					+ AixSunMoonDataColumns.MOON_SET + " INTEGER,"
					+ AixSunMoonDataColumns.MOON_PHASE + " INTEGER);");
		}
		
		private void migrateProperty(ContentValues values, Cursor cursor, String id, int column) {
			String color = cursor.getString(column);
			if (color != null) {
				values.put(id, color);
			}
		}
		
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.d(TAG, "onUpdate() oldVersion=" + oldVersion + ",newVersion=" + newVersion);
			if (oldVersion == 5) {
				ArrayList<ContentValues> widgets = new ArrayList<ContentValues>();
				ArrayList<ContentValues> widgetSettings = new ArrayList<ContentValues>();
				ArrayList<ContentValues> views = new ArrayList<ContentValues>();
				ArrayList<ContentValues> locations = new ArrayList<ContentValues>();
				
				Cursor cursor = db.query(TABLE_AIXWIDGETS, null, null, null, null, null, null);
				if (cursor != null) {
					if (cursor.moveToFirst()) {
						do {
							ContentValues widget = new ContentValues();
							widget.put(BaseColumns._ID, cursor.getLong(0));
							widget.put(AixWidgetsColumns.SIZE, AixWidgets.SIZE_LARGE_TINY);
							widget.put(AixWidgetsColumns.VIEWS, cursor.getLong(3));
							widgets.add(widget);
							
							ContentValues settings = new ContentValues();
							settings.put(AixSettingsColumns.ROW_ID, cursor.getLong(0));
							migrateProperty(settings, cursor, mContext.getString(R.string.temperature_units_string), 4);
							migrateProperty(settings, cursor, mContext.getString(R.string.precipitation_units_string), 5);
							migrateProperty(settings, cursor, mContext.getString(R.string.background_color_int), 7);
							migrateProperty(settings, cursor, mContext.getString(R.string.text_color_int), 8);
							migrateProperty(settings, cursor, mContext.getString(R.string.grid_color_int), 11);
							migrateProperty(settings, cursor, mContext.getString(R.string.grid_outline_color_int), 12);
							migrateProperty(settings, cursor, mContext.getString(R.string.max_rain_color_int), 13);
							migrateProperty(settings, cursor, mContext.getString(R.string.min_rain_color_int), 14);
							migrateProperty(settings, cursor, mContext.getString(R.string.above_freezing_color_int), 15);
							migrateProperty(settings, cursor, mContext.getString(R.string.below_freezing_color_int), 16);
							widgetSettings.add(settings);
						} while (cursor.moveToNext());
					}
					cursor.close();
				}
				cursor = db.query(TABLE_AIXVIEWS, null, null, null, null, null, null);
				if (cursor != null) {
					if (cursor.moveToFirst()) {
						do {
							ContentValues view = new ContentValues();
							long viewId = cursor.getLong(0);
							view.put(BaseColumns._ID, viewId);
							view.put(AixViewsColumns.LOCATION, cursor.getLong(3));
							view.put(AixViewsColumns.TYPE, AixViewsColumns.TYPE_DETAILED);
							views.add(view);
						} while (cursor.moveToNext());
					}
					cursor.close();
				}
				cursor = db.query(TABLE_AIXLOCATIONS, null, null, null, null, null, null);
				if (cursor != null) {
					if (cursor.moveToFirst()) {
						do {
							ContentValues location = new ContentValues();
							location.put(BaseColumns._ID, cursor.getLong(0));
							location.put(AixLocationsColumns.TITLE, cursor.getString(1));
							location.put(AixLocationsColumns.TITLE_DETAILED, cursor.getString(2));
							location.put(AixLocationsColumns.LATITUDE, cursor.getString(6));
							location.put(AixLocationsColumns.LONGITUDE, cursor.getString(7));
							locations.add(location);
						} while (cursor.moveToNext());
					}
					cursor.close();
				}
				
				db.execSQL("DROP TABLE IF EXISTS " + TABLE_AIXWIDGETS);
				db.execSQL("DROP TABLE IF EXISTS " + TABLE_AIXVIEWS);
				db.execSQL("DROP TABLE IF EXISTS " + TABLE_AIXLOCATIONS);
				db.execSQL("DROP TABLE IF EXISTS " + TABLE_AIXFORECASTS);
				createWidgetViewLocationTables(db);
				createForecastTable(db);
				
				for (ContentValues widget : widgets) {
					db.insert(TABLE_AIXWIDGETS, null, widget);
				}
				ContentValues temp = new ContentValues();
				for (ContentValues setting : widgetSettings) {
					long widgetId = setting.getAsLong(AixSettingsColumns.ROW_ID);
					setting.remove(AixSettingsColumns.ROW_ID);
					for (Entry<String, Object> entry : setting.valueSet()) {
						temp.clear();
						temp.put(AixSettingsColumns.ROW_ID, widgetId);
						temp.put(AixSettingsColumns.KEY, entry.getKey());
						temp.put(AixSettingsColumns.VALUE, (String)entry.getValue());
						db.insert(TABLE_AIXWIDGETSETTINGS, null, temp);
					}
				}
				for (ContentValues view : views) {
					db.insert(TABLE_AIXVIEWS, null, view);
				}
				for (ContentValues location : locations) {
					db.insert(TABLE_AIXLOCATIONS, null, location);
				}
			} else if (oldVersion == 6 || oldVersion == 7) {
				// Humidity and pressure fields were added to forecasts table
				db.execSQL("DROP TABLE IF EXISTS " + TABLE_AIXPOINTDATAFORECASTS);
				db.execSQL("DROP TABLE IF EXISTS " + TABLE_AIXINTERVALDATAFORECASTS);
				db.execSQL("DROP TABLE IF EXISTS " + TABLE_AIXFORECASTS);
				createForecastTable(db);
			} else {
				db.execSQL("DROP TABLE IF EXISTS " + TABLE_AIXWIDGETS);
				db.execSQL("DROP TABLE IF EXISTS " + TABLE_AIXVIEWS);
				db.execSQL("DROP TABLE IF EXISTS " + TABLE_AIXLOCATIONS);
				db.execSQL("DROP TABLE IF EXISTS " + TABLE_AIXFORECASTS);
				onCreate(db);
			}
			
			ContentValues values = new ContentValues();
			
			if (oldVersion < 7) {
				Cursor c = db.query(TABLE_AIXWIDGETS, null, null, null, null, null, null);
				if (c != null) {
					if (c.moveToFirst()) {
						values.put(AixWidgetSettings.KEY, mContext.getString(R.string.day_effect_bool));
						values.put(AixWidgetSettings.VALUE, "true");
						do {
							values.put(AixWidgetSettings.ROW_ID, c.getLong(0));
							db.insert(TABLE_AIXWIDGETSETTINGS, null, values);
						} while (c.moveToNext());
					}
				}
			}
			
			// All forecast data has been deleted.
			values.clear();
			values.put(AixLocationsColumns.LAST_FORECAST_UPDATE, 0);
			values.put(AixLocationsColumns.FORECAST_VALID_TO, 0);
			values.put(AixLocationsColumns.NEXT_FORECAST_UPDATE, 0);
			db.update(TABLE_AIXLOCATIONS, values, null, null);
		}
	}
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		if (LOGD) Log.d(TAG, "delete() with uri=" + uri);
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		
		int count = 0;
		
		switch (sUriMatcher.match(uri)) {
		case AIXWIDGETS: {
			count = db.delete(TABLE_AIXWIDGETS, selection, selectionArgs);
			break;
		}
		case AIXWIDGETS_ID: {
			count = db.delete(TABLE_AIXWIDGETS, buildSelectionStringFromIdUri(uri, selection), selectionArgs);
			break;
		}
		case AIXWIDGETSETTINGS: {
			count = db.delete(TABLE_AIXWIDGETSETTINGS, selection, selectionArgs);
			break;
		}
		case AIXWIDGETSETTINGS_ID: {
			count = db.delete(TABLE_AIXWIDGETSETTINGS, buildSelectionStringFromIdUri(uri, selection), selectionArgs);
			break;
		}
		case AIXVIEWS: {
			count = db.delete(TABLE_AIXVIEWS, selection, selectionArgs);
			break;
		}
		case AIXVIEWS_ID: {
			count = db.delete(TABLE_AIXVIEWS, buildSelectionStringFromIdUri(uri, selection), selectionArgs);
			break;
		}
		case AIXVIEWS_POINTDATAFORECASTS: {
			long locationId = findLocationFromView(db, uri);
			if (locationId != -1) {
				count = db.delete(
						TABLE_AIXPOINTDATAFORECASTS,
						buildSelectionString(AixPointDataForecastColumns.LOCATION,
								Long.toString(locationId), selection), selectionArgs);
			}
			break;
		}
		case AIXVIEWS_INTERVALDATAFORECASTS: {
			long locationId = findLocationFromView(db, uri);
			if (locationId != -1) {
				count = db.delete(
						TABLE_AIXINTERVALDATAFORECASTS,
						buildSelectionString(AixIntervalDataForecastColumns.LOCATION,
								Long.toString(locationId), selection), selectionArgs);
			}
			break;
		}
		case AIXVIEWS_SUNMOONDATA: {
			long locationId = findLocationFromView(db, uri);
			if (locationId != -1) {
				count = db.delete(
						TABLE_AIXSUNMOONDATA,
						buildSelectionString(AixSunMoonDataColumns.LOCATION,
								Long.toString(locationId), selection), selectionArgs);
			}
			break;
		}
		case AIXVIEWSETTINGS: {
			count = db.delete(TABLE_AIXVIEWSETTINGS, selection, selectionArgs);
			break;
		}
		case AIXVIEWSETTINGS_ID: {
			count = db.delete(TABLE_AIXVIEWSETTINGS, buildSelectionStringFromIdUri(uri, selection), selectionArgs);
			break;
		}
		case AIXLOCATIONS: {
			count = db.delete(TABLE_AIXLOCATIONS, selection, selectionArgs);
			break;
		}
		case AIXLOCATIONS_ID: {
			count = db.delete(TABLE_AIXLOCATIONS, buildSelectionStringFromIdUri(uri, selection), selectionArgs);
			break;
		}
		case AIXPOINTDATAFORECASTS: {
			count = db.delete(TABLE_AIXPOINTDATAFORECASTS, selection, selectionArgs);
			break;
		}
		case AIXPOINTDATAFORECASTS_ID: {
			count = db.delete(TABLE_AIXPOINTDATAFORECASTS, buildSelectionStringFromIdUri(uri, selection), selectionArgs);
			break;
		}
		case AIXINTERVALDATAFORECASTS: {
			count = db.delete(TABLE_AIXINTERVALDATAFORECASTS, selection, selectionArgs);
			break;
		}
		case AIXINTERVALDATAFORECASTS_ID: {
			count = db.delete(TABLE_AIXINTERVALDATAFORECASTS, buildSelectionStringFromIdUri(uri, selection), selectionArgs);
			break;
		}
		case AIXSUNMOONDATA: {
			count = db.delete(TABLE_AIXSUNMOONDATA, selection, selectionArgs);
			break;
		}
		case AIXSUNMOONDATA_ID: {
			count = db.delete(TABLE_AIXSUNMOONDATA, buildSelectionStringFromIdUri(uri, selection), selectionArgs);
			break;
		}
		}
		
		return count;
	}

	@Override
	public String getType(Uri uri) {
		switch (sUriMatcher.match(uri)) {
		case AIXWIDGETS:
			return AixWidgets.CONTENT_TYPE;
		case AIXWIDGETS_ID:
			return AixWidgets.CONTENT_ITEM_TYPE;
		case AIXWIDGETS_ID_SETTINGS:
			return AixWidgetSettings.CONTENT_TYPE;
		case AIXWIDGETSETTINGS:
			return AixWidgetSettings.CONTENT_TYPE;
		case AIXWIDGETSETTINGS_ID:
			return AixWidgetSettings.CONTENT_ITEM_TYPE;
		case AIXVIEWS:
			return AixViews.CONTENT_TYPE;
		case AIXVIEWS_ID:
			return AixViews.CONTENT_ITEM_TYPE;
		case AIXVIEWS_ID_SETTINGS:
			return AixViewSettings.CONTENT_TYPE;
		case AIXVIEWS_LOCATION:
			return AixLocations.CONTENT_ITEM_TYPE;
		case AIXVIEWS_POINTDATAFORECASTS:
			return AixPointDataForecasts.CONTENT_TYPE;
		case AIXVIEWS_INTERVALDATAFORECASTS:
			return AixIntervalDataForecasts.CONTENT_TYPE;
		case AIXVIEWS_SUNMOONDATA:
			return AixSunMoonData.CONTENT_TYPE;
		case AIXVIEWSETTINGS:
			return AixViewSettings.CONTENT_TYPE;
		case AIXVIEWSETTINGS_ID:
			return AixViewSettings.CONTENT_ITEM_TYPE;
		case AIXLOCATIONS:
			return AixLocations.CONTENT_TYPE;
		case AIXLOCATIONS_ID:
			return AixLocations.CONTENT_ITEM_TYPE;
		case AIXPOINTDATAFORECASTS:
			return AixPointDataForecasts.CONTENT_TYPE;
		case AIXPOINTDATAFORECASTS_ID:
			return AixPointDataForecasts.CONTENT_ITEM_TYPE;
		case AIXINTERVALDATAFORECASTS:
			return AixIntervalDataForecasts.CONTENT_TYPE;
		case AIXINTERVALDATAFORECASTS_ID:
			return AixIntervalDataForecasts.CONTENT_ITEM_TYPE;
		case AIXSUNMOONDATA:
			return AixSunMoonData.CONTENT_TYPE;
		case AIXSUNMOONDATA_ID:
			return AixSunMoonData.CONTENT_ITEM_TYPE;
		}
		throw new IllegalStateException();
	}
	
	@Override
	public ParcelFileDescriptor openFile(Uri uri, String mode)
			throws FileNotFoundException
	{
		if (sUriMatcher.match(uri) == AIXRENDER) {
			String widgetId = uri.getPathSegments().get(1);
			String updateId = uri.getPathSegments().get(2);
			long updateTime = Long.parseLong(updateId);
			
			File dir = getContext().getCacheDir();
			File[] files = dir.listFiles();
				
			for (int i = 0; i < files.length; i++) {
				File f = files[i];
				String[] s = f.getName().split("_");
				if (s != null && s.length == 4) {
					long filetime = Long.parseLong(s[2]);
					if (	(s[1].equals(widgetId) && filetime < updateTime) ||
							(filetime < updateTime - 6 * DateUtils.HOUR_IN_MILLIS))
					{
						f.delete();
					}
				}
			}
			
			AppWidgetManager manager = AppWidgetManager.getInstance(getContext());
			AppWidgetProviderInfo info = manager.getAppWidgetInfo(Integer.parseInt(widgetId));
			Log.d(TAG, "Delivering render minWidth=" + info.minWidth + " minHeight=" + info.minHeight);
			
			StringBuilder sb = new StringBuilder();
			sb.append("aix_");
			sb.append(widgetId);
			sb.append('_');
			sb.append(updateId);
			
			String orientation = uri.getPathSegments().get(3);
			if (orientation != null && orientation.equals("landscape")) {
				sb.append("_landscape.png");
			} else {
				sb.append("_portrait.png");
			}
			
			File file = new File(getContext().getCacheDir(), sb.toString());
			return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
		}
		return null;
	}
	
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		if (LOGD) Log.d(TAG, "insert() with uri=" + uri);
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		
		Uri resultUri = null;
		
		switch (sUriMatcher.match(uri)) {
		case AIXWIDGETS: {
			long rowId = db.insert(TABLE_AIXWIDGETS, null, values);
			if (rowId != -1) {
				resultUri = ContentUris.withAppendedId(AixWidgets.CONTENT_URI, rowId);
				getContext().getContentResolver().notifyChange(resultUri, null);
			}
			break;
		}
		case AIXWIDGETS_ID_SETTINGS: {
			String widgetId = uri.getPathSegments().get(1);
			boolean success = true;
			for (Entry<String, Object> entry : values.valueSet()) {
				if (addSetting(db, TABLE_AIXWIDGETSETTINGS, widgetId, entry) == -1) {
					success = false;
				}
			}
			if (success) {
				resultUri = AixWidgetSettings.CONTENT_URI;
			}
			break;
		}
		case AIXWIDGETSETTINGS: {
			long rowId = db.insert(TABLE_AIXWIDGETSETTINGS, null, values);
			if (rowId != -1) {
				resultUri = ContentUris.withAppendedId(AixWidgetSettings.CONTENT_URI, rowId);
				getContext().getContentResolver().notifyChange(resultUri, null);
			}
			break;
		}
		case AIXVIEWS: {
			long rowId = db.insert(TABLE_AIXVIEWS, null, values);
			if (rowId != -1) {
				resultUri = ContentUris.withAppendedId(AixViews.CONTENT_URI, rowId);
				getContext().getContentResolver().notifyChange(resultUri, null);
			}
			break;
		}
		case AIXVIEWS_ID_SETTINGS: {
			String viewId = uri.getPathSegments().get(1);
			boolean success = true;
			for (Entry<String, Object> entry : values.valueSet()) {
				if (addSetting(db, TABLE_AIXVIEWSETTINGS, viewId, entry) == -1) {
					success = false;
				}
			}
			if (success) {
				resultUri = AixViewSettings.CONTENT_URI;
			}
			break;
		}
		case AIXVIEWSETTINGS: {
			long rowId = db.insert(TABLE_AIXVIEWSETTINGS, null, values);
			if (rowId != -1) {
				resultUri = ContentUris.withAppendedId(AixViewSettings.CONTENT_URI, rowId);
				getContext().getContentResolver().notifyChange(resultUri, null);
			}
			break;
		}
		case AIXLOCATIONS: {
			long rowId = db.insert(TABLE_AIXLOCATIONS, null, values);
			if (rowId != -1) {
				resultUri = ContentUris.withAppendedId(AixLocations.CONTENT_URI, rowId);
				getContext().getContentResolver().notifyChange(resultUri, null);
			}
			break;
		}
		case AIXPOINTDATAFORECASTS: {
			long rowId = -1;
			
			Cursor cursor = db.query(
					TABLE_AIXPOINTDATAFORECASTS,
					null,
					AixPointDataForecastColumns.LOCATION + "=? AND " +
					AixPointDataForecastColumns.TIME + "=?",
					new String[] {
							values.getAsString(AixPointDataForecastColumns.LOCATION),
							values.getAsString(AixPointDataForecastColumns.TIME)
					}, null, null, null);
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					rowId = cursor.getLong(0);
				}
				cursor.close();
			}
			
			if (rowId != -1) {
				db.update(TABLE_AIXPOINTDATAFORECASTS, values, BaseColumns._ID + '=' + rowId, null);
			} else {
				rowId = db.insert(TABLE_AIXPOINTDATAFORECASTS, null, values);
			}
			
			if (rowId != -1) {
				resultUri = ContentUris.withAppendedId(AixPointDataForecasts.CONTENT_URI, rowId);
			}
			break;
		}
		case AIXINTERVALDATAFORECASTS: {
			long rowId = -1;
			
			Cursor cursor = db.query(
					TABLE_AIXINTERVALDATAFORECASTS,
					null,
					AixIntervalDataForecastColumns.LOCATION + "=? AND " +
					AixIntervalDataForecastColumns.TIME_FROM + "=? AND " +
					AixIntervalDataForecastColumns.TIME_TO + "=?",
					new String[] {
							values.getAsString(AixIntervalDataForecastColumns.LOCATION),
							values.getAsString(AixIntervalDataForecastColumns.TIME_FROM),
							values.getAsString(AixIntervalDataForecastColumns.TIME_TO)
					}, null, null, null);
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					rowId = cursor.getLong(0);
				}
				cursor.close();
			}
			
			if (rowId != -1) {
				db.update(TABLE_AIXINTERVALDATAFORECASTS, values, BaseColumns._ID + '=' + rowId, null);
			} else {
				rowId = db.insert(TABLE_AIXINTERVALDATAFORECASTS, null, values);
			}
			
			if (rowId != -1) {
				resultUri = ContentUris.withAppendedId(AixIntervalDataForecasts.CONTENT_URI, rowId);
			}
			break;
		}
		case AIXSUNMOONDATA: {
			long rowId = -1;
			
			Cursor cursor = db.query(
					TABLE_AIXSUNMOONDATA,
					null,
					AixSunMoonDataColumns.LOCATION + "=? AND " +
					AixSunMoonDataColumns.DATE + "=?",
					new String[] {
							values.getAsString(AixSunMoonDataColumns.LOCATION),
							values.getAsString(AixSunMoonDataColumns.DATE)
					}, null, null, null);
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					rowId = cursor.getLong(0);
				}
				cursor.close();
			}
			
			if (rowId != -1) {
				// A record already exists for this time and location. Update its values:
				db.update(TABLE_AIXSUNMOONDATA, values, BaseColumns._ID + '=' + rowId, null);
			} else {
				// No record exists for this time. Insert new row:
				rowId = db.insert(TABLE_AIXSUNMOONDATA, null, values);
			}
			
			if (rowId != -1) {
				resultUri = ContentUris.withAppendedId(AixSunMoonData.CONTENT_URI, rowId);
			}

			break;
		}
		default:
			throw new UnsupportedOperationException();
		}
		
		return resultUri;
	}

	@Override
	public boolean onCreate() {
		mOpenHelper = new DatabaseHelper(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder)
	{
		if (LOGD) Log.d(TAG, "query() with uri=" + uri);
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		
		int uriMatch = sUriMatcher.match(uri);
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

		switch (sUriMatcher.match(uri)) {
		case AIXWIDGETS: {
			qb.setTables(TABLE_AIXWIDGETS);
			break;
		}
		case AIXWIDGETS_ID: {
			qb.setTables(TABLE_AIXWIDGETS);
			qb.appendWhere(BaseColumns._ID + '=' + uri.getPathSegments().get(1));
			break;
		}
		case AIXWIDGETS_ID_SETTINGS: {
			qb.setTables(TABLE_AIXWIDGETSETTINGS);
			qb.appendWhere(AixSettingsColumns.ROW_ID + '=' + uri.getPathSegments().get(1));
			break;
		}
		case AIXWIDGETSETTINGS: {
			qb.setTables(TABLE_AIXWIDGETSETTINGS);
			break;
		}
		case AIXWIDGETSETTINGS_ID: {
			qb.setTables(TABLE_AIXWIDGETSETTINGS);
			qb.appendWhere(BaseColumns._ID + '=' + uri.getPathSegments().get(1));
		}
		case AIXVIEWS: {
			qb.setTables(TABLE_AIXVIEWS);
			break;
		}
		case AIXVIEWS_ID: {
			qb.setTables(TABLE_AIXVIEWS);
			qb.appendWhere(BaseColumns._ID + '=' + uri.getPathSegments().get(1));
			break;
		}
		case AIXVIEWS_ID_SETTINGS: {
			qb.setTables(TABLE_AIXVIEWSETTINGS);
			qb.appendWhere(AixSettingsColumns.ROW_ID + '=' + uri.getPathSegments().get(1));
			break;
		}
		case AIXVIEWS_LOCATION: {
			long locationId = findLocationFromView(db, uri);
			if (locationId != -1) {
				qb.setTables(TABLE_AIXLOCATIONS);
				qb.appendWhere(BaseColumns._ID + '=' + locationId);
			} else {
				if (LOGD) Log.d(TAG, "query() with uri=" + uri + " failed. No location in view!");
				return null; // Could not properly service request, as no location was found.
			}
			break;
		}
		case AIXVIEWS_POINTDATAFORECASTS: {
			long locationId = findLocationFromView(db, uri);
			if (locationId != -1) {
				qb.setTables(TABLE_AIXPOINTDATAFORECASTS);
				qb.appendWhere(AixPointDataForecastColumns.LOCATION + '=' + locationId);
				if (projection == null) projection = buildPointDataProjection(uri);
			} else {
				if (LOGD) Log.d(TAG, "query() with uri=" + uri + " failed. No location in view!");
				return null;
			}
			break;
		}
		case AIXVIEWS_INTERVALDATAFORECASTS: {
			long locationId = findLocationFromView(db, uri);
			if (locationId != -1) {
				qb.setTables(TABLE_AIXINTERVALDATAFORECASTS);
				qb.appendWhere(AixIntervalDataForecastColumns.LOCATION + '=' + locationId);
				if (projection == null) projection = buildIntervalDataProjection(uri);
			} else {
				if (LOGD) Log.d(TAG, "query() with uri=" + uri + " failed. No location in view!");
				return null;
			}
			break;
		}
		case AIXVIEWS_SUNMOONDATA: {
			long locationId = findLocationFromView(db, uri);
			if (locationId != -1) {
				qb.setTables(TABLE_AIXSUNMOONDATA);
				qb.appendWhere(AixSunMoonDataColumns.LOCATION + '=' + locationId);
			} else {
				if (LOGD) Log.d(TAG, "query() with uri=" + uri + " failed. No location in view!");
				return null;
			}
			break;
		}
		case AIXVIEWSETTINGS: {
			qb.setTables(TABLE_AIXVIEWSETTINGS);
			break;
		}
		case AIXVIEWSETTINGS_ID: {
			qb.setTables(TABLE_AIXVIEWSETTINGS);
			qb.appendWhere(BaseColumns._ID + '=' + uri.getPathSegments().get(1));
			break;
		}
		case AIXLOCATIONS: {
			qb.setTables(TABLE_AIXLOCATIONS);
			break;
		}
		case AIXLOCATIONS_ID: {
			qb.setTables(TABLE_AIXLOCATIONS);
			qb.appendWhere(BaseColumns._ID + '=' + uri.getPathSegments().get(1));
			break;
		}
		case AIXPOINTDATAFORECASTS: {
			qb.setTables(TABLE_AIXPOINTDATAFORECASTS);
			if (projection == null) projection = buildPointDataProjection(uri);
			break;
		}
		case AIXPOINTDATAFORECASTS_ID: {
			qb.setTables(TABLE_AIXPOINTDATAFORECASTS);
			qb.appendWhere(BaseColumns._ID + '=' + uri.getPathSegments().get(1));
			if (projection == null) projection = buildPointDataProjection(uri);
			break;
		}
		case AIXINTERVALDATAFORECASTS: {
			qb.setTables(TABLE_AIXINTERVALDATAFORECASTS);
			break;
		}
		case AIXINTERVALDATAFORECASTS_ID: {
			qb.setTables(TABLE_AIXINTERVALDATAFORECASTS);
			qb.appendWhere(BaseColumns._ID + '=' + uri.getPathSegments().get(1));
			break;
		}
		case AIXSUNMOONDATA: {
			qb.setTables(TABLE_AIXSUNMOONDATA);
			break;
		}
		case AIXSUNMOONDATA_ID: {
			qb.setTables(TABLE_AIXSUNMOONDATA);
			qb.appendWhere(BaseColumns._ID + '=' + uri.getPathSegments().get(1));
			break;
		}
		}

		return qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		if (LOGD) Log.d(TAG, "update() with uri=" + uri);
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		
		switch (sUriMatcher.match(uri)) {
		case AIXWIDGETS_ID: {
			return db.update(TABLE_AIXWIDGETS, values, buildSelectionStringFromIdUri(uri, selection), selectionArgs);
		}
		case AIXWIDGETS_ID_SETTINGS: {
			return db.update(TABLE_AIXWIDGETSETTINGS, values, buildSelectionString(AixSettingsColumns.ROW_ID, uri.getPathSegments().get(1), selection), selectionArgs);
		}
		case AIXWIDGETSETTINGS_ID: {
			return db.update(TABLE_AIXWIDGETSETTINGS, values, buildSelectionStringFromIdUri(uri, selection), selectionArgs);
		}
		case AIXVIEWS_ID: {
			return db.update(TABLE_AIXVIEWS, values, buildSelectionStringFromIdUri(uri, selection), selectionArgs);
		}
		case AIXVIEWS_ID_SETTINGS: {
			return db.update(TABLE_AIXVIEWSETTINGS, values, buildSelectionString(AixSettingsColumns.ROW_ID, uri.getPathSegments().get(1), selection), selectionArgs);
		}
		case AIXVIEWS_LOCATION: {
			long locationId = findLocationFromView(db, uri);
			
			if (locationId != -1) {
				return db.update(TABLE_AIXLOCATIONS, values, buildSelectionStringFromId(locationId, selection), selectionArgs);
			} else {
				if (LOGD) Log.d(TAG, "update() with uri=" + uri + " failed. No location in view!");
				return 0; // Could not properly service request, as no location was found.
			}
		}
		case AIXVIEWSETTINGS_ID: {
			return db.update(TABLE_AIXVIEWSETTINGS, values, buildSelectionStringFromIdUri(uri, selection), selectionArgs);
		}
		case AIXLOCATIONS_ID: {
			return db.update(TABLE_AIXLOCATIONS, values, buildSelectionStringFromIdUri(uri, selection), selectionArgs);
		}
		case AIXPOINTDATAFORECASTS_ID: {
			return db.update(TABLE_AIXPOINTDATAFORECASTS, values, buildSelectionStringFromIdUri(uri, selection), selectionArgs);
		}
		case AIXINTERVALDATAFORECASTS_ID: {
			return db.update(TABLE_AIXINTERVALDATAFORECASTS, values, buildSelectionStringFromIdUri(uri, selection), selectionArgs);
		}
		case AIXSUNMOONDATA_ID: {
			return db.update(TABLE_AIXSUNMOONDATA, values, buildSelectionStringFromIdUri(uri, selection), selectionArgs);
		}
		default:
			if (LOGD) Log.d(TAG, "update() with uri=" + uri + " not matched");
			return 0;
		}
	}

	private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	
	private static final int AIXWIDGETS = 101;
	private static final int AIXWIDGETS_ID = 102;
	private static final int AIXWIDGETS_ID_SETTINGS = 103;
	
	private static final int AIXWIDGETSETTINGS = 201;
	private static final int AIXWIDGETSETTINGS_ID = 202;
	
	private static final int AIXVIEWS = 301;
	private static final int AIXVIEWS_ID = 302;
	private static final int AIXVIEWS_ID_SETTINGS = 303;
	private static final int AIXVIEWS_LOCATION = 304;
	private static final int AIXVIEWS_POINTDATAFORECASTS = 305;
	private static final int AIXVIEWS_INTERVALDATAFORECASTS = 306;
	private static final int AIXVIEWS_SUNMOONDATA = 307;
	
	private static final int AIXVIEWSETTINGS = 401;
	private static final int AIXVIEWSETTINGS_ID = 402;
	
	private static final int AIXLOCATIONS = 501;
	private static final int AIXLOCATIONS_ID = 502;
	
	private static final int AIXPOINTDATAFORECASTS = 601;
	private static final int AIXPOINTDATAFORECASTS_ID = 602;
	
	private static final int AIXINTERVALDATAFORECASTS = 701;
	private static final int AIXINTERVALDATAFORECASTS_ID = 702;
	
	private static final int AIXSUNMOONDATA = 801;
	private static final int AIXSUNMOONDATA_ID = 802;
	
	private static final int AIXRENDER = 999;
	
	static {
		sUriMatcher.addURI(AUTHORITY, "aixwidgets", AIXWIDGETS);
		sUriMatcher.addURI(AUTHORITY, "aixwidgets/#", AIXWIDGETS_ID);
		sUriMatcher.addURI(AUTHORITY, "aixwidgets/#/settings", AIXWIDGETS_ID_SETTINGS);
		
		sUriMatcher.addURI(AUTHORITY, "aixwidgetsettings", AIXWIDGETSETTINGS);
		sUriMatcher.addURI(AUTHORITY, "aixwidgetsettings/#", AIXWIDGETSETTINGS_ID);
		
		sUriMatcher.addURI(AUTHORITY, "aixviews", AIXVIEWS);
		sUriMatcher.addURI(AUTHORITY, "aixviews/#", AIXVIEWS_ID);
		sUriMatcher.addURI(AUTHORITY, "aixviews/#/settings", AIXVIEWS_ID_SETTINGS);
		sUriMatcher.addURI(AUTHORITY, "aixviews/#/location", AIXVIEWS_LOCATION);
		sUriMatcher.addURI(AUTHORITY, "aixviews/#/pointdata_forecasts", AIXVIEWS_POINTDATAFORECASTS);
		sUriMatcher.addURI(AUTHORITY, "aixviews/#/intervaldata_forecasts", AIXVIEWS_INTERVALDATAFORECASTS);
		sUriMatcher.addURI(AUTHORITY, "aixviews/#/sunmoondata", AIXVIEWS_SUNMOONDATA);
		
		sUriMatcher.addURI(AUTHORITY, "aixviewsettings", AIXVIEWSETTINGS);
		sUriMatcher.addURI(AUTHORITY, "aixviewsettings/#", AIXVIEWSETTINGS_ID);
		
		sUriMatcher.addURI(AUTHORITY, "aixlocations", AIXLOCATIONS);
		sUriMatcher.addURI(AUTHORITY, "aixlocations/#", AIXLOCATIONS_ID);
		
		sUriMatcher.addURI(AUTHORITY, "aixpointdataforecasts", AIXPOINTDATAFORECASTS);
		sUriMatcher.addURI(AUTHORITY, "aixpointdataforecasts/#", AIXPOINTDATAFORECASTS_ID);
		
		sUriMatcher.addURI(AUTHORITY, "aixintervaldataforecasts", AIXINTERVALDATAFORECASTS);
		sUriMatcher.addURI(AUTHORITY, "aixintervaldataforecasts/#", AIXINTERVALDATAFORECASTS_ID);
		
		sUriMatcher.addURI(AUTHORITY, "aixsunmoondata", AIXSUNMOONDATA);
		sUriMatcher.addURI(AUTHORITY, "aixsunmoondata/#", AIXSUNMOONDATA_ID);
		
		sUriMatcher.addURI(AUTHORITY, "aixrender/#/#/*", AIXRENDER);
	}
	
//	public Uri addForecast(SQLiteDatabase db, ContentValues values) {
//		Uri resultUri = null;
//		long rowId = -1;
//		
//		if (	values.containsKey(AixForecastsColumns.SUN_RISE) ||
//				values.containsKey(AixForecastsColumns.SUN_SET))
//		{
//			// Sun stuff
//			Cursor cursor = db.query(
//					TABLE_AIXFORECASTS,
//					null,
//					AixForecastsColumns.LOCATION + "=?" + " AND " +
//					AixForecastsColumns.TIME_FROM + "=?" + " AND " +
//					AixForecastsColumns.TIME_TO + "=?" + " AND " +
//					AixForecastsColumns.SUN_RISE + " IS NOT NULL AND " +
//					AixForecastsColumns.SUN_SET + " IS NOT NULL",
//					new String[] {
//							values.getAsString(AixForecastsColumns.LOCATION),
//							values.getAsString(AixForecastsColumns.TIME_FROM),
//							values.getAsString(AixForecastsColumns.TIME_TO)
//					}, null, null, null);
//			if (cursor != null) {
//				if (cursor.moveToFirst()) {
//					rowId = cursor.getLong(AixForecastsColumns.FORECAST_ID_COLUMN);
//				}
//				cursor.close();
//			}
//		} else {
//			Cursor cursor = db.query(
//					TABLE_AIXFORECASTS,
//					null,
//					AixForecastsColumns.LOCATION + "=?" + " AND " +
//					AixForecastsColumns.TIME_FROM + "=?" + " AND " +
//					AixForecastsColumns.TIME_TO + "=?" + " AND " +
//					AixForecastsColumns.SUN_RISE + " IS NULL AND " +
//					AixForecastsColumns.SUN_SET + " IS NULL",
//					new String[] {
//							values.getAsString(AixForecastsColumns.LOCATION),
//							values.getAsString(AixForecastsColumns.TIME_FROM),
//							values.getAsString(AixForecastsColumns.TIME_TO)
//					}, null, null, null);
//			if (cursor != null) {
//				if (cursor.moveToFirst()) {
//					rowId = cursor.getLong(AixForecastsColumns.FORECAST_ID_COLUMN);
//				}
//				cursor.close();
//			}
//		}
//		
//		if (rowId != -1) {
//			// A record already exists for this time and location. Update its values:
//			values.put(BaseColumns._ID, Long.toString(rowId));
//			db.replace(TABLE_AIXFORECASTS, null, values);
//		} else {
//			// No record exists for this time. Insert new row:
//			rowId = db.insert(TABLE_AIXFORECASTS, AixForecastsColumns.LOCATION, values);
//		}
//		
//		if (rowId != -1) {
//			resultUri = ContentUris.withAppendedId(AixForecasts.CONTENT_URI, rowId);
//		}
//		
//		return resultUri;
//	}
	
	private long addSetting(SQLiteDatabase db, String table, String id, Entry<String, Object> entry) {
		long rowId = -1;
		
		Cursor settingCursor = db.query(
				table, null,
				AixSettingsColumns.ROW_ID + '=' + id + " AND " +
				AixSettingsColumns.KEY + "='" + entry.getKey() + '\'',
				null, null, null, null);
		if (settingCursor != null) {
			if (settingCursor.moveToFirst()) {
				rowId = settingCursor.getLong(AixSettingsColumns.SETTING_ID_COLUMN);
			}
			settingCursor.close();
		}
		
		ContentValues values = new ContentValues();
		values.put(AixSettingsColumns.ROW_ID, id);
		values.put(AixSettingsColumns.KEY, entry.getKey());
		values.put(AixSettingsColumns.VALUE, (String)entry.getValue());
		
		if (rowId != -1) {
			values.put(BaseColumns._ID, rowId);
			rowId = db.replace(TABLE_AIXWIDGETSETTINGS, null, values);
		} else {
			rowId = db.insert(TABLE_AIXWIDGETSETTINGS, null, values);
		}
		
		return rowId;
	}
	
	private String[] buildPointDataProjection(Uri uri) {
		String temperatureUnit = uri.getQueryParameter("tu");
		String temperatureQuery = AixPointDataForecastColumns.TEMPERATURE;
		if (temperatureUnit != null && temperatureUnit.toLowerCase().equals("f")) {
			temperatureQuery = '(' + temperatureQuery + "*9.0/5.0+32)";
		}
		return new String[] {
				BaseColumns._ID,
				AixPointDataForecastColumns.LOCATION,
				AixPointDataForecastColumns.TIME_ADDED,
				AixPointDataForecastColumns.TIME,
				temperatureQuery,
				AixPointDataForecastColumns.HUMIDITY,
				AixPointDataForecastColumns.PRESSURE
		};
	}
	
	private String[] buildIntervalDataProjection(Uri uri) {
		String precipitationUnit = uri.getQueryParameter("pu");
		String rainQuery = AixIntervalDataForecastColumns.RAIN_VALUE;
		String rainMinQuery = AixIntervalDataForecastColumns.RAIN_MINVAL;
		String rainMaxQuery = AixIntervalDataForecastColumns.RAIN_MAXVAL;
		
		if (precipitationUnit != null && precipitationUnit.toLowerCase().equals("i")) {
			rainQuery = '(' + rainQuery + "/25.4)";
			rainMinQuery = '(' + rainMinQuery + "/25.4)";
			rainMaxQuery = '(' + rainMaxQuery + "/25.4)";
		}
		return new String[] {
				BaseColumns._ID,
				AixIntervalDataForecastColumns.LOCATION,
				AixIntervalDataForecastColumns.TIME_ADDED,
				AixIntervalDataForecastColumns.TIME_FROM,
				AixIntervalDataForecastColumns.TIME_TO,
				rainQuery, rainMinQuery, rainMaxQuery,
				AixIntervalDataForecastColumns.WEATHER_ICON
		};
	}
	
	private String buildSelectionString(String property, String value, String selection) {
		StringBuilder where = new StringBuilder();
		where.append(property);
		where.append('=');
		where.append(value);
		if (!TextUtils.isEmpty(selection)) {
			where.append(" AND (");
			where.append(selection);
			where.append(')');
		}
		return where.toString();
	}
	
	private String buildSelectionStringFromId(long id, String selection) {
		StringBuilder where = new StringBuilder();
		where.append(BaseColumns._ID);
		where.append('=');
		where.append(id);
		if (!TextUtils.isEmpty(selection)) {
			where.append(" AND (");
			where.append(selection);
			where.append(')');
		}
		return where.toString();
	}
	
	private String buildSelectionStringFromIdUri(Uri uri, String selection) {
		StringBuilder where = new StringBuilder();
		where.append(BaseColumns._ID);
		where.append('=');
		where.append(uri.getPathSegments().get(1));
		if (!TextUtils.isEmpty(selection)) {
			where.append(" AND (");
			where.append(selection);
			where.append(')');
		}
		return where.toString();
	}
	
	private long findLocationFromView(SQLiteDatabase db, Uri viewUri) {
		long locationId = -1;
		
		Cursor cursor = db.query(
				TABLE_AIXVIEWS,
				null,
				BaseColumns._ID + '=' + viewUri.getPathSegments().get(1),
				null, null, null, null);
		
		if (cursor != null) {
			if (cursor.moveToFirst()) {
				locationId = cursor.getLong(AixViewsColumns.LOCATION_COLUMN);
			}
			cursor.close();
		}
		
		return locationId;
	}

}
