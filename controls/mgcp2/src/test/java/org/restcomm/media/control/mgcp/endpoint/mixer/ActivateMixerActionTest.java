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

package org.restcomm.media.control.mgcp.endpoint.mixer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.restcomm.media.component.audio.AudioMixer;
import org.restcomm.media.component.oob.OOBMixer;
import org.restcomm.media.control.mgcp.endpoint.EndpointIdentifier;
import org.restcomm.media.control.mgcp.endpoint.MediaGroupImpl;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointEvent;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointFsm;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointState;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointTransitionContext;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class ActivateMixerActionTest {

    @Test
    public void testActivateMixer() {
        // given
        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/mock", "127.0.0.1:2427");
        final MediaGroupImpl mediaGroup = mock(MediaGroupImpl.class);
        final AudioMixer mixer = mock(AudioMixer.class);
        final OOBMixer oobMixer = mock(OOBMixer.class);
        final MgcpMixerEndpointContext context = new MgcpMixerEndpointContext(endpointId, mediaGroup, mixer, oobMixer);

        final MgcpEndpointFsm fsm = mock(MgcpEndpointFsm.class);
        when(fsm.getContext()).thenReturn(context);

        // when
        final MgcpEndpointTransitionContext txContext = mock(MgcpEndpointTransitionContext.class);
        final ActivateMixerAction action = new ActivateMixerAction();

        action.execute(MgcpEndpointState.IDLE, MgcpEndpointState.ACTIVE, MgcpEndpointEvent.REGISTERED_CONNECTION, txContext, fsm);

        // then
        verify(mixer).start();
        verify(oobMixer).start();
    }

}
