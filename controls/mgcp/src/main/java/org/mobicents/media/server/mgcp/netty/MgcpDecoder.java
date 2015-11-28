package org.mobicents.media.server.mgcp.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

import org.mobicents.media.server.mgcp.message.MgcpMessage;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // Get bytes and decode to MGCP Message
        byte eventTypeByte = in.getByte(in.readerIndex());
        MgcpMessage mgcpMessage;

        if (eventTypeByte >= 48 && eventTypeByte <= 57) {
            mgcpMessage = MgcpMessageFactory.createResponse();
        } else {
            mgcpMessage = MgcpMessageFactory.createRequest();
        }
        
        mgcpMessage.write(in.nioBuffer());
        
    }

}
