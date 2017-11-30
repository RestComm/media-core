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

import com.google.common.util.concurrent.FutureCallback;
import org.restcomm.media.control.mgcp.command.param.NotifiedEntity;
import org.restcomm.media.control.mgcp.pkg.MgcpEventObserver;
import org.restcomm.media.control.mgcp.pkg.MgcpRequestedEvent;
import org.restcomm.media.control.mgcp.signal.BriefSignal;
import org.restcomm.media.control.mgcp.signal.TimeoutSignal;

import java.util.*;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class NotificationCenterContext {

    private static final NotifiedEntity DEFAULT_NOTIFIED_ENTITY = new NotifiedEntity();

    private String requestId;
    private String endpointId;
    private NotifiedEntity notifiedEntity;
    private final Set<MgcpRequestedEvent> requestedEvents;
    private final Set<TimeoutSignal> timeoutSignals;
    private final Queue<BriefSignal> pendingBriefSignals;
    private BriefSignal activeBriefSignal;

    private SignalQuarantine quarantine;

    private FutureCallback<Void> deactivationCallback;

    private final Set<MgcpEventObserver> eventObservers;

    public NotificationCenterContext() {
        this.requestId = "";
        this.endpointId = "";
        this.notifiedEntity = DEFAULT_NOTIFIED_ENTITY;
        this.requestedEvents = new HashSet<>(5);
        this.timeoutSignals = new HashSet<>(5);
        this.pendingBriefSignals = new ArrayDeque<>(5);
        this.activeBriefSignal = null;

        this.eventObservers = new HashSet<>(5);
    }

    protected String getRequestId() {
        return requestId;
    }

    protected void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getEndpointId() {
        return endpointId;
    }

    public void setEndpointId(String endpointId) {
        this.endpointId = endpointId;
    }

    protected NotifiedEntity getNotifiedEntity() {
        return notifiedEntity;
    }

    protected void setNotifiedEntity(NotifiedEntity notifiedEntity) {
        this.notifiedEntity = notifiedEntity;
    }

    protected boolean isEventRequested(String eventName) {
        for (MgcpRequestedEvent requestedEvent : this.requestedEvents) {
            if (requestedEvent.getQualifiedName().equalsIgnoreCase(eventName)) {
                return true;
            }
        }
        return false;
    }

    protected Set<MgcpRequestedEvent> getRequestedEvents() {
        return requestedEvents;
    }

    protected void setRequestedEvents(MgcpRequestedEvent[] events) {
        this.requestedEvents.clear();
        Collections.addAll(this.requestedEvents, events);
    }

    protected Set<TimeoutSignal> getTimeoutSignals() {
        return timeoutSignals;
    }

    protected void setTimeoutSignals(Collection<TimeoutSignal> signals) {
        this.timeoutSignals.clear();
        this.timeoutSignals.addAll(signals);
    }

    protected Queue<BriefSignal> getPendingBriefSignals() {
        return pendingBriefSignals;
    }

    protected void setPendingBriefSignals(Collection<BriefSignal> signals) {
        this.pendingBriefSignals.clear();
        this.pendingBriefSignals.addAll(signals);
    }

    public BriefSignal getActiveBriefSignal() {
        return activeBriefSignal;
    }

    public void setActiveBriefSignal(BriefSignal activeBriefSignal) {
        this.activeBriefSignal = activeBriefSignal;
    }

    public FutureCallback<Void> getDeactivationCallback() {
        return deactivationCallback;
    }

    public void setDeactivationCallback(FutureCallback<Void> callback) {
        this.deactivationCallback = callback;
    }

    public SignalQuarantine getQuarantine() {
        return quarantine;
    }

    public void setQuarantine(SignalQuarantine quarantine) {
        this.quarantine = quarantine;
    }

    public Set<MgcpEventObserver> getEventObservers() {
        return eventObservers;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("Request Identifier: ").append(this.requestId).append(System.lineSeparator());
        builder.append("Notified Entity: ").append(this.notifiedEntity).append(System.lineSeparator());
        builder.append("Requested Events: ").append(Arrays.toString(this.requestedEvents.toArray())).append(System.lineSeparator());
        builder.append("Requested BR Signals: ").append(Arrays.toString(this.pendingBriefSignals.toArray())).append(System.lineSeparator());
        builder.append("Requested TO Signals: ").append(Arrays.toString(this.timeoutSignals.toArray())).append(System.lineSeparator());
        return builder.toString();
    }

}
