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
import java.io.Reader;
import java.util.Vector;

public class MergeReader extends Reader{

	private Reader currentReader = null;
	private Vector<Reader> readers = null;
	private int currentPos = 0;
	
	public MergeReader(Vector<Reader> readers) {
		this.readers = readers;
		reset();
	}
	
	@Override
	public void close() throws IOException {
		for (Reader r : readers) {
			r.close();
		}
	}

	@Override
	public int read(char[] buf, int offset, int count) throws IOException {
		int charsread = 0;
		
		charsread = currentReader.read(buf, offset, count);
		
		// End of this reader?
		if (charsread == -1) {
			// Are there more readers?
			if (readers.size() > currentPos + 1) {
				currentPos++;
				currentReader = readers.elementAt(currentPos);
				charsread = currentReader.read(buf, offset, count);
			}
		}
		
		return charsread;
	}
	
	@Override
	public void reset() {
		currentReader = readers.firstElement();
		currentPos = 0;
	}
}
