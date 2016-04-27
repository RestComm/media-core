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

import org.mobicents.media.server.impl.resource.audio.AudioRecorderImpl;
import org.mobicents.media.server.impl.resource.dtmf.DetectorImpl;
import org.mobicents.media.server.impl.resource.dtmf.GeneratorImpl;
import org.mobicents.media.server.impl.resource.mediaplayer.audio.AudioPlayerImpl;
import org.mobicents.media.server.impl.resource.phone.PhoneSignalDetector;
import org.mobicents.media.server.impl.resource.phone.PhoneSignalGenerator;
import org.mobicents.media.server.mgcp.connection.LocalConnectionImpl;
import org.mobicents.media.server.mgcp.connection.RtpConnectionImpl;
import org.mobicents.media.server.mgcp.resources.ResourcesPool;
import org.mobicents.media.server.spi.pooling.ResourcePool;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class ResourcesPoolProvider implements Provider<ResourcesPool> {

    private final ResourcePool<LocalConnectionImpl> localConnections;
    private final ResourcePool<RtpConnectionImpl> remoteConnections;
    private final ResourcePool<AudioPlayerImpl> players;
    private final ResourcePool<AudioRecorderImpl> recorders;
    private final ResourcePool<DetectorImpl> dtmfDetectors;
    private final ResourcePool<GeneratorImpl> dtmfGenerators;
    private final ResourcePool<PhoneSignalDetector> signalDetectors;
    private final ResourcePool<PhoneSignalGenerator> signalGenerators;

    @Inject
    public ResourcesPoolProvider(ResourcePool<RtpConnectionImpl> rtpConnections,
            ResourcePool<LocalConnectionImpl> localConnections, ResourcePool<AudioPlayerImpl> players,
            ResourcePool<AudioRecorderImpl> recorders, ResourcePool<DetectorImpl> dtmfDetectors,
            ResourcePool<GeneratorImpl> dtmfGenerators, ResourcePool<PhoneSignalDetector> signalDetectors,
            ResourcePool<PhoneSignalGenerator> signalGenerators) {
        this.players = players;
        this.recorders = recorders;
        this.dtmfDetectors = dtmfDetectors;
        this.dtmfGenerators = dtmfGenerators;
        this.signalDetectors = signalDetectors;
        this.signalGenerators = signalGenerators;
        this.localConnections = localConnections;
        this.remoteConnections = rtpConnections;
    }

    @Override
    public ResourcesPool get() {
        return new ResourcesPool(remoteConnections, localConnections, players, recorders, dtmfDetectors, dtmfGenerators,
                signalDetectors, signalGenerators);
    }

}
