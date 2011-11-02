/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

package org.mobicents.media.server;

import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.ConnectionEvent;

/**
 * Implementation for connection event.
 * 
 * @author kulikov
 */
public class ConnectionEventImpl implements ConnectionEvent {
    //event identifier
    private int id;

    //event source
    private Connection source;

    /**
     * Creates new event object.
     *
     * @param id event identifier
     * @param source event source
     */
    protected ConnectionEventImpl(int id, Connection source) {
        this.id = id;
        this.source = source;
    }

    /**
     * (Non Java-doc.)
     *
     * @see org.mobicents.media.server.spi.ConnectionEvent#getId()
     */
    public int getId() {
        return this.id;
    }

    /**
     * (Non Java-doc.)
     *
     * @see org.mobicents.media.server.spi.ConnectionEvent#getSource()
     */
    public Connection getSource() {
        return this.source;
    }

}
