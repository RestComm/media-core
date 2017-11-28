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

package org.restcomm.media.control.mgcp.pkg.au.es;

import com.google.common.util.concurrent.FutureCallback;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpoint;
import org.restcomm.media.control.mgcp.pkg.MgcpEvent;
import org.restcomm.media.control.mgcp.pkg.au.SignalParameters;
import org.restcomm.media.control.mgcp.signal.MgcpSignal;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com) on 03/10/2017
 */
public class EndSignalTest {

    @Test
    @SuppressWarnings("unchecked")
    public void testEndSignal() {
        // given
        final String requestId = "12345";
        final String targetSignal = "pa";
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put(SignalParameters.SIGNAL.symbol(), "pa");

        // when - execute signal
        final FutureCallback<Void> callback = mock(FutureCallback.class);
        final EndSignal signal = new EndSignal(endpoint, requestId, parameters);

        signal.execute(callback);

        // then
        final ArgumentCaptor<FutureCallback> callbackCaptor = ArgumentCaptor.forClass(FutureCallback.class);
        verify(endpoint).endSignal(eq(requestId), eq(targetSignal), callbackCaptor.capture());

        // when - endpoint replies with success
        final MgcpEvent event = mock(MgcpEvent.class);
        callbackCaptor.getValue().onSuccess(event);

        // then
        verify(endpoint).onEvent(signal, event);
        verify(callback).onSuccess(null);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testEndUnknownSignal() {
        // given
        final String requestId = "12345";
        final String targetSignal = "pa";
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put(SignalParameters.SIGNAL.symbol(), "pa");

        // when - execute signal
        final FutureCallback<Void> callback = mock(FutureCallback.class);
        final EndSignal signal = new EndSignal(endpoint, requestId, parameters);

        signal.execute(callback);

        // then
        final ArgumentCaptor<FutureCallback> callbackCaptor = ArgumentCaptor.forClass(FutureCallback.class);
        verify(endpoint).endSignal(eq(requestId), eq(targetSignal), callbackCaptor.capture());

        // when - endpoint replies with failure
        Throwable t = new Exception("test purposes");
        callbackCaptor.getValue().onFailure(t);

        // then
        verify(endpoint, never()).onEvent(any(MgcpSignal.class), any(MgcpEvent.class));
        verify(callback).onFailure(t);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSignalParameterMissing() {
        // given
        final String requestId = "12345";
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        final Map<String, String> parameters = new HashMap<>(5);

        // when - execute signal
        final FutureCallback<Void> callback = mock(FutureCallback.class);
        final EndSignal signal = new EndSignal(endpoint, requestId, parameters);

        signal.execute(callback);

        // then
        verify(endpoint, never()).endSignal(eq(requestId), any(String.class), any(FutureCallback.class));
        verify(callback).onFailure(any(IllegalArgumentException.class));
    }

}
