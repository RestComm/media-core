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
 * MGCP is a modular and extensible protocol, however with extensibility comes the need to manage, identify, and name the
 * individual extensions.
 * <p>
 * This is achieved by the concept of packages, which are simply well-defined groupings of extensions.<br>
 * For example, one package may support a certain group of events and signals, e.g., off-hook and ringing, for analog access
 * lines. Another package may support another group of events and signals for analog access lines or for another type of
 * endpoint such as video.<br>
 * One or more packages may be supported by a given endpoint.
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public interface MgcpPackage {

    /**
     * Gets the name of the MGCP Package.
     * 
     * @return The name of the package.
     */
    String getPackageName();

    /**
     * Gets whether an event is supported by the package.
     * 
     * @param event The event name
     * @return true if supported; false otherwise.
     */
    boolean isEventSupported(String event);
    
    /**
     * Gets the details about a supported event type.
     * @param event The name of the event.
     * @return The event details IF supported. Returns null otherwise.
     */
    MgcpEventType getEventDetails(String event);

}
