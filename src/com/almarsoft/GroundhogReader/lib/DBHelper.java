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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
	private static final String DATABASE_NAME = "com.juanjux.usenetreader";
	private static final int DATABASE_VERSION = 5;
	
	public DBHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		
		// Groups we've subscribed to
		db.execSQL("CREATE TABLE subscribed_groups (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
				"                                   profile_id INTEGER, " +
				"                                   name TEXT, " +
				"                                   lastFetched INTEGER," +
				"                                   unread_count INTEGER);");
		
		// Downloaded message headers
		db.execSQL("CREATE TABLE headers (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
											 "subscribed_group_id INTEGER, " +
				                             "reference_list TEXT, " +
				                             "server_article_id TEXT, " +
				                             "date TEXT, " +
				                             "server_article_number INTEGER, " +
				                             "from_header TEXT, " +
				                             "subject_header TEXT, " +
				                             "clean_subject TEXT, " +
				                             "thread_id TEXT, " +
				                             "is_dummy INTEGER, " +
				                             "full_header TEXT, " +
				                             "starred INTEGER DEFAULT 0, " +
				                             "catched INTEGER DEFAULT 0, " +
				                             "has_attachments INTEGER DEFAULT 0, " +
				                             "attachments_fnames TEXT, " + 
		                                     "read_unixdate INTEGER DEFAULT 0, " +   // If 0, unread, else it has the unixdate when it was read (used for expiration)
				                             "read INTEGER DEFAULT 0);");
		
		// Downloaded message bodies
		db.execSQL("CREATE TABLE bodies (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
				                        "header_id INTEGER," +
				                        "bodytext TEXT);");
		
		// User profile, usually asociated with a server and identity
		// FIXME: Add profile support to the Preferences object
		db.execSQL("CREATE TABLE profiles (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
				                          "name TEXT);");
		
		// Threads filtered or starred
		db.execSQL("CREATE TABLE starred_threads (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
					                             "subscribed_group_id INTEGER," +
				                                 "clean_subject TEXT);");
		
		// Threads filtered or starred
		db.execSQL("CREATE TABLE banned_threads (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
				                                 "subscribed_group_id INTEGER," +
				                                 "bandisabled INTEGER," +
				                                 "clean_subject TEXT);");
		
		// Users filtered or starred, by name
		db.execSQL("CREATE TABLE banned_users (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
				                               "name TEXT," +
				                               "bandisabled INTEGER);");
		
		db.execSQL("CREATE TABLE favorite_users (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
				    " name TEXT);");
		
		db.execSQL("CREATE TABLE offline_sent_posts (_id INTEGER PRIMARY KEY AUTOINCREMENT, foo INTEGER);");
		
		// This is used to save the msgid of the messages we send so we can mark the replies easily
		// on the MessageList
		db.execSQL("CREATE TABLE sent_posts_log (_id INTEGER PRIMARY KEY AUTOINCREMENT, " + 
				   "server_article_id TEXT, " +
				   "subscribed_group_id INTEGER);");
	}
	
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int currentVersion) {
		
		if (oldVersion == 4 && currentVersion == 5) {
			db.execSQL("ALTER TABLE headers ADD COLUMN has_attachments INTEGER");
			db.execSQL("ALTER TABLE headers ADD COLUMN attachments_fnames TEXT");
			db.execSQL("ALTER TABLE headers ADD COLUMN read_unixdate INTEGER");
		}
		else {
			db.execSQL("DROP TABLE IF EXISTS subscribed_groups");
			db.execSQL("DROP TABLE IF EXISTS headers");
			db.execSQL("DROP TABLE IF EXISTS bodies");
			db.execSQL("DROP TABLE IF EXISTS profiles");
			db.execSQL("DROP TABLE IF EXISTS starred_threads");
			db.execSQL("DROP TABLE IF EXISTS banned_threads");
			db.execSQL("DROP TABLE IF EXISTS banned_users");
			db.execSQL("DROP TABLE IF EXISTS sent_posts");
			db.execSQL("DROP TABLE IF EXISTS favorite_users");
			db.execSQL("DROP TABLE IF EXISTS tmp_read_unread");
			db.execSQL("DROP TABLE IF EXISTS offline_sent_posts");
			db.execSQL("DROP TABLE IF EXISTS sent_posts_log");
			onCreate(db);
		}
		
		
	}


}
