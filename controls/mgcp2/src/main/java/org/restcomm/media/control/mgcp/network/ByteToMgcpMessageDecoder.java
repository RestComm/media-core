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

import java.util.List;

import org.restcomm.media.control.mgcp.exception.MgcpParseException;
import org.restcomm.media.control.mgcp.message.MgcpMessage;
import org.restcomm.media.control.mgcp.message.MgcpMessageParser;
import org.restcomm.media.control.mgcp.message.MgcpRequest;
import org.restcomm.media.control.mgcp.message.MgcpResponse;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

/**
 * Decoder that converts a {@link ByteBuf} into an {@link MgcpMessage}.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class ByteToMgcpMessageDecoder extends ByteToMessageDecoder {

    private final MgcpMessageParser parser;

    public ByteToMgcpMessageDecoder(MgcpMessageParser parser) {
        this.parser = parser;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        // Get data from buffer
        byte[] payload = new byte[msg.readableBytes()];
        msg.getBytes(0, payload);

        // Check message type based on first byte
        byte b = payload[0];

        // Produce message according to type
        MgcpMessage message;
        if (b >= 48 && b <= 57) {
            message = handleResponse(payload);
        } else {
            message = handleRequest(payload);
        }

        // Pass message to next handler
        out.add(message);
    }

    private MgcpRequest handleRequest(byte[] packet) throws MgcpParseException {
        return this.parser.parseRequest(packet);
    }

    private MgcpResponse handleResponse(byte[] packet) throws MgcpParseException {
        return this.parser.parseResponse(packet);
    }

}
