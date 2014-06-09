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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.commons.net.nntp.Article;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

// XXX: Usar las funciones de consultas preparadas

public class DBUtils {
	
	// Quote sql. This must improve...
	public static String esc(String sqltoken) {
		return "'" + sqltoken.replace("'", "''") + "'";
	}
	
	
	public static String[] getSubscribedGroups(Context context) {
		DBHelper db = new DBHelper(context);
		SQLiteDatabase dbread = db.getReadableDatabase();
		
		Cursor cur = dbread.rawQuery("SELECT name FROM subscribed_groups", null);
		int c = cur.getCount();
		String[] subscribed = null;
		if (c > 0) {
			subscribed = new String[c];

			cur.moveToFirst();
			for (int i=0; i < c; i++) { 
				subscribed[i] = cur.getString(0);
				cur.moveToNext();
			}
		}
		
		cur.close(); dbread.close(); db.close();		
		return subscribed;		
	}
	
	
    public static void updateUnreadInGroupsTable(int unreadCount, int groupid, Context context) {
    	DBHelper dbhelper = new DBHelper(context);
    	SQLiteDatabase dbwriter = dbhelper.getWritableDatabase();
    	dbwriter.execSQL("UPDATE subscribed_groups SET unread_count=" + unreadCount + " WHERE _id=" + groupid);
    	dbwriter.close(); dbhelper.close();    	
    }	

    
    public static void markAsRead(long server_article_number, Context context) {
    	DBHelper dbhelper = new DBHelper(context);
    	SQLiteDatabase dbwriter = dbhelper.getWritableDatabase();
    	dbwriter.execSQL("UPDATE headers SET read=1, read_unixdate=" + System.currentTimeMillis() + 
    			        " WHERE server_article_number="+server_article_number);
    	dbwriter.close(); dbhelper.close();
    }
    
    public static void markAsRead(String msgId, Context context) {
    	
    	if (msgId != null) {
    		
	    	DBHelper dbhelper = new DBHelper(context);
	    	SQLiteDatabase dbwriter = dbhelper.getWritableDatabase();
	    	dbwriter.execSQL("UPDATE headers SET read=1, read_unixdate=" + System.currentTimeMillis() + 
	    			        " WHERE server_article_id="+ esc(msgId));
	    	dbwriter.close(); dbhelper.close();
    	}
    	
    }

    public static void markAsUnread(String msgId, Context context) {
    	
    	if (msgId != null) {
    		
    		DBHelper dbhelper = new DBHelper(context);
    		SQLiteDatabase dbwriter = dbhelper.getWritableDatabase();
    		
    		dbwriter.execSQL("UPDATE headers SET read=0, read_unixdate=0 WHERE server_article_id="+ esc(msgId));
    		dbwriter.close(); dbhelper.close();
    	}
    }  

    public static void markAsUnRead(long server_article_number, Context context) {
    	
    	DBHelper dbhelper = new DBHelper(context);
    	SQLiteDatabase dbwriter = dbhelper.getWritableDatabase();
    	dbwriter.execSQL("UPDATE headers SET read=0, read_unixdate=0 WHERE server_article_number="+server_article_number);
    	dbwriter.close(); dbhelper.close();
    }
    
    
    public static int getGroupIdFromName(String group, Context context) {
    	
		DBHelper db = new DBHelper(context);
		SQLiteDatabase dbread = db.getReadableDatabase();
		
    	// First get the group ID
    	String query = "SELECT _ID FROM subscribed_groups WHERE name=" + esc(group);
    	Cursor cur = dbread.rawQuery(query, null);
    	
    	if (cur.getCount() != 1) { // WTF?? 
    		Log.w("GroundhogReader", "Trying to get id for group named " + group + " which doesnt exists on DB");
    		cur.close(); dbread.close(); db.close();
    		return -1;
    	}
    		
    	cur.moveToFirst();
    	int groupid = cur.getInt(0);
    	cur.close(); dbread.close(); db.close();
    	
    	return groupid;	
    }
    
    
    public static String getGroupNameFromId(int groupid, Context context) {
    	
    	DBHelper db = new DBHelper(context);
    	SQLiteDatabase dbread = db.getReadableDatabase();
    	
    	String query = "SELECT name FROM subscribed_groups WHERE _id=" + groupid;
    	Cursor cur = dbread.rawQuery(query, null);
    	
    	if (cur.getCount() != 1) {
    		Log.w("GroundhogReader", "Trying to get name for groupid " + groupid + " which doesnt exists on DB");
    		cur.close(); dbread.close(); db.close();
    		return null;
    	}
    	
    	cur.moveToFirst();
    	String groupname = cur.getString(0);
    	cur.close(); dbread.close(); db.close();
    	
    	return groupname;
    }
    
    
    public static void groupMarkAllRead(String group, Context context) {
    	int groupid = getGroupIdFromName(group, context);
    	
    	if (groupid == -1) 
    		return;
    	
		DBHelper db = new DBHelper(context);
		SQLiteDatabase dbWrite = db.getWritableDatabase();
    	
		String query = "UPDATE headers SET read=1, read_unixdate=" + System.currentTimeMillis() + 
		              " WHERE subscribed_group_id=" + groupid;
		dbWrite.execSQL(query);
		
		query = "UPDATE subscribed_groups SET unread_count=0 WHERE _ID=" + groupid;
		dbWrite.execSQL(query);

		dbWrite.close(); db.close();
    }


    /**
     * Unsubscribe a group, deleting from the DB the headers and from the disk the group's directory storage 
     * for bodies and attachments.
     * 
     */
	public static void unsubscribeGroup(String group, Context context) {
		
		int groupid = getGroupIdFromName(group, context);
		
		if (groupid == -1) 
			return;
		
		DBHelper db = new DBHelper(context);
		SQLiteDatabase dbWrite = db.getWritableDatabase();
		
		String query = "DELETE FROM subscribed_groups WHERE _ID=" + groupid;
		dbWrite.execSQL(query);
		query = "DELETE FROM headers WHERE subscribed_group_id=" + groupid;
		dbWrite.execSQL(query);
		dbWrite.close(); db.close();
		
		FSUtils.deleteDirectory(UsenetConstants.EXTERNALSTORAGE + "/" + UsenetConstants.APPNAME + "/offlinecache/groups/" + group);
		
		FSUtils.deleteDirectory(UsenetConstants.EXTERNALSTORAGE + "/" + UsenetConstants.APPNAME + "/" + 
                                UsenetConstants.ATTACHMENTSDIR  + "/" + group);
	}
	

	public static void updateStarredThread(boolean starred, String clean_subject, int groupid, Context context) {
		DBHelper db = new DBHelper(context);
		SQLiteDatabase dbWrite = db.getWritableDatabase();
		
		clean_subject = clean_subject.replace("'", "''");
		
		String query;
		
		if (starred == false) {
			query = "DELETE FROM starred_threads WHERE subscribed_group_id=" + groupid +
			               " AND clean_subject=" + esc(clean_subject);
			dbWrite.execSQL(query);
		}
		else {
			// Check that it's not already on the table
			query = "SELECT _ID FROM starred_threads WHERE subscribed_group_id=" + groupid +
			        " AND clean_subject=" + esc(clean_subject);
			Cursor c = dbWrite.rawQuery(query, null);
			
			if (c.getCount() == 0) {
				ContentValues cv = new ContentValues();
				cv.put("subscribed_group_id", groupid);
				cv.put("clean_subject", clean_subject);
				dbWrite.insert("starred_threads", null, cv);
			}
			c.close();
		}
		dbWrite.close(); db.close();
	}

	
	public static HashSet<String> getStarredSubjectsSet(Context context) {
		DBHelper db = new DBHelper(context);
		SQLiteDatabase dbread = db.getReadableDatabase();
		Cursor c;
		
		String query = "SELECT clean_subject FROM starred_threads";
		c = dbread.rawQuery(query, null);
		HashSet<String> set = new HashSet<String>(c.getCount());
		
		c.moveToFirst();
		int count = c.getCount();
		for (int i = 0; i < count; i++) {
			set.add(c.getString(0));
			c.moveToNext();
		}
		
		c.close(); dbread.close(); db.close();
		return set;
	}

	
	public static HashSet<String> getBannedThreads(String group, Context context) {
		
		HashSet<String> bannedThreads = null;
		
		int groupid = getGroupIdFromName(group, context);
		
		DBHelper db = new DBHelper(context);
		SQLiteDatabase dbread = db.getReadableDatabase();
		
		String q = "SELECT clean_subject FROM banned_threads WHERE subscribed_group_id=" 
			        + groupid + " AND bandisabled=0";
		
		Cursor c = dbread.rawQuery(q, null);
		if (c.getCount() > 0) {
			
			bannedThreads = new HashSet<String>(c.getCount());
			c.moveToFirst();
			
			int count = c.getCount();
			for (int i=0; i < count; i++) {
				bannedThreads.add(c.getString(0));
				c.moveToNext(); 
			}
		}
		
		c.close(); dbread.close(); db.close();
		
		if (bannedThreads == null) bannedThreads = new HashSet<String>(0);
		return bannedThreads;
	}
	
	
	public static HashSet<String> getFavoriteAuthors(Context context) {
		
		HashSet<String> favoriteAuthors = null;
		
		DBHelper db = new DBHelper(context);
		SQLiteDatabase dbread = db.getReadableDatabase();
		
		Cursor c = dbread.rawQuery("SELECT name FROM favorite_users", null);
		if (c.getCount() > 0) {
			favoriteAuthors = new HashSet<String>(c.getCount());
			c.moveToFirst();
			
			int count = c.getCount();
			for (int i=0; i < count; i++) {
				favoriteAuthors.add(c.getString(0));
				c.moveToNext();
			}
		}
		
		c.close(); dbread.close(); db.close();
		
		if (favoriteAuthors == null) favoriteAuthors = new HashSet<String>(0);
		return favoriteAuthors;
	}

	
	public static HashSet<String> getReadMessagesSet(String group, Context context) {
		int groupid = getGroupIdFromName(group, context);
		
		HashSet<String> readSet = null;
		
		DBHelper db = new DBHelper(context);
		SQLiteDatabase dbread = db.getReadableDatabase();
		
		String q = "SELECT server_article_id FROM headers WHERE read=1 AND subscribed_group_id=" + groupid;
		Cursor c = dbread.rawQuery(q, null);
		int count = c.getCount();
		
		if (count > 0) {
			readSet = new HashSet<String>(c.getCount());
			c.moveToFirst();
			
			for (int i=0; i < count; i++) {
				readSet.add(c.getString(0));
				c.moveToNext();
			}
		}
		
		c.close(); dbread.close(); db.close();
		
		if (readSet == null) readSet = new HashSet<String>(0);
		return readSet;
	}

	
	public static HashSet<String> getBannedTrolls(Context context) {
		
		HashSet<String> bannedTrolls = null;
		
		DBHelper db = new DBHelper(context);
		SQLiteDatabase dbwrite = db.getWritableDatabase();
		
		String q = "SELECT name FROM banned_users WHERE bandisabled=0";
		
		Cursor c = dbwrite.rawQuery(q, null);
		
		int count = c.getCount();
		if (count > 0) {
			
			bannedTrolls = new HashSet<String>(c.getColumnCount());
			c.moveToFirst();
			
			for (int i=0; i < count; i++) {
				bannedTrolls.add(c.getString(0));
				c.moveToNext();
			}
		}
		
		c.close(); dbwrite.close(); db.close();
		
		if (bannedTrolls == null) bannedTrolls = new HashSet<String>(0);
		return bannedTrolls;
	}	
	
	
	public static void banThread(String group, String clean_subject, Context context) {
		int groupid = getGroupIdFromName(group, context);
		
		DBHelper db = new DBHelper(context);
		SQLiteDatabase dbwrite = db.getWritableDatabase();

		
		// First, check if it already is on the banned_threads table (it could be with unbanned=1)
		Cursor c = dbwrite.rawQuery("SELECT _id FROM banned_threads " + 
				                    " WHERE subscribed_group_id=" + groupid +
				                    " AND clean_subject=" + esc(clean_subject), null);
		
		if (c.getCount() > 0) { // Existed
			c.moveToFirst();
			dbwrite.execSQL("UPDATE banned_threads SET bandisabled=0 WHERE _id=" + c.getInt(0));
			
		} else { 
			// New troll goes down to the pit
			ContentValues cv = new ContentValues();
			cv.put("subscribed_group_id", groupid);
			cv.put("bandisabled", 0);
			cv.put("clean_subject", clean_subject);
			dbwrite.insert("banned_threads", null, cv);
		}
		
		// Mark all the messages from the thread as read so they get cleaned later
		dbwrite.execSQL("UPDATE headers SET read=1, read_unixdate=" + System.currentTimeMillis() + 
				       " WHERE subscribed_group_id=" + groupid +
				        " AND clean_subject=" + esc(clean_subject)); 
		
		c.close(); dbwrite.close(); db.close();
	}

	
	public static void banUser(String decodedfrom, Context context) {
		DBHelper db = new DBHelper(context);
		SQLiteDatabase dbwrite = db.getWritableDatabase();
		
		Cursor c = dbwrite.rawQuery("SELECT _id FROM banned_users " +
				                    " WHERE name=" + esc(decodedfrom), 
				                    null);
		
		if (c.getCount() > 0) {
			c.moveToFirst();
			dbwrite.execSQL("UPDATE banned_users SET bandisabled=0 WHERE _id=" + c.getInt(0));
		} else {
			ContentValues cv = new ContentValues();
			cv.put("name", decodedfrom);
			cv.put("bandisabled", 0);
			dbwrite.insert("banned_users", null, cv);
		}
		
		// Mark all the user posts as read, so they get deleted later
		dbwrite.execSQL("UPDATE headers SET read=1, read_unixdate=" + System.currentTimeMillis() + 
				       " WHERE from_header=" + esc(decodedfrom));
		
		c.close(); dbwrite.close(); db.close();
	}	
	
	
	public static void unBanThread(String group, String clean_subject, Context context) {
		int groupid = getGroupIdFromName(group, context);
		
		DBHelper db = new DBHelper(context);
		SQLiteDatabase dbwrite = db.getWritableDatabase();
		
		dbwrite.execSQL("DELETE FROM banned_threads WHERE subscribed_group_id=" + groupid 
				        + " AND clean_subject=" + esc(clean_subject));
		dbwrite.close(); db.close();
		
	}
	
	
	public static void unBanUser(String decodedfrom, Context context) {
		
		DBHelper db = new DBHelper(context);
		SQLiteDatabase dbwrite = db.getWritableDatabase();

		dbwrite.execSQL("DELETE FROM banned_users WHERE name=" + esc(decodedfrom));
		dbwrite.close(); db.close();
	}

	
	// ======================================================================
	// Stores the number of the last fetched message to continue were we left
	// ======================================================================
	public static void storeGroupLastFetchedMessageNumber(String group, long lastNumber, Context context) {
		int groupid = getGroupIdFromName(group, context);
		
		DBHelper db = new DBHelper(context);
		SQLiteDatabase dbwrite = db.getWritableDatabase();

		String wQuery = "UPDATE subscribed_groups SET lastFetched=" + lastNumber + " WHERE _id=" + groupid;
		dbwrite.execSQL(wQuery);
		
		dbwrite.close(); db.close();
	}
	
	
	public static long insertArticleToGroupID(int groupID, Article articleInfo, String finalRefs, 
  			                                    String finalFrom, String finalSubject, Context context, 
  			                                    SQLiteDatabase catchedDB) {
		
		// The called can create a single SQLiteDatabase object to avoid too many object
		// creations if we're inside a loop
		DBHelper db 		   = null;
		SQLiteDatabase dbwrite = null;
		
		if (catchedDB == null) {
			db = new DBHelper(context);
			dbwrite = db.getWritableDatabase();
		}
		else {
			dbwrite = catchedDB;
		}
		
		ContentValues cv = new ContentValues();
		cv.put("subscribed_group_id", groupID);
		cv.put("reference_list", finalRefs);
		cv.put("server_article_id", articleInfo.getArticleId());
		cv.put("date", articleInfo.getDate());
		cv.put("server_article_number", articleInfo.getArticleNumber());
		cv.put("from_header", finalFrom);
		cv.put("subject_header", finalSubject);
		cv.put("read", 0);
		cv.put("catched", 0);
		
		long ret = dbwrite.insert("headers", null, cv);
		
		if (catchedDB == null) {
			dbwrite.close(); db.close();
		}
		return ret;
	}

	
	// Delete all messages and restart all counters from subscribed groups. This is used when 
	// the user changes the server in the preferences, since every server has differente message
	// numbers and messagecounts
	
	public static void restartAllGroupsMessages(Context context) {
		
		DBHelper db = new DBHelper(context);
		SQLiteDatabase dbwrite = db.getWritableDatabase();
		
		dbwrite.execSQL("DELETE FROM headers");
		dbwrite.execSQL("UPDATE subscribed_groups SET lastFetched=-1, unread_count=0");
		FSUtils.deleteDirectory(UsenetConstants.EXTERNALSTORAGE + "/" + UsenetConstants.APPNAME + "/offlinecache/groups");
		FSUtils.deleteDirectory(UsenetConstants.EXTERNALSTORAGE + "/" + UsenetConstants.APPNAME + "/" + UsenetConstants.ATTACHMENTSDIR);
		dbwrite.close(); db.close();
	}


	public static void setGroupAllRead(String group, Context context) {
		int groupid = getGroupIdFromName(group, context);
		
		DBHelper dbhelper = new DBHelper(context);
		SQLiteDatabase dbwriter = dbhelper.getWritableDatabase();
		
		dbwriter.execSQL("UPDATE headers SET read=1, read_unixdate=" + System.currentTimeMillis() + 
				        " WHERE subscribed_group_id=" + groupid);
		dbwriter.close();dbhelper.close();
	}
	
	
	public static long getGroupLastFetchedNumber(String group, Context context) {
		
		long lastFetched = -1;
		
		DBHelper dbhelper = new DBHelper(context);
		SQLiteDatabase readdb = dbhelper.getReadableDatabase();
		
		String fQuery = "SELECT lastFetched FROM subscribed_groups WHERE name=" + esc(group);
		Cursor cur = readdb.rawQuery(fQuery, null);

		if (cur.getCount() > 0) {
			cur.moveToFirst();
			lastFetched = cur.getInt(0);
		}
		
		cur.close(); readdb.close(); dbhelper.close();
		
		return lastFetched;
	}



	public static boolean isGroupSubscribed(String group, Context context) {
		int groupid = getGroupIdFromName(group, context);
		
		DBHelper dbhelper = new DBHelper(context);
		SQLiteDatabase dbread = dbhelper.getReadableDatabase();

		Cursor cur = dbread.rawQuery("SELECT _id FROM subscribed_groups WHERE _id=" + groupid, null);
		boolean ret = (cur.getCount() > 0);
		
		cur.close(); dbread.close(); dbhelper.close();
		return ret;
	}



	public static void subscribeGroup(String group, Context context) {
		
		DBHelper dbhelper = new DBHelper(context);
		SQLiteDatabase dbwrite = dbhelper.getWritableDatabase();
		
		ContentValues cv = new ContentValues();
		cv.put("profile_id", 1);
		cv.put("name", group);
		cv.put("lastFetched", -1);
		cv.put("unread_count", -1);
		dbwrite.insert("subscribed_groups", null, cv);
		
		dbwrite.close(); dbhelper.close();
	}



	public static boolean isAuthorFavorite(String author, Context context) {
		
		DBHelper dbhelper = new DBHelper(context);
		SQLiteDatabase dbread = dbhelper.getReadableDatabase();
		
		Cursor c = dbread.rawQuery("SELECT _id FROM favorite_users WHERE name=" + esc(author), null);

		boolean res = (c.getCount() > 0);

		c.close(); dbread.close(); dbhelper.close();
		return res;
	}



	public static void setAuthorFavorite(boolean isFavorite, boolean mustBeFavorite, String author, Context context) {
		
		if (isFavorite && mustBeFavorite || !isFavorite && !mustBeFavorite) 
			return;
		
		DBHelper dbhelper = new DBHelper(context);
		SQLiteDatabase dbwrite = dbhelper.getWritableDatabase();
		
		if (isFavorite && !mustBeFavorite) {
			// Remove from the table
			dbwrite.execSQL("DELETE FROM favorite_users WHERE name=" + esc(author));
		} 
		else if (!isFavorite && mustBeFavorite) {
			// Insert into the table
			ContentValues cv = new ContentValues();
			cv.put("name", author);
			dbwrite.insert("favorite_users", null, cv);
		}
		dbwrite.close(); dbhelper.close();
	}


	public static int getGroupUnreadCount(String group, Context context) {
		int groupid = getGroupIdFromName(group, context);
		
		DBHelper dbhelper = new DBHelper(context);
		SQLiteDatabase dbread = dbhelper.getReadableDatabase();
		
		Cursor c = dbread.rawQuery("SELECT _id FROM headers WHERE read=0 AND subscribed_group_id="+groupid, null);
		int result = c.getCount();
		
		c.close(); dbread.close(); dbhelper.close();
		return result;
	}


	public static void setMessageCatched(long id, boolean catched, Context context) {
		
		DBHelper db = new DBHelper(context);
		SQLiteDatabase dbwrite = db.getReadableDatabase();
		
		int numbool;
		
		if (catched) 
			numbool = 1;
		else
			numbool = 0;
		
		dbwrite.execSQL("UPDATE headers SET catched=" + numbool + " WHERE _id=" + id);
		dbwrite.close(); db.close();
	}
	
	
	public static Hashtable<String, Object> getHeaderRecordCatchedData(String group, long serverMsgNum, Context context) {
		int groupid = getGroupIdFromName(group, context);
		
		Hashtable<String, Object> result = null;
		
		DBHelper db = new DBHelper(context);
		SQLiteDatabase dbread = db.getReadableDatabase();
		
		Cursor c = dbread.rawQuery("SELECT _id, server_article_id, catched FROM headers WHERE subscribed_group_id=" +
				                   groupid + " AND server_article_number=" + serverMsgNum, null);
		
		if (c.getCount() == 1) {
			c.moveToFirst();
			
			result = new Hashtable<String, Object>(3);
			result.put("id", c.getInt(0));
			result.put("server_article_id", c.getString(1));
			if (c.getInt(2) == 1) 
				result.put("catched", true);
			else 
				result.put("catched", false);
		}
		
		
		c.close(); dbread.close(); db.close();
		
		return result;
	}


	// Delete OLD or ALL (expireAll=true) read messages from the cache and from the DB
	public static void expireReadMessages(Context context, boolean expireAll, long expireTime) {
		
		DBHelper db = new DBHelper(context);
		SQLiteDatabase dbwrite = db.getWritableDatabase();
		
		// Get all the expired messages so we can delete bodies and attachments
		long currentTime = System.currentTimeMillis();
		String q = null;
		
		if (expireAll) {
			q = "SELECT _id, subscribed_group_id, has_attachments, attachments_fnames " + 
         			"FROM headers " + 
         			"WHERE read=1 AND catched=1";
		} 
		else {
			q = "SELECT _id, subscribed_group_id, has_attachments, attachments_fnames " + 
		                 	"FROM headers " + 
		                 	"WHERE read=1 AND catched=1 AND read_unixdate < " + currentTime + " - " + expireTime;
		}
		
		Cursor c = dbwrite.rawQuery(q, null);
		
		int count = c.getCount();
		c.moveToFirst();
		String groupname;
		
		for (int i=0; i < count; i++) {
			
			groupname = getGroupNameFromId(c.getInt(1) /*subscribed_group_id*/, context);
			FSUtils.deleteCacheMessage(c.getInt(0)/* _id */, groupname);
			
			if (c.getInt(2)/*has_attach*/ == 1) {
				FSUtils.deleteAttachments(c.getString(3) /*attachments_fnames*/, groupname);
			}
			
			c.moveToNext();
		}
		
		if (expireAll)
			q = "DELETE FROM headers WHERE read=1";
		else
			q = "DELETE FROM headers WHERE read=1 AND read_unixdate < " + currentTime + " - " + expireTime;
		dbwrite.execSQL(q);
		c.close(); dbwrite.close(); db.close();
	}

	

	public static long insertOfflineSentPost(Context context) {
		
		DBHelper db = new DBHelper(context);
		SQLiteDatabase dbwrite = db.getWritableDatabase();
		
		ContentValues cv = new ContentValues();
		cv.put("foo", 1);
		long ret = dbwrite.insert("offline_sent_posts", null, cv);
		
		dbwrite.close(); db.close();
		
		return ret;
	}


	// ================================================================================
	// Return a vector with the servernumbers of the unread and uncatched articles so 
	// the ServerManager can download the full messages when the user selects "sync" in
	// offline mode
	// ================================================================================
	
	public static Vector<Long> getUnreadNoncatchedArticleList(String group, Context context) {
		int groupid = getGroupIdFromName(group, context);

		Vector<Long> artList = null;
		DBHelper db = new DBHelper(context);
		SQLiteDatabase dbread = db.getReadableDatabase();
		
		String q = "SELECT server_article_number FROM headers WHERE subscribed_group_id=" + groupid + " AND read=0 AND catched=0";
		Cursor c = dbread.rawQuery(q , null);
		
		int count = c.getCount();
		artList = new Vector<Long>(count);
		
		c.moveToFirst();
		
		for (int i=0; i < count; i++) { 
			artList.add(c.getLong(0));
			c.moveToNext();
		}
		
		c.close(); dbread.close(); db.close();
		return artList;
	}


	// This return nulls if the articleInfo with that servernumber and group is not in the database, 
	// or the _id+mdgId if it already was
	public static Vector<Object> isHeaderInDatabase(Long number, String group, Context context) {
		int groupid = getGroupIdFromName(group, context);
		
		DBHelper db = new DBHelper(context);
		SQLiteDatabase dbread = db.getReadableDatabase();
		Vector<Object> retVal = null;
		
		String q = "SELECT _id, server_article_id FROM headers WHERE subscribed_group_id=" + groupid + " AND server_article_number=" + number;
		Cursor c = dbread.rawQuery(q, null);
		int count = c.getCount();
		
		if (count > 0) {
			c.moveToFirst();
			retVal = new Vector<Object>(2);
			retVal.add(c.getLong(0));
			retVal.add(c.getString(1));
		}
		
		c.close(); dbread.close(); db.close();
		
		return retVal;
	}


	public static Vector<Long> getPendingOutgoingMessageIds(Context context) {
		
		Vector<Long> retVal = null;
		DBHelper db = new DBHelper(context);
		SQLiteDatabase dbread = db.getReadableDatabase();
		
		Cursor c = dbread.rawQuery("SELECT _id FROM offline_sent_posts", null);
		int count = c.getCount();
		
		if (count == 0) {
			retVal = new Vector<Long>(0);
		} else {
			retVal = new Vector<Long>(count);
			c.moveToFirst();
			
			for (int i=0; i < count; i++) {
				retVal.add(c.getLong(0));
				c.moveToNext();
			}
		}
			
		c.close(); dbread.close(); db.close();
		return retVal;
	}

	


	public static void logSentMessage(String msgId, String group, Context context) {
		int groupid = getGroupIdFromName(group, context);
		
		DBHelper db = new DBHelper(context);
		SQLiteDatabase dbwrite = db.getWritableDatabase();
		
		/* Check first that the number of logged messages for this group is not greater than the 
		* limit impossed per group, because if it's greater we must delete number-limit older logs
		* until the table only has the limit. This is done this way because on the MessageList a set
		* is built with the post messages from that group, and then every loaded message's msgId is checked 
		* to see if it's in the set (to check for replies to our messages), so allowing it to grow too much
		* could make the MessageView slow
		*/
		
		Cursor c = dbwrite.rawQuery("SELECT _id FROM sent_posts_log WHERE subscribed_group_id=" + groupid + " ORDER BY _id", null);
		int count = c.getCount();
		int toKill = count - UsenetConstants.SENT_POSTS_LOG_LIMIT_PER_GROUP;
		int kennyId;
		
		if (toKill > 0) {
			// Delete some more than needed so we don't have to do this on every post sent
			toKill += UsenetConstants.SENT_POST_KILL_ADITIONAL;
			c.moveToFirst();
			
			for (int i=0; i < toKill; i++) {
				kennyId = c.getInt(0);
				dbwrite.execSQL("DELETE FROM sent_posts_log WHERE _id="+kennyId);
				c.moveToNext();
			}
		}
		c.close(); 
		
		// Now we have room for sure, insert the log
		ContentValues cv = new ContentValues(2);
		cv.put("server_article_id", msgId);
		cv.put("subscribed_group_id", groupid);
		dbwrite.insert("sent_posts_log", null, cv);
		
		dbwrite.close(); db.close();
	}
	
	
	public static HashSet<String> getGroupSentMessagesSet(String group, Context context) {
		int groupid = getGroupIdFromName(group, context);
		
		HashSet<String> retVal = null;
		DBHelper db            = new DBHelper(context);
		SQLiteDatabase dbread  = db.getReadableDatabase();
		
		String q = "SELECT server_article_id FROM sent_posts_log WHERE subscribed_group_id="+groupid;
		Cursor c = dbread.rawQuery(q, null);
		int count = c.getCount();
		
		retVal = new HashSet<String>(count);
		c.moveToFirst();
		
		for (int i=0; i < count; i++) {
			retVal.add(c.getString(0));
			c.moveToNext();
		}
		
		c.close(); dbread.close(); db.close();
		
		return retVal;
	}


	public static boolean groupHasUncatchedMessages(String group, Context context) {
		int groupid = getGroupIdFromName(group, context);

		DBHelper db = new DBHelper(context);
		SQLiteDatabase dbread = db.getReadableDatabase();
		
		String q = "SELECT _id FROM headers WHERE subscribed_group_id=" + groupid + " AND read=0 AND catched=0";
		Cursor c = dbread.rawQuery(q , null);
		
		int count = c.getCount();
		
		c.close(); dbread.close(); db.close();
		
		return (count > 0);
	}


	/*
	 * Receive a vector of  "attachData" hashmaps which is a key-value with data of an attachment, update the database record
	 * for the header with the information of the attachment/s filename/s so both are related
	 */
	public static void updateHeaderRecordAttachments(int headerId, Vector<HashMap<String, String>> attachsVector, Context context) {

			if (attachsVector == null || attachsVector.size() == 0)
				return;
			
			StringBuffer strbu = new StringBuffer();
			HashMap<String, String> attachData = null;
			int len = attachsVector.size();
			
			for (int i=0; i<len; i++) {
				attachData = attachsVector.get(i);
				strbu.append(attachData.get("md5"));
				if (i != len-1)
					strbu.append(";");
			}
			
			DBHelper db = new DBHelper(context);
	    	SQLiteDatabase dbwriter = db.getWritableDatabase();
	    	String q = "UPDATE headers SET has_attachments=1, attachments_fnames=" + esc(strbu.toString()) +  " WHERE _id="+ headerId;
	    	dbwriter.execSQL(q);
	    	dbwriter.close(); db.close();
	}
}
