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

package org.restcomm.media.bootstrap.ioc.provider.mgcp;

import org.restcomm.media.control.mgcp.connection.MgcpConnectionProvider;
import org.restcomm.media.control.mgcp.connection.local.LocalDataChannelProvider;
import org.restcomm.media.control.mgcp.pkg.MgcpEventProvider;
import org.restcomm.media.core.configuration.MediaServerConfiguration;
import org.restcomm.media.rtp.RtpConnectionFactory;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpConnectionGuiceProvider implements Provider<MgcpConnectionProvider> {

    private final MediaServerConfiguration configuration;
    private final ListeningScheduledExecutorService executor;
    private final MgcpEventProvider eventProvider;
    private final LocalDataChannelProvider localDataChannelProvider;
    private final RtpConnectionFactory rtpConnectionFactory;

    @Inject
    public MgcpConnectionGuiceProvider(MediaServerConfiguration configuration, MgcpEventProvider eventProvider,
            LocalDataChannelProvider localDataChannelProvider, RtpConnectionFactory rtpConnectionFactory,
            ListeningScheduledExecutorService executor) {
        super();
        this.configuration = configuration;
        this.eventProvider = eventProvider;
        this.localDataChannelProvider = localDataChannelProvider;
        this.rtpConnectionFactory = rtpConnectionFactory;
        this.executor = executor;
    }

    @Override
    public MgcpConnectionProvider get() {
        int timeout = this.configuration.getMediaConfiguration().getMaxDuration();
        return new MgcpConnectionProvider(timeout, this.eventProvider, this.localDataChannelProvider, this.rtpConnectionFactory, executor);
    }

}
