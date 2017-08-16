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

package org.restcomm.media.control.mgcp.connection;

import java.util.concurrent.atomic.AtomicInteger;

import org.restcomm.media.control.mgcp.connection.local.LocalDataChannel;
import org.restcomm.media.control.mgcp.connection.local.LocalDataChannelProvider;
import org.restcomm.media.control.mgcp.connection.local.MgcpLocalConnectionContext;
import org.restcomm.media.control.mgcp.connection.local.MgcpLocalConnectionFsmBuilder;
import org.restcomm.media.control.mgcp.connection.local.MgcpLocalConnectionImpl;
import org.restcomm.media.control.mgcp.connection.remote.MgcpRemoteConnectionContext;
import org.restcomm.media.control.mgcp.connection.remote.MgcpRemoteConnectionImpl;
import org.restcomm.media.control.mgcp.pkg.MgcpEventProvider;
import org.restcomm.media.rtp.RtpConnection;
import org.restcomm.media.rtp.RtpConnectionFactory;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpConnectionProvider {

    private final AtomicInteger idGenerator;
    private final int timeout;
    private final ListeningScheduledExecutorService executor;
    private final MgcpEventProvider eventProvider;
    private final LocalDataChannelProvider localDataChannelProvider;
    private final RtpConnectionFactory rtpConnectionFactory;

    public MgcpConnectionProvider(int timeout, MgcpEventProvider eventProvider,
            LocalDataChannelProvider localDataChannelProvider, RtpConnectionFactory rtpConnectionFactory,
            ListeningScheduledExecutorService executor) {
        this.idGenerator = new AtomicInteger(0);
        this.timeout = timeout;
        this.eventProvider = eventProvider;
        this.executor = executor;
        this.localDataChannelProvider = localDataChannelProvider;
        this.rtpConnectionFactory = rtpConnectionFactory;
    }

    public MgcpRemoteConnection provideRemote(int callId) {
        RtpConnection rtpConnection = this.rtpConnectionFactory.build();
        MgcpRemoteConnectionContext context = new MgcpRemoteConnectionContext(idGenerator.incrementAndGet(), callId, this.timeout, rtpConnection);
        return new MgcpRemoteConnectionImpl(context, this.eventProvider, this.executor);
    }

    public MgcpLocalConnection provideLocal(int callId) {
        LocalDataChannel dataChannel = this.localDataChannelProvider.provide();
        MgcpLocalConnectionContext context = new MgcpLocalConnectionContext(idGenerator.incrementAndGet(), callId, dataChannel);
        return new MgcpLocalConnectionImpl(context, this.eventProvider, this.executor, MgcpLocalConnectionFsmBuilder.INSTANCE);
    }

}
