/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
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

package org.restcomm.media.control.mgcp.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.restcomm.media.control.mgcp.command.param.NotifiedEntity;
import org.restcomm.media.control.mgcp.pkg.MgcpRequestedEvent;
import org.restcomm.media.control.mgcp.signal.MgcpSignal;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class NotificationRequest {

    private final int transactionId;
    private final String requestIdentifier;
    private final NotifiedEntity notifiedEntity;
    private final MgcpRequestedEvent[] requestedEvents;
    private final List<MgcpSignal<?>> requestedSignals;

    public NotificationRequest(int transactionId, String requestIdentifier, NotifiedEntity notifiedEntity, MgcpRequestedEvent[] requestedEvents, MgcpSignal<?>... requestedSignals) {
        super();
        this.transactionId = transactionId;
        this.requestIdentifier = requestIdentifier;
        this.notifiedEntity = notifiedEntity;
        this.requestedEvents = requestedEvents;
        this.requestedSignals = new ArrayList<>(requestedSignals.length);
        Collections.addAll(this.requestedSignals, requestedSignals);
    }

    public int getTransactionId() {
        return transactionId;
    }

    public String getRequestIdentifier() {
        return requestIdentifier;
    }

    public NotifiedEntity getNotifiedEntity() {
        return notifiedEntity;
    }

    public MgcpRequestedEvent[] getRequestedEvents() {
        return requestedEvents;
    }

    public List<MgcpSignal<?>> getRequestedSignals() {
        return this.requestedSignals;
    }

    public boolean isListening(String event) {
        for (MgcpRequestedEvent evt : this.requestedEvents) {
            if (evt.getQualifiedName().equalsIgnoreCase(event)) {
                return true;
            }
        }
        return false;
    }
}
