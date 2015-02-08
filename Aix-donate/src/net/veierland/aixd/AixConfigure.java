package net.veierland.aixd;

import java.util.Arrays;

import net.veierland.aixd.AixProvider.AixLocationsColumns;
import net.veierland.aixd.AixProvider.AixViews;
import net.veierland.aixd.AixProvider.AixViewsColumns;
import net.veierland.aixd.AixProvider.AixWidgets;
import net.veierland.aixd.AixProvider.AixWidgetsColumns;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.*;
import android.provider.BaseColumns;
import android.widget.Button;
import android.widget.Toast;
import android.util.Log;
import android.view.View;

public class AixConfigure extends PreferenceActivity implements View.OnClickListener, Preference.OnPreferenceClickListener {

	private long mLocationId = -1;
	private Button mAddWidgetButton = null;
	private Preference mLocationPreference = null;
	
	private final static int SELECT_LOCATION = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.aix_configure);
		addPreferencesFromResource(R.xml.aix_configuration);
		
		mLocationPreference = findPreference("select_location");
		mLocationPreference.setOnPreferenceClickListener(this);
		
		mAddWidgetButton = (Button) findViewById(R.id.add_widget_button);
		mAddWidgetButton.setOnClickListener(this);
		
		setResult(Activity.RESULT_CANCELED);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
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
	}

	@Override
	public void onClick(View v) {
		if (v == mAddWidgetButton) {
			if (mLocationId == -1) {
				Toast.makeText(this, "You must set a location", Toast.LENGTH_SHORT).show();
			} else {
				Intent intent = getIntent();
				Bundle extras = intent.getExtras();
				int appWidgetId = -1;
				if (extras != null) {
					appWidgetId = extras.getInt(
							AppWidgetManager.EXTRA_APPWIDGET_ID,
							AppWidgetManager.INVALID_APPWIDGET_ID);
					if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
						
						ContentResolver resolver = getContentResolver();
						
						ContentValues values = new ContentValues();
						values.put(AixViewsColumns.LOCATION, mLocationId);
						values.put(AixViewsColumns.UNITS, ((ListPreference)findPreference("measurement_unit")).getValue());
						Uri viewUri = resolver.insert(AixViews.CONTENT_URI, values);
						
						if (viewUri != null) {
							values.clear();
							values.put(BaseColumns._ID, appWidgetId);
							values.put(AixWidgetsColumns.VIEWS, viewUri.getLastPathSegment());
							getContentResolver().insert(AixWidgets.CONTENT_URI, values);
							
							AixService.requestUpdate(appWidgetId);
							startService(new Intent(this, AixService.class));
							
							Intent resultIntent = new Intent();
							resultIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
							setResult(Activity.RESULT_OK, resultIntent);
						}
					}
				}
				finish();
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
	
}