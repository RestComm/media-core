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

package org.restcomm.media.control.mgcp.signal;

import com.google.common.util.concurrent.FutureCallback;

/**
 * A Call Agent may request certain signals to be applied to an endpoint (e.g., dial-tone) or connection.
 * 
 * <p>
 * Signals are divided into different types:
 * <ul>
 * <li>TIMEOUT (TO) - Once applied, these signals last until they are either cancelled or a signal-specific period of time has
 * elapsed. In later case, it raises an event.</li>
 * <li>BRIEF (BR) - The duration of these signals is normally so short that they stop on their own. Active BR signals cannot be
 * canceled.</li>
 * <li>ON/OFF (OO) - Once applied, these signals last until they are turned off.</li>
 * </ul>
 * 
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public interface MgcpSignal<T> {

    String getRequestId();

    void execute(FutureCallback<T> callback);
    
    @Override
    boolean equals(Object obj);

}
