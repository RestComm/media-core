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

package org.restcomm.media.control.mgcp.endpoint;

import org.restcomm.media.control.mgcp.connection.MgcpConnection;
import org.restcomm.media.control.mgcp.connection.MgcpConnectionProvider;
import org.restcomm.media.server.component.audio.AudioSplitter;
import org.restcomm.media.server.component.oob.OOBSplitter;

/**
 * Provides MGCP endpoints that rely on a Mixer to relay media.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpSplitterEndpoint extends GenericMgcpEndpoint {

    private final AudioSplitter inbandSplitter;
    private final OOBSplitter outbandSplitter;

    public MgcpSplitterEndpoint(EndpointIdentifier endpointId, AudioSplitter inbandSplitter, OOBSplitter outbandSplitter, MgcpConnectionProvider connectionProvider, MediaGroup mediaGroup) {
        super(endpointId, connectionProvider, mediaGroup);
        this.inbandSplitter = inbandSplitter;
        this.outbandSplitter = outbandSplitter;
    }

    @Override
    protected void onConnectionCreated(MgcpConnection connection) {
        if (connection.isLocal()) {
            this.inbandSplitter.addInsideComponent(connection.getAudioComponent());
            this.outbandSplitter.addInsideComponent(connection.getOutOfBandComponent());
        } else {
            this.inbandSplitter.addOutsideComponent(connection.getAudioComponent());
            this.outbandSplitter.addOutsideComponent(connection.getOutOfBandComponent());
        }
    }

    @Override
    protected void onConnectionDeleted(MgcpConnection connection) {
        if (connection.isLocal()) {
            this.inbandSplitter.releaseInsideComponent(connection.getAudioComponent());
            this.outbandSplitter.releaseInsideComponent(connection.getOutOfBandComponent());
        } else {
            this.inbandSplitter.releaseOutsideComponent(connection.getAudioComponent());
            this.outbandSplitter.releaseOutsideComponent(connection.getOutOfBandComponent());
        }
    }

    @Override
    protected void onActivated() {
        this.inbandSplitter.start();
        this.outbandSplitter.start();
    }

    @Override
    protected void onDeactivated() {
        this.inbandSplitter.stop();
        this.outbandSplitter.stop();
    }

}
