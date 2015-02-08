package net.veierland.aix.util;

import net.veierland.aix.AixProvider.AixLocations;
import net.veierland.aix.AixProvider.AixViews;
import net.veierland.aix.AixProvider.AixViewsColumns;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

public class AixViewInfo {
	
	private long _id = -1;
	
	//private boolean _modified = false;
	
	private Long _location;
	private Integer _type;
	
	private AixLocationInfo _locationInfo = null;
	
	public AixViewInfo() { }
	
	public AixViewInfo(long location, int type) {
		_location = location;
		_type = type;
	}
	
	public static AixViewInfo build(Context context, Uri viewUri) throws Exception {		
		if (context == null)
		{
			throw new IllegalArgumentException("AixViewInfo.build() failed: context must be non-null");
		}
		if (viewUri == null)
		{
			throw new IllegalArgumentException("AixViewInfo.build() failed: viewUri must be non-null");
		}
		
		AixViewInfo viewInfo = null;
		
		ContentResolver resolver = context.getContentResolver();
		Cursor viewCursor = resolver.query(viewUri, null, null, null, null);
		
		if (viewCursor != null) {
			try {
				if (viewCursor.moveToFirst()) {
					viewInfo = buildFromCursor(viewCursor);
				}
			} catch (Exception e) {
				// Bad things happened :<
				e.printStackTrace();
			} finally {
				viewCursor.close();
			}
		}
		
		if (viewInfo != null) {
			viewInfo.setupLocation(context);
		} else {
			throw new Exception("AixViewInfo.build() failed: Could not build AixViewInfo (uri=" + viewUri + ")");
		}
		
		return viewInfo;
	}
	
	public ContentValues buildContentValues()
	{
		ContentValues values = new ContentValues();
		values.put(AixViewsColumns.LOCATION, _location);
		values.put(AixViewsColumns.TYPE, _type);
		return values;
	}
	
	public static AixViewInfo buildFromCursor(Cursor c) {
		AixViewInfo viewInfo = new AixViewInfo();
		
		viewInfo._id = c.getLong(c.getColumnIndexOrThrow(BaseColumns._ID));
		
		int locationColumn = c.getColumnIndex(AixViewsColumns.LOCATION);
		if (locationColumn != -1) viewInfo._location = c.getLong(locationColumn);
		
		int typeColumn = c.getColumnIndex(AixViewsColumns.TYPE);
		if (typeColumn != -1) viewInfo._type = c.getInt(typeColumn);
		
		return viewInfo;
	}

	public Uri commit(Context context)
	{
		Uri viewUri = null;
		
		if (_locationInfo != null)
		{
			Uri locationUri = _locationInfo.commit(context);
			_location = ContentUris.parseId(locationUri);
		}
		
		//if (_modified)
		//{
			ContentResolver resolver = context.getContentResolver();
			ContentValues values = buildContentValues();
			
			if (_id == -1)
			{
				viewUri = resolver.insert(AixViews.CONTENT_URI, values);
				_id = ContentUris.parseId(viewUri);
			}
			else
			{
				viewUri = ContentUris.withAppendedId(AixViews.CONTENT_URI, _id);
				resolver.update(viewUri, values, null, null);
			}
			
		//}
		return viewUri;
	}
	
	public long getId() {
		return _id;
	}
	
	public Long getLocation() {
		return _location;
	}
	
	public AixLocationInfo getLocationInfo() {
		return _locationInfo;
	}
	
	public Integer getType() {
		return _type;
	}
	
	//public boolean isModified() {
		//return _modified;
	//}
	
	public void setId(long id) {
		_id = id;
		//_modified = true;
	}
	
	public void setLocation(Long location) {
		_location = location;
		_locationInfo = null;
		//_modified = true;
	}
	
	public void setLocationInfo(AixLocationInfo locationInfo) {
		_locationInfo = locationInfo;
		//_modified = true;
	}
	
	public void setType(Integer type) {
		_type = type;
		//_modified = true;
	}
	
	public void setupLocation(Context context) throws Exception {
		Uri locationUri = ContentUris.withAppendedId(AixLocations.CONTENT_URI, _location);
		_locationInfo = AixLocationInfo.build(context, locationUri);
	}
	
}
