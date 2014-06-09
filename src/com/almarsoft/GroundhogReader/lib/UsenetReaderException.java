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

public class UsenetReaderException extends Exception {
	
	private static final long serialVersionUID = 3840358264033319232L;
	String mMessage;
	
	public UsenetReaderException(String message) {
		super();
		mMessage = message;
		
	}
	
	public String getUsenetMessage() {
		return mMessage;
	}
	
	@Override
	public String toString() {
		return mMessage;
	}
	
	@Override
	public String getMessage() {
		return mMessage;
	}

}
