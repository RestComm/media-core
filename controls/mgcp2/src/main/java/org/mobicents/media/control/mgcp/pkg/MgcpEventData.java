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
 * Data accessor for {@link MgcpEvent}.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public interface MgcpEventData {

    /**
     * Gets the package the event belongs to.
     * 
     * @return The package symbol
     */
    String getPackage();

    /**
     * Gets the symbol representing the event.
     * 
     * @return The event symbol
     */
    String getSymbol();

    /**
     * Gets the name of the signal who fired the event.
     * 
     * @return The name of the signal
     */
    String getSignal();
    
    /**
     * Gets a parameter from the event.
     * 
     * @param type The type of parameter to be returned.
     * @return The value of the parameter. Returns null if no such parameter exists.
     */
    String getParameter(String type);

}
