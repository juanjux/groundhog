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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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


public class QuotingActivity extends Activity {

	private ArrayList<QuoteLineItem> mQuoteLineItemsList;
	private Button mDoneButton;
	private ListView mLinesListView;
	private String mMultipleFollowup;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.quote);
        mLinesListView = (ListView) findViewById(R.id.list_quotelines);
        mDoneButton    = (Button)   findViewById(R.id.btn_quoting_done);
        
        Button allButton = (Button) findViewById(R.id.btn_all);
        Button noneButton = (Button) findViewById(R.id.btn_none);
        
    	allButton.setOnClickListener(new OnClickListener() {
    		public void onClick(View v) {
    			QuotingActivity.this.touchAll(true);
    		}
    	});

    	noneButton.setOnClickListener(new OnClickListener() {
    		public void onClick(View v) {
    			QuotingActivity.this.touchAll(false);
    		}
    	});

        
        String origText = getIntent().getExtras().getString("origText");
        mMultipleFollowup = getIntent().getExtras().getString("multipleFollowup");
        
        
        String[] lines = origText.split("\n");
        mQuoteLineItemsList = new ArrayList<QuoteLineItem>(lines.length); 
        
        for (String line : lines) {
        	mQuoteLineItemsList.add(new QuoteLineItem(line, false));
        }
        
    	mLinesListView.setAdapter(new QuoteLineAdapter(this, R.layout.banned_item, mQuoteLineItemsList));
    	mLinesListView.setItemsCanFocus(false);
    	mLinesListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
    	mLinesListView.setTextFilterEnabled(true);

    	mLinesListView.setOnItemClickListener(mBannedItemListener);
    	mDoneButton.setOnClickListener(mDoneListener);
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
			mQuoteLineItemsList.get(position).imageActive = !mQuoteLineItemsList.get(position).imageActive;	
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
		
		StringBuffer finalText = new StringBuffer();
		QuoteLineItem q;
		
		int lastAddedIndex = 0; 
		boolean first = true;
		
		int quoteLinesListLen = mQuoteLineItemsList.size();
		for (int i=0; i < quoteLinesListLen; i++) {
			q = mQuoteLineItemsList.get(i);
			
			if (q.imageActive) {
				// Add a newline between different blocks
				if (i - lastAddedIndex > 1) {
					if (!first)finalText.append("\n\n"); // Not for the first quote
					else first = false;
				}
				finalText.append(q.text + "\n");
				lastAddedIndex = i;
			}
		}
		
		Intent data = new Intent();
		data.putExtra("quotedMessage", finalText.toString());
		data.putExtra("multipleFollowup", mMultipleFollowup);
		setResult(RESULT_OK, data);
		finish();
	} 
	
	
	private void touchAll(boolean check) {
		
		for (QuoteLineItem b : mQuoteLineItemsList) 
			b.imageActive = check;
			
		mLinesListView.invalidateViews();	
	}
	
	
	private class QuoteLineItem {
		String text;
		boolean imageActive;
		
		QuoteLineItem(String t, boolean active) {
			text = t;
			imageActive = active;
		}
	}
	

	// ===================================================================
	// Extension of ArrayAdapter which holds and maps the article fields
	// ===================================================================
	private class QuoteLineAdapter extends ArrayAdapter<QuoteLineItem> {

		private ArrayList<QuoteLineItem> items;

		public QuoteLineAdapter(Context context, int textViewResourceId, ArrayList<QuoteLineItem> items) {
			super(context, textViewResourceId, items);
			this.items = items;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {

			View v = convertView;
			if (v == null) {
				LayoutInflater vi = (LayoutInflater) QuotingActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(R.layout.quoted_item, null);
			}

			QuoteLineItem it = items.get(position);

			if (it != null) {
				TextView text = (TextView) v.findViewById(R.id.text_quoteline);
				text.setText(it.text);

				final ImageView banimg = (ImageView) v.findViewById(R.id.img_quoting);
				
				banimg.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						QuotingActivity.this.imgQuoteLineClicked(position);
					}
				});
				
				if (it.imageActive)
					banimg.setImageDrawable(getResources().getDrawable(R.drawable.btn_check_on));
				else
					banimg.setImageDrawable(getResources().getDrawable(R.drawable.btn_check_off));
			}
			return v;
		}
	}
	

	void imgQuoteLineClicked(int position) {

		QuoteLineItem quotelineitem = mQuoteLineItemsList.get(position);
		quotelineitem.imageActive = !quotelineitem.imageActive;

		mLinesListView.invalidateViews();
	}	
	
}


