/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2017, Telestax Inc and individual contributors
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

package org.restcomm.media.control.mgcp.endpoint.splitter;

import org.junit.Test;
import org.restcomm.media.component.audio.AudioSplitter;
import org.restcomm.media.component.oob.OOBSplitter;
import org.restcomm.media.control.mgcp.endpoint.*;
import org.restcomm.media.control.mgcp.endpoint.notification.NotificationCenter;

import static org.mockito.Mockito.*;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class ActivateSplitterActionTest {

    @Test
    public void testActivateSplitter() {
        // given
        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/mock", "127.0.0.1:2427");
        final AudioSplitter splitter = mock(AudioSplitter.class);
        final OOBSplitter oobSplitter = mock(OOBSplitter.class);
        final NotificationCenter notificationCenter = mock(NotificationCenter.class);
        final MgcpSplitterEndpointContext context = new MgcpSplitterEndpointContext(endpointId, notificationCenter, splitter, oobSplitter);

        final MgcpEndpointFsm fsm = mock(MgcpEndpointFsm.class);
        when(fsm.getContext()).thenReturn(context);

        // when
        final MgcpEndpointTransitionContext txContext = mock(MgcpEndpointTransitionContext.class);
        final ActivateSplitterAction action = new ActivateSplitterAction();

        action.execute(MgcpEndpointState.IDLE, MgcpEndpointState.ACTIVE, MgcpEndpointEvent.REGISTERED_CONNECTION, txContext, fsm);

        // then
        verify(splitter).start();
        verify(oobSplitter).start();
    }

}
