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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.commons.net.nntp.Article;
import org.apache.commons.net.nntp.NNTPNoSuchMessageException;
import org.apache.james.mime4j.message.Header;
import org.apache.james.mime4j.message.Message;
import org.apache.james.mime4j.message.TextBody;
import org.apache.james.mime4j.parser.Field;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.almarsoft.GroundhogReader.lib.DBUtils;
import com.almarsoft.GroundhogReader.lib.FSUtils;
import com.almarsoft.GroundhogReader.lib.GroundhogApplication;
import com.almarsoft.GroundhogReader.lib.MessageTextProcessor;
import com.almarsoft.GroundhogReader.lib.ServerAuthException;
import com.almarsoft.GroundhogReader.lib.ServerManager;
import com.almarsoft.GroundhogReader.lib.UsenetConstants;
import com.almarsoft.GroundhogReader.lib.UsenetReaderException;


public class MessageActivity extends Activity {
    /** Activity showing one message */
	
	private static final int FINISHED_GET_OK = 1;
	private static final int FETCH_FINISHED_ERROR = 2;
	private static final int FETCH_FINISHED_NOMESSAGE = 3;
	private static final int FETCH_FINISHED_NODISK = 4;
	
	private int mMsgIndexInArray;
	private long[] mArticleNumbersArray;
	private String mGroup;
	
	// Loaded from the thread, read by the UI updater:
	private String mBodyText;
	private String mOriginalText;
	private String mSubjectText;
	private String mAuthorText;
	private String mLastSubject;
	private String mCharset = null;
	private Header mHeader;
	private Message mMessage;
	private Vector<HashMap<String, String>> mMimePartsVector;
	private boolean mIsFavorite = false;
	private boolean mShowFullHeaders = false;
	
	private LinearLayout mMainLayout;
	//private LinearLayout mLayoutAuthor;
	private LinearLayout mLayoutSubject;
	private LinearLayout mLayoutDate;
	private TextView mAuthor;
	private ImageView mHeart;
	private TextView mSubject;
	private TextView mDate;
	private WebView mContent;
	private WebSettings mWebSettings;
	private ImageButton mButton_Prev;
	private ImageButton mButton_Next;
	private ImageButton mButton_GoGroup;
	private ScrollView mScroll;
	private Context mContext;
	private AlertDialog mConfigAlert;
	
	private SharedPreferences mPrefs;
	final Handler mHandler = new Handler();
	private ServerManager mServerManager;
	private boolean mOfflineMode;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    
    	mContext = getApplicationContext();
    	setContentView(R.layout.message);
    	
    	// Config checker alert dialog
    	mConfigAlert = new AlertDialog.Builder(this).create();
		mConfigAlert.setButton(getString(R.string.ok),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dlg, int sumthin) {
						startActivity(new Intent(MessageActivity.this, OptionsActivity.class));
					}
				}
		); 
    	
    	mPrefs   = PreferenceManager.getDefaultSharedPreferences(this);
    	
    	mOfflineMode = mPrefs.getBoolean("offlineMode", true);
    
    	Bundle extras = getIntent().getExtras();
    	mMsgIndexInArray     = extras.getInt("msgIndexInArray");
    	mArticleNumbersArray = extras.getLongArray("articleNumbers");
    	mGroup               = extras.getString("group");

    	mMainLayout    = (LinearLayout) this.findViewById(R.id.main_message_layout);
    	//mLayoutAuthor  = (LinearLayout) this.findViewById(R.id.layout_author);
    	mLayoutSubject = (LinearLayout) this.findViewById(R.id.layout_subject);
    	mLayoutDate    = (LinearLayout) this.findViewById(R.id.layout_date);
    	
        mAuthor  = (TextView) this.findViewById(R.id.text_author);
        mHeart   = (ImageView) this.findViewById(R.id.img_love);
        //mHeart.setVisibility(ImageView.INVISIBLE);
        mDate    = (TextView) this.findViewById(R.id.text_date);
        mSubject = (TextView) this.findViewById(R.id.text_subject);
        mSubjectText = null;
        mLastSubject = null;
        
        mContent = (WebView) this.findViewById(R.id.text_content);
        mWebSettings = mContent.getSettings();
        mWebSettings.setDefaultTextEncodingName("utf-8");
        mWebSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        mWebSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        mWebSettings.setJavaScriptEnabled(false);
        mWebSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        mWebSettings.setSupportZoom(false);
        this.setWebViewSizeFromPrefs(0);
        
        mScroll  = (ScrollView) this.findViewById(R.id.textAreaScroller);
        
        // Conectar los botones next y prev (sumar/restar 1 a mMsgIndexInArray y
        // llamar a loadMessage();
        mButton_Prev = (ImageButton) this.findViewById(R.id.btn_prev);        
		    mButton_Prev.setOnClickListener(new OnClickListener() {
		    	
				public void onClick(View arg0) {
					if (mMsgIndexInArray > 0) {
						mMsgIndexInArray--;
						loadMessage();
					} else {
						Toast.makeText(MessageActivity.this, getString(R.string.at_first_message), Toast.LENGTH_SHORT).show();
					}
	
				}
	        });
		    
        mButton_Next = (ImageButton) this.findViewById(R.id.btn_next);        
	    mButton_Next.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				if (mMsgIndexInArray+1 < mArticleNumbersArray.length) {
					mMsgIndexInArray++;
					loadMessage();
				} else {
					Toast.makeText(MessageActivity.this, getString(R.string.no_more_messages), Toast.LENGTH_SHORT).show();
				}

			}
        });
	    
	    mButton_GoGroup = (ImageButton) this.findViewById(R.id.btn_gogroup);
	    	mButton_GoGroup.setOnClickListener(new OnClickListener() {
	    		public void onClick(View v) {
	    			/*
	        		Intent intent_MsgList = new Intent(MessageActivity.this, MessageListActivity.class);
	    			intent_MsgList.putExtra("selectedGroup", mGroup);
	    			MessageActivity.this.startActivity(intent_MsgList);
	    			*/
	    			MessageActivity.this.finish();
	    		}
	    	}
	    	);
	    	
	    
    	mServerManager = new ServerManager(mContext);
		    
        loadMessage();
        
		mButton_Prev.setFocusable(false);        
        mButton_Next.setFocusable(false);
        mContent.requestFocus();
    }
    
    
    private WebViewClient mWebViewClient = new WebViewClient() {
        @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
        
          // Workaround for an Android bug, sometimes if the url doesn't contain a server address it doesn't works
          if (url.startsWith("attachment://fake.com/")) {
        	  MessageActivity.this.attachClicked(url.replace("attachment://fake.com/", ""));
        	  return true;
          }
          
          else {
        	 Intent intent = new Intent();
			 intent.setAction(android.content.Intent.ACTION_VIEW);
			 intent.addCategory("android.intent.category.BROWSABLE"); 
			 Uri myUri = Uri.parse(url);
			 intent.setData(myUri);
			 startActivity(intent);
			 return true;
          }
        }
    };
    
    
    protected void attachClicked(final String attachcode) {
    	
    	HashMap<String, String> tmpattachPart = null;
    	String tmpmd5 = null;
    	
    	for (HashMap<String, String> part : mMimePartsVector) {
    		tmpmd5 = part.get("md5");
    		if (tmpmd5.equals(attachcode)) {
    			tmpattachPart = part;
    			break;
    		}
    	}
    	
    	final String md5 = tmpmd5;
    	final HashMap<String, String> attachPart = tmpattachPart;
    	
    	if (attachPart != null && md5 != null) {
	    		
			new AlertDialog.Builder(this).setTitle(getString(R.string.attachment)).setMessage(
					getString(R.string.open_save_attach_question))
				.setPositiveButton(getString(R.string.open),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dlg, int sumthin) {
							 Intent intent = new Intent(); 
							 intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); 
							 intent.setAction(android.content.Intent.ACTION_VIEW);
							 File attFile = new File(UsenetConstants.EXTERNALSTORAGE + "/" + UsenetConstants.APPNAME + "/" + UsenetConstants.ATTACHMENTSDIR + "/" + mGroup, md5);
							 Uri attachUri = Uri.fromFile(attFile);
							 intent.setDataAndType(attachUri, attachPart.get("type"));
							 startActivity(intent); 
						}
					}
				).setNegativeButton(getString(R.string.save),	
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dlg, int sumthin) {
							try {
								String finalPath = FSUtils.saveAttachment(md5, mGroup, attachPart.get("name"));
								Toast.makeText(MessageActivity.this, getString(R.string.saved_to) + finalPath, Toast.LENGTH_LONG).show();
							} catch(IOException e) { 	
								e.printStackTrace();
								Toast.makeText(MessageActivity.this, getString(R.string.could_not_save_colon) + e.toString(), Toast.LENGTH_LONG).show();
							}
						}
					})
				.show();
    	}
	}


	private void setWebViewSizeFromPrefs(int increase) {
    	
    	int textSize = mPrefs.getInt("webViewTextSize", UsenetConstants.TEXTSIZE_NORMAL);
    	
    	if (increase > 0) {
    		if (textSize < UsenetConstants.TEXTSIZE_LARGEST) {
    			textSize++;
    		}
    	} else if (increase < 0) {
    		if (textSize > UsenetConstants.TEXTSIZE_SMALLEST) 
    			textSize--;
    		
    	}
    	
		Editor editor = mPrefs.edit();
		editor.putInt("webViewTextSize", textSize);
		editor.commit();
    	
    	if (mContent != null) {
    		switch(textSize) {
    		case UsenetConstants.TEXTSIZE_SMALLEST:
    			mWebSettings.setTextSize(WebSettings.TextSize.SMALLEST);
    			break;
    			
    		case UsenetConstants.TEXTSIZE_SMALLER:
    			mWebSettings.setTextSize(WebSettings.TextSize.SMALLER);
    			break;
    			
    		case UsenetConstants.TEXTSIZE_NORMAL:
    			mWebSettings.setTextSize(WebSettings.TextSize.NORMAL);
    			break;
    			
    		case UsenetConstants.TEXTSIZE_LARGER:
    			mWebSettings.setTextSize(WebSettings.TextSize.LARGER);
    			break;
    			
    		case UsenetConstants.TEXTSIZE_LARGEST:
    			mWebSettings.setTextSize(WebSettings.TextSize.LARGEST);
    		}
    	}
	}

	@Override
    protected void onPause() {
    	super.onPause();
    	
    	Log.d(UsenetConstants.APPNAME, "MessageActivity onPause");
    	
    	if (mServerManager != null) 
    		mServerManager.stop();
    	mServerManager = null;
    	
    	if (mContent != null)
    		mContent.clearCache(true);
    }
    

    // Try to detect server hostname changes in the settings (if true, go to the 
    // (grouplist activity which will handle better the change)
	@Override
	protected void onResume() {
		super.onResume();
		
		Log.d(UsenetConstants.APPNAME, "MessageActivity onResume");
		
    	// =============================================
    	// Detect empty-values errors in the settings
    	// =============================================
    	GroundhogApplication grapp = (GroundhogApplication)getApplication();
		
    	if (grapp.checkEmptyConfigValues(this, mPrefs)) {
    		mConfigAlert.setTitle(grapp.getConfigValidation_errorTitle());
			mConfigAlert.setMessage(grapp.getConfigValidation_errorText());
			if (mConfigAlert.isShowing()) mConfigAlert.hide();
			mConfigAlert.show();
    	}
    	else {
    		if (mConfigAlert.isShowing()) mConfigAlert.hide();
    	}
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		if (prefs.getBoolean("hostChanged", false)) {
			// The host  has changed in the prefs, go to the GroupList
			this.startActivity(new Intent(MessageActivity.this, GroupListActivity.class));
		}
		
		if (mServerManager == null)
			mServerManager = new ServerManager(mContext);
	}
    
    
    @Override
    protected void onActivityResult(int intentCode, int resultCode, Intent data) {
    	super.onActivityResult(intentCode, resultCode, data);
    	
    	if (intentCode == UsenetConstants.COMPOSEMESSAGEINTENT) {
    		
    		if (resultCode == RESULT_OK) { 
    			
    			if (mOfflineMode && !mPrefs.getBoolean("postDirectlyInOfflineMode", false))
    				Toast.makeText(mContext, getString(R.string.stored_outbox_send_next_sync), Toast.LENGTH_SHORT).show();
    			else
    				Toast.makeText(mContext, getString(R.string.message_sent), Toast.LENGTH_SHORT).show();
    		}
    		else if (resultCode == RESULT_CANCELED) 
    			Toast.makeText(mContext, getString(R.string.message_discarded), Toast.LENGTH_SHORT).show();
    		
    	} else if (intentCode == UsenetConstants.BANNEDACTIVITYINTENT) {
    		
    		if (resultCode == RESULT_OK) 
    			Toast.makeText(mContext, getString(R.string.reload_tosee_unbanned_authors), Toast.LENGTH_LONG).show();
    		else if (resultCode == RESULT_CANCELED) 
    			Toast.makeText(mContext, getString(R.string.nothing_to_unban), Toast.LENGTH_SHORT).show();
    		
    	} else if (intentCode == UsenetConstants.QUOTINGINTENT) {
    		
    		String composeText;
    		if (resultCode == RESULT_OK) 
    			composeText = data.getStringExtra("quotedMessage");
    		else 
    			composeText = mOriginalText;
    		
    		Intent intent_Post = new Intent(MessageActivity.this, ComposeActivity.class);
			intent_Post.putExtra("isNew", false);
			intent_Post.putExtra("From", mAuthorText);
			intent_Post.putExtra("Newsgroups", mHeader.getField("Newsgroups").getBody().trim());
			intent_Post.putExtra("Date", mHeader.getField("Date").getBody().trim());
			intent_Post.putExtra("Message-ID", mHeader.getField("Message-ID").getBody().trim());
			if (mHeader.getField("References") != null)
				intent_Post.putExtra("References", mHeader.getField("References").getBody().trim());
			if (mSubjectText != null)
				intent_Post.putExtra("Subject", mSubjectText);
			intent_Post.putExtra("bodytext", composeText);			
			if (data != null)
				intent_Post.putExtra("multipleFollowup", data.getStringExtra("multipleFollowup"));
			intent_Post.putExtra("group", mGroup);
			startActivityForResult(intent_Post, UsenetConstants.COMPOSEMESSAGEINTENT);
    	}
    }
	
	
	// ================================================
	// Menu setting
	// ================================================
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		new MenuInflater(mContext).inflate(R.menu.messagemenu, menu);
		
		return(super.onCreateOptionsMenu(menu));

	}
	
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		int textSize = mPrefs.getInt("webViewTextSize", UsenetConstants.TEXTSIZE_NORMAL);

		MenuItem bigtext   = menu.findItem(R.id.message_menu_bigtext);
		MenuItem smalltext = menu.findItem(R.id.message_menu_smalltext);
		
		if (textSize == UsenetConstants.TEXTSIZE_LARGEST) 
			bigtext.setEnabled(false);
		else
			bigtext.setEnabled(true);
		
		if (textSize == UsenetConstants.TEXTSIZE_SMALLEST)
			smalltext.setEnabled(false);
		else
			smalltext.setEnabled(true);
		return (super.onPrepareOptionsMenu(menu));
		
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		
			case R.id.message_menu_reply:
				
				if (mHeader != null) {
					String multipleFollowup = mPrefs.getString("multipleFollowup", "ASK");
			    	String groups = mHeader.getField("Newsgroups").getBody().trim();
			    	String[] groupsArray = null;
			    	
			    	// If is configured to ask for multiple followup and there are in fact multiple, show the dialog
			    	if (groups != null) {
			    		groupsArray = groups.split(",");
			    		if (groupsArray.length > 1) {
			    			if (multipleFollowup.equalsIgnoreCase("ASK")) {
			    				showFollowupDialog(groupsArray);
			    				return true;
			    			}
			    		}
			    	} 
			    	
			    	startPostingOrQuotingActivity(multipleFollowup);
				} else {
					Toast.makeText(this, getString(R.string.cant_reply_no_header_data), Toast.LENGTH_SHORT).show();
				}
				
		    	return true;
				
			case R.id.message_menu_settings:
        		startActivity(new Intent(MessageActivity.this, OptionsActivity.class));
				return true;
				
			case R.id.message_menu_markunread:
				DBUtils.markAsUnRead(mArticleNumbersArray[mMsgIndexInArray], mContext);
				Toast.makeText(this, getString(R.string.message_marked_unread), Toast.LENGTH_SHORT).show();
				return true;
				
			case R.id.message_menu_forward:
				forwardMessage();
				return true;
				
			case R.id.message_menu_favorite:
				toggleFavoriteAuthor();
				return true;
				
			case R.id.message_menu_ban:
				if (mHeader != null) {
					DBUtils.banUser(mHeader.getField("From").getBody().trim(), mContext);
					Toast.makeText(this, getString(R.string.author_banned_reload_tohide), Toast.LENGTH_LONG).show();
				} else 
					Toast.makeText(this, getString(R.string.cant_ban_no_header_data), Toast.LENGTH_SHORT).show();
						
				return true;
				
			case R.id.message_menu_manageban:
				Intent intent_bannedthreads = new Intent(MessageActivity.this, BannedActivity.class);
				intent_bannedthreads.putExtra("typeban", UsenetConstants.BANNEDTROLLS);
				startActivityForResult(intent_bannedthreads, UsenetConstants.BANNEDACTIVITYINTENT);
				return true;
				
			case R.id.message_menu_bigtext:
				setWebViewSizeFromPrefs(1);
				return true;
				
			case R.id.message_menu_smalltext:
				setWebViewSizeFromPrefs(-1);
				return true;
				
			case R.id.message_menu_fullheaders:
				toggleFullHeaders();
				return true;
		}
		return false;
	}    

	
    @Override 
    public void onConfigurationChanged(Configuration newConfig) { 
      //ignore orientation change because it would cause the message body to be reloaded
      super.onConfigurationChanged(newConfig);
    }
    
    
    private void showFollowupDialog(String[] groups) {
    	StringBuilder buf = new StringBuilder(groups.length);
    	
    	for (String g : groups)
    		buf.append(g + "\n");
    	
		new AlertDialog.Builder(this).setTitle("Multiple followup").setMessage(
				getString(R.string.followup_multigroup_question) + buf.toString())
			.setPositiveButton(getString(R.string.followup_all_groups),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dlg, int sumthin) {
						startPostingOrQuotingActivity("ALL");
						}
					}
			).setNegativeButton(getString(R.string.followup_current_group),	
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dlg, int sumthin) {
						startPostingOrQuotingActivity("CURRENT");
						}
					})
			.show();
    }
    
    
    private void startPostingOrQuotingActivity(String multipleFollowup) {
    	
    	if (mHeader == null) {
    		Toast.makeText(this, getString(R.string.cant_reply_no_header_data), Toast.LENGTH_SHORT);
    		return;
    	}
    		
    	
		// Check that the user has set "name" and "email on preferences
		String name  = mPrefs.getString("name", null);
		String email = mPrefs.getString("email", null);
		
		if (name == null || name.trim().length() == 0 || email == null || email.trim().length() == 0) {
			new AlertDialog.Builder(this).setTitle(getString(R.string.user_info_unset)).setMessage(
					getString(R.string.must_fill_name_email_goto_settings))
					.setPositiveButton(getString(R.string.yes),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dlg, int sumthin) {
									startActivity(new Intent(MessageActivity.this, OptionsActivity.class));
									}
								}
							).setNegativeButton(getString(R.string.no), null)
							.show();
			return;
		}
		
		boolean useQuoter = mPrefs.getBoolean("useQuotingView", true);
		if (useQuoter) {
			
			Intent intent_Quote = new Intent(MessageActivity.this, QuotingActivity.class);
			intent_Quote.putExtra("origText", mOriginalText);
			intent_Quote.putExtra("multipleFollowup", multipleFollowup);
			startActivityForResult(intent_Quote, UsenetConstants.QUOTINGINTENT);
			
		} else {
			
			Intent intent_Post = new Intent(MessageActivity.this, ComposeActivity.class);
			intent_Post.putExtra("isNew", false);
			intent_Post.putExtra("From", mAuthorText);
			intent_Post.putExtra("bodytext", mOriginalText);
			intent_Post.putExtra("multipleFollowup", multipleFollowup);
			intent_Post.putExtra("group", mGroup);
			intent_Post.putExtra("Newsgroups", mHeader.getField("Newsgroups").getBody().trim());
			intent_Post.putExtra("Date", mHeader.getField("Date").getBody().trim());
			intent_Post.putExtra("Message-ID", mHeader.getField("Message-ID").getBody().trim());
			if (mHeader.getField("References") != null)
				intent_Post.putExtra("References", mHeader.getField("References").getBody().trim());
			if (mSubjectText != null)
				intent_Post.putExtra("Subject", mSubjectText);
			
			startActivityForResult(intent_Post, UsenetConstants.COMPOSEMESSAGEINTENT);
		}    	
    }
    
    
    private void toggleFavoriteAuthor() {

    	if (mHeader == null) {
    		Toast.makeText(this, getString(R.string.cant_make_favorite_no_header), Toast.LENGTH_SHORT);
    		return;
    	}
    	
    	DBUtils.setAuthorFavorite(mIsFavorite, !mIsFavorite, mHeader.getField("From").getBody().trim(), mContext);
    	mIsFavorite = !mIsFavorite; 
    	
        if (mIsFavorite) {
        	mHeart.setImageDrawable(getResources().getDrawable(R.drawable.love));
        } else {
        	mHeart.setImageDrawable(getResources().getDrawable(R.drawable.nullimage));
        }
    }
    
    
    // ===============================================================
    // Forward a message by email using the configured email program
    // ===============================================================
    private void forwardMessage() {
    	String forwardMsg = "\n\n\nForwarded message originally written by " + mHeader.getField("From").getBody().trim() + 
    	                    " on the newsgroup [" +  mGroup + "]: \n\n" + mOriginalText;
    	
    	Intent i = new Intent(Intent.ACTION_SEND);
    	i.putExtra(Intent.EXTRA_TEXT, forwardMsg);
    	i.putExtra(Intent.EXTRA_SUBJECT, "FWD: " + mSubjectText);
    	i.setType("message/rfc822");
    	startActivity(Intent.createChooser(i, "Title:"));
    }

    
    // Create the progress dialog and call the AsyncTask
    private void loadMessage() {
		new LoadMessageTask().execute(mArticleNumbersArray[mMsgIndexInArray]);
    }
    

    private void toggleFullHeaders() {
    	
    	mShowFullHeaders = !mShowFullHeaders;
    	loadMessage();
    }
    
    
    
    
    // ====================================================
    // ====================================================
    // AsyncTask to load the message and display it
    // ====================================================
    // ====================================================
    
    private class LoadMessageTask extends AsyncTask<Long, String, Integer > {
    	
    	private String mErrorMsg;
    	private ProgressDialog mProgress = null;
    	
    	@Override
    	protected void onPreExecute() {
    		MessageActivity mi = MessageActivity.this;
    		mProgress = ProgressDialog.show(mi, MessageActivity.this.getString(R.string.message), mi.getString(R.string.requesting_message));
    	}
    	
    	@SuppressWarnings("unchecked")
		protected Integer doInBackground(Long... serverMsgNumbers ) {
    		
    		mErrorMsg = "";
    		
    		try {
	    		publishProgress(getString(R.string.fetching_body));
	    		
	    		long serverMsgNumber = serverMsgNumbers[0];  		
	    		Hashtable<String, Object> articleData = DBUtils.getHeaderRecordCatchedData(mGroup, serverMsgNumber, MessageActivity.this);
	    		boolean isCatched = (Boolean) articleData.get("catched");
	    		
	    		if (!isCatched) 
	    			mServerManager.selectNewsGroup(mGroup, mOfflineMode);
    		    else  // This wont connect (since the message is catched), so the loading will be faster even in online mode
    		    	mServerManager.selectNewsGroupWithoutConnect(mGroup);
	    		
	    		// ===========================================================================================
	    		// Get or load the header, and from the header, the from, subject, date,
	    		// and Content-Transfer-Encoding
	    		// ===========================================================================================

	    		mHeader = mServerManager.getHeader((Integer)articleData.get("id"), 
	    				                           (String)articleData.get("server_article_id"), 
	    				                           false, isCatched);    	    		

	    		if (mHeader == null)
	    			throw new UsenetReaderException(getString(R.string.could_not_fetch_header));
	    		
	    		// ===========================================================================================
	    		// Extract the charset from the Content-Type header or if it's MULTIPART/MIME, the boundary
	    		// between parts
	    		// ===========================================================================================

	    		String[] tmpContentArr = null;
	    		String[] contentTypeParts = null;
	    		String tmpFirstToken;
	    		
	    		mCharset = mPrefs.getString("readDefaultCharset", "ISO8859-15");
	    		
	    		Field tmpField = mHeader.getField("Content-Type");
	    		if (tmpField != null) {
	    			tmpContentArr = tmpField.getBody().trim().split(";");
	    		
    	    		for (String cont : tmpContentArr) {
    	    			contentTypeParts = cont.split("=", 2);
    	    			tmpFirstToken = contentTypeParts[0].trim();
    	    			
    	    			if (contentTypeParts.length > 1 && tmpFirstToken.equalsIgnoreCase("charset")) 
    	    				mCharset = contentTypeParts[1].replace("\"", "").trim();
    	    		}
	    		}
	    		
	    		// ===============================================================================
	    		// Get or load the body, extract the mime text/plain part if it is a Mime message
	    		// and decode if it is QuotedPrintable
	    		// ===============================================================================
	    		mMessage = mServerManager.getMessage(mHeader, 
	    				                             (Integer)articleData.get("id"),
	    				                             (String)articleData.get("server_article_id"),
	    				                             false, isCatched, mCharset, getCacheDir());

	    		Vector<Object> body_attachs = MessageTextProcessor.extractBodySaveAttachments(mGroup, mMessage);
	    		TextBody textBody = (TextBody)body_attachs.get(0);
	    		if (textBody == null)
	    			throw new UsenetReaderException("Unkown error parsing the message :(");
	    		
	    		if (mHeader.getField("MIME-Version") != null)
	    			mMimePartsVector  = (Vector<HashMap<String, String>>)body_attachs.get(1);

	    		
	    		if (mSubjectText != null)
	    			mLastSubject = Article.simplifySubject(mSubjectText);
	    		
	    		mSubjectText = MessageTextProcessor.decodeSubject(mHeader.getField("Subject"), mCharset, mMessage);
	    		
	    		// Check for uuencoded attachments
	    		BufferedReader bodyReader = new BufferedReader(textBody.getReader());
	    		Vector<HashMap<String, String>> uuattachData = MessageTextProcessor.saveUUEncodedAttachments(bodyReader, mGroup);
	    		
	    		if (uuattachData != null) {
	    			mBodyText = uuattachData.get(0).get("body");
	    			uuattachData.removeElementAt(0);
	    			
	    			if (uuattachData.size() > 0) {
	    				if (mMimePartsVector == null || mMimePartsVector.size() == 0) 
	    					mMimePartsVector = uuattachData;
	    				else {
	    					// Join the two vectors
	    					for (HashMap<String, String> attach : uuattachData) 
	    						mMimePartsVector.add(attach);
	    				}
	    			}
	    		}
	    		
	    		if (mMimePartsVector != null && mMimePartsVector.size() > 0)
	    			DBUtils.updateHeaderRecordAttachments( (Integer)articleData.get("id"), mMimePartsVector, mContext);
	    		
	    		return FINISHED_GET_OK;
    	
		} catch (NNTPNoSuchMessageException e) {
			mErrorMsg = getString(R.string.error);
			e.printStackTrace();
			return FETCH_FINISHED_NOMESSAGE;
		} catch (FileNotFoundException e) {
			mErrorMsg = getString(R.string.error);
			e.printStackTrace();			
			return FETCH_FINISHED_NODISK;
		} catch (IOException e) {
			mErrorMsg = getString(R.string.error);
			e.printStackTrace();			
			return FETCH_FINISHED_ERROR;
		} catch (ServerAuthException e) {
			mErrorMsg = getString(R.string.error);
			e.printStackTrace();			
			return FETCH_FINISHED_ERROR;
		} catch (UsenetReaderException e) {
			mErrorMsg = getString(R.string.error);
			e.printStackTrace();			
			return FETCH_FINISHED_ERROR;
		} catch (OutOfMemoryError e) {
			mErrorMsg = "Memory error, message with too huge attachments? : " + getString(R.string.error);
			e.printStackTrace();
			return FETCH_FINISHED_ERROR;
		}
    }
    	
    	@Override
	    protected void onProgressUpdate(String... message) {
	    	mProgress.setMessage(message[0]);
	    }
    
    
	    protected void onPostExecute(Integer result) { 
	    	
	    	if (mProgress != null) mProgress.dismiss();
	    	
	    	switch(result) {
	    	
	    	case FINISHED_GET_OK:
	    		// Show or hide the heart marking favorite authors
	            mIsFavorite = DBUtils.isAuthorFavorite(mHeader.getField("From").getBody().trim(), mContext);
	            
	            if (mIsFavorite) 
	            	mHeart.setImageDrawable(getResources().getDrawable(R.drawable.love));
	            else 
	            	mHeart.setImageDrawable(getResources().getDrawable(R.drawable.nullimage));
	            
	            
	            mHeart.invalidate();
	            //mLayoutAuthor.invalidate();
	            mMainLayout.invalidate();
	
	    		// Save a copy of the body for the reply so we don't break netiquette rules with
	    		// the justification applied in sanitizeLinebreaks
	    		mOriginalText = mBodyText;
	    		
	    		// Justify the text removing artificial '\n' chars so it looks square and nice on the phone screen
	    		// XXX: Optimizacion: aqui se puede utilizar de forma intermedia un StringBuffer (sanitize
	    		// lo devuelve y se le pasa a prepareHTML)
	    		
	    		boolean justify = mPrefs.getBoolean("justifyText", false);
	    		if (justify) {
	    			mBodyText = MessageTextProcessor.sanitizeLineBreaks(mBodyText);
	    		}
	    		
	    		mBodyText = MessageTextProcessor.getHtmlHeader(mCharset) + 
	    		            MessageTextProcessor.getAttachmentsHtml(mMimePartsVector)  + 
	    		            MessageTextProcessor.prepareHTML(mBodyText, justify);
	    		
	    		// Show the nice, short, headers or the ugly full headers if the user selected that
	    		if (!mShowFullHeaders) {
	    			//mLayoutAuthor.setVisibility(View.VISIBLE);
	    			mLayoutDate.setVisibility(View.VISIBLE);
	    			mLayoutSubject.setVisibility(View.VISIBLE);
	    			
	    			mAuthorText = MessageTextProcessor.decodeFrom(mHeader.getField("From"), mCharset, mMessage);
	    			mAuthor.setText(mAuthorText);
	    			mDate.setText(mHeader.getField("Date").getBody().trim());
	    			mSubject.setText(mSubjectText);
	    			
	    		} else {
	    			//mLayoutAuthor.setVisibility(View.INVISIBLE);
	    			mLayoutDate.setVisibility(View.INVISIBLE);
	    			mLayoutSubject.setVisibility(View.INVISIBLE);
	    			mBodyText = MessageTextProcessor.htmlizeFullHeaders(mMessage) + mBodyText;
	    		}
	    		
	    		mContent.loadDataWithBaseURL("x-data://base", mBodyText, "text/html", mCharset, null);
	    		mBodyText = null;
	    		mContent.requestFocus();
	    		
	    		DBUtils.markAsRead(mHeader.getField("Message-ID").getBody().trim(), mContext);
	    		
	    		// Go to the start of the message
	    		mScroll.scrollTo(0, 0);
	    		
	    		String simplifiedSubject = Article.simplifySubject(mSubjectText);
	
	    		if (mLastSubject != null && (!mLastSubject.equalsIgnoreCase(simplifiedSubject))) {
	    			Toast.makeText(mContext, getString(R.string.new_subject) + simplifiedSubject, Toast.LENGTH_SHORT).show();
	    		}
	    		
	            // Intercept "attachment://" url clicks
	            mContent.setWebViewClient(mWebViewClient);
	            break;	    	
	    	
    		case FETCH_FINISHED_ERROR:
	    		if (mProgress != null) mProgress.dismiss();
	    		
	    		mContent.loadData(getString(R.string.error_loading_kept_unread), "text/html", "UTF-8");
	    		
				new AlertDialog.Builder(MessageActivity.this)
				.setTitle(getString(R.string.error))
				.setMessage(getString(R.string.error_loading_kept_unread_long) + ":" + mErrorMsg)
			    .setNeutralButton(getString(R.string.close), null)
			    .show();
				break;
	    	
	    	case FETCH_FINISHED_NODISK:
		    		mContent.loadData(getString(R.string.error_saving_kept_unread), "text/html", "UTF-8");
		    		
					new AlertDialog.Builder(MessageActivity.this)
					.setTitle(getString(R.string.error))
					.setMessage(getString(R.string.error_saving_kept_unread_long) + ":" + mErrorMsg)					    
				    .setNeutralButton(getString(R.string.close), null)
				    .show();
					break;
	    	
	    	case FETCH_FINISHED_NOMESSAGE:
		    		mContent.loadData(getString(R.string.server_doesnt_have_message_long), "text/html", "UTF-8");
		    		
					new AlertDialog.Builder(MessageActivity.this)
					.setTitle(getString(R.string.error))
					.setMessage(getString(R.string.server_doesnt_have_message) + ":" + mErrorMsg)
				    .setNeutralButton(getString(R.string.close), null)
				    .show();
					
					DBUtils.markAsRead(mArticleNumbersArray[mMsgIndexInArray], mContext);
					break;
	    	}
	    }
    }
}
