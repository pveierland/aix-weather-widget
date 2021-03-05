package net.veierland.aix;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;

import net.veierland.aix.util.AixWidgetInfo;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.Xml;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class AixDeviceProfileActivity extends Activity
		implements OnCheckedChangeListener,
				   OnClickListener,
				   OnItemSelectedListener,
				   OnKeyListener
{
	private static final String TAG = "AixDeviceProfileActivity";
	
	private final static int RESULT_FAILED = 1;
	private final static int RESULT_SYNC_SUCCESSFUL = 2;
	private final static int RESULT_UPDATE_SUCCESSFUL = 3;
	private final static int RESULT_SUBMIT_SUCCESSFUL = 4;
	
	private final static int ACTION_SYNC = 1;
	private final static int ACTION_SUBMIT_PORTRAIT = 2;
	private final static int ACTION_SUBMIT_LANDSCAPE = 3;
	
	private int mNumColumns;
	private int mNumRows;
	
	private int mInvalidEditTextColor;
	
	private boolean mValidPortraitWidth;
	private boolean mValidPortraitHeight;
	private boolean mValidLandscapeWidth;
	private boolean mValidLandscapeHeight;
	
	private AixSettings mAixSettings = null;
	private AixWidgetInfo mAixWidgetInfo = null;
	
	private ProfileSyncTask mSyncTask = null;
	private ProfileSyncTask mPortraitUploadTask = null;
	private ProfileSyncTask mLandscapeUploadTask = null;
	
	private SharedPreferences mSharedPreferences = null;
	
	private Object mTaskLock = new Object();
	
	/* UI Elements Begin */

	private TextView mModelTextView;
	private Spinner mOrientationModeSpinner;
	private CheckBox mActivateSpecificDimensionsCheckBox;
	
	private LinearLayout mFocusLayout;
	
	private Button mSyncButton;
	private ProgressBar mSyncProgressBar;
	private TextView mSyncStatusLabel;
	
	private TextView mPortraitDimensionsLabel, mPortraitDimensionsProfileStatus;
	private TextView mPortraitWidthLabel, mPortraitHeightLabel;
	private EditText mPortraitWidthEditText, mPortraitHeightEditText;
	private ImageButton mPortraitRevertButton;
	private Button mPortraitCalibrateButton, mPortraitSubmitButton;
	private ProgressBar mPortraitProgressBar;
	
	private TextView mLandscapeDimensionsLabel, mLandscapeDimensionsProfileStatus;
	private TextView mLandscapeWidthLabel, mLandscapeHeightLabel;
	private EditText mLandscapeWidthEditText, mLandscapeHeightEditText;
	private ImageButton mLandscapeRevertButton;
	private Button mLandscapeCalibrateButton, mLandscapeSubmitButton;
	private ProgressBar mLandscapeProgressBar;
	
	/* UI Elements End */
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setResult(RESULT_CANCELED);
		
		Uri widgetUri = getIntent().getData();
		if (widgetUri == null)
		{
			Toast.makeText(this, "Error: Widget URI was null!", Toast.LENGTH_SHORT).show();
			Log.d(TAG, "Killed AixDeviceProfileActivity: Widget URI was null!");
			finish();
			return;
		}
		
		try {
			mAixWidgetInfo = AixWidgetInfo.build(this, widgetUri);
		} catch (Exception e) {
			Toast.makeText(this, "Error: Failed to get widget information!", Toast.LENGTH_SHORT).show();
			Log.d(TAG, "Killed AixDeviceProfileActivity: Failed to get widget information! (uri=" + widgetUri +")");
			e.printStackTrace();
			finish();
			return;
		}
		
		mNumColumns = mAixWidgetInfo.getNumColumns();
		mNumRows = mAixWidgetInfo.getNumRows();
		
		mAixSettings = AixSettings.build(this, mAixWidgetInfo);
		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		
		setContentView(R.layout.activity_device_profiles);
		
		setupUIElements();
		initialize();
		updateUIState();
	}

	private void initialize()
	{
		Resources resources = getResources();
		
		TextView tv = (TextView)findViewById(R.id.aix_device_profile_guide);
		tv.setMovementMethod(LinkMovementMethod.getInstance());
		tv.setText(Html.fromHtml(getString(R.string.device_profile_guide_link)));
		
		mInvalidEditTextColor = resources.getColor(R.color.invalid_dimension_color);
		
		mModelTextView.setText(String.format("%s (%s)", Build.MODEL, Build.PRODUCT));
		
		mValidPortraitWidth = true;
		mValidPortraitHeight = true;
		mValidLandscapeWidth = true;
		mValidLandscapeHeight = true;
		
		// Use Device Specific Dimension Property
		boolean useDeviceSpecificDimensions = mAixSettings.getUseDeviceProfilePreference();
		mActivateSpecificDimensionsCheckBox.setChecked(useDeviceSpecificDimensions);
		mActivateSpecificDimensionsCheckBox.setOnCheckedChangeListener(this);
		
		// Orientation Mode Property
		int orientationMode = mAixSettings.getOrientationModePreference();
		mOrientationModeSpinner.setSelection(orientationMode);
		mOrientationModeSpinner.setOnItemSelectedListener(this);
		
		mSyncButton.setOnClickListener(this);
		
		mPortraitWidthEditText.setOnKeyListener(this);
		mPortraitHeightEditText.setOnKeyListener(this);
		mPortraitRevertButton.setOnClickListener(this);
		mPortraitCalibrateButton.setOnClickListener(this);
		mPortraitSubmitButton.setOnClickListener(this);
		
		mLandscapeWidthEditText.setOnKeyListener(this);
		mLandscapeHeightEditText.setOnKeyListener(this);
		mLandscapeRevertButton.setOnClickListener(this);
		mLandscapeCalibrateButton.setOnClickListener(this);
		mLandscapeSubmitButton.setOnClickListener(this);
	}
	
	private void setupUIElements() {
		mModelTextView = (TextView)findViewById(R.id.aix_device_profile_model);
		mOrientationModeSpinner = (Spinner)findViewById(R.id.aix_device_profile_orientation);
	
		mFocusLayout = (LinearLayout)findViewById(R.id.focusLayout);
		
		mActivateSpecificDimensionsCheckBox = (CheckBox)findViewById(R.id.checkbox_activate_specific_dimensions);
		
		mSyncButton = (Button)findViewById(R.id.button_sync_device_profile);
		mSyncProgressBar = (ProgressBar)findViewById(R.id.progressbar_sync);
		mSyncStatusLabel = (TextView)findViewById(R.id.aix_device_profile_last_update);
		
		mPortraitDimensionsLabel = (TextView)findViewById(R.id.portrait_dimensions_label);
		mPortraitDimensionsProfileStatus = (TextView)findViewById(R.id.portrait_dimensions_profile_status);
		mPortraitWidthLabel = (TextView)findViewById(R.id.portrait_width_label);
		mPortraitHeightLabel = (TextView)findViewById(R.id.portrait_height_label);
		
		mPortraitWidthEditText = (EditText)findViewById(R.id.aix_device_profile_portrait_width);
		mPortraitHeightEditText = (EditText)findViewById(R.id.aix_device_profile_portrait_height);
		mPortraitRevertButton = (ImageButton)findViewById(R.id.adp_revert_portrait_button);
		mPortraitCalibrateButton = (Button)findViewById(R.id.button_calibrate_portrait);
		mPortraitSubmitButton = (Button)findViewById(R.id.button_submit_portrait);
		mPortraitProgressBar = (ProgressBar)findViewById(R.id.progressbar_portrait);
		
		mLandscapeDimensionsLabel = (TextView)findViewById(R.id.landscape_dimensions_label);
		mLandscapeDimensionsProfileStatus = (TextView)findViewById(R.id.landscape_dimensions_profile_status);
		mLandscapeWidthLabel = (TextView)findViewById(R.id.landscape_width_label);
		mLandscapeHeightLabel = (TextView)findViewById(R.id.landscape_height_label);
		
		mLandscapeWidthEditText = (EditText)findViewById(R.id.aix_device_profile_landscape_width);
		mLandscapeHeightEditText = (EditText)findViewById(R.id.aix_device_profile_landscape_height);
		mLandscapeRevertButton = (ImageButton)findViewById(R.id.adp_revert_landscape_button);
		mLandscapeCalibrateButton = (Button)findViewById(R.id.button_calibrate_landscape);
		mLandscapeSubmitButton = (Button)findViewById(R.id.button_submit_landscape);
		mLandscapeProgressBar = (ProgressBar)findViewById(R.id.progressbar_landscape);
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		if (buttonView == mActivateSpecificDimensionsCheckBox) {
			mAixSettings.setUseDeviceProfilePreference(isChecked);
			updateUIState();
		}
	}
	
	@Override
	public void onClick(View view) {
		if (view == mSyncButton) {
			startSync();
		} else if (view == mPortraitSubmitButton) {
			submitPortraitDimensions();
		} else if (view == mPortraitRevertButton) {
			mAixSettings.revertPixelDimensionsPreference(mNumColumns, mNumRows, false);
			updateUIState();
		} else if (view == mLandscapeSubmitButton) {
			submitLandscapeDimensions();
		} else if (view == mLandscapeRevertButton) {
			mAixSettings.revertPixelDimensionsPreference(mNumColumns, mNumRows, true);
			updateUIState();
		} else if (view == mPortraitCalibrateButton || view == mLandscapeCalibrateButton) {
			startCalibration();
		}
	}

	@Override
	public void onItemSelected (AdapterView<?> parent, View view, int position, long id) {
		if (parent == mOrientationModeSpinner) {
			if (position >= 0 && position <= 2) {
				mAixSettings.setOrientationModePreference(position);
			}
		}
	}
	
	@Override
	public void onNothingSelected(AdapterView<?> arg0) { }
	
	@Override
	public boolean onKey(View v, int keyCode, KeyEvent event) {
		// Avoid evaluating function twice on key events.
		// Handle up + multiple action events.
		if (event.getAction() != KeyEvent.ACTION_DOWN)
		{
			if (v == mPortraitWidthEditText || v == mPortraitHeightEditText) {
				final String widthString = mPortraitWidthEditText.getText().toString();
				final String heightString = mPortraitHeightEditText.getText().toString();
				
				int portraitWidth = mAixSettings.validateStringValue(widthString);
				int portraitHeight = mAixSettings.validateStringValue(heightString);
	
				mValidPortraitWidth = (portraitWidth != -1);
				mValidPortraitHeight = (portraitHeight != -1);
				
				if (mValidPortraitWidth && mValidPortraitHeight)
				{
					mAixSettings.storePixelDimensionsPreference(mNumColumns, mNumRows, false, new Point(portraitWidth, portraitHeight));
				}
				
				updateUIState(false);
			} else if (v == mLandscapeWidthEditText || v == mLandscapeHeightEditText) {
				final String widthString = mLandscapeWidthEditText.getText().toString();
				final String heightString = mLandscapeHeightEditText.getText().toString();
				
				int landscapeWidth = mAixSettings.validateStringValue(widthString);
				int landscapeHeight = mAixSettings.validateStringValue(heightString);
				
				mValidLandscapeWidth = (landscapeWidth != -1);
				mValidLandscapeHeight = (landscapeHeight != -1);
				
				if (mValidLandscapeWidth && mValidLandscapeHeight)
				{
					mAixSettings.storePixelDimensionsPreference(mNumColumns, mNumRows, true, new Point(landscapeWidth, landscapeHeight));
				}
				
				updateUIState(false);
			}
		}
		
		// Do not consume event, or else text will not change.
		return false;
	}

	private void startCalibration() {
		setResult(AixConfigure.EXIT_CONFIGURATION);
		finish();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mFocusLayout.requestFocus();
	}

	private void startSync() {
		boolean startSyncTask = false;
		
		synchronized (mTaskLock) {
			if (mSyncTask == null) {
				mSyncTask = new ProfileSyncTask();
				startSyncTask = true;
			}
		}
		
		if (startSyncTask) {
			updateUIState();
			mSyncTask.execute();
		} else {
			Toast.makeText(this, "Could not synchronize: update already active.", Toast.LENGTH_SHORT).show();
		}
	}
	
	private void submitLandscapeDimensions() {
		Point landscapeDimension = null;
		
		try {
			landscapeDimension = AixUtils.buildDimension(
					mLandscapeWidthEditText.getText().toString(),
					mLandscapeHeightEditText.getText().toString());
		} catch (Exception e) {
			String msg = String.format("Could not submit landscape dimensions: %s", e.getMessage());
			Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
			return;
		}
		
		boolean startNewTask = false;
		
		synchronized (mTaskLock) {
			if (mLandscapeUploadTask == null) {
				mLandscapeUploadTask = new ProfileSyncTask(
						ACTION_SUBMIT_LANDSCAPE,
						mNumColumns,
						mNumRows,
						landscapeDimension);
				startNewTask = true;
			}
		}
		
		if (startNewTask) {
			// Update UI state to reflect that a task has been started
			updateUIState(false);
			// Start upload task
			mLandscapeUploadTask.execute();
		} else {
			Toast.makeText(this, "Could not submit landscape dimensions: Upload in progress.", Toast.LENGTH_SHORT).show();
		}
	}

	private void submitPortraitDimensions() {
		Point portraitDimension = null;
		
		try {
			portraitDimension = AixUtils.buildDimension(
					mPortraitWidthEditText.getText().toString(),
					mPortraitHeightEditText.getText().toString());
		} catch (Exception e) {
			String msg = String.format("Could not submit portrait dimensions: %s", e.getMessage());
			Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
			return;
		}
		
		boolean startNewTask = false;
		
		synchronized (mTaskLock) {
			if (mPortraitUploadTask == null) {
				mPortraitUploadTask = new ProfileSyncTask(
						ACTION_SUBMIT_PORTRAIT,
						mNumColumns,
						mNumRows,
						portraitDimension);
				startNewTask = true;
			}
		}
		
		if (startNewTask) {
			// Update UI state to reflect that a task has been started
			updateUIState(false);
			// Start upload task
			mPortraitUploadTask.execute();
		} else {
			Toast.makeText(this, "Could not submit portrait dimensions: Upload in progress.", Toast.LENGTH_SHORT).show();
		}
	}

	private void updateEditTextBackgroundColors()
	{
		if (mValidPortraitWidth)
		{
			mPortraitWidthEditText.getBackground().clearColorFilter();
		}
		else
		{
			mPortraitWidthEditText.getBackground().setColorFilter(mInvalidEditTextColor, PorterDuff.Mode.MULTIPLY);
		}
		if (mValidPortraitHeight)
		{
			mPortraitHeightEditText.getBackground().clearColorFilter();
		}
		else
		{
			mPortraitHeightEditText.getBackground().setColorFilter(mInvalidEditTextColor, PorterDuff.Mode.MULTIPLY);
		}
		if (mValidLandscapeWidth)
		{
			mLandscapeWidthEditText.getBackground().clearColorFilter();
		}
		else
		{
			mLandscapeWidthEditText.getBackground().setColorFilter(mInvalidEditTextColor, PorterDuff.Mode.MULTIPLY);
		}
		if (mValidLandscapeHeight)
		{
			mLandscapeHeightEditText.getBackground().clearColorFilter();
		}
		else
		{
			mLandscapeHeightEditText.getBackground().setColorFilter(mInvalidEditTextColor, PorterDuff.Mode.MULTIPLY);
		}
	}
	
	private void updateUIState() {
		updateUIState(true);
	}
	
	private void updateUIState(boolean updateEditTextBoxes)
	{
		boolean activateSpecificDimensions = mActivateSpecificDimensionsCheckBox.isChecked();
		
		boolean enablePortraitControls = activateSpecificDimensions && (mSyncTask == null);
		boolean enableLandscapeControls = activateSpecificDimensions && (mSyncTask == null);
		
		if (mSyncTask != null) {
			mSyncProgressBar.setVisibility(View.VISIBLE);
			mSyncStatusLabel.setVisibility(View.GONE);
		} else {
			long lastProfileSync = mSharedPreferences.getLong(getString(R.string.preference_lastProfileSync_long), -1);
			
			if (lastProfileSync != -1) {
				
				Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
				calendar.setTimeInMillis(lastProfileSync);
				calendar.setTimeZone(TimeZone.getDefault());
				
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				
				mSyncStatusLabel.setText("Last database sync:\n" + sdf.format(calendar.getTime()));
			} else {
				mSyncStatusLabel.setText("Last database sync:\nNot synced");
			}
			
			mSyncProgressBar.setVisibility(View.GONE);
			mSyncStatusLabel.setVisibility(View.VISIBLE);
		}
		
		if (mPortraitUploadTask != null) {
			mPortraitProgressBar.setVisibility(View.VISIBLE);
			mPortraitSubmitButton.setEnabled(false);
		} else {
			boolean isPortraitModified = mAixSettings.isPixelDimensionsPreferenceModified(mNumColumns, mNumRows, false);
			
			mPortraitSubmitButton.setEnabled(enablePortraitControls && isPortraitModified);
			mPortraitProgressBar.setVisibility(View.GONE);
		}
		
		if (mLandscapeUploadTask != null) {
			mLandscapeProgressBar.setVisibility(View.VISIBLE);
			mLandscapeSubmitButton.setEnabled(false);
		} else {
			boolean isLandscapeModified = mAixSettings.isPixelDimensionsPreferenceModified(mNumColumns, mNumRows, true);
			
			mLandscapeSubmitButton.setEnabled(enableLandscapeControls && isLandscapeModified);
			mLandscapeProgressBar.setVisibility(View.GONE);
		}
		
		enablePortraitControls &= activateSpecificDimensions;
		enableLandscapeControls &= activateSpecificDimensions;
		
		mSyncButton.setEnabled(activateSpecificDimensions);
		mSyncStatusLabel.setEnabled(activateSpecificDimensions);
		
		mPortraitDimensionsLabel.setEnabled(enablePortraitControls);
		mPortraitWidthLabel.setEnabled(enablePortraitControls);
		mPortraitHeightLabel.setEnabled(enablePortraitControls);
		
		mPortraitWidthEditText.setEnabled(enablePortraitControls);
		mPortraitHeightEditText.setEnabled(enablePortraitControls);
		mPortraitCalibrateButton.setEnabled(enablePortraitControls);
		mPortraitRevertButton.setEnabled(enablePortraitControls);
		
		mLandscapeDimensionsLabel.setEnabled(enableLandscapeControls);
		mLandscapeWidthLabel.setEnabled(enableLandscapeControls);
		mLandscapeHeightLabel.setEnabled(enableLandscapeControls);
		
		mLandscapeWidthEditText.setEnabled(enableLandscapeControls);
		mLandscapeHeightEditText.setEnabled(enableLandscapeControls);
		mLandscapeCalibrateButton.setEnabled(enableLandscapeControls);
		mLandscapeRevertButton.setEnabled(enableLandscapeControls);
		
		if (updateEditTextBoxes) {
			Point portraitDimensions = mAixSettings.getPixelDimensionsPreferenceOrStandard(mNumColumns, mNumRows, false);
			mPortraitWidthEditText.setText(Integer.toString(portraitDimensions.x));
			mPortraitHeightEditText.setText(Integer.toString(portraitDimensions.y));
			
			Point landscapeDimensions = mAixSettings.getPixelDimensionsPreferenceOrStandard(mNumColumns, mNumRows, true);
			mLandscapeWidthEditText.setText(Integer.toString(landscapeDimensions.x));
			mLandscapeHeightEditText.setText(Integer.toString(landscapeDimensions.y));
		}
		
		updateEditTextBackgroundColors();
		
		boolean deviceProfileSynced = mSharedPreferences.getBoolean(
				getString(R.string.preference_deviceProfileSynced_bool), false);
		
		int portraitState = mAixSettings.getPixelDimensionsStatePreference(mNumColumns, mNumRows, false);
		
		if (deviceProfileSynced || portraitState == AixSettings.DEVICE_PROFILE_STATE_USER_SUBMITTED ||
								   portraitState == AixSettings.DEVICE_PROFILE_STATE_RECOMMENDED)
		{
			mPortraitDimensionsProfileStatus.setVisibility(View.VISIBLE);
			
			if (portraitState == AixSettings.DEVICE_PROFILE_STATE_USER_SUBMITTED) {
				mPortraitDimensionsProfileStatus.setText("User-submitted dimensions: May not be valid");
				mPortraitDimensionsProfileStatus.setBackgroundResource(R.drawable.tip_yellow);
			} else if (portraitState == AixSettings.DEVICE_PROFILE_STATE_RECOMMENDED) {
				mPortraitDimensionsProfileStatus.setText("Recommended layout");
				mPortraitDimensionsProfileStatus.setBackgroundResource(R.drawable.tip_green);
			} else {
				mPortraitDimensionsProfileStatus.setText("No matching profile found: Calibration needed");
				mPortraitDimensionsProfileStatus.setBackgroundResource(R.drawable.tip_red);
			}
		} else {
			mPortraitDimensionsProfileStatus.setVisibility(View.GONE);
		}
		
		int landscapeState = mAixSettings.getPixelDimensionsStatePreference(mNumColumns, mNumRows, true);
		
		if (deviceProfileSynced || landscapeState == AixSettings.DEVICE_PROFILE_STATE_USER_SUBMITTED ||
								   landscapeState == AixSettings.DEVICE_PROFILE_STATE_RECOMMENDED)
		{
			mLandscapeDimensionsProfileStatus.setVisibility(View.VISIBLE);
			
			if (landscapeState == AixSettings.DEVICE_PROFILE_STATE_USER_SUBMITTED) {
				mLandscapeDimensionsProfileStatus.setText("User-submitted dimensions: May not be valid");
				mLandscapeDimensionsProfileStatus.setBackgroundResource(R.drawable.tip_yellow);
			} else if (landscapeState == AixSettings.DEVICE_PROFILE_STATE_RECOMMENDED){
				mLandscapeDimensionsProfileStatus.setText("Recommended layout");
				mLandscapeDimensionsProfileStatus.setBackgroundResource(R.drawable.tip_green);
			} else {
				mLandscapeDimensionsProfileStatus.setText("No matching profile found: Calibration needed");
				mLandscapeDimensionsProfileStatus.setBackgroundResource(R.drawable.tip_red);
			}
		} else {
			mLandscapeDimensionsProfileStatus.setVisibility(View.GONE);
		}
	}
	
	private static class AixDeviceProfileWidgetDimension
	{
		public boolean isLandscape, isPromoted = false;
		public int numColumns, numRows;
		public int width, height;
		
		public AixDeviceProfileWidgetDimension() { }
	}
	
	private class ProfileSyncTask extends AsyncTask<Void, Void, Integer>
	{
		private boolean mTaskIsLandscape;
		
		private int mTaskAction;
		private int mTaskNumColumns;
		private int mTaskNumProfiles;
		private int mTaskNumRows;
		
		private Point mTaskDimension;

		private String mTaskUser;
		private String mTaskDevice;
		
		public ProfileSyncTask() {
			super();
			mTaskAction = ACTION_SYNC;
		}
		
		public ProfileSyncTask(int action, int numColumns, int numRows, Point dimension) {
			super();
			mTaskAction = action;
			mTaskNumColumns = numColumns;
			mTaskNumRows = numRows;
			mTaskDimension = dimension;
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}
		
		@Override
		protected void onPostExecute(Integer result) {
			super.onPostExecute(result);

			String toastMessage = null;
			
			if (mTaskAction == ACTION_SYNC) {
				if (result == RESULT_SYNC_SUCCESSFUL) {
					toastMessage = "Database sync successful!\n(" + mTaskNumProfiles + " profiles retrieved)";
				}
				
				synchronized (mTaskLock) {
					mSyncTask = null;
				}
				
				updateUIState();
			} else if (mTaskAction == ACTION_SUBMIT_PORTRAIT) {
				if (result == RESULT_FAILED) {
					toastMessage = "Failed to upload portrait data!";
				} else if (result == RESULT_UPDATE_SUCCESSFUL) {
					toastMessage = "Successfully updated portrait data!";
				} else if (result == RESULT_SUBMIT_SUCCESSFUL) {
					toastMessage = "Successfully submitted portrait data!";
				}
				
				synchronized (mTaskLock) {
					mPortraitUploadTask = null;
				}
				
				updateUIState();
			} else if (mTaskAction == ACTION_SUBMIT_LANDSCAPE) {
				if (result == RESULT_FAILED) {
					toastMessage = "Failed to upload landscape data!";
				} else if (result == RESULT_UPDATE_SUCCESSFUL) {
					toastMessage = "Successfully updated landscape data!";
				} else if (result == RESULT_SUBMIT_SUCCESSFUL) {
					toastMessage = "Successfully submitted landscape data!";
				}
				
				synchronized (mTaskLock) {
					mLandscapeUploadTask = null;
				}
				
				updateUIState();
			}
			
			if (toastMessage != null) {
				Toast toast = Toast.makeText(AixDeviceProfileActivity.this, toastMessage, Toast.LENGTH_SHORT);
				try {
					((TextView)((LinearLayout)toast.getView()).getChildAt(0)).setGravity(Gravity.CENTER_HORIZONTAL);
				} catch (Exception e) { }
				toast.show();
			}
		}
		
		@Override
		protected Integer doInBackground(Void... params) {
			int result = RESULT_FAILED;
			
			mTaskDevice = String.format("%s_%s", Build.MODEL, Build.PRODUCT).toLowerCase();
			
			if (mTaskAction == ACTION_SYNC) {
				// Download the profile for this device and insert the various data into settings
				
				try {
					InputStream response = doDownload();
					if (response == null)
					{
						throw new IOException("Response from download was null");
					}
					
					AixDeviceProfileResult deviceProfile = AixDeviceProfileParser.parse(response);
					
					if (  !((deviceProfile != null) &&
							(deviceProfile.device != null) &&
							(deviceProfile.device.equals(mTaskDevice))))
					{
						throw new IOException("AixDeviceProfileActivity.ProfileSyncTask.doInBackground(): Invalid sync response: Header mismatch");
					}
					
					if (deviceProfile.widgetDimensions != null) {
						Editor editor = mSharedPreferences.edit();
						for (AixDeviceProfileWidgetDimension dimension : deviceProfile.widgetDimensions)
						{
							mAixSettings.editPixelDimensionsByKey(
									editor,
									mAixSettings.buildPixelDimensionsKey(AixSettings.PREFERENCE, dimension.numColumns, dimension.numRows, dimension.isLandscape),
									new Point(dimension.width, dimension.height));
							
							editor.putInt(
									mAixSettings.buildPixelDimensionsStateKey(AixSettings.PREFERENCE, dimension.numColumns, dimension.numRows, dimension.isLandscape),
									dimension.isPromoted ? AixSettings.DEVICE_PROFILE_STATE_RECOMMENDED : AixSettings.DEVICE_PROFILE_STATE_USER_SUBMITTED);
						}
						
						// Save time of successful sync
						Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
						mAixSettings.editLastProfileSync(editor, AixSettings.PREFERENCE, calendar.getTimeInMillis());
						
						editor.putBoolean(getString(R.string.preference_deviceProfileSynced_bool), true);
						
						editor.commit();
						
						mTaskNumProfiles = deviceProfile.widgetDimensions.size();
					} else {
						mTaskNumProfiles = 0;
					}
					
					result = RESULT_SYNC_SUCCESSFUL;
				} catch (Exception e) {
					Log.d(TAG, "Failed to sync device profile. (Exception=" + e.getMessage() + ")");
					e.printStackTrace();
				}
			} else if (mTaskAction == ACTION_SUBMIT_LANDSCAPE || mTaskAction == ACTION_SUBMIT_PORTRAIT) {
				// Upload the landscape / portrait data for this device for a given dimension
				mTaskUser = AixInstallation.id(AixDeviceProfileActivity.this);
				mTaskIsLandscape = (mTaskAction == ACTION_SUBMIT_LANDSCAPE);
				
				try {
					InputStream response = doUpload();
					if (response == null)
					{
						throw new IOException("Response from upload was null");
					}
					
					AixDeviceProfileResult deviceProfile = AixDeviceProfileParser.parse(response);
					
					if (  !((deviceProfile != null) &&
							(deviceProfile.status != null) &&
							(deviceProfile.status.equals("updated") || deviceProfile.status.equals("added")) &&
							(deviceProfile.device != null) &&
							(deviceProfile.device.equals(mTaskDevice)) &&
							(deviceProfile.user != null) &&
							(deviceProfile.user.equals(mTaskUser)) &&
							(deviceProfile.timeAdded != -1)))
					{
						throw new IOException("AixDeviceProfileActivity.ProfileSyncTask.doInBackground(): Invalid upload response: Header mismatch");
					}
					
					if (deviceProfile.widgetDimensions == null || deviceProfile.widgetDimensions.size() != 1)
					{
						throw new IOException("AixDeviceProfileActivity.ProfileSyncTask.doInBackground(): Invalid upload response: No widget dimension data");
					}

					AixDeviceProfileWidgetDimension dimension = deviceProfile.widgetDimensions.get(0);
					
					if (	dimension == null ||
							dimension.isLandscape != mTaskIsLandscape ||
							dimension.numColumns != mTaskNumColumns ||
							dimension.numRows != mTaskNumRows ||
							dimension.width != mTaskDimension.x ||
							dimension.height != mTaskDimension.y)
					{
						throw new IOException("AixDeviceProfileActivity.ProfileSyncTask.doInBackground(): Invalid upload response: Data mismatch");
					}

					// Update settings to reflect successfully submitted settings
					mAixSettings.storeUploadedPixelDimensions(mTaskNumColumns, mTaskNumRows, mTaskIsLandscape, mTaskDimension);
					
					result = deviceProfile.status.equals("updated") ? RESULT_UPDATE_SUCCESSFUL : RESULT_SUBMIT_SUCCESSFUL;
				} catch (Exception e) {
					Log.d(TAG, "Failed to upload device profile. (Exception=" + e.getMessage() + ")");
					e.printStackTrace();
				}
			}
			
			return result;
		}
		
		private InputStream doUpload() throws URISyntaxException, IOException {
			URI uri = new URI("https", "www.veierland.net", "/aix/aix.php",
					"d=" + mTaskDevice + "&u=" + mTaskUser + "&l=" + mTaskIsLandscape +
					"&c=" + mTaskNumColumns + "&r=" + mTaskNumRows +
					"&w=" + mTaskDimension.x + "&h=" + mTaskDimension.y, null);
			
			Log.d(TAG, "Attempting to upload device profile. (URI=" + uri.toString() + ")");
			
			HttpClient httpclient = AixUtils.setupHttpClient(getApplicationContext());
			HttpGet httpGet = new HttpGet(uri);
			HttpResponse response = httpclient.execute(httpGet);
			InputStream content = response.getEntity().getContent();
			
			Log.d(TAG, "Successfully uploaded device profile. (URI=" + uri.toString() + ")");

			return content;
		}
		
		private InputStream doDownload() throws URISyntaxException, IOException {
			URI uri = new URI("https", "www.veierland.net", "/aix/aix.php", "d=" + mTaskDevice, null);
			
			Log.d(TAG, "Attempting to download device profile. (URI=" + uri.toString() + ")");
			
			HttpClient httpclient = AixUtils.setupHttpClient(getApplicationContext());
			HttpGet httpGet = new HttpGet(uri);
			HttpResponse response = httpclient.execute(httpGet);
			InputStream content = response.getEntity().getContent();
			
			Log.d(TAG, "Successfully downloaded device profile. (URI=" + uri.toString() + ")");
			
			return content;
		}
		
	}
	
	private static class AixDeviceProfileResult {
		
		public long timeAdded = -1;
		
		public String status = null;
		public String device = null;
		public String user = null;
		
		public ArrayList<AixDeviceProfileWidgetDimension> widgetDimensions =
				new ArrayList<AixDeviceProfileWidgetDimension>();
		
	}
	
	private static class AixDeviceProfileParser {
		
		private static final int PARSE_STATUS				= 1<<0;
		private static final int PARSE_DEVICE				= 1<<1;
		private static final int PARSE_USER					= 1<<2;
		private static final int PARSE_WIDGET_DIMENSION		= 1<<3;
		private static final int PARSE_TIME_ADDED			= 1<<4;
		private static final int PARSE_LANDSCAPE			= 1<<5;
		private static final int PARSE_COLUMNS				= 1<<6;
		private static final int PARSE_ROWS					= 1<<7;
		private static final int PARSE_WIDTH				= 1<<8;
		private static final int PARSE_HEIGHT				= 1<<9;
		private static final int PARSE_PROMOTED				= 1<<10;
		
		private static AixDeviceProfileResult parse(InputStream content) throws XmlPullParserException, IOException, ParseException {
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
			dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
			
			AixDeviceProfileResult result = new AixDeviceProfileResult();
			AixDeviceProfileWidgetDimension widgetDimension = null;
			
			XmlPullParser parser = Xml.newPullParser();
			
			parser.setInput(content, null);
			int eventType = parser.getEventType();
			int state = 0;
			
			while (eventType != XmlPullParser.END_DOCUMENT) {
				String name = parser.getName();
				String text = parser.getText();
				if (text != null) text = text.trim();
				
				switch (eventType) {
				case XmlPullParser.TEXT:
					if ((state & PARSE_STATUS) != 0) result.status = text;
					else if ((state & PARSE_DEVICE) != 0) result.device = text;
					else if ((state & PARSE_USER) != 0) result.user = text;
					else if ((state & PARSE_TIME_ADDED) != 0) {
						result.timeAdded = dateFormat.parse(text).getTime();
					}
					else if ((state & PARSE_LANDSCAPE) != 0 && widgetDimension != null) {
						widgetDimension.isLandscape = Boolean.parseBoolean(text);
					}
					else if ((state & PARSE_COLUMNS) != 0 && widgetDimension != null) {
						widgetDimension.numColumns = Integer.parseInt(text);
					}
					else if ((state & PARSE_ROWS) != 0 && widgetDimension != null) {
						widgetDimension.numRows = Integer.parseInt(text);
					}
					else if ((state & PARSE_WIDTH) != 0 && widgetDimension != null) {
						widgetDimension.width = Integer.parseInt(text);
					}
					else if ((state & PARSE_HEIGHT) != 0 && widgetDimension != null) {
						widgetDimension.height = Integer.parseInt(text);
					}
					else if ((state & PARSE_PROMOTED) != 0 && widgetDimension != null) {
						widgetDimension.isPromoted = Boolean.parseBoolean(text);
					}
					break;
				case XmlPullParser.END_TAG:
					if (name.equals("status")) state &= ~PARSE_STATUS;
					else if (name.equals("device")) state &= ~PARSE_DEVICE;
					else if (name.equals("user")) state &= ~PARSE_USER;
					else if (name.equals("widget-dimension"))
					{
						state &= ~PARSE_WIDGET_DIMENSION;
						if (widgetDimension != null) {
							result.widgetDimensions.add(widgetDimension);
						}
					}
					else if (name.equals("time-added")) state &= ~PARSE_TIME_ADDED;
					else if (name.equals("landscape")) state &= ~PARSE_LANDSCAPE;
					else if (name.equals("columns")) state &= ~PARSE_COLUMNS;
					else if (name.equals("rows")) state &= ~PARSE_ROWS;
					else if (name.equals("width")) state &= ~PARSE_WIDTH;
					else if (name.equals("height")) state &= ~PARSE_HEIGHT;
					else if (name.equals("promoted")) state &= ~PARSE_PROMOTED;
					break;
				case XmlPullParser.START_TAG:
					if (name.equals("status")) state |= PARSE_STATUS;
					else if (name.equals("device")) state |= PARSE_DEVICE;
					else if (name.equals("user")) state |= PARSE_USER;
					else if (name.equals("widget-dimension"))
					{
						state |= PARSE_WIDGET_DIMENSION;
						widgetDimension = new AixDeviceProfileWidgetDimension();
					}
					else if (name.equals("time-added")) state |= PARSE_TIME_ADDED;
					else if (name.equals("landscape")) state |= PARSE_LANDSCAPE;
					else if (name.equals("columns")) state |= PARSE_COLUMNS;
					else if (name.equals("rows")) state |= PARSE_ROWS;
					else if (name.equals("width")) state |= PARSE_WIDTH;
					else if (name.equals("height")) state |= PARSE_HEIGHT;
					else if (name.equals("promoted")) state |= PARSE_PROMOTED;
					break;
				}
				eventType = parser.next();
			}
			
			return result;
		}
		
	}
	
}
