package net.veierland.aix;

import net.veierland.aix.AixProvider.AixLocationsColumns;
import net.veierland.aix.AixProvider.AixViews;
import net.veierland.aix.AixProvider.AixViewsColumns;
import net.veierland.aix.AixProvider.AixWidgets;
import net.veierland.aix.AixProvider.AixWidgetsColumns;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class AixConfigure extends PreferenceActivity implements View.OnClickListener, Preference.OnPreferenceClickListener, ColorPickerView.OnColorSelectedListener {

	private static final String TAG = "AixConfigure";
	
	private long mLocationId = -1;
	private String mLocationName = null;
	
	private Button mAddWidgetButton = null;
	private Preference mLocationPreference = null;
	
	private Preference mBackgroundColorPreference = null;
	private Preference mTextColorPreference = null;
	private Preference mLocationBackgroundColorPreference = null;
	private Preference mLocationTextColorPreference = null;
	private Preference mGridColorPreference = null;
	private Preference mGridOutlineColorPreference = null;
	private Preference mMaxRainColorPreference = null;
	private Preference mMinRainColorPreference = null;
	private Preference mAboveFreezingColorPreference = null;
	private Preference mBelowFreezingColorPreference = null;
	
	private final static int SET_UNITS = 0;
	private final static int CHANGE_COLORS = 1;
	private final static int SYSTEM_SETTINGS = 2;
	private final static int SELECT_LOCATION = 3;
	
	private Uri mWidgetUri = null, mViewUri = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Log.d(TAG, "Creating Configure Activity");
		
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		Editor editor = settings.edit();
		editor.clear();
		editor.putBoolean(getString(R.string.preference_awake_only), settings.getBoolean(getString(R.string.preference_awake_only), false));
		editor.putBoolean(getString(R.string.preference_wifi_only), settings.getBoolean(getString(R.string.preference_wifi_only), false));
		editor.putString(getString(R.string.preference_update_rate), settings.getString(getString(R.string.preference_update_rate), "0"));
		
		Intent intent = getIntent();
		String action = intent.getAction();
		
		if (Intent.ACTION_EDIT.equals(action)) {
			mWidgetUri = intent.getData();
			
			ContentResolver resolver = getContentResolver();
			Cursor widgetCursor = resolver.query(mWidgetUri, null, null, null, null);
			
			if (widgetCursor != null) {
				if (widgetCursor.moveToFirst()) {
					mViewUri = ContentUris.withAppendedId(AixViews.CONTENT_URI,
							widgetCursor.getLong(AixWidgetsColumns.VIEWS_COLUMN));
					loadWidgetSettingsFromProvider(widgetCursor, editor);
				}
				widgetCursor.close();
			}
			
			if (mViewUri != null) {
				Cursor viewCursor = resolver.query(mViewUri, null, null, null, null);
				if (viewCursor != null) {
					if (viewCursor.moveToFirst()) {
						// Load stuff from view
					}
					viewCursor.close();
				}
				
				Cursor locationCursor = resolver.query(
						Uri.withAppendedPath(mViewUri, AixViews.TWIG_LOCATION),
						null, null, null, null);
				if (locationCursor != null) {
					if (locationCursor.moveToFirst()) {
						mLocationId = locationCursor.getLong(0);
						mLocationName = locationCursor.getString(AixLocationsColumns.TITLE_COLUMN);
					}
					locationCursor.close();
				}
			}
		}
		editor.commit();
		
		setContentView(R.layout.aix_configure);
		addPreferencesFromResource(R.xml.aix_configuration);

		mLocationPreference = findPreference("select_location");
		mLocationPreference.setOnPreferenceClickListener(this);
		
		mAddWidgetButton = (Button) findViewById(R.id.add_widget_button);
		mAddWidgetButton.setOnClickListener(this);
		
		mBackgroundColorPreference = findPreference(getString(R.string.preference_background_color));
		mBackgroundColorPreference.setOnPreferenceClickListener(this);
		mTextColorPreference = findPreference(getString(R.string.preference_text_color));
		mTextColorPreference.setOnPreferenceClickListener(this);
		mLocationBackgroundColorPreference = findPreference(getString(R.string.preference_location_background_color));
		mLocationBackgroundColorPreference.setOnPreferenceClickListener(this);
		mLocationTextColorPreference = findPreference(getString(R.string.preference_location_text_color));
		mLocationTextColorPreference.setOnPreferenceClickListener(this);
		mGridColorPreference = findPreference(getString(R.string.preference_grid_color));
		mGridColorPreference.setOnPreferenceClickListener(this);
		mGridOutlineColorPreference = findPreference(getString(R.string.preference_grid_outline_color));
		mGridOutlineColorPreference.setOnPreferenceClickListener(this);
		mMaxRainColorPreference = findPreference(getString(R.string.preference_max_rain_color));
		mMaxRainColorPreference.setOnPreferenceClickListener(this);
		mMinRainColorPreference = findPreference(getString(R.string.preference_min_rain_color));
		mMinRainColorPreference.setOnPreferenceClickListener(this);
		mAboveFreezingColorPreference = findPreference(getString(R.string.preference_above_freezing_color));
		mAboveFreezingColorPreference.setOnPreferenceClickListener(this);
		mBelowFreezingColorPreference = findPreference(getString(R.string.preference_below_freezing_color));
		mBelowFreezingColorPreference.setOnPreferenceClickListener(this);
		
		if (Intent.ACTION_EDIT.equals(action)) {
			if (mLocationName != null) {
				mLocationPreference.setSummary(mLocationName);
			}
			mAddWidgetButton.setText("Apply Settings");
		}
		
		setResult(Activity.RESULT_CANCELED);
	}

	private void addColorToEditor(Cursor widgetCursor, int columnId, Editor editor,
			int colorStringId)
	{
		String colorString = widgetCursor.getString(columnId);
		if (colorString != null) {
			try {
				int color = Integer.parseInt(colorString);
				editor.putInt(getString(colorStringId), color);
			} catch (NumberFormatException e) { }
		}
	}
	
	private void loadWidgetSettingsFromProvider(Cursor widgetCursor, Editor editor) {
		String temperatureUnit = widgetCursor.getString(AixWidgetsColumns.TEMPERATURE_UNITS_COLUMN);
		if (temperatureUnit != null) {
			editor.putString(getString(R.string.preference_temperature_units), temperatureUnit);
		} else {
			editor.putString(getString(R.string.preference_temperature_units), "1");
		}
		String precipitationUnit = widgetCursor.getString(AixWidgetsColumns.PRECIPITATION_UNITS_COLUMN);
		if (precipitationUnit != null) {
			editor.putString(getString(R.string.preference_precipitation_units), precipitationUnit);
		} else {
			editor.putString(getString(R.string.preference_precipitation_units), "1");
		}
		
		addColorToEditor(widgetCursor, AixWidgetsColumns.BACKGROUND_COLOR_COLUMN, editor, R.string.preference_background_color);
		addColorToEditor(widgetCursor, AixWidgetsColumns.TEXT_COLOR_COLUMN, editor, R.string.preference_text_color);
		addColorToEditor(widgetCursor, AixWidgetsColumns.LOCATION_BACKGROUND_COLOR_COLUMN, editor, R.string.preference_location_background_color);
		addColorToEditor(widgetCursor, AixWidgetsColumns.LOCATION_TEXT_COLOR_COLUMN, editor, R.string.preference_location_text_color);
		addColorToEditor(widgetCursor, AixWidgetsColumns.GRID_COLOR_COLUMN, editor, R.string.preference_grid_color);
		addColorToEditor(widgetCursor, AixWidgetsColumns.GRID_OUTLINE_COLOR_COLUMN, editor, R.string.preference_grid_outline_color);
		addColorToEditor(widgetCursor, AixWidgetsColumns.MAX_RAIN_COLOR_COLUMN, editor, R.string.preference_max_rain_color);
		addColorToEditor(widgetCursor, AixWidgetsColumns.MIN_RAIN_COLOR_COLUMN, editor, R.string.preference_min_rain_color);
		addColorToEditor(widgetCursor, AixWidgetsColumns.ABOVE_FREEZING_COLOR_COLUMN, editor, R.string.preference_above_freezing_color);
		addColorToEditor(widgetCursor, AixWidgetsColumns.BELOW_FREEZING_COLOR_COLUMN, editor, R.string.preference_below_freezing_color);
	}
	
	private void saveWidgetSettingsToProvider(SharedPreferences settings, ContentValues values) {
		values.put(AixWidgetsColumns.TEMPERATURE_UNITS,
				settings.getString(getString(R.string.preference_temperature_units), "1"));
		values.put(AixWidgetsColumns.PRECIPITATION_UNITS,
				settings.getString(getString(R.string.preference_precipitation_units), "1"));
		
		Resources resources = getResources();
		values.put(AixWidgetsColumns.BACKGROUND_COLOR, settings.getInt(getString(R.string.preference_background_color), resources.getColor(R.color.background_default)));
		values.put(AixWidgetsColumns.TEXT_COLOR, settings.getInt(getString(R.string.preference_text_color), resources.getColor(R.color.text_default)));
		values.put(AixWidgetsColumns.LOCATION_BACKGROUND_COLOR, settings.getInt(getString(R.string.preference_location_background_color), resources.getColor(R.color.location_background_default)));
		values.put(AixWidgetsColumns.LOCATION_TEXT_COLOR, settings.getInt(getString(R.string.preference_location_text_color), resources.getColor(R.color.location_text_default)));
		values.put(AixWidgetsColumns.GRID_COLOR, settings.getInt(getString(R.string.preference_grid_color), resources.getColor(R.color.grid_default)));
		values.put(AixWidgetsColumns.GRID_OUTLINE_COLOR, settings.getInt(getString(R.string.preference_grid_outline_color), resources.getColor(R.color.grid_outline_default)));
		values.put(AixWidgetsColumns.MAX_RAIN_COLOR, settings.getInt(getString(R.string.preference_max_rain_color), resources.getColor(R.color.maximum_rain_default)));
		values.put(AixWidgetsColumns.MIN_RAIN_COLOR, settings.getInt(getString(R.string.preference_min_rain_color), resources.getColor(R.color.minimum_rain_default)));
		values.put(AixWidgetsColumns.ABOVE_FREEZING_COLOR, settings.getInt(getString(R.string.preference_above_freezing_color), resources.getColor(R.color.above_freezing_default)));
		values.put(AixWidgetsColumns.BELOW_FREEZING_COLOR, settings.getInt(getString(R.string.preference_below_freezing_color), resources.getColor(R.color.below_freezing_default)));
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case SELECT_LOCATION: {
			if (resultCode == Activity.RESULT_OK) {
				Uri tempUri = Uri.parse(data.getStringExtra("location"));
				Cursor cursor = getContentResolver().query(
						tempUri, null, null, null, null);
				if (cursor != null) {
					if (cursor.moveToFirst()) {
						findPreference("select_location").setSummary(cursor.getString(AixLocationsColumns.TITLE_COLUMN));
						mLocationId = cursor.getLong(AixLocationsColumns.LOCATIONS_ID_COLUMN);
					}
					cursor.close();
				}
			}
			break;
		}
		}
	}

	@Override
	public void onClick(View v) {
		if (v == mAddWidgetButton) {
			if (mLocationId == -1) {
				Toast.makeText(this, "You must set a location", Toast.LENGTH_SHORT).show();
			} else {
				SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
				ContentValues widgetValues = new ContentValues();
				saveWidgetSettingsToProvider(settings, widgetValues);
				
				Intent intent = getIntent();
				String action = intent.getAction();
				
				if (Intent.ACTION_EDIT.equals(action)) {
					getContentResolver().update(mWidgetUri, widgetValues, null, null);
					ContentValues values = new ContentValues();
					values.put(AixViewsColumns.LOCATION, mLocationId);
					getContentResolver().update(mViewUri, values, null, null);
					AixService.requestUpdate(Integer.parseInt(mWidgetUri.getLastPathSegment()));
					startService(new Intent(this, AixService.class));
					setResult(Activity.RESULT_OK);
				} else {
					int appWidgetId = -1;
					Bundle extras = intent.getExtras();
					if (extras != null) {
						appWidgetId = extras.getInt(
								AppWidgetManager.EXTRA_APPWIDGET_ID,
								AppWidgetManager.INVALID_APPWIDGET_ID);
						if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
							ContentResolver resolver = getContentResolver();
							ContentValues values = new ContentValues();
							values.put(AixViewsColumns.LOCATION, mLocationId);
							//values.put(AixViewsColumns.UNITS, ((ListPreference)findPreference("measurement_unit")).getValue());
							Uri viewUri = resolver.insert(AixViews.CONTENT_URI, values);
							
							if (viewUri != null) {
								widgetValues.put(BaseColumns._ID, appWidgetId);
								widgetValues.put(AixWidgetsColumns.VIEWS, viewUri.getLastPathSegment());
								getContentResolver().insert(AixWidgets.CONTENT_URI, widgetValues);
								
								AixService.requestUpdate(appWidgetId);
								startService(new Intent(this, AixService.class));
								
								Intent resultIntent = new Intent();
								resultIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
								setResult(Activity.RESULT_OK, resultIntent);
							}
						}
					}
				}
				finish();
			}
		}
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		Resources resources = getResources();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		
		if (preference == mLocationPreference) {
			Intent intent = new Intent(AixConfigure.this, AixLocationSelectionActivity.class);
			startActivityForResult(intent, SELECT_LOCATION);
			return true;
		} else if (preference == mBackgroundColorPreference) {
			int color = settings.getInt(getString(R.string.preference_background_color), resources.getColor(R.color.background_default));
			ColorPickerView.showColorPickerDialog(
					this, getLayoutInflater(), R.string.preference_background_color, color, this);
			return true;
		} else if (preference == mTextColorPreference) {
			int color = settings.getInt(getString(R.string.preference_text_color), resources.getColor(R.color.text_default));
			ColorPickerView.showColorPickerDialog(
					this, getLayoutInflater(), R.string.preference_text_color, color, this);
			return true;
		} else if (preference == mLocationBackgroundColorPreference) {
			int color = settings.getInt(getString(R.string.preference_location_background_color), resources.getColor(R.color.location_background_default));
			ColorPickerView.showColorPickerDialog(
					this, getLayoutInflater(), R.string.preference_location_background_color, color, this);
			return true;
		} else if (preference == mLocationTextColorPreference) {
			int color = settings.getInt(getString(R.string.preference_location_text_color), resources.getColor(R.color.location_text_default));
			ColorPickerView.showColorPickerDialog(
					this, getLayoutInflater(), R.string.preference_location_text_color, color, this);
			return true;
		} else if (preference == mGridColorPreference) {
			int color = settings.getInt(getString(R.string.preference_grid_color), resources.getColor(R.color.grid_default));
			ColorPickerView.showColorPickerDialog(
					this, getLayoutInflater(), R.string.preference_grid_color, color, this);
			return true;
		} else if (preference == mGridOutlineColorPreference) {
			int color = settings.getInt(getString(R.string.preference_grid_outline_color), resources.getColor(R.color.grid_outline_default));
			ColorPickerView.showColorPickerDialog(
					this, getLayoutInflater(), R.string.preference_grid_outline_color, color, this);
			return true;
		} else if (preference == mMaxRainColorPreference) {
			int color = settings.getInt(getString(R.string.preference_max_rain_color), resources.getColor(R.color.maximum_rain_default));
			ColorPickerView.showColorPickerDialog(
					this, getLayoutInflater(), R.string.preference_max_rain_color, color, this);
			return true;
		} else if (preference == mMinRainColorPreference) {
			int color = settings.getInt(getString(R.string.preference_min_rain_color), resources.getColor(R.color.minimum_rain_default));
			ColorPickerView.showColorPickerDialog(
					this, getLayoutInflater(), R.string.preference_min_rain_color, color, this);
			return true;
		} else if (preference == mAboveFreezingColorPreference) {
			int color = settings.getInt(getString(R.string.preference_above_freezing_color), resources.getColor(R.color.above_freezing_default));
			ColorPickerView.showColorPickerDialog(
					this, getLayoutInflater(), R.string.preference_above_freezing_color, color, this);
			return true;
		} else if (preference == mBelowFreezingColorPreference) {
			int color = settings.getInt(getString(R.string.preference_below_freezing_color), resources.getColor(R.color.below_freezing_default));
			ColorPickerView.showColorPickerDialog(
					this, getLayoutInflater(), R.string.preference_below_freezing_color, color, this);
			return true;
		}
		
		return false;
	}

	@Override
	public void colorSelected(int requestCode, int color) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		Editor editor = settings.edit();
		
		switch (requestCode) {
		case R.string.preference_background_color:
			editor.putInt(getString(R.string.preference_background_color), color);
			break;
		case R.string.preference_text_color:
			editor.putInt(getString(R.string.preference_text_color), color);
			break;
		case R.string.preference_location_background_color:
			editor.putInt(getString(R.string.preference_location_background_color), color);
			break;
		case R.string.preference_location_text_color:
			editor.putInt(getString(R.string.preference_location_text_color), color);
			break;
		case R.string.preference_grid_color:
			editor.putInt(getString(R.string.preference_grid_color), color);
			break;
		case R.string.preference_grid_outline_color:
			editor.putInt(getString(R.string.preference_grid_outline_color), color);
			break;
		case R.string.preference_max_rain_color:
			editor.putInt(getString(R.string.preference_max_rain_color), color);
			break;
		case R.string.preference_min_rain_color:
			editor.putInt(getString(R.string.preference_min_rain_color), color);
			break;
		case R.string.preference_above_freezing_color:
			editor.putInt(getString(R.string.preference_above_freezing_color), color);
			break;
		case R.string.preference_below_freezing_color:
			editor.putInt(getString(R.string.preference_below_freezing_color), color);
			break;
		}

		editor.commit();
	}
	
}