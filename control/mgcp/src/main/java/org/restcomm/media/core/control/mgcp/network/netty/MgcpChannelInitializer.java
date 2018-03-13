/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2017, Telestax Inc and individual contributors
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

package org.restcomm.media.core.control.mgcp.network.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.FixedRecvByteBufAllocator;

/**
 * Initializes the MGCP channel and builds its pipeline.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpChannelInitializer extends ChannelInitializer<Channel> {

    private int channelBuffer;
    private static final ChannelHandler[] NO_HANDLERS = new ChannelHandler[0];

    private final ChannelHandler[] handlers;

    public MgcpChannelInitializer(int channelBuffer, ChannelHandler... handlers) {
		/* channelBuffer configuration from MGCP section of mediaserver.xml */
		this.channelBuffer = channelBuffer;
		this.handlers = (handlers == null || handlers.length == 0) ? NO_HANDLERS : handlers;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        ch.config().setOption(ChannelOption.SO_RCVBUF, this.channelBuffer);
        ch.config().setOption(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(this.channelBuffer));

        // Build handlers pipeline
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(this.handlers);
    }

}
