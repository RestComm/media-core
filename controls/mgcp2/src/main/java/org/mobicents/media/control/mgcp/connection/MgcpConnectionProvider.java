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
        
package org.mobicents.media.control.mgcp.connection;

import java.util.concurrent.atomic.AtomicInteger;

import org.mobicents.media.control.mgcp.connection.local.MgcpLocalConnection;
import org.mobicents.media.control.mgcp.connection.remote.MgcpRemoteConnection;
import org.mobicents.media.server.impl.rtp.channels.MediaChannelProvider;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpConnectionProvider {
    
    private final AtomicInteger idGenerator;
    private final MediaChannelProvider channelProvider;
    
    public MgcpConnectionProvider(MediaChannelProvider channelProvider) {
        this.idGenerator = new AtomicInteger(0);
        this.channelProvider = channelProvider;
    }
    
    public MgcpRemoteConnection provideRemote() {
        return new MgcpRemoteConnection(this.idGenerator.incrementAndGet(), channelProvider);
    }
    
    public MgcpLocalConnection provideLocal() {
        // TODO implement MgcpConnectionProvider.provideLocal
        return null;
    }

}
