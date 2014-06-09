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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.HashMap;

import org.apache.commons.codec.digest.DigestUtils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class FSUtils {
	
	public static String loadStringFromDiskFile(String fullPath, boolean existenceChecked) 
							throws UsenetReaderException, IOException {

		String ret = null;
		File f = new File(fullPath);
		
		if (!existenceChecked && !f.exists()) {
			throw new UsenetReaderException("File could not be found in " + fullPath);
		}
		
		char[] buff = new char[(int)f.length()];
		BufferedReader in = null;
		
		try {
			FileReader freader = new FileReader(fullPath);
			in = new BufferedReader(freader);
			in.read(buff);
			ret = new String(buff);
		} finally {
			if (in != null) 
				in.close();
		}
		
		return ret;
	}
	
	public static FileReader getReaderFromDiskFile(String fullPath, boolean existenceChecked) 
	throws UsenetReaderException, IOException {

		File f = new File(fullPath);

		if (!existenceChecked && !f.exists()) {
			throw new UsenetReaderException("File could not be found in " + fullPath);
		}

		return new FileReader(fullPath);		
	}

	
	
	public static void writeStringToDiskFile(String data, String fullPath, String fileName) throws IOException {
		
		File outDir = new File(fullPath);
		
		if (!outDir.exists()) 
			outDir.mkdirs();
		
		BufferedWriter out = null;
		
		try {
			FileWriter writer = new FileWriter(fullPath + fileName);
			out = new BufferedWriter(writer);
			out.write(data);
			out.flush();
		} finally {
			if (out != null) 
				out.close();
		}
	}	
	
	public static void writeReaderToDiskFile(Reader data, String fullPath, String fileName) throws IOException {
		
		File outDir = new File(fullPath);
		
		if (!outDir.exists()) 
			outDir.mkdirs();
		
		BufferedWriter out = null;
		
		try {
			FileWriter writer = new FileWriter(fullPath + fileName);
			out = new BufferedWriter(writer);
			
			String readData = null;
			char[] buf = new char[1024];
			int numRead = 0;
			
			while((numRead = data.read(buf)) != -1) {
				readData = String.valueOf(buf, 0, numRead);
				out.write(readData);
				buf = new char[1024];
			}
			
			//out.write(data);
			out.flush();
		} finally {
			if (out != null) 
				out.close();
		}
	}

	// Currently used only for saving attachments
	public static long writeInputStreamAndGetSize(String directory, String filename, InputStream is) 
	throws IOException {
		
		File outDir = new File(directory);
		if (!outDir.exists()) outDir.mkdirs();
		
		File outFile = new File(directory, filename);

		DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outFile)));
		
		byte buf[]=new byte[1024];
		int len;
		while((len=is.read(buf))>0) 
			dos.write(buf,0,len);
			
		dos.close();
		
		return outFile.length();
		
	}
	
	public static long writeByteArrayToDiskFileAndGetSize(byte[] data, String directory, String fileName) throws IOException {
		
		File outDir = new File(directory);
		if (!outDir.exists()) 
			outDir.mkdirs();

		File outFile = new File(directory, fileName);
		
		BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(outFile));
		
		try {
			fos.write(data);
		} finally {
			fos.close();
		}
		
		return outFile.length();
	}
	
	
	public static void deleteOfflineSentPost(long id, Context context) {
		
		DBHelper db = new DBHelper(context);
		SQLiteDatabase dbwrite = db.getReadableDatabase();
		
		dbwrite.execSQL("DELETE FROM offline_sent_posts WHERE _id="+id);
		dbwrite.close(); db.close();
	}
	
	
	public static void deleteCacheMessage(long msgid, String groupname) {
		
		String basePath = UsenetConstants.EXTERNALSTORAGE + "/" + UsenetConstants.APPNAME + 
		                  "/offlinecache/groups/" + groupname;
		
		new File(basePath + "/header/" + msgid).delete();
		new File(basePath + "/body/" + msgid).delete();
	}
	
	
	
	public static boolean deleteDirectory(String directory) {
		
		Process process;
		try {
			process = Runtime.getRuntime().exec("rm -r " + directory);
			process.waitFor();
			if (process.exitValue() == 0)
				return true;
		} catch (IOException e) {
			Log.d("Groundhog", "IOException in rm -r " + directory);
			e.printStackTrace();
		} catch (InterruptedException e) {
			Log.d("Groundhog", "InterruptedException in rm -r " + directory);
			e.printStackTrace();
		}
		
		return false;
	    
	}

	// SLOW, use only for attachments or filenames we're not controlling
	public static String sanitizeFileName(String name) {
		name = name.replace(':', '_');
		name = name.replace('*', '_');
		name = name.replace('/', '_');
		name = name.replace('\\', '_');
		name = name.replace('<', '[');
		name = name.replace('>', ']');
		name = name.replace('?', '_');
		name = name.replace('|', '_');
		name = name.replace(' ', '_');
		name = name.replace(',', '_');
		
		return name;
	}
	

	
	// =================================================
	// Save an attachment to the sdcard downloads folder
	// =================================================
	
	public static String saveAttachment(String md5, String group, String name) 
	throws IOException {
		
		
		String outDir = UsenetConstants.EXTERNALSTORAGE + "/downloads";
		File dirOutDir = new File(outDir);
		if (!dirOutDir.exists()) 
			dirOutDir.mkdirs();
		
		name = sanitizeFileName(name);
		
		String origFilePath = UsenetConstants.EXTERNALSTORAGE + "/" + UsenetConstants.APPNAME + "/" + 
		                      UsenetConstants.ATTACHMENTSDIR  + "/" + group + "/" + md5;
		String destFilePath = outDir + "/" + name;
		
		FileInputStream in = new FileInputStream(origFilePath);
		FileOutputStream out = new FileOutputStream(destFilePath);
		
        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
		
		return destFilePath;
	}
	

	// =====================================================================================
	// Decode a string un uuencoding and save it to disk, returning an attachData
	// =====================================================================================
	
	public static HashMap<String, String> saveUUencodedAttachment(String uuencodedData, String filename, String group) 
	throws IOException, UsenetReaderException {
		
		UUDecoder uudecoder = new UUDecoder();
		
		String directory = UsenetConstants.EXTERNALSTORAGE + "/" + UsenetConstants.APPNAME + "/" + 
		                   UsenetConstants.ATTACHMENTSDIR  + "/" + group + "/";
		
		File dirFile = new File(directory);
		if (!dirFile.exists())
			dirFile.mkdirs();
		
		writeStringToDiskFile(uuencodedData, directory, "uutmp");
		byte[] bindata = uudecoder.decode(directory + "uutmp");
		uuencodedData = null;
		
		String md5 = DigestUtils.md5Hex(bindata);
		String ext = filename.substring(filename.lastIndexOf('.')+1, filename.length());
		md5 = md5 + "." + ext;
		
		long fsize = writeByteArrayToDiskFileAndGetSize(bindata, directory, md5);
		String sizeStr = new Long(fsize).toString();
		
		HashMap<String, String> retPart = new HashMap<String, String>();
		retPart.put("name", sanitizeFileName(filename));
		retPart.put("size", sizeStr);
		retPart.put("md5", md5);
		retPart.put("type", GetTypeFromExtension(ext));
		return retPart;
	}


	private static String GetTypeFromExtension(String ext) {
		if (ext.equalsIgnoreCase("jpg"))
			return "image/*";
		else if (ext.equalsIgnoreCase("png"))
			return "image/*";
		else if (ext.equalsIgnoreCase("gif"))
			return "image/*";
		else if (ext.equalsIgnoreCase("bmp"))
			return "image/*";
		else if (ext.equalsIgnoreCase("jpeg"))
			return "image/*";
		else if (ext.equalsIgnoreCase("tiff"))
			return "image/*";
		
		
		else if (ext.equalsIgnoreCase("mp3"))
			return "audio/*";
		else if (ext.equalsIgnoreCase("wav"))
			return "audio/*";
		else if (ext.equalsIgnoreCase("ogg"))
			return "audio/*";
		else if (ext.equalsIgnoreCase("mp4"))
			return "audio/*";
		else if (ext.equalsIgnoreCase("ac3"))
			return "audio/*";
		else if (ext.equalsIgnoreCase("amr"))
			return "audio/*";
		else if (ext.equalsIgnoreCase("mpa"))
			return "audio/*";
		else if (ext.equalsIgnoreCase("mp4"))
			return "audio/*";
		
		else if (ext.equalsIgnoreCase("wmv"))
			return "video/*";
		else if (ext.equalsIgnoreCase("avi"))
			return "video/*";
		else if (ext.equalsIgnoreCase("divx"))
			return "video/*";
		else if (ext.equalsIgnoreCase("3gp"))
			return "video/*";
		else if (ext.equalsIgnoreCase("3gpp"))
			return "video/*";
		else if (ext.equalsIgnoreCase("mpg"))
			return "video/*";
		else if (ext.equalsIgnoreCase("mpeg"))
			return "video/*";
		else if (ext.equalsIgnoreCase("mov"))
			return "video/*";
		
		else
			return "application/*";
	}


	public static void deleteAttachments(String attachments, String group) {
		
		String basePath    = UsenetConstants.EXTERNALSTORAGE + "/" + UsenetConstants.APPNAME + "/" + 
		                     UsenetConstants.ATTACHMENTSDIR  + "/" + group + "/" ;
		
		String[] fNames = attachments.split(";");
		
		for (String fname : fNames)
			new File(basePath + fname).delete();
	}
}
