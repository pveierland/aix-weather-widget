package net.veierland.aixd;

import java.util.Arrays;
import java.util.List;

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
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

public class AixProvider extends ContentProvider {

	private static final String TAG = "AixProvider";
	private static final boolean LOGD = false;
	
	public static final String AUTHORITY = "net.veierland.aixd";
	
	public interface AixWidgetsColumns {
		/* FUTURE: A specific title given to a widget */
		public static final String TITLE = "title";
		
		/* FUTURE: The size of the widget, e.g. 2x4 */
		public static final String SIZE = "size";
		
		/* A colon-separated array in string format of the view IDs linked to the widget */
		public static final String VIEWS = "views";

		public static final int APPWIDGET_ID_COLUMN = 0;
		public static final int TITLE_COLUMN = 1;
		public static final int SIZE_COLUMN = 2;
		public static final int VIEWS_COLUMN = 3;
	}
	
	public static class AixWidgets implements BaseColumns, AixWidgetsColumns {
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/aixwidgets");
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/aixwidget";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/aixwidget";
	}
	
	public interface AixViewsColumns {
		/* The title given a specific view, e.g. "Summer cabin" */
		public static final String TITLE = "title";
		
		/* The type of a specific view, e.g. long-term 6-hour resolution */
		public static final String TYPE = "type";
		
		/* The ID of the location of the forecast the view is showing */
		public static final String LOCATION = "location";
		
		/* The temperature units the view will use; Celsius or Fahrenheit */
		public static final String UNITS = "units";
		public static final int UNITS_CELSIUS = 1;
		public static final int UNITS_FAHRENHEIT = 2;
		
		/* The period of the view; 24 hours / 48 hours / 96 hours */
		public static final String PERIOD = "period";
		public static final int PERIOD_24_HOURS = 1;
		public static final int PERIOD_48_HOURS = 2;
		public static final int PERIOD_96_HOURS = 3;
		
		public static final int VIEWS_ID_COLUMN = 0;
		public static final int TITLE_COLUMN = 1;
		public static final int TYPE_COLUMN = 2;
		public static final int LOCATION_COLUMN = 3;
		public static final int UNITS_COLUMN = 4;
		public static final int PERIOD_COLUMN = 5;
	}
	
	public static class AixViews implements BaseColumns, AixViewsColumns {
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/aixviews");
		public static final String TWIG_LOCATION = "location";
		public static final String TWIG_FORECASTS = "forecasts";
		public static final String TWIG_FORECASTS_AT = "forecasts_at";
		public static final String TWIG_FORECASTS_FROM_TO = "forecasts_from_to";
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/aixview";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/aixview";
	}
	
	public interface AixLocationsColumns {
		/* The title of the location. This can be set statically or via reverse geolocation */
		public static final String TITLE = "title";
		public static final String TITLE_DETAILED = "title_detailed";
		
		public static final String TIME_ZONE = "timeZone";
		
		/* The type of the location; whether it is set statically or updated dynamically */
		public static final String TYPE = "type";
		public static final int LOCATION_STATIC = 1;
		public static final int LOCATION_DYNAMIC_PASSIVE = 2;
		public static final int LOCATION_DYNAMIC_ACTIVE = 3;
		
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
		
		public static final int LOCATIONS_ID_COLUMN = 0;
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
	
	public interface AixForecastsColumns {
		public static final String LOCATION = "location";
		public static final String TIME_FROM = "timeFrom";
		public static final String TIME_TO = "timeTo";
		public static final String TEMPERATURE = "temperature";
		public static final String RAIN_LOWVAL = "rainLowVal";
		public static final String RAIN_VALUE = "rainValue";
		public static final String RAIN_HIGHVAL = "rainHighVal";
		public static final String WEATHER_ICON = "weatherIcon";
		public static final String SUN_RISE = "sunRise";
		public static final String SUN_SET = "sunSet";
		
		public static final int FORECASTS_ID_COLUMN = 0;
		public static final int LOCATION_COLUMN = 1;
		public static final int TIME_FROM_COLUMN = 2;
		public static final int TIME_TO_COLUMN = 3;
		public static final int TEMPERATURE_COLUMN = 4;
		public static final int RAIN_LOWVAL_COLUMN = 5;
		public static final int RAIN_VALUE_COLUMN = 6;
		public static final int RAIN_HIGHVAL_COLUMN = 7;
		public static final int WEATHER_ICON_COLUMN = 8;
		public static final int SUN_RISE_COLUMN = 9;
		public static final int SUN_SET_COLUMN = 10;
	}
	
	public static class AixForecasts implements BaseColumns, AixForecastsColumns {
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/aixforecasts");
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/aixforecast";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/aixforecast";
	}
	
	private static final String TABLE_AIXWIDGETS = "aixwidgets";
	private static final String TABLE_AIXVIEWS = "aixviews";
	private static final String TABLE_AIXLOCATIONS = "aixlocations";
	private static final String TABLE_AIXFORECASTS = "aixforecasts";
	
	private static final String[] AIX_TABLES = {
		TABLE_AIXWIDGETS, TABLE_AIXVIEWS, TABLE_AIXLOCATIONS, TABLE_AIXFORECASTS
	};
	
	private DatabaseHelper mOpenHelper;
	
	private static class DatabaseHelper extends SQLiteOpenHelper {
		private static final String DATABASE_NAME = "aix_database.db";
		private static final int VERSION_INITIAL = 1;
		private static final int DATABASE_VERSION = VERSION_INITIAL;
		
		public DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + TABLE_AIXWIDGETS + " ("
					+ BaseColumns._ID + " INTEGER PRIMARY KEY,"
					+ AixWidgetsColumns.TITLE + " TEXT,"
					+ AixWidgetsColumns.SIZE + " INTEGER,"
					+ AixWidgetsColumns.VIEWS + " STRING);");
			
			db.execSQL("CREATE TABLE " + TABLE_AIXVIEWS + " ("
					+ BaseColumns._ID + " INTEGER PRIMARY KEY,"
					+ AixViewsColumns.TITLE + " TEXT,"
					+ AixViewsColumns.TYPE + " INTEGER,"
					+ AixViewsColumns.LOCATION + " INTEGER,"
					+ AixViewsColumns.UNITS + " INTEGER,"
					+ AixViewsColumns.PERIOD + " INTEGER);");
			
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
			
			db.execSQL("CREATE TABLE " + TABLE_AIXFORECASTS + " ("
					+ BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
					+ AixForecastsColumns.LOCATION + " INTEGER,"
					+ AixForecastsColumns.TIME_FROM + " INTEGER,"
					+ AixForecastsColumns.TIME_TO + " INTEGER,"
					+ AixForecastsColumns.TEMPERATURE + " REAL,"
					+ AixForecastsColumns.RAIN_LOWVAL + " REAL,"
					+ AixForecastsColumns.RAIN_VALUE + " REAL,"
					+ AixForecastsColumns.RAIN_HIGHVAL + " REAL,"
					+ AixForecastsColumns.WEATHER_ICON + " INTEGER,"
					+ AixForecastsColumns.SUN_RISE + " INTEGER,"
					+ AixForecastsColumns.SUN_SET + " INTEGER);");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_AIXWIDGETS);
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_AIXVIEWS);
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_AIXLOCATIONS);
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_AIXFORECASTS);
			onCreate(db);
		}
		
	}
	
	private String buildSelectionStringWithId(Uri uri, String selection) {
		StringBuilder where = new StringBuilder(
				BaseColumns._ID + '=' + uri.getPathSegments().get(1));
		if (!TextUtils.isEmpty(selection)) {
			where.append(" AND (" + selection + ')');
		}
		return where.toString();
	}
	
	private int deleteViewForecasts(SQLiteDatabase db, Uri uri, String selection,
			String[] selectionArgs) {
		int count = 0;
		
		Cursor cursor = db.query(
				TABLE_AIXVIEWS,
				null,
				buildSelectionStringWithId(uri, selection),
				selectionArgs,
				null, null, null);
		
		if (cursor != null) {
			long locationId = -1;
			if (cursor.moveToFirst()) {
				locationId = cursor.getLong(AixViewsColumns.LOCATION_COLUMN);
			}
			cursor.close();
			
			if (locationId != -1) {
				List<String> pathSegments = uri.getPathSegments();
				if (pathSegments.size() == 3) {
					// Delete all forecasts for the given location
					count = db.delete(
							TABLE_AIXFORECASTS,
							AixForecasts.LOCATION + '=' + locationId,
							null);
				} else {
					String timeFrom = null, timeTo = null;
					if (pathSegments.size() == 4) {
						timeFrom = timeTo = pathSegments.get(3);
					} else if (pathSegments.size() == 5) {
						timeFrom = pathSegments.get(3);
						timeTo = pathSegments.get(4);
					}
					if (timeFrom != null && timeTo != null) {
						count = db.delete(
								TABLE_AIXFORECASTS,
								AixForecastsColumns.TIME_FROM + "=? AND " +
								AixForecastsColumns.TIME_TO + "=?",
								new String[] {
										timeFrom, timeTo
								});
					}
				}
				
			}
		}
		
		return count;
	}
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		if (LOGD) Log.d(TAG, "delete() with uri=" + uri);
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		
		int count = 0;
		int match = sUriMatcher.match(uri);
		int table = match / 100;
		int action = match % 100;
		
		if (LOGD) Log.d(TAG, "table=" + table + " action=" + action);
		
		if ((table >= 1 && table <= 4) && action <= 2) {
			count = db.delete(
					AIX_TABLES[table - 1],
					action == 2 ? buildSelectionStringWithId(uri, selection) : selection,
					selectionArgs);
			// Really gotta fix notifications at some point..
		} else if (table == 2 && (action >= 4 && action <= 6)) {
			deleteViewForecasts(db, uri, selection, selectionArgs);
		} else {
			throw new UnsupportedOperationException();
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
		case AIXVIEWS:
			return AixViews.CONTENT_TYPE;
		case AIXVIEWS_ID:
			return AixViews.CONTENT_ITEM_TYPE;
		case AIXVIEWS_FORECASTS:
			return AixForecasts.CONTENT_ITEM_TYPE;
		case AIXVIEWS_FORECASTS_AT:
			return AixForecasts.CONTENT_ITEM_TYPE;
		case AIXVIEWS_FORECASTS_FROM_TO:
			return AixForecasts.CONTENT_ITEM_TYPE;
		case AIXLOCATIONS:
			return AixLocations.CONTENT_TYPE;
		case AIXLOCATIONS_ID:
			return AixLocations.CONTENT_ITEM_TYPE;
		case AIXFORECASTS:
			return AixForecasts.CONTENT_TYPE;
		case AIXFORECASTS_ID:
			return AixForecasts.CONTENT_ITEM_TYPE;
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
			long rowId = db.insert(TABLE_AIXWIDGETS, AixWidgetsColumns.TITLE, values);
			if (rowId != -1) {
				resultUri = ContentUris.withAppendedId(AixWidgets.CONTENT_URI, rowId);
				getContext().getContentResolver().notifyChange(resultUri, null);
			}
			break;
		}
		case AIXVIEWS: {
			long rowId = db.insert(TABLE_AIXVIEWS, AixViewsColumns.TITLE, values);
			if (rowId != -1) {
				resultUri = ContentUris.withAppendedId(AixViews.CONTENT_URI, rowId);
				getContext().getContentResolver().notifyChange(resultUri, null);
			}
			break;
		}
		case AIXLOCATIONS: {
			long rowId = db.insert(TABLE_AIXLOCATIONS, AixLocationsColumns.TITLE, values);
			if (rowId != -1) {
				resultUri = ContentUris.withAppendedId(AixLocations.CONTENT_URI, rowId);
				getContext().getContentResolver().notifyChange(resultUri, null);
				//getContext().getContentResolver().notifyChange(AixLocations.CONTENT_URI, null);
			}
			break;
		}
		case AIXFORECASTS: {
			addForecast(db, values);
			break;
		}
		default:
			throw new UnsupportedOperationException();
		}
		
		return resultUri;
	}
	
	public Uri addForecast(SQLiteDatabase db, ContentValues values) {
		Uri resultUri = null;
		long rowId = -1;
		
		if (values.containsKey(AixForecastsColumns.SUN_RISE) || values.containsKey(AixForecastsColumns.SUN_SET)) {
			// Sun stuff
			Cursor cursor = db.query(
					TABLE_AIXFORECASTS,
					null,
					AixForecastsColumns.LOCATION + "=?" + " AND " +
					AixForecastsColumns.TIME_FROM + "=?" + " AND " +
					AixForecastsColumns.TIME_TO + "=?" + " AND " +
					AixForecastsColumns.SUN_RISE + " IS NOT NULL AND " +
					AixForecastsColumns.SUN_SET + " IS NOT NULL",
					new String[] {
							values.getAsString(AixForecastsColumns.LOCATION),
							values.getAsString(AixForecastsColumns.TIME_FROM),
							values.getAsString(AixForecastsColumns.TIME_TO)
					}, null, null, null);
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					rowId = cursor.getLong(0);
				}
				cursor.close();
			}
		} else {
			Cursor cursor = db.query(
					TABLE_AIXFORECASTS,
					null,
					AixForecastsColumns.LOCATION + "=?" + " AND " +
					AixForecastsColumns.TIME_FROM + "=?" + " AND " +
					AixForecastsColumns.TIME_TO + "=?" + " AND " +
					AixForecastsColumns.SUN_RISE + " IS NULL AND " +
					AixForecastsColumns.SUN_SET + " IS NULL",
					new String[] {
							values.getAsString(AixForecastsColumns.LOCATION),
							values.getAsString(AixForecastsColumns.TIME_FROM),
							values.getAsString(AixForecastsColumns.TIME_TO)
					}, null, null, null);
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					rowId = cursor.getLong(0);
				}
				cursor.close();
			}
		}
		
		if (rowId != -1) {
			// A record already exists for this time and location. Update its values:
			values.put(BaseColumns._ID, Long.toString(rowId));
			db.replace(TABLE_AIXFORECASTS, null, values);
		} else {
			// No record exists for this time. Insert new row:
			rowId = db.insert(TABLE_AIXFORECASTS, AixForecastsColumns.LOCATION, values);
		}
		
		if (rowId != -1) {
			resultUri = ContentUris.withAppendedId(AixForecasts.CONTENT_URI, rowId);
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
			String[] selectionArgs, String sortOrder) {
		if (LOGD) Log.d(TAG, "query() with uri=" + uri);
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		
		String limit = null;
		
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
		case AIXVIEWS: {
			qb.setTables(TABLE_AIXVIEWS);
			break;
		}
		case AIXVIEWS_ID: {
			qb.setTables(TABLE_AIXVIEWS);
			qb.appendWhere(BaseColumns._ID + '=' + uri.getPathSegments().get(1));
			break;
		}
		case AIXVIEWS_LOCATION: {
			qb.setTables(TABLE_AIXLOCATIONS);
			
			Cursor cursor = db.query(
					TABLE_AIXVIEWS,
					null,
					BaseColumns._ID + '=' + uri.getPathSegments().get(1),
					null, null, null, null);

			long locationId = -1;
			
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					locationId = cursor.getLong(AixViewsColumns.LOCATION_COLUMN);
				}
				cursor.close();
			}
			
			if (locationId != -1) {
				qb.appendWhere(BaseColumns._ID + '=' + locationId);
			} else {
				if (LOGD) Log.d(TAG, "query() with uri=" + uri + " failed. No location in view!");
				return null; // Could not properly service request, as no location was found.
			}
			break;
		}
		case AIXVIEWS_FORECASTS: {
			qb.setTables(TABLE_AIXFORECASTS);
			
			Cursor cursor = db.query(
					TABLE_AIXVIEWS,
					null,
					BaseColumns._ID + '=' + uri.getPathSegments().get(1),
					null, null, null, null);

			long locationId = -1;
			
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					locationId = cursor.getLong(AixViewsColumns.LOCATION_COLUMN);
				}
				cursor.close();
			}
			
			if (locationId != -1) {
				qb.appendWhere(AixForecastsColumns.LOCATION + '=' + locationId);
			} else {
				if (LOGD) Log.d(TAG, "query() with uri=" + uri + " failed. No location in view!");
				return null; // Could not properly service request, as no location was found.
			}
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
		case AIXFORECASTS: {
			qb.setTables(TABLE_AIXFORECASTS);
			break;
		}
		case AIXFORECASTS_ID: {
			qb.setTables(TABLE_AIXFORECASTS);
			qb.appendWhere(BaseColumns._ID + '=' + uri.getPathSegments().get(1));
			break;
		}
		
//		case AIXWIDGETS_FORECASTS: {
//			String aixWidgetId = uri.getPathSegments().get(1);
//			qb.setTables(TABLE_AIXFORECASTS);
//			qb.appendWhere(AixForecastsColumns.APPWIDGET_ID + "=" + aixWidgetId);
//			sortOrder = AixForecastsColumns.TIME_FROM + " ASC";
//			break;
//		}
//		case AIXWIDGETS_FORECASTS_AT: {
//			String aixWidgetId = uri.getPathSegments().get(1);
//			String atTime = uri.getPathSegments().get(3);
//			qb.setTables(TABLE_AIXFORECASTS);
//			qb.appendWhere(AixForecastsColumns.APPWIDGET_ID + "=" + aixWidgetId);
//			qb.appendWhere(AixForecastsColumns.TIME_FROM + "=" + atTime);
//			qb.appendWhere(AixForecastsColumns.TIME_TO + "=" + atTime);
//			break;
//		}
//		case AIXWIDGETS_FORECASTS_FROM_TO: {
//			String aixWidgetId = uri.getPathSegments().get(1);
//			String fromTime = uri.getPathSegments().get(3);
//			String toTime = uri.getPathSegments().get(4);
//			qb.setTables(TABLE_AIXFORECASTS);
//			qb.appendWhere(AixForecastsColumns.APPWIDGET_ID + "=" + aixWidgetId);
//			qb.appendWhere(AixForecastsColumns.TIME_FROM + "=" + fromTime);
//			qb.appendWhere(AixForecastsColumns.TIME_TO + "=" + toTime);
//			break;
//		}
		}

		return qb.query(db, projection, selection, selectionArgs, null, null, null);
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		if (LOGD) Log.d(TAG, "update() with uri=" + uri);
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		
		switch (sUriMatcher.match(uri)) {
		case AIXWIDGETS_ID: {
			return db.update(TABLE_AIXWIDGETS, values, buildSelectionStringWithId(uri, selection), selectionArgs);
		}
		case AIXVIEWS_ID: {
			return db.update(TABLE_AIXVIEWS, values, buildSelectionStringWithId(uri, selection), selectionArgs);
		}
		case AIXVIEWS_LOCATION: {
			Cursor cursor = db.query(
					TABLE_AIXVIEWS,
					null,
					buildSelectionStringWithId(uri, selection),
					selectionArgs,
					null, null, null);

			long locationId = -1;
			
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					locationId = cursor.getLong(AixViewsColumns.LOCATION_COLUMN);
				}
				cursor.close();
			}
			
			if (locationId != -1) {
				StringBuilder where = new StringBuilder(
						BaseColumns._ID + '=' + locationId);
				if (!TextUtils.isEmpty(selection)) {
					where.append(" AND (" + selection + ')');
				}
				return db.update(TABLE_AIXLOCATIONS, values, where.toString(), selectionArgs);
			} else {
				if (LOGD) Log.d(TAG, "update() with uri=" + uri + " failed. No location in view!");
				return 0; // Could not properly service request, as no location was found.
			}
		}
		default:
			if (LOGD) Log.d(TAG, "update() with uri=" + uri + " not matched");
			break;
		}

		return 0;
	}

	private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	
	private static final int AIXWIDGETS = 101;
	private static final int AIXWIDGETS_ID = 102;
	
	private static final int AIXVIEWS = 201;
	private static final int AIXVIEWS_ID = 202;
	private static final int AIXVIEWS_LOCATION = 203;
	private static final int AIXVIEWS_FORECASTS = 204;
	private static final int AIXVIEWS_FORECASTS_AT = 205;
	private static final int AIXVIEWS_FORECASTS_FROM_TO = 206;
	
	private static final int AIXLOCATIONS = 301;
	private static final int AIXLOCATIONS_ID = 302;
	
	private static final int AIXFORECASTS = 401;
	private static final int AIXFORECASTS_ID = 402;
	
	static {
		/* URI for retrieving all widgets */
		sUriMatcher.addURI(AUTHORITY, "aixwidgets", AIXWIDGETS);
		/* URI for retrieving a specific widget by ID */
		sUriMatcher.addURI(AUTHORITY, "aixwidgets/#", AIXWIDGETS_ID);
		
		/* URI for retrieving all views */
		sUriMatcher.addURI(AUTHORITY, "aixviews", AIXVIEWS);
		/* URI for retrieving a specific view by ID */
		sUriMatcher.addURI(AUTHORITY, "aixviews/#", AIXVIEWS_ID);
		/* URI for retrieving the location for a specific view by ID */
		sUriMatcher.addURI(AUTHORITY, "aixviews/#/location", AIXVIEWS_LOCATION);
		/* URI for retrieving all forecasts for a specific view by ID */
		sUriMatcher.addURI(AUTHORITY, "aixviews/#/forecasts", AIXVIEWS_FORECASTS);
		/* URI for retrieving forecasts at a given time for a specific view by ID */
		sUriMatcher.addURI(AUTHORITY, "aixviews/#/forecasts_at/#", AIXVIEWS_FORECASTS_AT);
		/* URI for retrieving forecasts for a time interval for a specific view by ID */
		sUriMatcher.addURI(AUTHORITY, "aixviews/#/forecasts_from_to/#/#", AIXVIEWS_FORECASTS_FROM_TO);
		
		/* URI for retrieving all locations */
		sUriMatcher.addURI(AUTHORITY, "aixlocations", AIXLOCATIONS);
		/* URI for retrieving a specific location by ID */
		sUriMatcher.addURI(AUTHORITY, "aixlocations/#", AIXLOCATIONS_ID);
		
		/* URI for retrieving all forecasts */
		sUriMatcher.addURI(AUTHORITY, "aixforecasts", AIXFORECASTS);
		/* URI for retrieving a specific forecast by ID */
		sUriMatcher.addURI(AUTHORITY, "aixforecasts/#", AIXFORECASTS_ID);
	}
}
