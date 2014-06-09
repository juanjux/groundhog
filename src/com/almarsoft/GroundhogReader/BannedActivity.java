
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

import java.util.ArrayList;
import java.util.HashSet;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.almarsoft.GroundhogReader.lib.DBUtils;
import com.almarsoft.GroundhogReader.lib.UsenetConstants;



public class BannedActivity extends Activity {

	private ArrayList<BannedItem> mBannedItemsList;
	private int mBanType;
	private String mGroup;
	
	private Button mDoneButton;
	private ListView mBannedListView;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.banneds);
        mBannedListView = (ListView) findViewById(R.id.list_banned);
        mDoneButton     = (Button)   findViewById(R.id.btn_banned_done);
        TextView topBar = (TextView) findViewById(R.id.topbar);
        
        mBanType = getIntent().getExtras().getInt("typeban");
        
        HashSet<String> bannedStringsSet = null;
        
        if (mBanType == UsenetConstants.BANNEDTHREADS) {
        	topBar.setText(getString(R.string.banned_threads));
        	mGroup   = getIntent().getExtras().getString("group");
        	bannedStringsSet = DBUtils.getBannedThreads(mGroup, getApplicationContext());
        }
        
        else if (mBanType == UsenetConstants.BANNEDTROLLS) {
        	topBar.setText(getString(R.string.banned_users));
        	bannedStringsSet = DBUtils.getBannedTrolls(getApplicationContext());
        }
        
        	
        if (bannedStringsSet == null || bannedStringsSet.size() == 0) {
			new AlertDialog.Builder(this).setTitle(getString(R.string.nothing_to_do)).setMessage(getString(R.string.there_are_no_bans))
			.setNeutralButton(getString(R.string.return_), 
			    new DialogInterface.OnClickListener() {
	    			public void onClick(DialogInterface dlg, int sumthin) {
	    				BannedActivity.this.setResult(RESULT_CANCELED);
	    				BannedActivity.this.finish();
	    			}
	    		}
			).show();
			
        } else  {

        	mBannedItemsList = new ArrayList<BannedItem>(bannedStringsSet.size());
        	ArrayList<BannedItem> proxyBannedItemsList = mBannedItemsList;
        	
        	for (String item : bannedStringsSet) {
        		if (item != null) 
        			proxyBannedItemsList.add( new BannedItem(item, true) );
        	}
        	
        	mBannedListView.setAdapter(new BannedAdapter(this, R.layout.banned_item, mBannedItemsList));
        	mBannedListView.setItemsCanFocus(false);
        	mBannedListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        	mBannedListView.setTextFilterEnabled(true);

        	mBannedListView.setOnItemClickListener(mBannedItemListener);
        	mDoneButton.setOnClickListener(mDoneListener);
        }
    }
    
    
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// ignore orientation change because it would cause the message list to
		// be reloaded
		super.onConfigurationChanged(newConfig);
	}
    

	// ==========================
	// Listener for button "Done"
	// ==========================
	
    OnClickListener mDoneListener = new OnClickListener() {

		public void onClick(View v) { done(); }
	};
	
	
	
	// ========================================
	// Listener for item list (togle ban/unban)
	// ========================================
	OnItemClickListener mBannedItemListener = new OnItemClickListener() {
		
		public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
			BannedItem it = mBannedItemsList.get(position);
			it.imageActive = !it.imageActive;
		}
	};

	
	// =======================================
	// Options menu shown with the "Menu" key
	// =======================================
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		new MenuInflater(getApplication()).inflate(R.menu.banneditemsmenu, menu);
		return(super.onCreateOptionsMenu(menu));

	}
	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
				
			case R.id.banned_menu_select_all:
				touchAll(true);
				return true;
			
			case R.id.banned_menu_unselect_all:
				touchAll(false);
				return true;

		}
		return false;
	}

	
	private void done() {
		
		// Proxy stuff to optimize the loop (see developer guide)
		ArrayList<BannedItem> proxyBannedItemsList = mBannedItemsList;
		int proxyBanType = mBanType;
		int BANTHREADS = UsenetConstants.BANNEDTHREADS;
		int BANTROLLS  = UsenetConstants.BANNEDTROLLS;
		String group = mGroup;
		Context appContext = getApplicationContext();
		// End proxy stuff
		
		for (BannedItem b : proxyBannedItemsList) {
			
			if (!b.imageActive) {
				if (proxyBanType == BANTHREADS)
					DBUtils.unBanThread(group, b.text, appContext);
				
				else if (proxyBanType == BANTROLLS)
					DBUtils.unBanUser(b.text, appContext);
			}
		}
		
		setResult(RESULT_OK);
		finish();
	} 
	
	
	private void touchAll(boolean check) {

		// Proxy stuff
		ArrayList<BannedItem> proxyBannedItemsList = mBannedItemsList;
		
		for (BannedItem b : proxyBannedItemsList) {
			b.imageActive = check;
		}
		mBannedListView.invalidateViews();	
	}
	
	
	private class BannedItem {
		String text;
		boolean imageActive;
		
		BannedItem(String t, boolean active) {
			text = t;
			imageActive = active;
		}
	}
	

	// ===================================================================
	// Extension of ArrayAdapter which holds and maps the article fields
	// ===================================================================
	private class BannedAdapter extends ArrayAdapter<BannedItem> {

		private ArrayList<BannedItem> items;

		public BannedAdapter(Context context, int textViewResourceId, ArrayList<BannedItem> items) {
			super(context, textViewResourceId, items);
			this.items = items;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {

			View v = convertView;
			if (v == null) {
				LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(R.layout.banned_item, null);
			}

			BannedItem it = items.get(position);

			if (it != null) {
				TextView text = (TextView) v.findViewById(R.id.text_banned);
				text.setText(it.text);

				final ImageView banimg = (ImageView) v .findViewById(R.id.img_banned);
				
				banimg.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						imgBannedClicked(position);
					}
				});

				if (it.imageActive)
					banimg.setImageDrawable(getResources().getDrawable(
							R.drawable.presence_busy));
				else
					banimg.setImageDrawable(getResources().getDrawable(
							R.drawable.presence_busy_off));
			}
			return v;
		}
	}
	

	private void imgBannedClicked(int position) {

		BannedItem banned = mBannedItemsList.get(position);
		banned.imageActive = !banned.imageActive;

		mBannedListView.invalidateViews();
	}	
}


