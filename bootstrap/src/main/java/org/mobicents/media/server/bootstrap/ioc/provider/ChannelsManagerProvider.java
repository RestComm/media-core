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
        
package org.mobicents.media.server.bootstrap.ioc.provider;

import org.mobicents.media.core.configuration.MediaServerConfiguration;
import org.mobicents.media.server.impl.rtp.ChannelsManager;
import org.mobicents.media.server.impl.rtp.crypto.DtlsSrtpServerProvider;
import org.mobicents.media.server.io.network.UdpManager;
import org.mobicents.media.server.scheduler.PriorityQueueScheduler;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class ChannelsManagerProvider implements Provider<ChannelsManager> {

    private final UdpManager udpManager;
    private final PriorityQueueScheduler mediaScheduler;
    private final MediaServerConfiguration config;
    private final DtlsSrtpServerProvider dtlsServerProvider;

    @Inject
    public ChannelsManagerProvider(MediaServerConfiguration config, UdpManager udpManager,
            PriorityQueueScheduler mediaScheduler, DtlsSrtpServerProvider dtlsServerProvider) {
        this.udpManager = udpManager;
        this.mediaScheduler = mediaScheduler;
        this.config = config;
        this.dtlsServerProvider = dtlsServerProvider;
    }

    @Override
    public ChannelsManager get() {
        ChannelsManager channelsManager = new ChannelsManager(this.udpManager, this.dtlsServerProvider);
        channelsManager.setScheduler(mediaScheduler);
        channelsManager.setJitterBufferSize(config.getMediaConfiguration().getJitterBufferSize());
        channelsManager.setCodecs(config.getMediaConfiguration().getCodecs());
        return channelsManager;
    }

}
