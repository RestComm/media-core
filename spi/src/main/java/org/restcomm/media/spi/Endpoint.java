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

package org.restcomm.media.spi;

import org.restcomm.media.Component;
import org.restcomm.media.ComponentType;
import org.restcomm.media.scheduler.PriorityQueueScheduler;

/**
 * The basic implementation of the endpoint.
 *
 * An Endpoint is a logical representation of a physical entity, such as an
 * analog phone or a channel in a trunk. Endpoints are sources or sinks of data
 * and can be physical or virtual. Physical endpoint creation requires hardware
 * installation while software is sufficient for creating a virtual Endpoint.
 * An interface on a gateway that terminates a trunk connected to a PSTN switch
 * is an example of a physical Endpoint. An audio source in an audio-content
 * server is an example of a virtual Endpoint.
 *
 * @author Yulian Oifa
 * @author amit.bhayani
 */
public interface Endpoint {
    
    /**
     * Gets the local name attribute.
     *
     * @return the local name.
     */
    public String getLocalName();

    /**
     * Gets the current state of the endpoint
     * 
     * @return the state of the endpoint.
     */
    public EndpointState getState();
    
    /**
     * Starts endpoint.
     */
    public void start() throws ResourceUnavailableException;
    
    /**
     * Terminates endpoint's execution.
     */
    public void stop();
    
    /**
     * Creates new connection with specified mode.
     *
     * @param type transport type
     */
    public Connection createConnection(ConnectionType type,Boolean isLocal)
            throws ResourceUnavailableException;

    /**
     * Deletes specified connection.
     *
     * @param connection the connection to be deleted.
     */
    public void deleteConnection(Connection connection);

    /**
     * Deletes specified connection.
     *
     * @param connection the connection to be deleted.
     */
    public void deleteConnection(Connection connection,ConnectionType type);
    
    /**
     * Notifies that mode has been update on one of connections
     *
     * @param connection the connection to be deleted.
     */
    public void modeUpdated(ConnectionMode oldMode,ConnectionMode newMode);
    
    /**
     * Deletes all connection associated with this Endpoint.
     */
    public void deleteAllConnections();
    
    /**
     * Gets number of active connections associated with this Endpoint.
     */
    public int getActiveConnectionsCount();
    
    /**
     * Configure Endpoint.
     */
    public void configure(boolean isALaw);
    
    /**
     * Gets the scheduler
     */
    public PriorityQueueScheduler getScheduler();
    
    /**
     * Sets the scheduler
     */
    public void setScheduler(PriorityQueueScheduler scheduler);
    
    /**
     * Provides access to the specific resource of the endpoint.
     * 
     * @param component type of the requested resource
     * @return The component implementing resource.
     */
    public Component getResource(MediaType mediaType, ComponentType componentType);
    
    /**
     * Provides access to the specific resource of the endpoint.
     * 
     * @param component type of the requested resource
     * @return The component implementing resource.
     */
    public boolean hasResource(MediaType mediaType, ComponentType componentType);
    
    /**
     * Allows to release resource when not needed anymore
     * 
     * @param component type of the requested resource
     * @return The component implementing resource.
     */
    public void releaseResource(MediaType mediaType, ComponentType componentType);
}
