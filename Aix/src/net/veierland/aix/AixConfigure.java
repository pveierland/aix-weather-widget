package net.veierland.aix;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.veierland.aix.AixProvider.AixLocationsColumns;
import net.veierland.aix.AixProvider.AixSettings;
import net.veierland.aix.AixProvider.AixSettingsColumns;
import net.veierland.aix.AixProvider.AixViews;
import net.veierland.aix.AixProvider.AixViewsColumns;
import net.veierland.aix.AixProvider.AixWidgetSettings;
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
import android.text.TextUtils;
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
				Cursor viewCursor = resolver.query(mViewUri, null, null, null, null);
				
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
		
		mBackgroundColorPreference = findPreference(getString(R.string.background_color_int));
		mBackgroundColorPreference.setOnPreferenceClickListener(this);
		mTextColorPreference = findPreference(getString(R.string.text_color_int));
		mTextColorPreference.setOnPreferenceClickListener(this);
		mLocationBackgroundColorPreference = findPreference(getString(R.string.location_background_color_int));
		mLocationBackgroundColorPreference.setOnPreferenceClickListener(this);
		mLocationTextColorPreference = findPreference(getString(R.string.location_text_color_int));
		mLocationTextColorPreference.setOnPreferenceClickListener(this);
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
		
		findPreference(getString(R.string.day_color_int)).setOnPreferenceClickListener(this);
		findPreference(getString(R.string.night_color_int)).setOnPreferenceClickListener(this);
		
		if (Intent.ACTION_EDIT.equals(action)) {
			if (mLocationName != null) {
				mLocationPreference.setSummary(mLocationName);
			}
			mAddWidgetButton.setText("Apply Settings");
		}

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
						findPreference("select_location").setSummary(cursor.getString(AixLocationsColumns.TITLE_COLUMN));
						mLocationId = cursor.getLong(AixLocationsColumns.LOCATION_ID_COLUMN);
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
								AppWidgetManager mgr = AppWidgetManager.getInstance(getApplicationContext());
								String widgetClassName = mgr.getAppWidgetInfo(appWidgetId).provider.getClassName();
								
								ContentValues widgetValues = new ContentValues();
								widgetValues.put(BaseColumns._ID, appWidgetId);
								
								//if (widgetClassName.equals("AixWidget")) {
									widgetValues.put(AixWidgetsColumns.SIZE, AixWidgetsColumns.SIZE_LARGE_TINY);
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
				put(getString(R.string.background_color_int), resources.getColor(R.color.background_default));
				put(getString(R.string.text_color_int), resources.getColor(R.color.text_default));
				put(getString(R.string.location_background_color_int), resources.getColor(R.color.location_background_default));
				put(getString(R.string.location_text_color_int), resources.getColor(R.color.location_text_default));
				put(getString(R.string.grid_color_int), resources.getColor(R.color.grid_default));
				put(getString(R.string.grid_outline_color_int), resources.getColor(R.color.grid_outline_default));
				put(getString(R.string.max_rain_color_int), resources.getColor(R.color.maximum_rain_default));
				put(getString(R.string.min_rain_color_int), resources.getColor(R.color.minimum_rain_default));
				put(getString(R.string.above_freezing_color_int), resources.getColor(R.color.above_freezing_default));
				put(getString(R.string.below_freezing_color_int), resources.getColor(R.color.below_freezing_default));
				put(getString(R.string.day_color_int), resources.getColor(R.color.day_default));
				put(getString(R.string.night_color_int), resources.getColor(R.color.night_default));
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
	
}