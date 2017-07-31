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

package org.restcomm.media.rtp.connection;

import static org.junit.Assert.*;

import org.junit.Test;
import org.restcomm.media.rtp.RtpSession;
import org.restcomm.media.rtp.sdp.SdpBuilder;
import org.restcomm.media.sdp.SessionDescription;

import static org.mockito.Mockito.*;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class GenerateLocalSdpActionTest {

    @Test
    public void testGenerateLocalSdp() {
        // given
        final String cname = "cname";
        final boolean inbound = true;
        final String localAddress = "127.0.0.1";
        final String externalAddress = "";
        final RtpSession session = mock(RtpSession.class);
        final SessionDescription localSdp = mock(SessionDescription.class);
        final SdpBuilder sdpBuilder = mock(SdpBuilder.class);
        final RtpConnectionTransitionContext context = new RtpConnectionTransitionContext();
        final RtpConnectionFsm fsm = mock(RtpConnectionFsm.class);
        final RtpConnectionContext globalContext = mock(RtpConnectionContext.class);
        final GenerateLocalSdpAction action = new GenerateLocalSdpAction();

        context.set(RtpConnectionTransitionParameter.CNAME, cname);
        context.set(RtpConnectionTransitionParameter.INBOUND, inbound);
        context.set(RtpConnectionTransitionParameter.BIND_ADDRESS, localAddress);
        context.set(RtpConnectionTransitionParameter.EXTERNAL_ADDRESS, externalAddress);
        context.set(RtpConnectionTransitionParameter.RTP_SESSION, session);
        context.set(RtpConnectionTransitionParameter.SDP_BUILDER, sdpBuilder);

        when(fsm.getContext()).thenReturn(globalContext);
        when(sdpBuilder.buildSessionDescription(!inbound, cname, localAddress, externalAddress, session)).thenReturn(localSdp);

        // when
        action.execute(RtpConnectionState.NEGOTIATING_SESSION, RtpConnectionState.GENERATING_LOCAL_SDP, RtpConnectionEvent.SESSION_NEGOTIATED, context, fsm);

        // then
        verify(sdpBuilder, times(1)).buildSessionDescription(!inbound, cname, localAddress, externalAddress, session);
        assertEquals(localSdp, context.get(RtpConnectionTransitionParameter.LOCAL_SDP, SessionDescription.class));
        assertNull(context.get(RtpConnectionTransitionParameter.ERROR, Throwable.class));
        verify(fsm, times(1)).fire(RtpConnectionEvent.GENERATED_LOCAL_SDP, context);
        verify(globalContext).setLocalDescription(localSdp);
    }

    @Test
    public void testGenerateLocalSdpFailure() {
        // given
        final String cname = "cname";
        final boolean inbound = true;
        final String localAddress = "127.0.0.1";
        final String externalAddress = "";
        final RtpSession session = mock(RtpSession.class);
        final SdpBuilder sdpBuilder = mock(SdpBuilder.class);
        final RtpConnectionTransitionContext context = new RtpConnectionTransitionContext();
        final RtpConnectionFsm fsm = mock(RtpConnectionFsm.class);
        final RtpConnectionContext globalContext = mock(RtpConnectionContext.class);
        final GenerateLocalSdpAction action = new GenerateLocalSdpAction();

        context.set(RtpConnectionTransitionParameter.CNAME, cname);
        context.set(RtpConnectionTransitionParameter.INBOUND, inbound);
        context.set(RtpConnectionTransitionParameter.BIND_ADDRESS, localAddress);
        context.set(RtpConnectionTransitionParameter.EXTERNAL_ADDRESS, externalAddress);
        context.set(RtpConnectionTransitionParameter.RTP_SESSION, session);
        context.set(RtpConnectionTransitionParameter.SDP_BUILDER, sdpBuilder);

        when(fsm.getContext()).thenReturn(globalContext);
        when(sdpBuilder.buildSessionDescription(!inbound, cname, localAddress, externalAddress, session)).thenThrow(new RuntimeException());

        // when
        action.execute(RtpConnectionState.NEGOTIATING_SESSION, RtpConnectionState.GENERATING_LOCAL_SDP, RtpConnectionEvent.SESSION_NEGOTIATED, context, fsm);

        // then
        verify(sdpBuilder, times(1)).buildSessionDescription(!inbound, cname, localAddress, externalAddress, session);
        assertNull(context.get(RtpConnectionTransitionParameter.LOCAL_SDP, SessionDescription.class));
        verify(globalContext, never()).setLocalDescription(any(SessionDescription.class));
        assertNotNull(context.get(RtpConnectionTransitionParameter.ERROR, Throwable.class));
        verify(fsm, times(1)).fire(RtpConnectionEvent.FAILURE, context);
    }

}
