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

package org.mobicents.media.control.mgcp.endpoint;

import org.mobicents.media.control.mgcp.command.AbstractMgcpEndpoint;
import org.mobicents.media.control.mgcp.connection.MgcpConnection;
import org.mobicents.media.server.component.audio.AudioMixer;
import org.mobicents.media.server.component.oob.OOBMixer;
import org.mobicents.media.server.scheduler.PriorityQueueScheduler;

/**
 * Implementation of an MGCP Endpoint that mixes audio frames from all sources.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpMixerEndpoint extends AbstractMgcpEndpoint {

    // Core Components
    private final AudioMixer inbandMixer;
    private final OOBMixer outbandMixer;

    public MgcpMixerEndpoint(String endpointId, PriorityQueueScheduler mediaScheduler) {
        super(endpointId);
        this.inbandMixer = new AudioMixer(mediaScheduler);
        this.outbandMixer = new OOBMixer(mediaScheduler);
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
        this.inbandMixer.start();
        this.outbandMixer.start();
    }
    
    @Override
    protected void onDeactivated() {
        this.inbandMixer.stop();
        this.outbandMixer.stop();
    }

}
