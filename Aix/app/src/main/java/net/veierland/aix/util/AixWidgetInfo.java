package net.veierland.aix.util;

import net.veierland.aix.AixProvider.AixViews;
import net.veierland.aix.AixProvider.AixWidgets;
import net.veierland.aix.AixProvider.AixWidgetsColumns;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class AixWidgetInfo {
	
	private int mAppWidgetId, mSize;
	
	private AixViewInfo mAixViewInfo = null;
	
	private AixWidgetSettings mAixWidgetSettings = null;
	
	public AixWidgetInfo(int appWidgetId, int size, AixViewInfo aixViewInfo) {
		mAppWidgetId = appWidgetId;
		mSize = size;
		mAixViewInfo = aixViewInfo;
	}
	
	public static AixWidgetInfo build(Context context, Uri widgetUri) throws Exception {
		Cursor cursor = null;
		
		try {
			ContentResolver resolver = context.getContentResolver();
			cursor = resolver.query(widgetUri, null, null, null, null);
			Log.d("AixWidgetInfo", "cursor" + cursor);
			if (cursor != null && cursor.moveToFirst())
			{
				int columnIndex = cursor.getColumnIndexOrThrow(AixWidgetsColumns.APPWIDGET_ID);
				int appWidgetId = cursor.getInt(columnIndex);
				
				int size = -1;
				columnIndex = cursor.getColumnIndex(AixWidgetsColumns.SIZE);
				if (columnIndex != -1 && !cursor.isNull(columnIndex))
				{
					size = cursor.getInt(columnIndex);
				}
				// Size may be invalid due to old versions. If non-valid, default to 4x1		
				if (!(size >= 1 && size <= 16))
				{
					size = AixWidgetsColumns.SIZE_LARGE_TINY;
				}
				
				AixViewInfo aixViewInfo = null;
				columnIndex = cursor.getColumnIndex(AixWidgetsColumns.VIEWS);
				if (columnIndex != -1 && !cursor.isNull(columnIndex))
				{
					String viewString = cursor.getString(columnIndex);
					Uri viewUri = AixViews.CONTENT_URI.buildUpon().appendPath(viewString).build();
					aixViewInfo = AixViewInfo.build(context, viewUri);
				}
				
				return new AixWidgetInfo(appWidgetId, size, aixViewInfo);
			}
			else
			{
				throw new Exception("Failed to build AixWidgetInfo");
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
	
	public ContentValues buildContentValues() {
		ContentValues values = new ContentValues();
		values.put(AixWidgetsColumns.APPWIDGET_ID, mAppWidgetId);
		values.put(AixWidgetsColumns.SIZE, mSize);
		
		if (mAixViewInfo != null)
		{
			values.put(AixWidgetsColumns.VIEWS, Long.toString(mAixViewInfo.getId()));
		}
		else
		{
			values.putNull(AixWidgetsColumns.VIEWS);
		}
		
		return values;
	}
	
	public Uri commit(Context context)
	{
		if (mAixViewInfo != null)
		{
			mAixViewInfo.commit(context);
		}
		
		ContentValues values = buildContentValues();
		ContentResolver resolver = context.getContentResolver();
		
		Uri widgetUri = resolver.insert(AixWidgets.CONTENT_URI, values);
		
		return widgetUri;
	}
	
	public int getAppWidgetId() {
		return mAppWidgetId;
	}
	
	public int getNumColumns() {
		return (mSize - 1) / 4 + 1;
	}
	
	public int getNumRows() {
		return (mSize - 1) % 4 + 1;
	}
	
	public AixViewInfo getViewInfo()
	{
		return mAixViewInfo;
	}
	
	public AixWidgetSettings getWidgetSettings()
	{
		return mAixWidgetSettings;
	}
	
	public Uri getWidgetUri() {
		return ContentUris.withAppendedId(AixWidgets.CONTENT_URI, mAppWidgetId);
	}
	
	public void loadSettings(Context context)
	{
		mAixWidgetSettings = AixWidgetSettings.build(context, getWidgetUri());
	}
	
	public void setViewInfo(AixViewInfo viewInfo)
	{
		mAixViewInfo = viewInfo;
	}
	
	public void setViewInfo(AixLocationInfo aixLocationInfo, int type)
	{
		if (mAixViewInfo != null)
		{
			mAixViewInfo.setAixLocationInfo(aixLocationInfo);
			mAixViewInfo.setType(type);
		}
		else
		{
			mAixViewInfo = new AixViewInfo(null, aixLocationInfo, type);
		}
	}
	
	public String toString() {
		return "AixWidgetInfo(" + mAppWidgetId + "," + mSize + "," + mAixViewInfo + ")";
	}
	
}
