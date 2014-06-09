/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.mime4j.util;

import org.apache.james.mime4j.util.ByteArrayBuffer;

import junit.framework.TestCase;

/**
 * Unit tests for {@link ByteArrayBuffer}.
 */
public class TestByteArrayBuffer extends TestCase {

    public void testConstructor() throws Exception {
        ByteArrayBuffer buffer = new ByteArrayBuffer(16);
        assertEquals(16, buffer.capacity()); 
        assertEquals(0, buffer.length());
        assertNotNull(buffer.buffer());
        assertEquals(16, buffer.buffer().length);
        try {
            new ByteArrayBuffer(-1);
            fail("IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }
    
    public void testSimpleAppend() throws Exception {
        ByteArrayBuffer buffer = new ByteArrayBuffer(16);
        assertEquals(16, buffer.capacity()); 
        assertEquals(0, buffer.length());
        byte[] b1 = buffer.toByteArray();
        assertNotNull(b1);
        assertEquals(0, b1.length);
        assertTrue(buffer.isEmpty());
        assertFalse(buffer.isFull());
        
        byte[] tmp = new byte[] { 1, 2, 3, 4};
        buffer.append(tmp, 0, tmp.length);
        assertEquals(16, buffer.capacity()); 
        assertEquals(4, buffer.length());
        assertFalse(buffer.isEmpty());
        assertFalse(buffer.isFull());
        
        byte[] b2 = buffer.toByteArray();
        assertNotNull(b2);
        assertEquals(4, b2.length);
        for (int i = 0; i < tmp.length; i++) {
            assertEquals(tmp[i], b2[i]);
            assertEquals(tmp[i], buffer.byteAt(i));
        }
        buffer.clear();
        assertEquals(16, buffer.capacity()); 
        assertEquals(0, buffer.length());
        assertTrue(buffer.isEmpty());
        assertFalse(buffer.isFull());
    }
    
    public void testExpandAppend() throws Exception {
        ByteArrayBuffer buffer = new ByteArrayBuffer(4);
        assertEquals(4, buffer.capacity()); 
        
        byte[] tmp = new byte[] { 1, 2, 3, 4};
        buffer.append(tmp, 0, 2);
        buffer.append(tmp, 0, 4);
        buffer.append(tmp, 0, 0);

        assertEquals(8, buffer.capacity()); 
        assertEquals(6, buffer.length());
        
        buffer.append(tmp, 0, 4);
        
        assertEquals(16, buffer.capacity()); 
        assertEquals(10, buffer.length());
    }
    
    public void testInvalidAppend() throws Exception {
        ByteArrayBuffer buffer = new ByteArrayBuffer(4);
        buffer.append((byte[])null, 0, 0);

        byte[] tmp = new byte[] { 1, 2, 3, 4};
        try {
            buffer.append(tmp, -1, 0);
            fail("IndexOutOfBoundsException should have been thrown");
        } catch (IndexOutOfBoundsException ex) {
            // expected
        }
        try {
            buffer.append(tmp, 0, -1);
            fail("IndexOutOfBoundsException should have been thrown");
        } catch (IndexOutOfBoundsException ex) {
            // expected
        }
        try {
            buffer.append(tmp, 0, 8);
            fail("IndexOutOfBoundsException should have been thrown");
        } catch (IndexOutOfBoundsException ex) {
            // expected
        }
        try {
            buffer.append(tmp, 10, Integer.MAX_VALUE);
            fail("IndexOutOfBoundsException should have been thrown");
        } catch (IndexOutOfBoundsException ex) {
            // expected
        }
        try {
            buffer.append(tmp, 2, 4);
            fail("IndexOutOfBoundsException should have been thrown");
        } catch (IndexOutOfBoundsException ex) {
            // expected
        }
    }

    public void testAppendOneByte() throws Exception {
        ByteArrayBuffer buffer = new ByteArrayBuffer(4);
        assertEquals(4, buffer.capacity()); 
        
        byte[] tmp = new byte[] { 1, 127, -1, -128, 1, -2};
        for (byte b : tmp) {
            buffer.append(b);
        }
        assertEquals(8, buffer.capacity()); 
        assertEquals(6, buffer.length());
        
        for (int i = 0; i < tmp.length; i++) {
            assertEquals(tmp[i], buffer.byteAt(i));
        }
    }
    
    public void testSetLength() throws Exception {
        ByteArrayBuffer buffer = new ByteArrayBuffer(4);
        buffer.setLength(2);
        assertEquals(2, buffer.length());
    }
    
    public void testSetInvalidLength() throws Exception {
        ByteArrayBuffer buffer = new ByteArrayBuffer(4);
        try {
            buffer.setLength(-2);
            fail("IndexOutOfBoundsException should have been thrown");
        } catch (IndexOutOfBoundsException ex) {
            // expected
        }
        try {
            buffer.setLength(200);
            fail("IndexOutOfBoundsException should have been thrown");
        } catch (IndexOutOfBoundsException ex) {
            // expected
        }
    }
    
}
