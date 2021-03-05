package net.veierland.aix;

import java.io.InputStream;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import net.veierland.aix.AixProvider.AixLocations;
import net.veierland.aix.AixProvider.AixLocationsColumns;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.HttpHostConnectException;
import org.json.JSONArray;
import org.json.JSONObject;

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

    private static final List<String> geonamesDetailedNameComponents = Collections.unmodifiableList(
            Arrays.asList("name", "adminName5", "adminName4", "adminName3", "adminName2", "adminName1", "countryName"));

    private static final Map<Integer, String> geonamesWebserviceExceptions;
    static {
        Map<Integer, String> gwe = new HashMap<Integer, String>();
        gwe.put(10, "Authorization exception");
        gwe.put(11, "Record does not exist");
        gwe.put(12, "Other error");
        gwe.put(13, "Database timeout");
        gwe.put(14, "Invalid parameter");
        gwe.put(15, "No result found");
        gwe.put(16, "Duplicate exception");
        gwe.put(17, "Postal code not found");
        gwe.put(18, "Daily limit of credits exceeded");
        gwe.put(19, "Hourly limit of credits exceeded");
        gwe.put(20, "Weekly limit of credits exceeded");
        gwe.put(21, "Invalid input");
        gwe.put(22, "Server overloaded exception");
        gwe.put(23, "Service not implemented");
        gwe.put(24, "Radius too large");
        geonamesWebserviceExceptions = Collections.unmodifiableMap(gwe);
    }

    private static String buildTitleDetailed(final JSONObject result) {
        StringBuilder titleDetailedSb = new StringBuilder();

        for (String key : geonamesDetailedNameComponents) {
            if (result.has(key)) {
                String component = result.optString(key).trim();

                if (component.length() > 0) {
                    if (titleDetailedSb.length() > 0) {
                        titleDetailedSb.append(", ");
                    }
                    titleDetailedSb.append(component);
                }
            }
        }

        return titleDetailedSb.toString();
    }

	private static final String TAG = "AixLocationSelection";
	
	private static final int DIALOG_ADD = 0;
	private static final int DIALOG_EDIT = 1;
	
	private static final int CONTEXT_MENU_EDIT = Menu.FIRST;
	private static final int CONTEXT_MENU_DELETE = Menu.FIRST + 1;
	
	private Button mAddLocationButton = null;
	private Context mContext = null;
	private Cursor mCursor = null;
	private boolean mResetSearch = false;
	private EditText mEditText = null;
	
	@SuppressWarnings("deprecation") // startManagingCursor() and SimpleCursorAdapter constructor is deprecated
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
		setContentView(R.layout.location_selection_list);
		
		mAddLocationButton = (Button) findViewById(R.id.add_location_button);
		mAddLocationButton.setOnClickListener(this);
		
		ContentResolver cr = getContentResolver();
        mCursor = cr.query(AixLocations.CONTENT_URI, null, null, null, null);
        
        startManagingCursor(mCursor);

        setListAdapter(new SimpleCursorAdapter(
        		mContext,
        		R.layout.location_selection_row,
        		mCursor,
        		new String[] {
        				AixLocationsColumns.TITLE_DETAILED,
        				AixLocationsColumns.TITLE,
        				AixLocationsColumns.LATITUDE,
        				AixLocationsColumns.LONGITUDE
        		},
        		new int[] {
        				R.id.location_selection_row_title,
        				R.id.location_selection_row_display_title,
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
		menu.add(0, CONTEXT_MENU_EDIT, 0, "Edit display title");
		menu.add(0, CONTEXT_MENU_DELETE, 0, getString(R.string.location_list_context_delete));
	}

	private String mLocationName;
	private long mLocationId;
	
	// Cursor.requery() and showDialog() are deprecated
	@SuppressWarnings("deprecation")
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo adapterMenuInfo = (AdapterContextMenuInfo) (item.getMenuInfo());
		switch (item.getItemId()) {
		case CONTEXT_MENU_DELETE:
			mCursor.moveToPosition(adapterMenuInfo.position);
			if (mCursor.isAfterLast()) return false;
			getContentResolver().delete(
					ContentUris.withAppendedId(AixLocations.CONTENT_URI, mCursor.getLong(mCursor.getColumnIndexOrThrow(BaseColumns._ID))),
					null, null);
			mCursor.requery();
			return true;
		case CONTEXT_MENU_EDIT:
			mCursor.moveToPosition(adapterMenuInfo.position);
			if (mCursor.isAfterLast()) return false;
			mLocationId = mCursor.getLong(mCursor.getColumnIndexOrThrow(BaseColumns._ID));
			mLocationName = mCursor.getString(mCursor.getColumnIndexOrThrow(AixLocations.TITLE));
			showDialog(DIALOG_EDIT);
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
		
		View content = getLayoutInflater().inflate(R.layout.dialog_edittext, null);
		mEditText = (EditText) content.findViewById(R.id.edittext);
		mEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE);
		
		switch (id) {
		case DIALOG_ADD:
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
		case DIALOG_EDIT:
			dialog = new AlertDialog.Builder(this)
					.setTitle("Display title:")
					.setView(content)
					.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
	                		imm.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
	                		String displayTitle = mEditText.getText().toString();
							
	                		if (TextUtils.isEmpty(displayTitle)) {
	                			Toast.makeText(AixLocationSelectionActivity.this, "Invalid display title", Toast.LENGTH_SHORT).show();
	                		} else {
								setLocationDisplayTitle(displayTitle);
	                		}
						}
					})
					.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
	                		imm.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
	                    	dialog.cancel();
						}
					}).create();
			break;
		}

        return dialog;
	}
	
	// Cursor.requery() is deprecated
	@SuppressWarnings("deprecation")
	private void setLocationDisplayTitle(String displayTitle) {
		ContentValues values = new ContentValues();
		values.put(AixLocations.TITLE, displayTitle);
		getContentResolver().update(
				ContentUris.withAppendedId(AixLocations.CONTENT_URI, mLocationId),
				values, null, null);
		mCursor.requery();
	}
	
	// Activity.onPrepareDialog() is deprecated
	@SuppressWarnings("deprecation")
	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		final EditText editText = (EditText) dialog.findViewById(R.id.edittext);
		
		switch (id) {
		case DIALOG_ADD:
			if (mResetSearch) {
				editText.setText("");
			} else {
				editText.setSelection(editText.length());
			}
			break;
		case DIALOG_EDIT:
			editText.setText(mLocationName);
			editText.setSelection(mEditText.getText().length());
			break;
		default:
			super.onPrepareDialog(id, dialog);
		}
	}
	
	private class LocationSearchTask extends AsyncTask<String, Integer, Integer> implements DialogInterface.OnCancelListener {
		
		private final static int MAX_RESULTS = 7;
		
		private final static int INVALID_INPUT = 0;
		private final static int NO_CONNECTION = 1;
		private final static int SEARCH_CANCELLED = 2;
		private final static int SEARCH_SUCCESS = 4;
		private final static int NO_RESULTS = 5;
		private final static int OVER_QUERY_LIMIT = 6;
		private final static int REQUEST_DENIED = 7;
		private final static int INVALID_REQUEST = 8;
		private final static int SEARCH_ERROR = 100;

		private int mAttempts = 0;
		private List<AixAddress> mAddresses;
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			mProgressDialog = ProgressDialog.show(
					mContext, getString(R.string.location_search_progress_dialog_title), getString(R.string.location_search_progress_dialog_message), true, true, this);
		}
		
		@Override
		protected void onPostExecute(Integer result) {
			super.onPostExecute(result);
			if (mProgressDialog != null) {
				mProgressDialog.dismiss();
			}

			if (result >= SEARCH_ERROR) {
			    final int errorCode = result - SEARCH_ERROR;
			    final String errorString = geonamesWebserviceExceptions.containsKey(errorCode)
                        ? geonamesWebserviceExceptions.get(errorCode)
                        : getString(R.string.location_search_error_toast);
                Toast.makeText(mContext, errorString, Toast.LENGTH_SHORT).show();
                return;
            }

			switch (result) {
			case INVALID_INPUT:
				mResetSearch = true;
				Toast.makeText(
						mContext,
						getString(R.string.invalid_search_input_toast),
						Toast.LENGTH_SHORT).show();
				break;
			case NO_CONNECTION:
				Toast.makeText(
						mContext,
						getString(R.string.location_search_no_connection_toast),
						Toast.LENGTH_SHORT).show();
				break;
			case SEARCH_CANCELLED:
				Log.d(TAG, "Search was cancelled!");
				break;
			case SEARCH_SUCCESS:
				mResetSearch = true;
				
				String[] listItems = new String[mAddresses.size()];
				for (int i = 0; i < mAddresses.size(); i++) {
					listItems[i] = mAddresses.get(i).title_detailed;
				}
				
				AlertDialog alertDialog = new AlertDialog.Builder(mContext)
						.setTitle(R.string.location_search_results_select_dialog_title)
						.setItems(listItems, new DialogInterface.OnClickListener() {
							// Cursor.requery() is deprecated
							@SuppressWarnings("deprecation")
							@Override
							public void onClick(DialogInterface dialog, int which) {
								// Add selected location to provider
								AixAddress a = mAddresses.get(which);
								ContentResolver resolver = mContext.getContentResolver();
								ContentValues values = new ContentValues();
								values.put(AixLocationsColumns.LATITUDE, a.latitude);
								values.put(AixLocationsColumns.LONGITUDE, a.longitude);
								values.put(AixLocationsColumns.TITLE, a.title);
								values.put(AixLocationsColumns.TITLE_DETAILED, a.title_detailed);
								resolver.insert(AixLocations.CONTENT_URI, values);

								mCursor.requery();
								getListView().setSelection(getListView().getCount() - 1);
							}
						})
						.create();
				alertDialog.show();
				break;
			case NO_RESULTS:
				Toast.makeText(
						AixLocationSelectionActivity.this,
						getString(R.string.location_search_no_results),
						Toast.LENGTH_SHORT).show();
				break;
			case OVER_QUERY_LIMIT:
				Toast.makeText(
						AixLocationSelectionActivity.this,
						getString(R.string.location_search_over_query_limit_toast),
						Toast.LENGTH_LONG).show();
				break;
			case REQUEST_DENIED:
				Toast.makeText(
						AixLocationSelectionActivity.this,
						getString(R.string.location_search_request_denied_toast),
						Toast.LENGTH_SHORT).show();
				break;
			case INVALID_REQUEST:
				Toast.makeText(
						AixLocationSelectionActivity.this,
						getString(R.string.location_search_invalid_request_toast),
						Toast.LENGTH_SHORT).show();
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
			if (params.length != 1 || TextUtils.isEmpty(params[0])) {
				return INVALID_INPUT;
			}
			
			Log.d(TAG, "Starting search for " + params[0]);
			
			while (!isCancelled()) {
				mAttempts++;
				
				try {
                    URI uri = new URI("http", "api.geonames.org", "/searchJSON",
                            "q=" + params[0].trim() +
                                    "&lang=" + Locale.getDefault().getLanguage() +
                                    "&maxRows=" + MAX_RESULTS +
                                    "&username=aix_widget", null);

					HttpGet httpGet = new HttpGet(uri);
					httpGet.addHeader("Accept-Encoding", "gzip");

					HttpClient httpclient = AixUtils.setupHttpClient(mContext);
					HttpResponse response = httpclient.execute(httpGet);
					InputStream content = response.getEntity().getContent();
					
					Header contentEncoding = response.getFirstHeader("Content-Encoding");
					if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
						content = new GZIPInputStream(content);
					}

                    JSONObject jObject = new JSONObject(AixUtils.convertStreamToString(content));

					if (jObject.has("status")) {
                        int errorCode = jObject.getJSONObject("status").optInt("value", 0);
                        return SEARCH_ERROR + errorCode;
                    }

					mAddresses = new ArrayList<AixAddress>();

					JSONArray results = jObject.getJSONArray("geonames");
					int numResults = Math.min(results.length(), MAX_RESULTS);

                    if (numResults <= 0) {
                        return NO_RESULTS;
                    }

					for (int i = 0; i < numResults; i++) {
						try {
							JSONObject result = results.getJSONObject(i);
							
							AixAddress address = new AixAddress();
							address.title = result.getString("name");
							address.title_detailed = buildTitleDetailed(result);
							address.latitude = result.getString("lat");
							address.longitude = result.getString("lng");
							mAddresses.add(address);
						} catch (Exception e) { }
					}
					
					if (mAddresses != null && mAddresses.size() > 0) {
						return SEARCH_SUCCESS;
					} else {
						return NO_RESULTS;
					}
				} catch (HttpHostConnectException e) {
					return NO_CONNECTION;
				} catch (UnknownHostException e) {
					return NO_CONNECTION;
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				publishProgress(mAttempts);
				
				try {
					Thread.sleep(2222);
				} catch (InterruptedException e) { }
			}

			return SEARCH_CANCELLED;
		}

		@Override
		public void onCancel(DialogInterface dialog) {
			cancel(false);
		}
		
	}
	
	// Activity.showDialog() is deprecated
	@SuppressWarnings("deprecation")
	@Override
	public void onClick(View v) {
		if (v == mAddLocationButton) {
			showDialog(DIALOG_ADD);
		}
	}
	
	private static class AixAddress {
		
		public String title, title_detailed, latitude, longitude;
		
	}
	
}
