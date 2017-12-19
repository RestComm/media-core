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

package org.restcomm.media.bootstrap.ioc.provider.rtp;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.inject.Inject;
import com.google.inject.Provider;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.restcomm.media.rtp.RtpChannelInitializer;
import org.restcomm.media.rtp.RtpNetworkManager;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com) created on 18/12/2017
 */
public class RtpNetworkManagerProvider implements Provider<RtpNetworkManager> {

    private final Bootstrap bootstrap;
    private final EventLoopGroup eventLoopGroup;

    @Inject
    public RtpNetworkManagerProvider(ListeningScheduledExecutorService executor) {
        this.eventLoopGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors(), executor);
        this.bootstrap = new Bootstrap().channel(NioDatagramChannel.class).group(eventLoopGroup);
    }

    @Override
    public RtpNetworkManager get() {
        final RtpChannelInitializer channelInitializer = new RtpChannelInitializer();
        // TODO Add handlers to RTP Channel Initializer
        return new RtpNetworkManager(this.bootstrap, channelInitializer);
    }

}
