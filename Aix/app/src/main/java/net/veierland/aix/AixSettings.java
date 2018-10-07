package net.veierland.aix;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.veierland.aix.AixProvider.AixSettingsColumns;
import net.veierland.aix.AixProvider.AixWidgets;
import net.veierland.aix.util.AixWidgetInfo;
import android.appwidget.AppWidgetManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.Point;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;

public class AixSettings {
	
	private static final String TAG = "AixSettings";
	
	public final static int DEVICE_PROFILE_STATE_NONE = 0;
    public final static int DEVICE_PROFILE_STATE_USER_SUBMITTED = 1;
    public final static int DEVICE_PROFILE_STATE_RECOMMENDED = 2;

	public static final int CALIBRATION_STATE_INVALID = 0;
	public static final int CALIBRATION_STATE_HORIZONTAL = 1;
	public static final int CALIBRATION_STATE_VERTICAL = 2;
	public static final int CALIBRATION_STATE_FINISHED = 3;

	public static final String LANDSCAPE_HEIGHT = "global_lh";
	public static final String LANDSCAPE_WIDTH = "global_lw";
	public static final String PORTRAIT_HEIGHT = "global_ph";
	public static final String PORTRAIT_WIDTH = "global_pw";
	
	public final static int ORIENTATION_MODE_AUTO = 0;
	public final static int ORIENTATION_MODE_FIXED_PORTRAIT = 1;
	public final static int ORIENTATION_MODE_FIXED_LANDSCAPE = 2;
	
	public final static boolean PORTRAIT = false;
	public final static boolean LANDSCAPE = true;
    
	public final static boolean NO_CROP = false;
	public final static boolean CROP = true;
    
	public final static boolean KEEP_STATE = false;
	public final static boolean CLEAR_STATE = true;
    
	public final static boolean NO_TOUCH = false;
	public final static boolean TOUCH = true;
    
	public final static boolean GLOBAL = false;
	public final static boolean PREFERENCE = true;
    
	private Context mContext;
	private DisplayMetrics mDisplayMetrics;
	private SharedPreferences mSharedPreferences;
	private AixWidgetInfo mAixWidgetInfo;
	
	private boolean mAwakeOnly, mUseSpecificDimensions, mWifiOnly;
	private int mNumUpdateHours, mOrientationMode, mProvider;
	
	private AixSettings(
			Context context,
			DisplayMetrics displayMetrics,
			SharedPreferences sharedPreferences,
			AixWidgetInfo aixWidgetInfo)
	{
		mContext = context;
		mDisplayMetrics = displayMetrics;
		mSharedPreferences = sharedPreferences;
		mAixWidgetInfo = aixWidgetInfo;
	}
	
	public static AixSettings build(Context context, AixWidgetInfo widgetInfo)
	{
		DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		return new AixSettings(context, displayMetrics, sharedPreferences, widgetInfo);
	}
	
	public boolean getCachedAwakeOnly() {
		return mAwakeOnly;
	}
	
	public Context getContext() {
		return mContext;
	}

	private int getIntegerFromStringPreference(
			Editor editor, String key, int defaultValue)
	{
		int returnValue = defaultValue;
		
		try {
			String defaultString = Integer.toString(defaultValue);
			String valueString = mSharedPreferences.getString(key, defaultString);
			returnValue = Integer.parseInt(valueString);
		} catch (ClassCastException e) {
			editor.remove(key);
		} catch (NumberFormatException e) {
			editor.remove(key);
		}
		
		return returnValue;
	}
	
	public int getCachedNumUpdateHours() {
		return mNumUpdateHours;
	}
	
	public int getCachedOrientationMode() {
		return mOrientationMode;
	}
	
	public int getCachedProvider() {
		return mProvider;
	}
	
	public boolean getCachedUseSpecificDimensions() {
		return mUseSpecificDimensions;
	}
	
	public boolean getCachedWifiOnly() {
		return mWifiOnly;
	}
	
	public void loadSettings() {
		Editor editor = mSharedPreferences.edit();
		mNumUpdateHours = getIntegerFromStringPreference(editor, mContext.getString(R.string.update_rate_string), 0);
		mProvider = getIntegerFromStringPreference(editor, mContext.getString(R.string.provider_string), 1);
		editor.commit();
		
		mOrientationMode = mSharedPreferences.getInt(mContext.getString(R.string.orientationMode_int), 0);
		
		mAwakeOnly = mSharedPreferences.getBoolean(mContext.getString(R.string.awake_only_bool), false);
		mUseSpecificDimensions = mSharedPreferences.getBoolean(mContext.getString(R.string.useDeviceSpecificDimensions_bool), false);
		mWifiOnly = mSharedPreferences.getBoolean(mContext.getString(R.string.wifi_only_bool), false);
	}
	
	/* BEGIN SETTINGS BACKUP/RESTORE */
	
	public ContentValues[] buildContentValues(Map<String, ?> settingsMap) {
		ArrayList<ContentValues> settingList = new ArrayList<ContentValues>();

		for (Entry<String, ?> setting : settingsMap.entrySet()) {
			String key = setting.getKey();
			
			if (!key.startsWith("global") && !key.startsWith("backup")) {
				Object value = setting.getValue();
				String valueString = convertValueToString(key, value);
				
				if (valueString != null)
				{
					ContentValues contentValues = new ContentValues();
					contentValues.put(AixSettingsColumns.KEY, key);
					contentValues.put(AixSettingsColumns.VALUE, valueString);
					settingList.add(contentValues);
				}
			}
		}
		
		return settingList.toArray(new ContentValues[settingList.size()]);
	}
	
	private String convertValueToString(String key, Object value)
	{
		String valueString = null;
		try {
			if (key.endsWith("bool")) {
				valueString = Boolean.toString((Boolean)value);
			} else if (key.endsWith("int")) {
				valueString = Integer.toString((Integer)value);
			} else if (key.endsWith("long")) {
				valueString = Long.toString((Long)value);
			} else if (key.endsWith("string")) {
				valueString = (String)value;
			} else {
				throw new IllegalArgumentException();
			}
		} catch (ClassCastException e) {
			Log.d(TAG, String.format(
					"Failed to convert setting to string: ClassCastException. (key=%s,value=%s)",
					key, value.toString() ));
		} catch (IllegalArgumentException e) {
			Log.d(TAG, String.format(
					"Failed to convert setting to string: IllegalArgumentException. (key=%s,value=%s)",
					key, value.toString() ));
		}
		
		return valueString;
	}
	
	private void copyKey(Editor editor, String sourceKey, String destKey, Object value)
	{
		try {
			if (sourceKey.endsWith("bool")) {
				editor.putBoolean(destKey, (Boolean)value);
			} else if (sourceKey.endsWith("int")) {
				editor.putInt(destKey, (Integer)value);
			} else if (sourceKey.endsWith("long")) {
				editor.putLong(destKey, (Long)value);
			} else if (sourceKey.endsWith("string")) {
				editor.putString(destKey, (String)value);
			}
		}
		catch (ClassCastException e)
		{
			Log.d(TAG, String.format(
					"Failed to copy setting. (key=%s,value=%s)",
					sourceKey, value.toString() ));
		}
	}
	
	public Editor editClear(Editor editor, Map<String, ?> settingsMap) {
		for (Entry<String, ?> setting : settingsMap.entrySet()) {
			String key = setting.getKey();
			if (	  key.startsWith("global_temp") ||
					(!key.startsWith("global") && !key.startsWith("backup")))
			{
				editor.remove(key);
			}
		}
		
		return editor;
	}
	
	public Editor editClearBackup(Editor editor, Map<String, ?> settingsMap) {
		for (Entry<String, ?> setting : settingsMap.entrySet()) {
			String key = setting.getKey();
			if (key.startsWith("backup"))
			{
				editor.remove(key);
			}
		}
		
		return editor;
	}
	
	public Editor editCreateBackup(Editor editor, Map<String, ?> settingsMap)
	{
		for (Entry<String, ?> setting : settingsMap.entrySet())
		{
			String key = setting.getKey();
			
			if (!key.startsWith("global") && !key.startsWith("backup"))
			{
				String backupKey = "backup_" + key;
				Object value = setting.getValue();
				copyKey(editor, key, backupKey, value);
			}
		}
		
		return editor;
	}
	
	public Editor editLoadFromProvider(Editor editor) {
		Cursor widgetSettingsCursor = mContext.getContentResolver().query(
				Uri.withAppendedPath(mAixWidgetInfo.getWidgetUri(), AixWidgets.TWIG_SETTINGS),
				null, null, null, null);
		
		if (widgetSettingsCursor != null) {
			try {
				if (widgetSettingsCursor.moveToFirst()) {
					do {
						String key = widgetSettingsCursor.getString(AixSettingsColumns.KEY_COLUMN);
						String value = widgetSettingsCursor.getString(AixSettingsColumns.VALUE_COLUMN);
						
						if (!TextUtils.isEmpty(key) && !TextUtils.isEmpty(value)) {
							insertKey(editor, key, value);
						}
						else
						{
							Log.d(TAG, String.format(
									"Failed to load setting from provider. (key=%s,value=%s)",
									key, value));
						}
					} while (widgetSettingsCursor.moveToNext());
				}
			} finally {
				widgetSettingsCursor.close();
			}
		}
		
		return editor;
	}
	
	public Editor editLoadGlobalSettings(Editor editor, Map<String, ?> settingsMap) {
		for (Entry<String, ?> setting : settingsMap.entrySet()) {
			String key = setting.getKey();
			if (key.startsWith("global") && !key.startsWith("global_temp"))
			{
				String tempKey = "global_temp" + key.substring(6);
				Object value = setting.getValue();
				copyKey(editor, key, tempKey, value);
			}
		}
		
		return editor;
	}
	
	public Editor editRestoreBackup(Editor editor, Map<String, ?> settingsMap)
	{
		for (Entry<String, ?> setting : settingsMap.entrySet())
		{
			String key = setting.getKey();
			
			if (key.startsWith("backup"))
			{
				String destKey = key.substring(7);
				Object value = setting.getValue();
				copyKey(editor, key, destKey, value);
			}
		}
		
		return editor;
	}
	
	/*
	public Editor editSaveDeviceProfileSettings(Editor editor, Map<String, ?> settingsMap)
	{
		for (Entry<String, ?> setting : settingsMap.entrySet())
		{
			String key = setting.getKey();
			
			if (key.startsWith("global_temp_dp"))
			{
				String globalDpKey = "global" + key.substring(11);
				Object value = setting.getValue();
				copyKey(editor, key, globalDpKey, value);
			}
		}
		
		return editor;
	}
	*/
	
	public Editor editSaveGlobalSettings(Editor editor, Map<String, ?> settingsMap, Set<String> avoidKeys)
	{
		for (Entry<String, ?> setting : settingsMap.entrySet())
		{
			String key = setting.getKey();
			
			if (key.startsWith("global_temp") && (avoidKeys == null || !avoidKeys.contains(key)))
			{
				String globalKey = "global" + key.substring(11);
				Object value = setting.getValue();
				copyKey(editor, key, globalKey, value);
			}
		}
		
		return editor;
	}
	
	public void initializePreferencesExistingWidget()
	{
		Map<String, ?> settingsMap = mSharedPreferences.getAll();
		Editor editor = mSharedPreferences.edit();
		
		editClear(editor, settingsMap);
		editLoadFromProvider(editor);
		editLoadGlobalSettings(editor, settingsMap);
		editor.commit();
	}
	
	public void initializePreferencesNewWidget()
	{
		Map<String, ?> settingsMap = mSharedPreferences.getAll();
		Editor editor = mSharedPreferences.edit();
		
		editClear(editor, settingsMap);
		editRestoreBackup(editor, settingsMap);
		editLoadGlobalSettings(editor, settingsMap);
		editor.commit();
	}
	
	private void insertKey(Editor editor, String key, String value)
	{
		try {
			if (key.endsWith("bool")) {
				editor.putBoolean(key, Boolean.parseBoolean(value));
			} else if (key.endsWith("int")) {
				editor.putInt(key, Integer.parseInt(value));
			} else if (key.endsWith("long")) {
				editor.putLong(key, Long.parseLong(value));
			} else if (key.endsWith("string")) {
				editor.putString(key, value);
			}
		} catch (NumberFormatException e) {
			Log.d(TAG, String.format(
					"Failed to load setting from provider: NumberFormatException. (key=%s,value=%s)",
					key, value));
		}
	}
	
	private boolean isGlobalSettingsModified(Map<String, ?> settingsMap, Set<String> avoidKeys)
	{
		Map<String, Object> globalSettings = new HashMap<String, Object>();
		for (Entry<String, ?> setting : settingsMap.entrySet())
		{
			String key = setting.getKey();
			if (key.startsWith("global") && !key.startsWith("global_temp"))
			{
				globalSettings.put(key, setting.getValue());
			}
		}
		
		for (Entry<String, ?> setting : settingsMap.entrySet())
		{
			String key = setting.getKey();
			
			if (key.startsWith("global_temp") && (avoidKeys == null || !avoidKeys.contains(key)))
			{
				String globalKey = "global" + key.substring(11);
				
				if (!globalSettings.containsKey(globalKey))
				{
					// A new global setting has been added
					return true;
				}
				
				Object tempObject = setting.getValue();
				Object globalObject = globalSettings.get(globalKey);

				if (tempObject != null && (globalObject == null || !tempObject.equals(globalObject)))
				{
					// An existing global setting has been modified
					return true;
				}
			}
		}
		
		return false;
	}
	
	public boolean saveAllPreferences(boolean calibrate)
	{
		Map<String, ?> settingsMap = mSharedPreferences.getAll();
		
		savePreferencesToProvider(settingsMap);
		
		Editor editor = mSharedPreferences.edit();
		editClearBackup(editor, settingsMap);
		editCreateBackup(editor, settingsMap);
		
		Set<String> avoidKeys = null;

		if (calibrate)
		{
			editCalibrationMode(editor);
			
			int numColumns = mAixWidgetInfo.getNumColumns();
			int numRows = mAixWidgetInfo.getNumRows();

			// When calibrating, avoid storing the settings which are being modified
			avoidKeys = new HashSet<String>();
			avoidKeys.add(mContext.getString(R.string.preference_useDeviceSpecificDimensions_bool));
			avoidKeys.add(mContext.getString(R.string.preference_calibrationState_int));
			avoidKeys.add(mContext.getString(R.string.preference_calibrationTarget_int));
			avoidKeys.add(mContext.getString(R.string.preference_calibrationPortraitDimension_string));
			avoidKeys.add(mContext.getString(R.string.preference_calibrationLandscapeDimension_string));
			avoidKeys.add(buildPixelDimensionsKey(PREFERENCE, numColumns, numRows, false));
			avoidKeys.add(buildPixelDimensionsKey(PREFERENCE, numColumns, numRows, true));
		}
		else
		{
			AixUtils.editWidgetState(editor, mAixWidgetInfo.getAppWidgetId(), 0);
		}
		
		editSaveGlobalSettings(editor, settingsMap, avoidKeys);
		
		boolean globalModified = isGlobalSettingsModified(settingsMap, avoidKeys);
		
		editor.commit();
		
		return globalModified;
	}
	
	/*
	public void saveDeviceProfilePreferences()
	{
		Map<String, ?> settingsMap = mSharedPreferences.getAll();
		
		Editor editor = mSharedPreferences.edit();
		editSaveDeviceProfileSettings(editor, settingsMap);
		editor.commit();
	}
	*/
	
	public void savePreferencesToProvider(Map<String, ?> settingsMap) {
		ContentResolver resolver = mContext.getContentResolver();
		Uri aixWidgetSettingsUri = Uri.withAppendedPath(
				mAixWidgetInfo.getWidgetUri(), AixWidgets.TWIG_SETTINGS);
		ContentValues[] values = buildContentValues(settingsMap);
		resolver.bulkInsert(aixWidgetSettingsUri, values);
	}
	
	/* END SETTINGS BACKUP/RESTORE */
	
	/* BEGIN WIDGET STATE STUFF */
	
	public int getWidgetState() 
	{
		return getWidgetState(mAixWidgetInfo.getAppWidgetId());
	}
	
	public int getWidgetState(int widgetId) 
	{
		String key = "global_widget_" + widgetId;
		return mSharedPreferences.getInt(key, 0);
	}
	
	public void setAllWidgetStates(int widgetState)
	{
		setAllWidgetStates(null, widgetState).commit();
	}
	
	public Editor setAllWidgetStates(Editor editor, int widgetState) {
		if (editor == null) {
			editor = mSharedPreferences.edit();
		}
		
		Map<String, ?> settingMap = (Map<String, ?>)mSharedPreferences.getAll();
		for (Entry<String, ?> setting : settingMap.entrySet()) {
			String key = setting.getKey();
			if (key.startsWith("global_widget")) {
				if (widgetState == 0) {
					editor.remove(key);
				} else {
					editor.putInt(key, widgetState);
				}
			}
		}
		
		return editor;
	}
	
	public static void clearAllWidgetStates(SharedPreferences sharedPreferences)
	{
		Editor editor = sharedPreferences.edit();
		
		Map<String, ?> settingMap = (Map<String, ?>)sharedPreferences.getAll();
		for (Entry<String, ?> setting : settingMap.entrySet()) {
			String key = setting.getKey();
			if (key.startsWith("global_widget")) {
				editor.remove(key);
			}
		}
		
		editor.commit();
	}
	
	public void setWidgetState(int widgetState)
	{
		setWidgetState(mAixWidgetInfo.getAppWidgetId(), widgetState);
	}
	
	public void setWidgetState(int widgetId, int widgetState)
	{
		setWidgetState(null, widgetId, widgetState).commit();
	}
	
	public Editor setWidgetState(Editor editor, int widgetId, int widgetState)
	{
		if (editor == null) {
			editor = mSharedPreferences.edit();
		}
		
		String key = "global_widget_" + widgetId;
		if (widgetState == 0) {
			editor.remove(key);
		} else {
			editor.putInt(key, widgetState);
		}
		
		return editor;
	}
	
	public static Editor removeWidgetSettings(
			Context context, SharedPreferences sharedPreferences,
			Editor editor, int appWidgetId)
	{
		if (editor == null)
		{
			if (sharedPreferences == null)
			{
				sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
			}
			editor = sharedPreferences.edit();
		}
		
		editor.remove("global_widget_" + appWidgetId);	// Clear draw state
		editor.remove("global_country_" + appWidgetId);	// Clear country information
		
		return editor;
	}
	
	/* END WIDGET STATE STUFF */
	
	/* BEGIN ??? */
	
	public Editor editLastProfileSync(Editor editor, boolean temporary, long value) {
		if (editor == null) {
			editor = mSharedPreferences.edit();
		}
		
		String key = mContext.getString(
				temporary ? R.string.preference_lastProfileSync_long
						  : R.string.lastProfileSync_long);
		
		editor.putLong(key, value);
		
		return editor;
	}
	
	public Editor editLocationCountryCode(Editor editor, long locationId, String value)
	{
		if (editor == null)
		{
			editor = mSharedPreferences.edit();
		}
		
		editor.putString("global_lcountry_" + locationId, value);
		
		return editor;
	}
	
	public Editor editOrientationMode(Editor editor, boolean temporary, int value) {
		// Verify value
		if (value < 0 || value > 2) {
			throw new IllegalArgumentException("setOrientationMode(): Invalid value (" + value + ")");
		}
		
		if (editor == null) {
			editor = mSharedPreferences.edit();
		}
		
		String key = mContext.getString(
				temporary ? R.string.preference_orientationMode_int
						  : R.string.orientationMode_int);
		
		editor.putInt(key, value);
		
		return editor;
	}
	
	public Editor editUseDeviceProfile(Editor editor, boolean temporary, boolean value) {
		if (editor == null) {
			editor = mSharedPreferences.edit();
		}
		
		String key = mContext.getString(
				temporary ? R.string.preference_useDeviceSpecificDimensions_bool
						  : R.string.useDeviceSpecificDimensions_bool);
		
		editor.putBoolean(key, value);
		
		return editor;
	}
	
	public long getLastProfileSync() {
		String key = mContext.getString(R.string.lastProfileSync_long);
		return mSharedPreferences.getLong(key, -1);
	}
	
	public String getLocationCountryCode(long locationId)
	{
		String key = "global_lcountry_" + locationId;
		return mSharedPreferences.getString(key, null);
	}
	
	public long getLastProfileSyncPreference() {
		String key = mContext.getString(R.string.preference_lastProfileSync_long);
		return mSharedPreferences.getLong(key, -1);
	}
	
	public int getOrientationMode() {
		return mSharedPreferences.getInt(
				mContext.getString(R.string.orientationMode_int),
				ORIENTATION_MODE_AUTO);
	}
	
	public int getOrientationModePreference() {
		return mSharedPreferences.getInt(
				mContext.getString(R.string.preference_orientationMode_int),
				ORIENTATION_MODE_AUTO);
	}
	
	public boolean getUseDeviceProfile() {
		return mSharedPreferences.getBoolean(mContext.getString(R.string.useDeviceSpecificDimensions_bool), false);
	}
	
	public boolean getUseDeviceProfilePreference() {
		return mSharedPreferences.getBoolean(mContext.getString(R.string.preference_useDeviceSpecificDimensions_bool), false);
	}
	
	public boolean isProviderPreferenceModified()
	{
		String defaultValue = Integer.toString(AixUtils.PROVIDER_AUTO);
		String providerValue = mSharedPreferences.getString(mContext.getString(R.string.provider_string), defaultValue);
		String providerPreferenceValue = mSharedPreferences.getString(mContext.getString(R.string.preference_provider_string), defaultValue);
		return !providerValue.equals(providerPreferenceValue);
	}
	
	public void setLastProfileSyncPreference(long value) {
		editLastProfileSync(null, PREFERENCE, value).commit();
	}
	
	public void setLocationCountryCode(long locationId, String value)
	{
		editLocationCountryCode(null, locationId, value).commit();
	}
	
	public void setOrientationModePreference(int value) {
		editOrientationMode(null, PREFERENCE, value).commit();
	}
	
	public void setUseDeviceProfilePreference(boolean value) {
		editUseDeviceProfile(null, PREFERENCE, value).commit();
	}
	
	/* END ??? */
	
	/* BEGIN CALIBRATION STUFF */
	
	public void adjustCalibrationDimension(String property, int change) {
		boolean isLandscape = property.equals(LANDSCAPE_HEIGHT) || property.equals(LANDSCAPE_WIDTH);
		boolean isHorizontal = property.equals(PORTRAIT_WIDTH) || property.equals(LANDSCAPE_WIDTH);
		
		String calibrationKey = mContext.getString(isLandscape
				? R.string.calibrationLandscapeDimension_string
				: R.string.calibrationPortraitDimension_string);
		
		Point dimensions = getPixelDimensionsFromKey(calibrationKey);
		
		int numColumns = mAixWidgetInfo.getNumColumns();
		int numRows = mAixWidgetInfo.getNumRows();
		
		if (dimensions == null) {
			dimensions = getStandardPixelDimensions(numColumns, numRows, isLandscape, false);
		} else {
			// Only modify if existing calibration settings exist
			if (isHorizontal) {
				dimensions.x += change;
			} else {
				dimensions.y += change;
			}
		}
		
		try {
			editPixelDimensionsByKey(null, calibrationKey, dimensions).commit();
		} catch (IllegalArgumentException e) { /* IGNORE */ }
	}
	
	public Editor editCalibrationMode(Editor editor)
	{
		int appWidgetId = mAixWidgetInfo.getAppWidgetId();
		
		Point portraitDimensions = getPixelDimensionsPreferenceOrStandard(false);
		Point landscapeDimensions = getPixelDimensionsPreferenceOrStandard(true);
		
		editor.putInt(mContext.getString(R.string.calibrationTarget_int), appWidgetId);
		editor.putInt(mContext.getString(R.string.calibrationState_int), CALIBRATION_STATE_HORIZONTAL);
		
		editPixelDimensionsByKey(editor,
				mContext.getString(R.string.calibrationPortraitDimension_string),
				portraitDimensions);
		
		editPixelDimensionsByKey(editor,
				mContext.getString(R.string.calibrationLandscapeDimension_string),
				landscapeDimensions);
		
		return editor;
	}
	
	public void exitCalibrationMode() {
		Editor editor = mSharedPreferences.edit();
		editor.remove(mContext.getString(R.string.calibrationTarget_int));
		editor.putBoolean(mContext.getString(R.string.useDeviceSpecificDimensions_bool), true);
		editor.commit();
	}
	
	public int getCalibrationState() {
		return mSharedPreferences.getInt(mContext.getString(R.string.calibrationState_int), CALIBRATION_STATE_INVALID);
	}
	
	public int getCalibrationTarget() {
		return mSharedPreferences.getInt(
				mContext.getString(R.string.calibrationTarget_int),
				AppWidgetManager.INVALID_APPWIDGET_ID);
	}
	
	public void saveCalibratedDimension(String property)
	{
		boolean isLandscape = property.equals(LANDSCAPE_HEIGHT) || property.equals(LANDSCAPE_WIDTH);
		
		String calibrationKey = mContext.getString(isLandscape
				? R.string.calibrationLandscapeDimension_string
				: R.string.calibrationPortraitDimension_string);
		
		Point dimensions = getPixelDimensionsFromKey(calibrationKey);
		
		int numColumns = mAixWidgetInfo.getNumColumns();
		int numRows = mAixWidgetInfo.getNumRows();
		
		if (dimensions == null) {
			dimensions = getStandardPixelDimensions(numColumns, numRows, isLandscape, false);
		}
		
		Editor editor = editPixelDimensionsByKey(null,
				buildPixelDimensionsKey(GLOBAL, numColumns, numRows, isLandscape),
				dimensions);
		
		editor.remove(buildPixelDimensionsStateKey(GLOBAL, numColumns, numRows, isLandscape));
		editor.remove(mContext.getString(R.string.deviceProfileSynced_bool));
		editor.commit();
	}
	
	public void setCalibrationState(int state) {
		Editor editor = mSharedPreferences.edit();
		editor.putInt(mContext.getString(R.string.calibrationState_int), state);
		editor.commit();
	}
	
	/*
	public void setupCalibrationMode()
	{
		Map<String, ?> settingsMap = mSharedPreferences.getAll();
		
		Editor editor = mSharedPreferences.edit();
		editSaveDeviceProfileSettings(editor, settingsMap);
		editCalibrationMode(editor);
		editor.commit();
	}
	*/
	
	/* END CALIBRATION STUFF */
	
	/* BEGIN PIXEL DIMENSION STUFF */
	
	public String buildPixelDimensionsStateKey(boolean temporary, int numColumns, int numRows, boolean isLandscape) {
		String formatter = mContext.getString(
				temporary ? R.string.preference_deviceProfileState_int
						  : R.string.deviceProfileState_int);
		return String.format(formatter, numColumns, numRows, isLandscape ? "landscape" : "portrait");
	}
	
	public String buildUploadedPixelDimensionsKey(boolean temporary, int numColumns, int numRows, boolean isLandscape) {
		String formatter = mContext.getString(
				temporary ? R.string.preference_deviceProfileStored_string
						  : R.string.deviceProfileStored_string);
		return String.format(formatter, numColumns, numRows, isLandscape ? "landscape" : "portrait");
	}
	
	public String buildPixelDimensionsKey(boolean temporary, int numColumns, int numRows, boolean isLandscape) {
		String formatter = mContext.getString(
				temporary ? R.string.preference_deviceProfile_string
						  : R.string.deviceProfile_string);
		return String.format(formatter, numColumns, numRows, isLandscape ? "landscape" : "portrait");
	}
	
	public Editor editPixelDimensionsByKey(
			Editor editor, String key, Point dimensions)
	{
		if (editor == null) {
			editor = mSharedPreferences.edit();
		}
		
		if (dimensions != null) {
			int maxDimension = Math.max(
					mDisplayMetrics.widthPixels,
					mDisplayMetrics.heightPixels);
			
			if (dimensions.x < 1 || dimensions.x > maxDimension ||
					dimensions.y < 1 || dimensions.y > maxDimension)
			{
				throw new IllegalArgumentException();	
			}
			
			// int x = AixUtils.clamp(dimensions.x, 1, maxDimension);
			// int y = AixUtils.clamp(dimensions.y, 1, maxDimension);
			
			String value = String.format("%dx%d", dimensions.x, dimensions.y);
			
			editor.putString(key, value);
		} else {
			editor.remove(key);
		}
		
		return editor;
	}
	
	public Point getCalibrationPixelDimensionsOrStandard(boolean isLandscape)
	{
		return getCalibrationPixelDimensionsOrStandard(
				mAixWidgetInfo.getNumColumns(),
				mAixWidgetInfo.getNumRows(),
				isLandscape);
	}
	
	public Point getCalibrationPixelDimensionsOrStandard(
			int numColumns, int numRows, boolean isLandscape)
	{
		String key = mContext.getString(isLandscape
				? R.string.calibrationLandscapeDimension_string
				: R.string.calibrationPortraitDimension_string);
		Point dimensions = getPixelDimensionsFromKey(key);
		
		if (dimensions == null) {
			dimensions = getStandardPixelDimensions(numColumns, numRows, isLandscape, false);
		}
		return dimensions;
	}
	
	public Point getPixelDimensionsFromKey(String key)
	{
		Point p = null;
		
		String value = mSharedPreferences.getString(key, null);
		
		if (value != null) {
			String[] valArray = value.split("x");
			boolean invalid = true;
			int width = -1, height = -1;
			
			try {
				if (valArray.length == 2) {
					width = Integer.parseInt(valArray[0]);
					height = Integer.parseInt(valArray[1]);
					invalid = false;
				}
			} catch (NumberFormatException e) { }
			
			if (invalid) {
				// Invalid value in preferences; remove it
				// TODO What about state etc?
				//Editor editor = mSharedPreferences.edit();
				//editor.remove(key);
				//editor.commit();
			} else {
				p = new Point(width, height);
			}
		}
		
		return p;
	}
	
	public Point getPixelDimensionsOrStandard(boolean isLandscape)
	{
		return getPixelDimensionsOrStandard(
				mAixWidgetInfo.getNumColumns(),
				mAixWidgetInfo.getNumRows(),
				isLandscape);
	}
	
	public Point getPixelDimensionsOrStandard(
			int numColumns, int numRows, boolean isLandscape)
	{
		String key = buildPixelDimensionsKey(GLOBAL, numColumns, numRows, isLandscape);
		Point dimensions = getPixelDimensionsFromKey(key);
		if (dimensions == null) {
			dimensions = getStandardPixelDimensions(numColumns, numRows, isLandscape, false);
		}
		return dimensions;
	}
	
	public Point getPixelDimensionsPreference(
			int numColumns, int numRows, boolean isLandscape)
	{
		String key = buildPixelDimensionsKey(true, numColumns, numRows, isLandscape);
		return getPixelDimensionsFromKey(key);
	}
	
	public Point getPixelDimensionsPreferenceOrStandard(boolean isLandscape)
	{
		return getPixelDimensionsPreferenceOrStandard(
				mAixWidgetInfo.getNumColumns(),
				mAixWidgetInfo.getNumRows(),
				isLandscape);
	}
	
	public Point getPixelDimensionsPreferenceOrStandard(
			int numColumns, int numRows, boolean isLandscape)
	{
		String key = buildPixelDimensionsKey(PREFERENCE, numColumns, numRows, isLandscape);
		Point dimensions = getPixelDimensionsFromKey(key);
		if (dimensions == null) {
			dimensions = getStandardPixelDimensions(numColumns, numRows, isLandscape, false);
		}
		return dimensions;
	}
	
	public int getPixelDimensionsStatePreference(int numColumns, int numRows, boolean isLandscape)
	{
		String key = buildPixelDimensionsStateKey(true, numColumns, numRows, isLandscape);
		return mSharedPreferences.getInt(key, DEVICE_PROFILE_STATE_NONE);
	}

	public Point getStandardPixelDimensions(
			int numColumns, int numRows,
			boolean isLandscape, boolean crop)
	{
		float width = 70.0f * numColumns + 30.0f;
		float height = 70.0f * numRows + 30.0f;
		
		if (crop) {
			width -= 2.0f;
			height -= 2.0f;
		}
		
		float density = mDisplayMetrics.density;
		
		Point p = new Point(
				Math.round(width * density),
				Math.round(height * density));
		
		return p;
	}

	public boolean isPixelDimensionsPreferenceModified(int numColumns, int numRows, boolean isLandscape)
	{
		Point pixelDimensionPreference = getPixelDimensionsFromKey(buildPixelDimensionsKey(true, numColumns, numRows, isLandscape));
		Point uploadedPixelDimension = getPixelDimensionsFromKey(buildUploadedPixelDimensionsKey(false, numColumns, numRows, isLandscape));
		
		return (pixelDimensionPreference == null) ||
			   (uploadedPixelDimension == null || !pixelDimensionPreference.equals(uploadedPixelDimension));
	}
	
	public void revertPixelDimensionsPreference(int numColumns, int numRows, boolean isLandscape) {
		Editor editor = editPixelDimensionsByKey(null,
				buildPixelDimensionsKey(PREFERENCE, numColumns, numRows, isLandscape),
				getStandardPixelDimensions(numColumns, numRows, isLandscape, NO_CROP));
		
		editor.putInt(buildPixelDimensionsStateKey(PREFERENCE, numColumns, numRows, isLandscape), DEVICE_PROFILE_STATE_NONE);
		editor.putBoolean(mContext.getString(R.string.preference_deviceProfileSynced_bool), false);
		editor.commit();
	}
	
	public void storePixelDimensionsPreference(int numColumns, int numRows, boolean isLandscape, Point dimensions) {
		Editor editor = editPixelDimensionsByKey(null,
				buildPixelDimensionsKey(true, numColumns, numRows, isLandscape),
				dimensions);
		
		// When storing a new dimension by manual edits in the DeviceProfile activity:
		// The state of the dimension must be cleared, and the "recently synced" flag cleared.
		editor.putInt(buildPixelDimensionsStateKey(PREFERENCE, numColumns, numRows, isLandscape), DEVICE_PROFILE_STATE_NONE);
		editor.putBoolean(mContext.getString(R.string.preference_deviceProfileSynced_bool), false);
		editor.commit();
	}
	
	public void storeUploadedPixelDimensions(int numColumns, int numRows, boolean isLandscape, Point dimensions) {
		String key = buildUploadedPixelDimensionsKey(false, numColumns, numRows, isLandscape);
		editPixelDimensionsByKey(null, key, dimensions).commit();
	}
	
	public int validateStringValue(String value) {
		if (TextUtils.isEmpty(value))
		{
			return -1;
		}
		
		int numericValue;
		
		try {
			numericValue = Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return -1;
		}
		
		int maxDimension = Math.max(
				mDisplayMetrics.widthPixels,
				mDisplayMetrics.heightPixels);
		
		if (numericValue < 1 || numericValue > maxDimension) {
			return -1;
		}
		
		return numericValue;
	}
	
	/* END PIXEL DIMENSION STUFF */
	
}
