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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.KeyEvent;

public class OptionsActivity extends PreferenceActivity {
	
	private SharedPreferences mPrefs; 

	// Used to detect changes
	private String oldHost;
	private boolean oldAlarm;
	private long oldAlarmPeriod;
	private String oldReadCharset;
	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);			

		mPrefs = PreferenceManager.getDefaultSharedPreferences(OptionsActivity.this);		
		addPreferencesFromResource(R.layout.options);

	}
	
	// ============================================================================================
	// Save the value of host; we'll check it again onPause to see if the server changed to delete
	// the group messages and restore the article pointer to -1
	// ============================================================================================
	
	@Override
	protected void onResume() {
		oldHost             = mPrefs.getString("host", null);
		oldReadCharset = mPrefs.getString("readDefaultCharset", null);		
		oldAlarm    = mPrefs.getBoolean("enableNotifications", false);
		oldAlarmPeriod = new Long(mPrefs.getString("notifPeriod", "3600000")).longValue();
		
		super.onResume();
	}
	
	/**
	 * Check if the host changed to reset al groups messages
	 */
	protected void checkHostChanged() {
		
		String newHost = mPrefs.getString("host", null);
		if (oldHost != null && newHost != null) {
			if (!oldHost.equalsIgnoreCase(newHost)) {
				// Host changed, store it in hostchanged so other activities can detect it
				Editor editor = mPrefs.edit();
				editor.putBoolean("hostChanged", true);
				editor.commit();
			} 
		}
	}
	
	protected void checkCharsetChanged() {
		
		String newReadCharset = mPrefs.getString("readDefaultCharset", null);
		if (oldReadCharset != null && newReadCharset != null) {
			if (!oldReadCharset.equalsIgnoreCase(newReadCharset)) {
				// Charset changed, store it in charsetChanged so other activities can detect it
				Editor editor = mPrefs.edit();
				editor.putBoolean("readCharsetChanged", true);
				editor.commit();
			}
		}
	}
	
	/**
	 * Check that the alarm changed to enable/reset/disable the current alarm
	 */
	protected void checkAlarmChanged() {		
		boolean newAlarm    = mPrefs.getBoolean("enableNotifications", false);
		long newAlarmPeriod = new Long(mPrefs.getString("notifPeriod", "3600000")).longValue();
		
		Intent alarmIntent   = new Intent(this, GroupsCheckAlarmReceiver.class);
		PendingIntent sender = PendingIntent.getBroadcast(this, 0, alarmIntent, 0);
		AlarmManager am      = (AlarmManager) getSystemService(ALARM_SERVICE);		
		
		if (oldAlarm == false && newAlarm == true) { // User enabled the alarm
			am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime()+newAlarmPeriod, newAlarmPeriod, sender);			
		}
		else if (oldAlarm == true && newAlarm == false) { // User disabled the alarm
			am.cancel(sender);
		}
		else if (newAlarm == true && (oldAlarmPeriod != newAlarmPeriod)) { // User changed the interval
			am.cancel(sender);
			am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime()+newAlarmPeriod, newAlarmPeriod, sender);
		}
	}
	
	
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        checkHostChanged();
        checkCharsetChanged();
        checkAlarmChanged();
        
      	return super.onKeyDown(keyCode, event);
    }
    
}
