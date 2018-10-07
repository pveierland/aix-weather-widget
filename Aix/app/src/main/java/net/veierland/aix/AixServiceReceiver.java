package net.veierland.aix;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AixServiceReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {
		AixService.enqueueWork(context, intent);
	}

}
