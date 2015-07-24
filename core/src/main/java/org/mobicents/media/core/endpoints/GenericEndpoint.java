/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
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

package org.mobicents.media.core.endpoints;

import org.apache.log4j.Logger;
import org.mobicents.media.Component;
import org.mobicents.media.ComponentType;
import org.mobicents.media.core.ResourcesPool;
import org.mobicents.media.server.concurrent.ConcurrentMap;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.ConnectionType;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.spi.EndpointState;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.RelayType;
import org.mobicents.media.server.spi.ResourceUnavailableException;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public abstract class GenericEndpoint implements Endpoint {

    // Core elements
    private ResourcesPool resourcesPool;
    private Scheduler scheduler;

    // Endpoint properties
    private final String localName;
    private final RelayType relayType;
    private final ConcurrentMap<Connection> connections;
    private EndpointState state;
    protected MediaGroup mediaGroup;

    public GenericEndpoint(String localName, RelayType relayType) {
        this.localName = localName;
        this.relayType = relayType;
        this.connections = new ConcurrentMap<Connection>();
        this.state = EndpointState.READY;
    }

    @Override
    public String getLocalName() {
        return this.localName;
    }

    @Override
    public EndpointState getState() {
        return this.state;
    }

    public void setState(EndpointState state) {
        this.state = state;
    }

    @Override
    public RelayType getRelayType() {
        return this.relayType;
    }

    @Override
    public Scheduler getScheduler() {
        return this.scheduler;
    }

    @Override
    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public void setResourcesPool(ResourcesPool resourcesPool) {
        this.resourcesPool = resourcesPool;
    }

    @Override
    public void start() throws ResourceUnavailableException {
        if (scheduler == null) {
            throw new ResourceUnavailableException("Scheduler is not available");
        }

        if (resourcesPool == null) {
            throw new ResourceUnavailableException("Resources pool is not available");
        }

        // create connections subsystem
        this.mediaGroup = new MediaGroup(resourcesPool, this);
    }

    @Override
    public void stop() {
        mediaGroup.releaseAll();
        deleteAllConnections();
        // TODO: unregister at scheduler level
        getLogger().info("Endpoint " + localName + "has stopped.");
    }

    @Override
    public Connection createConnection(ConnectionType type, Boolean isLocal) throws ResourceUnavailableException {
        try {
            Connection connection = resourcesPool.newConnection(type, isLocal);
            connection.setEndpoint(this);
            connection.halfOpen();
            connections.put(connection.getId(), connection);
            return connection;
        } catch (Exception e) {
            getLogger().error("Could not create a new connection: " + e.getMessage(), e);
            throw new ResourceUnavailableException(e.getMessage());
        }
    }

    @Override
    public void deleteConnection(Connection connection) {
        connection.close();
    }

    @Override
    public void deleteConnection(Connection connection, ConnectionType type) {
        // TODO Auto-generated method stub

    }

    @Override
    public void modeUpdated(ConnectionMode oldMode, ConnectionMode newMode) {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteAllConnections() {
        // TODO Auto-generated method stub

    }

    @Override
    public int getActiveConnectionsCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void configure(boolean isALaw) {
        // TODO Auto-generated method stub

    }

    @Override
    public Component getResource(MediaType mediaType, ComponentType componentType) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasResource(MediaType mediaType, ComponentType componentType) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void releaseResource(MediaType mediaType, ComponentType componentType) {
        // TODO Auto-generated method stub

    }
    
    protected abstract Logger getLogger();

}
