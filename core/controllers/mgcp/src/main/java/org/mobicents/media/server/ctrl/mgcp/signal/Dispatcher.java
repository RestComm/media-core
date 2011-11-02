/*
 * JBoss, Home of Professional Open Source
 * Copyright XXXX, Red Hat Middleware LLC, and individual contributors as indicated
 * by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a full listing
 * of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU General Public License, v. 2.0.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License,
 * v. 2.0 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */

package org.mobicents.media.server.ctrl.mgcp.signal;

import jain.protocol.ip.mgcp.message.parms.EventName;
import org.mobicents.media.server.ctrl.mgcp.UnknownActivityException;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.Endpoint;

/**
 * Two way communicator between entities.
 * 
 * @author kulikov
 */
public interface Dispatcher {
    /**
     * Trigered when new event detected.
     * 
     * @param event the event detected.
     */
    public void onEvent(EventName event);
    
    /**
     * Indicates that operation is completed
     */
    public void completed();
    
    /**
     * Request for endpoint.
     * 
     * @return endpoint object.
     */
    public Endpoint getEndpoint();
    
    /**
     * Request for connection.
     * 
     * @param  ID the local identifier of the connection.
     * @return media server connection
     */
    public Connection getConnection(String ID) throws UnknownActivityException;
}
