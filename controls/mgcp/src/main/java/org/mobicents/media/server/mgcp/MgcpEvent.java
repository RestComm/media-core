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

package org.mobicents.media.server.mgcp;

import java.net.SocketAddress;
import org.mobicents.media.server.mgcp.message.MgcpMessage;
import org.mobicents.media.server.spi.listener.Event;

/**
 * Mgcp protocol event.
 * 
 * @author kulikov
 */
public interface MgcpEvent extends Event<MgcpProvider> {
    //Event types constants
    public final static int REQUEST = 1;
    public final static int RESPONSE = 2;
    
    /**
     * Gets the type of this event.
     * 
     * @return the event type constant
     */
    public int getEventID();
    
    /**
     * Gets the message associated with this event.
     * 
     * @return the MGCP message.
     */
    public MgcpMessage getMessage();
    
    /**
     * Gets the address from which the message was received.
     * 
     * @return the address object.
     */
    public SocketAddress getAddress();
    
    /**
     * Allows to recycle this event object and thus prevent unnecessary GC.
     */
    public void recycle();
}
