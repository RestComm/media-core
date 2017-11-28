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

package org.restcomm.media.control.mgcp.endpoint.provider;

import org.restcomm.media.component.audio.AudioSplitter;
import org.restcomm.media.component.oob.OOBSplitter;
import org.restcomm.media.control.mgcp.endpoint.EndpointIdentifier;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointFsm;
import org.restcomm.media.control.mgcp.endpoint.notification.NotificationCenter;
import org.restcomm.media.control.mgcp.endpoint.notification.NotificationCenterContext;
import org.restcomm.media.control.mgcp.endpoint.notification.NotificationCenterFsmBuilder;
import org.restcomm.media.control.mgcp.endpoint.notification.NotificationCenterImpl;
import org.restcomm.media.control.mgcp.endpoint.splitter.MgcpSplitterEndpoint;
import org.restcomm.media.control.mgcp.endpoint.splitter.MgcpSplitterEndpointContext;
import org.restcomm.media.control.mgcp.endpoint.splitter.MgcpSplitterEndpointFsmBuilder;
import org.restcomm.media.scheduler.PriorityQueueScheduler;

/**
 * Provides MGCP endpoints that rely on a Splitter to relay media.
 *
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class MgcpSplitterEndpointProvider extends AbstractMgcpEndpointProvider<MgcpSplitterEndpoint> {

    private final PriorityQueueScheduler mediaScheduler;

    public MgcpSplitterEndpointProvider(String namespace, String domain, PriorityQueueScheduler mediaScheduler) {
        super(namespace, domain);
        this.mediaScheduler = mediaScheduler;
    }

    @Override
    public MgcpSplitterEndpoint provide() {
        final EndpointIdentifier endpointId = new EndpointIdentifier(generateId(), getDomain());
        final AudioSplitter audioSplitter = new AudioSplitter(this.mediaScheduler);
        final OOBSplitter oobSplitter = new OOBSplitter(this.mediaScheduler);

        final NotificationCenterContext notificationCenterContext = new NotificationCenterContext();
        final NotificationCenter notificationCenter = new NotificationCenterImpl(NotificationCenterFsmBuilder.INSTANCE.build(notificationCenterContext));

        final MgcpSplitterEndpointContext context = new MgcpSplitterEndpointContext(endpointId, notificationCenter, audioSplitter, oobSplitter);
        final MgcpEndpointFsm fsm = MgcpSplitterEndpointFsmBuilder.INSTANCE.build(context);
        return new MgcpSplitterEndpoint(context, fsm);
    }

}
