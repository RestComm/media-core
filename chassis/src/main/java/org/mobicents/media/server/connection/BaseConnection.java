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

package org.mobicents.media.server.connection;

import org.mobicents.media.server.*;
import java.util.Collection;
import org.mobicents.media.CheckPoint;
import org.mobicents.media.server.component.Splitter;
import org.mobicents.media.server.component.audio.AudioMixer;
import org.mobicents.media.server.component.video.VideoMixer;
import org.mobicents.media.server.impl.rtp.sdp.AVProfile;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormat;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormats;
import org.mobicents.media.server.impl.rtp.sdp.SessionDescription;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.scheduler.Task;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.ConnectionEvent;
import org.mobicents.media.server.spi.ConnectionListener;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.ConnectionState;
import org.mobicents.media.server.spi.ConnectionFailureListener;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.ModeNotSupportedException;
import org.mobicents.media.server.spi.dsp.DspFactory;
import org.mobicents.media.server.spi.format.Format;
import org.mobicents.media.server.spi.format.Formats;
import org.mobicents.media.server.spi.listener.Listeners;
import org.mobicents.media.server.spi.listener.TooManyListenersException;
/**
 * Implements connection's FSM.
 *
 * @author kulikov
 */
public abstract class BaseConnection implements Connection {

    //Identifier of this connection
    private String id;

    //connections subsystem
    protected final Connections connections;

    //scheduler instance
    private Scheduler scheduler;

    /** FSM current state */
    private volatile ConnectionState state = ConnectionState.NULL;
    private final Object stateMonitor = new Integer(0);

    //connection event listeners
    private Listeners<ConnectionListener> listeners = new Listeners();

    //events
    private ConnectionEvent stateEvent;

    //media channels
    protected Channel audioChannel;
    protected Channel videoChannel;

    /** Remaining time to live in current state */
    private volatile long ttl;
    private HeartBeat heartBeat;

    /**
     * Creates basic connection implementation.
     *
     * @param id the unique identifier of this connection within endpoint.
     * @param endpoint the endpoint owner of this connection.
     */
    public BaseConnection(String id, Connections connections,Boolean isLocalToRemote) throws Exception {
        this.id = id;
        this.connections = connections;
        this.scheduler = connections.scheduler;
        
        heartBeat = new HeartBeat(scheduler);

        //intialize media channels
        if(!isLocalToRemote)
        {
        	audioChannel = new Channel(this, connections, MediaType.AUDIO, new AudioMixer(scheduler), new Splitter(scheduler));
        	videoChannel = new Channel(this, connections, MediaType.AUDIO, new VideoMixer(scheduler), new Splitter(scheduler));
        }
        else
        {
        	audioChannel = new LocalToRemoteChannel(this, connections, MediaType.AUDIO, new AudioMixer(scheduler), new Splitter(scheduler));
        	videoChannel = new LocalToRemoteChannel(this, connections, MediaType.AUDIO, new VideoMixer(scheduler), new Splitter(scheduler));
        }
        
        //initialize event objects
        this.stateEvent = new ConnectionEventImpl(ConnectionEvent.STATE_CHANGE, this);
    }
    
    /**
     * (Non Java-doc).
     *
     * @see org.mobicents.media.server.spi.Connection#getId()
     */
    public String getId() {
        return id;
    }

    /**
     * (Non Java-doc).
     *
     * @see org.mobicents.media.server.spi.Connection#getState()
     */
    public ConnectionState getState() {
        synchronized(stateMonitor) {
            return state;
        }
    }

    /**
     * Modifies state of the connection.
     *
     * @param state the new value for the state.
     */
    private void setState(ConnectionState state) {
        //change state
        this.state = state;
        this.ttl = state.getTimeout()*10 + 1;
        
        switch (state) {
            case HALF_OPEN:
            	//heartBeat.setDeadLine(scheduler.getClock().getTime() + 1000000000L);
                scheduler.submitHeatbeat(heartBeat);
                break;
            case NULL:
                heartBeat.cancel();
                break;
        }

        //notify listeners
        try {
            listeners.dispatch(stateEvent);
        } catch (Exception e) {
        }
    }

    /**
     * (Non Java-doc).
     *
     * @see org.mobicents.media.server.spi.Connection#getDescriptor()
     */
    public String getDescriptor() {
        return null;
    }

    /**
     * (Non Java-doc).
     *
     * @see org.mobicents.media.server.spi.Connection#getEndpoint() 
     */
    public Endpoint getEndpoint() {
        return connections.endpoint;
    }

    /**
     * (Non Java-doc).
     *
     * @see org.mobicents.media.server.spi.Connection#getMode(org.mobicents.media.server.spi.MediaType)
     */
    public ConnectionMode getMode(MediaType mediaType) {
    	return channel(mediaType).getMode();    	
    }

    /**
     * (Non Java-doc.)
     * 
     * @see org.mobicents.media.server.spi.Connection#setGain(double) 
     */
    public void setGain(double gain) {
        this.audioChannel.setGain(gain);
    }
    
    /**
     * (Non Java-doc.)
     * 
     * @see org.mobicents.media.server.spi.Connection#setDtmfClamp(boolean) 
     */
    public void setDtmfClamp(boolean dtmfClamp) {
    	audioChannel.setDtmfClamp(dtmfClamp);
    }
    
    
    /**
     * (Non Java-doc).
     *
     * @see org.mobicents.media.server.spi.Connection#setMode(org.mobicents.media.server.spi.ConnectionMode, org.mobicents.media.server.spi.MediaType)
     */
    public void setMode(ConnectionMode mode, MediaType mediaType) throws ModeNotSupportedException  {
    		ConnectionMode currentMode = this.getMode(mediaType);    		
    		try {
    			//check that mode is going to be changed really
    			if (currentMode == mode) {    				
    				return;
    			}
    			
    			//changing mode
    			channel(mediaType).setMode(mode);
    			connections.updateMode(mediaType);    			
    		} catch (ModeNotSupportedException e) {    			
    			//rollback to previous mode
    			channel(mediaType).setMode(currentMode);    			
    			connections.updateMode(mediaType);
    		}    	
    }

    /**
     * (Non Java-doc).
     *
     * @see org.mobicents.media.server.spi.Connection#setMode(org.mobicents.media.server.spi.ConnectionMode)
     */
    public void setMode(ConnectionMode mode) throws ModeNotSupportedException  {    	
    		this.setMode(mode, MediaType.AUDIO);
//        	this.setMode(mode, MediaType.VIDEO);
//        	System.out.println("Set video " + mode);    	
    }

    public CheckPoint getCheckPoint(int i) {
        return connections.getCheckPoint(MediaType.AUDIO, i);
    }
    
    public CheckPoint getCheckPoint(MediaType mediaType, int i) {
        return channel(mediaType).getCheckPoint(i);
    }
    
    /**
     * (Non Java-doc).
     *
     * @see org.mobicents.media.server.spi.Connection#addListener(org.mobicents.media.server.spi.ConnectionListener)
     */
    public void addListener(ConnectionListener listener) {
        try {
            listeners.add(listener);
        } catch (TooManyListenersException e) {
        }
    }


    /**
     * (Non Java-doc).
     *
     * @see org.mobicents.media.server.spi.Connection#removeListener(org.mobicents.media.server.spi.ConnectionListener)
     */
    public void removeListener(ConnectionListener listener) {
        listeners.remove(listener);
    }

    /**
     * Initiates transition from NULL to HALF_OPEN state.
     */
    public void bind() throws Exception {
        synchronized (stateMonitor) {
            //check current state
            if (this.state != ConnectionState.NULL) {
                throw new IllegalStateException("Connection already bound");
            }

            //execute call back
            this.onCreated();

            //update state
            setState(ConnectionState.HALF_OPEN);
        }
    }

    /**
     * Initiates transition from HALF_OPEN to OPEN state.
     */
    public void join() throws Exception {
        synchronized (stateMonitor) {
            if (this.state == ConnectionState.NULL) {
                throw new IllegalStateException("Connection not bound yet");
            }

            if (this.state == ConnectionState.OPEN) {
                throw new IllegalStateException("Connection opened already");
            }

            //execute callback
            this.onOpened();

            //update state
            setState(ConnectionState.OPEN);
        }
    }

    /**
     * Initiates transition from any state to state NULL.
     */
    public void close() {
        synchronized (stateMonitor) {
            if (this.state != ConnectionState.NULL) {
                this.onClosed();
                setState(ConnectionState.NULL);
            }
        }
    }

    /**
     * Sets connection failure listener.
     * 
     *
     */
    public abstract void setConnectionFailureListener(ConnectionFailureListener connectionFailureListener);
    
    /**
     * Called when connection created.
     */
    protected abstract void onCreated() throws Exception;

    /**
     * Called when connected moved to OPEN state.
     * @throws Exception
     */
    protected abstract void onOpened() throws Exception;

    /**
     * Called when connection is moving from OPEN state to NULL state.
     */
    protected abstract void onClosed();

    /**
     * Called if failure has bean detected during transition.
     */
    protected abstract void onFailed();

    /**
     * Gets supported formats for the specified media type.
     *
     * @param mt the media type
     * @return the list of formats supported by endpoint.
     */
    private Collection<Format> getFormats(MediaType mt) {
        return null;
    }

    /**
     * Gets the channel of specified media type.
     * 
     * @param mediaType the media type
     * @return channel
     */
    private Channel channel(MediaType mediaType) {
        switch(mediaType) {
            case AUDIO: return audioChannel;
            case VIDEO: return videoChannel;
            default: return null;
        }
    }

    private class HeartBeat extends Task {

        public HeartBeat(Scheduler scheduler) {
            super(scheduler);
        }        

        public int getQueueNumber()
        {
        	return scheduler.HEARTBEAT_QUEUE;
        }   
        
        @Override
        public long perform() {
                synchronized(stateMonitor) {
                	ttl--;
                    if (ttl == 0) {
                        //setState(ConnectionState.NULL);
                        close();
                    } else {
                        //setDeadLine(scheduler.getClock().getTime() +  1000000000L);
                        scheduler.submitHeatbeat(this);
                    }
                }            
            return 0;
        }

    }
}
