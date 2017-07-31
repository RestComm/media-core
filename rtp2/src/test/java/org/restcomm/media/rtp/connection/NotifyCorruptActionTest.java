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

import com.google.common.util.concurrent.FutureCallback;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class NotifyCorruptActionTest {

    @Test
    public void testOpen() {
        // given
        final RtpConnectionFsm fsm = mock(RtpConnectionFsm.class);
        final FutureCallback<?> callback = mock(FutureCallback.class);
        final Throwable error = new Exception("test purposes");
        final RtpConnectionTransitionContext context = new RtpConnectionTransitionContext();
        context.set(RtpConnectionTransitionParameter.CALLBACK, callback);
        context.set(RtpConnectionTransitionParameter.ERROR, error);

        // when
        final NotifyCorruptAction action = new NotifyCorruptAction();
        action.execute(RtpConnectionState.PARSING_REMOTE_SDP, RtpConnectionState.OPEN, RtpConnectionEvent.GENERATED_LOCAL_SDP, context, fsm);

        // then
        verify(callback).onFailure(error);
    }

}
