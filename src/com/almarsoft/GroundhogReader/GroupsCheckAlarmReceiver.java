/*
Groundhog Usenet Reader
Copyright (C) 2008-2010  Juan Jose Alvarez Martinez

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/

package com.almarsoft.GroundhogReader;

import java.lang.reflect.Method;
import java.util.Vector;

import com.almarsoft.GroundhogReader.lib.DBUtils;
import com.almarsoft.GroundhogReader.lib.ServerManager;
import com.almarsoft.GroundhogReader.lib.ServerMessageGetter;
import com.almarsoft.GroundhogReader.lib.UsenetConstants;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;

public class GroupsCheckAlarmReceiver extends BroadcastReceiver{
	
	private final int CHECK_FINISHED_OK = 5;
	
	private ServerMessageGetter mServerMessageGetter = null;
	private Context mContext = null;
	private WakeLock mLock = null;
	private SharedPreferences mPrefs = null;
	
	@SuppressWarnings("unchecked")
	@Override
	public void onReceive(Context context, Intent intent) {
		
		mContext = context;
		mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		// Check if we're under wifi and the option to only check under wifi is enabled
		if (mPrefs.getBoolean("notif_wifiOnly", false)) {
			ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo net = connMgr.getActiveNetworkInfo();
			
			if (net == null || net.getType() != ConnectivityManager.TYPE_WIFI) {
				Log.i("Groundhog", "Background Checker: exiting because there is no WIFI and notif_wifiOnly is enabled");
				return;
			}
		}
		
		
		Log.i("Groundhog", "Starting background check");
    	PowerManager mgr = (PowerManager) 	context.getSystemService(Context.POWER_SERVICE);
		mLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,   "GroundhogLock");
		mLock.acquire();
		
		try{
			Class postPartypes[] = new Class[2];
			postPartypes[0] = String.class;
			postPartypes[1] = Integer.class;
			Method postCallback = this.getClass().getMethod("postCheckMessagesCallBack", postPartypes);
		
			ServerManager myServerManager = new ServerManager(context);
			mServerMessageGetter = new ServerMessageGetter(this, null, null, postCallback, postCallback, 
					                                       context, myServerManager, 100, false, true);
			
			String[] groupsarr = DBUtils.getSubscribedGroups(context);
			Vector<String> groups = new Vector<String>(groupsarr.length);
			for(String group: groupsarr) {
				groups.add(group);
			}
			mServerMessageGetter.execute(groups);
    	} catch(NoSuchMethodException e) {
			e.printStackTrace();
		} catch(SecurityException e) {
			e.printStackTrace();
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			if (mLock != null && mLock.isHeld())
				mLock.release();
		}
		
	}
	

	public void postCheckMessagesCallBack(String status, Integer resultObj) {
		Log.i("Groundhog", "Background checking finished, publishing notification (if needed)");
		
		if (mLock != null && mLock.isHeld())
			mLock.release();
		
		int result = resultObj.intValue();

		if (result != CHECK_FINISHED_OK) {
			Log.w("Groundhog", "Warning: non OK status returned when checking new messages: " + result);
			if (status != null)
				Log.w("Groundhog", "Also, status text: " + status);
		} 
		
		mServerMessageGetter = null;
        
        String [] groupsInfo = status.split(";");
        StringBuffer text = new StringBuffer();
        boolean hasSome = false;
        int total = 0;
        for(String groupMsgs : groupsInfo) {
        	String [] nameMsgs = groupMsgs.split(":");
        	int newmsgs = new Integer(nameMsgs[1]).intValue();
        	total += newmsgs;
        	if (newmsgs > 0) {
        		hasSome = true;
        		text.append(newmsgs);
        		text.append(" - " + nameMsgs[0].trim() + "\n");
        	}
        }
        
        NotificationManager nm = (NotificationManager)  mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notif = new Notification(R.drawable.icon, total + " new", System.currentTimeMillis());
        
        
        if (mPrefs.getBoolean("notif_useSound", true)) 
        	notif.defaults |= Notification.DEFAULT_SOUND;
        if (mPrefs.getBoolean("notif_useVibration", true))
        	notif.defaults |= Notification.DEFAULT_VIBRATE;
        if (mPrefs.getBoolean("notif_useLight", true))
        	notif.defaults |= Notification.DEFAULT_LIGHTS;
        
        notif.flags      |= Notification.FLAG_AUTO_CANCEL;        

        if (hasSome) {
        	RemoteViews contentView = new RemoteViews(mContext.getPackageName(), R.layout.custom_notification_layout);
        	contentView.setImageViewResource(R.id.notif_image, R.drawable.icon);
        	contentView.setTextViewText(R.id.notif_text, text.toString());
        	notif.contentView = contentView;        	
        	
        	Intent notifyIntent     = new Intent(mContext, GroupListActivity.class);
        	notifyIntent.putExtra("fromNotify", true);
        	PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0, notifyIntent, android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
        	notif.contentIntent = contentIntent;
        	
        	nm.cancel(UsenetConstants.CHECK_ALARM_CODE);        	
        	nm.notify(UsenetConstants.CHECK_ALARM_CODE, notif);        	
        }
	}	

}
