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

import java.io.Closeable;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CancellationException;

import org.restcomm.media.control.mgcp.pkg.MgcpEvent;
import org.restcomm.media.control.mgcp.signal.TimeoutSignal;

import com.google.common.util.concurrent.FutureCallback;

/**
 * Quarantine where signals from a canceled RQNT are kept for historic purposes.
 * <p>
 * Mostly queried by EndSignal signal, trying to raise notification from a TO signal.
 *
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
class SignalQuarantine implements Closeable {

    private final String requestId;
    private final Map<TimeoutSignal, MgcpEvent> quarantinedSignals;
    private final Map<TimeoutSignal, FutureCallback<MgcpEvent>> resultObservers;

    SignalQuarantine(String requestId, Collection<TimeoutSignal> signals) {
        this.requestId = requestId;
        this.quarantinedSignals = new HashMap<>(signals.size());
        for (TimeoutSignal signal : signals) {
            this.quarantinedSignals.put(signal, null);
        }
        this.resultObservers = new HashMap<>(5, 0.9f);
    }

    String getRequestId() {
        return requestId;
    }

    boolean contains(TimeoutSignal signal) {
        return this.quarantinedSignals.containsKey(signal);
    }

    public void getSignalResult(TimeoutSignal signal, FutureCallback<MgcpEvent> callback) {
        if (this.quarantinedSignals.containsKey(signal)) {
            MgcpEvent result = this.quarantinedSignals.get(signal);
            if (result == null) {
                // Register callback in observers list, waiting for the time a result is submitted
                this.resultObservers.put(signal, callback);
            } else {
                // Reply immediately if signal result has already been submitted
                callback.onSuccess(result);
            }
        } else {
            // Reply to callback with failure, since signal is unknown
            IllegalArgumentException t = new IllegalArgumentException("Signal " + signal + " was not requested in RQNT X:" + this.requestId);
            callback.onFailure(t);
        }
    }

    public void getSignalResult(String signal, FutureCallback<MgcpEvent> callback) {
        final Set<TimeoutSignal> keys = this.quarantinedSignals.keySet();
        for (TimeoutSignal key : keys) {
            if (key.getName().equals(signal)) {
                getSignalResult(key, callback);
                return;
            }
        }

        // No matching signal
        // Reply to callback with failure, since signal is unknown
        IllegalArgumentException t = new IllegalArgumentException("Signal " + signal + " was not requested in RQNT X:" + this.requestId);
        callback.onFailure(t);
    }

    void onSignalCompleted(TimeoutSignal signal, MgcpEvent event) {
        final boolean signalExists = this.quarantinedSignals.containsKey(signal);
        if (signalExists) {
            // Set result of the signal
            this.quarantinedSignals.put(signal, event);

            // Reply to any possible callback waiting for signal completion
            final FutureCallback<MgcpEvent> observer = this.resultObservers.remove(signal);
            if (observer != null) {
                observer.onSuccess(event);
            }
        } else {
            throw new IllegalArgumentException("Signal " + signal + " was not requested in RQNT X:" + this.requestId);
        }
    }

    @Override
    public void close() {
        // Warn any pending observers that possible pending operation was cancelled
        if (!this.resultObservers.isEmpty()) {
            final CancellationException exception = new CancellationException("RQNT X:" + this.requestId + " was cancelled.");
            final Iterator<Entry<TimeoutSignal, FutureCallback<MgcpEvent>>> iterator = this.resultObservers.entrySet().iterator();

            while (iterator.hasNext()) {
                final Entry<TimeoutSignal, FutureCallback<MgcpEvent>> entry = iterator.next();
                entry.getValue().onFailure(exception);
                iterator.remove();
            }
        }

        // Clear set of quarantined signals
        this.quarantinedSignals.clear();
    }

    @Override
    public String toString() {
        return new StringBuilder("Quarantined RQNT X:").append(this.requestId).append(" , Signals ").append(this.quarantinedSignals.keySet()).toString();
    }
}
