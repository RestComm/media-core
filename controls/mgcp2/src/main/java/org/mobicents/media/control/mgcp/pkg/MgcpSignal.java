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

import org.mobicents.media.control.mgcp.command.param.NotifiedEntity;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public interface MgcpSignal extends MgcpEventSubject {

    /**
     * Gets the name of the signal which is composed of the MGCP package plus the signal name.
     * 
     * @return The name of the signal.
     */
    String getName();

    /**
     * Gets the identifier of the request that submitted the signal.
     * 
     * @return The request identifier.
     */
    int getRequestId();

    /**
     * Gets the entity which requested the notification of events resulting from the completion of the signal.
     * <p>
     * This parameter is equal to the NotifiedEntity parameter of the NotificationRequest that triggered this notification. The
     * parameter is absent if there was no such parameter in the triggering request.
     * </p>
     * 
     * @return The entity that submitted the signal. May return null if parameter was not specified in original RQNT command.
     */
    NotifiedEntity getNotifiedEntity();

    /**
     * Gets the type of signal.
     * 
     * @return The type of the signal.
     */
    SignalType getSignalType();

    /**
     * Executes the signal.
     */
    void execute();

    /**
     * Cancels the executing signal.<br>
     * No action is taken if signal is not executing.
     */
    void cancel();

    /**
     * Gets whether the signal is currently executing.
     * 
     * @return <code>true</code> if executing; otherwise returns <code>false</code>
     */
    boolean isExecuting();

}
