/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.restcomm.media.control.mgcp.message;

import org.mobicents.media.server.utils.Text;
import org.restcomm.media.control.mgcp.message.MgcpRequest;
import org.restcomm.media.control.mgcp.message.Parameter;

import java.nio.ByteBuffer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author kulikov
 */
public class RequestTest {
    private byte[] buff = (
            "CRCX 1205 aaln/1@rgw-2569.whatever.net MGCP 1.0\n" +
      "C: A3C47F21456789F0\n"+
      "L: p:10, a:PCMU\n"+
      "M: sendrecv\n"+
      "\n"+
      "v=0\n"+
      "o=- 25678 753849 IN IP4 128.96.41.1\n"+
      "s=-\n"+
      "c=IN IP4 128.96.41.1\n"+
      "t=0 0\n"+
      "m=audio 3456 RTP/AVP 0\n"
            ).getBytes();
        
    private MgcpRequest request;
    
    public RequestTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() {
        request = new MgcpRequest();
        
        long s = System.nanoTime();
        request.strain(buff, 0, buff.length);
        
        long f = System.nanoTime();
        System.out.println("Duration = " + (f-s));
        
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of getCommand method, of class Request.
     */
    @Test
    public void testGetCommand() {
        assertEquals("CRCX", request.getCommand().toString());
    }

    /**
     * Test of getEndpoint method, of class Request.
     */
    @Test
    public void testGetEndpoint() {
        assertEquals("aaln/1@rgw-2569.whatever.net", request.getEndpoint().toString());
    }

    /**
     * Test of getTxID method, of class Request.
     */
    @Test
    public void testGetTxID() {
        assertEquals(1205, request.getTxID());
    }

    /**
     * Test of getParameter method, of class Request.
     */
    @Test
    public void testGetCallI() {
        assertEquals("A3C47F21456789F0", request.getParameter(Parameter.CALL_ID).toString());
    }

    @Test
    public void testGetMode() {
        assertEquals("sendrecv", request.getParameter(Parameter.MODE).toString());
    }

//    @Test
    public void testGetSecondEndpoint() {
        assertEquals("second@domain.com", request.getParameter(Parameter.SECOND_ENDPOINT).toString());
    }
   
    
    @Test
    public void testHeaderEncoding() {
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        
        MgcpRequest req = new MgcpRequest();
        req.setCommand(new Text("CRCX"));
        req.setTxID(1);
        req.setEndpoint(new Text("test@localhost"));
        req.setParameter(new Text("c"), new Text("abcd"));
        req.write(buffer);
        
        int size = buffer.limit();
        byte[] data = new byte[size];
        
        buffer.get(data);
        System.out.println(new String(data));
    }
    
    @Test
    public void testSdp() {
        String sdp = request.getParameter(Parameter.SDP).toString();
        System.out.println(sdp);
    }
    
    @Test
    public void testChars() {
        char[] digits = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
        for (int i = 0; i < digits.length; i++) {
            System.out.println((byte)digits[i]);
        }
            System.out.println((byte)'\n');
    }
    
}
