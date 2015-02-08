package net.veierland.aixd;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.format.DateUtils;
import android.util.Log;

public class AixServiceReceiver extends BroadcastReceiver {
	private static final String TAG = "AixServiceReceiver";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		//String action = intent.getAction();
		
		//if (Intent.ACTION_CONFIGURATION_CHANGED.equals(action)) {
			// Orientation has changed.
			//Log.d(TAG, "ORIENTATION HAS CHANGED!!!");
			// Update all widgets
			//Intent updateIntent = new Intent(AixService.ACTION_UPDATE_ALL);
			//updateIntent.setClass(this, AixService.class);
			//context.startService(updateIntent);
		//} else
			
			/*
			if ( Intent.ACTION_TIME_CHANGED.equals(action) ||
					Intent.ACTION_TIMEZONE_CHANGED.equals(action) ||
					Intent.ACTION_DATE_CHANGED.equals(action))
		{*/
			/* Set an alarm to run a full update in 15 seconds */
			long updateTime = System.currentTimeMillis() + 15 * DateUtils.SECOND_IN_MILLIS;
			
			Intent updateIntent = new Intent(AixService.ACTION_UPDATE_ALL);
			updateIntent.setClass(context, AixService.class);
			
			PendingIntent pendingIntent = PendingIntent.getService(context, 0, updateIntent, 0);
			AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
			alarmManager.set(AlarmManager.RTC_WAKEUP, updateTime, pendingIntent);
		//}
	}

}
