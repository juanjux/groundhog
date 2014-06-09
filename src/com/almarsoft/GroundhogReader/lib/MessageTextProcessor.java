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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.james.mime4j.codec.DecoderUtil;
import org.apache.james.mime4j.field.address.Mailbox;
import org.apache.james.mime4j.field.address.MailboxList;
import org.apache.james.mime4j.message.BinaryBody;
import org.apache.james.mime4j.message.Body;
import org.apache.james.mime4j.message.BodyPart;
import org.apache.james.mime4j.message.Entity;
import org.apache.james.mime4j.message.Header;
import org.apache.james.mime4j.message.Message;
import org.apache.james.mime4j.message.Multipart;
import org.apache.james.mime4j.message.TextBody;
import org.apache.james.mime4j.parser.Field;
import org.apache.james.mime4j.parser.MimeEntityConfig;

import android.util.Log;

public class MessageTextProcessor {

	public static String readerToString(Reader reader) throws IOException {
		BufferedReader bufReader = new BufferedReader(reader);
		StringBuilder sb = new StringBuilder();
		String temp = bufReader.readLine();
		
		while (temp != null) {
			sb.append(temp);
			sb.append("\n");
			temp = bufReader.readLine();
		}

		return sb.toString();
	}
	
	// ======================================================================================================
	// If the encoding is declared, call to message.getSubject() which takes care of transcoding
	// If not, we encode if on the header or user declared encoding
	// Note that this will probably fail if the string contains a =? that doesnt declare a charset... oh well
	// ======================================================================================================
	public static String decodeSubject(Field subjectField, String charset, Message message) 
	throws UnsupportedEncodingException {
		String rawStr = new String(subjectField.getRaw().toByteArray());
		if (rawStr.indexOf("=?") != -1) {
			return message.getSubject();
		}
		return new String(subjectField.getRaw().toByteArray(), charset).replaceFirst("Subject: ", "");
	}
	
	// ======================================================================================================
	// If the encoding is declared, call to message.getFrom() which takes care of transcoding
	// If not, we encode if on the header or user declared encoding
	// Note that this will probably fail if the string contains a =? that doesnt declare a charset... oh well
	// ======================================================================================================
	
	public static String decodeFrom(Field fromField, String charset, Message message) {
		
		String rawStr = new String(fromField.getRaw().toByteArray());
		if (rawStr.indexOf("=?") != -1) {
			MailboxList authorList = message.getFrom();
			Mailbox author;
			
			if ((authorList != null) && (author = authorList.get(0)) != null) 
				return author.getName() + "<" + author.getAddress() + ">";
			else {
				// Parse error on mime4j; some people use emails like "bla@@@bla.com", some stuuuuuupid servers accept them, and mime4j
				// gives (rightly) a parse error
				return DecoderUtil.decodeEncodedWords(fromField.getBody().trim());
			}
		}

		try {
			return new String(fromField.getRaw().toByteArray(), charset).replaceFirst("From: ", "");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return "Unknown";
		}
	}
	
	
	public static String decodeHeaderInArticleInfo(String originalHeader, String charset) {
		
		if (originalHeader.indexOf("=?") != -1) {
			return DecoderUtil.decodeEncodedWords(originalHeader);
		}
		try{
			return new String(originalHeader.getBytes("ISO8859-1"), charset);
		} catch (UnsupportedEncodingException e) {
			return "Unknown";
		}
		
	}
	
    // =============================================================================
    // Remove all \n except the ones that are after a point. We do this
    // because the screen is probably going to be less than 70 chars anyway, so 
    // we let the GUI reflow the new lines, which makes the text looks "justified".
	// (we'll split the lines at 70 again before posting, this is only for reading)
    // =============================================================================
    public static String sanitizeLineBreaks(String inputText) {
    	
    	if (inputText == null) 
    		return null;
    	
    	StringBuilder newBody = new StringBuilder(inputText.length());
    	char charcur, charprev, charnext, selectedChar;
    	StringBuilder previousLine = new StringBuilder();
    	int inputTextLen = inputText.length();
    	boolean hasNext;
    	
    	for (int i = 0; i < inputTextLen; i++) {
    		
    		previousLine.append(inputText.charAt(i));
    		selectedChar = inputText.charAt(i);
    		
    		if (i > 1) {
    			charcur  = inputText.charAt(i);
    			charprev = inputText.charAt(i-1);
    			
    			if (inputText.length() > i+1) {
    				charnext = inputText.charAt(i+1);
    				hasNext = true;
    			}
    			else {
    				charnext = charcur;
    				hasNext = false;
    			}
    			

    			if (charcur == '\n') {
    				
					//if( previousLine.charAt(0) != '>'
						if((charprev != '.')
						&& (hasNext && charnext != '>')
					    && (charprev != '!')
					    && (charprev != ':')
						&& (charprev != '?')
						&& (!inputText.substring(i-2, i).equals("> "))
						&& (charprev != '>')
						&& (charprev != '\n') // Don't remove newlines when there are more than one together (formatting newlines)
						&& (hasNext && charnext != '\n')) {
						
						selectedChar = ' ';
					}
					
					if (inputText.substring(i-2, i).equals("--")) {
						selectedChar = '\n';
					}
					
					previousLine = new StringBuilder();
    			}
    		}
			newBody.append(selectedChar);
    	}
    	
    	return newBody.toString();
    }
	
    
    private static String getBlockQuotes(int level, boolean isopen) {
    	StringBuilder res = new StringBuilder();
    	String tagopen = "\n<blockquote style=\"margin: 0pt 0pt 0pt 0.2ex; border-left: 2px solid #00008B; padding-left: 0.5ex;\">\n";
    	String tagclose = "\n</blockquote>\n";
    	
    	for (int i=0; i < level; i++) {
    		if (isopen) res.append(tagopen);
    		else res.append(tagclose);
    	}
    	
    	return res.toString();
    }
    
    /**
     * XXX: Esto es un horror, reajustar el churro de ifs
     */
    
    public static String prepareHTML(String inputText, boolean justify) {
    	
    	StringBuilder html = new StringBuilder(inputText.length());

    	String[] lines = inputText.split("\n");
    	String quoteColor;
    	int quoteLevel = 0;
    	int lastQuoteLevel;
    	boolean lastWasP = false;
    	
    	for (String line : lines) {
    		
    		lastQuoteLevel = quoteLevel;
    		quoteLevel = getQuotingLevel(line);
    		
    		if (quoteLevel > 0) {
    			// We're in a quote
    			
    			quoteColor = getQuotingColor(quoteLevel);
    			
        		// Remove empty quoting lines like ">\n" and ">> \n"
				if (lastQuoteLevel > 0) 
					html.append("</I>\n");
				
    			if (quoteLevel != lastQuoteLevel) {
    				html.append("</P>\n");
    				html.append(getBlockQuotes(lastQuoteLevel, false));
    				html.append(getBlockQuotes(quoteLevel, true));
    				html.append("<P style=\"BACKGROUND-COLOR: ");
    				html.append(quoteColor);
    				html.append("\">\n");
    			}
    			
        		if (isEmptyQuoting(line)) {
        			html.append("<BR/>");
        			continue;
        		}
        		
        		if (quoteLevel > 0)
        			html.append("<I>");
    			
        		// XXX Revisar (con espacios en medio y tal)
    			line       = removeStartingQuotes(line);

    		}
    		else { 
    			if (lastQuoteLevel > 0) {
    				// We're not in quote and last was quote, close the <i>
    				html.append("</I></P>");
    				html.append(getBlockQuotes(lastQuoteLevel, false));
    			}
    			html.append(getBlockQuotes(lastQuoteLevel, false));

    		}
    		
			if (line.length() == 0) {
				html.append("<P>\n");
				lastWasP = true;
			}
			else {
				if (!lastWasP) 
					html.append("<BR/>\n");
				lastWasP = false;
			}
    		
    		
    		line = escapeHtmlWithLinks(line);
    		line = escapeInitialSpaces(line);
    		html.append(line);
    	}
    	
    	html.append("\n</BODY> </HTML>\n");
    	return html.toString();
    }
    
    /*
     * Replace initial spaces with &nbsp; (for code indentation, poetry, etc)
     */
    private static String escapeInitialSpaces(String line) {
    	
    	StringBuilder newline = new StringBuilder(line.length());
    	char c;
    	boolean atStart = true;
    	String append = null;
    	
    	for (int i = 0; i < line.length(); i++) {
    		c = line.charAt(i);
    		append = null;
    		
    		if (atStart) {
    			if (c == ' ') 
    				append = "&nbsp;";
    			else
    				atStart = false;
    		}
    		if (append == null)
    			append = Character.toString(c);
    		
    		newline.append(append);
    	}
		return newline.toString();
	}

    
	// =================================================================================================
    // Escape a text converting it to HTML. It also convert urls to links; this conversion is not
    // the most advanced: it only works when the link is entirely contained within the same line and it
    // only converts the first link in the line. But it covers 95% of cases found on Usenet.
    // =================================================================================================
    private static String escapeHtmlWithLinks(String line) {
    	
    	StringBuffer buf = null;
    	
    	// Shortcut for most cases with line not having a link    	
    	int idx = line.toLowerCase().indexOf("http://");
    	
    	if (idx == -1) {
    		return StringEscapeUtils.escapeHtml(line);
    	} 
    	else {
    		buf = new StringBuffer();
    		buf.append(StringEscapeUtils.escapeHtml(line.substring(0, idx)));
    		
    		char c;
    		String endLine;
    		StringBuffer urlBuf = new StringBuffer();
    		int lineLen = line.length();
    		
    		for(;idx < lineLen; idx++) {
    			c = line.charAt(idx);
    			if (Character.isSpace(c)) {
    				break;
    			}
    			urlBuf.append(c);
    		}
    		
    		if (urlBuf.length() > 0) {
    			buf.append("<A HREF=\""); buf.append(urlBuf); buf.append("\" >"); buf.append(urlBuf); buf.append("</A> ");
    		}
    		
    		endLine = line.substring(idx);
    		if (endLine.length() > 0)
    			buf.append(StringEscapeUtils.escapeHtml(line.substring(idx)));
    	}
    	
    	return buf.toString();
    }
	
	
    private static boolean isEmptyQuoting(String line) {
    	line = line.trim();
    	boolean emptyQuote = true;
    	int lineLen = line.length();
    	
    	for (int i=0; i<lineLen; i++) {
    		if (line.charAt(i) != '>') {
    			emptyQuote = false;
    			break;
    		}
    	}
		return emptyQuote;
	}

    private static int getQuotingLevel(String line) {
    	int count = 0;
    	int lineLen = line.length();
    	
    	for (int i = 0; i<lineLen;i++) {
    		if      (line.charAt(i) == ' ') continue; 
    		else if (line.charAt(i) != '>') break; 
    		count++;
    	}
    	
    	return count;
    }
    
    /*
     * Remove the quoting chars at the start of the lines
     * Works for:
     * ">>>"
     * "> > >"
     * ">> >> > >"
     * etc...
     */
    private static String removeStartingQuotes(String line) {
    	int idx = 0;
    	boolean finished = false;
    	try{ 
    		while (!finished && idx < line.length()) {
    			
    			if (line.charAt(idx) == '>') {
    				idx++;
    				continue;
    			}
    			else if (line.charAt(idx) == ' ' && idx+1 < line.length() && line.charAt(idx+1) == '>') {
    				idx+=2;
    				continue;
    			}
    			else 
    				finished = true;
    		}
    	} catch (IndexOutOfBoundsException e) {
    		if (idx == 0) return line;
    		else idx--;
    	}
    	return line.substring(idx);
    }

	private static String getQuotingColor(int level) {
    	
    	switch(level) {
    		case 0: return "white";
    		case 1: return "palegreen";
    		case 2: return "lightblue";
    		case 3: return "lightcoral";
    	}
    	
    	// More than 3
    	return "violet";
	}
    	
	// =======================================================
	// Converts a header as String into a mime4j Header object
	// =======================================================
	
	public static Header strToHeader(String strHeader) 
	throws IOException {
		
		StringReader strread = new StringReader(strHeader);
		ReaderInputStream ris = new ReaderInputStream(strread);
		MimeEntityConfig mimeConfig = new MimeEntityConfig();
		mimeConfig.setMaxLineLen(-1);
		Header header = new Header(ris);
		
		return header;
	}	
	
	// ============================================================================================
	// Replace in the configured quote header template the replacing tokens with the real values.
	// This doesn't validate for null values so make sure the arguments are not null before calling
	// this function
	// ============================================================================================
	private static String parseQuoteHeaderTemplate(String template, String from, String date) {
		
		String retVal = template;
		retVal = retVal.replace("[user]", from);
		retVal = retVal.replace("[date]", date);
		
		return retVal;
	}
	


	// ======================================================
	// Takes the original body and adds the quotes
	// ======================================================
	public static String quoteBody(String origText, String quoteheader, String from, String date) {
		
		String[] tmpLines = origText.split("\n");
		ArrayList<String> list = new ArrayList<String>(tmpLines.length+2);
		
		// Add the attribution
		
		if (from != null && date != null && quoteheader != null) {
			//list.add("On " + date + " " + from + " wrote:\n");
			list.add(parseQuoteHeaderTemplate(quoteheader, from, date));
		} 
		
		boolean startWhiteSpace = true;
		
		for (String currentLine : tmpLines) {
			
			if (currentLine.trim().length() > 0) { 
				list.add("> " + currentLine);
				
				if (startWhiteSpace) 
					startWhiteSpace = false;
			}
			else 
				if (!startWhiteSpace) 
					list.add("\n");
		}
		
		// Leave some space for the reply
		list.add("\n");
		
		// Now make a string again
		StringBuffer retBuf = new StringBuffer(tmpLines.length+2);
		int listLen = list.size();
		String line;
		
		for (int i=0; i < listLen; i++) {
			line = list.get(i);
			retBuf.append(line);
			retBuf.append("\n");
		}

		return retBuf.toString().trim();
	}

	
	public static String getHtmlHeader(String charset) {
		StringBuilder html = new StringBuilder();
		html.append("<HTML>\n");
		html.append("<HEAD>\n");
		html.append("<meta http-equiv=\"Content-Type\" content=\"text/html;" + charset + "\">\n");
		html.append("</HEAD>\n");
		html.append("<BODY>\n");		
		return html.toString();
	}


	public static String getCharsetFromHeader(HashMap<String, String> header) 
	{
		String[] tmpContentArr = null;
		String[] contentTypeParts = null;
		String tmpFirstToken;
		
		String charset = "iso-8859-1";
		
		if (header.containsKey("Content-Type")) {
			tmpContentArr = header.get("Content-Type").split(";");
		
			for (String content : tmpContentArr) {
				
				contentTypeParts = content.split("=", 2);
				tmpFirstToken = contentTypeParts[0].trim();
				
				if (contentTypeParts.length > 1 && tmpFirstToken.equalsIgnoreCase("charset")) {
					// Found
					return contentTypeParts[1].replace("\"", "").trim();					
					
				}				
			}
		}		
		return charset;
	}

	
	public static String htmlizeFullHeaders(Message message) {
		
		Header header = message.getHeader();
		StringBuilder html = new StringBuilder();
		
		// Since this is a unsorted HashMap, put some logical order on the first fields		
		if (header.getField("From") != null)
			html.append("<strong>From:</Strong> " + "<i>" + escapeHtmlWithLinks(header.getField("From").getBody()) + "</i> <br/>\n");
		
		if (header.getField("Subject") != null)
			html.append("<strong>Subject:</Strong> " + "<i>" + escapeHtmlWithLinks(message.getSubject()) + "</i><br/>\n");
		
		if (header.getField("Date") != null)
			html.append("<strong>Date:</Strong> " + "<i>" + escapeHtmlWithLinks(header.getField("Date").getBody()) + "</i><br/>\n");
		
		if (header.getField("Newsgroups") != null)
			html.append("<strong>Newsgroups:</Strong> " + "<i>" + escapeHtmlWithLinks(header.getField("Newsgroups").getBody()) + "</i><br/>\n");
		
		if (header.getField("Organization") != null)
			html.append("<strong>Organization:</Strong> " + "<i>" + escapeHtmlWithLinks(header.getField("Organization").getBody()) + "</i><br/>\n");
		
		if (header.getField("Message-ID") != null)
			html.append("<strong>Message-ID:</Strong> " + "<i>" + escapeHtmlWithLinks(message.getMessageId()) + "</i><br/>\n");
		
		if (header.getField("References") != null)
			html.append("<strong>References:</Strong> " + "<i>" + escapeHtmlWithLinks(header.getField("References").getBody()) + "</i><br/>\n");
		
		if (header.getField("Path") != null)
			html.append("<strong>Path:</Strong> " + "<i>" + escapeHtmlWithLinks(header.getField("Path").getBody()) + "</i><br/>\n");
		
		List<Field> fields = header.getFields();
		int fieldsLen = fields.size();
		String fieldName = null;
		
		for (int i=0; i < fieldsLen; i++) {
			fieldName = fields.get(i).getName();
			if (fieldName.equals("From") || fieldName.equals("Subject") || fieldName.equals("Date") || fieldName.equals("Newsgroups")
				|| fieldName.equals("Message-ID") || fieldName.equals("References") || fieldName.equals("Organization") || fieldName.equals("Path"))
					continue;
			html.append("<strong>" + fieldName + ":" + "</strong>" + " <i>" + escapeHtmlWithLinks(fields.get(i).getBody()) + "</i><br/>\n ");
		}
		html.append("<br/><br/>");		
		return html.toString();
	}


	public static Vector<HashMap<String, String>> saveUUEncodedAttachments(BufferedReader bodyTextReader, String group) 
	throws IOException { 
		
		Vector<HashMap<String, String>> bodyAttachments = new Vector<HashMap<String, String>>(1);
		String newBody = null;
		Vector<HashMap<String, String>> attachDatas = null;
		
		StringBuilder newBodyBuilder = new StringBuilder();
		StringBuilder attachment     = new StringBuilder();
		
		boolean inAttach = false;
		boolean firstOfTheEnd = false;
		
		String line, sline, filename = null;
		HashMap<String, String> attachData = null;
		
		attachDatas = new Vector<HashMap<String, String>>();
		
		while ((line = bodyTextReader.readLine()) != null) {
			
			// XXX: Probar a quitar esto (optimizacion)
			sline = line.trim();
			
			if (sline.equals("`")) {
				firstOfTheEnd = true;
				attachment.append(line + "\n");
			}
			
			else if (firstOfTheEnd && inAttach && sline.equals("end")) {
				
				attachment.append(line + "\n");
				if (attachDatas == null)
					attachDatas = new Vector<HashMap<String, String>>();
				
				try {
					attachData = FSUtils.saveUUencodedAttachment(attachment.toString(), filename, group);
					attachDatas.add(attachData);
				} catch (IOException e) {
					e.printStackTrace();
				} catch (UsenetReaderException e) {
					e.printStackTrace();
				}
				attachment = null;
				inAttach = false;
				firstOfTheEnd = false;
			} 
			
			else if (firstOfTheEnd && inAttach && !sline.equals("end")) {
				firstOfTheEnd = false; // False alarm?
			}
			
			// XXX: ESTO NO SOPORTA UUENCODED SIN PERMISOS!!!
			else if (sline.length() >= 11 && sline.substring(0, 6).equals("begin ") 
					  && Character.isDigit(sline.charAt(6))
					  && Character.isDigit(sline.charAt(7))
					  && Character.isDigit(sline.charAt(8))
					  && Character.isWhitespace(sline.charAt(9))
					  && !Character.isWhitespace(sline.charAt(10))) {
				
				filename = sline.substring(10);
				inAttach = true;
				attachment.append(line + "\n");
			}
			
			else if (inAttach) {
				attachment.append(line + "\n");
			}
			
			else {
				newBodyBuilder.append(line + "\n");
			}
							  
		}
		newBody = newBodyBuilder.toString();
		
		// Add the new body as first element
		HashMap<String, String> bodyMap = new HashMap<String, String>(1);
		bodyMap.put("body", newBody);
		bodyAttachments.add(bodyMap);
		
		
		if (attachDatas != null) {
			for (HashMap<String, String> attData : attachDatas) {
				bodyAttachments.insertElementAt(attData, 1);
			}
		}
		
		return bodyAttachments;
	}


	public static String getAttachmentsHtml(Vector<HashMap<String, String>> mimePartsVector) {

		if (mimePartsVector == null || mimePartsVector.size() == 0)
			return "<!-- No attachments -->\n";
		
		String retString = null;

		if (mimePartsVector == null || mimePartsVector.size() == 0) 
			retString = "";
		
		else {
			
			StringBuilder returnHtml = new StringBuilder();
			returnHtml.append("<I>Attachments:</i><BR/>\n");
			returnHtml.append("<hr>");
			returnHtml.append("<table>\n");
			
			for (HashMap<String, String> attachData : mimePartsVector) {
				returnHtml.append("<tr bgcolor=\"#FFFF00\">");
				returnHtml.append("<td>\n");
				returnHtml.append("<A HREF=\"attachment://fake.com/" + attachData.get("md5") + "\">" + 
						          attachData.get("name") + "</A><BR/>\n");
				returnHtml.append("</td>\n");
				
				returnHtml.append("<td>\n");
				returnHtml.append(attachData.get("type"));
				returnHtml.append("</td>\n");
				
				returnHtml.append("<td>\n");
				returnHtml.append(new Integer(attachData.get("size"))/1024);
				returnHtml.append(" KB");
				returnHtml.append("</td>\n");
				
				returnHtml.append("</tr>\n");
			}
			
			returnHtml.append("</table>\n");
			returnHtml.append("<hr>");
			retString = returnHtml.toString();
		}
		return retString;
	}



	// ====================================================================
	// Short lines to more or less 70 chars, breaking by spaces if possible
	// ====================================================================
	public static String shortenPostLines(String body) {
		StringBuilder builder = new StringBuilder();
		
		String[] lines = body.split("\n");
		int indexSpace;
		
		for (String line : lines) {
			
			if (line.length() > 70) {
				
				while(true) {
					
					indexSpace = line.substring(0, 70).lastIndexOf(' ');
					
					if (indexSpace == -1) {
						if (line.length() < 70) 					
							indexSpace = line.length() - 1;
						else
							indexSpace = 70;
					}
					
  				    builder.append(line.substring(0, indexSpace + 1) + "\n");
					line = line.substring(indexSpace + 1);
					
					if (line.length() < 70) {
						builder.append(line + "\n");
						break;
					}
				}
			}
			
			else {
				builder.append(line + "\n");
			}
		}
		
		return builder.toString();
	}



	// =============================================================================================
	// Split the message into its body and attachments. The attachments are saved to disk/sdcard
	// and only a reference to the filepath (as an md5) is passed.
	// =============================================================================================
	public static Vector<Object> extractBodySaveAttachments(String group, Message message) {
		
		Vector<Object> body_attachs = new Vector<Object>(2);
		TextBody realBody = null;
		
		Body body = message.getBody();
		
		// attachsVector = vector of maps with {content(BinaryBody), name(String), md5(String), size(long)} keys/values
		Vector<HashMap<String, String>> attachsVector = new Vector<HashMap<String, String>>(1);
		
		if (body instanceof Multipart) {
			Multipart multipart = (Multipart) body;
			
			for (BodyPart part : multipart.getBodyParts()) {
				Body partbody = part.getBody();
				
				if (partbody instanceof TextBody) {
					realBody = (TextBody) partbody;
				}
				else if (partbody instanceof BinaryBody) {
					HashMap<String, String> binaryBody = saveBinaryBody((BinaryBody) partbody, group);
					if (binaryBody != null)
						attachsVector.add(binaryBody);
					partbody.dispose();
				}				
			}
		} 
		else if (body instanceof TextBody) {
			realBody = (TextBody) body;
		}
		else if (body instanceof Message) {
		}
		else if (body instanceof BinaryBody) {
			HashMap<String, String> binaryBody = saveBinaryBody((BinaryBody) body, group);
			if (binaryBody != null)
				attachsVector.add(binaryBody);
			body.dispose();
		}
		
		body_attachs.add(realBody);
		body_attachs.add(attachsVector);
		return body_attachs;
	}

	
	// ========================================================
	// Save an attachment to disk/sdcard and return a data hashmap with its information
	// ========================================================
	
	private static HashMap<String, String> saveBinaryBody(BinaryBody body, String group) {
		
		HashMap<String, String> partData = new HashMap<String, String>();
		Entity parent = body.getParent();
		long size = 0;
		
		// I prefer to name the file as an md5 for privacy reasons (not much privacy because it can be 
		// opened from the SDCARD, but protects from potential onlookers on a directory listing)
		String path    = UsenetConstants.EXTERNALSTORAGE + "/" + UsenetConstants.APPNAME + "/" + UsenetConstants.ATTACHMENTSDIR + "/" + group + "/";
		String fname = parent.getFilename();
		if (fname == null)
			return null;
		String ext      = fname.substring(fname.lastIndexOf('.') + 1, fname.length());
		partData.put("md5", DigestUtils.md5Hex(parent.getFilename()) + "." + ext);
		try {
			size = FSUtils.writeInputStreamAndGetSize(path, partData.get("md5"), body.getInputStream());
		} catch (IOException e) {
			Log.w(UsenetConstants.APPNAME, "Unable to save attachment " + partData.get("md5")+ ":" + e.getMessage());
			e.printStackTrace();
		}
		
		partData.put("size", Long.toString(size));
		partData.put("name", fname);
		partData.put("path", path);
		partData.put("type", parent.getMimeType());
		
		return partData;
	}
}



