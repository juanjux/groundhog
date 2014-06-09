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

import java.lang.reflect.Method;
import android.content.Context;

public class AsyncTaskProxy {

	protected Method mPreCallback         = null;
	protected Method mProgressCallback    = null;
	protected Method mPostCallback        = null;
	protected Method mCancelCallback	   = null;
	protected Object  mCallerInstance     = null;
	protected Context mContext            = null;

	public AsyncTaskProxy(Object callerInstance, Method preCallback, Method progressCallback, Method postCallback, Method cancelCallback, Context context) {
		
		mCallerInstance        = callerInstance;
		mPreCallback     	     = preCallback;
		mProgressCallback   = progressCallback;
		mPostCallback          = postCallback;
		mCancelCallback  		= cancelCallback;
		mContext                  = context;
	}
	
}
