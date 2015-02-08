package net.veierland.aix;

import java.util.List;
import java.util.Locale;

import net.veierland.aix.AixProvider.AixLocations;
import net.veierland.aix.AixProvider.AixLocationsColumns;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

public class AixLocationSelectionActivity extends ListActivity implements OnClickListener {

	private static final String TAG = "AixLocationSelectionActivity";
	
	private static final int DIALOG_ADD = 0;
	
	private static final int CONTEXT_MENU_DELETE = Menu.FIRST;
	
	private Button mAddLocationButton = null;
	private Context mContext = null;
	private Cursor mCursor = null;
	private boolean mResetSearch = false;
	private EditText mEditText = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
		setContentView(R.layout.aix_location_selection_list);
		
		mAddLocationButton = (Button) findViewById(R.id.add_location_button);
		mAddLocationButton.setOnClickListener(this);
		
		ContentResolver cr = getContentResolver();
        mCursor = cr.query(AixLocations.CONTENT_URI, null, null, null, null);
        
        startManagingCursor(mCursor);

        setListAdapter(new SimpleCursorAdapter(
        		mContext,
        		R.layout.aix_location_selection_row,
        		mCursor,
        		new String[] {
        				AixLocationsColumns.TITLE_DETAILED,
        				AixLocationsColumns.LATITUDE,
        				AixLocationsColumns.LONGITUDE
        		},
        		new int[] {
        				R.id.location_selection_row_title,
        				R.id.location_selection_row_latitude,
        				R.id.location_selection_row_longitude
        		}));
        
        registerForContextMenu(getListView());
    }

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Intent intent = new Intent();
		String result = ContentUris.withAppendedId(AixLocations.CONTENT_URI, id).toString();
		intent.putExtra("location", result);
		setResult(Activity.RESULT_OK, intent);
		finish();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		AdapterContextMenuInfo adapterMenuInfo = (AdapterContextMenuInfo) (menuInfo);
		mCursor.moveToPosition(adapterMenuInfo.position);
		if (mCursor.isAfterLast()) return;
		menu.setHeaderTitle(String.format(getString(R.string.location_list_context_title), mCursor.getString(mCursor.getColumnIndexOrThrow(AixLocationsColumns.TITLE))));
		menu.add(0, CONTEXT_MENU_DELETE, 0, getString(R.string.location_list_context_delete));
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo adapterMenuInfo = (AdapterContextMenuInfo) (item.getMenuInfo());
		switch (item.getItemId()) {
		case CONTEXT_MENU_DELETE:
			mCursor.moveToPosition(adapterMenuInfo.position);
			if (mCursor.isAfterLast()) return false;
			getContentResolver().delete(ContentUris.withAppendedId(AixLocations.CONTENT_URI, mCursor.getLong(mCursor.getColumnIndexOrThrow(BaseColumns._ID))), null, null);
			mCursor.requery();
			return true;
		}
		return false;
	}
	
	private ProgressDialog mProgressDialog = null;
	private LocationSearchTask mLocationSearchTask = null;
	
	@Override
	protected void onPause() {
		if (mProgressDialog != null) {
			mLocationSearchTask.cancel(false);
			mProgressDialog.dismiss();
		}
		super.onPause();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = null;
		switch (id) {
		case DIALOG_ADD:
			View content = getLayoutInflater().inflate(R.layout.dialog_edittext, null);
			mEditText = (EditText) content.findViewById(R.id.edittext);
			mEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE);
			
			dialog = new AlertDialog.Builder(this)
					.setTitle(R.string.dialog_search_location)
	                .setView(content)
	                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
	                    public void onClick(DialogInterface dialog, int which) {
	                    	InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
	                		imm.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
	                		String searchString = mEditText.getText().toString();
	                		if (!TextUtils.isEmpty(searchString)) {
	                			mLocationSearchTask = new LocationSearchTask();
	                			mLocationSearchTask.execute(searchString);
	                    	} else {
	                    		mEditText.setText("");
	                    		Toast.makeText(mContext, getString(R.string.location_empty_search_string_toast), Toast.LENGTH_SHORT).show();
	                    	}
	                    }})
	                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
	                    public void onClick(DialogInterface dialog, int which) {
	                    	InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
	                		imm.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
	                    	dialog.cancel();
	                    }})
	                .create();
			
			dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN |
					WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
	        break;
		}

        return dialog;
	}
	
	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		switch (id) {
		case DIALOG_ADD:
			if (mResetSearch) {
				final EditText editText = (EditText) dialog.findViewById(R.id.edittext);
				editText.setText("");
			}
			break;
			default:
				super.onPrepareDialog(id, dialog);
		}
	}
	
	private String[] mAddressList = null;
	private String[] mAddressListDetailed = null;
	
	private class LocationSearchTask extends AsyncTask<String, Integer, Integer> implements DialogInterface.OnCancelListener {
		
		private final static int MAX_RESULTS = 5;
		
		private final static int INVALID_INPUT = 0;
		private final static int SEARCH_CANCELLED = 1;
		private final static int NO_RESULTS = 2;
		private final static int SEARCH_SUCCESS = 3;

		private int mAttempts = 0;
		private List<Address> mAddresses;
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			mProgressDialog = ProgressDialog.show(
					mContext, getString(R.string.location_search_progress_dialog_title), getString(R.string.location_search_progress_dialog_message), true, true, this);
		}
		
		@Override
		protected void onPostExecute(Integer result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			if (mProgressDialog != null) {
				mProgressDialog.dismiss();
			}
			
			switch (result.intValue()) {
			case INVALID_INPUT:
				mResetSearch = true;
				Toast.makeText(
						mContext,
						getString(R.string.invalid_search_input_toast),
						Toast.LENGTH_SHORT).show();
				break;
			case SEARCH_CANCELLED:
				Log.d(TAG, "Search was cancelled!");
				break;
			case NO_RESULTS:
				Toast.makeText(
						getApplicationContext(),
						getString(R.string.location_search_no_results),
						Toast.LENGTH_SHORT).show();
				break;
			case SEARCH_SUCCESS:
				mResetSearch = true;
				mAddressList = new String[mAddresses.size()];
				mAddressListDetailed = new String[mAddresses.size()];
				for (int i = 0; i < mAddresses.size(); i++) {
					Address address = mAddresses.get(i);
					mAddressList[i] = buildLocationTitle(address);
					mAddressListDetailed[i] = buildDetailedLocationTitle(address);
				}
				AlertDialog alertDialog = new AlertDialog.Builder(mContext)
						.setTitle(R.string.location_search_results_select_dialog_title)
						.setItems(mAddressListDetailed, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								// Add selected location to provider
								Address a = mAddresses.get(which);
								ContentResolver resolver = mContext.getContentResolver();
								ContentValues values = new ContentValues();
								values.put(AixLocationsColumns.LATITUDE, a.getLatitude());
								values.put(AixLocationsColumns.LONGITUDE, a.getLongitude());
								values.put(AixLocationsColumns.TITLE, mAddressList[which]);
								values.put(AixLocationsColumns.TITLE_DETAILED, mAddressListDetailed[which]);
								resolver.insert(AixLocations.CONTENT_URI, values);

								mCursor.requery();
								getListView().setSelection(getListView().getCount() - 1);
							}
						})
						.create();
				alertDialog.show();
				break;
			}
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			super.onProgressUpdate(values);
			if (!isCancelled()) {
				mProgressDialog.setMessage(getString(R.string.location_search_progress_dialog_message) + " (" + values[0] + ")");
			}
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
			Log.d(TAG, "Cancelled!");
		}
		
		@Override
		protected Integer doInBackground(String... params) {
			Log.d(TAG, "Hard at work!");

			if (params.length != 1 || TextUtils.isEmpty(params[0])) {
				return INVALID_INPUT;
			}
			
			while (!isCancelled()) {
				mAttempts++;
				
				try {
					Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
					mAddresses = geocoder.getFromLocationName(params[0].trim(), MAX_RESULTS);
					if (mAddresses != null && mAddresses.size() > 0) {
						return SEARCH_SUCCESS;
					} else {
						return NO_RESULTS;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				publishProgress(mAttempts);
			}

			return SEARCH_CANCELLED;
		}

		@Override
		public void onCancel(DialogInterface dialog) {
			cancel(false);
		}
		
	}
	
	private String buildLocationTitle(Address address) {
		if (!TextUtils.isEmpty(address.getLocality())) {
			return address.getLocality();
		} else if (!TextUtils.isEmpty(address.getAddressLine(0))) {
			return address.getAddressLine(0);
		} else {
			return address.toString();
		}
	}
	
	private String buildDetailedLocationTitle(Address address) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 5; i++) {
			String addressLine = address.getAddressLine(i);
			if (addressLine != null) {
				if (sb.length() > 0) sb.append(", ");
				sb.append(addressLine);
			}
		}
		return sb.toString();
	}
	
	@Override
	public void onClick(View v) {
		if (v == mAddLocationButton) {
			showDialog(DIALOG_ADD);
		}
	}
	
}
