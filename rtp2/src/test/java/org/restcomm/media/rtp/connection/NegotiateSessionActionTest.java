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

import static org.mockito.Mockito.*;

import static org.junit.Assert.*;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.restcomm.media.rtp.MediaType;
import org.restcomm.media.rtp.RtpSession;
import org.restcomm.media.rtp.session.exception.RtpSessionNegotiationException;
import org.restcomm.media.sdp.SessionDescription;
import org.restcomm.media.sdp.fields.MediaDescriptionField;

import com.google.common.util.concurrent.FutureCallback;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class NegotiateSessionActionTest {

    @Test
    public void testSuccessfulNegotiation() {
        // given
        final RtpConnectionTransitionContext context = new RtpConnectionTransitionContext();
        final RtpConnectionFsm fsm = mock(RtpConnectionFsm.class);
        final NegotiateSessionAction action = new NegotiateSessionAction();

        final RtpSession session = mock(RtpSession.class);
        final SessionDescription remoteSdp = mock(SessionDescription.class);
        final MediaDescriptionField remoteAudio = mock(MediaDescriptionField.class);
        final String audioType = MediaType.AUDIO.name().toLowerCase();

        when(remoteSdp.getMediaDescription(audioType)).thenReturn(remoteAudio);

        context.set(RtpConnectionTransitionParameter.RTP_SESSION, session);
        context.set(RtpConnectionTransitionParameter.REMOTE_SDP, remoteSdp);

        // when
        action.execute(RtpConnectionState.ALLOCATING_SESSION, RtpConnectionState.NEGOTIATING_SESSION, RtpConnectionEvent.ALLOCATED_SESSION, context, fsm);

        // then
        ArgumentCaptor<NegotiateSessionCallback> callbackCaptor = ArgumentCaptor.forClass(NegotiateSessionCallback.class);
        verify(session, only()).negotiate(eq(remoteAudio), callbackCaptor.capture());

        // when
        callbackCaptor.getValue().onSuccess(null);
        
        // then
        verify(fsm, times(1)).fire(RtpConnectionEvent.SESSION_NEGOTIATED, context);
        assertNull(context.get(RtpConnectionTransitionParameter.ERROR, Throwable.class));
    }

    @Test
    public void testFailedNegotiation() {
        // given
        final RtpConnectionTransitionContext context = new RtpConnectionTransitionContext();
        final RtpConnectionFsm fsm = mock(RtpConnectionFsm.class);
        final NegotiateSessionAction action = new NegotiateSessionAction();

        final RtpSession session = mock(RtpSession.class);
        final SessionDescription remoteSdp = mock(SessionDescription.class);
        final MediaDescriptionField remoteAudio = mock(MediaDescriptionField.class);
        final String audioType = MediaType.AUDIO.name().toLowerCase();

        when(remoteSdp.getMediaDescription(audioType)).thenReturn(remoteAudio);

        context.set(RtpConnectionTransitionParameter.RTP_SESSION, session);
        context.set(RtpConnectionTransitionParameter.REMOTE_SDP, remoteSdp);

        // when
        action.execute(RtpConnectionState.ALLOCATING_SESSION, RtpConnectionState.NEGOTIATING_SESSION, RtpConnectionEvent.ALLOCATED_SESSION, context, fsm);

        // then
        ArgumentCaptor<NegotiateSessionCallback> callbackCaptor = ArgumentCaptor.forClass(NegotiateSessionCallback.class);
        verify(session, only()).negotiate(eq(remoteAudio), callbackCaptor.capture());

        // when
        final RtpSessionNegotiationException error = new RtpSessionNegotiationException("test purposes");
        callbackCaptor.getValue().onFailure(error);
        
        // then
        verify(fsm, times(1)).fire(RtpConnectionEvent.FAILURE, context);
        assertEquals(error, context.get(RtpConnectionTransitionParameter.ERROR, Throwable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testNegotiationWhenRemoteAudioIsInexistent() {
        // given
        final RtpConnectionTransitionContext context = new RtpConnectionTransitionContext();
        final RtpConnectionFsm fsm = mock(RtpConnectionFsm.class);
        final NegotiateSessionAction action = new NegotiateSessionAction();

        final RtpSession session = mock(RtpSession.class);
        final SessionDescription remoteSdp = mock(SessionDescription.class);
        final String audioType = MediaType.AUDIO.name().toLowerCase();

        when(remoteSdp.getMediaDescription(audioType)).thenReturn(null);

        context.set(RtpConnectionTransitionParameter.RTP_SESSION, session);
        context.set(RtpConnectionTransitionParameter.REMOTE_SDP, remoteSdp);

        // when
        action.execute(RtpConnectionState.ALLOCATING_SESSION, RtpConnectionState.NEGOTIATING_SESSION, RtpConnectionEvent.ALLOCATED_SESSION, context, fsm);

        // then
        verify(session, never()).negotiate(any(MediaDescriptionField.class), any(FutureCallback.class));
        verify(fsm, times(1)).fire(RtpConnectionEvent.FAILURE, context);
        assertNotNull(context.get(RtpConnectionTransitionParameter.ERROR, Throwable.class));
    }

}
