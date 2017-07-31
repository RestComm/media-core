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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.restcomm.media.rtp.RtpSession;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class CloseActionTest {

    @Test
    public void closeActiveSession() {
        // given
        final RtpConnectionTransitionContext context = new RtpConnectionTransitionContext();
        final RtpSession session = mock(RtpSession.class);
        final RtpConnectionFsm fsm = mock(RtpConnectionFsm.class);

        context.set(RtpConnectionTransitionParameter.RTP_SESSION, session);

        // when
        final CloseAction action = new CloseAction();
        action.execute(RtpConnectionState.OPEN, RtpConnectionState.CLOSING, RtpConnectionEvent.CLOSE, context, fsm);

        // then
        ArgumentCaptor<AbstractRtpConnectionActionCallback> captor = ArgumentCaptor.forClass(AbstractRtpConnectionActionCallback.class);
        verify(session).close(captor.capture());

        // when
        captor.getValue().onSuccess(null);

        // then
        verify(fsm).fire(RtpConnectionEvent.SESSION_CLOSED, context);
    }

    @Test
    public void closeWhenSessionIsInactive() {
        // given
        final RtpConnectionTransitionContext context = new RtpConnectionTransitionContext();
        final RtpConnectionFsm fsm = mock(RtpConnectionFsm.class);

        context.set(RtpConnectionTransitionParameter.RTP_SESSION, null);

        // when
        final CloseAction action = new CloseAction();
        action.execute(RtpConnectionState.OPEN, RtpConnectionState.CLOSING, RtpConnectionEvent.CLOSE, context, fsm);

        // then
        verify(fsm).fire(RtpConnectionEvent.CLOSED, context);
    }

}
