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

package org.restcomm.media.control.mgcp.connection.local;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.Test;
import org.restcomm.media.rtp.LocalDataChannel;

import com.google.common.util.concurrent.FutureCallback;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class NotifySuccessActionTest {

    @Test
    @SuppressWarnings("unchecked")
    public void testNotifySuccess() throws IOException {
        // given
        final int identifier = 1;
        final int callIdentifier = 1;
        final LocalDataChannel dataChannel = mock(LocalDataChannel.class);
        final MgcpLocalConnectionContext context = new MgcpLocalConnectionContext(identifier, callIdentifier, dataChannel);
        final MgcpLocalConnectionFsm fsm = mock(MgcpLocalConnectionFsm.class);
        final FutureCallback<Object> callback = mock(FutureCallback.class);

        when(fsm.getContext()).thenReturn(context);

        // when
        final MgcpLocalConnectionTransitionContext txContext = new MgcpLocalConnectionTransitionContext();
        txContext.set(MgcpLocalConnectionParameter.CALLBACK, callback);

        final NotifySuccessAction action = new NotifySuccessAction();
        action.execute(MgcpLocalConnectionState.IDLE, MgcpLocalConnectionState.OPEN, MgcpLocalConnectionEvent.OPEN, txContext, fsm);

        // then
        verify(callback).onSuccess(null);
    }

}
