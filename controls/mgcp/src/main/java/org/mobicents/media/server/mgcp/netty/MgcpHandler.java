package org.mobicents.media.server.mgcp.netty;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import org.apache.log4j.Logger;

@Sharable
public class MgcpHandler extends ChannelInboundHandlerAdapter {
    
    private static Logger LOGGER = Logger.getLogger(MgcpHandler.class);
            
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // TODO Auto-generated method stub
        super.channelActive(ctx);
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // TODO Auto-generated method stub
        super.channelRead(ctx, msg);
    }

}
