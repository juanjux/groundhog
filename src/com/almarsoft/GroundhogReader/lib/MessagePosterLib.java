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
import java.net.SocketException;
import java.nio.charset.Charset;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.net.nntp.SimpleNNTPHeader;
import org.apache.james.mime4j.codec.EncoderUtil;
import org.apache.james.mime4j.util.ByteSequence;
import org.apache.james.mime4j.util.CharsetUtil;
import org.apache.james.mime4j.util.ContentUtil;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.preference.PreferenceManager;

public class MessagePosterLib {

	private String mCurrentGroup;
	private String mGroups;
	private String mBody;
	private String mSubject;
	private String mReferences;
	private String mPrevMsgId;
	private String mMyMsgId;
	private String mPostCharset;
	private PowerManager.WakeLock mWakeLock = null;
	
	SharedPreferences mPrefs;
	Context mContext;
	
	
	public MessagePosterLib(String currentGroup, String groups, String body, String subject, 
			                String references, String prevMsgId, Context context){
	
		mCurrentGroup = currentGroup;
		mContext = context;
		mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);		
		mGroups = groups.trim();
		mBody = body;
		mSubject = subject.trim();
		mPostCharset = mPrefs.getString("postCharset", "ISO8859_15");
		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK|PowerManager.ON_AFTER_RELEASE, "GroundhogSending");
		
		// Reply to non-first post in a thread
		if (references != null && references.length() > 0) 
			mReferences = references.trim();
		else 
			mReferences = null;
		
		
		// Reply to a thread
		if (prevMsgId != null && prevMsgId.length() > 0)
			mPrevMsgId = prevMsgId.trim();
		else
			mPrevMsgId = null; // Message starting new thread
		
		// Reply to the first post in thread
		if (mReferences == null && mPrevMsgId != null) {
			mReferences = mPrevMsgId;
		}
		
	}

	
	public void postMessage() throws EncoderException, SocketException, IOException, ServerAuthException, UsenetReaderException {
		
		String headerText = createHeaderData();
		String signature  = getSignature();

		ServerManager serverMgr = new ServerManager(mContext);
		mBody = MessageTextProcessor.shortenPostLines(mBody);
		Charset charset = CharsetUtil.getCharset(mPostCharset);		
		ByteSequence bytebody = ContentUtil.encode(charset, mBody);
		mBody = new String(bytebody.toByteArray(), "ISO-8859-1");

		try{
			mWakeLock.acquire();
			serverMgr.postArticle(headerText, mBody, signature);
		} finally {
			if (mWakeLock.isHeld()) mWakeLock.release();
		}
		
		// Log the message to check against future replies in the MessageList
		if (mMyMsgId != null && mCurrentGroup != null)
			DBUtils.logSentMessage(mMyMsgId, mCurrentGroup, mContext);
	}

	
	private String getSignature() {
		String signature = mPrefs.getString("signature", "");
		if (signature.length() > 0) {
			signature = "\n\n" + "-- \n" + signature; 
		}
		
		return signature;
	}
	
	private String createHeaderData() throws EncoderException {        
        String references, from, name, email, date;

        Date now = new Date();
        Format formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);
        date = formatter.format(now);
         
        Charset headerCharset = CharsetUtil.getCharset(mPostCharset);
        
        String tmpName = mPrefs.getString("name", "anonymous");        
        if (EncoderUtil.hasToBeEncoded(tmpName, 0)) {
        	name = EncoderUtil.encodeEncodedWord(tmpName, EncoderUtil.Usage.TEXT_TOKEN, 0, headerCharset, null);
        } else 
        	name = tmpName;
        
		email = "<" + mPrefs.getString("email", "nobody@nobody.no").trim() + ">";
		from = name + " " + email;		
		
		if (EncoderUtil.hasToBeEncoded(mSubject, 0)) {
			mSubject = EncoderUtil.encodeEncodedWord(mSubject, EncoderUtil.Usage.TEXT_TOKEN, 0, headerCharset, null);
		}
		//mSubject = EncoderUtil.encodeIfNecessary(mSubject, EncoderUtil.Usage.TEXT_TOKEN, 0);
		
		SimpleNNTPHeader header = new SimpleNNTPHeader(from, mSubject);
		String[] groups = mGroups.trim().split(",");
		
		for (String group : groups) {
			header.addNewsgroup(group);
		}
		
		header.addHeaderField("Date", date);		
		header.addHeaderField("Content-Type", "text/plain; charset=" + CharsetUtil.toMimeCharset(mPostCharset) +"; format=flowed");
		header.addHeaderField("Content-Transfer-Encoding", "8bit");	

        if (mReferences != null) {
            if (mPrevMsgId != null) {
                references = mReferences + " " + mPrevMsgId;
                header.addHeaderField("In-Reply-To", mPrevMsgId);                
            }
            else {
                references = mReferences;
            }            
            header.addHeaderField("References", references);
        }

        mMyMsgId = generateMsgId();
        header.addHeaderField("Message-ID", mMyMsgId);
        header.addHeaderField("User-Agent", "Groundhog Newsreader for Android");        
        return header.toString();
        
	}
	
	
	private String generateMsgId() {
		String host = mPrefs.getString("host", "noknownhost.com").trim();
		Random rand = new Random();
		String randstr = Long.toString(Math.abs(rand.nextLong()), 72);
		
		return "<" + "almarsoft." + randstr + "@" + host + ">";
		
	}

}
