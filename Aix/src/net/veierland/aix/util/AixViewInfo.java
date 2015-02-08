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

public class AixViewInfo {
	
	private int mType;
	private AixLocationInfo mAixLocationInfo;
	private Uri mViewUri;
	
	public AixViewInfo(Uri viewUri, AixLocationInfo aixLocationInfo, int type)
	{
		mViewUri = viewUri;
		mAixLocationInfo = aixLocationInfo;
		mType = type;
	}
	
	public static AixViewInfo build(Context context, Uri viewUri)
			throws Exception
	{
		Cursor cursor = null;
		try
		{
			ContentResolver contentResolver = context.getContentResolver();
			cursor = contentResolver.query(viewUri, null, null, null, null);
			
			if (cursor != null && cursor.moveToFirst())
			{
				AixLocationInfo aixLocationInfo = null;
				
				int columnIndex = cursor.getColumnIndex(AixViewsColumns.LOCATION);
				if (columnIndex != -1 && !cursor.isNull(columnIndex))
				{
					long locationId = cursor.getLong(columnIndex);
					Uri aixLocationUri = ContentUris.withAppendedId(AixLocations.CONTENT_URI, locationId);
					aixLocationInfo = AixLocationInfo.build(context, aixLocationUri);
				}
				
				int type = AixViewsColumns.TYPE_DETAILED;
				columnIndex = cursor.getColumnIndex(AixViewsColumns.TYPE);
				if (columnIndex != -1 && !cursor.isNull(columnIndex))
				{
					type = cursor.getInt(columnIndex);
				}
				
				return new AixViewInfo(viewUri, aixLocationInfo, type);
			}
			else
			{
				throw new Exception("Failed to build AixViewInfo");
			}
		}
		finally
		{
			if (cursor != null)
			{
				cursor.close();
			}
		}
	}
	
	public ContentValues buildContentValues()
	{
		ContentValues values = new ContentValues();
		
		if (mAixLocationInfo != null)
		{
			values.put(AixViewsColumns.LOCATION, mAixLocationInfo.getId());
		}
		else
		{
			values.putNull(AixViewsColumns.LOCATION);
		}
		
		values.put(AixViewsColumns.TYPE, mType);
		
		return values;
	}

	public Uri commit(Context context)
	{
		if (mAixLocationInfo != null)
		{
			mAixLocationInfo.commit(context);
		}
		
		ContentValues values = buildContentValues();
		ContentResolver resolver = context.getContentResolver();
		
		if (mViewUri != null)
		{
			resolver.update(mViewUri, values, null, null);
		}
		else
		{
			mViewUri = resolver.insert(AixViews.CONTENT_URI, values);
		}
		
		return mViewUri;
	}
	
	public long getId() {
		if (mViewUri != null)
		{
			return ContentUris.parseId(mViewUri);
		}
		else
		{
			return -1;
		}
	}
	
	public AixLocationInfo getLocationInfo() {
		return mAixLocationInfo;
	}
	
	public int getType() {
		return mType;
	}

	public void setAixLocationInfo(AixLocationInfo aixLocationInfo)
	{
		mAixLocationInfo = aixLocationInfo;
	}
	
	public void setType(int type)
	{
		mType = type;
	}
	
	public void setUri(Uri uri)
	{
		mViewUri = uri;
	}
	
	@Override
	public String toString() {
		return "AixViewInfo(" + mViewUri + "," + mType + "," + mAixLocationInfo + ")";
	}
	
}
