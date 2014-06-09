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


import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class ReadCharsetActivity extends PreferenceActivity {

	private SharedPreferences mPrefs; 
	
	// Used to detect changes
	private String oldReadCharset;
	private String newReadCharset;	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mPrefs = PreferenceManager.getDefaultSharedPreferences(ReadCharsetActivity.this);
		addPreferencesFromResource(R.layout.optionsreadcharset);

	}
	
	@Override
	protected void onResume() {		
		oldReadCharset = mPrefs.getString("readDefaultCharset", null);
		super.onResume();
	}	
	
	@Override
	protected void onPause() {
		
		newReadCharset = mPrefs.getString("readDefaultCharset", null);
		if (oldReadCharset != null && newReadCharset != null) {
			if (!oldReadCharset.equalsIgnoreCase(newReadCharset)) {
				// Charset changed, store it in charsetChanged so other activities can detect it
				Editor editor = mPrefs.edit();
				editor.putBoolean("readCharsetChanged", true);
				editor.commit();
			}
		}
		super.onPause();
	}

}
