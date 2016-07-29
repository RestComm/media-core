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

package org.mobicents.media.control.mgcp.pkg;

/**
 * Holds information about a RequestedEvent from an MGCP RQNT command.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpRequestedEvent {

    private final String packageName;
    private final String eventType;
    private final MgcpActionType action;

    public MgcpRequestedEvent(String packageName, String eventType, MgcpActionType action) {
        this.packageName = packageName;
        this.eventType = eventType;
        this.action = action;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getEventType() {
        return eventType;
    }
    
    public String getQualifiedName() {
        return this.packageName + "/" + this.eventType;
    }

    public MgcpActionType getAction() {
        return action;
    }

    @Override
    public String toString() {
        return this.packageName + "/" + this.eventType + "(" + this.action + ")";
    }

}
