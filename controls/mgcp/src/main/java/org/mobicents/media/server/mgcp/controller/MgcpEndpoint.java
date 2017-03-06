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
package org.mobicents.media.server.mgcp.controller;

import java.net.SocketAddress;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import org.mobicents.media.server.mgcp.MgcpEvent;
import org.mobicents.media.server.mgcp.MgcpListener;
import org.mobicents.media.server.mgcp.MgcpProvider;
import org.mobicents.media.server.mgcp.controller.signal.MgcpPackage;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.ConnectionType;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.spi.ResourceUnavailableException;
import org.mobicents.media.server.spi.TooManyConnectionsException;
import org.mobicents.media.server.utils.Text;
import org.restcomm.media.server.concurrent.ConcurrentCyclicFIFO;
import org.restcomm.media.server.concurrent.ConcurrentMap;

/**
 * Represents media endpoint on MGCP controller side.
 * 
 * @author yulian oifa
 */
public class MgcpEndpoint {
	public static AtomicInteger txID = new AtomicInteger(1);
    
    //The size of the connection's pool
    private final static int N = 15;
    
    public final static int STATE_LOCKED = 1;
    public final static int STATE_FREE = 2;
    public final static int STATE_BUSY = 3;
    
    //native endpoint
    private Endpoint endpoint;
    protected Text fullName;
    
    //the state of this activity
    private AtomicInteger state = new AtomicInteger(STATE_FREE);
    
    //Request executor associated with endpoint
    Request request;
    
    //pool of connection activities, limited to 15
    private ConcurrentCyclicFIFO<MgcpConnection> connections = new ConcurrentCyclicFIFO<MgcpConnection>();
    
    //list of active connections
    private ConcurrentMap<MgcpConnection> activeConnections=new ConcurrentMap<MgcpConnection>();
    private Iterator<Integer> keyIterator;
    
    protected MgcpProvider mgcpProvider;
    
    private MgcpListener listener;
    private MgcpEndpointStateListener stateListener;
    /**
     * Constructs new endpoint activity.
     * 
     * @param endpoint native endpoint.
     */
    public MgcpEndpoint(Endpoint endpoint, MgcpProvider mgcpProvider, String domainName, int port, Collection<MgcpPackage> packages) {
        this.endpoint = endpoint;
        this.mgcpProvider = mgcpProvider;
        this.fullName = new Text(endpoint.getLocalName() + "@" + domainName + ":" + port);
        
        //create request executor
        request=new Request(this, packages);        

        for (int i = 0; i < N; i++) {
            connections.offer(new MgcpConnection());
        }
    }

    /**
     * Gets the MGCP name of this activity.
     * 
     * @return the name of activity
     */
    public String getName() {
        return endpoint.getLocalName();
    }

    public Text getFullName() {
        return fullName;
    }

    /**
     * Provides the access to the native endpoint.
     * 
     * @return the native endpoint.
     */
    public Endpoint getEndpoint() {
        return endpoint;
    }

    public void setMgcpListener(MgcpListener listener) {
        this.listener = listener;
    }
    
    public void setMgcpEndpointStateListener(MgcpEndpointStateListener listener) {
        this.stateListener = listener;
    }
    
    /**
     * Gets the current state of this activity.
     * 
     * @return the state indicator.
     */
    public int getState() {
        return state.get();
    }

    /**
     * Gets the access to the request executor.
     * 
     * @return request executor.
     */
    public Request getRequest() {
    	return this.request;
    }
    
    /**
     * Exclusively locks this activity.
     */
    public void lock() {
        this.state.set(STATE_LOCKED);
    }

    /**
     * Unlocks this activity if it is not busy
     */
    public void share() {
        if (this.state.get() == STATE_LOCKED) {
            this.state.set(STATE_FREE);
            if(this.stateListener!=null)
            	this.stateListener.onFreed(this);
        }
    }

    /**
     * Creates new RTP connection.
     * 
     * Creation of connection makes endpoint activity busy.
     * 
     * @return connection activity.
     */
    public MgcpConnection createConnection(MgcpCall call, ConnectionType type,boolean isLocal) throws TooManyConnectionsException, ResourceUnavailableException {
    	//create connection
    	Connection connection = endpoint.createConnection(type,isLocal);
    	//wrap connection with relative activity
    	MgcpConnection mgcpConnection = connections.poll();
    	if(mgcpConnection==null)
    		mgcpConnection=new MgcpConnection();
    	
    	mgcpConnection.wrap(this, call, connection);

    	//put connection activity into active list
    	activeConnections.put(mgcpConnection.id,mgcpConnection);

    	//change state to BUSY what means that this connection has at least one
    	//connection
    	this.state.set(STATE_BUSY);
        
    	return mgcpConnection;    	
    }

    /**
     * Deletes connection.
     * 
     * @param id the identifier of the relative connection activity.
     */
    public void deleteConnection(Integer id) {
    	MgcpConnection mgcpConnection=activeConnections.remove(id);

    	//connection not found?
    	if (mgcpConnection == null) {
    		//TODO: throw exception
    		return;
    	}

    	//remove activity from list and terminate
    	mgcpConnection.release();

    	endpoint.deleteConnection(mgcpConnection.connection);
        
    	//return object to pool
    	connections.offer(mgcpConnection);

    	//update state    	
    	if (activeConnections.isEmpty()) {
    		int oldValue=this.state.getAndSet(STATE_FREE);
    		if(oldValue!=STATE_FREE && this.stateListener!=null)
    				this.stateListener.onFreed(this);    		
    	}
    	
    	this.request.cancel();    	    	   
    }

    public void deleteAllConnections() {
    	keyIterator = activeConnections.keysIterator();
    	while(keyIterator.hasNext())
    		connections.offer(activeConnections.remove(keyIterator.next()));        
    	
        endpoint.deleteAllConnections();
        
        int oldValue=this.state.getAndSet(STATE_FREE);
        
        if(oldValue!=STATE_FREE && this.stateListener!=null)
        	this.stateListener.onFreed(this);
        
        this.request.cancel();
    }
    
    public MgcpConnection getConnection(Integer connectionID) {
    	return activeConnections.get(connectionID);      
    }

    protected void send(MgcpEvent message, SocketAddress address) {
    	listener.process(message);
    }
        
    /**
     * Asks mgcp connection object from pool.
     * 
     * @param call the call to which connection belongs
     * @return MgcpConnection object.
     */
    protected MgcpConnection poll(MgcpCall call) {
    	//take first from pool and put into list of active    	
        MgcpConnection mgcpConnection = connections.poll();
        if(mgcpConnection==null)
    		mgcpConnection=new MgcpConnection();
        
        activeConnections.put(mgcpConnection.id,mgcpConnection);
        
        //assign call
        mgcpConnection.setCall(call);        
        return mgcpConnection;
    }
    
    /**
     * Reclaims used connection object.
     * 
     * @param connection the object to be reclaimed
     */
    protected void offer(MgcpConnection mgcpConnection) {
    	//remove from active list
    	activeConnections.remove(mgcpConnection.id);
    	
        //reclaim
        mgcpConnection.release();
        
        endpoint.deleteConnection(mgcpConnection.connection);
        
        //back to pool
        connections.offer(mgcpConnection);
        
        if (activeConnections.isEmpty()) {
    		int oldValue=this.state.getAndSet(STATE_FREE);
    		if(oldValue!=STATE_FREE && this.stateListener!=null)
    				this.stateListener.onFreed(this);    		
    	}
    }
    
    public void configure(boolean isALaw)
    {
    	endpoint.configure(isALaw);
    }
}
