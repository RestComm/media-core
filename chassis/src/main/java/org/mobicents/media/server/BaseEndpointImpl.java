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

import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.mobicents.media.MediaSink;
import org.mobicents.media.MediaSource;
import org.mobicents.media.server.connection.BaseConnection;
import org.mobicents.media.server.connection.Connections;
import org.mobicents.media.server.connection.LocalToRemoteConnections;
import org.mobicents.media.server.connection.RemoteToRemoteConnections;
import org.mobicents.media.server.impl.rtp.RTPManager;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.ConnectionType;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.spi.EndpointState;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.ResourceUnavailableException;
import org.mobicents.media.server.spi.TooManyConnectionsException;
import org.mobicents.media.server.spi.dsp.DspFactory;

/**
 * Basic implementation of the endpoint.
 * 
 * @author kulikov
 * @author amit bhayani
 */
public abstract class BaseEndpointImpl implements Endpoint {

	public static final int ENDPOINT_NORMAL=0;
	public static final int ENDPOINT_LOCAL_TO_REMOTE=1;
	public static final int ENDPOINT_REMOTE_TO_REMOTE=2;
	
    //local name of this endpoint
    private String localName;

    //current state of this endpoint
    private EndpointState state = EndpointState.READY;

    //connections subsytem
    private Connections connections;

    //rtp protocol manager
    private RTPManager rtpManager;
    
    protected ArrayList<MediaType> mediaTypes = new ArrayList();

    //job scheduler
    private Scheduler scheduler;

    //Signaling processor factory;
    private DspFactory dspFactory;
    
    private int endpointType=ENDPOINT_NORMAL;
    
    //logger instance
    private final Logger logger = Logger.getLogger(BaseEndpointImpl.class);
    
    private int localConnections = 10;
    private int rtpConnections = 10;
    
    public BaseEndpointImpl(String localName,int endpointType) {
        this.localName = localName;
        this.endpointType=endpointType;        
    }


    /**
     * (Non Java-doc.)
     *
     * @see org.mobicents.media.server.spi.Endpoint#getLocalName()
     */
    public String getLocalName() {
        return localName;
    }

    /**
     * Assigns scheduler.
     *
     * @param scheduler the scheduler instance.
     */
    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    /**
     * Provides access to the scheduler.
     *
     * @return scheduler instance.
     */
    public Scheduler getScheduler() {
        return scheduler;
    }

    /**
     * Provides access to the wall clock used by this endpoint and scheduler
     *
     * @return the wall clock instance.
     */
    public Clock getClock() {
        return scheduler.getClock();
    }

    /**
     * (Non Java-doc.)
     *
     * @see org.mobicents.media.server.spi.Endpoint#getState() 
     */
    public EndpointState getState() {
        return state;
    }

    /**
     * Modifies state indicator.
     *
     * @param state the new value of the state indicator.
     */
    protected void setState(EndpointState state) {
        this.state = state;
    }
    
    /**
     * (Non Java-doc.)
     *
     * @see org.mobicents.media.server.spi.Endpoint#getMediaTypes()
     */
    public Collection<MediaType> getMediaTypes() {
        return mediaTypes;
    }

    /**
     * Assigns DSP factory to this endpoint.
     * 
     * @param dspFactory the DSP factory.
     */
    public void setDspFactory(DspFactory dspFactory) {
        this.dspFactory = dspFactory;
    }
    
    /**
     * Gets access to the DSP factory.
     * 
     * @return the DSP factory instance
     */
    public DspFactory getDspFactory() {
        return dspFactory;
    }
    
    /**
     * Assigns number of local connections.
     * 
     * @param localConnections the number of connections.
     */
    public void setLocalConnections(int localConnections)
    {
    	this.localConnections=localConnections;
    }
    
    /**
     * Assigns number of rtp connections.
     * 
     * @param rtpConnections the number of connections.
     */
    public void setRtpConnections(int rtpConnections)
    {
    	this.rtpConnections=rtpConnections;
    }
    
    /**
     * Provides access to the media sink.
     *
     * @param media the media type
     * @return sink of this media type
     */
    public abstract MediaSink getSink(MediaType media);

    /**
     * Provides access to the media source.
     *
     * @param media the media type
     * @return sink of this media type
     */
    public abstract MediaSource getSource(MediaType media);

    /**
     * (Non Java-doc.)
     *
     * @see org.mobicents.media.server.spi.Endpoint#start()
     */
    public void start() throws ResourceUnavailableException {
        //do checks before start
        if (scheduler == null) {
            throw new ResourceUnavailableException("Scheduler is not available");
        }

        //create connections subsystem
        try {
        	if(this.endpointType==ENDPOINT_LOCAL_TO_REMOTE)
        		connections = new LocalToRemoteConnections(this, localConnections, rtpConnections);
        	else if(this.endpointType==ENDPOINT_REMOTE_TO_REMOTE)
        		connections = new RemoteToRemoteConnections(this, localConnections, rtpConnections);
        	else
        		connections = new Connections(this, localConnections, rtpConnections,false);
        	
        } catch (Exception e) {  
        	throw new ResourceUnavailableException(e);
        }
    }

    /**
     * (Non Java-doc.)
     *
     * @see org.mobicents.media.server.spi.Endpoint#stop()
     */
    public void stop() {
        //TODO: unregister at scheduler level
        logger.info("Stopped " + localName);
    }

    /**
     * Real time transmission over IP network service
     *
     * @param rtpManager the entry point to the RTP service
     */
    public void setRtpManager(RTPManager rtpManager) {
        this.rtpManager = rtpManager;
    }

    /**
     * Real time transmission over IP network service
     *
     * @return the entry point to the RTP service
     */
    public RTPManager getRtpManager() {
        return this.rtpManager;
    }

    public Collection<Connection> getConnections() {
        return null;// connections.values();
    }

    /**
     * Gets the SDP of this endpoint.
     * 
     * @param mediaType
     * @return
     * @throws ResourceUnavailableException
     */
    public String describe(MediaType mediaType) throws ResourceUnavailableException {
        String sdp = null;
        return sdp;
    }

    /**
     * (Non Java-doc.)
     * 
     * @see org.mobicents.media.server.spi.Endpoint#createConnection(org.mobicents.media.server.spi.ConnectionMode);
     */
    public Connection createConnection(ConnectionType type) throws TooManyConnectionsException, ResourceUnavailableException {
        BaseConnection connection = (BaseConnection) connections.createConnection(type);
        
        try {
            connection.bind();
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResourceUnavailableException(e.getMessage());
        }
        
        return connection;
    }

    /**
     * (Non Java-doc.)
     *
     * @see org.mobicents.media.server.spi.Endpoint#deleteConnection(Connection)
     */
    public void deleteConnection(Connection connection) {
        ((BaseConnection)connection).close();
    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.server.spi.Endpoint#deleteAllConnections();
     */
    public void deleteAllConnections() {
        connections.release();
    }


    public Connection getConnection(String connectionID) {
        return null;
    }
    
    public int getActiveConnectionsCount()
    {
    	return connections.getActiveConnectionsCount();
    }

    public abstract void unblock();
    public abstract void block();
    
}
