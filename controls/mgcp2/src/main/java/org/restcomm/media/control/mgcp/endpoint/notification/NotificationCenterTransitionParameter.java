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

import org.restcomm.media.control.mgcp.command.param.NotifiedEntity;
import org.restcomm.media.control.mgcp.pkg.MgcpEvent;
import org.restcomm.media.control.mgcp.pkg.MgcpRequestedEvent;
import org.restcomm.media.control.mgcp.signal.MgcpSignal;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public enum NotificationCenterTransitionParameter {

    REQUEST_IDENTIFIER(Integer.class), NOTIFIED_ENTITY(NotifiedEntity.class), REQUESTED_EVENTS(MgcpRequestedEvent[].class), REQUESTED_SIGNALS(MgcpSignal[].class),
    SIGNAL_RESULT(MgcpEvent.class), SIGNAL(MgcpSignal.class), FAILED_SIGNAL(MgcpSignal.class), ERROR(Throwable.class);

    private final Class<?> type;

    private NotificationCenterTransitionParameter(Class<?> type) {
        this.type = type;
    }

    public Class<?> type() {
        return type;
    }

}
