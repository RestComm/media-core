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

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;

/**
 * Initializes the MGCP channel and builds its pipeline.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpChannelInitializer extends ChannelInitializer<Channel> {

    private final MgcpMessageDecoder mgcpDecoder;
    private final MgcpMessageEncoder mgcpEncoder;

    public MgcpChannelInitializer(MgcpMessageDecoder mgcpDecoder, MgcpMessageEncoder mgcpEncoder) {
        this.mgcpDecoder = mgcpDecoder;
        this.mgcpEncoder = mgcpEncoder;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addFirst(MgcpMessageDecoder.PIPELINE_KEY, this.mgcpDecoder);
        pipeline.addFirst(MgcpMessageEncoder.PIPELINE_KEY, this.mgcpEncoder);
    }

}
