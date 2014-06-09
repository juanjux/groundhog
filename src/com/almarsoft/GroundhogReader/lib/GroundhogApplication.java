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

package com.almarsoft.GroundhogReader.lib;

import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;

import com.almarsoft.GroundhogReader.R;

public class GroundhogApplication extends Application {
	
	// Global state vars
	private boolean hostChanged = false;
	private boolean readCharsetChanged = false;
	private String configValidation_errorTitle = null;
	private String configValidation_errorText = null;
	
	public void setHostChanged(boolean hostChanged) {
		this.hostChanged = hostChanged;
	}
	public boolean isHostChanged() {
		return hostChanged;
	}
	
	
	public void setReadCharsetChanged(boolean readCharsetChanged) {
		this.readCharsetChanged = readCharsetChanged;
	}
	public boolean isReadCharsetChanged() {
		return readCharsetChanged;
	}
	
	public String getConfigValidation_errorTitle() {
		return configValidation_errorTitle;
	}
	
	public String getConfigValidation_errorText() {
		return configValidation_errorText;
	}
	
	private boolean isEmpty(String prefname, SharedPreferences prefs) { 
		
		String configOption  = prefs.getString(prefname, "");
		String toption = configOption.trim();
		if (toption == null  || toption.length() == 0) {
			return true;
		}
		
		return false;
	}
	
	
	public boolean checkEmptyConfigValues(Activity currentActivity, SharedPreferences prefs) {
		boolean haserror = false;
		
		if (haserror = isEmpty("host", prefs)) {
			this.configValidation_errorTitle = getString(R.string.empty_host_title);
			this.configValidation_errorText  = getString(R.string.empty_host_error);
		}
		
		else if (haserror = isEmpty("port", prefs)) {
			this.configValidation_errorTitle = getString(R.string.empty_port_title);
			this.configValidation_errorText  = getString(R.string.empty_port_error);  
		}

		else if (prefs.getBoolean("needsAuth", false)) {
			if (haserror = isEmpty("login", prefs)) {
            	this.configValidation_errorTitle = getString(R.string.empty_login_title);
            	this.configValidation_errorText = getString(R.string.empty_login_error);
			}
			
			else if (haserror = isEmpty("pass", prefs)) {
    			this.configValidation_errorTitle = getString(R.string.empty_pass_title);
    			this.configValidation_errorText = getString(R.string.empty_pass_error);
			}
		}
		
		return haserror;
	}
}
