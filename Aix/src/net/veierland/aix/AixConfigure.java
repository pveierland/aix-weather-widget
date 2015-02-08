package net.veierland.aix;

import java.util.Map;
import java.util.Map.Entry;

import net.veierland.aix.AixProvider.AixIntervalDataForecasts;
import net.veierland.aix.AixProvider.AixLocations;
import net.veierland.aix.AixProvider.AixLocationsColumns;
import net.veierland.aix.AixProvider.AixPointDataForecasts;
import net.veierland.aix.AixProvider.AixSettingsColumns;
import net.veierland.aix.AixProvider.AixViews;
import net.veierland.aix.AixProvider.AixViewsColumns;
import net.veierland.aix.AixProvider.AixWidgets;
import net.veierland.aix.AixProvider.AixWidgetsColumns;
import android.app.Activity;
import android.app.Dialog;
import android.appwidget.AppWidgetManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class AixConfigure extends PreferenceActivity implements View.OnClickListener, Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener, OnSharedPreferenceChangeListener {

	private static final String TAG = "AixConfigure";
	
	public static Dialog mDialog;
	
	private long mLocationId = -1;
	private String mLocationName = null;
	
	private Button mAddWidgetButton = null;
	private Preference mLocationPreference = null;
	
	private Preference mTemperatureUnitPreference = null;
	private Preference mPrecipitationUnitPreference = null;
	private EditTextPreference mPrecipitationScalingPreference = null;
	
	private ListPreference mTopTextVisibilityPreference = null;
	
	private ListPreference mProviderPreference = null;
	
	private EditTextPreference mBorderThicknessPreference = null;
	private EditTextPreference mBorderRoundingPreference = null;
	
	private Uri mWidgetUri = null, mViewUri = null;
	
	private final static int SET_UNITS = 0;
	private final static int CHANGE_COLORS = 1;
	private final static int SYSTEM_SETTINGS = 2;
	private final static int SELECT_LOCATION = 3;
	
	private boolean mProviderChanged;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setResult(Activity.RESULT_CANCELED);
		
		Intent intent = getIntent();
		if (intent == null) {
			finish();
			return;
		}
		
		String action = intent.getAction();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		
		if (Intent.ACTION_EDIT.equals(action)) {
			ContentResolver resolver = getContentResolver();
			mWidgetUri = intent.getData();
			
			if (mWidgetUri == null) {
				finish();
				return;
			}
			
			Cursor widgetCursor = resolver.query(mWidgetUri, null, null, null, null);
				
			if (widgetCursor != null) {
				if (widgetCursor.moveToFirst()) {
					// TODO This will be changed in the future. Assuming 1 view / widget atm.
					mViewUri = ContentUris.withAppendedId(AixViews.CONTENT_URI,
							widgetCursor.getLong(AixWidgetsColumns.VIEWS_COLUMN));
				}
				widgetCursor.close();
			}
			
			if (mViewUri == null) {
				Toast.makeText(this, "Error: Please recreate widget", Toast.LENGTH_SHORT).show();
				finish();
				return;
			}
			
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
			
			
			clearSettings(settings);
			
//			editor.putBoolean(getString(R.string.awake_only_bool), settings.getBoolean(getString(R.string.awake_only_bool), false));
//			editor.putBoolean(getString(R.string.wifi_only_bool), settings.getBoolean(getString(R.string.wifi_only_bool), false));
//			editor.putString(getString(R.string.update_rate_string), settings.getString(getString(R.string.update_rate_string), "0"));
//			editor.putString(getString(R.string.provider_string), settings.getString(getString(R.string.provider_string), "1"));
			Editor editor = settings.edit();
			loadWidgetSettingsFromProvider(resolver, mWidgetUri, editor);
			editor.commit();
		} else {
			loadSettingsBackup(settings);
		}
		
		setContentView(R.layout.aix_configure);
		addPreferencesFromResource(R.xml.aix_configuration);
		
		mLocationPreference = findPreference("select_location");
		mLocationPreference.setOnPreferenceClickListener(this);
		
		mAddWidgetButton = (Button) findViewById(R.id.add_widget_button);
		mAddWidgetButton.setOnClickListener(this);
		
		mTemperatureUnitPreference = findPreference(getString(R.string.temperature_units_string));
		mPrecipitationUnitPreference = findPreference(getString(R.string.precipitation_units_string));
		mPrecipitationUnitPreference.setOnPreferenceChangeListener(this);
		mPrecipitationScalingPreference = (EditTextPreference)findPreference(
				getString(R.string.precipitation_scaling_string));
		mPrecipitationScalingPreference.setOnPreferenceChangeListener(this);
		
		mTopTextVisibilityPreference = (ListPreference)findPreference(
				getString(R.string.top_text_visibility_string));
		
		mBorderThicknessPreference = (EditTextPreference)findPreference(
				getString(R.string.border_thickness_string));
		mBorderThicknessPreference.setOnPreferenceChangeListener(this);
		mBorderRoundingPreference = (EditTextPreference)findPreference(
				getString(R.string.border_rounding_string));
		mBorderRoundingPreference.setOnPreferenceChangeListener(this);
		
		mProviderPreference = (ListPreference)findPreference(getString(R.string.provider_string));
		mProviderPreference.setOnPreferenceChangeListener(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.config_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.help:
			Intent helpIntent = new Intent(this, AixIntro.class);
			startActivity(helpIntent);
			return true;
		case R.id.donate:
			Intent donateIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://market.android.com/details?id=net.veierland.aixd"));
			startActivity(donateIntent);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
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
			if (!setting.getKey().startsWith("global_") && !setting.getKey().startsWith("backup_")) {
				String value = null;
				if (setting.getKey().endsWith("bool")) {
					value = Boolean.toString((Boolean)setting.getValue());
				} else if (setting.getKey().endsWith("int")) {
					value = Integer.toString((Integer)setting.getValue());
				} else if (setting.getKey().endsWith("string")) {
					value = (String)setting.getValue();
				}
				if (value != null) {
					values.put(setting.getKey(), value);
				}
			}
		}
	}
	
	private void clearSettings(SharedPreferences settings) {
		Editor editor = settings.edit();
		Map<String, Object> settingMap = (Map<String, Object>)settings.getAll();
		for (Entry<String, Object> setting : settingMap.entrySet()) {
			String key = setting.getKey();
			Object value = setting.getValue();
			if (!key.startsWith("global")) {
				editor.remove(key);
			}
		}
		editor.commit();
	}
	
	private void clearWidgetDrawState(SharedPreferences settings, long appWidgetId) {
		Editor editor = settings.edit();
		editor.remove("global_widget_" + appWidgetId);
		editor.commit();
	}
	
	private void clearWidgetStates(SharedPreferences settings) {
		Editor editor = settings.edit();
		Map<String, Object> settingMap = (Map<String, Object>)settings.getAll();
		for (Entry<String, Object> setting : settingMap.entrySet()) {
			String key = setting.getKey();
			Object value = setting.getValue();
			if (key.startsWith("global_widget")) {
				editor.remove(key);
			}
		}
		editor.commit();
	}
	
	private void saveSettingsBackup(SharedPreferences settings) {
		Editor editor = settings.edit();
		Map<String, Object> settingMap = (Map<String, Object>)settings.getAll();
		for (Entry<String, Object> setting : settingMap.entrySet()) {
			String key = setting.getKey();
			Object value = setting.getValue();
			if (!key.startsWith("global") && key.startsWith("backup")) {
				editor.remove(key);
			}
		}
		editor.commit();
		
		editor = settings.edit();
		settingMap = (Map<String, Object>)settings.getAll();
		for (Entry<String, Object> setting : settingMap.entrySet()) {
			String key = setting.getKey();
			Object value = setting.getValue();
			
			if (!key.startsWith("global")) {
				if (key.endsWith("bool")) {
					editor.putBoolean("backup_" + key, (Boolean)value);
				} else if (key.endsWith("int")) {
					editor.putInt("backup_" + key, (Integer)value);
				} else if (key.endsWith("string")) {
					editor.putString("backup_" + key, (String)value);
				}
			}
		}
		editor.commit();
	}

	private void loadSettingsBackup(SharedPreferences settings) {
		
		Editor editor = settings.edit();
		Map<String, Object> settingMap = (Map<String, Object>)settings.getAll();
		for (Entry<String, Object> setting : settingMap.entrySet()) {
			String key = setting.getKey();
			Object value = setting.getValue();
			if (!key.startsWith("global") && !key.startsWith("backup")) {
				editor.remove(key);
			}
		}
		editor.commit();
		
		editor = settings.edit();
		settingMap = (Map<String, Object>)settings.getAll();
		for (Entry<String, Object> setting : settingMap.entrySet()) {
			String key = setting.getKey();
			Object value = setting.getValue();
			
			if (!key.startsWith("global") && key.startsWith("backup")) {
				if (key.endsWith("bool")) {
					editor.putBoolean(key.substring(7), (Boolean)value);
				} else if (key.endsWith("int")) {
					editor.putInt(key.substring(7), (Integer)value);
				} else if (key.endsWith("string")) {
					editor.putString(key.substring(7), (String)value);
				}
			}
		}
		editor.commit();
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
		
		if (mDialog != null) {
			mDialog.dismiss();
		}
		
		Intent intent = getIntent();
		String action = intent.getAction();
		
		if (Intent.ACTION_EDIT.equals(action) && mWidgetUri != null) {
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
			
			ContentValues widgetSettings = new ContentValues();
			saveWidgetSettingsToProvider(settings, widgetSettings);
			
			ContentValues viewValues = new ContentValues();
			viewValues.put(AixViewsColumns.LOCATION, mLocationId);
			viewValues.put(AixViewsColumns.TYPE, AixViewsColumns.TYPE_DETAILED);
			
			ContentResolver resolver = getContentResolver();
			
			resolver.insert(Uri.withAppendedPath(mWidgetUri, AixWidgets.TWIG_SETTINGS), widgetSettings);
			resolver.update(mViewUri, viewValues, null, null);
			
			saveSettingsBackup(settings);
			
			Intent updateIntent;
			
			if (mProviderChanged) {
				clearWidgetStates(settings);
				updateIntent = new Intent(AixService.ACTION_UPDATE_ALL, null, getApplicationContext(), AixService.class);
			} else {
				updateIntent = new Intent(AixService.ACTION_UPDATE_WIDGET, mWidgetUri, getApplicationContext(), AixService.class);
			}
			
			startService(updateIntent);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		if (Intent.ACTION_EDIT.equals(getIntent().getAction())) {
			findViewById(R.id.bottomBar).setVisibility(View.GONE);
		}
		
		mProviderChanged = false;
		
		if (mLocationId != -1) {
			Cursor locationCursor = getContentResolver().query(
					AixLocations.CONTENT_URI,
					null, BaseColumns._ID + "=" + mLocationId, null, null);
			if (locationCursor != null) {
				if (locationCursor.moveToFirst()) {
					mLocationName = locationCursor.getString(AixLocationsColumns.TITLE_COLUMN);
				}
				locationCursor.close();
			}
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
			
			String precipitationScaling = sharedPreferences.getString(
					getString(R.string.precipitation_scaling_string),
					precipitationUnit.equals("2")
					? getString(R.string.precipitation_scaling_inches_default)
					: getString(R.string.precipitation_scaling_mm_default));

			mPrecipitationScalingPreference.setSummary(
					precipitationScaling + " " + (precipitationUnit.equals("2") ? "in" : "mm"));
			mPrecipitationScalingPreference.setText(precipitationScaling);
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
			mPrecipitationScalingPreference.setText(precipitationScaling);
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
		if (v != mAddWidgetButton) return;
		if (mLocationId == -1) {
			Toast.makeText(this, getString(R.string.must_select_location), Toast.LENGTH_SHORT).show();
			return;
		}
		
		Intent intent = getIntent();
		String action = intent.getAction();
				
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
				
		ContentValues widgetSettings = new ContentValues();
		saveWidgetSettingsToProvider(settings, widgetSettings);				
		
		ContentValues viewValues = new ContentValues();
		viewValues.put(AixViewsColumns.LOCATION, mLocationId);
		viewValues.put(AixViewsColumns.TYPE, AixViewsColumns.TYPE_DETAILED);
		
		ContentResolver resolver = getContentResolver();
		
		int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
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
					
					Intent resultIntent = new Intent();
					resultIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
					setResult(Activity.RESULT_OK, resultIntent);
					finish();
				}
			} 
		}
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		if (preference == mLocationPreference) {
			Intent intent = new Intent(AixConfigure.this, AixLocationSelectionActivity.class);
			startActivityForResult(intent, SELECT_LOCATION);
			return true;
		}
		return false;
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
		} else if (preference == mPrecipitationUnitPreference) {
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			String oldUnit = settings.getString(
					getString(R.string.precipitation_units_string), "1");
			String newUnit = (String)newValue;
			
			if (!oldUnit.equals(newUnit)) {
				String defaultScalingValue =
					newUnit.equals("2")
					? getString(R.string.precipitation_scaling_inches_default)
					: getString(R.string.precipitation_scaling_mm_default);
			
				mPrecipitationScalingPreference.setSummary(
						defaultScalingValue + " " + (newUnit.equals("2") ? "in" : "mm"));
				mPrecipitationScalingPreference.setText(defaultScalingValue);
			}
			
			return true;
		} else if (preference == mProviderPreference) {
			ContentResolver resolver = getContentResolver();
			resolver.delete(AixPointDataForecasts.CONTENT_URI, null, null);
			resolver.delete(AixIntervalDataForecasts.CONTENT_URI, null, null);
			
			ContentValues values = new ContentValues();
			values.put(AixLocationsColumns.LAST_FORECAST_UPDATE, 0);
			values.put(AixLocationsColumns.FORECAST_VALID_TO, 0);
			values.put(AixLocationsColumns.NEXT_FORECAST_UPDATE, 0);
			resolver.update(AixLocations.CONTENT_URI, values, null, null);
			
			mProviderChanged = true;
			return true;
		}

		return false;
	}
	
}