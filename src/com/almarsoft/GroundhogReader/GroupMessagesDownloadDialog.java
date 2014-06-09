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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Vector;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;

import com.almarsoft.GroundhogReader.lib.ServerManager;
import com.almarsoft.GroundhogReader.lib.ServerMessageGetter;
import com.almarsoft.GroundhogReader.lib.ServerMessagePoster;

/**
 * This file is conceptually complex, but can be easily explained. 
 * 
 * The thing is that the download dialog can be called from different activities (GroupList and MessageList) so
 * the caller must provide a callback so the dialog can return to the caller once it has finished the job.
 * 
 * The real network code is not on this class because it would be ugly and also because the background notification service
 * wants to access it too, so to have the classes that really download or post messages (ServerMessageGetter and ServerMessagePost)
 *  update us, we in turn provide callbacks to that classes.
 * 
 * So this is a little like a callback orgy.
 *
 */


public class GroupMessagesDownloadDialog {

	private static final int FINISHED_ERROR = 1;
	protected static final int FINISHED_ERROR_AUTH = 2;
	protected static final int FETCH_FINISHED_OK = 3;
	private static final int FINISHED_INTERRUPTED = 4;
	private static final int POST_FINISHED_OK = 5;

	private ServerManager mServerManager;
	private Context mContext;
	private int mLimit;

	private PowerManager.WakeLock mWakeLock = null;
	// Used to pass information from synchronize() to the other two
	// method-threads
	private Method mCallback = null;
	private Method mCancelCallback = null;
	private Object mCallerInstance = null;
	private boolean mTmpOfflineMode = false;
	private Vector<String> mTmpGroups = null;
	private ServerMessageGetter mServerMessageGetter = null;
	private ServerMessagePoster mServerMessagePoster = null;
	
	private ProgressDialog mProgressGetMessages = null;
	private ProgressDialog mProgressPostMessages = null;
	

	public GroupMessagesDownloadDialog(ServerManager manager, Context context) {
		mServerManager = manager;
		mContext = context;
		PowerManager pm = (PowerManager) context
				.getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK
				| PowerManager.ON_AFTER_RELEASE, "GroundhogDownloading");
	}

	public void interrupt() {
		if (mWakeLock.isHeld())
			mWakeLock.release();

		if (mServerMessageGetter != null)
			mServerMessageGetter.interrupt();

		if (mServerMessagePoster != null)
			mServerMessagePoster.interrupt();
		
		if (mProgressGetMessages != null)
			mProgressGetMessages.dismiss();
	}

	public void synchronize(boolean offlineMode, final Vector<String> groups, Method callback, Method cancelCallback, Object caller) {
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		String maxFetch = prefs.getString("maxFetch", "100");
		if (maxFetch.length() == 0) maxFetch = "100";
		mLimit = new Integer(maxFetch);
		mCallback = callback;
		mCancelCallback = cancelCallback;
		mCallerInstance = caller;
		mTmpOfflineMode = offlineMode;
		mTmpGroups = groups;

		mWakeLock.acquire();
		postPendingOutgoingMessages();
	}
	
	// ============================================================
	// CallBacks for the ServerMessageGetter
	// ============================================================
	
	public void preGetMessagesCallBack() {
		mProgressGetMessages = new ProgressDialog(mContext);
		mProgressGetMessages.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		mProgressGetMessages.setTitle(mContext.getString(R.string.group));
		mProgressGetMessages.setMessage(mContext.getString(R.string.starting_download));
		mProgressGetMessages.setOnCancelListener(new OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				if (mServerMessageGetter != null) {
					mServerMessageGetter.interrupt();
				}
			}
		});
		mProgressGetMessages.show();
	}
	
	public void progressGetMessagesCallBack(String status, String title, Integer progressCurrent, Integer progressMax) {
		if (!mProgressGetMessages.isShowing())
			mProgressGetMessages.show();
		mProgressGetMessages.setMax(progressMax);
		mProgressGetMessages.setProgress(progressCurrent);
		mProgressGetMessages.setMessage(status);
		mProgressGetMessages.setTitle(title);
	}
	
	public void cancelGetMessagesCallBack() {
		if (mWakeLock.isHeld())
			mWakeLock.release();
		
		if (mProgressGetMessages != null)
			mProgressGetMessages.dismiss();

		Object[] noparams = new Object[0];
		try {
			mCancelCallback.invoke(mCallerInstance, noparams);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}
	
	public void postGetMessagesCallBack(String status, Integer resultObj) {
		if (mWakeLock.isHeld())
			mWakeLock.release();
		
		if (mProgressGetMessages != null)
			mProgressGetMessages.dismiss();
		
		String close = mContext.getString(R.string.close);
		int result = resultObj.intValue();

		switch (result) {

		case FETCH_FINISHED_OK:
			mTmpGroups = null;
			mTmpOfflineMode = false;

			if (mCallback != null && mCallerInstance != null) {
				try {
					Object[] noparams = new Object[0];
					mCallback.invoke(mCallerInstance, noparams);
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
			}
			break;

		case FINISHED_ERROR:
		case FINISHED_INTERRUPTED:
		case FINISHED_ERROR_AUTH:
			new AlertDialog.Builder(mContext).setTitle(
					mContext.getString(R.string.error)).setMessage(status).setNeutralButton(close, null).show();
			break;
		}
		mServerMessageGetter = null;
	}
	
	
	/**
	 * This calls the ServerMessageGetter, which will create and AsyncTask and download the messages. We'll provide the 
	 * callbacks so the ServerMessageGetter call them on pre/progress and update so we can update the progress dialog.
	 */
	
	@SuppressWarnings("unchecked")
	public void getMessagesFromServer() {
		try {
			Class prePartypes[] = new Class[0];		
			Method preCallback = this.getClass().getMethod("preGetMessagesCallBack", prePartypes);
			
			Class progressPartypes[] = new Class[4];
			progressPartypes[0] = String.class;
			progressPartypes[1] = String.class;
			progressPartypes[2] = Integer.class;
			progressPartypes[3] = Integer.class;
			Method progressCallback = this.getClass().getMethod("progressGetMessagesCallBack", progressPartypes);
			
			Class postPartypes[] = new Class[2];
			postPartypes[0] = String.class;
			postPartypes[1] = Integer.class;
			Method postCallback = this.getClass().getMethod("postGetMessagesCallBack", postPartypes);
			
			Method cancelCallback = this.getClass().getMethod("cancelGetMessagesCallBack", null);
			
			mServerMessageGetter = new ServerMessageGetter(this, preCallback, progressCallback, postCallback, cancelCallback,
					                                       mContext, mServerManager, mLimit, mTmpOfflineMode, false);
			mServerMessageGetter.execute(mTmpGroups);
		} catch(NoSuchMethodException e) {
			e.printStackTrace();
		} catch(SecurityException e) {
			e.printStackTrace();
		}
	} 

	
	// ============================================================
	// CallBacks for the ServerMessagePoster
	// ============================================================
	
	public void prePostMessagesCallBack() {
		mProgressPostMessages = new ProgressDialog(mContext);
		mProgressPostMessages.setTitle(mContext.getString(R.string.posting));
		mProgressPostMessages.setMessage(mContext.getString(R.string.posting_pending_messages));
		mProgressPostMessages.setOnCancelListener(new OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				if (mServerMessagePoster != null) {
					mServerMessagePoster.interrupt();
				}
			}
			
		});
		mProgressPostMessages.show();
	}
	
	public void postCancelMessagesCallBack() {
		if (mWakeLock.isHeld())
			mWakeLock.release();
		
		if (mProgressGetMessages != null)
			mProgressGetMessages.dismiss();
	
		Object[] noparams = new Object[0];
		try {
			mCancelCallback.invoke(mCallerInstance, noparams);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}
	
	public void postPostMessagesCallBack(String status, Integer resultObj) {
		
		if (mWakeLock.isHeld())
			mWakeLock.release();
		
		if (mProgressPostMessages != null)
			mProgressPostMessages.dismiss();
		
		String close = mContext.getString(R.string.close);
		int result = resultObj.intValue();

		switch (result) {

		case POST_FINISHED_OK:
			mWakeLock.acquire();
			getMessagesFromServer();
			break;

		case FINISHED_ERROR:
		case FINISHED_INTERRUPTED:
		case FINISHED_ERROR_AUTH:
			new AlertDialog.Builder(mContext).setTitle(
					mContext.getString(R.string.error)).setMessage(status)	.setNeutralButton(close, null).show();
			break;

		}

		mServerMessagePoster = null;		
	}
	
	
	// =========================================
	// The name says it all :)
	// =========================================
	
	@SuppressWarnings("unchecked")
	private void postPendingOutgoingMessages() {
		try {
			Method preCallback = this.getClass().getMethod("prePostMessagesCallBack", new Class[0]);
			
			Class postPartypes[] = new Class[2];
			postPartypes[0] = String.class;
			postPartypes[1] = Integer.class;
			Method postCallback = this.getClass().getMethod("postPostMessagesCallBack", postPartypes);
			
			Method cancelCallback = this.getClass().getMethod("postCancelMessagesCallBack", new Class[0]);
			
			mServerMessagePoster = new ServerMessagePoster(this, preCallback, null, postCallback, cancelCallback, mContext, mServerManager);
			mServerMessagePoster.execute();
			
		} catch(NoSuchMethodException e) {
			e.printStackTrace();
		} catch(SecurityException e) {
			e.printStackTrace();
		}
	}

}
