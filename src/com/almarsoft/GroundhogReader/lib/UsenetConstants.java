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

import android.os.Environment;

public final class UsenetConstants {
	
	public final static int BANNEDTHREADS = 0;
	public final static int BANNEDTROLLS  = 1;
	
	public final static int COMPOSEMESSAGEINTENT = 2;
	public final static int BANNEDACTIVITYINTENT = 3;
	public static final int QUOTINGINTENT = 4;
	
	public final static int TEXTSIZE_SMALLEST = 0;
	public final static int TEXTSIZE_SMALLER = 1;
	public final static int TEXTSIZE_NORMAL = 2;
	public final static int TEXTSIZE_LARGER = 3;
	public final static int TEXTSIZE_LARGEST = 4;
	
	public final static int CHECK_ALARM_CODE = 534366991;	
	
	public final static String APPNAME = "Groundhog";
	public final static String ATTACHMENTSDIR = "attachments";
	public static final int SENT_POSTS_LOG_LIMIT_PER_GROUP = 100;
	public static final int SENT_POST_KILL_ADITIONAL = 10;
	public static final String EXTERNALSTORAGE = Environment.getExternalStorageDirectory().getAbsolutePath();
	public static final String QUICKHELPURL = "http://almarsoft.com/groundhog_quick.htm";

}
