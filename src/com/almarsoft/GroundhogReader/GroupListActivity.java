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

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Vector;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

import com.almarsoft.GroundhogReader.lib.DBUtils;
import com.almarsoft.GroundhogReader.lib.GroundhogApplication;
import com.almarsoft.GroundhogReader.lib.ServerAuthException;
import com.almarsoft.GroundhogReader.lib.ServerManager;
import com.almarsoft.GroundhogReader.lib.UsenetConstants;

public class GroupListActivity extends Activity {
    /** Activity showing the list of subscribed groups. */
	
	private static final int MENU_ITEM_MARKALLREAD = 1;
	private static final int MENU_ITEM_UNSUBSCRIBE = 2;
	private static final int MENU_ITEM_CATCHUP = 3;

	private static final int ID_DIALOG_DELETING = 0;
	private static final int ID_DIALOG_UNSUBSCRIBING = 1;
	private static final int ID_DIALOG_MARKREAD = 2;
	private static final int ID_DIALOG_CATCHUP=3;
	
	// Real name of the groups, used for calling the MessageListActivity with the correct name
	private String[] mGroupsArray;
	private String mTmpSelectedGroup;
	
	// Name of the group + unread count, used for the listView arrayAdapter
	private String[] mGroupsWithUnreadCountArray;
	
	// This is a member so we can interrupt its operation, but be carefull to create it just
	// before the operation and assign to null once it has been used (at the start of the callback, not in the next line!!!)
	private GroupMessagesDownloadDialog mDownloader = null;
	private ServerManager mServerManager;
	
	private ListView mGroupsList;
	private boolean mOfflineMode;
	private SharedPreferences mPrefs;
	private Context mContext;
	private AlertDialog mConfigAlert; 
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);

    	Log.d(UsenetConstants.APPNAME, "GroupList onCreate");
    	setContentView(R.layout.grouplist);

    	// Config checker alert dialog
    	mConfigAlert = new AlertDialog.Builder(this).create();
		mConfigAlert.setButton(getString(R.string.ok),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dlg, int sumthin) {
						startActivity(new Intent(GroupListActivity.this, OptionsActivity.class));
					}
				}
		);    	
    	
		// Group list
    	mGroupsList = (ListView) this.findViewById(R.id.list_groups);
    	mGroupsList.setOnItemClickListener(mListItemClickListener);
		registerForContextMenu(mGroupsList);
		
		// ServerManager
		mContext = getApplicationContext();
		mServerManager = new ServerManager(mContext);
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		// Detect first-time usage and send to settings
		boolean firstTime = mPrefs.getBoolean("firstTime", true);
		
		if (firstTime) {
			Editor ed = mPrefs.edit();
			ed.putBoolean("firstTime", false);
			ed.commit();
			startActivity(new Intent(GroupListActivity.this, OptionsActivity.class));
		}
		
		mOfflineMode = mPrefs.getBoolean("offlineMode", true);
		
		long expireTime = new Long(mPrefs.getString("expireMode",  "86400000")).longValue(); // 1 day default
		long lastExpiration = mPrefs.getLong("lastExpiration", 0);
		
		 // 0 = manual expiration only. And we check so we don't do more than one expiration a day
		if (expireTime != 0 && ((System.currentTimeMillis() - lastExpiration) > 86400000)) {
			expireReadMessages(false);
		}
		
		// "Add Group" button
		Button addButton        = (Button) this.findViewById(R.id.btn_add);
		addButton.setText(getString(R.string.grouplist_add_groups));
		addButton.setOnClickListener(	
					new Button.OnClickListener() {
						public void onClick(View v) {
							startActivity(new Intent(GroupListActivity.this, SubscribeActivity.class));
						}
					}
		);
		
		// Settings button
		Button settingsButton = (Button) this.findViewById(R.id.btn_settings);
		settingsButton.setText(getString(R.string.global_settings));
		settingsButton.setOnClickListener(
					new Button.OnClickListener() {
						public void onClick(View v) {
							startActivity(new Intent(GroupListActivity.this, OptionsActivity.class));
						}
					}
		);
		
    }
    
    
    @Override
    protected void onResume() {
    	super.onResume();
    	
    	Log.d(UsenetConstants.APPNAME, "GroupList onResume");
    	
    	// =============================================
    	// Detect empty-values errors in the settings
    	// =============================================
    	GroundhogApplication grapp = (GroundhogApplication)getApplication();
    	
    	if (grapp.checkEmptyConfigValues(this, mPrefs)) {
    		mConfigAlert.setTitle(grapp.getConfigValidation_errorTitle());
			mConfigAlert.setMessage(grapp.getConfigValidation_errorText());
			if (mConfigAlert.isShowing()) 
				mConfigAlert.hide();
			mConfigAlert.show();
    	}
    	else {
    		if (mConfigAlert.isShowing())
    			mConfigAlert.hide();
    	}
    	
		// =====================================================
        // Detect server hostname changes in the settings
    	// =====================================================
    	
		if (mPrefs.getBoolean("hostChanged", false)) {
			// The host  has changed in the prefs, show the dialog and clean the group headers
			new AlertDialog.Builder(GroupListActivity.this)
			.setTitle(getString(R.string.group_headers))
			.setMessage(getString(R.string.server_change_detected))
		    .setNeutralButton("Close", null)
		    .show();	
			
			DBUtils.restartAllGroupsMessages(mContext);
			
			// Finally remote the "dirty" mark and repaint the screen
			Editor editor = mPrefs.edit();
			editor.putBoolean("hostChanged", false);
			editor.commit();
			
		}
			
		Log.d(UsenetConstants.APPNAME, "onResume, recreating ServerManager");
		if (mServerManager == null)
			mServerManager = new ServerManager(mContext);
		
        //=======================================================================
        // Load the group names and unreadcount from the subscribed_groups table
        //=======================================================================
    	updateGroupList();
    	
    	// Check if we came here from a notification, offer to sync messages in that case
    	Bundle extras = getIntent().getExtras();
		if (extras != null && extras.containsKey("fromNotify")) {
			getIntent().removeExtra("fromNotify");
			getAllMessages(false);
		}
    }
    
    
	@Override
	protected void onPause() {
		super.onPause();
	
		Log.d(UsenetConstants.APPNAME, "GroupListActivity onPause");
		
		if (mDownloader != null) 
			mDownloader.interrupt();
		
		
    	if (mServerManager != null) 
    		mServerManager.stop();
    	mServerManager = null;
	}    
	
	
	@Override
	protected Dialog onCreateDialog(int id) {
		ProgressDialog loadingDialog = null;
		
		if(id == ID_DIALOG_DELETING){
			loadingDialog = new ProgressDialog(this);
			loadingDialog.setMessage(getString(R.string.expiring_d));
			loadingDialog.setIndeterminate(true);
			loadingDialog.setCancelable(true);
			return loadingDialog;
			
		} else if(id == ID_DIALOG_UNSUBSCRIBING){
			loadingDialog = new ProgressDialog(this);
			loadingDialog.setMessage(getString(R.string.unsubscribing_deleting_caches));
			loadingDialog.setIndeterminate(true);
			loadingDialog.setCancelable(true);
			return loadingDialog;
			
		} else if(id == ID_DIALOG_MARKREAD){
			loadingDialog = new ProgressDialog(this);
			loadingDialog.setMessage(getString(R.string.marking_read_deleting_caches));
			loadingDialog.setIndeterminate(true);
			loadingDialog.setCancelable(true);
			return loadingDialog;
		} else if(id == ID_DIALOG_CATCHUP){
			loadingDialog = new ProgressDialog(this);
			loadingDialog.setMessage(getString(R.string.catchingup_server));
			loadingDialog.setIndeterminate(true);
			loadingDialog.setCancelable(false);
			return loadingDialog;
			
		} 

		return super.onCreateDialog(id);
	}
	
    
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// ignore orientation change because it would cause the message list to
		// be reloaded
		
		super.onConfigurationChanged(newConfig);
	}

    
    public void updateGroupList() {

    	// We're probably called from mDownloader, so clear it
    	if (mDownloader != null) 
    		mDownloader = null;
    	
    	String[] proxyGroupsArray = DBUtils.getSubscribedGroups(mContext);
    	int count = 0;
    	if (proxyGroupsArray != null) 
    		count = proxyGroupsArray.length;
    	else
    		proxyGroupsArray = new String[0];
    	
    	String[] proxyGroupsUnreadCount = new String[count];
    	String curGroup = null;
		int unread;
		StringBuilder builder = new StringBuilder(80);
		
		for (int i = 0; i < count; i++) {
			curGroup = proxyGroupsArray[i];
			unread = DBUtils.getGroupUnreadCount(curGroup, mContext);
			
			if (unread <= 0) 
				proxyGroupsUnreadCount[i] = curGroup;
			else {              
				proxyGroupsUnreadCount[i] = builder
							                .append('(')
							                .append(unread)
							                .append(") ")				
			                                .append(curGroup)
			                                .toString();
				builder.delete(0, builder.length());
			}
		}
		
		mGroupsWithUnreadCountArray = proxyGroupsUnreadCount;
		mGroupsArray = proxyGroupsArray;
		
		// Finally fill the list
        mGroupsList.setAdapter(new ArrayAdapter<String>(this, R.layout.grouplist_item, mGroupsWithUnreadCountArray));
        mGroupsList.invalidateViews();
    }
    
	// ================================================
	// Menu setting
	// ================================================
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		new MenuInflater(getApplication()).inflate(R.menu.grouplistmenu, menu);
		return(super.onCreateOptionsMenu(menu));
		
	}
	
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {		
		
		MenuItem getAll = menu.findItem(R.id.grouplist_menu_getall);
		MenuItem offline = menu.findItem(R.id.grouplist_menu_offline);
		
		if (mOfflineMode) {
			getAll.setTitle(getString(R.string.sync_messages));
			getAll.setIcon(android.R.drawable.ic_menu_upload);
			offline.setTitle(getString(R.string.set_online_mode));
			offline.setIcon(android.R.drawable.presence_online);
			
		} else {
			getAll.setTitle(getString(R.string.get_all_headers));
			getAll.setIcon(android.R.drawable.ic_menu_set_as);
			offline.setTitle(getString(R.string.set_offline_mode));
			offline.setIcon(android.R.drawable.presence_offline);
		}
		return (super.onPrepareOptionsMenu(menu));
	}
	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.grouplist_menu_addgroups:
				startActivity(new Intent(GroupListActivity.this, SubscribeActivity.class));
				return true;
				
			case R.id.grouplist_menu_settings:
				startActivity(new Intent(GroupListActivity.this, OptionsActivity.class));
				return true;
				
			case R.id.grouplist_menu_getall:
				getAllMessages(false);
				return true;
				
			case R.id.grouplist_get_latest:
				getAllMessages(true);
				
			case R.id.grouplist_menu_offline:
				mOfflineMode = !mOfflineMode;
				Editor editor = mPrefs.edit();
				editor.putBoolean("offlineMode", mOfflineMode);
				editor.commit();
				
				if (mOfflineMode) 
					setTitle(getString(R.string.group_offline_mode));
				else 
					setTitle(getString(R.string.group_online_mode));
				return true;
				
			case R.id.grouplist_menu_expire_messages:
				showExpireMessagesDialog();
				return true;
				
			case R.id.grouplist_menu_quickhelp:
				startActivity(new Intent(GroupListActivity.this, HelpActivity.class));
				return true;
				
			case R.id.grouplist_catchup_server_all:
				catchupGroups(mGroupsArray);
				return true;
		}
		return false;
	}
	
	
	private void showExpireMessagesDialog() {
		new AlertDialog.Builder(GroupListActivity.this)
		.setTitle(getString(R.string.clear_cache))
		.setMessage(getString(R.string.confirm_expire_messages))
	    .setPositiveButton(getString(R.string.yes), 
	    	new DialogInterface.OnClickListener() {
	    		public void onClick(DialogInterface dlg, int sumthin) { 
	    			expireReadMessages(true);
	    		} 
	        } 
	     )		     
	     .setNegativeButton(getString(R.string.no), null)		     		    		 
	     .show();		
	}
	
	
	private void expireReadMessages(boolean expireAll) {
		
		AsyncTask<Boolean, Void, Void> readExpirerTask = new AsyncTask<Boolean, Void, Void>() {
			@Override
			protected Void doInBackground(Boolean... args) {
				boolean expireAll = args[0];

				long expireTime = new Long(mPrefs.getString("expireMode", "86400000")).longValue();
				DBUtils.expireReadMessages(mContext, expireAll, expireTime);
				return null;
			}
			
			protected void onPostExecute(Void arg0) {
				// Store the expiration date so we don't do more than once a day
				Editor ed = mPrefs.edit();
				ed.putLong("lastExpiration", System.currentTimeMillis());
				ed.commit();
				
				updateGroupList();
				try {
					dismissDialog(ID_DIALOG_DELETING);
				} catch (IllegalArgumentException e) {}
			}
		};
		
		showDialog(ID_DIALOG_DELETING);
		readExpirerTask.execute(expireAll);
	}
	
	private void catchupGroups(String[] groups) {
		AsyncTask<String, Void, Void> catchupTask = new AsyncTask<String, Void, Void>() {
			
			@Override
			protected Void doInBackground(String... groupArr) {
				
				for(String group : groupArr) {
					
					try {
						mServerManager.catchupGroup(group);
					} catch (IOException e) {
						Log.w("Groundhog", "Problem catching up with the server");
						e.printStackTrace();
					} catch (ServerAuthException e) {
						Log.w("Groundhog", "Problem catching up with the server");
						e.printStackTrace();
					}
				}
				return null;
			}
			
			protected void onPostExecute(Void arg0) {
				Toast.makeText(GroupListActivity.this, R.string.catchup_done, Toast.LENGTH_SHORT);
				dismissDialog(ID_DIALOG_CATCHUP);
			}
		};
		
		showDialog(ID_DIALOG_CATCHUP);
		catchupTask.execute(groups);
	}	

	
	@SuppressWarnings("unchecked")
	private void getAllMessages(boolean getlatest) {
		
		if (mGroupsArray.length == 0) return;
		
		Vector<String> groupVector = new Vector<String>(mGroupsArray.length);
		
		for (String group : mGroupsArray) {
			groupVector.add(group);
		}
		
		try {
			Class[] noargs = new Class[0];
			Method successCallback = this.getClass().getMethod("updateGroupList", noargs);
			
			mServerManager.setFetchLatest(getlatest);
			mDownloader = new GroupMessagesDownloadDialog(mServerManager, this);
			// Even if it is interrupted we update the counters, so we pass the same callback twice
			mDownloader.synchronize(mOfflineMode, groupVector, successCallback, successCallback, this);
			
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}		
	}
    
	// ==============================
	// Contextual menu on group
	// ==============================
	
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    	new MenuInflater(getApplicationContext()).inflate(R.menu.grouplist_item_menu, menu);
    	menu.setHeaderTitle(getString(R.string.group_menu));
    	super.onCreateContextMenu(menu, v, menuInfo);
    }
    
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
    	
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        //HeaderItemClass header = mHeaderItemsList.get(info.position);
        final String groupname = mGroupsArray[info.position];
        int order = item.getOrder();
        
    	// "Mark all as read" => Show confirm dialog and call markAllRead
    	if (order == MENU_ITEM_MARKALLREAD) {
    		String msg = getString(R.string.mark_read_question);
    		msg = java.text.MessageFormat.format(msg, groupname);   
    		
			new AlertDialog.Builder(GroupListActivity.this)
			.setTitle(getString(R.string.mark_all_read))
			.setMessage(msg)
		    .setPositiveButton(getString(R.string.yes), 
		    	new DialogInterface.OnClickListener() {
		    		public void onClick(DialogInterface dlg, int sumthin) { 
		    			markAllRead(groupname);
		    		} 
		        } 
		     )		     
		     .setNegativeButton(getString(R.string.no), null)		     		    		 
		     .show();	
    		return true;
    	}
    	
    	// "Unsubscribe group" => Show confirm dialog and call unsubscribe
    	else if (order == MENU_ITEM_UNSUBSCRIBE) {
    		String msg = getString(R.string.unsubscribe_question);
    		msg = java.text.MessageFormat.format(msg, groupname);     		
    		
			new AlertDialog.Builder(GroupListActivity.this)
			.setTitle(getString(R.string.unsubscribe))
			.setMessage(msg)
		    .setPositiveButton(getString(R.string.yes), 
		    	new DialogInterface.OnClickListener() {
		    		public void onClick(DialogInterface dlg, int sumthin) { 
		    			unsubscribe(groupname);
		    		} 
		        } 
		     )		     
		     .setNegativeButton(getString(R.string.no), null)		     		    		 
		     .show();	
    		return true;
    	}
    	
    	else if (order == MENU_ITEM_CATCHUP) {
    		String[] groupArr = new String[1];
    		groupArr[0] = groupname;
    		catchupGroups(groupArr);
    	}
    	
    	
        return false;
    }
    
    
    private void markAllRead(final String group) {
    	
		AsyncTask<Void, Void, Void> markAllReadTask = new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... arg0) {
	    		DBUtils.groupMarkAllRead(group, GroupListActivity.this.getApplicationContext());
				return null;
			}
			
			protected void onPostExecute(Void arg0) {
				updateGroupList();
				dismissDialog(ID_DIALOG_MARKREAD);
			}

		};
		
		showDialog(ID_DIALOG_MARKREAD);
		markAllReadTask.execute();
    }
    
    
    private void unsubscribe(final String group) {

		AsyncTask<Void, Void, Void> unsubscribeTask = new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... arg0) {
    			DBUtils.unsubscribeGroup(group, GroupListActivity.this.getApplicationContext());
				return null;
			}
			
			protected void onPostExecute(Void arg0) {
				updateGroupList();
				dismissDialog(ID_DIALOG_UNSUBSCRIBING);
			}

		};
		
		showDialog(ID_DIALOG_UNSUBSCRIBING);
		unsubscribeTask.execute();    	
    }
    
    // ==================================================================================================
    // OnItem Clicked Listener (start the MessageListActivity and pass the clicked group name
    // ==================================================================================================

	OnItemClickListener mListItemClickListener = new OnItemClickListener() {

		public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
	
			final String groupName = mGroupsArray[position];
			
			mTmpSelectedGroup = groupName;
			
			// If in offlinemode, offer to synchronize uncatched messages first, if there is any
			Context context = getApplicationContext();
			
			if (mOfflineMode) {
				// -------------------------------------------------------------------------------------------------
				// If we've headers downloaded in online mode offer to download the bodies before entering the group
				// -------------------------------------------------------------------------------------------------
				if (DBUtils.groupHasUncatchedMessages(mTmpSelectedGroup, context)) {
					new AlertDialog.Builder(GroupListActivity.this)
					.setTitle(getString(R.string.get_new))
					.setMessage(getString(R.string.warning_online_to_offline_sync))
					
				    .setPositiveButton(getString(R.string.yes_sync), 
				    	new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dlg, int sumthin) {
									callDownloaderForMessageList(mTmpSelectedGroup);
				    		} 
				        } 
				     )		     
				     .setNegativeButton(getString(R.string.no_enter_anyway),
				        new DialogInterface.OnClickListener() {
				    	 	public void onClick(DialogInterface dlg, int sumthin) {
				    	 		fetchFinishedStartMessageList();
				    	 	}
				     	}
				     )		     		    		 
				     .show();
				}
				// -----------------------------------------------------------------------------
				// If there are 0 unread messages on offline mode offer to synchronice the group
				// -----------------------------------------------------------------------------
				else if (DBUtils.getGroupUnreadCount(mTmpSelectedGroup, context) == 0) {
					new AlertDialog.Builder(GroupListActivity.this)
					.setTitle(getString(R.string.get_new))
					.setMessage(getString(R.string.offline_group_has_no_messages_sync))
					
				    .setPositiveButton(getString(R.string.yes_sync), 
				    	new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dlg, int sumthin) {
									callDownloaderForMessageList(mTmpSelectedGroup);
				    		} 
				        } 
				     )		     
				     .setNegativeButton(getString(R.string.no_enter_anyway),
				        new DialogInterface.OnClickListener() {
				    	 	public void onClick(DialogInterface dlg, int sumthin) {
				    	 		fetchFinishedStartMessageList();
				    	 	}
				     	}
				     )		     		    		 
				     .show();
					
				} else {
					fetchFinishedStartMessageList();
				}
				
			} else {
				// ==========================================
				// Online mode, ask about getting new headers
				// ==========================================
	    		String msg = getString(R.string.fetch_headers_question);
	    		msg = java.text.MessageFormat.format(msg, mTmpSelectedGroup);
	    		
				new AlertDialog.Builder(GroupListActivity.this)
				.setTitle(getString(R.string.get_new))
				.setMessage(msg)
				
			    .setPositiveButton(getString(R.string.yes), 
			    	new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dlg, int sumthin) {
								callDownloaderForMessageList(mTmpSelectedGroup);
			    		} 
			        } 
			     )		     
			     .setNegativeButton("No",
			        new DialogInterface.OnClickListener() {
			    	 	public void onClick(DialogInterface dlg, int sumthin) {
			    	 		fetchFinishedStartMessageList();
			    	 	}
			     	}
			     )		     		    		 
			     .show();	
			}
	    }
	};
	

	@SuppressWarnings("unchecked")
	private void callDownloaderForMessageList(String group) {
		
		try {
			Vector<String> groupVector = new Vector<String>(1);
			groupVector.add(group);
			
			Class[] noargs = new Class[0];
			// This will be called after the synchronize from mDownloader:
			Method successCallback = GroupListActivity.this.getClass().getMethod("fetchFinishedStartMessageList", noargs);
			Method cancelledCallback = this.getClass().getMethod("updateGroupList", noargs);
			mServerManager.setFetchLatest(false);
			mDownloader    = new GroupMessagesDownloadDialog(mServerManager, GroupListActivity.this);
			mDownloader.synchronize(mOfflineMode, groupVector, successCallback, cancelledCallback, GroupListActivity.this);
			
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}			
	}
	
    public void fetchFinishedStartMessageList() {
  		mDownloader = null;
    	Intent msgList = new Intent(GroupListActivity.this, MessageListActivity.class);
    	msgList.putExtra("selectedGroup", mTmpSelectedGroup);
    	startActivity(msgList);
    }
}

