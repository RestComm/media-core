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

package org.restcomm.media.core.control.mgcp.endpoint.provider;

import org.restcomm.media.component.audio.AudioMixer;
import org.restcomm.media.component.oob.OOBMixer;
import org.restcomm.media.core.control.mgcp.connection.MgcpConnectionProvider;
import org.restcomm.media.core.control.mgcp.endpoint.EndpointIdentifier;
import org.restcomm.media.core.control.mgcp.endpoint.MediaGroup;
import org.restcomm.media.core.control.mgcp.endpoint.MgcpMixerEndpoint;
import org.restcomm.media.core.scheduler.PriorityQueueScheduler;

/**
 * Provides MGCP endpoints that rely on a Mixer to relay media.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpMixerEndpointProvider extends AbstractMgcpEndpointProvider<MgcpMixerEndpoint> {

    private final PriorityQueueScheduler mediaScheduler;
    private final MgcpConnectionProvider connectionProvider;
    private final MediaGroupProvider mediaGroupProvider;

    public MgcpMixerEndpointProvider(String namespace, String domain, PriorityQueueScheduler mediaScheduler, MgcpConnectionProvider connectionProvider, MediaGroupProvider mediaGroupProvider) {
        super(namespace, domain);
        this.mediaScheduler = mediaScheduler;
        this.connectionProvider = connectionProvider;
        this.mediaGroupProvider = mediaGroupProvider;
    }

    @Override
    public MgcpMixerEndpoint provide() {
        final EndpointIdentifier endpointId = new EndpointIdentifier(generateId(), getDomain());
        final AudioMixer audioMixer = new AudioMixer(this.mediaScheduler);
        final OOBMixer oobMixer = new OOBMixer(this.mediaScheduler);
        final MediaGroup mediaGroup = this.mediaGroupProvider.provide();
        return new MgcpMixerEndpoint(endpointId, audioMixer, oobMixer, this.connectionProvider, mediaGroup);
    }

}
