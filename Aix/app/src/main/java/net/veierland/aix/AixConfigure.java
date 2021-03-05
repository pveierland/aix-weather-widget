package net.veierland.aix;

import net.veierland.aix.AixProvider.AixViews;
import net.veierland.aix.AixProvider.AixWidgets;
import net.veierland.aix.util.AixLocationInfo;
import net.veierland.aix.util.AixWidgetInfo;
import android.app.Activity;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ContentUris;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class AixConfigure extends PreferenceActivity
		implements OnSharedPreferenceChangeListener,
				   Preference.OnPreferenceClickListener,
				   Preference.OnPreferenceChangeListener,
				   View.OnClickListener
{

	private static final String TAG = "AixConfigure";

	private final static int SELECT_LOCATION = 1;
	private final static int DEVICE_PROFILES = 2;
	
	public final static int EXIT_CONFIGURATION = 77;
	
	private AixWidgetInfo mAixWidgetInfo = null;
	private AixSettings mAixSettings = null;
	
	private Button mAddWidgetButton;
	
	private EditTextPreference mBorderThicknessPreference; 
	private EditTextPreference mBorderRoundingPreference;
	private EditTextPreference mPrecipitationScalingPreference;
	
	private Preference mDeviceProfilePreference;
	private Preference mLocationPreference;
	private Preference mPrecipitationUnitPreference;
	private Preference mProviderPreference;
	private Preference mTemperatureUnitPreference;
	private Preference mTopTextVisibilityPreference;
	//private Preference mUpdateRatePreference;
	
	private boolean mActionEdit = false;
	private boolean mActivateCalibrationMode = false;

	@SuppressWarnings("deprecation") // findPreference is deprecated
	private void initializePreferences() {
		mAddWidgetButton = (Button) findViewById(R.id.add_widget_button);

		mBorderThicknessPreference = (EditTextPreference)findPreference(getString(R.string.border_thickness_string));
		mBorderRoundingPreference = (EditTextPreference)findPreference(getString(R.string.border_rounding_string));
		mPrecipitationScalingPreference = (EditTextPreference)findPreference(getString(R.string.precipitation_scaling_string));
		
		mDeviceProfilePreference = findPreference(getString(R.string.device_profiles_key));
		mLocationPreference = findPreference(getString(R.string.location_settings_key));
		mPrecipitationUnitPreference = findPreference(getString(R.string.precipitation_units_string));
		mProviderPreference = findPreference(getString(R.string.preference_provider_string));
		mTemperatureUnitPreference = findPreference(getString(R.string.temperature_units_string));
		mTopTextVisibilityPreference = findPreference(getString(R.string.top_text_visibility_string));
		//mUpdateRatePreference = findPreference(getString(R.string.update_rate_string));
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case SELECT_LOCATION: {
			if (resultCode == Activity.RESULT_OK) {
				Uri locationUri = Uri.parse(data.getStringExtra("location"));
				try
				{
					AixLocationInfo aixLocationInfo = AixLocationInfo.build(this, locationUri);
					mAixWidgetInfo.setViewInfo(aixLocationInfo, AixViews.TYPE_DETAILED);
					Log.d(TAG, "onActivityResult(): locationInfo=" + aixLocationInfo);
				}
				catch (Exception e)
				{
					Toast.makeText(this, "Failed to set up location info", Toast.LENGTH_SHORT).show();
					Log.d(TAG, "onActivityResult(): Failed to get location data.");
					e.printStackTrace();
				}
			}
			break;
		}
		case DEVICE_PROFILES:
		{
			if (resultCode == EXIT_CONFIGURATION)
			{
				mActivateCalibrationMode = true;
				
				boolean editMode = Intent.ACTION_EDIT.equals(getIntent().getAction());
				
				if (editMode)
				{
					int activeCalibrationTarget = mAixSettings.getCalibrationTarget();
					
					boolean isProviderModified = mAixSettings.isProviderPreferenceModified();
					boolean globalSettingModified = mAixSettings.saveAllPreferences(mActivateCalibrationMode);
					
					if (activeCalibrationTarget != AppWidgetManager.INVALID_APPWIDGET_ID)
					{
						// Redraw the currently active calibration widget
						AixService.enqueueWork(getApplicationContext(), new Intent(
								AixService.ACTION_UPDATE_WIDGET,
								ContentUris.withAppendedId(AixWidgets.CONTENT_URI, activeCalibrationTarget),
								this, AixService.class));
					}
					
					Uri widgetUri = mAixWidgetInfo.getWidgetUri();
					
					if (isProviderModified || globalSettingModified)
					{
						AixService.enqueueWork(
								getApplicationContext(),
								new Intent(isProviderModified
										? AixService.ACTION_UPDATE_ALL_PROVIDER_CHANGE
										: AixService.ACTION_UPDATE_ALL,
									widgetUri, this, AixService.class));
					}
					else
					{
						AixService.enqueueWork(
								getApplicationContext(),
								new Intent(AixService.ACTION_UPDATE_WIDGET,
										widgetUri, this, AixService.class));
					}
					
					finish();
				}
			}
		}
		}
	}

	@Override
	public void onClick(View v) {
		if (v == mAddWidgetButton)
		{
			if (mAixWidgetInfo.getViewInfo() == null || mAixWidgetInfo.getViewInfo().getLocationInfo() == null)
			{
				Toast.makeText(this, getString(R.string.must_select_location), Toast.LENGTH_SHORT).show();
				return;
			}
			
			Log.d(TAG, "onClick(): " + mAixWidgetInfo.toString());
			mAixWidgetInfo.commit(this);
			Log.d(TAG, "onClick(): Committed=" + mAixWidgetInfo.toString());
			
			boolean isProviderModified = mAixSettings.isProviderPreferenceModified();
			boolean globalSettingModified = mAixSettings.saveAllPreferences(mActivateCalibrationMode);
			
			PendingIntent configurationIntent = AixUtils.buildConfigurationIntent(this, mAixWidgetInfo.getWidgetUri());
			AixUtils.updateWidgetRemoteViews(this, mAixWidgetInfo.getAppWidgetId(), "Loading Aix...", true, configurationIntent);
			
			Uri widgetUri = mAixWidgetInfo.getWidgetUri();
			
			if (isProviderModified || globalSettingModified)
			{
				AixService.enqueueWork(
						getApplicationContext(),
						new Intent(isProviderModified
									? AixService.ACTION_UPDATE_ALL_PROVIDER_CHANGE
									: AixService.ACTION_UPDATE_ALL,
								widgetUri, this, AixService.class));
			}
			else
			{
				AixService.enqueueWork(
						getApplicationContext(),
						new Intent(AixService.ACTION_UPDATE_WIDGET, widgetUri, this, AixService.class));
			}
			
			Intent resultIntent = new Intent();
			resultIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAixWidgetInfo.getAppWidgetId());
			setResult(Activity.RESULT_OK, resultIntent);
			
			finish();
		}
	}
	
	@SuppressWarnings("deprecation") // addPreferencesFromResource() is deprecated
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setResult(Activity.RESULT_CANCELED);
		
		if (getIntent() == null || getIntent().getAction() == null) {
			Toast.makeText(this, "Could not start configuration activity: Intent or action was null.", Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		
		mActionEdit = getIntent().getAction().equals(Intent.ACTION_EDIT);
		
		if (mActionEdit) {
			Uri widgetUri = getIntent().getData();
			
			if (widgetUri == null) {
				Toast.makeText(this, "Could not start configuration activity: Data was null. Remove and recreate the widget.", Toast.LENGTH_SHORT).show();
				finish();
				return;
			}
			
			try {
				mAixWidgetInfo = AixWidgetInfo.build(this, widgetUri);
			} catch (Exception e) {
				Toast.makeText(this, "Failed to get widget information from database. Try removing the widget and creating a new one.", Toast.LENGTH_SHORT).show();
				e.printStackTrace();
				finish();
				return;
			}
		} else {
			int appWidgetId = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
			
			if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
				Toast.makeText(this, "Could not start configuration activity: Missing AppWidgetId.", Toast.LENGTH_SHORT).show();
				finish();
				return;
			}
			
			AixUtils.deleteWidget(this, appWidgetId);
			
			mAixWidgetInfo = new AixWidgetInfo(appWidgetId, AixWidgets.SIZE_LARGE_TINY, null);
			
			Log.d(TAG, "Commit=" + mAixWidgetInfo.commit(this));
		}
		
		Log.d(TAG, "onCreate(): " + mAixWidgetInfo.toString());
		
		mAixSettings = AixSettings.build(this, mAixWidgetInfo);
		
		if (mActionEdit) {
			mAixSettings.initializePreferencesExistingWidget();
		} else {
			mAixSettings.initializePreferencesNewWidget();
		}
		
		setContentView(R.layout.activity_configure);
		addPreferencesFromResource(R.xml.aix_configuration);
		
		initializePreferences();
		setupListeners();
		
		mAddWidgetButton.setText(mActionEdit ? "Apply Changes" : "Add Widget");
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.config_menu, menu);
		return true;
	}
	
	private boolean onFloatPreferenceChange(Preference preference, Object newValue,
			float rangeMin, float rangeMax, int invalidNumberString, int invalidRangeString)
	{
		float f;
		
		try {
			f = Float.parseFloat((String)newValue);
		} catch (NumberFormatException e) {
			Toast.makeText(this, getString(invalidNumberString), Toast.LENGTH_SHORT).show();
			return false;
		}
		
		if ((f >= rangeMin) && (f <= rangeMax)) {
			return true;
		} else {
			Toast.makeText(this, getString(invalidRangeString), Toast.LENGTH_SHORT).show();
			return false;
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.help) {
			startActivity(new Intent(AixIntro.ACTION_SHOW_HELP, null, this, AixIntro.class));
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
	}
	
	private boolean onPrecipitationUnitPreferenceChange(
			Preference preference, Object newValue)
	{
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		
		String oldUnit = settings.getString(getString(R.string.precipitation_units_string), "1");
		String newUnit = (String)newValue;
		
		if (!oldUnit.equals(newUnit)) {
			// When changing the precipitation unit, reset the scaling to default value
			String defaultScalingValue =
				newUnit.equals("1")
				? getString(R.string.precipitation_scaling_mm_default)
				: getString(R.string.precipitation_scaling_inches_default);
			
			mPrecipitationScalingPreference.setSummary(
					defaultScalingValue + " " + (newUnit.equals("1") ? "mm" : "in"));
			mPrecipitationScalingPreference.setText(defaultScalingValue);
		}
		
		return true;
	}
	
	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		if (preference == mBorderRoundingPreference) {
			return onFloatPreferenceChange(preference, newValue, 0.0f, 20.0f,
					R.string.border_rounding_invalid_number_toast,
					R.string.border_rounding_invalid_range_toast);
		} else if (preference == mBorderThicknessPreference) {
			return onFloatPreferenceChange(preference, newValue, 0.0f, 20.0f,
					R.string.border_thickness_invalid_number_toast,
					R.string.border_thickness_invalid_range_toast);
		} else if (preference == mPrecipitationScalingPreference) {
			return onFloatPreferenceChange(preference, newValue, 0.000001f, 100.0f,
					R.string.precipitation_units_invalid_number_toast,
					R.string.precipitation_units_invalid_range_toast);
		} else if (preference == mPrecipitationUnitPreference) {
			return onPrecipitationUnitPreferenceChange(preference, newValue);
		}
		return false;
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		if (preference == mLocationPreference) {
			Intent intent = new Intent(AixConfigure.this, AixLocationSelectionActivity.class);
			startActivityForResult(intent, SELECT_LOCATION);
			return true;
		} else if (preference == mDeviceProfilePreference) {
			Intent intent = new Intent(AixConfigure.this, AixDeviceProfileActivity.class);
			intent.setAction(getIntent().getAction());
			intent.setData(mAixWidgetInfo.getWidgetUri());
			startActivityForResult(intent, DEVICE_PROFILES);
			return true;
		}
		return false;
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		String locationName = null;
		
		if (mAixWidgetInfo.getViewInfo() != null && mAixWidgetInfo.getViewInfo().getLocationInfo() != null)
		{
			AixLocationInfo locationInfo = mAixWidgetInfo.getViewInfo().getLocationInfo();
			if (locationInfo != null && locationInfo.getTitle() != null) {
				locationName = locationInfo.getTitle();
			}
		}
		mLocationPreference.setSummary(locationName);
		
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		settings.registerOnSharedPreferenceChangeListener(this);
		
		onSharedPreferenceChanged(settings, getString(R.string.border_rounding_string));
		onSharedPreferenceChanged(settings, getString(R.string.border_thickness_string));
		onSharedPreferenceChanged(settings, getString(R.string.precipitation_scaling_string));
		onSharedPreferenceChanged(settings, getString(R.string.precipitation_units_string));
		onSharedPreferenceChanged(settings, getString(R.string.preference_provider_string));
		onSharedPreferenceChanged(settings, getString(R.string.temperature_units_string));
		onSharedPreferenceChanged(settings, getString(R.string.top_text_visibility_string));
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals(getString(R.string.border_rounding_string))) {
			updateBorderRoundingPreference(sharedPreferences);
		}
		else if (key.equals(getString(R.string.border_thickness_string)))
		{
			updateBorderThicknessPreference(sharedPreferences);
		}
		else if (getString(R.string.precipitation_scaling_string).equals(key) ||
				 getString(R.string.precipitation_units_string).equals(key))
		{
			updatePrecipitationScalingPreference(sharedPreferences);
		}
		else if (key.equals(getString(R.string.preference_provider_string))) {
			updateProviderPreference(sharedPreferences);
		}
		else if (getString(R.string.temperature_units_string).equals(key))
		{
			updateTemperatureUnitPreference(sharedPreferences);
		}
		else if (getString(R.string.top_text_visibility_string).equals(key))
		{
			updateTopTextVisibilityPreference(sharedPreferences);
		}
	}
	
	private void setupListeners() {
		mAddWidgetButton.setOnClickListener(this);
		
		mDeviceProfilePreference.setOnPreferenceClickListener(this);
		mLocationPreference.setOnPreferenceClickListener(this);
		
		mBorderRoundingPreference.setOnPreferenceChangeListener(this);
		mBorderThicknessPreference.setOnPreferenceChangeListener(this);
		mPrecipitationScalingPreference.setOnPreferenceChangeListener(this);
		mPrecipitationUnitPreference.setOnPreferenceChangeListener(this);
	}
	
	private void updateBorderRoundingPreference(
			SharedPreferences sharedPreferences)
	{
		String borderRounding = sharedPreferences.getString(
				getString(R.string.border_rounding_string),
				getString(R.string.border_rounding_default));
		
		mBorderRoundingPreference.setSummary(borderRounding + "px");
		mBorderRoundingPreference.setText(borderRounding);
	}
	
	private void updateBorderThicknessPreference(
			SharedPreferences sharedPreferences)
	{
		String borderThickness = sharedPreferences.getString(
				getString(R.string.border_thickness_string),
				getString(R.string.border_thickness_default));
		
		mBorderThicknessPreference.setSummary(borderThickness + "px");
		mBorderThicknessPreference.setText(borderThickness);
	}
	
	private void updatePrecipitationScalingPreference(
			SharedPreferences sharedPreferences)
	{
		String precipitationUnit = sharedPreferences.getString(
				getString(R.string.precipitation_units_string), "1");
		
		String defaultPrecipitationScaling =
				precipitationUnit.equals("1")
				? getString(R.string.precipitation_scaling_mm_default)
				: getString(R.string.precipitation_scaling_inches_default);
		
		String precipitationScaling = sharedPreferences.getString(
				getString(R.string.precipitation_scaling_string),
				defaultPrecipitationScaling);

		String precipitationUnitString =
				precipitationUnit.equals("1") ? "mm" : "in";
		
		mPrecipitationUnitPreference.setSummary(
				precipitationUnit.equals("1")
				? getString(R.string.precipitation_units_mm)
				: getString(R.string.precipitation_units_inches));
		
		mPrecipitationScalingPreference.setDialogTitle(
				precipitationUnit.equals("1")
				? getString(R.string.precipitation_scaling_title_mm)
				: getString(R.string.precipitation_scaling_title_inches));
		
		mPrecipitationScalingPreference.setSummary(
				precipitationScaling + " " + precipitationUnitString);
		
		mPrecipitationScalingPreference.setText(precipitationScaling);
		
		mPrecipitationScalingPreference.setTitle(
				precipitationUnit.equals("1")
				? getString(R.string.precipitation_scaling_title_mm)
				: getString(R.string.precipitation_scaling_title_inches));
	}
	
	private void updateProviderPreference(SharedPreferences sharedPreferences) {
		String[] providerStrings = getResources().
				getStringArray(R.array.provider_readable);
		
		String providerDefaultValueString =
				getString(R.string.provider_auto);
		
		int index = Integer.parseInt(sharedPreferences.getString(
				getString(R.string.preference_provider_string),
				providerDefaultValueString)) - 1;
		// Subtract 1 since array is zero-indexed
		
		mProviderPreference.setSummary(providerStrings[index]);
	}
	
	private void updateTemperatureUnitPreference(
			SharedPreferences sharedPreferences)
	{
		String temperatureUnit = sharedPreferences.getString(
				getString(R.string.temperature_units_string), "1");
		
		mTemperatureUnitPreference.setSummary(
				temperatureUnit.equals("1")
				? getString(R.string.temperature_units_celsius)
				: getString(R.string.temperature_units_fahrenheit));
	}
	
	private void updateTopTextVisibilityPreference(
			SharedPreferences sharedPreferences)
	{
		String[] topTextVisibilityStrings = getResources().
				getStringArray(R.array.top_text_visibility_readable);
		
		String topTextVisibilityDefaultValueString =
				getString(R.string.top_text_visibility_default);
		
		int index = Integer.parseInt(sharedPreferences.getString(
				getString(R.string.top_text_visibility_string),
				topTextVisibilityDefaultValueString)) - 1;
		// Subtract 1 since array is zero-indexed
		
		mTopTextVisibilityPreference.setSummary(topTextVisibilityStrings[index]);
	}
	
}