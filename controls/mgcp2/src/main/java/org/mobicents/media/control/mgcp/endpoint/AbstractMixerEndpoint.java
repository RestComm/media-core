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

import java.util.concurrent.atomic.AtomicBoolean;

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
public class AbstractMixerEndpoint extends AbstractMgcpEndpoint {

    // Core Components
    private final AudioMixer audioMixer;
    private final OOBMixer oobMixer;

    public AbstractMixerEndpoint(String endpointId, PriorityQueueScheduler mediaScheduler) {
        super(endpointId);
        this.audioMixer = new AudioMixer(mediaScheduler);
        this.oobMixer = new OOBMixer(mediaScheduler);
    }

    @Override
    protected void onConnectionCreated(MgcpConnection connection) {
        this.audioMixer.addComponent(connection.getAudioComponent());
        this.oobMixer.addComponent(connection.getOutOfBandComponent());
    }

    @Override
    protected void onConnectionDeleted(MgcpConnection connection) {
        this.audioMixer.release(connection.getAudioComponent());
        this.oobMixer.release(connection.getOutOfBandComponent());
    }
    
    @Override
    protected void onActivated() {
        this.audioMixer.start();
        this.oobMixer.start();
    }
    
    @Override
    protected void onDeactivated() {
        this.audioMixer.stop();
        this.oobMixer.stop();
    }

}
