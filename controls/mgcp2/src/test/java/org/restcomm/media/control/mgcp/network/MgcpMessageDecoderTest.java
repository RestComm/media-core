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

package org.restcomm.media.control.mgcp.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.restcomm.media.control.mgcp.exception.MgcpParseException;
import org.restcomm.media.control.mgcp.message.MgcpMessageParser;
import org.restcomm.media.control.mgcp.message.MgcpParameterType;
import org.restcomm.media.control.mgcp.message.MgcpRequest;
import org.restcomm.media.control.mgcp.message.MgcpRequestType;
import org.restcomm.media.control.mgcp.message.MgcpResponse;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpMessageDecoderTest {

    @Test
    public void testDecodeRequest() throws Exception {
        // given
        StringBuilder builder = new StringBuilder();
        builder.append("CRCX 147483653 mobicents/bridge/$@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("C:1").append(System.lineSeparator());
        builder.append("M:sendrecv").append(System.lineSeparator());
        builder.append("N:restcomm@127.0.0.1:2727").append(System.lineSeparator());
        builder.append("Z2:mobicents/ivr/$@127.0.0.1:2427");

        final byte[] payload = builder.toString().getBytes();
        final MgcpMessageParser parser = new MgcpMessageParser();
        final ChannelHandlerContext context = mock(ChannelHandlerContext.class);
        final ByteBuf byteBuf = mock(ByteBuf.class);
        final List<Object> outList = new ArrayList<>(1);
        final MgcpMessageDecoder decoder = new MgcpMessageDecoder(parser);

        // when
        when(byteBuf.readableBytes()).thenReturn(payload.length);
        when(byteBuf.getBytes(eq(0), any(byte[].class))).then(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                byte[] realPayload = invocation.getArgumentAt(1, byte[].class);
                System.arraycopy(payload, 0, realPayload, 0, payload.length);
                return null;
            }
        });

        decoder.decode(context, byteBuf, outList);

        // then
        assertFalse(outList.isEmpty());
        assertTrue(outList.get(0) instanceof MgcpRequest);

        MgcpRequest request = (MgcpRequest) outList.get(0);
        assertEquals(MgcpRequestType.CRCX, request.getRequestType());
        assertEquals(147483653, request.getTransactionId());
        assertEquals("mobicents/bridge/$@127.0.0.1:2427", request.getEndpointId());
        assertEquals("1", request.getParameter(MgcpParameterType.CALL_ID));
        assertEquals("sendrecv", request.getParameter(MgcpParameterType.MODE));
        assertEquals("restcomm@127.0.0.1:2727", request.getParameter(MgcpParameterType.NOTIFIED_ENTITY));
        assertEquals("mobicents/ivr/$@127.0.0.1:2427", request.getParameter(MgcpParameterType.SECOND_ENDPOINT));
    }

    @Test
    public void testDecodeResponse() throws Exception {
        // given
        StringBuilder builder = new StringBuilder();
        builder.append("200 147483653 Successful Transaction").append(System.lineSeparator());
        builder.append("I:1f").append(System.lineSeparator());
        builder.append("Z:mobicents/bridge/1@127.0.0.1:2427").append(System.lineSeparator());
        builder.append("Z2:mobicents/ivr/1@127.0.0.1:2427").append(System.lineSeparator());
        builder.append("I2:10").append(System.lineSeparator());

        final byte[] payload = builder.toString().getBytes();
        final MgcpMessageParser parser = new MgcpMessageParser();
        final ChannelHandlerContext context = mock(ChannelHandlerContext.class);
        final ByteBuf byteBuf = mock(ByteBuf.class);
        final List<Object> outList = new ArrayList<>(1);
        final MgcpMessageDecoder decoder = new MgcpMessageDecoder(parser);

        // when
        when(byteBuf.readableBytes()).thenReturn(payload.length);
        when(byteBuf.getBytes(eq(0), any(byte[].class))).then(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                byte[] realPayload = invocation.getArgumentAt(1, byte[].class);
                System.arraycopy(payload, 0, realPayload, 0, payload.length);
                return null;
            }
        });

        decoder.decode(context, byteBuf, outList);

        // then
        assertFalse(outList.isEmpty());
        assertTrue(outList.get(0) instanceof MgcpResponse);

        MgcpResponse response = (MgcpResponse) outList.get(0);
        assertEquals(200, response.getCode());
        assertEquals(147483653, response.getTransactionId());
        assertEquals("Successful Transaction", response.getMessage());
        assertEquals("1f", response.getParameter(MgcpParameterType.CONNECTION_ID));
        assertEquals("mobicents/bridge/1@127.0.0.1:2427", response.getParameter(MgcpParameterType.ENDPOINT_ID));
        assertEquals("mobicents/ivr/1@127.0.0.1:2427", response.getParameter(MgcpParameterType.SECOND_ENDPOINT));
        assertEquals("10", response.getParameter(MgcpParameterType.CONNECTION_ID2));
    }

    @Test(expected = MgcpParseException.class)
    public void testDecodeMalformedMessage() throws Exception {
        // given
        StringBuilder builder = new StringBuilder();
        builder.append("CRCX 147483653 mobicents/bridge/$@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("C:1").append(System.lineSeparator());
        builder.append("M:sendrecv").append(System.lineSeparator());
        builder.append("N:restcomm@127.0.0.1:2727").append(System.lineSeparator());
        builder.append("Z2:mobicents/ivr/$@127.0.0.1:2427");

        final byte[] payload = builder.toString().getBytes();
        final MgcpMessageParser parser = mock(MgcpMessageParser.class);
        final ChannelHandlerContext context = mock(ChannelHandlerContext.class);
        final ByteBuf byteBuf = mock(ByteBuf.class);
        final List<Object> outList = new ArrayList<>(1);
        final MgcpMessageDecoder decoder = new MgcpMessageDecoder(parser);

        // when
        when(byteBuf.readableBytes()).thenReturn(payload.length);
        when(byteBuf.getBytes(eq(0), any(byte[].class))).then(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                byte[] realPayload = invocation.getArgumentAt(1, byte[].class);
                System.arraycopy(payload, 0, realPayload, 0, payload.length);
                return null;
            }
        });
        when(parser.parseRequest(any(byte[].class))).thenThrow(new MgcpParseException("Testing purposes!"));

        decoder.decode(context, byteBuf, outList);
    }

}
