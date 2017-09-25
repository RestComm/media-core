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

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

import org.restcomm.media.control.mgcp.command.param.NotifiedEntity;
import org.restcomm.media.control.mgcp.pkg.MgcpRequestedEvent;
import org.restcomm.media.control.mgcp.signal.BriefSignal;
import org.restcomm.media.control.mgcp.signal.TimeoutSignal;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class NotificationCenterContext {

    private static final NotifiedEntity DEFAULT_NOTIFIED_ENTITY = new NotifiedEntity();
    private static final Set<MgcpRequestedEvent> EMPTY_EVENTS = new HashSet<>(0);
    private static final Set<TimeoutSignal> EMPTY_TO_SIGNALS = new HashSet<>(0);
    private static final Queue<BriefSignal> EMPTY_BR_SIGNAL = new ArrayDeque<>(0);

    private int requestId;
    private NotifiedEntity notifiedEntity;
    private Set<MgcpRequestedEvent> requestedEvents;
    private Set<TimeoutSignal> timeoutSignals;
    private Queue<BriefSignal> briefSignals;

    public NotificationCenterContext() {
        this.requestId = -1;
        this.notifiedEntity = DEFAULT_NOTIFIED_ENTITY;
        this.requestedEvents = EMPTY_EVENTS;
        this.timeoutSignals = EMPTY_TO_SIGNALS;
        this.briefSignals = EMPTY_BR_SIGNAL;
    }
    
    protected int getRequestId() {
        return requestId;
    }
    
    protected void setRequestId(int requestId) {
        this.requestId = requestId;
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

    protected void setRequestedEvents(Set<MgcpRequestedEvent> requestedEvents) {
        this.requestedEvents = requestedEvents;
    }

    protected Set<TimeoutSignal> getTimeoutSignals() {
        return timeoutSignals;
    }

    protected void setTimeoutSignals(Set<TimeoutSignal> timeoutSignals) {
        this.timeoutSignals = timeoutSignals;
    }

    protected Queue<BriefSignal> getBriefSignals() {
        return briefSignals;
    }

    protected void setBriefSignals(Queue<BriefSignal> briefSignals) {
        this.briefSignals = briefSignals;
    }
    
}
