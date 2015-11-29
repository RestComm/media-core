/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
 * by the @authors tag. 
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

package org.mobicents.media.server.mgcp.io;

import junit.framework.Assert;

import org.junit.Test;
import org.mobicents.media.server.mgcp.MgcpActionType;
import org.mobicents.media.server.mgcp.MgcpParameterType;
import org.mobicents.media.server.mgcp.MgcpRequest;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpDecoderTest {

    private static final String SDP = "v=0\n" 
            + "o=- 25678 753849 IN IP4 128.96.41.1\n" 
            + "s=-\n"
            + "c=IN IP4 128.96.41.1\n" 
            + "t=0 0\n" 
            + "m=audio 3456 RTP/AVP 0";

    private static final String CRCX = "CRCX 1205 /mobicents/bridge/1@rgw-2569.whatever.net MGCP 1.0\n" 
            + "C: A3C47F21456789F0\n"
            + "L: p:10, a:PCMU\n" 
            + "M: sendrecv\n" 
            + "\n" 
            + SDP
            +"\n";

    @Test
    public void testCrcx() {
        // When
        MgcpRequest request = MgcpDecoder.parseRequest(CRCX);
        
        // Then
        Assert.assertEquals(MgcpActionType.CRCX, request.getActionType());
        Assert.assertEquals(1205, request.getTransactionId());
        Assert.assertEquals("/mobicents/bridge/1", request.getEndpointId());
        Assert.assertEquals(SDP, request.getSdp());
        Assert.assertEquals("A3C47F21456789F0", request.getParameter(MgcpParameterType.CALL_ID));
        Assert.assertEquals("p:10, a:PCMU", request.getParameter(MgcpParameterType.LOCAL_CONNECTION_OPTS));
        Assert.assertEquals("sendrecv", request.getParameter(MgcpParameterType.MODE));
    }

}
