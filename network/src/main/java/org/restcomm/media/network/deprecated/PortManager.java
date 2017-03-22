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

package org.restcomm.media.network.deprecated;

/**
 * Utility class that manages a range of ports.
 *
 * The range of available port is identified by a pair of integer constants. Method <code>next</code> consequently peeks up even
 * port either from beginning or from the end of range.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * @author yulian oifa
 *
 */
public interface PortManager {

    /**
     * Gets the low boundary of available range.
     * 
     * @return Minimum port number
     */
    int getLowest();

    /**
     * Gets the upper boundary of available range.
     * 
     * @return Maximum port number
     */
    int getHighest();

    /**
     * Gets the current port.
     * 
     * @return The current port.
     */
    public int current();

    /**
     * Peeks into the next available port. Does not move the internal pointer.
     * 
     * @return The next available port.
     */
    public int peek();

    /**
     * Moves to the next available port.
     * 
     * @return The next available port.
     */
    public int next();

}
