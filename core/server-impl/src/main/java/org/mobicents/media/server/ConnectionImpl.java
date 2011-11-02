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

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.mobicents.media.Component;
import org.mobicents.media.server.resource.Channel;
import org.mobicents.media.server.resource.ChannelFactory;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.ConnectionListener;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.ConnectionState;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.NotificationListener;
import org.mobicents.media.server.spi.ResourceUnavailableException;
import org.mobicents.media.server.spi.clock.Task;

/**
 *
 * @author kulikov
 * @author amit bhayani
 */
public abstract class ConnectionImpl implements Connection, Task {

    private static int GEN = 1;
    private String id;
    private int index;
    private BaseEndpointImpl endpoint;
    private ConnectionState state = ConnectionState.NULL;
    
    //The default ConnectionMode is INACTIVE where there is no tx or rx
    protected ConnectionMode[] mode = {ConnectionMode.INACTIVE, ConnectionMode.INACTIVE};    //media channels
    protected Channel[] txChannels = new Channel[2];
    protected Channel[] rxChannels = new Channel[2];
    protected ConcurrentHashMap connectionListeners = new ConcurrentHashMap();
    
    protected volatile int stream;
    
    //The default lifeTime for each ConnectionState. For NULL lifetime is 0 sec, for 
    //IDLE lifetime is 30Sec, for HALF_OPEN lifetime is 60min, for OPEN lifetime
    //is 60min, for CLOSED is 0
    private int[] lifeTime = new int[]{0, 1000*30, 1000*60*60, 1000*60*60, 0};
    private long startTime;

    private final static Logger logger = Logger.getLogger(ConnectionImpl.class);
    public ConnectionImpl(Endpoint endpoint, ConnectionFactory factory) throws ResourceUnavailableException {
        this.endpoint = (BaseEndpointImpl) endpoint;
        this.id = this.genID();

        //getting connection factory
        initRx(factory);
        initTx(factory);
    }

    /**
     * Initializes tx channel for all media sources declared by endpoint.
     * 
     * This methods scrolls over list of media sources then it creates channel and 
     * connecting channel created to the respective media source. The channel is kept 
     * in the list of tx channels where index is media type.
     * 
     * @param factory
     */
    private void initTx(ConnectionFactory factory) throws ResourceUnavailableException {
        //getting rx factory
        ChannelFactory[] txFactories = factory.getTxFactory();

        //creating channel for each media source joing channel with source
        Collection<MediaType> mediaTypes = endpoint.getMediaTypes();
        for (MediaType mediaType : mediaTypes) {
            //getting rx channel factory for media type and create channel
            ChannelFactory channelFactory = txFactories[mediaType.getCode()];
            if (channelFactory != null) {
                Channel channel = channelFactory.newInstance(endpoint, mediaType);
                channel.setEndpoint(endpoint);
                channel.setConnection(this);
                txChannels[mediaType.getCode()] = channel;
            }
        }
    }

    /**
     * Initializes RX channel for all media sinks declared by endpoint.
     * 
     * This methods scrolls over list of media siks then it creates channel and 
     * connecting channel created to the respective media sink. The channel is kept 
     * in the list of rx channels where index is media type.
     * 
     * @param factory
     */
    private void initRx(ConnectionFactory factory) throws ResourceUnavailableException {
        //getting rx factory
        ChannelFactory[] rxFactories = factory.getRxFactory();

        //creating channel for each media source joing channel with source
        Collection<MediaType> mediaTypes = endpoint.getMediaTypes();
        for (MediaType mediaType : mediaTypes) {
            //getting rx channel factory for media type and create channel
            ChannelFactory channelFactory = rxFactories[mediaType.getCode()];
            if (channelFactory != null) {
                Channel channel = channelFactory.newInstance(endpoint, mediaType);
                channel.setEndpoint(endpoint);
                channel.setConnection(this);
                rxChannels[mediaType.getCode()] = channel;
            }
        }
    }
    
    protected void join(MediaType mediaType){
    	int i = mediaType.getCode();
    	
    	//connecting rx channels to sinks
    	if (rxChannels[i] != null && (endpoint.getSink(mediaType) != null)) {
    		rxChannels[i].connect(endpoint.getSink(mediaType));
    	}
    	
    	//connecting tx channels to sinks
    	if (txChannels[i] != null && (endpoint.getSource(mediaType) != null)) {
    		txChannels[i].connect(endpoint.getSource(mediaType));
    	}
    	
    	if(this.state == ConnectionState.NULL){
    		setState(ConnectionState.IDLE);
    	}
    	
    	this.stream |= mediaType.getMask();
    }

    /**
     * Joins channels with sources and sinks.
     */
    protected void join() {
        Collection<MediaType> mediaTypes = endpoint.getMediaTypes();
        for (MediaType mediaType : mediaTypes) {
        	this.join(mediaType);
        }
    }
    
    protected void bind(MediaType mediaType) throws ResourceUnavailableException {
    	//this.setMode(mode, mediaType);
    	
    	//Set the state to HALF_OPEN only if its IDLE. 
    	//TODO That means if the first stream was suppose Audio, 
    	//and latter user decided it also wants Video, in this case event 
    	//will not be fired about state change. Should state also be specific to MediaType?
    	if(this.state == ConnectionState.IDLE){
    		setState(ConnectionState.HALF_OPEN);
    	}
    }

    /**
     * Generates unique identifier for this connection.
     * 
     * @return hex view of the unique integer.
     */
    private String genID() {
        GEN++;
        if (GEN == Integer.MAX_VALUE) {
            GEN = 1;
        }
        return Integer.toHexString(GEN);
    }

    public String getId() {
        return this.id;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public ConnectionState getState() {
        return state;
    }

    protected void setState(ConnectionState state) {
        ConnectionState oldState = this.state;
        this.state = state;

        if (oldState != this.state) {
            //FIXME: does this require sync, maybe we should add here tasks?
            for (Object ocl : this.connectionListeners.keySet()) {
                ConnectionListener cl = (ConnectionListener) ocl;
                cl.onStateChange(this, oldState);
            }
        }
    }

    public int[] getLifeTime() {
    	return this.lifeTime;
    }

    public void setLifeTime(int[] lifeTime) {
    	this.lifeTime = lifeTime;
    }

    public ConnectionMode getMode(MediaType mediaType){
    	return this.mode[mediaType.getCode()];
    }
    
    public void setMode(ConnectionMode mode, MediaType mediaType){
    	//We want to set Mode only if we are joined
    	if((this.stream & mediaType.getMask()) != 0x0){
            Channel txChannel = txChannels[mediaType.getCode()];
            Channel rxChannel = rxChannels[mediaType.getCode()];
            if (mode == ConnectionMode.SEND_RECV) {
                if (txChannel != null) {
                    txChannel.start();
                }
                if (rxChannel != null) {
                    rxChannel.start();
                }
            } else if (mode == ConnectionMode.SEND_ONLY) {
                if (txChannel != null) {
                    txChannel.start();
                }
                if (rxChannel != null) {
                    rxChannel.stop();
                }
            } else if (mode == ConnectionMode.RECV_ONLY) {
                if (txChannel != null) {
                    txChannel.stop();
                }
                if (rxChannel != null) {
                    rxChannel.start();
                }
            } if (mode == ConnectionMode.INACTIVE) {
                if (txChannel != null) {
                    txChannel.stop();
                }
                if (rxChannel != null) {
                    rxChannel.stop();
                }            	
            }
            
            //Set Mode only for passed MediaType
            this.mode[mediaType.getCode()] = mode;
    	}
    }

    public void setMode(ConnectionMode mode) {
        setMode(mode, MediaType.AUDIO);
        setMode(mode, MediaType.VIDEO);
    }
    
    public Endpoint getEndpoint() {
        return endpoint;
    }

    public void addListener(ConnectionListener listener) {
        this.connectionListeners.put(listener, listener);
    }

    public void removeListener(ConnectionListener listener) {
        this.connectionListeners.remove(listener);
    }

    public Component getComponent(String name) {
        Component c = getComponent(rxChannels, name);
        if (c == null) {
            c = getComponent(txChannels, name);
        }
        return c;
    }

    private Component getComponent(Channel[] channels, String name) {
        for (Channel channel : channels) {
            Component component = channel.getComponent(name);
            if (component != null) {
                return component;
            }
        }
        return null;
    }

    public Component getComponent(MediaType type, Class interface1) {
        Component c = this.getComponent(rxChannels[type.getCode()], interface1);
        if (c == null) {
            c = this.getComponent(txChannels[type.getCode()], interface1);
        }
        return c;
    }

    private Component getComponent(Channel channel, Class interface1) {
        if (channel != null) {
            return channel.getComponent(interface1);
        }
        return null;
    }

    public void addNotificationListener(NotificationListener listener) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void removeNotificationListener(NotificationListener listener) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    protected void close(MediaType mediaType){

    	//We want to disconnect only if already joined
    	if((this.stream & mediaType.getMask()) != 0x0){
	    	int i = mediaType.getCode();
	
	        //connecting rx channels to sinks
	        if (rxChannels[i] != null && endpoint.getSink(mediaType) != null) {
	        	rxChannels[i].stop();
	        	rxChannels[i].disconnect(endpoint.getSink(mediaType));
	        }
	
	        //connecting tx channels to sinks
	        if (txChannels[i] != null && endpoint.getSource(mediaType) != null) {
	        	txChannels[i].stop();
	        	txChannels[i].disconnect(endpoint.getSource(mediaType));
	        }
	        
	        //reset the mode to INACTIVE
	        this.mode[i] = ConnectionMode.INACTIVE;
	        
	        this.stream ^= mediaType.getMask();
	        
	        if((this.stream & 0x03) == 0x0){
	        	setState(ConnectionState.NULL);
	        }
    	}
    }

    protected void close() {
        Collection<MediaType> mediaTypes = endpoint.getMediaTypes();
        for (MediaType mediaType : mediaTypes) {
        	this.close(mediaType);
        }
    }
    
    
    //methods of Task
    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public void cancel() {
    }
   
    public boolean isActive(){
    	return !((this.state == ConnectionState.NULL) || (this.state == ConnectionState.CLOSED));
    }
    
    public int perform(){
    	long currentTime = System.currentTimeMillis();
    	long endTime = this.startTime + this.lifeTime[this.state.getCode()];
    	
    	if(currentTime >= endTime){
            logger.info("Time out for state = " + this.state + " connection " + this);
    		//Time to close this connection
    		this.endpoint.deleteConnection(this.id);
    		
    		return 0;
    	} else{
    		return (int)(endTime - currentTime);
    	}
    	
    }
    
}
