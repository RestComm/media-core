/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
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

package org.mobicents.media.control.mgcp.message;

import static org.junit.Assert.*;

import org.junit.Test;
import org.mobicents.media.control.mgcp.exception.MgcpParseException;
import org.mobicents.media.control.mgcp.message.MgcpMessageParser;
import org.mobicents.media.control.mgcp.message.MgcpParameterType;
import org.mobicents.media.control.mgcp.message.MgcpRequest;
import org.mobicents.media.control.mgcp.message.MgcpRequestType;
import org.mobicents.media.control.mgcp.message.MgcpResponse;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpMessageParserTest {

    @Test
    public void testParseCrcxRequest() {
        // given
        StringBuilder builder = new StringBuilder();
        builder.append("CRCX 147483653 mobicents/bridge/$@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("C:1").append(System.lineSeparator());
        builder.append("M:sendrecv").append(System.lineSeparator());
        builder.append("N:restcomm@127.0.0.1:2727").append(System.lineSeparator());
        builder.append("Z2:mobicents/ivr/$@127.0.0.1:2427");
        MgcpMessageParser parser = new MgcpMessageParser();

        try {
            // when
            MgcpRequest request = parser.parseRequest(builder.toString());

            // then
            assertEquals(MgcpRequestType.CRCX, request.getRequestType());
            assertEquals(147483653, request.getTransactionId());
            assertEquals("mobicents/bridge/$@127.0.0.1:2427", request.getEndpointId());
            assertEquals("1", request.getParameter(MgcpParameterType.CALL_ID));
            assertEquals("sendrecv", request.getParameter(MgcpParameterType.MODE));
            assertEquals("restcomm@127.0.0.1:2727", request.getParameter(MgcpParameterType.NOTIFIED_ENTITY));
            assertEquals("mobicents/ivr/$@127.0.0.1:2427", request.getParameter(MgcpParameterType.SECOND_ENDPOINT));
        } catch (MgcpParseException e) {
            fail();
        }
    }

    @Test
    public void testParseCrcxRequestWithSdp() {
        // given
        StringBuilder builder = new StringBuilder();
        builder.append("CRCX 147483655 mobicents/bridge/1@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("C:1").append(System.lineSeparator());
        builder.append("M:sendrecv").append(System.lineSeparator());
        builder.append("N:restcomm@127.0.0.1:2727").append(System.lineSeparator());
        builder.append("L:webrtc:false").append(System.lineSeparator());
        builder.append(System.lineSeparator());
        StringBuilder builderSdp = new StringBuilder();
        builderSdp.append("v=0").append(System.lineSeparator());
        builderSdp.append("o=hrosa 3616 1899 IN IP4 127.0.0.1").append(System.lineSeparator());
        builderSdp.append("s=Talk").append(System.lineSeparator());
        builderSdp.append("c=IN IP4 127.0.0.1").append(System.lineSeparator());
        builderSdp.append("t=0 0").append(System.lineSeparator());
        builderSdp.append("a=rtcp-xr:rcvr-rtt=all:10000 stat-summary=loss,dup,jitt,TTL voip-metrics")
                .append(System.lineSeparator());
        builderSdp.append("m=audio 7070 RTP/AVP 8 0 101").append(System.lineSeparator());
        builderSdp.append("a=rtpmap:101 telephone-event/8000");
        builder.append(builderSdp.toString());
        MgcpMessageParser parser = new MgcpMessageParser();

        try {
            // when
            MgcpRequest request = parser.parseRequest(builder.toString());

            // then
            assertEquals(MgcpRequestType.CRCX, request.getRequestType());
            assertEquals(147483655, request.getTransactionId());
            assertEquals("mobicents/bridge/1@127.0.0.1:2427", request.getEndpointId());
            assertEquals("1", request.getParameter(MgcpParameterType.CALL_ID));
            assertEquals("sendrecv", request.getParameter(MgcpParameterType.MODE));
            assertEquals("restcomm@127.0.0.1:2727", request.getParameter(MgcpParameterType.NOTIFIED_ENTITY));
            assertEquals("webrtc:false", request.getParameter(MgcpParameterType.LOCAL_CONNECTION_OPTIONS));
            assertTrue(request.isSdpDetected());
            assertEquals(builderSdp.toString(), request.getParameter(MgcpParameterType.SDP));
        } catch (MgcpParseException e) {
            fail();
        }
    }

    @Test
    public void testParseMdcxRequest() {
        // given
        StringBuilder builder = new StringBuilder();
        builder.append("MDCX 147483654 mobicents/ivr/1@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("C:1").append(System.lineSeparator());
        builder.append("I:10").append(System.lineSeparator());
        builder.append("M:sendrecv").append(System.lineSeparator());
        MgcpMessageParser parser = new MgcpMessageParser();

        try {
            // when
            MgcpRequest request = parser.parseRequest(builder.toString());

            // then
            assertEquals(MgcpRequestType.MDCX, request.getRequestType());
            assertEquals(147483654, request.getTransactionId());
            assertEquals("mobicents/ivr/1@127.0.0.1:2427", request.getEndpointId());
            assertEquals("1", request.getParameter(MgcpParameterType.CALL_ID));
            assertEquals("sendrecv", request.getParameter(MgcpParameterType.MODE));
            assertEquals("10", request.getParameter(MgcpParameterType.CONNECTION_ID));
        } catch (MgcpParseException e) {
            fail();
        }
    }

    @Test
    public void testParseRqntRequest() {
        // given
        StringBuilder builder = new StringBuilder();
        builder.append("RQNT 147483656 mobicents/ivr/1@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("N:restcomm@127.0.0.1:2727").append(System.lineSeparator());
        builder.append("X:1").append(System.lineSeparator());
        builder.append(
                "S:AU/pa(an=http://localhost:8080/restcomm/cache/ACae6e420f425248d6a26948c17a9e2acf/35ea210b73dcaa203f471c0e30304811a8b89b94a598aa9c44f1c3a5d8a7ce88.wav it=1)")
                .append(System.lineSeparator());
        builder.append("R:AU/oc(N),AU/of(N)").append(System.lineSeparator());
        MgcpMessageParser parser = new MgcpMessageParser();

        try {
            // when
            MgcpRequest request = parser.parseRequest(builder.toString());

            // then
            assertEquals(MgcpRequestType.RQNT, request.getRequestType());
            assertEquals(147483656, request.getTransactionId());
            assertEquals("mobicents/ivr/1@127.0.0.1:2427", request.getEndpointId());
            assertEquals("1", request.getParameter(MgcpParameterType.REQUEST_ID));
            assertEquals(
                    "AU/pa(an=http://localhost:8080/restcomm/cache/ACae6e420f425248d6a26948c17a9e2acf/35ea210b73dcaa203f471c0e30304811a8b89b94a598aa9c44f1c3a5d8a7ce88.wav it=1)",
                    request.getParameter(MgcpParameterType.REQUESTED_SIGNALS));
            assertEquals("AU/oc(N),AU/of(N)", request.getParameter(MgcpParameterType.REQUESTED_EVENTS));
        } catch (MgcpParseException e) {
            fail();
        }
    }

    @Test
    public void testParseNtfyRequest() {
        // given
        StringBuilder builder = new StringBuilder();
        builder.append("NTFY 2 mobicents/ivr/1@127.0.0.1:2427").append(System.lineSeparator());
        builder.append("N:restcomm@127.0.0.1:2727").append(System.lineSeparator());
        builder.append("O:AU/oc(rc=100)").append(System.lineSeparator());
        builder.append("X:1").append(System.lineSeparator());
        MgcpMessageParser parser = new MgcpMessageParser();

        try {
            // when
            MgcpRequest request = parser.parseRequest(builder.toString());

            // then
            assertEquals(MgcpRequestType.NTFY, request.getRequestType());
            assertEquals(2, request.getTransactionId());
            assertEquals("mobicents/ivr/1@127.0.0.1:2427", request.getEndpointId());
            assertEquals("1", request.getParameter(MgcpParameterType.REQUEST_ID));
            assertEquals("AU/oc(rc=100)", request.getParameter(MgcpParameterType.OBSERVED_EVENT));
        } catch (MgcpParseException e) {
            fail();
        }
    }
    
    @Test
    public void testParseOkResponse() {
        // given
        StringBuilder builder = new StringBuilder();
        builder.append("200 147483653 Successful Transaction").append(System.lineSeparator());
        builder.append("I:1f").append(System.lineSeparator());
        builder.append("Z:mobicents/bridge/1@127.0.0.1:2427").append(System.lineSeparator());
        builder.append("Z2:mobicents/ivr/1@127.0.0.1:2427").append(System.lineSeparator());
        builder.append("I2:10").append(System.lineSeparator());
        MgcpMessageParser parser = new MgcpMessageParser();
        
        try {
            // when
            MgcpResponse response = parser.parseResponse(builder.toString());
            
            // then
            assertEquals(200, response.getCode());
            assertEquals(147483653, response.getTransactionId());
            assertEquals("Successful Transaction", response.getMessage());
            assertEquals("1f", response.getParameter(MgcpParameterType.CONNECTION_ID));
            assertEquals("mobicents/bridge/1@127.0.0.1:2427", response.getParameter(MgcpParameterType.ENDPOINT_ID));
            assertEquals("mobicents/ivr/1@127.0.0.1:2427", response.getParameter(MgcpParameterType.SECOND_ENDPOINT));
            assertEquals("10", response.getParameter(MgcpParameterType.CONNECTION_ID2));
        } catch (MgcpParseException e) {
            fail();
        }
    }

    @Test
    public void testParseOkResponseWithSdp() {
        // given
        StringBuilder builder = new StringBuilder();
        builder.append("200 147483655 Successful Transaction").append(System.lineSeparator());
        builder.append("I:20").append(System.lineSeparator());
        builder.append("Z:mobicents/bridge/1@127.0.0.1:2427").append(System.lineSeparator());
        builder.append(System.lineSeparator());
        StringBuilder builderSdp = new StringBuilder();
        builderSdp.append("v=0").append(System.lineSeparator());
        builderSdp.append("o=- 1461941902737 1 IN IP4 127.0.0.1").append(System.lineSeparator());
        builderSdp.append("s=Mobicents Media Server").append(System.lineSeparator());
        builderSdp.append("c=IN IP4 127.0.0.1").append(System.lineSeparator());
        builderSdp.append("t=0 0").append(System.lineSeparator());
        builderSdp.append("m=audio 65534 RTP/AVP 8 0 101").append(System.lineSeparator());
        builderSdp.append("c=IN IP4 127.0.0.1").append(System.lineSeparator());
        builderSdp.append("a=sendrecv").append(System.lineSeparator());
        builderSdp.append("a=rtcp:65535 IN IP4 127.0.0.1").append(System.lineSeparator());
        builderSdp.append("a=ptime:20").append(System.lineSeparator());
        builderSdp.append("a=rtpmap:0 pcmu/8000").append(System.lineSeparator());
        builderSdp.append("a=rtpmap:101 telephone-event/8000").append(System.lineSeparator());
        builderSdp.append("a=rtpmap:8 pcma/8000").append(System.lineSeparator());
        builderSdp.append("a=ssrc:2849765541 cname:l3l35Nm+9PFH1A9O");
        builder.append(builderSdp.toString());
        MgcpMessageParser parser = new MgcpMessageParser();
        
        try {
            // when
            MgcpResponse response = parser.parseResponse(builder.toString());
            
            // then
            assertEquals(200, response.getCode());
            assertEquals(147483655, response.getTransactionId());
            assertEquals("Successful Transaction", response.getMessage());
            assertEquals("20", response.getParameter(MgcpParameterType.CONNECTION_ID));
            assertEquals("mobicents/bridge/1@127.0.0.1:2427", response.getParameter(MgcpParameterType.ENDPOINT_ID));
            assertEquals(builderSdp.toString(), response.getParameter(MgcpParameterType.SDP));
        } catch (MgcpParseException e) {
            fail();
        }
    }

}
