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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.mobicents.media.MediaSink;
import org.mobicents.media.MediaSource;
import org.mobicents.media.Component;
import org.mobicents.media.ComponentType;
import org.mobicents.media.core.connections.BaseConnection;
import org.mobicents.media.core.connections.RtpConnectionImpl;
import org.mobicents.media.core.connections.LocalConnectionImpl;
import org.mobicents.media.core.ResourcesPool;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.ConnectionType;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.spi.EndpointState;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.ResourceUnavailableException;
import org.mobicents.media.server.spi.TooManyConnectionsException;
import org.mobicents.media.server.spi.dsp.DspFactory;

/**
 * Basic implementation of the endpoint.
 * 
 * @author yulian oifa
 * @author amit bhayani
 */
public abstract class BaseEndpointImpl implements Endpoint {
	
	//local name of this endpoint
    private String localName;

    //current state of this endpoint
    private EndpointState state = EndpointState.READY;

    //media group
    protected MediaGroup mediaGroup;
    
    //resources pool 
    protected ResourcesPool resourcesPool;
    
    //job scheduler
    private Scheduler scheduler;

    //logger instance
    private final Logger logger = Logger.getLogger(BaseEndpointImpl.class);
    
    private ConcurrentHashMap<String,Connection> connections;
    
    public BaseEndpointImpl(String localName) {
        this.localName = localName;              
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
     * Assigns resources pool.
     *
     * @param resourcesPool the resources pool instance.
     */
    public void setResourcesPool(ResourcesPool resourcesPool) {
        this.resourcesPool = resourcesPool;
    }

    /**
     * Provides access to the resources pool.
     *
     * @return scheduler instance.
     */
    public ResourcesPool getResourcesPool() {
        return resourcesPool;
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
    public void setState(EndpointState state) {
        this.state = state;
    }
    
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

        if (resourcesPool == null) {
            throw new ResourceUnavailableException("Resources pool is not available");
        }
        
        //create connections subsystem
        mediaGroup=new MediaGroup(resourcesPool,this);
    	connections=new ConcurrentHashMap();
    }

    /**
     * (Non Java-doc.)
     *
     * @see org.mobicents.media.server.spi.Endpoint#stop()
     */
    public void stop() {
    	mediaGroup.releaseAll();
    	deleteAllConnections();
        //TODO: unregister at scheduler level
        logger.info("Stopped " + localName);
    }

    /**
     * (Non Java-doc.)
     * 
     * @see org.mobicents.media.server.spi.Endpoint#createConnection(org.mobicents.media.server.spi.ConnectionMode);
     */
    public Connection createConnection(ConnectionType type,Boolean isLocal) throws ResourceUnavailableException {
    	
    	Connection connection=null;
    	switch(type)
    	{
    		case RTP:
    			connection=resourcesPool.newConnection(false);
    			break;
    		case LOCAL:
    			connection=resourcesPool.newConnection(true);
    			break;
    	}
    	
    	connection.setIsLocal(isLocal);
        
        try {
            ((BaseConnection)connection).bind();
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResourceUnavailableException(e.getMessage());
        }
        
        connection.setEndpoint(this);
        connections.put(connection.getId(),connection);
        return connection;
    }

    public void deleteConnection(Connection connection)
    {
    	((BaseConnection)connection).close();    	
    }
    
    /**
     * (Non Java-doc.)
     *
     * @see org.mobicents.media.server.spi.Endpoint#deleteConnection(Connection)
     */
    public void deleteConnection(Connection connection,ConnectionType connectionType) {
    	connections.remove(connection.getId());
    	
    	switch(connectionType)
    	{
    		case RTP:
    			resourcesPool.releaseConnection(connection,false);
    			break;
    		case LOCAL:
    			resourcesPool.releaseConnection(connection,true);
    			break;
    	}
    	
    	if(connections.size()==0)
    		mediaGroup.releaseAll();    	
    }
    
    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.server.spi.Endpoint#deleteAllConnections();
     */
    public void deleteAllConnections() {
    	Connection currConnection;
        for(Enumeration<String> e = connections.keys() ; e.hasMoreElements() ;) {
                currConnection=connections.get(e.nextElement());
                ((BaseConnection)currConnection).close();                
        }
    }


    public Connection getConnection(String connectionID) {
        return connections.get(connectionID);
    }
    
    public int getActiveConnectionsCount()
    {
    	return connections.size();
    }

    public void configure(boolean isALaw)
    {
    }
    
    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.server.spi.Endpoint#getResource();
     */
    public Component getResource(MediaType mediaType, ComponentType componentType)
    {
    	switch(mediaType)
    	{
    		case AUDIO:
    			switch(componentType)
    			{
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
    			}    			
    			break;
    	}
    	
    	return null;
    }
    
    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.server.spi.Endpoint#getResource();
     */
    public boolean hasResource(MediaType mediaType, ComponentType componentType)
    {
    	switch(mediaType)
    	{
    		case AUDIO:
    			switch(componentType)
    			{
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
    			}
    			break;
    	}
    	
    	return false;
    }
    
    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.server.spi.Endpoint#getResource();
     */
    public void releaseResource(MediaType mediaType, ComponentType componentType)
    {
    	switch(mediaType)
    	{
    		case AUDIO:
    			switch(componentType)
    			{
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
    			}    			
    			break;
    	}    	
    }
    
    //should be handled on higher layers
    public abstract void modeUpdated(ConnectionMode oldMode,ConnectionMode newMode);
}
