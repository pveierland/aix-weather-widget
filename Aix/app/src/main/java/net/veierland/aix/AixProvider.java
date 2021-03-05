package net.veierland.aix;


import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map.Entry;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.BaseColumns;
import androidx.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

public class AixProvider extends ContentProvider {

	private static final String TAG = "AixProvider";
	private static final boolean LOGD = false;
	
	public static final String AUTHORITY = "net.veierland.aix";
	
	public interface AixWidgetsColumns {
		public static final String APPWIDGET_ID = BaseColumns._ID;
		
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

		public static final String[] ALL_COLUMNS = new String[] {
				APPWIDGET_ID, SIZE, VIEWS };
	}
	
	public static class AixWidgets implements BaseColumns, AixWidgetsColumns {
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/aixwidgets");
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/aixwidget";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/aixwidget";
		
		public static final String TWIG_SETTINGS = "settings";
	}
	
	public interface AixViewsColumns {
		public static final String VIEW_ID = BaseColumns._ID;
		/* The location row ID in the AixLocations table for the view location */
		public static final String LOCATION = "location";
		
		/* The type of a specific view, e.g. detailed or long-term */
		public static final String TYPE = "type";
		public static final int TYPE_DETAILED = 1;
		
		public static final int VIEW_ID_COLUMN = 0;
		public static final int LOCATION_COLUMN = 1;
		public static final int TYPE_COLUMN = 2;

		public static final String[] ALL_COLUMNS = new String[] {
				VIEW_ID, LOCATION, TYPE };
	}
	
	public static class AixViews implements BaseColumns, AixViewsColumns {
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/aixviews");
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/aixview";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/aixview";
		
		public static final String TWIG_SETTINGS = "settings";
		public static final String TWIG_LOCATION = "location";
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

		public static final String[] ALL_COLUMNS = new String[] {
				BaseColumns._ID,
				TITLE,
				TITLE_DETAILED,
				TIME_ZONE,
				TYPE,
				TIME_OF_LAST_FIX,
				LATITUDE,
				LONGITUDE,
				LAST_FORECAST_UPDATE,
				FORECAST_VALID_TO,
				NEXT_FORECAST_UPDATE };
	}
	
	public static class AixLocations implements BaseColumns, AixLocationsColumns {
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/aixlocations");
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/aixlocation";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/aixlocation";
		
		public static final String TWIG_POINTDATAFORECASTS = "pointdata_forecasts";
		public static final String TWIG_INTERVALDATAFORECASTS = "intervaldata_forecasts";
		public static final String TWIG_SUNMOONDATA = "sunmoondata";
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

		public static final String[] ALL_COLUMNS = new String[] {
				BaseColumns._ID, LOCATION, TIME_ADDED, TIME, TEMPERATURE, HUMIDITY, PRESSURE };
	}
	
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

		public static final String[] ALL_COLUMNS = new String[] {
				BaseColumns._ID, LOCATION, TIME_ADDED, TIME_FROM, TIME_TO, RAIN_VALUE, RAIN_MINVAL, RAIN_MAXVAL, WEATHER_ICON };
	}
	
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

		public static final String[] ALL_COLUMNS = new String[] {
				BaseColumns._ID,
				LOCATION,
				TIME_ADDED,
				DATE,
				SUN_RISE,
				SUN_SET,
				MOON_RISE,
				MOON_SET,
				MOON_PHASE
		};
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

		public static final String[] ALL_COLUMNS = new String[] {
				BaseColumns._ID, ROW_ID, KEY, VALUE };
	}
	
	public static abstract class AixSettings implements BaseColumns, AixSettingsColumns {
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/aixsetting";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/aixsetting";
	}
	
	public static class AixWidgetSettingsDatabase extends AixSettings {
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
	
	public static final String AIX_RENDER_FORMATTER = "content://net.veierland.aix/aixrender/%d/%d/%s";
	
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

            db.execSQL("DROP TABLE IF EXISTS " + TABLE_AIXWIDGETS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_AIXVIEWS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_AIXLOCATIONS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_AIXFORECASTS);

            onCreate(db);

			// All forecast data has been deleted.
            ContentValues values = new ContentValues();
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
			count = db.delete(TABLE_AIXWIDGETS, null, null);
			break;
		}
		case AIXWIDGETS_ID: {
			count = db.delete(
			        TABLE_AIXWIDGETS,
			        BaseColumns._ID + "=?",
			        new String[] { uri.getPathSegments().get(1) });
			break;
		}
        case AIXWIDGETS_ID_SETTINGS: {
            count = db.delete(
                    TABLE_AIXWIDGETSETTINGS,
                    AixSettingsColumns.ROW_ID + "=?",
                    new String[] { uri.getPathSegments().get(1) });
            break;
        }
        case AIXWIDGETSETTINGS: {
			count = db.delete(TABLE_AIXWIDGETSETTINGS, null, null);
			break;
		}
		case AIXWIDGETSETTINGS_ID: {
			count = db.delete(
			        TABLE_AIXWIDGETSETTINGS,
                    BaseColumns._ID + "=?",
                    new String[] { uri.getPathSegments().get(1) });
			break;
		}
		case AIXVIEWS: {
			count = db.delete(TABLE_AIXVIEWS, null, null);
			break;
		}
		case AIXVIEWS_ID: {
			count = db.delete(
			        TABLE_AIXVIEWS,
			        BaseColumns._ID + "=?",
			        new String[] { uri.getPathSegments().get(1) });
			break;
		}
		case AIXVIEWS_ID_SETTINGS: {
		    count = db.delete(
                    TABLE_AIXVIEWSETTINGS,
                    AixSettingsColumns.ROW_ID + "=?",
                    new String[] { uri.getPathSegments().get(1) });
		    break;
		}
		case AIXVIEWSETTINGS: {
			count = db.delete(TABLE_AIXVIEWSETTINGS, null, null);
			break;
		}
		case AIXVIEWSETTINGS_ID: {
			count = db.delete(
			        TABLE_AIXVIEWSETTINGS,
                    BaseColumns._ID + "=?",
                    new String[] { uri.getPathSegments().get(1) });
			break;
		}
		case AIXLOCATIONS: {
			count = db.delete(TABLE_AIXLOCATIONS, null, null);
			break;
		}
		case AIXLOCATIONS_ID: {
			count = db.delete(
			        TABLE_AIXLOCATIONS,
                    BaseColumns._ID + "=?",
                    new String[] { uri.getPathSegments().get(1) });
			break;
		}
		case AIXLOCATIONS_POINTDATAFORECASTS: {
			count = db.delete(
			        TABLE_AIXPOINTDATAFORECASTS,
                    AixPointDataForecastColumns.LOCATION + "=?",
                    new String[] { uri.getPathSegments().get(1) });
			break;
		}
		case AIXLOCATIONS_INTERVALDATAFORECASTS: {
			count = db.delete(
			        TABLE_AIXINTERVALDATAFORECASTS,
                    AixIntervalDataForecastColumns.LOCATION + "=?",
                    new String[] { uri.getPathSegments().get(1) });
			break;
		}
		case AIXLOCATIONS_SUNMOONDATA: {
			count = db.delete(
			        TABLE_AIXSUNMOONDATA,
                    AixSunMoonDataColumns.LOCATION + "=?",
                    new String[] { uri.getPathSegments().get(1) });
			break;
		}
		case AIXPOINTDATAFORECASTS: {
			final String before = uri.getQueryParameter("before");
            final String after = uri.getQueryParameter("after");

			if (before != null) {
                count += db.delete(
                        TABLE_AIXPOINTDATAFORECASTS,
                        AixPointDataForecasts.TIME + "<?",
                        new String[] { before });
            } else if (after != null) {
			    count += db.delete(
			            TABLE_AIXPOINTDATAFORECASTS,
                        AixPointDataForecasts.TIME + ">=?",
                        new String[] { after });
            } else {
                count = db.delete(TABLE_AIXPOINTDATAFORECASTS, null, null);
            }

			break;
		}
		case AIXPOINTDATAFORECASTS_ID: {
			count = db.delete(
			        TABLE_AIXPOINTDATAFORECASTS,
                    BaseColumns._ID + "=?",
                    new String[] { uri.getPathSegments().get(1) });
			break;
		}
		case AIXINTERVALDATAFORECASTS: {
            final String before = uri.getQueryParameter("before");
            final String after = uri.getQueryParameter("after");

            if (before != null) {
                count += db.delete(
                        TABLE_AIXINTERVALDATAFORECASTS,
                        AixIntervalDataForecasts.TIME_TO + "<?",
                        new String[] { before });
            } else if (after != null) {
                count += db.delete(
                        TABLE_AIXINTERVALDATAFORECASTS,
                        AixIntervalDataForecasts.TIME_FROM + ">=?",
                        new String[] { after });
            } else {
                count = db.delete(TABLE_AIXINTERVALDATAFORECASTS, null, null);
            }

			break;
		}
		case AIXINTERVALDATAFORECASTS_ID: {
			count = db.delete(
			        TABLE_AIXINTERVALDATAFORECASTS,
			        BaseColumns._ID + "=?",
			        new String[] { uri.getPathSegments().get(1) });
			break;
		}
		case AIXSUNMOONDATA: {
            final String before = uri.getQueryParameter("before");
            final String after = uri.getQueryParameter("after");

            if (before != null) {
                count += db.delete(
                        TABLE_AIXSUNMOONDATA,
                        AixSunMoonData.DATE + "<?",
                        new String[] { before });
            } else if (after != null) {
                count += db.delete(
                        TABLE_AIXSUNMOONDATA,
                        AixSunMoonData.DATE + ">=?",
                        new String[] { after });
            } else {
                count = db.delete(TABLE_AIXSUNMOONDATA, null, null);
            }

			break;
		}
		case AIXSUNMOONDATA_ID: {
			count = db.delete(
			        TABLE_AIXSUNMOONDATA,
			        BaseColumns._ID + "=?",
			        new String[] { uri.getPathSegments().get(1) });
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
			return AixWidgetSettingsDatabase.CONTENT_TYPE;
		case AIXWIDGETSETTINGS:
			return AixWidgetSettingsDatabase.CONTENT_TYPE;
		case AIXWIDGETSETTINGS_ID:
			return AixWidgetSettingsDatabase.CONTENT_ITEM_TYPE;
		case AIXVIEWS:
			return AixViews.CONTENT_TYPE;
		case AIXVIEWS_ID:
			return AixViews.CONTENT_ITEM_TYPE;
		case AIXVIEWS_ID_SETTINGS:
			return AixViewSettings.CONTENT_TYPE;
		case AIXVIEWS_LOCATION:
			return AixLocations.CONTENT_ITEM_TYPE;
		case AIXVIEWSETTINGS:
			return AixViewSettings.CONTENT_TYPE;
		case AIXVIEWSETTINGS_ID:
			return AixViewSettings.CONTENT_ITEM_TYPE;
		case AIXLOCATIONS:
			return AixLocations.CONTENT_TYPE;
		case AIXLOCATIONS_ID:
			return AixLocations.CONTENT_ITEM_TYPE;
		case AIXLOCATIONS_POINTDATAFORECASTS:
			return AixPointDataForecasts.CONTENT_ITEM_TYPE;
		case AIXLOCATIONS_INTERVALDATAFORECASTS:
			return AixIntervalDataForecasts.CONTENT_ITEM_TYPE;
		case AIXLOCATIONS_SUNMOONDATA:
			return AixSunMoonData.CONTENT_ITEM_TYPE;
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
		case AIXRENDER:
			return " ";
		}
		throw new IllegalStateException();
	}
	
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		if (LOGD) Log.d(TAG, "insert() with uri=" + uri);
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		
		Uri resultUri = null;
		
		switch (sUriMatcher.match(uri)) {
		case AIXWIDGETS: {
			long rowId = db.replace(TABLE_AIXWIDGETS, null, values);
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
				resultUri = AixWidgetSettingsDatabase.CONTENT_URI;
			}
			break;
		}
		case AIXWIDGETSETTINGS: {
			long rowId = db.insert(TABLE_AIXWIDGETSETTINGS, null, values);
			if (rowId != -1) {
				resultUri = ContentUris.withAppendedId(AixWidgetSettingsDatabase.CONTENT_URI, rowId);
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
			long rowId = db.insert(TABLE_AIXPOINTDATAFORECASTS, null, values);
			
			if (rowId != -1) {
				resultUri = ContentUris.withAppendedId(AixPointDataForecasts.CONTENT_URI, rowId);
			}

			break;
		}
		case AIXINTERVALDATAFORECASTS: {
			long rowId = db.insert(TABLE_AIXINTERVALDATAFORECASTS, null, values);
			
			if (rowId != -1) {
				resultUri = ContentUris.withAppendedId(AixIntervalDataForecasts.CONTENT_URI, rowId);
			}

			break;
		}
		case AIXSUNMOONDATA: {
			long rowId = -1;

            SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
            qb.setStrict(true);
            qb.setTables(TABLE_AIXSUNMOONDATA);

            Cursor cursor = qb.query(db,
                    new String[] { BaseColumns._ID },
                    AixSunMoonDataColumns.LOCATION + "=? AND " + AixSunMoonDataColumns.DATE + "=?",
                    new String[] {
                            values.getAsString(AixSunMoonDataColumns.LOCATION),
                            values.getAsString(AixSunMoonDataColumns.DATE)
                    }, null, null, null);

			if (cursor != null) {
				if (cursor.moveToFirst()) {
					rowId = cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID));
				}
				cursor.close();
			}
			
			if (rowId != -1) {
				// A record already exists for this time and location. Update its values:
				db.update(TABLE_AIXSUNMOONDATA, values, BaseColumns._ID + "=?", new String[] { Long.toString(rowId) });
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
	public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode)
			throws FileNotFoundException
	{
		if (sUriMatcher.match(uri) != AIXRENDER)
		{
			throw new FileNotFoundException("Uri does not follow AixRender format. (uri=" + uri + ")");
		}
		
		List<String> pathSegments = uri.getPathSegments();
		if (pathSegments == null || pathSegments.size() != 4)
		{
			throw new FileNotFoundException();
		}
		
		String appWidgetIdString = pathSegments.get(1);
		String updateTimeString = pathSegments.get(2);
		
		int appWidgetId = -1;
		long updateTime = -1;
		
		try
		{
			appWidgetId = Integer.parseInt(appWidgetIdString);
			updateTime = Long.parseLong(updateTimeString);
			
			if (appWidgetId == -1 || updateTime == -1)
			{
				throw new NumberFormatException();
			}
		}
		catch (NumberFormatException e)
		{
			String errorMessage = String.format(
					"Invalid arguments (appWidgetIdString=%s,updateTimeString=%s)",
					appWidgetIdString, updateTimeString);
			Log.d(TAG, "openFile(): " + errorMessage);
			throw new FileNotFoundException(errorMessage);
		}
		
		String orientation = pathSegments.get(3);
		if (orientation == null || !(orientation.equals("portrait") || orientation.equals("landscape")))
		{
			String errorMessage = "Invalid orientation parameter. (orientation=" + orientation + ")";
			Log.d(TAG, "openFile(): " + errorMessage);
			throw new FileNotFoundException();
		}
		
		Context context = getContext();

		if (context != null) {
			AixUtils.deleteTemporaryFile(context, appWidgetId, updateTime, orientation);
			String fileName = context.getString(R.string.bufferImageFileName, appWidgetId, updateTime, orientation);

			File file = new File(context.getFilesDir(), fileName);
			return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
		} else {
			throw new FileNotFoundException("could not open file: " + uri);
		}
	}
	
	@Override
	public Cursor query(@NonNull Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder)
	{
		if (LOGD) Log.d(TAG, "query() with uri=" + uri);

		String   qbTables        = null;
		String[] qbProjection    = null;
		String   qbSelection     = null;
		String[] qbSelectionArgs = null;
		String   qbSortOrder     = null;

		switch (sUriMatcher.match(uri)) {
		case AIXWIDGETS: {
			qbTables     = TABLE_AIXWIDGETS;
			qbProjection = AixWidgets.ALL_COLUMNS;
			break;
		}
		case AIXWIDGETS_ID: {
			qbTables        = TABLE_AIXWIDGETS;
			qbProjection    = AixWidgets.ALL_COLUMNS;
			qbSelection     = BaseColumns._ID + "=?";
			qbSelectionArgs = new String[] { uri.getPathSegments().get(1) };
			break;
		}
		case AIXWIDGETS_ID_SETTINGS: {
			qbTables        = TABLE_AIXWIDGETSETTINGS;
			qbProjection    = AixSettings.ALL_COLUMNS;
			qbSelection     = AixSettings.ROW_ID + "=?";
			qbSelectionArgs = new String[] { uri.getPathSegments().get(1) };
			break;
		}
		case AIXWIDGETSETTINGS: {
			qbTables     = TABLE_AIXWIDGETSETTINGS;
			qbProjection = AixSettings.ALL_COLUMNS;
			break;
		}
		case AIXWIDGETSETTINGS_ID: {
			qbTables        = TABLE_AIXWIDGETSETTINGS;
			qbProjection    = AixSettings.ALL_COLUMNS;
			qbSelection     = BaseColumns._ID + "=?";
			qbSelectionArgs = new String[] { uri.getPathSegments().get(1) };
			break;
		}
		case AIXVIEWS: {
			qbTables     = TABLE_AIXVIEWS;
			qbProjection = AixViews.ALL_COLUMNS;
			break;
		}
		case AIXVIEWS_ID: {
			qbTables        = TABLE_AIXVIEWS;
			qbProjection    = AixViews.ALL_COLUMNS;
			qbSelection     = BaseColumns._ID + "=?";
			qbSelectionArgs = new String[] { uri.getPathSegments().get(1) };
			break;
		}
		case AIXVIEWS_ID_SETTINGS: {
			qbTables        = TABLE_AIXVIEWSETTINGS;
			qbProjection    = AixSettings.ALL_COLUMNS;
			qbSelection     = AixSettings.ROW_ID + "=?";
			qbSelectionArgs = new String[] { uri.getPathSegments().get(1) };
			break;
		}
		case AIXVIEWSETTINGS: {
			qbTables     = TABLE_AIXVIEWSETTINGS;
			qbProjection = AixSettings.ALL_COLUMNS;
			break;
		}
		case AIXVIEWSETTINGS_ID: {
			qbTables        = TABLE_AIXVIEWSETTINGS;
			qbProjection    = AixSettings.ALL_COLUMNS;
			qbSelection     = BaseColumns._ID + "=?";
			qbSelectionArgs = new String[] { uri.getPathSegments().get(1) };
			break;
		}
		case AIXLOCATIONS: {
			qbTables     = TABLE_AIXLOCATIONS;
			qbProjection = AixLocations.ALL_COLUMNS;
			break;
		}
		case AIXLOCATIONS_ID: {
			qbTables        = TABLE_AIXLOCATIONS;
			qbProjection    = AixLocations.ALL_COLUMNS;
			qbSelection     = BaseColumns._ID + "=?";
			qbSelectionArgs = new String[] { uri.getPathSegments().get(1) };
			break;
		}
		case AIXLOCATIONS_POINTDATAFORECASTS: {
			qbTables     = TABLE_AIXPOINTDATAFORECASTS;
			qbProjection = AixPointDataForecasts.ALL_COLUMNS;

			final String locationId = uri.getPathSegments().get(1);
			final String start      = uri.getQueryParameter("start");
			final String end        = uri.getQueryParameter("end");

			if (start != null && end != null) {
				qbSelection = AixPointDataForecasts.LOCATION + "=? AND "
						+ AixPointDataForecasts.TIME + ">=? AND "
						+ AixPointDataForecasts.TIME + "<=?";
				qbSelectionArgs = new String[] { locationId, start, end };
				qbSortOrder     = AixPointDataForecasts.TIME + " ASC";
			} else {
				qbSelection     = AixPointDataForecasts.LOCATION + "=?";
				qbSelectionArgs = new String[] { locationId };
			}

			break;
		}
		case AIXLOCATIONS_INTERVALDATAFORECASTS: {
			qbTables     = TABLE_AIXINTERVALDATAFORECASTS;
			qbProjection = AixIntervalDataForecasts.ALL_COLUMNS;

			final String locationId = uri.getPathSegments().get(1);
			final String start      = uri.getQueryParameter("start");
			final String end        = uri.getQueryParameter("end");

			if (start != null && end != null) {
				qbSelection = AixIntervalDataForecasts.LOCATION + "=? AND "
						+ AixIntervalDataForecasts.TIME_TO + ">? AND "
						+ AixIntervalDataForecasts.TIME_FROM + "<?";
				qbSelectionArgs = new String[] { locationId, start, end };
				qbSortOrder     = '(' + AixIntervalDataForecasts.TIME_TO + '-' +
						AixIntervalDataForecasts.TIME_FROM + ") ASC," +
						AixIntervalDataForecasts.TIME_FROM + " ASC";
			}
			else
			{
				qbSelection     = AixIntervalDataForecasts.LOCATION + "=?";
				qbSelectionArgs = new String[] { locationId };
			}

			break;
		}
		case AIXLOCATIONS_SUNMOONDATA: {
			qbTables     = TABLE_AIXSUNMOONDATA;
			qbProjection = AixSunMoonData.ALL_COLUMNS;

            final String locationId = uri.getPathSegments().get(1);
            final String start      = uri.getQueryParameter("start");
            final String end        = uri.getQueryParameter("end");

            if (start != null && end != null) {
            	qbSelection = AixSunMoonData.LOCATION + "=? AND "
						+ AixSunMoonData.DATE + ">=? AND "
						+ AixSunMoonData.DATE + "<=?";
            	qbSelectionArgs = new String[] { locationId, start, end };
            	qbSortOrder     = AixSunMoonData.DATE + " ASC";
			}
			else
			{
				qbSelection = AixSunMoonData.LOCATION + "=?";
				qbSelectionArgs = new String[] { locationId };
			}

			break;
		}
		case AIXPOINTDATAFORECASTS: {
			qbTables     = TABLE_AIXPOINTDATAFORECASTS;
			qbProjection = AixPointDataForecasts.ALL_COLUMNS;
			break;
		}
		case AIXPOINTDATAFORECASTS_ID: {
			qbTables        = TABLE_AIXPOINTDATAFORECASTS;
			qbProjection    = AixPointDataForecasts.ALL_COLUMNS;
			qbSelection     = BaseColumns._ID + "=?";
			qbSelectionArgs = new String[] { uri.getPathSegments().get(1) };
			break;
		}
		case AIXINTERVALDATAFORECASTS: {
			qbTables     = TABLE_AIXINTERVALDATAFORECASTS;
			qbProjection = AixIntervalDataForecasts.ALL_COLUMNS;
			break;
		}
		case AIXINTERVALDATAFORECASTS_ID: {
			qbTables        = TABLE_AIXINTERVALDATAFORECASTS;
			qbProjection    = AixIntervalDataForecasts.ALL_COLUMNS;
			qbSelection     = BaseColumns._ID + "=?";
			qbSelectionArgs = new String[] { uri.getPathSegments().get(1) };
			break;
		}
		case AIXSUNMOONDATA: {
			qbTables     = TABLE_AIXSUNMOONDATA;
			qbProjection = AixSunMoonData.ALL_COLUMNS;
			break;
		}
		case AIXSUNMOONDATA_ID: {
			qbTables        = TABLE_AIXSUNMOONDATA;
			qbProjection    = AixSunMoonData.ALL_COLUMNS;
			qbSelection     = BaseColumns._ID + "=?";
			qbSelectionArgs = new String[] { uri.getPathSegments().get(1) };
			break;
		}
		}

		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setStrict(true);
		qb.setTables(qbTables);

		return qb.query(db, qbProjection, qbSelection, qbSelectionArgs, null, null, qbSortOrder);
	}


	@Override
	public int update(@NonNull Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		if (LOGD) Log.d(TAG, "update() with uri=" + uri);
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();

		switch (sUriMatcher.match(uri)) {
		case AIXWIDGETS_ID: {
			return db.update(
			        TABLE_AIXWIDGETS,
                    values,
                    BaseColumns._ID + "=?",
                    new String[] { uri.getPathSegments().get(1) });
		}
		case AIXWIDGETS_ID_SETTINGS: {
			return db.update(
			        TABLE_AIXWIDGETSETTINGS,
                    values,
                    AixSettingsColumns.ROW_ID + "=?",
                    new String[] { uri.getPathSegments().get(1) });
		}
		case AIXWIDGETSETTINGS_ID: {
			return db.update(
			        TABLE_AIXWIDGETSETTINGS,
                    values,
                    BaseColumns._ID + "=?",
                    new String[] { uri.getPathSegments().get(1) });
		}
		case AIXVIEWS_ID: {
			return db.update(
			        TABLE_AIXVIEWS,
                    values,
                    BaseColumns._ID + "=?",
                    new String[] { uri.getPathSegments().get(1) });
		}
		case AIXVIEWS_ID_SETTINGS: {
			return db.update(
			        TABLE_AIXVIEWSETTINGS,
                    values,
                    AixSettingsColumns.ROW_ID + "=?",
                    new String[] { uri.getPathSegments().get(1) });
		}
		case AIXVIEWS_LOCATION: {
			long locationId = findLocationFromView(db, uri);
			
			if (locationId != -1) {
				return db.update(
				        TABLE_AIXLOCATIONS,
                        values,
                        BaseColumns._ID + "=?",
                        new String[] { Long.toString(locationId) });
			} else {
				if (LOGD) Log.d(TAG, "update() with uri=" + uri + " failed. No location in view!");
				return 0; // Could not properly service request, as no location was found.
			}
		}
		case AIXVIEWSETTINGS_ID: {
			return db.update(
			        TABLE_AIXVIEWSETTINGS,
                    values,
                    BaseColumns._ID + "=?",
                    new String[] { uri.getPathSegments().get(1) });
		}
		case AIXLOCATIONS: {
			return db.update(TABLE_AIXLOCATIONS, values, null, null);
		}
		case AIXLOCATIONS_ID: {
			return db.update(
			        TABLE_AIXLOCATIONS,
                    values,
                    BaseColumns._ID + "=?",
			        new String[] { uri.getPathSegments().get(1) });
		}
		case AIXPOINTDATAFORECASTS: {
			return db.delete(TABLE_AIXPOINTDATAFORECASTS, "exists " +
					"(select 1 from aixpointdataforecasts x where " +
					"aixpointdataforecasts.location=x.location and " +
					"aixpointdataforecasts.time=x.time and " +
					"aixpointdataforecasts.timeAdded < x.timeAdded)",
					null);
		}
		case AIXPOINTDATAFORECASTS_ID: {
			return db.update(
			        TABLE_AIXPOINTDATAFORECASTS,
                    values,
                    BaseColumns._ID + "=?",
                    new String[] { uri.getPathSegments().get(1) });
		}
		case AIXINTERVALDATAFORECASTS: {
			return db.delete(TABLE_AIXINTERVALDATAFORECASTS, "exists " +
					"(select 1 from aixintervaldataforecasts x where " +
					"aixintervaldataforecasts.location=x.location and " +
					"aixintervaldataforecasts.timeFrom=x.timeFrom and " +
					"aixintervaldataforecasts.timeTo=x.timeTo and " +
					"aixintervaldataforecasts.timeAdded < x.timeAdded);", null);
		}
		case AIXINTERVALDATAFORECASTS_ID: {
			return db.update(
			        TABLE_AIXINTERVALDATAFORECASTS,
                    values,
                    BaseColumns._ID + "=?",
                    new String[] { uri.getPathSegments().get(1) });
		}
		case AIXSUNMOONDATA_ID: {
			return db.update(
			        TABLE_AIXSUNMOONDATA,
                    values,
                    BaseColumns._ID + "=?",
                    new String[] { uri.getPathSegments().get(1) });
		}
		default:
			if (LOGD) Log.d(TAG, "update() with uri=" + uri + " not matched");
			return 0;
		}
	}

	@Override
	public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
		if (LOGD) Log.d(TAG, "bulkInsert() with uri=" + uri);
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		
		final int match = sUriMatcher.match(uri);
		int numInserted = 0;
		
		switch (match)
		{
		case AIXPOINTDATAFORECASTS:
			try
			{
				db.beginTransaction();
				numInserted = bulkInsertPointData(db, values);
				db.setTransactionSuccessful();
			} finally {
				db.endTransaction();
			}
			break;
		case AIXINTERVALDATAFORECASTS:
			try
			{
				db.beginTransaction();
				numInserted = bulkInsertIntervalData(db, values);
				db.setTransactionSuccessful();
			} finally {
				db.endTransaction();
			}
			break;
		case AIXWIDGETS_ID_SETTINGS:
			String widgetId = uri.getPathSegments().get(1);
			try {
				db.beginTransaction();
				bulkInsertSettings(db, values, widgetId);
				db.setTransactionSuccessful();
			} finally {
				db.endTransaction();
			}
			break;
		default:
			throw new UnsupportedOperationException("AixProvider.bulkInsert() Unsupported URI: " + uri); 
		}

		return numInserted;
	}
	
	private void bulkInsertSettings(SQLiteDatabase db, ContentValues[] values, String appWidgetId)
	{
		SQLiteStatement updateStatement =
				db.compileStatement("UPDATE " + TABLE_AIXWIDGETSETTINGS
						+ " SET " + AixSettingsColumns.VALUE + "=?"
						+ " WHERE " + AixSettingsColumns.ROW_ID + "=?"
							+ " AND " + AixSettingsColumns.KEY + "=?");
		
		SQLiteStatement insertStatement =
				db.compileStatement("INSERT INTO " + TABLE_AIXWIDGETSETTINGS + "("
						+ AixSettingsColumns.ROW_ID + ","
						+ AixSettingsColumns.KEY + ","
						+ AixSettingsColumns.VALUE + ") "
						+ "VALUES (?,?,?)");

		for (ContentValues value: values)
		{
			String settingKey = value.getAsString(AixWidgetSettingsDatabase.KEY);
			String settingValue = value.getAsString(AixWidgetSettingsDatabase.VALUE);
			
			if (!TextUtils.isEmpty(settingKey))
			{
				long existingRowId = checkForSetting(db, appWidgetId, settingKey);
				
				if (existingRowId != -1)
				{
					// Update existing row
					if (settingValue == null) {
						updateStatement.bindNull(1);
					} else {
						updateStatement.bindString(1, settingValue);
					}
					updateStatement.bindString(2, appWidgetId);
					updateStatement.bindString(3, settingKey);
					updateStatement.execute();
				}
				else
				{
					// Insert new row
					insertStatement.bindString(1, appWidgetId);
					insertStatement.bindString(2, settingKey);
					
					if (settingValue == null) {
						insertStatement.bindNull(3);
					} else {
						insertStatement.bindString(3, settingValue);
					}
					
					insertStatement.execute();
				}
			}
		}
	}
	
	private long checkForSetting(SQLiteDatabase db, String appWidgetId, String key)
	{
		long rowId = -1;

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setStrict(true);
        qb.setTables(TABLE_AIXWIDGETSETTINGS);
		
		Cursor cursor = qb.query(db,
				new String[] { BaseColumns._ID },
				AixSettingsColumns.ROW_ID + "=? AND " + AixSettingsColumns.KEY + "=?",
				new String[] { appWidgetId, key },
				null, null, null);
		
		if (cursor != null)
		{
			try {
				if (cursor.moveToFirst())
				{
					rowId = cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID));
				}
			}
			finally
			{
				cursor.close();
			}
		}
		
		return rowId;
	}
	
	private int bulkInsertIntervalData(SQLiteDatabase db, ContentValues[] values)
	{
		int numInserted = 0;
		
		SQLiteStatement insert =
				db.compileStatement("INSERT INTO " + TABLE_AIXINTERVALDATAFORECASTS + "("
						+ AixIntervalDataForecasts.LOCATION + ","
						+ AixIntervalDataForecasts.TIME_ADDED + ","
						+ AixIntervalDataForecasts.TIME_FROM + ","
						+ AixIntervalDataForecasts.TIME_TO + ","
						+ AixIntervalDataForecasts.RAIN_VALUE + ","
						+ AixIntervalDataForecasts.RAIN_MINVAL + ","
						+ AixIntervalDataForecasts.RAIN_MAXVAL + ","
						+ AixIntervalDataForecasts.WEATHER_ICON + ") "
						+ "VALUES (?,?,?,?,?,?,?,?)");
		
		for (ContentValues value : values)
		{
			Long location = value.getAsLong(AixIntervalDataForecasts.LOCATION);
			Long timeAdded = value.getAsLong(AixIntervalDataForecasts.TIME_ADDED);
			Long timeFrom = value.getAsLong(AixIntervalDataForecasts.TIME_FROM);
			Long timeTo = value.getAsLong(AixIntervalDataForecasts.TIME_TO);
			
			if (location != null && timeAdded != null && timeFrom != null && timeTo != null)
			{
				insert.bindLong(1, location);
				insert.bindLong(2, timeAdded);
				insert.bindLong(3, timeFrom);
				insert.bindLong(4, timeTo);
				
				Double rainValue = value.getAsDouble(AixIntervalDataForecasts.RAIN_VALUE);
				if (rainValue != null) {
					insert.bindDouble(5, rainValue);
				} else {
					insert.bindNull(5);
				}
				
				Double rainMinValue = value.getAsDouble(AixIntervalDataForecasts.RAIN_MINVAL);
				if (rainMinValue != null) {
					insert.bindDouble(6, rainMinValue);
				} else {
					insert.bindNull(6);
				}
				
				Double rainMaxValue = value.getAsDouble(AixIntervalDataForecasts.RAIN_MAXVAL);
				if (rainMaxValue != null) {
					insert.bindDouble(7, rainMaxValue);
				} else {
					insert.bindNull(7);
				}
				
				Long weatherIcon = value.getAsLong(AixIntervalDataForecasts.WEATHER_ICON);
				if (weatherIcon != null) {
					insert.bindDouble(8, weatherIcon);
				} else {
					insert.bindNull(8);
				}
				
				insert.execute();
				numInserted++;
			}
		}
		
		return numInserted;
	}
	
	private int bulkInsertPointData(SQLiteDatabase db, ContentValues[] values)
	{
		int numInserted = 0;
		
		SQLiteStatement insert =
				db.compileStatement("INSERT INTO " + TABLE_AIXPOINTDATAFORECASTS + "("
						+ AixPointDataForecasts.LOCATION + ","
						+ AixPointDataForecasts.TIME_ADDED + ","
						+ AixPointDataForecasts.TIME + ","
						+ AixPointDataForecasts.TEMPERATURE + ","
						+ AixPointDataForecasts.HUMIDITY + ","
						+ AixPointDataForecasts.PRESSURE + ") "
						+ "VALUES (?,?,?,?,?,?)");
		
		for (ContentValues value : values)
		{
			Long location = value.getAsLong(AixPointDataForecasts.LOCATION);
			Long timeAdded = value.getAsLong(AixPointDataForecasts.TIME_ADDED);
			Long time = value.getAsLong(AixPointDataForecasts.TIME);
			
			if (location != null && timeAdded != null && time != null)
			{
				insert.bindLong(1, location);
				insert.bindLong(2, timeAdded);
				insert.bindLong(3, time);
				
				Double temperature = value.getAsDouble(AixPointDataForecasts.TEMPERATURE);
				if (temperature != null) {
					insert.bindDouble(4, temperature);
				} else {
					insert.bindNull(4);
				}
				
				Double humidity = value.getAsDouble(AixPointDataForecasts.HUMIDITY);
				if (humidity != null) {
					insert.bindDouble(5, humidity);
				} else {
					insert.bindNull(5);
				}
				
				Double pressure = value.getAsDouble(AixPointDataForecasts.PRESSURE);
				if (pressure != null) {
					insert.bindDouble(6, pressure);
				} else {
					insert.bindNull(6);
				}
				
				insert.execute();
				numInserted++;
			}
		}
		
		return numInserted;
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
	//private static final int AIXVIEWS_POINTDATAFORECASTS = 305;
	//private static final int AIXVIEWS_INTERVALDATAFORECASTS = 306;
	//private static final int AIXVIEWS_SUNMOONDATA = 307;
	
	private static final int AIXVIEWSETTINGS = 401;
	private static final int AIXVIEWSETTINGS_ID = 402;
	
	private static final int AIXLOCATIONS = 501;
	private static final int AIXLOCATIONS_ID = 502;
	private static final int AIXLOCATIONS_POINTDATAFORECASTS = 503;
	private static final int AIXLOCATIONS_INTERVALDATAFORECASTS = 504;
	private static final int AIXLOCATIONS_SUNMOONDATA = 505;
	
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
//		sUriMatcher.addURI(AUTHORITY, "aixviews/#/pointdata_forecasts", AIXVIEWS_POINTDATAFORECASTS);
//		sUriMatcher.addURI(AUTHORITY, "aixviews/#/intervaldata_forecasts", AIXVIEWS_INTERVALDATAFORECASTS);
//		sUriMatcher.addURI(AUTHORITY, "aixviews/#/sunmoondata", AIXVIEWS_SUNMOONDATA);
		
		sUriMatcher.addURI(AUTHORITY, "aixviewsettings", AIXVIEWSETTINGS);
		sUriMatcher.addURI(AUTHORITY, "aixviewsettings/#", AIXVIEWSETTINGS_ID);
		
		sUriMatcher.addURI(AUTHORITY, "aixlocations", AIXLOCATIONS);
		sUriMatcher.addURI(AUTHORITY, "aixlocations/#", AIXLOCATIONS_ID);
		sUriMatcher.addURI(AUTHORITY, "aixlocations/#/pointdata_forecasts", AIXLOCATIONS_POINTDATAFORECASTS);
		sUriMatcher.addURI(AUTHORITY, "aixlocations/#/intervaldata_forecasts", AIXLOCATIONS_INTERVALDATAFORECASTS);
		sUriMatcher.addURI(AUTHORITY, "aixlocations/#/sunmoondata", AIXLOCATIONS_SUNMOONDATA);
		
		sUriMatcher.addURI(AUTHORITY, "aixpointdataforecasts", AIXPOINTDATAFORECASTS);
		sUriMatcher.addURI(AUTHORITY, "aixpointdataforecasts/#", AIXPOINTDATAFORECASTS_ID);
		
		sUriMatcher.addURI(AUTHORITY, "aixintervaldataforecasts", AIXINTERVALDATAFORECASTS);
		sUriMatcher.addURI(AUTHORITY, "aixintervaldataforecasts/#", AIXINTERVALDATAFORECASTS_ID);
		
		sUriMatcher.addURI(AUTHORITY, "aixsunmoondata", AIXSUNMOONDATA);
		sUriMatcher.addURI(AUTHORITY, "aixsunmoondata/#", AIXSUNMOONDATA_ID);
		
		sUriMatcher.addURI(AUTHORITY, "aixrender/#/#/*", AIXRENDER);
	}
	
	private long findLocationFromView(SQLiteDatabase db, Uri viewUri) {
		long locationId = -1;

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setStrict(true);
        qb.setTables(TABLE_AIXVIEWS);
		
		Cursor cursor = qb.query(db,
				new String[] { AixViewsColumns.LOCATION },
				BaseColumns._ID + "=?",
				new String[] { viewUri.getPathSegments().get(1) },
				null, null, null);
		
		if (cursor != null) {
			if (cursor.moveToFirst()) {
				locationId = cursor.getLong(cursor.getColumnIndexOrThrow(AixViewsColumns.LOCATION));
			}
			cursor.close();
		}
		
		return locationId;
	}

	private long addSetting(SQLiteDatabase db, String table, String id, Entry<String, Object> entry) {
		long rowId = -1;

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setStrict(true);
        qb.setTables(table);

		Cursor cursor = qb.query(db,
				new String[] { BaseColumns._ID },
				AixSettingsColumns.ROW_ID + "=? AND " + AixSettingsColumns.KEY + "=?",
				new String[] { id, entry.getKey() },
				null, null, null);

		if (cursor != null) {
			if (cursor.moveToFirst()) {
				rowId = cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID));
			}
			cursor.close();
		}

		ContentValues values = new ContentValues();
		values.put(AixSettingsColumns.ROW_ID, id);
		values.put(AixSettingsColumns.KEY, entry.getKey());
		values.put(AixSettingsColumns.VALUE, (String) entry.getValue());

		if (rowId != -1) {
			values.put(BaseColumns._ID, rowId);
			rowId = db.replace(TABLE_AIXWIDGETSETTINGS, null, values);
		} else {
			rowId = db.insert(TABLE_AIXWIDGETSETTINGS, null, values);
		}

		return rowId;
	}
}
