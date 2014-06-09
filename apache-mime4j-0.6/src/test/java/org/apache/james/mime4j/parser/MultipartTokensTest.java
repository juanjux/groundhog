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

package org.apache.james.mime4j.parser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import junit.framework.TestCase;

import org.apache.commons.io.IOUtils;
import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.util.CharsetUtil;

public class MultipartTokensTest extends TestCase {

    private static final Charset US_ASCII = CharsetUtil.US_ASCII;
    
    private static final String BODY = "A Preamble\r\n" +
                "--1729\r\n\r\n" +
                "Simple plain text\r\n" +
                "--1729\r\n" +
                "Content-Type: text/plain; charset=US-ASCII\r\n\r\n" +
                "Some more text\r\n" +
                "--1729--\r\n";
    public static final String MESSAGE = "To: Road Runner <runner@example.org>\r\n" +
            "From: Wile E. Cayote <wile@example.org>\r\n" +
            "Date: Tue, 12 Feb 2008 17:34:09 +0000 (GMT)\r\n" +
            "Subject: Mail\r\n" +
            "Content-Type: multipart/mixed;boundary=1729\r\n\r\n" +
            BODY;
    
    public static final String COMPLEX_MESSAGE = "To: Wile E. Cayote <wile@example.org>\r\n" +
    "From: Road Runner <runner@example.org>\r\n" +
    "Date: Tue, 19 Feb 2008 17:34:09 +0000 (GMT)\r\n" +
    "Subject: Mail\r\n" +
    "Content-Type: multipart/mixed;boundary=42\r\n\r\n" +
    "A little preamble\r\n" +
    "--42\r\n" +
    "Content-Type: text/plain; charset=US-ASCII\r\n\r\n" +
    "Rhubard!\r\n" +
    "--42\r\n" +
    "Content-Type: message/rfc822\r\n\r\n" +
    MESSAGE +
    "\r\n" +
    "--42\r\n" +
    "\r\n" +
    "Custard!" +
    "\r\n" +
    "--42--\r\n";
    
    public static final String COMPLEX_QP_MESSAGE = 
        "Content-Transfer-Encoding: quoted-printable\r\n" +
        "Content-Type: message/rfc822; charset=us-ascii\r\n" +
        "\r\n" +
        "Subject: The subject\r\n" +
        "Content-Type: multipart/alternative;\r\n" +
        "        boundary=3D=22----=3DNextPart=22\r\n" +
        "\r\n" +
        "This is a multi-part message in MIME format.\r\n" +
        "\r\n" +
        "------=3DNextPart\r\n" +
        "Content-Type: text/plain;\r\n" +
        "        charset=3D=22iso-8859-1=22\r\n" +
        "\r\n" +
        "Some text\r\n" +
        "\r\n" +
        "------=3DNextPart\r\n" +
        "Content-Type: text/html;\r\n" +
        "        charset=3D=22iso-8859-1=22\r\n" +
        "\r\n" +
        "<HTML><BODY>=3D Some HTML =3D</BODY></HTML>\r\n" +
        "------=3DNextPart--\r\n" +
        "\r\n" +
        "\r\n";
    
    MimeTokenStream parser;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        parser = new MimeTokenStream();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testShouldParseSimpleMessage() throws Exception {
        parser.parse(new ByteArrayInputStream(US_ASCII.encode(MESSAGE).array()));
        checkState(MimeTokenStream.T_START_HEADER);
        checkState(MimeTokenStream.T_FIELD);
        checkState(MimeTokenStream.T_FIELD);
        checkState(MimeTokenStream.T_FIELD);
        checkState(MimeTokenStream.T_FIELD);
        checkState(MimeTokenStream.T_FIELD);
        checkState(MimeTokenStream.T_END_HEADER);
        checkState(MimeTokenStream.T_START_MULTIPART);
        checkState(MimeTokenStream.T_PREAMBLE);
        checkState(MimeTokenStream.T_START_BODYPART);
        checkState(MimeTokenStream.T_START_HEADER);
        checkState(MimeTokenStream.T_END_HEADER);
        checkState(MimeTokenStream.T_BODY);
        checkState(MimeTokenStream.T_END_BODYPART);
        checkState(MimeTokenStream.T_START_BODYPART);
        checkState(MimeTokenStream.T_START_HEADER);
        checkState(MimeTokenStream.T_FIELD);
        checkState(MimeTokenStream.T_END_HEADER);
        checkState(MimeTokenStream.T_BODY);
        checkState(MimeTokenStream.T_END_BODYPART);
        checkState(MimeTokenStream.T_EPILOGUE);
        checkState(MimeTokenStream.T_END_MULTIPART);
        checkState(MimeTokenStream.T_END_MESSAGE);
        checkState(MimeTokenStream.T_END_OF_STREAM);
    }
    
    public void testShouldParseMoreComplexMessage() throws Exception {
        String message = 
            "Content-Type: multipart/alternative; boundary=\"outer-boundary\"\r\n" +
            "\r\n" +
            "--outer-boundary\r\n" +
            "Content-Type: multipart/alternative; boundary=\"inner-boundary\"\r\n" +
            "\r\n" +
            "--inner-boundary\r\n" +
            "Content-Type: text/plain\r\n" +
            "\r\n" +
            "Some text\r\n" +
            "--inner-boundary--\r\n" +
            "\r\n" +
            "foo\r\n" +
            "--outer-boundary--\r\n";
        
        parser.parse(new ByteArrayInputStream(US_ASCII.encode(message).array()));
        checkState(MimeTokenStream.T_START_HEADER);
        checkState(MimeTokenStream.T_FIELD);
        checkState(MimeTokenStream.T_END_HEADER);
        checkState(MimeTokenStream.T_START_MULTIPART);
        checkState(MimeTokenStream.T_PREAMBLE);
        checkState(MimeTokenStream.T_START_BODYPART);
        checkState(MimeTokenStream.T_START_HEADER);
        checkState(MimeTokenStream.T_FIELD);
        checkState(MimeTokenStream.T_END_HEADER);
        checkState(MimeTokenStream.T_START_MULTIPART);
        checkState(MimeTokenStream.T_PREAMBLE);
        checkState(MimeTokenStream.T_START_BODYPART);
        checkState(MimeTokenStream.T_START_HEADER);
        checkState(MimeTokenStream.T_FIELD);
        checkState(MimeTokenStream.T_END_HEADER);
        checkState(MimeTokenStream.T_BODY);
        checkState(MimeTokenStream.T_END_BODYPART);
        checkState(MimeTokenStream.T_EPILOGUE);
        checkState(MimeTokenStream.T_END_MULTIPART);
        checkState(MimeTokenStream.T_END_BODYPART);
        checkState(MimeTokenStream.T_EPILOGUE);
        checkState(MimeTokenStream.T_END_MULTIPART);
        checkState(MimeTokenStream.T_END_MESSAGE);
        checkState(MimeTokenStream.T_END_OF_STREAM);
    }
    
    public void testShouldParseMessageWithEmbeddedMessage() throws Exception {
        parser.parse(new ByteArrayInputStream(US_ASCII.encode(COMPLEX_MESSAGE).array()));
        checkState(MimeTokenStream.T_START_HEADER);
            checkState(MimeTokenStream.T_FIELD);
            checkState(MimeTokenStream.T_FIELD);
            checkState(MimeTokenStream.T_FIELD);
            checkState(MimeTokenStream.T_FIELD);
            checkState(MimeTokenStream.T_FIELD);
        checkState(MimeTokenStream.T_END_HEADER);
        checkState(MimeTokenStream.T_START_MULTIPART);
            checkState(MimeTokenStream.T_PREAMBLE);
            checkState(MimeTokenStream.T_START_BODYPART);
                checkState(MimeTokenStream.T_START_HEADER);
                    checkState(MimeTokenStream.T_FIELD);
                checkState(MimeTokenStream.T_END_HEADER);
                checkState(MimeTokenStream.T_BODY);
            checkState(MimeTokenStream.T_END_BODYPART);
            checkState(MimeTokenStream.T_START_BODYPART);
                checkState(MimeTokenStream.T_START_HEADER);
                    checkState(MimeTokenStream.T_FIELD);
                checkState(MimeTokenStream.T_END_HEADER);
                checkState(MimeTokenStream.T_START_MESSAGE);
                    checkState(MimeTokenStream.T_START_HEADER);
                        checkState(MimeTokenStream.T_FIELD);
                        checkState(MimeTokenStream.T_FIELD);
                        checkState(MimeTokenStream.T_FIELD);
                        checkState(MimeTokenStream.T_FIELD);
                        checkState(MimeTokenStream.T_FIELD);
                    checkState(MimeTokenStream.T_END_HEADER);
                    checkState(MimeTokenStream.T_START_MULTIPART);
                        checkState(MimeTokenStream.T_PREAMBLE);
                        checkState(MimeTokenStream.T_START_BODYPART);
                            checkState(MimeTokenStream.T_START_HEADER);
                            checkState(MimeTokenStream.T_END_HEADER);   
                            checkState(MimeTokenStream.T_BODY);
                        checkState(MimeTokenStream.T_END_BODYPART);
                        checkState(MimeTokenStream.T_START_BODYPART);
                            checkState(MimeTokenStream.T_START_HEADER);
                                checkState(MimeTokenStream.T_FIELD);
                            checkState(MimeTokenStream.T_END_HEADER);
                            checkState(MimeTokenStream.T_BODY);
                        checkState(MimeTokenStream.T_END_BODYPART);
                        checkState(MimeTokenStream.T_EPILOGUE);
                    checkState(MimeTokenStream.T_END_MULTIPART);
                checkState(MimeTokenStream.T_END_MESSAGE);
            checkState(MimeTokenStream.T_END_BODYPART);
            checkState(MimeTokenStream.T_START_BODYPART);
                checkState(MimeTokenStream.T_START_HEADER);
                checkState(MimeTokenStream.T_END_HEADER);
                checkState(MimeTokenStream.T_BODY);
            checkState(MimeTokenStream.T_END_BODYPART);
            checkState(MimeTokenStream.T_EPILOGUE);
        checkState(MimeTokenStream.T_END_MULTIPART);
        checkState(MimeTokenStream.T_END_MESSAGE);
        checkState(MimeTokenStream.T_END_OF_STREAM);
    }

    public void testShouldParseMessagesWithEmbeddedQuotedPrintableEncodedMessage() throws Exception {
        parser.parse(new ByteArrayInputStream(US_ASCII.encode(COMPLEX_QP_MESSAGE).array()));
        checkState(MimeTokenStream.T_START_HEADER);
        checkState(MimeTokenStream.T_FIELD);
        checkState(MimeTokenStream.T_FIELD);
        checkState(MimeTokenStream.T_END_HEADER);
        checkState(MimeTokenStream.T_START_MESSAGE);
        checkState(MimeTokenStream.T_START_HEADER);
        checkState(MimeTokenStream.T_FIELD);
        checkState(MimeTokenStream.T_FIELD);
        checkState(MimeTokenStream.T_END_HEADER);
        checkState(MimeTokenStream.T_START_MULTIPART);
        checkState(MimeTokenStream.T_PREAMBLE);
        checkState(MimeTokenStream.T_START_BODYPART);
        checkState(MimeTokenStream.T_START_HEADER);
        checkState(MimeTokenStream.T_FIELD);
        checkState(MimeTokenStream.T_END_HEADER);
        checkState(MimeTokenStream.T_BODY);
        assertEquals("text/plain", parser.getBodyDescriptor().getMimeType());
        assertEquals("iso-8859-1", parser.getBodyDescriptor().getCharset());
        assertEquals("Some text\r\n", 
                IOUtils.toString(parser.getInputStream()));
        checkState(MimeTokenStream.T_END_BODYPART);
        checkState(MimeTokenStream.T_START_BODYPART);
        checkState(MimeTokenStream.T_START_HEADER);
        checkState(MimeTokenStream.T_FIELD);
        checkState(MimeTokenStream.T_END_HEADER);
        checkState(MimeTokenStream.T_BODY);
        assertEquals("text/html", parser.getBodyDescriptor().getMimeType());
        assertEquals("iso-8859-1", parser.getBodyDescriptor().getCharset());
        assertEquals("<HTML><BODY>= Some HTML =</BODY></HTML>", 
                IOUtils.toString(parser.getInputStream()));
        checkState(MimeTokenStream.T_END_BODYPART);
        checkState(MimeTokenStream.T_EPILOGUE);
        checkState(MimeTokenStream.T_END_MULTIPART);
        checkState(MimeTokenStream.T_END_MESSAGE);
        checkState(MimeTokenStream.T_END_MESSAGE);
        checkState(MimeTokenStream.T_END_OF_STREAM);
    }
    
    public void testMultipartMessageWithoutHeader() throws Exception {
        parser.parseHeadless(new ByteArrayInputStream(US_ASCII.encode(BODY).array()), 
                "multipart/mixed;boundary=1729");
        checkState(MimeTokenStream.T_END_HEADER);
        checkState(MimeTokenStream.T_START_MULTIPART);
        checkState(MimeTokenStream.T_PREAMBLE);
        checkState(MimeTokenStream.T_START_BODYPART);
        checkState(MimeTokenStream.T_START_HEADER);
        checkState(MimeTokenStream.T_END_HEADER);
        checkState(MimeTokenStream.T_BODY);
        checkState(MimeTokenStream.T_END_BODYPART);
        checkState(MimeTokenStream.T_START_BODYPART);
        checkState(MimeTokenStream.T_START_HEADER);
        checkState(MimeTokenStream.T_FIELD);
        checkState(MimeTokenStream.T_END_HEADER);
        checkState(MimeTokenStream.T_BODY);
        checkState(MimeTokenStream.T_END_BODYPART);
        checkState(MimeTokenStream.T_EPILOGUE);
        checkState(MimeTokenStream.T_END_MULTIPART);
        checkState(MimeTokenStream.T_END_MESSAGE);
        checkState(MimeTokenStream.T_END_OF_STREAM);
    }
    
    private void checkState(final int state) throws IOException, MimeException {
        assertEquals(MimeTokenStream.stateToString(state), MimeTokenStream.stateToString(parser.next()));
    }
}
