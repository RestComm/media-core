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

package org.restcomm.media.control.mgcp.endpoint.notification;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import java.util.Set;
import java.util.concurrent.CancellationException;

import com.google.common.util.concurrent.FutureCallback;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.internal.util.collections.Sets;
import org.restcomm.media.control.mgcp.pkg.MgcpEvent;
import org.restcomm.media.control.mgcp.signal.TimeoutSignal;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class SignalQuarantineTest {

    @Test
    public void testGetImmediateResult() {
        // given
        final String requestId = "1A35";
        final TimeoutSignal signal1 = mock(TimeoutSignal.class);
        final TimeoutSignal signal2 = mock(TimeoutSignal.class);
        final TimeoutSignal signal3 = mock(TimeoutSignal.class);
        final Set<TimeoutSignal> signals = Sets.newSet(signal1, signal2, signal3);

        final MgcpEvent event = mock(MgcpEvent.class);

        final SignalQuarantine quarantine = new SignalQuarantine(requestId, signals);

        // when
        quarantine.onSignalCompleted(signal1, event);

        final FutureCallback resultCallback = mock(FutureCallback.class);
        quarantine.getSignalResult(signal1, resultCallback);

        // then
        verify(resultCallback).onSuccess(event);
    }

    @Test
    public void testGetDelayedResult() {
        // given
        final String requestId = "1A35";
        final TimeoutSignal signal1 = mock(TimeoutSignal.class);
        final TimeoutSignal signal2 = mock(TimeoutSignal.class);
        final TimeoutSignal signal3 = mock(TimeoutSignal.class);
        final Set<TimeoutSignal> signals = Sets.newSet(signal1, signal2, signal3);

        final MgcpEvent event = mock(MgcpEvent.class);

        final SignalQuarantine quarantine = new SignalQuarantine(requestId, signals);

        // when - request signal result
        final FutureCallback resultCallback = mock(FutureCallback.class);
        quarantine.getSignalResult(signal1, resultCallback);

        // then
        verify(resultCallback, never()).onSuccess(any(MgcpEvent.class));

        // when - submit signal result
        quarantine.onSignalCompleted(signal1, event);

        // then
        verify(resultCallback).onSuccess(event);
    }

    @Test
    public void testNotifyPendingObserversOnClose() {
        // given
        final String requestId = "1A35";
        final TimeoutSignal signal1 = mock(TimeoutSignal.class);
        final TimeoutSignal signal2 = mock(TimeoutSignal.class);
        final TimeoutSignal signal3 = mock(TimeoutSignal.class);
        final Set<TimeoutSignal> signals = Sets.newSet(signal1, signal2, signal3);

        final MgcpEvent event1 = mock(MgcpEvent.class);
        final MgcpEvent event2 = mock(MgcpEvent.class);

        final SignalQuarantine quarantine = new SignalQuarantine(requestId, signals);

        // when - request signal result
        final FutureCallback resultCallback = mock(FutureCallback.class);
        quarantine.getSignalResult(signal1, resultCallback);

        quarantine.close();

        // then
        verify(resultCallback).onFailure(any(CancellationException.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnknownSignalCompletion() {
        // given
        final String requestId = "1A35";
        final TimeoutSignal signal1 = mock(TimeoutSignal.class);
        final TimeoutSignal signal2 = mock(TimeoutSignal.class);
        final TimeoutSignal signal3 = mock(TimeoutSignal.class);
        final Set<TimeoutSignal> signals = Sets.newSet(signal1, signal2);

        final MgcpEvent event = mock(MgcpEvent.class);

        final SignalQuarantine quarantine = new SignalQuarantine(requestId, signals);

        // when
        quarantine.onSignalCompleted(signal3, event);
    }

    @Test
    public void testGetResultOfUnknownSignal() {
        // given
        final String requestId = "1A35";
        final TimeoutSignal signal1 = mock(TimeoutSignal.class);
        final TimeoutSignal signal2 = mock(TimeoutSignal.class);
        final TimeoutSignal signal3 = mock(TimeoutSignal.class);
        final Set<TimeoutSignal> signals = Sets.newSet(signal1, signal2);

        final MgcpEvent event = mock(MgcpEvent.class);

        final SignalQuarantine quarantine = new SignalQuarantine(requestId, signals);

        // when
        final FutureCallback resultCallback = mock(FutureCallback.class);
        quarantine.getSignalResult(signal3, resultCallback);

        // then
        verify(resultCallback).onFailure(any(IllegalArgumentException.class));
    }

}
