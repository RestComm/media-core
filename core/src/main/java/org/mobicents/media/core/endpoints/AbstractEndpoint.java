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

package org.mobicents.media.core.endpoints;

import org.apache.log4j.Logger;
import org.mobicents.media.Component;
import org.mobicents.media.ComponentType;
import org.mobicents.media.core.ResourcesPool;
import org.mobicents.media.server.component.Dsp;
import org.mobicents.media.server.concurrent.ConcurrentMap;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.ConnectionType;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.spi.EndpointState;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.RelayType;
import org.mobicents.media.server.spi.ResourceUnavailableException;

/**
 * Basic implementation of the endpoint.
 * 
 * @author yulian oifa
 * @author amit bhayani
 */
public abstract class AbstractEndpoint implements Endpoint {

    // Core elements
    protected ResourcesPool resourcesPool;
    private Scheduler scheduler;

    // Endpoint properties
    private final String localName;
    private RelayType relayType;
    private final ConcurrentMap<Connection> connections;
    private EndpointState state;
    protected MediaGroup mediaGroup;

    public AbstractEndpoint(String localName, RelayType relayType) {
        this.localName = localName;
        this.relayType = relayType;
        this.connections = new ConcurrentMap<Connection>();
        this.state = EndpointState.READY;
    }

    public AbstractEndpoint(String localName) {
        this(localName, RelayType.MIXER);
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
    public void setRelayType(RelayType relayType) {
        this.relayType = relayType;
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

    public Connection getConnection(int connectionId) {
        return this.connections.get(connectionId);
    }

    @Override
    public int getActiveConnectionsCount() {
        return this.connections.size();
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
        if(this.connections.containsKey(connection.getId())) {
            // Release the connection back to the resources pool
            this.connections.remove(connection.getId());

            // Close the connection if necessary
            if (!connection.isClosed()) {
                connection.close();
            }
            this.resourcesPool.releaseConnection(connection);
            
            // Release the media group is no more connections are active
            if (this.connections.isEmpty()) {
                this.mediaGroup.releaseAll();
            }
        }
    }

    @Override
    public void deleteAllConnections() {
        for (Connection connection : connections.values()) {
            deleteConnection(connection);
        }
    }

    @Override
    public void configure(boolean isALaw) {
        // Does nothing
    }

    @Override
    public Component getResource(MediaType mediaType, ComponentType componentType) {
        switch (mediaType) {
            case AUDIO:
                switch (componentType) {
                    case PLAYER:
                        return mediaGroup.getPlayer();
                    case RECORDER:
                        return mediaGroup.getRecorder();
                    case DTMF_DETECTOR:
                        return mediaGroup.getDtmfDetector();
                    case DTMF_GENERATOR:
                        return mediaGroup.getDtmfGenerator();
                    case SIGNAL_DETECTOR:
                        return mediaGroup.getSignalDetector();
                    case SIGNAL_GENERATOR:
                        return mediaGroup.getSignalGenerator();
                    default:
                        return null;
                }
            default:
                return null;
        }
    }

    @Override
    public boolean hasResource(MediaType mediaType, ComponentType componentType) {
        switch (mediaType) {
            case AUDIO:
                switch (componentType) {
                    case PLAYER:
                        return mediaGroup.hasPlayer();
                    case RECORDER:
                        return mediaGroup.hasRecorder();
                    case DTMF_DETECTOR:
                        return mediaGroup.hasDtmfDetector();
                    case DTMF_GENERATOR:
                        return mediaGroup.hasDtmfGenerator();
                    case SIGNAL_DETECTOR:
                        return mediaGroup.hasSignalDetector();
                    case SIGNAL_GENERATOR:
                        return mediaGroup.hasSignalGenerator();
                    default:
                        return false;
                }
            default:
                return false;
        }
    }

    @Override
    public void releaseResource(MediaType mediaType, ComponentType componentType) {
        switch (mediaType) {
            case AUDIO:
                switch (componentType) {
                    case PLAYER:
                        mediaGroup.releasePlayer();
                    case RECORDER:
                        mediaGroup.releaseRecorder();
                    case DTMF_DETECTOR:
                        mediaGroup.releaseDtmfDetector();
                    case DTMF_GENERATOR:
                        mediaGroup.releaseDtmfGenerator();
                    case SIGNAL_DETECTOR:
                        mediaGroup.releaseSignalDetector();
                    case SIGNAL_GENERATOR:
                        mediaGroup.releaseSignalGenerator();
                    default:
                        break;
                }
                break;

            default:
                break;
        }
    }

    protected abstract Logger getLogger();

}
