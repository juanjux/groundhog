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

import org.apache.commons.net.nntp.Article;

public class HeaderItemClass {
	
	private Article mArticle;
	public Article getArticle() {	return mArticle; }

	private int mLevel;
	private String mLevelStr = null;
	private String mSpaceStr = null;
	
	public int getLevel() { return mLevel; }
	
	//private char mLevelIndicator = '»';
	//public void setLevelIndicator(char levelIndicator) { mLevelIndicator = levelIndicator; }
	
	private String mFromNoEmail;
	
	public boolean read    = false;
	public boolean starred = false;
	public boolean banned  = false;
	public boolean myreply = false;

	// =================================================================
	// In the constructor we generate the from without the email
	// =================================================================
	
	public HeaderItemClass(Article article, int level) {
		
		mArticle = article;
		mLevel = level;
		
		if (!article.isDummy()) {
			
			String from = article.getFrom();
			int idx = from.indexOf('<');
			
			if (idx != -1) {
				mFromNoEmail = from.substring(0, from.indexOf('<'));
				
			} else 
				mFromNoEmail = from;
			
			mFromNoEmail = mFromNoEmail.replaceAll("\"", "").trim();
			
			// Fix for some articles that have level 0 even being inside a thread
			// (probably bug in the Threader)
			if (article.getReferences().length > 0 && mLevel == 0)
				mLevel = 1;
		}
	}

	
	public String getLevelStr() {
		
		if (mLevelStr == null) {
			StringBuilder levelBuf = new StringBuilder("");
		
			int proxLevel = mLevel;
			if (proxLevel > 0) {
				for (int i=0; i<proxLevel; i++) 
					levelBuf.append("=");
			}
			mLevelStr = levelBuf.toString();
		}
		return mLevelStr;
	}

	
	public String getSpaceStr() {
		
		if (mSpaceStr == null) {
			StringBuilder levelBuf = new StringBuilder("");
			int proxLevel = mLevel;
		
			if (proxLevel > 0) {
				for (int i=0; i<proxLevel; i++) 
					levelBuf.append(' ');
			}
			mSpaceStr = levelBuf.toString();
		}
		
		return mSpaceStr;
	}
	
	
	public String getFromNoEmail() {
		return mFromNoEmail;
	}
	
}
