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

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import com.almarsoft.GroundhogReader.lib.UsenetConstants;


public class HelpActivity extends Activity {
    private static final int ID_DIALOG_LOADING = 0;
	/** Activity showing one message */
	
	private WebView mContent;
	private Button mButton_Close;
	private WebSettings mWebSettings;
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	
    	setContentView(R.layout.quickhelp);
    	
        mContent = (WebView) this.findViewById(R.id.help_content);
        
        mWebSettings = mContent.getSettings();
        mWebSettings.setDefaultTextEncodingName("utf-8");
        mWebSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        mWebSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        mWebSettings.setJavaScriptEnabled(false);
        mWebSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        mWebSettings.setSupportZoom(true);
        
        mButton_Close = (Button) this.findViewById(R.id.btn_close);        
		mButton_Close.setOnClickListener(new OnClickListener() {
		    	
			public void onClick(View arg0) {
				finish();
			}
        });
		    
        mButton_Close.setFocusable(false);
        mContent.requestFocus();
        
        this.showDialog(ID_DIALOG_LOADING);
        
        mContent.setWebViewClient(new WebViewClient() {  
		     @Override  
		     public void onPageFinished(WebView view, String url)  
		     {
		    	 HelpActivity.this.dismissDialog(ID_DIALOG_LOADING);
		     }  
       	});          
        mContent.loadUrl(UsenetConstants.QUICKHELPURL);
    }
    
    
	@Override
	protected Dialog onCreateDialog(int id) {
		if(id == ID_DIALOG_LOADING){
			
			ProgressDialog loadingDialog = new ProgressDialog(this);
			loadingDialog.setMessage(getString(R.string.loading_help_d));
			loadingDialog.setIndeterminate(true);
			loadingDialog.setCancelable(true);
			return loadingDialog;
		}

		return super.onCreateDialog(id);
	}
    
}