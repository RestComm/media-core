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
import org.restcomm.media.server.component.audio.AudioMixer;
import org.restcomm.media.server.component.oob.OOBMixer;

/**
 * Implementation of an MGCP Endpoint that mixes audio frames from all sources.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpMixerEndpoint extends GenericMgcpEndpoint {

    // Core Components
    private final AudioMixer inbandMixer;
    private final OOBMixer outbandMixer;

    public MgcpMixerEndpoint(EndpointIdentifier endpointId, AudioMixer inbandMixer, OOBMixer outbandMixer, MgcpConnectionProvider connectionProvider, MediaGroup mediaGroup) {
        super(endpointId, connectionProvider, mediaGroup);
        this.inbandMixer = inbandMixer;
        this.outbandMixer = outbandMixer;
    }

    @Override
    protected void onConnectionCreated(MgcpConnection connection) {
        this.inbandMixer.addComponent(connection.getAudioComponent());
        this.outbandMixer.addComponent(connection.getOutOfBandComponent());
    }

    @Override
    protected void onConnectionDeleted(MgcpConnection connection) {
        this.inbandMixer.release(connection.getAudioComponent());
        this.outbandMixer.release(connection.getOutOfBandComponent());
    }

    @Override
    protected void onActivated() {
        // Wire media group to mixer
        this.inbandMixer.addComponent(((MediaGroupImpl) this.mediaGroup).getAudioComponent());
        this.outbandMixer.addComponent(((MediaGroupImpl) this.mediaGroup).getOobComponent());

        // Start mixers
        this.inbandMixer.start();
        this.outbandMixer.start();
    }

    @Override
    protected void onDeactivated() {
        // Disconnect media group from mixer
        this.inbandMixer.release(((MediaGroupImpl) this.mediaGroup).getAudioComponent());
        this.outbandMixer.release(((MediaGroupImpl) this.mediaGroup).getOobComponent());

        // Stop mixer
        this.inbandMixer.stop();
        this.outbandMixer.stop();
    }

}
