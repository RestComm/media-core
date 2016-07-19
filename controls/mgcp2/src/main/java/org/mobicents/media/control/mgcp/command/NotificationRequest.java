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

package org.mobicents.media.control.mgcp.command;

import java.util.ArrayDeque;
import java.util.Queue;

import org.mobicents.media.control.mgcp.command.param.NotifiedEntity;
import org.mobicents.media.control.mgcp.pkg.MgcpSignal;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class NotificationRequest {

    private final int transactionId;
    private final String requestIdentifier;
    private final NotifiedEntity notifiedEntity;
    private final String[] requestedEvents;
    private final Queue<MgcpSignal> requestedSignals;

    public NotificationRequest(int transactionId, String requestIdentifier, NotifiedEntity notifiedEntity,
            String[] requestedEvents, MgcpSignal... requestedSignals) {
        super();
        this.transactionId = transactionId;
        this.requestIdentifier = requestIdentifier;
        this.notifiedEntity = notifiedEntity;
        this.requestedEvents = requestedEvents;
        this.requestedSignals = new ArrayDeque<>(requestedSignals.length);
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

    public boolean isListening(String event) {
        for (String evt : this.requestedEvents) {
            if (evt.equalsIgnoreCase(event)) {
                return true;
            }
        }
        return false;
    }

    public MgcpSignal pollSignal() {
        return this.requestedSignals.poll();
    }
}
