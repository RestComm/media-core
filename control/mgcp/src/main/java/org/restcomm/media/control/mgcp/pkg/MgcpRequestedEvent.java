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

package org.restcomm.media.control.mgcp.pkg;

/**
 * Holds information about a RequestedEvent from an MGCP RQNT command.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpRequestedEvent {

    private static final String[] EMPTY_PARAMS = new String[0];

    private final int requestId;
    private final String packageName;
    private final String eventType;
    private final MgcpActionType action;
    private final int connectionId;
    private final String[] parameters;

    public MgcpRequestedEvent(int requestId, String packageName, String eventType, MgcpActionType action, int connectionId, String... parameters) {
        this.requestId = requestId;
        this.packageName = packageName;
        this.eventType = eventType;
        this.action = action;
        this.connectionId = connectionId;
        this.parameters = (parameters == null) ? EMPTY_PARAMS : parameters;
    }

    public MgcpRequestedEvent(int requestId, String packageName, String eventType, MgcpActionType action) {
        this(requestId, packageName, eventType, action, 0, EMPTY_PARAMS);
    }

    public int getRequestId() {
        return requestId;
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

    public int getConnectionId() {
        return connectionId;
    }

    public String[] getParameters() {
        return parameters;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(this.packageName).append("/").append(this.eventType);
        if (this.connectionId > 0) {
            builder.append("@").append(Integer.toHexString(this.connectionId));
        }
        builder.append("(").append(this.action).append(")");
        if (this.parameters.length > 0) {
            builder.append("(");
            for (int i = 0; i < parameters.length; i++) {
                builder.append(this.parameters[i]);
                if (i < parameters.length - 1) {
                    builder.append(",");
                }
            }
            builder.append(")");
        }
        return builder.toString();
    }

}
