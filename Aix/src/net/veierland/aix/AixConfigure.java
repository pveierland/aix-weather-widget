package net.veierland.aix;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.veierland.aix.AixProvider.AixLocationsColumns;
import net.veierland.aix.AixProvider.AixSettingsColumns;
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
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class AixConfigure extends PreferenceActivity implements View.OnClickListener, Preference.OnPreferenceClickListener, ColorPickerView.OnColorSelectedListener, OnPreferenceChangeListener, OnSharedPreferenceChangeListener {

	private static final String TAG = "AixConfigure";
	
	private long mLocationId = -1;
	private String mLocationName = null;
	
	private Button mAddWidgetButton = null;
	private Preference mLocationPreference = null;
	
	private Preference mTemperatureUnitPreference = null;
	private Preference mPrecipitationUnitPreference = null;
	private EditTextPreference mPrecipitationScalingPreference = null;
	
	private Preference mBackgroundColorPreference = null;
	private Preference mPatternColorPreference = null;
	private Preference mTextColorPreference = null;
	private ListPreference mTopTextVisibilityPreference = null;
	
	private Preference mBorderColorPreference = null;
	private EditTextPreference mBorderThicknessPreference = null;
	private EditTextPreference mBorderRoundingPreference = null;
	
	private Preference mDayBackgroundColorPreference = null;
	private Preference mNightBackgroundColorPreference = null;
	
	private Preference mGridColorPreference = null;
	private Preference mGridOutlineColorPreference = null;
	
	private Preference mMaxRainColorPreference = null;
	private Preference mMinRainColorPreference = null;
	private Preference mAboveFreezingColorPreference = null;
	private Preference mBelowFreezingColorPreference = null;
	
	private Uri mWidgetUri = null, mViewUri = null;
	
	private final static int SET_UNITS = 0;
	private final static int CHANGE_COLORS = 1;
	private final static int SYSTEM_SETTINGS = 2;
	private final static int SELECT_LOCATION = 3;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		
		Editor editor = settings.edit();
		editor.clear();
		// Preserve global settings
		editor.putBoolean(getString(R.string.awake_only_bool), settings.getBoolean(getString(R.string.awake_only_bool), false));
		editor.putBoolean(getString(R.string.wifi_only_bool), settings.getBoolean(getString(R.string.wifi_only_bool), false));
		editor.putString(getString(R.string.update_rate_string), settings.getString(getString(R.string.update_rate_string), "0"));
		
		Intent intent = getIntent();
		String action = intent.getAction();
		
		if (Intent.ACTION_EDIT.equals(action)) {
			ContentResolver resolver = getContentResolver();
			mWidgetUri = intent.getData();
			
			if (mWidgetUri != null) {
				Cursor widgetCursor = resolver.query(mWidgetUri, null, null, null, null);
				
				if (widgetCursor != null) {
					if (widgetCursor.moveToFirst()) {
						// TODO This will be changed in the future. Assuming 1 view / widget atm.
						mViewUri = ContentUris.withAppendedId(AixViews.CONTENT_URI,
								widgetCursor.getLong(AixWidgetsColumns.VIEWS_COLUMN));
					}
					widgetCursor.close();
				}
				
				loadWidgetSettingsFromProvider(resolver, mWidgetUri, editor);
			}
			
			if (mViewUri != null) {
				Cursor locationCursor = resolver.query(
						Uri.withAppendedPath(mViewUri, AixViews.TWIG_LOCATION),
						null, null, null, null);
				if (locationCursor != null) {
					if (locationCursor.moveToFirst()) {
						mLocationId = locationCursor.getLong(AixLocationsColumns.LOCATION_ID_COLUMN);
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
		
		mTemperatureUnitPreference = findPreference(getString(R.string.temperature_units_string));
		mPrecipitationUnitPreference = findPreference(getString(R.string.precipitation_units_string));
		mPrecipitationScalingPreference = (EditTextPreference)findPreference(
				getString(R.string.precipitation_scaling_string));
		mPrecipitationScalingPreference.setOnPreferenceChangeListener(this);
		
		mBackgroundColorPreference = findPreference(getString(R.string.background_color_int));
		mBackgroundColorPreference.setOnPreferenceClickListener(this);
		mPatternColorPreference = findPreference(getString(R.string.pattern_color_int));
		mPatternColorPreference.setOnPreferenceClickListener(this);
		mTextColorPreference = findPreference(getString(R.string.text_color_int));
		mTextColorPreference.setOnPreferenceClickListener(this);
		mTopTextVisibilityPreference = (ListPreference)findPreference(
				getString(R.string.top_text_visibility_string));
		
		mBorderColorPreference = findPreference(getString(R.string.border_color_int));
		mBorderColorPreference.setOnPreferenceClickListener(this);
		mBorderThicknessPreference = (EditTextPreference)findPreference(
				getString(R.string.border_thickness_string));
		mBorderThicknessPreference.setOnPreferenceChangeListener(this);
		mBorderRoundingPreference = (EditTextPreference)findPreference(
				getString(R.string.border_rounding_string));
		mBorderRoundingPreference.setOnPreferenceChangeListener(this);
		
		mDayBackgroundColorPreference = findPreference(getString(R.string.day_color_int));
		mDayBackgroundColorPreference.setOnPreferenceClickListener(this);
		mNightBackgroundColorPreference = findPreference(getString(R.string.night_color_int));
		mNightBackgroundColorPreference.setOnPreferenceClickListener(this);
		
		mGridColorPreference = findPreference(getString(R.string.grid_color_int));
		mGridColorPreference.setOnPreferenceClickListener(this);
		mGridOutlineColorPreference = findPreference(getString(R.string.grid_outline_color_int));
		mGridOutlineColorPreference.setOnPreferenceClickListener(this);
		mMaxRainColorPreference = findPreference(getString(R.string.max_rain_color_int));
		mMaxRainColorPreference.setOnPreferenceClickListener(this);
		mMinRainColorPreference = findPreference(getString(R.string.min_rain_color_int));
		mMinRainColorPreference.setOnPreferenceClickListener(this);
		mAboveFreezingColorPreference = findPreference(getString(R.string.above_freezing_color_int));
		mAboveFreezingColorPreference.setOnPreferenceClickListener(this);
		mBelowFreezingColorPreference = findPreference(getString(R.string.below_freezing_color_int));
		mBelowFreezingColorPreference.setOnPreferenceClickListener(this);

		setResult(Activity.RESULT_CANCELED);
	}

	private void loadWidgetSettingsFromProvider(ContentResolver resolver, Uri widgetUri, Editor editor) {
		Cursor widgetSettingsCursor = resolver.query(
				Uri.withAppendedPath(widgetUri, AixWidgets.TWIG_SETTINGS),
				null, null, null, null);
		
		if (widgetSettingsCursor != null) {
			if (widgetSettingsCursor.moveToFirst()) {
				do {
					String key = widgetSettingsCursor.getString(AixSettingsColumns.KEY_COLUMN);
					String value = widgetSettingsCursor.getString(AixSettingsColumns.VALUE_COLUMN);
					
					if (!TextUtils.isEmpty(key) && !TextUtils.isEmpty(value)) {
						if (key.endsWith("bool")) {
							editor.putBoolean(key, Boolean.parseBoolean(value));
						} else if (key.endsWith("int")) {
							editor.putInt(key, Integer.parseInt(value));
						} else if (key.endsWith("string")) {
							editor.putString(key, value);
						}
					}
				} while (widgetSettingsCursor.moveToNext());
			}
			widgetSettingsCursor.close();
		}
	}
	
	private void saveWidgetSettingsToProvider(SharedPreferences settings, ContentValues values) {
		Map<String, Object> settingMap = (Map<String, Object>)settings.getAll();
		for (Entry<String, Object> setting : settingMap.entrySet()) {
			if (!setting.getKey().startsWith("global_")) {
				String value = null;
				if (setting.getKey().endsWith("bool")) {
					value = Boolean.toString((Boolean)setting.getValue());
					Log.d(TAG, "Saving bool " + setting.getKey() + " has value " + value);
				} else if (setting.getKey().endsWith("int")) {
					value = Integer.toString((Integer)setting.getValue());
					Log.d(TAG, "Saving int " + setting.getKey() + " has value " + value);
				} else if (setting.getKey().endsWith("string")) {
					value = (String)setting.getValue();
					Log.d(TAG, "Saving string " + setting.getKey() + " has value " + value);
				}
				if (value != null) {
					values.put(setting.getKey(), value);
				}
			}
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case SELECT_LOCATION: {
			if (resultCode == Activity.RESULT_OK) {
				Uri locationUri = Uri.parse(data.getStringExtra("location"));
				Cursor cursor = getContentResolver().query(
						locationUri, null, null, null, null);
				if (cursor != null) {
					if (cursor.moveToFirst()) {
						mLocationId = cursor.getLong(AixLocationsColumns.LOCATION_ID_COLUMN);
						mLocationName = cursor.getString(AixLocationsColumns.TITLE_COLUMN);
					}
					cursor.close();
				}
			}
			break;
		}
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
				.unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (Intent.ACTION_EDIT.equals(getIntent().getAction())) {
			mAddWidgetButton.setText(getString(R.string.apply_settings));
		}
		
		if (mLocationName != null) {
			mLocationPreference.setSummary(mLocationName);
		}

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		
		onSharedPreferenceChanged(settings, getString(R.string.temperature_units_string));
		onSharedPreferenceChanged(settings, getString(R.string.precipitation_units_string));
		onSharedPreferenceChanged(settings, getString(R.string.precipitation_scaling_string));
		onSharedPreferenceChanged(settings, getString(R.string.top_text_visibility_string));
		onSharedPreferenceChanged(settings, getString(R.string.border_thickness_string));
		onSharedPreferenceChanged(settings, getString(R.string.border_rounding_string));
		
		settings.registerOnSharedPreferenceChangeListener(this);
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals(getString(R.string.temperature_units_string))) {
			String temperatureUnit = sharedPreferences.getString(
					getString(R.string.temperature_units_string), "1");
			mTemperatureUnitPreference.setSummary(
					temperatureUnit.equals("2")
					? getString(R.string.temperature_units_fahrenheit)
					: getString(R.string.temperature_units_celsius));
		} else if (key.equals(getString(R.string.precipitation_units_string))) {
			String precipitationUnit = sharedPreferences.getString(
					getString(R.string.precipitation_units_string), "1");
			
			mPrecipitationUnitPreference.setSummary(
					precipitationUnit.equals("2")
					? getString(R.string.precipitation_units_inches)
					: getString(R.string.precipitation_units_mm));
			mPrecipitationScalingPreference.setDialogTitle(
					precipitationUnit.equals("2")
					? getString(R.string.precipitation_scaling_title_inches)
					: getString(R.string.precipitation_scaling_title_mm));
			mPrecipitationScalingPreference.setTitle(
					precipitationUnit.equals("2")
					? getString(R.string.precipitation_scaling_title_inches)
					: getString(R.string.precipitation_scaling_title_mm));
			
			String precipitationValue = precipitationUnit.equals("2") ? "0.05" : "1";

			mPrecipitationScalingPreference.setSummary(
					sharedPreferences.getString(
							getString(R.string.precipitation_scaling_string), "1")
					+ " " + (precipitationUnit.equals("2") ? "in" : "mm"));
			mPrecipitationScalingPreference.setText(precipitationValue);
		} else if (key.equals(getString(R.string.precipitation_scaling_string))) {
			String precipitationUnit = sharedPreferences.getString(
					getString(R.string.precipitation_units_string), "1");
			String precipitationScaling = sharedPreferences.getString(
					getString(R.string.precipitation_scaling_string),
					precipitationUnit.equals("2")
					? getString(R.string.precipitation_scaling_inches_default)
					: getString(R.string.precipitation_scaling_mm_default));
			
			mPrecipitationScalingPreference.setSummary(
					precipitationScaling + " " + (precipitationUnit.equals("2") ? "in" : "mm"));
		} else if (key.equals(getString(R.string.top_text_visibility_string))) {
			String[] topTextVisibilityStrings = getResources().
					getStringArray(R.array.top_text_visibility_readable);
			int index = Integer.parseInt(sharedPreferences.getString(
					getString(R.string.top_text_visibility_string),
					getString(R.string.top_text_visibility_default))) - 1;
			mTopTextVisibilityPreference.setSummary(topTextVisibilityStrings[index]);
		} else if (key.equals(getString(R.string.border_thickness_string))) {
			String borderThickness = sharedPreferences.getString(
					getString(R.string.border_thickness_string),
					getString(R.string.border_thickness_default));
			mBorderThicknessPreference.setSummary(borderThickness + "px");
			mBorderThicknessPreference.setText(borderThickness);
		} else if (key.equals(getString(R.string.border_rounding_string))) {
			String borderRounding = sharedPreferences.getString(
					getString(R.string.border_rounding_string),
					getString(R.string.border_rounding_default));
			mBorderRoundingPreference.setSummary(borderRounding + "px");
			mBorderRoundingPreference.setText(borderRounding);
		}
		
	}

	@Override
	public void onClick(View v) {
		if (v == mAddWidgetButton) {
			if (mLocationId == -1) {
				Toast.makeText(this, getString(R.string.must_select_location), Toast.LENGTH_SHORT).show();
			} else {
				Intent intent = getIntent();
				String action = intent.getAction();
				
				SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
				
				ContentValues widgetSettings = new ContentValues();
				saveWidgetSettingsToProvider(settings, widgetSettings);
				
				ContentValues viewValues = new ContentValues();
				viewValues.put(AixViewsColumns.LOCATION, mLocationId);
				viewValues.put(AixViewsColumns.TYPE, AixViewsColumns.TYPE_DETAILED);
				
				ContentResolver resolver = getContentResolver();
				
				if (Intent.ACTION_EDIT.equals(action)) {
					resolver.insert(Uri.withAppendedPath(mWidgetUri, AixWidgets.TWIG_SETTINGS), widgetSettings);
					resolver.update(mViewUri, viewValues, null, null);
					
					Intent updateIntent = new Intent(AixService.ACTION_UPDATE_WIDGET, mWidgetUri, getApplicationContext(), AixService.class);
					startService(updateIntent);
					setResult(Activity.RESULT_OK);
				} else {
					int appWidgetId = -1;
					Bundle extras = intent.getExtras();
					if (extras != null) {
						appWidgetId = extras.getInt(
								AppWidgetManager.EXTRA_APPWIDGET_ID,
								AppWidgetManager.INVALID_APPWIDGET_ID);
						if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
							Uri viewUri = resolver.insert(AixViews.CONTENT_URI, viewValues);
							if (viewUri != null) {
								// Get the size of the widget
								//AppWidgetManager mgr = AppWidgetManager.getInstance(getApplicationContext());
								//String widgetClassName = mgr.getAppWidgetInfo(appWidgetId).provider.getShortClassName();
								
								ContentValues widgetValues = new ContentValues();
								widgetValues.put(BaseColumns._ID, appWidgetId);
								
								//if (widgetClassName.equals(".AixWidget")) {
									widgetValues.put(AixWidgetsColumns.SIZE, AixWidgetsColumns.SIZE_LARGE_TINY);
								//} else if (widgetClassName.equals(".widget.AixWidgetLargeSmall")) {
								//	widgetValues.put(AixWidgetsColumns.SIZE, AixWidgetsColumns.SIZE_LARGE_SMALL);
								//}
								
								widgetValues.put(AixWidgetsColumns.VIEWS, viewUri.getLastPathSegment());
								mWidgetUri = resolver.insert(AixWidgets.CONTENT_URI, widgetValues);
								
								if (mWidgetUri != null) {
									resolver.insert(Uri.withAppendedPath(mWidgetUri, AixWidgets.TWIG_SETTINGS), widgetSettings);
									
									Intent updateIntent = new Intent(AixService.ACTION_UPDATE_WIDGET, mWidgetUri, getApplicationContext(), AixService.class);
									startService(updateIntent);
									setResult(Activity.RESULT_OK);
									
									Intent resultIntent = new Intent();
									resultIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
									setResult(Activity.RESULT_OK, resultIntent);
								}
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
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		
		if (preference == mLocationPreference) {
			Intent intent = new Intent(AixConfigure.this, AixLocationSelectionActivity.class);
			startActivityForResult(intent, SELECT_LOCATION);
			return true;
		} else {
			final Resources resources = getResources();
			
			Map<String, Integer> colorMap = new HashMap<String, Integer>() {{
				put(getString(R.string.border_color_int), resources.getColor(R.color.border_default));
				put(getString(R.string.background_color_int), resources.getColor(R.color.background_default));
				put(getString(R.string.text_color_int), resources.getColor(R.color.text_default));
				put(getString(R.string.pattern_color_int), resources.getColor(R.color.pattern_default));
				put(getString(R.string.day_color_int), resources.getColor(R.color.day_default));
				put(getString(R.string.night_color_int), resources.getColor(R.color.night_default));
				put(getString(R.string.grid_color_int), resources.getColor(R.color.grid_default));
				put(getString(R.string.grid_outline_color_int), resources.getColor(R.color.grid_outline_default));
				put(getString(R.string.max_rain_color_int), resources.getColor(R.color.maximum_rain_default));
				put(getString(R.string.min_rain_color_int), resources.getColor(R.color.minimum_rain_default));
				put(getString(R.string.above_freezing_color_int), resources.getColor(R.color.above_freezing_default));
				put(getString(R.string.below_freezing_color_int), resources.getColor(R.color.below_freezing_default));
			}};
			
			if (colorMap.containsKey(preference.getKey())) {
				int color = settings.getInt(preference.getKey(), colorMap.get(preference.getKey()));
				ColorPickerView.showColorPickerDialog(
						this, getLayoutInflater(), preference.getKey(), color, this);
				return true;
			}
		}
		
		return false;
	}

	@Override
	public void colorSelected(String colorId, int color) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		Editor editor = settings.edit();
		editor.putInt(colorId, color);
		editor.commit();
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		if (preference == mPrecipitationScalingPreference) {
			try {
				float f = Float.parseFloat((String)newValue);
				if (f > 0.0f && f <= 100.0f) {
					return true;
				} else {
					Toast.makeText(getApplicationContext(),
							getString(R.string.precipitation_units_invalid_range_toast),
							Toast.LENGTH_SHORT).show();
				}
			} catch (NumberFormatException e) {
				Toast.makeText(getApplicationContext(),
						getString(R.string.precipitation_units_invalid_number_toast),
						Toast.LENGTH_SHORT).show();
			}
		} else if (preference == mBorderThicknessPreference) {
			try {
				float f = Float.parseFloat((String)newValue);
				if (f >= 0.0f && f <= 20.0f) {
					return true;
				} else {
					Toast.makeText(getApplicationContext(),
							getString(R.string.border_thickness_invalid_range_toast),
							Toast.LENGTH_SHORT).show();
				}
			} catch (NumberFormatException e) {
				Toast.makeText(getApplicationContext(),
						getString(R.string.border_thickness_invalid_number_toast),
						Toast.LENGTH_SHORT).show();
			}
		} else if (preference == mBorderRoundingPreference) {
			try {
				float f = Float.parseFloat((String)newValue);
				if (f >= 0.0f && f <= 20.0f) {
					return true;
				} else {
					Toast.makeText(getApplicationContext(),
							getString(R.string.border_rounding_invalid_range_toast),
							Toast.LENGTH_SHORT).show();
				}
			} catch (NumberFormatException e) {
				Toast.makeText(getApplicationContext(),
						getString(R.string.border_rounding_invalid_number_toast),
						Toast.LENGTH_SHORT).show();
			}
		}

		return false;
	}
	
}