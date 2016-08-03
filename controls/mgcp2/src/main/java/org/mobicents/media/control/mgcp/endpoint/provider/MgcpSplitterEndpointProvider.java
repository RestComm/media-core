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

package org.mobicents.media.control.mgcp.endpoint.provider;

import org.mobicents.media.control.mgcp.connection.MgcpConnectionProvider;
import org.mobicents.media.control.mgcp.endpoint.MediaGroup;
import org.mobicents.media.control.mgcp.endpoint.MgcpSplitterEndpoint;
import org.mobicents.media.server.component.audio.AudioSplitter;
import org.mobicents.media.server.component.oob.OOBSplitter;
import org.mobicents.media.server.scheduler.PriorityQueueScheduler;

/**
 * Provides MGCP endpoints that rely on a Splitter to relay media.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpSplitterEndpointProvider extends AbstractMgcpEndpointProvider<MgcpSplitterEndpoint> {

    private final PriorityQueueScheduler mediaScheduler;
    private final MgcpConnectionProvider connectionProvider;
    private final MediaGroupProvider mediaGroupProvider;

    public MgcpSplitterEndpointProvider(String namespace, PriorityQueueScheduler mediaScheduler, MgcpConnectionProvider connectionProvider, MediaGroupProvider mediaGroupProvider) {
        super(namespace);
        this.mediaScheduler = mediaScheduler;
        this.connectionProvider = connectionProvider;
        this.mediaGroupProvider = mediaGroupProvider;
    }

    @Override
    public MgcpSplitterEndpoint provide() {
        final String endpointId = generateId();
        final AudioSplitter audioSplitter = new AudioSplitter(this.mediaScheduler);
        final OOBSplitter oobSplitter = new OOBSplitter(this.mediaScheduler);
        final MediaGroup mediaGroup = this.mediaGroupProvider.provide();
        return new MgcpSplitterEndpoint(endpointId, audioSplitter, oobSplitter, this.connectionProvider, mediaGroup);
    }

}
