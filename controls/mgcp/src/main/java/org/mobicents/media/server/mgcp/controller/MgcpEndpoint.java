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

import org.apache.log4j.Logger;
import org.mobicents.media.server.concurrent.ConcurrentMap;
import org.mobicents.media.server.concurrent.pooling.ConcurrentResourcePool;
import org.mobicents.media.server.concurrent.pooling.ResourcePool;
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

/**
 * Represents media endpoint on MGCP controller side.
 * 
 * @author yulian oifa
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class MgcpEndpoint {
    
    private static final Logger LOGGER = Logger.getLogger(MgcpEndpoint.class);
    
    // Transaction ID generator
	public static final AtomicInteger txID = new AtomicInteger(1);
    
    // Default size of the connection pool
    private final static int N = 15;
    
    // List of possible states
    public final static int STATE_LOCKED = 1;
    public final static int STATE_FREE = 2;
    public final static int STATE_BUSY = 3;
    
    // Endpoint Properties
    protected final MgcpProvider mgcpProvider;
    private final Endpoint endpoint;
    protected final Text fullName;
    private AtomicInteger state;
    
    // Listeners
    private MgcpEndpointStateListener stateListener;
    private MgcpListener listener;
    
    // Connection Pool
    private final ResourcePool<MgcpConnection> connectionPool;
    private final ConcurrentMap<MgcpConnection> activeConnections;

    // Request executor associated with endpoint
    private final Request request;
    
    /**
     * Constructs new endpoint activity.
     * 
     * @param endpoint native endpoint.
     */
    public MgcpEndpoint(Endpoint endpoint, MgcpProvider mgcpProvider, String domainName, int port, Collection<MgcpPackage> packages) {
        // Endpoint properties
        this.endpoint = endpoint;
        this.mgcpProvider = mgcpProvider;
        this.fullName = new Text(endpoint.getLocalName() + "@" + domainName + ":" + port);
        this.state = new AtomicInteger(STATE_FREE);
        
        // Connection pool
        this.connectionPool = new MgcpConnectionPool();
        this.activeConnections=new ConcurrentMap<MgcpConnection>();

        for (int i = 0; i < N; i++) {
            this.connectionPool.offer(new MgcpConnection(this));
        }
        
        // Request Executor
        this.request = new Request(this, packages);        
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
            if(this.stateListener!=null) {
            	this.stateListener.onFreed(this);
            }
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
    	//create connection and wrap it in the MGCP activity
    	Connection connection = this.endpoint.createConnection(type,isLocal);
    	MgcpConnection mgcpConnection = this.connectionPool.poll();
    	mgcpConnection.wrap(call, connection);

    	//put connection activity into active list
        this.activeConnections.put(mgcpConnection.id, mgcpConnection);
    	this.state.set(STATE_BUSY);
    	return mgcpConnection;    	
    }

    /**
     * Deletes connection.
     * 
     * @param id the identifier of the relative connection activity.
     */
    public void deleteConnection(int id) {
        // Remove connection from active list
    	MgcpConnection mgcpConnection=activeConnections.remove(id);

    	if (mgcpConnection == null) {
    	    LOGGER.warn("Connection id="+ id + " not found on endpoint " + this.endpoint.getLocalName());
    		//TODO: throw exception
    		return;
    	}

    	// Release connection
    	mgcpConnection.release();
    	endpoint.releaseConnection(mgcpConnection.getConnectionId());
    	this.connectionPool.offer(mgcpConnection);

    	// Free the endpoint   	
        if (this.activeConnections.isEmpty()) {
            int oldValue = this.state.getAndSet(STATE_FREE);
            if (oldValue != STATE_FREE && this.stateListener != null) {
                this.stateListener.onFreed(this);
            }
        }
    	this.request.cancel();    	    	   
    }

    public void deleteAllConnections() {
        // Release all connections locally
    	Iterator<Integer> keyIterator = activeConnections.keysIterator();
    	while(keyIterator.hasNext()) {
    	    int connectionId = keyIterator.next();
    	    MgcpConnection mgcpConnection = activeConnections.remove(connectionId);
    	    mgcpConnection.release();
    		this.connectionPool.offer(mgcpConnection);        
    	}
    	
    	// Release all connections in the endpoint
        endpoint.releaseConnections();
        
        // Free the endpoint
        int oldValue=this.state.getAndSet(STATE_FREE);
        if(oldValue!=STATE_FREE && this.stateListener!=null) {
        	this.stateListener.onFreed(this);
        }
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
        MgcpConnection mgcpConnection = connectionPool.poll();
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
        // remove from active list
        this.activeConnections.remove(mgcpConnection.id);

        // reclaim
        mgcpConnection.release();

        endpoint.releaseConnection(mgcpConnection.getConnectionId());

        // back to pool
        connectionPool.offer(mgcpConnection);

        if (activeConnections.isEmpty()) {
            int oldValue = this.state.getAndSet(STATE_FREE);
            if (oldValue != STATE_FREE && this.stateListener != null) {
                this.stateListener.onFreed(this);
            }
        }
    }

    public void configure(boolean isALaw) {
        endpoint.configure(isALaw);
    }
    
    private class MgcpConnectionPool extends ConcurrentResourcePool<MgcpConnection> {

        private final AtomicInteger count = new AtomicInteger(N);

        @Override
        public MgcpConnection poll() {
            // Attempt to retrieve pooled connection
            MgcpConnection connection = super.poll();

            if (connection == null) {
                // Create new connection in case pool is empty
                connection = new MgcpConnection(MgcpEndpoint.this);
                int newSize = this.count.incrementAndGet();

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Allocated new MGCP connection in " + endpoint.getLocalName() + ", pooled=" + newSize
                            + ", free=" + this.size());
                }
            }

            return connection;

        }

        @Override
        public void offer(MgcpConnection resource) {
            super.offer(resource);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Released MGCP connection in " + endpoint.getLocalName() + ", pooled=" + this.count.get()
                        + ", free=" + this.size());
            }
        }

    }
}
