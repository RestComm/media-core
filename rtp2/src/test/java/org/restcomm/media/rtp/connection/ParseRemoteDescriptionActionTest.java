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

import org.junit.Test;

import static org.junit.Assert.*;

import org.restcomm.media.sdp.SdpException;
import org.restcomm.media.sdp.SessionDescription;
import org.restcomm.media.sdp.SessionDescriptionParser;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class ParseRemoteDescriptionActionTest {

    @Test
    public void testParsingSuccess() throws SdpException {
        // given
        final RtpConnectionFsm fsm = mock(RtpConnectionFsm.class);
        final RtpConnectionContext globalContext = mock(RtpConnectionContext.class);
        final String remoteSdpString = "xyz";
        final SessionDescription remoteSdp = mock(SessionDescription.class);
        final SessionDescriptionParser parser = mock(SessionDescriptionParser.class);
        final RtpConnectionTransitionContext context = new RtpConnectionTransitionContext();
        final ParseRemoteDescriptionAction action = new ParseRemoteDescriptionAction();

        context.set(RtpConnectionTransitionParameter.REMOTE_SDP_STRING, remoteSdpString);
        context.set(RtpConnectionTransitionParameter.SDP_PARSER, parser);

        when(fsm.getContext()).thenReturn(globalContext);
        when(parser.parse(remoteSdpString)).thenReturn(remoteSdp);

        // when
        action.execute(RtpConnectionState.IDLE, RtpConnectionState.PARSING_REMOTE_SDP, RtpConnectionEvent.OPEN, context, fsm);

        // then
        verify(parser, times(1)).parse(remoteSdpString);
        assertEquals(remoteSdp, context.get(RtpConnectionTransitionParameter.REMOTE_SDP, SessionDescription.class));
        verify(globalContext).setRemoteDescription(remoteSdp);
        verify(fsm, times(1)).fire(RtpConnectionEvent.PARSED_REMOTE_SDP, context);
    }

    @Test
    public void testParsingFailure() throws SdpException {
        // given
        final RtpConnectionFsm fsm = mock(RtpConnectionFsm.class);
        final RtpConnectionContext globalContext = mock(RtpConnectionContext.class);
        final String remoteSdpString = "xyz";
        final SessionDescriptionParser parser = mock(SessionDescriptionParser.class);
        final RtpConnectionTransitionContext context = new RtpConnectionTransitionContext();
        final ParseRemoteDescriptionAction action = new ParseRemoteDescriptionAction();

        context.set(RtpConnectionTransitionParameter.REMOTE_SDP_STRING, remoteSdpString);
        context.set(RtpConnectionTransitionParameter.SDP_PARSER, parser);

        when(fsm.getContext()).thenReturn(globalContext);
        final SdpException exception = new SdpException("testing purposes");
        when(parser.parse(remoteSdpString)).thenThrow(exception);

        // when
        action.execute(RtpConnectionState.IDLE, RtpConnectionState.PARSING_REMOTE_SDP, RtpConnectionEvent.OPEN, context, fsm);

        // then
        verify(parser, times(1)).parse(remoteSdpString);
        assertNull(context.get(RtpConnectionTransitionParameter.REMOTE_SDP, SessionDescription.class));
        verify(globalContext, never()).setRemoteDescription(any(SessionDescription.class));
        assertEquals(exception, context.get(RtpConnectionTransitionParameter.ERROR, Throwable.class));
        verify(fsm, times(1)).fire(RtpConnectionEvent.FAILURE, context);
    }

}
