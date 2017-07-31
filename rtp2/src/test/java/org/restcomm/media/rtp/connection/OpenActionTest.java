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
import org.restcomm.media.sdp.SessionDescription;

import com.google.common.util.concurrent.FutureCallback;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class OpenActionTest {

    @Test
    @SuppressWarnings("unchecked")
    public void testOpen() {
        // given
        final RtpConnectionFsm fsm = mock(RtpConnectionFsm.class);
        final FutureCallback<String> callback = mock(FutureCallback.class);
        final SessionDescription localSdp = mock(SessionDescription.class);
        final String localSdpString = "local_sdp";
        final RtpConnectionTransitionContext context = new RtpConnectionTransitionContext();
        context.set(RtpConnectionTransitionParameter.CALLBACK, callback);
        context.set(RtpConnectionTransitionParameter.LOCAL_SDP, localSdp);

        when(localSdp.toString()).thenReturn(localSdpString);

        // when
        final OpenAction action = new OpenAction();
        action.execute(RtpConnectionState.GENERATING_LOCAL_SDP, RtpConnectionState.OPEN, RtpConnectionEvent.GENERATED_LOCAL_SDP, context, fsm);

        // then
        verify(callback).onSuccess(localSdpString);
    }

}
