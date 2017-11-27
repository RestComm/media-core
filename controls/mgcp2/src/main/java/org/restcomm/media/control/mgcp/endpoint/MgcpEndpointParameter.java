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

package org.restcomm.media.control.mgcp.endpoint;

import org.restcomm.media.control.mgcp.command.rqnt.NotificationRequest;
import org.restcomm.media.control.mgcp.connection.MgcpConnection;
import org.restcomm.media.control.mgcp.pkg.MgcpEventObserver;

import com.google.common.util.concurrent.FutureCallback;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public enum MgcpEndpointParameter {

    CALL_ID(Integer.class),
    CONNECTION_ID(Integer.class),
    CONNECTION_COUNT(Integer.class),
    REGISTERED_CONNECTION(MgcpConnection.class),
    EVENT_OBSERVER(MgcpEventObserver.class),
    UNREGISTERED_CONNECTIONS(MgcpConnection[].class), 
    REQUESTED_NOTIFICATION(NotificationRequest.class),
    CALLBACK(FutureCallback.class);

    private final Class<?> type;

    private MgcpEndpointParameter(Class<?> type) {
        this.type = type;
    }

    public Class<?> type() {
        return type;
    }

}
