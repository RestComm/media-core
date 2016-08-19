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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;

import org.junit.Test;
import org.mobicents.media.control.mgcp.connection.MgcpConnectionProvider;
import org.mobicents.media.control.mgcp.endpoint.provider.MediaGroupProvider;
import org.mobicents.media.control.mgcp.endpoint.provider.MgcpSplitterEndpointProvider;
import org.mobicents.media.server.scheduler.PriorityQueueScheduler;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpSplitterEndpointProviderTest {

    @Test
    public void testProvide() {
        // given
        final String namespace = "ms/mock/";
        final String domain = "restcomm.com";
        final PriorityQueueScheduler mediaScheduler = mock(PriorityQueueScheduler.class);
        final MgcpConnectionProvider connections = mock(MgcpConnectionProvider.class);
        final MediaGroupProvider mediaGroupProvider = mock(MediaGroupProvider.class);
        final MgcpSplitterEndpointProvider provider = new MgcpSplitterEndpointProvider(namespace, domain, mediaScheduler, connections, mediaGroupProvider);

        // when
        MgcpSplitterEndpoint endpoint1 = provider.provide();
        MgcpSplitterEndpoint endpoint2 = provider.provide();
        MgcpSplitterEndpoint endpoint3 = provider.provide();

        // then
        assertEquals(namespace + 1 + "@" + domain, endpoint1.getEndpointId().toString());
        assertFalse(endpoint1.isActive());
        assertEquals(namespace + 2 + "@" + domain, endpoint2.getEndpointId().toString());
        assertFalse(endpoint2.isActive());
        assertEquals(namespace + 3 + "@" + domain, endpoint3.getEndpointId().toString());
        assertFalse(endpoint3.isActive());
    }

}
