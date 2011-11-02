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
package org.mobicents.media.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;
import org.mobicents.media.Component;
import org.mobicents.media.MediaSink;
import org.mobicents.media.MediaSource;
import org.mobicents.media.server.impl.clock.LocalTask;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.ConnectionState;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.spi.EndpointState;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.NotificationListener;
import org.mobicents.media.server.spi.ResourceUnavailableException;
import org.mobicents.media.server.spi.TooManyConnectionsException;
import org.mobicents.media.server.spi.rtp.RtpManager;

/**
 * 
 * @author kulikov
 * @author amit bhayani
 */
public abstract class BaseEndpointImpl implements Endpoint {

    private String localName;
    
    private EndpointState state = EndpointState.READY;
    private LocalTask stateHandler;
    
    protected ConnectionFactory connectionFactory;
    protected RtpManager rtpFactory;
    
    private HashMap<MediaType, MediaSource> sources = new HashMap();
    private HashMap<MediaType, MediaSink> sinks = new HashMap();
    
    private ArrayList<MediaType> mediaTypes = new ArrayList();
    /** The list of indexes available for connection enumeration within endpoint */
    private ArrayList<Integer> index = new ArrayList();
    /** The last generated connection's index */
    private int lastIndex = -1;
    /** Holder for created connections */
    // protected transient HashMap<String, Connection> connections = new
    // HashMap();
    //private int maxConnections = 10;
    //private static final int _CONNECTION_TAB_SIZE = 10;
    
    // collection of prestarted local connections
    private int connectionPoolSize = 0; //def is 0. connectionTableSize might be better for impl, but "pool" sounds better from user POV
    private LocalConnectionImpl[] localConnections;    // collection of prestarted network connections
    private RtpConnectionImpl[] networkConnections;    // collection of active connections
    private Connection[] connections;                  // collection of activly used connections
    private int count;
    
    protected ReentrantLock stateLock = new ReentrantLock();
    private final Logger logger = Logger.getLogger(BaseEndpointImpl.class);

    private long lastUsed;
    
    public BaseEndpointImpl(String localName) {
        this.localName = localName;
    }
    
    public int getConnectionPoolSize() {
		return connectionPoolSize;
	}
    
	public void setConnectionPoolSize(int connectionTableSize) {
		this.connectionPoolSize = connectionTableSize;
	}

	public String getLocalName() {
        return localName;
    }

    public void setLocalName(String localName) {
        this.localName = localName;
    }

    public EndpointState getState() {
        return state;
    }
    
    public Collection<MediaType> getMediaTypes() {
        return mediaTypes;
    }

    protected void setMediaTypes(Collection<MediaType> mediaTypes) {
        this.mediaTypes.addAll(mediaTypes);
    }
    
    public MediaSink getSink(MediaType media) {
        MediaSink sink = sinks.get(media);
        return sink;
    }

    protected void setSinks(HashMap<MediaType, MediaSink> sinks) {
        this.sinks.putAll(sinks);
    }
    
    public MediaSource getSource(MediaType media) {
        return sources.get(media);
    }

    protected void setSources(HashMap<MediaType, MediaSource> sources) {
        this.sources.putAll(sources);
    }
    /**
     * Calculates index of the new connection.
     * 
     * The connection uses this method to ask endpoint for new lowerest index.
     * The index is unique withing endpoint but it is not used as connection
     * identifier outside of the endpoint.
     * 
     * @return the lowerest available integer value.
     */
    public int getConnectionIndex() {
        return index.isEmpty() ? ++lastIndex : index.remove(0);
    }


    public void start() throws ResourceUnavailableException {
    	//baranowb: why all 3 have the same size.
        networkConnections = new RtpConnectionImpl[connectionPoolSize];
        localConnections = new LocalConnectionImpl[connectionPoolSize];
        connections = new ConnectionImpl[connectionPoolSize];

        // let's create several connections
        for (int i = 0; i < connections.length; i++) {
            localConnections[i] = new LocalConnectionImpl(this, connectionFactory);
            this.setLifeTime(localConnections[i]);
        }

        // let's create several connections
        if (this.rtpFactory != null) {
            for (int i = 0; i < connections.length; i++) {
                networkConnections[i] = new RtpConnectionImpl(this, connectionFactory, rtpFactory);
                this.setLifeTime(networkConnections[i]);
            }
        }
    }

    private void setLifeTime(Connection connection) {

        //lets set the LifeTime for connection
        if (this.connectionFactory != null && this.connectionFactory.getConnectionStateManager() != null) {
            Hashtable<ConnectionState, Integer> connStateLifeTime = this.connectionFactory.getConnectionStateManager().getConnStateLifeTime();
            for (ConnectionState connState : connStateLifeTime.keySet()) {
                connection.getLifeTime()[connState.getCode()] = connStateLifeTime.get(connState);
                if (logger.isDebugEnabled()) {
                    logger.debug("silence for state " + connState + " is " + connStateLifeTime.get(connState));
                }
            }
        }
    }

    public void stop() {
        logger.info("Stopped " + localName);
    }

    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public void setRtpManager(RtpManager rtpFactory) {
        this.rtpFactory = rtpFactory;
    }

    public RtpManager getRtpManager() {
        return this.rtpFactory;
    }

    public Collection<Connection> getConnections() {
        return null;// connections.values();
    }

    public String describe(MediaType mediaType) throws ResourceUnavailableException {
        String sdp = null;
        stateLock.lock();
        try {
            RtpConnectionImpl connection = null;
            for (int i = 0; i < networkConnections.length; i++) {
                if (networkConnections[i].getState() == ConnectionState.NULL) {
                    connection = networkConnections[i];
                    break;
                }
            }

            // connection not found?
            if (connection == null) {
                throw new ResourceUnavailableException(
                        "The limit of network connection is exeeded");
            }

            if (mediaType == null) {
                connection.join();
                sdp = connection.getLocalDescriptor();
                connection.close();
            } else {
                connection.join(mediaType);
                sdp = connection.getLocalDescriptor();
                connection.close(mediaType);
            }
        } catch (Exception e) {
            logger.error("Could not create RTP connection", e);
            throw new ResourceUnavailableException(e.getMessage());
        } finally {
            stateLock.unlock();
        }
        return sdp;
    }

    /**
     * (Non Java-doc.)
     * 
     * @see org.mobicents.media.server.spi.Endpoint#createConnection(org.mobicents.media.server.spi.ConnectionMode);
     */
    public Connection createConnection() throws TooManyConnectionsException, ResourceUnavailableException {
        stateLock.lock();
        try {
            if (count == connections.length) {
                throw new TooManyConnectionsException("The limit of connections exceeded");
            }
            // what we need is to find the first unused connection from list
            // of network connections and return it.
            RtpConnectionImpl connection = null;
            for (int i = 0; i < networkConnections.length; i++) {
                if (networkConnections[i].getState() == ConnectionState.NULL) {
                    connection = networkConnections[i];
                    break;
                }
            }

            // connection not found?
            if (connection == null) {
                throw new ResourceUnavailableException(
                        "The limit of network connection is exeeded");
            }

            // connection found. put this connection to the list of used
            // connections
            // searching first suitable place
            for (int i = 0; i < connections.length; i++) {
                if (connections[i] == null) {
                    connection.setIndex(i);
                    connections[i] = connection;
                    break;
                }
            }

            connection.join();
            connection.bind();

            //Sync the Connection for LifeTime handling
            //logger.info("Synced Connection "+ connection);
//            connection.setStartTime(System.currentTimeMillis());
//            stateHandler = Server.scheduler.execute(connection);

            count++;
            this.state = EndpointState.BUSY;
            return connection;
        } catch (Exception e) {
            logger.error("Could not create RTP connection", e);
            throw new ResourceUnavailableException(e.getMessage());
        } finally {
            stateLock.unlock();
        }
    }

    /**
     * (Non Java-doc.)
     * 
     * @see org.mobicents.media.server.spi.Endpoint#createConnection(org.mobicents.media.server.spi.ConnectionMode);
     */
    public Connection createLocalConnection() throws TooManyConnectionsException, ResourceUnavailableException {
        stateLock.lock();
        try {
            if (count == connections.length) {
                throw new TooManyConnectionsException("The limit of connections exceeded");
            }
            
            // what we need is to find the first unused connection from list
            // of local connections and return it.
            LocalConnectionImpl connection = null;
            for (int i = 0; i < localConnections.length; i++) {
                if (localConnections[i].getState() == ConnectionState.NULL) {
                    connection = localConnections[i];
                    break;
                }
            }

            // connection not found?
            if (connection == null) {
                throw new ResourceUnavailableException(
                        "The limit of network connection is exeeded");
            }

            // connection found. put this connection to the list of used
            // connections
            // searching first suitable place
            for (int i = 0; i < connections.length; i++) {
                if (connections[i] == null) {
                    connection.setIndex(i);
                    connections[i] = connection;
                    if (logger.isInfoEnabled()) {
                        logger.info("Connection index: " + i + ", on endpoint: " + this.localName);
                    }
                    break;
                }
            }

            connection.join();
            connection.bind();

            //Sync the Connection for LifeTime handling
//            connection.setStartTime(System.currentTimeMillis());
//            stateHandler = Server.scheduler.execute(connection);

            count++;
            this.state = EndpointState.BUSY;
            return connection;
        } catch (Exception e) {
            logger.error("Could not create Local connection", e);
            throw new ResourceUnavailableException(e.getMessage());
        } finally {
            stateLock.unlock();
        }
    }

    public void deleteConnection(String connectionID) {
        stateLock.lock();
        try {
            // find this connection
            ConnectionImpl connection = null;
            for (int i = 0; i < connections.length; i++) {
                if (connections[i] != null && ((ConnectionImpl) connections[i]).getId().equals(
                        connectionID)) {
                    connection = (ConnectionImpl) connections[i];
                    // do not forget to remove it from list of active
                    // connections
                    connections[i] = null;
                    break;
                }
            }

            if (connection != null) {
                connection.close();
                count--;
            } else {
                logger.warn(String.format("*********** connection not found ****, endpoint=%s, id=%s, total amount=%d, list=%s", this.getLocalName(), connectionID, count, this.getUsedConnections()));
            }

            if(count > 0){
            	this.state = EndpointState.BUSY;
            } else {
                this.state = EndpointState.READY;
                this.lastUsed = System.currentTimeMillis();
            }
        } finally {
            stateLock.unlock();
        }
    }

    private String getUsedConnections() {
        String s = "{";
        for (int i = 0; i < connections.length; i++) {            
            if (connections[i] != null ) {
                s += connections[i].getId() + " ";
            }
        }
        s+= "}";
        return s;
    }
    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.server.spi.Endpoint#deleteAllConnections();
     */
    public void deleteAllConnections() {
        stateLock.lock();
        try {
            for (int i = 0; i < connections.length; i++) {
                if (connections[i] != null) {
                    ((ConnectionImpl) connections[i]).close();
                    connections[i] = null;
                }
            }
            count = 0;
            this.state = EndpointState.READY;
            this.lastUsed = System.currentTimeMillis();
        } finally {
            stateLock.unlock();
        }
    }

    public void addNotificationListener(NotificationListener listener) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void removeNotificationListener(NotificationListener listener) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Connection getConnection(String connectionID) {
        for (int i = 0; i < connections.length; i++) {
            if (connections[i] != null && connections[i].getId().equals(connectionID)) {
                return connections[i];
            }
        }
        return null;
    }

    public Component getComponent(String name) {
        Collection<MediaSource> components = sources.values();
        for (MediaSource source : components) {
            if (source.getName().matches(name)) {
                return source;
            }
        }

        Collection<MediaSink> components2 = sinks.values();
        for (MediaSink sink : components2) {
            if (sink.getName().matches(name)) {
                return sink;
            }
        }

        return null;
    }


    public String getLocalAddress(String media) {
        return rtpFactory != null ? rtpFactory.getBindAddress() : "0.0.0.0";
    }

    public int getLocalPort(String media) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public abstract void unblock();
    public abstract void block();
    
    public long lastTimeUsed() {
        return this.lastUsed;
    }
}
