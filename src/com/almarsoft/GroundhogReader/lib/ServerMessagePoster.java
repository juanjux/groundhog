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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Vector;

import com.almarsoft.GroundhogReader.R;
import android.content.Context;
import android.os.AsyncTask;


public class ServerMessagePoster extends AsyncTaskProxy {

	
	private ServerManager mServerManager = null;
	private AsyncTask<Void, Void, Integer> mTask  = null;
	
	public ServerMessagePoster(Object callerInstance, Method preCallback, Method progressCallback, Method postCallback, Method cancelCallback,
			                    Context context, ServerManager serverManager) {
		
		super(callerInstance, preCallback, progressCallback, postCallback, cancelCallback, context);
		
		mServerManager = serverManager;
	}
	
	public void interrupt() {
		if (mTask != null	&& mTask.getStatus() != AsyncTask.Status.FINISHED) {
			mTask.cancel(false);
		}
	}	
	
	public void execute() {
		mTask = new ServerMessagePosterTask();
		mTask.execute();
	}
	
	// ===============================
	// Post pending outgoing messages on the outbox
	// ===============================

	private class ServerMessagePosterTask extends AsyncTask<Void, Void, Integer> {

		String mStatusMsg = null;
		
		private static final int FINISHED_ERROR = 1;
		protected static final int FINISHED_ERROR_AUTH = 2;
		private static final int FINISHED_INTERRUPTED = 4;
		private static final int POST_FINISHED_OK = 5;

		@Override
		protected void onPreExecute() {
			
			if (mCallerInstance != null && mPreCallback != null) {
				try {
					Object[] noparams = new Object[0];
					mPreCallback.invoke(mCallerInstance, noparams);
			   } catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
			}
		}

		
		@Override
		protected Integer doInBackground(Void... voids) {

			try {
				Vector<Long> pendingIds = DBUtils.getPendingOutgoingMessageIds(mContext);

				if (pendingIds == null || pendingIds.size() == 0)
					return POST_FINISHED_OK;

				String basePath = UsenetConstants.EXTERNALSTORAGE + "/" + UsenetConstants.APPNAME + "/offlinecache/outbox/";
				String msgPath;
				String message;

				for (long pId : pendingIds) {

					if (isCancelled()) {
						mStatusMsg = mContext.getString(R.string.download_interrupted_sleep);

						return FINISHED_INTERRUPTED;
					}

					msgPath = basePath + Long.toString(pId);
					try {
						// XXX: Aqui seria mejor usar el getDiskFromDiskFile para no petar la memoria si es muy grande
						message = FSUtils.loadStringFromDiskFile(msgPath, false);
						mServerManager.postArticle(message, true);
					} catch (UsenetReaderException e) {
						// Message not found for some reason, just skip but
						// delete from DB
					}
					FSUtils.deleteOfflineSentPost(pId, mContext);

				}
				FSUtils.deleteDirectory(UsenetConstants.EXTERNALSTORAGE + "/"
						+ UsenetConstants.APPNAME + "/offlinecache/outbox");

				return POST_FINISHED_OK;

			} catch (IOException e) {
				e.printStackTrace();
				mStatusMsg = mContext.getString(R.string.error_post_check_settings)	+ ": " + e.toString();
				return FINISHED_ERROR;

			} catch (ServerAuthException e) {
				e.printStackTrace();
				mStatusMsg = mContext
						.getString(R.string.error_authenticating_check_pass)
						+ ": " + e.toString();
				return FINISHED_ERROR_AUTH;
			}
		}

		
		@Override
		protected void onPostExecute(Integer resultObj) {
			if (mCallerInstance != null && mPostCallback != null) {
				try {
					Object[] postParams = new Object[2];
					postParams[0] = mStatusMsg;
					postParams[1] = resultObj;
					
					mPostCallback.invoke(mCallerInstance, postParams);

			   } catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
			}
			mTask = null;
		}
		
		
		
		@Override
		protected void onCancelled() {
			try {
				mCancelCallback.invoke(mCallerInstance,  new Object[0]);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}

	}	
}
