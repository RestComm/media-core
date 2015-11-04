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

import java.util.Iterator;

import org.apache.log4j.Logger;
import org.mobicents.media.Component;
import org.mobicents.media.ComponentType;
import org.mobicents.media.core.ResourcesPool;
import org.mobicents.media.core.connections.BaseConnection;
import org.mobicents.media.server.concurrent.ConcurrentMap;
import org.mobicents.media.server.scheduler.PriorityQueueScheduler;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.ConnectionType;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.spi.EndpointState;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.ResourceUnavailableException;

/**
 * Basic implementation of the endpoint.
 * 
 * @author yulian oifa
 * @author amit bhayani
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public abstract class BaseEndpointImpl implements Endpoint {

    private static final Logger logger = Logger.getLogger(BaseEndpointImpl.class);

    // Task Scheduler
    private final PriorityQueueScheduler scheduler;

    // Resources Pool
    protected final ResourcesPool resourcesPool;
    protected final MediaGroup mediaGroup;

    // Endpoint Properties
    private final String localName;
    private EndpointState state;
    protected final ConcurrentMap<Connection> connections;
    private volatile boolean started;

    public BaseEndpointImpl(String localName, PriorityQueueScheduler scheduler, ResourcesPool resourcesPool) {
        // Task Scheduler
        this.scheduler = scheduler;

        // Resources Pool
        this.resourcesPool = resourcesPool;
        this.mediaGroup = new MediaGroup(resourcesPool, this);

        // Endpoint Properties
        this.localName = localName;
        this.state = EndpointState.READY;
        this.connections = new ConcurrentMap<Connection>();
        this.started = false;
    }

    @Override
    public String getLocalName() {
        return localName;
    }

    @Override
    public PriorityQueueScheduler getScheduler() {
        return scheduler;
    }

    @Override
    public EndpointState getState() {
        return state;
    }

    /**
     * Modifies state indicator.
     * 
     * @param state the new value of the state indicator.
     */
    public void setState(EndpointState state) {
        this.state = state;
    }

    public boolean isStarted() {
        return started;
    }

    @Override
    public void start() throws ResourceUnavailableException {
        if (!started) {
            this.started = true;
            logger.info("Started " + localName);
        }
    }

    @Override
    public void stop() {
        if (started) {
            this.started = false;
            mediaGroup.releaseAll();
            releaseConnections();
            // TODO: unregister at scheduler level
            logger.info("Stopped " + localName);
        }
    }

    @Override
    public Connection createConnection(ConnectionType type, Boolean isLocal) throws ResourceUnavailableException {
        Connection connection = null;
        switch (type) {
            case RTP:
                connection = resourcesPool.newConnection(false);
                break;
            case LOCAL:
                connection = resourcesPool.newConnection(true);
                break;
        }

        connection.setIsLocal(isLocal);

        try {
            ((BaseConnection) connection).bind();
        } catch (Exception e) {
            logger.error("Could not bind connection.", e);
            throw new ResourceUnavailableException(e.getMessage());
        }

        connection.setEndpoint(this);
        connections.put(connection.getId(), connection);

        // We have at least one connection so we can activate the endpoint
        if (!started) {
            start();
        }

        return connection;
    }
    
    protected void releaseConnection(Connection connection) {
        // Close the connection
        connection.close();

        // Put back the connection in the pool
        switch (connection.getType()) {
            case RTP:
                resourcesPool.releaseConnection(connection, false);
                break;
            case LOCAL:
                resourcesPool.releaseConnection(connection, true);
                break;
        }

        // Stop the endpoint if there are no active connections
        if (connections.isEmpty()) {
            stop();
        }
    }

    @Override
    public void releaseConnections() {
        Iterator<Connection> connectionsIterator = connections.valuesIterator();
        while (connectionsIterator.hasNext()) {
            Connection connection = connectionsIterator.next();
            releaseConnection(connection.getId());
        }
    }

    public Connection getConnection(int connectionID) {
        return connections.get(connectionID);
    }

    @Override
    public int getActiveConnectionsCount() {
        return connections.size();
    }

    @Override
    public void configure(boolean isALaw) {
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
                        break;
                }
                break;
            default:
                break;
        }
        return null;
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
                        break;
                }
                break;
            default:
                break;
        }
        return false;
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

}
