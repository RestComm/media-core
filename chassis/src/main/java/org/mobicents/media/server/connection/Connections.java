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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.mobicents.media.CheckPoint;
import org.mobicents.media.MediaSink;
import org.mobicents.media.MediaSource;
import org.mobicents.media.server.BaseEndpointImpl;
import org.mobicents.media.server.component.Mixer;
import org.mobicents.media.server.component.Splitter;
import org.mobicents.media.server.component.audio.AudioMixer;
import org.mobicents.media.server.component.video.VideoMixer;
import org.mobicents.media.server.impl.PipeImpl;
import org.mobicents.media.server.impl.rtp.RTPManager;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.ConnectionType;
import org.mobicents.media.server.spi.FormatNotSupportedException;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.ModeNotSupportedException;
import org.mobicents.media.server.spi.ResourceUnavailableException;
import org.mobicents.media.server.spi.dsp.DspFactory;
import org.mobicents.media.server.spi.format.AudioFormat;
import org.mobicents.media.server.spi.format.FormatFactory;
import org.mobicents.media.server.spi.format.Formats;
import org.mobicents.media.server.spi.format.VideoFormat;
/**
 * Implements connection management subsystem.
 *
 * Procedure of joining endponts must work very fast however dynamic connection
 * creation upon request cause long and unpredictable delays. Preallocated
 * connection objects gives better result.
 *
 *
 * @author kulikov
 */
public class Connections {
    //endpoint running connections
    protected BaseEndpointImpl endpoint;

    //job scheduler
    protected Scheduler scheduler;

    //pool of local connections
    private ConcurrentLinkedQueue<BaseConnection> localConnections=new ConcurrentLinkedQueue();
    //pool of RTP connections
    private ConcurrentLinkedQueue<BaseConnection> rtpConnections=new ConcurrentLinkedQueue();

    //list of currently active connections
    protected ConcurrentHashMap<String,BaseConnection> activeConnections;    

    //used for referecning connection
    private BaseConnection connection;

    //audio channel joining connections with endpoint
    protected Channel audioChannel;

    //video channel joining connections with endpoint
    protected Channel videoChannel;

    /**
     * active local channels
     */
    protected ConcurrentHashMap<String,LocalChannel> localChannels = new ConcurrentHashMap();

    //intermediate audio and video formats.
    private Formats audioFormats = new Formats();
    private Formats videoFormats = new Formats();

    protected RTPManager rtpManager;
    /** Signaling processors factory */
    protected DspFactory dspFactory;
    /**
     * Creates new connections subsystem.
     *
     * @param endpoint the endpoint running connections
     * @param poolSize the number of available connections.
     */
    public Connections(BaseEndpointImpl endpoint, int localPoolSize, int rtpPoolSize, Boolean isLocalToRemote) throws Exception {
    	this.endpoint = endpoint;
        this.scheduler = endpoint.getScheduler();
        this.rtpManager = endpoint.getRtpManager();
        this.dspFactory = endpoint.getDspFactory();
        
        audioFormats.add(FormatFactory.createAudioFormat("linear", 8000, 16, 1));
        videoFormats.add(FormatFactory.createVideoFormat("unknown"));

        //creates transmission channels (between endpoint and connections)
        audioChannel = new Channel(new AudioMixer(scheduler), new Splitter(scheduler), MediaType.AUDIO);
        videoChannel = new Channel(new VideoMixer(scheduler), new Splitter(scheduler), MediaType.VIDEO);
        
        //fill pool with objects.
        int count = 1;

        //prepare local connections
        for (int i = 0; i < localPoolSize; i++) {
        	localConnections.add(new LocalConnectionImpl(Integer.toString(count++), this, isLocalToRemote));        	
        }

        //prepare rtp connections
        for (int i = 0; i < rtpPoolSize; i++) {
            rtpConnections.add(new RtpConnectionImpl(Integer.toString(count++), this, isLocalToRemote));
        }

        //create holder for active connections
        activeConnections = new ConcurrentHashMap(localPoolSize + rtpPoolSize);        
    }

    public BaseEndpointImpl getEndpoint()
    {
    	return endpoint;
    }
    
    public int getActiveConnectionsCount()
    {
    	return activeConnections.size();
    }
    
    /**
     * Creates connection with specified type.
     *
     * @param type the type of connection
     * @return connection instance.
     */
    public Connection createConnection(ConnectionType type) throws ResourceUnavailableException {
    	BaseConnection currConnection=null;
    	switch (type) {
           	case LOCAL:
           		currConnection=localConnections.poll();
           		if(currConnection!=null)
           			activeConnections.put(currConnection.getId(),currConnection);
           		
           		return currConnection;
           	case RTP:
           		currConnection=rtpConnections.poll();
           		if(currConnection!=null)
               		activeConnections.put(currConnection.getId(),currConnection);
           		
           		return currConnection;
           	default:
           		throw new ResourceUnavailableException("Unknown connection type");
    	}            	    
    }
    
    public void releaseConnection(BaseConnection connection,ConnectionType type)
    {
    	switch (type) {
       		case LOCAL:
       			activeConnections.remove(connection.getId());
       			localConnections.add(connection);
       			break;
       		case RTP:
       			activeConnections.remove(connection.getId());
       			rtpConnections.add(connection);       			
       			break;
    	}
    	
    	removeFromConference(connection);
    }

    /**
     * Gets the intermediate audio format.
     *
     * @return the audio format descriptor.
     */
    public AudioFormat getAudioFormat() {
        return (AudioFormat) this.audioFormats.get(0);
    }

    /**
     * Sets the intermediate audio format.
     *
     * @param audioFormat the audio format descriptor.
     */
    public void setAudioFormat(AudioFormat audioFormat) {
        this.audioFormats.clean();
        this.audioFormats.add(audioFormat);
    }

    /**
     * Gets the intermediate video format.
     *
     * @return the video format descriptor.
     */
    public VideoFormat getVideoFormat() {
        return (VideoFormat) this.videoFormats.get(0);
    }

    /**
     * Sets the intermediate video format.
     *
     * @param videoFormat the video format descriptor.
     */
    public void setVideoFormat(VideoFormat videoFormat) {
        this.videoFormats.clean();
        this.videoFormats.add(videoFormat);
    }
    
    /**
     * Gets intermediate as collection.
     * 
     * @param mediaType the media type 
     * @return the collection wich contains single element with intermediate format.
     */
    protected Formats getFormats(MediaType mediaType) {
        switch (mediaType) {
            case AUDIO:
                return audioFormats;
            case VIDEO:
                return videoFormats;
            default:
                return null;
        }
    }
    
    /**
     * Closes all activities connections.
     */
    public void release() {
    	BaseConnection currConnection;
        for(Enumeration<String> e = activeConnections.keys() ; e.hasMoreElements() ;) {
        	currConnection=activeConnections.remove(e.nextElement());
        	currConnection.close();
        	removeFromConference(currConnection);
        }        
    }

    /**
     * Get access to the mixer.
     *
     * @param mediaType the type of the mixer: audio or video
     * @return mixer component
     */
    public Mixer getMixer(MediaType mediaType) {
        switch (mediaType) {
            case AUDIO:

        }
        return audioChannel.mixer;
    }

    /**
     * Gets access to the splitter.
     *
     * @param mediaType the type of the splitter: audio or video
     * @return splitter component.
     */
    public Splitter getSplitter(MediaType mediaType) {
        return audioChannel.splitter;
    }

    /**
     * Gets the channel of specified media type
     *
     * @param mediaType the media type of stream
     * @return the channel streaming media with mediType content
     */
    private Channel getChannel(MediaType mediaType) {
        switch (mediaType) {
            case AUDIO:
                return this.audioChannel;
            case VIDEO:
                return this.videoChannel;            	
            default:
                return null;
        }
    }

    /**
     * Determines transmission mode between endpoint and connections.
     *
     * This method is called when mode of any connection has been changed.
     * It checks modes of all active connections and determines endpoint
     * transition mode as a combination of connections modes.
     */
    public void updateMode(MediaType mediaType) throws ModeNotSupportedException {
    	//originaly everything is false
    	boolean send = false;
    	boolean recv = false;
    	boolean loop = false;

    	Iterator<BaseConnection> currConnections=activeConnections.values().iterator();
    	//check mode for each active connection
    	while(currConnections.hasNext()) {
    		//if found connection with loopback mode no need to search more
    		if (loop) {
    			break;
    		}

    		connection = currConnections.next();
    		switch (connection.getMode(mediaType)) {
               	case SEND_ONLY:
               		send = true;
               		break;
               	case RECV_ONLY:
               		recv = true;
               		break;
               	case SEND_RECV:
               	case CONFERENCE:
               		send = true;
               		recv = true;               		
               		break;
               	case LOOPBACK:
               		loop = true;
               		break;
    		}
    	}

    	//loopback mode detected
    	if (loop) {
    		getChannel(mediaType).setMode(ConnectionMode.LOOPBACK);
    		return;
    	}

    	//send only detected
    	if (send && !recv) {
    		getChannel(mediaType).setMode(ConnectionMode.SEND_ONLY);
    		return;
    	}

    	//receive only detected
    	if (!send && recv) {
    		getChannel(mediaType).setMode(ConnectionMode.RECV_ONLY);
    		return;
    	}

    	//full duplex detected
    	if (send && recv) {
    		getChannel(mediaType).setMode(ConnectionMode.SEND_RECV);
    		return;
    	}

    	//inactivity
    	if (!send && !recv) {
    		getChannel(mediaType).setMode(null);
    		return;
    	}    	
    }

    protected String getChannelId(BaseConnection c1,BaseConnection c2)
    {
    	if(c1==null || c2==null)
    		return null;
    	
    	Integer val1=Integer.parseInt(c1.getId());
    	Integer val2=Integer.parseInt(c2.getId());
    	if(val1>val2)
    		return c1.getId() + ":" + c2.getId();
    	
    	return c2.getId() + ":" + c1.getId();
    }
    
    protected void addToConference(BaseConnection connection) {
    	BaseConnection c;   
    	LocalChannel channel,channel2;
    	for(Enumeration<String> e = activeConnections.keys() ; e.hasMoreElements() ;) {
    		String key=e.nextElement();
    		c=activeConnections.get(key);
    		if (c!=null && c.getMode(MediaType.AUDIO) == ConnectionMode.CONFERENCE  && c!=connection) {
    			channel = new LocalChannel();
    			channel2=localChannels.putIfAbsent(getChannelId(c,connection),channel);
    			if(channel2==null)
    				channel.join(c, connection);           
    		}
        }    	    	
    }
    
    protected void updateConnectionChannels(BaseConnection connection) {
    	String key;
    	LocalChannel channel;
    	for(Enumeration<String> e = localChannels.keys() ; e.hasMoreElements() ;) {
    		key=e.nextElement();
    		channel=localChannels.get(key);
    		if(channel!=null && channel.match(connection))
    			channel.update();
    	}
    }
    
    protected void removeFromConference(BaseConnection connection) {
    	//should remove all channells and not only one
        LocalChannel channel;        
        String key;
        for(Enumeration<String> e = localChannels.keys() ; e.hasMoreElements() ;) {
    		key=e.nextElement();
    		channel=localChannels.get(key);
    		if(channel.match(connection)) {
    			localChannels.remove(key);
    			channel.unjoin();    		
    		}
    	}
    }   
    
    public String report() {
        StringBuilder builder = new StringBuilder();
        builder.append(audioChannel.splitter.report());
        builder.append(audioChannel.mixer.report());
        return builder.toString();
    }
    /**
     * Reads data from specified check point.
     * 
     * @param n the identifier of check point
     * @return the data collected by checkpoint.
     */
    public CheckPoint getCheckPoint(MediaType mediaType, int n) {
        switch (n) {
            case 1:
                MediaSource s = endpoint.getSource(mediaType);
                return s == null? new CheckPointImpl(0,0) : 
                    new CheckPointImpl(s.getPacketsTransmitted(), s.getBytesTransmitted());
            case 2:
                MediaSink sink = endpoint.getSink(mediaType);
                return sink == null? new CheckPointImpl(0,0) :
                    new CheckPointImpl(sink.getPacketsReceived(), sink.getBytesReceived());
            case 3:
                sink = this.getChannel(mediaType).splitter.getInput();
                return new CheckPointImpl(sink.getPacketsReceived(), sink.getBytesReceived());
            case 4:
                s = this.getChannel(mediaType).mixer.getOutput();
                return new CheckPointImpl(s.getPacketsTransmitted(), s.getBytesTransmitted());
            default:
                throw new IllegalArgumentException("Unknown check point");
        }
    }

    /**
     * Transmission channel between connections and endpoint
     */
    protected class Channel {
        protected Mixer mixer;
        protected Splitter splitter;

        //current transmission mode
        private Mode mode;

        //available transmission modes
        private Mode sendOnly;
        private Mode recvOnly;
        private Mode sendRecv;
        private Mode loopback;

        //the media type of this channel
        private MediaType mediaType;

        /**
         * Creates new channel.
         *
         * @param mixer the mixer used for mixing signals from connections
         * @param splitter the splitter for splitting signals between connections
         * @param mediaType the media type of the stream
         */
        protected Channel(Mixer mixer, Splitter splitter, MediaType mediaType) {
            this.mixer = mixer;
            this.splitter = splitter;
            this.mediaType = mediaType;

            //prepare transmission modes
            sendOnly = new SendOnly(this);
            recvOnly = new RecvOnly(this);
            sendRecv = new SendRecv(this);
            loopback = new Loopback(this);
        }

        /**
         * Gets the media type of this channel
         *
         * @return the media type identifier
         */
        protected MediaType getMediaType() {
            return mediaType;
        }

        /**
         * Enables transmission in specified mode.
         *
         * @param mode the mode of transmission
         * @throws ModeNotSupportedException
         */
        protected void setMode(ConnectionMode mode) throws ModeNotSupportedException {
            //switch transmission of
            if (this.mode != null) {
                this.mode.off();
            }

            //inactive mode?
            if (mode == null) {
                return;
            }

            //change mode value
            switch (mode) {
                case SEND_ONLY:
                    this.mode = sendOnly;
                    break;
                case RECV_ONLY:
                    this.mode = recvOnly;
                    break;
                case SEND_RECV:
                    this.mode = sendRecv;
                    break;
                case LOOPBACK:
                    this.mode = loopback;
                    break;
            }

            //switch transmission on
            if (this.mode != null) {
                this.mode.on();
            }
        }
   }


    /**
     * Transmission mode
     */
    private abstract class Mode {
        //transmission channel
        protected Channel channel;
        
        /**
         * Creates mode.
         * 
         * @param channel transmission channel
         */
        protected Mode(Channel channel) {
            this.channel = channel;
        }

        /** 
         * Switches off transmission in this mode
         */
        protected abstract void off();

        /**
         * Switches on transmission in this mode
         */
        protected abstract void on() throws ModeNotSupportedException;

        /**
         * The number of frames received by endpoint.
         *
         * @return the number of frames.
         */
        protected abstract int rxPackets();

        /**
         * The number of frames transmitted by endpoint.
         *
         * @return the number of frames.
         */
        protected abstract int txPackets();
    }

    /**
     * This mode defines transmission from endpoint to connections.
     */
    private class SendOnly extends Mode {
        private long mediaTime;
        private MediaSource source;
        
        //pipe for joining endpoint and connections
        private PipeImpl pipe = new PipeImpl();

        /**
         * Creates new mode switch.
         *
         * @param channel the channel to wich this switch applies
         */
        protected SendOnly(Channel channel) {
            super(channel);
        }

        @Override
        protected void off() {
            pipe.stop();
            pipe.disconnect();
            mediaTime = source.getMediaTime();
        }

        @Override
        protected void on() throws ModeNotSupportedException {
            //get the endpoint source
            source = endpoint.getSource(channel.getMediaType());
            source.setMediaTime(mediaTime);
                        
            //check that endpoint has source
            if (source == null) {
                throw new ModeNotSupportedException("SEND_ONLY");
            }

            //assign formats
            try {
                channel.splitter.getInput().setFormats(source.getFormats());
            } catch (FormatNotSupportedException e) {
                throw new ModeNotSupportedException(e.getMessage());
            }

            //join source with channel
            pipe.connect(source);
            pipe.connect(channel.splitter.getInput());
            pipe.start();
        }

        @Override
        protected int rxPackets() {
            return 0;
        }

        @Override
        protected int txPackets() {
            return pipe.getTxPackets();
        }
    }

    /**
     * This mode defines the transmission to the endpoint
     */
    private class RecvOnly extends Mode {
        //transmission pipe
        private PipeImpl pipe = new PipeImpl();

        /**
         * Creates this switch
         * @param channel the transmission channel
         */
        protected RecvOnly(Channel channel) {
            super(channel);
        }

        @Override
        protected void off() {
            channel.mixer.stop();
            pipe.stop();
            pipe.disconnect();
        }

        @Override
        protected void on() throws ModeNotSupportedException {
            //get the sink at the endpoint side
            MediaSink sink = endpoint.getSink(channel.getMediaType());
            
            //check that sink is present
            if (sink == null) {
                throw new ModeNotSupportedException("RECV_ONLY");
            }

            //assign formats (enable transcoding if required)
            try {
                channel.mixer.getOutput().setFormats(sink.getFormats());
            } catch (FormatNotSupportedException e) {
                throw new ModeNotSupportedException(e.getMessage());
            }

            //join sink with channel and start transmission
            pipe.connect(sink);
            pipe.connect(channel.mixer.getOutput());
            
            channel.mixer.start();
            pipe.start();
        }

        @Override
        protected int rxPackets() {
            return pipe.getRxPackets();
        }

        @Override
        protected int txPackets() {
            return 0;
        }

    }

    /**
     * This mode defines transmission from and to endpoint.
     */
    private class SendRecv extends Mode {
        private Mode sendOnly;
        private Mode recvOnly;

        /**
         * Creates switch.
         *
         * @param channel transmission channel
         */
        protected SendRecv (Channel channel) {
            super(channel);
            sendOnly = new SendOnly(channel);
            recvOnly = new RecvOnly(channel);
        }

        @Override
        protected void off() {
            sendOnly.off();
            recvOnly.off();
        }

        @Override
        protected void on() throws ModeNotSupportedException {
            recvOnly.on();
            sendOnly.on();
        }

        @Override
        protected int rxPackets() {
            return recvOnly.rxPackets();
        }

        @Override
        protected int txPackets() {
            return sendOnly.txPackets();
        }

    }

    /**
     * In this mode the endpoint transmits media to itself.
     */
    private class Loopback extends Mode {
        private PipeImpl pipe = new PipeImpl();

        /**
         * Creates switch.
         *
         * @param channel not used.
         */
        protected Loopback(Channel channel) {
            super(channel);
        }

        @Override
        protected void off() {
            pipe.stop();
            pipe.disconnect();
        }

        @Override
        protected void on() throws ModeNotSupportedException {
            //get the sink from endpoint
            MediaSink sink = endpoint.getSink(channel.getMediaType());

            //get the source from endpoint
            MediaSource source = endpoint.getSource(channel.getMediaType());

            //check that both are present
            if (sink == null || source == null) {
                throw new ModeNotSupportedException("LOOPBACK");
            }

            //join sink and source and start transmission
            pipe.connect(sink);
            pipe.connect(source);
            pipe.start();
        }

        @Override
        protected int rxPackets() {
            return pipe.getRxPackets();
        }

        @Override
        protected int txPackets() {
            return pipe.getRxPackets();
        }

    }

    /**
     * Implements connection's check points
     */
    private class CheckPointImpl implements CheckPoint {
        private int frames, bytes;

        /**
         * Creates check point instance.
         *
         * @param frames
         * @param bytes
         */
        protected CheckPointImpl(long frames, long bytes) {
            this.frames = (int) frames;
            this.bytes = (int) bytes;
        }

        /**
         * (Non Java-doc.)
         *
         * @see org.mobicents.media.CheckPoint#getFrames()
         */
        public int getFrames() {
            return this.frames;
        }

        /**
         * (Non Java-doc.)
         *
         * @see org.mobicents.media.CheckPoint#getBytes()
         */
        public int getBytes() {
            return this.bytes;
        }

        @Override
        public String toString() {
            return String.format("frame=%d, bytes=%d", frames, bytes);
        }
    }

    /**
     * Channel for joining connections in CNF mode.
     */
    protected class LocalChannel {
    	private Party party1 = new Party();
        private Party party2 = new Party();

        private PipeImpl audioRxPipe = new PipeImpl();
        private PipeImpl audioTxPipe = new PipeImpl();
        
        public LocalChannel()
        {        	
        }
        
        //should be opposite since connecting inside endpoint and not to outside
        protected void join(BaseConnection connection1, BaseConnection connection2) {
        	boolean c1Send=true,c1Recv=true,c2Send=true,c2Recv=true;
        	        	
        	switch(connection1.getMode(MediaType.AUDIO))
        	{
        		case SEND_ONLY:
        			c1Recv=false;
        			break;
        		case RECV_ONLY:
        			c1Send=false;
        			break;
        		case NETWORK_LOOPBACK:
        			c1Send=false;
        			c1Recv=false;
        			break;
        	}
        	
        	switch(connection2.getMode(MediaType.AUDIO))
        	{
        		case SEND_ONLY:
        			c2Recv=false;
        			break;
        		case RECV_ONLY:
        			c2Send=false;
        			break;
        		case NETWORK_LOOPBACK:
        			c2Send=false;
        			c2Recv=false;        			
        			break;
        	}
        	
        	party1.send=c1Send;
        	party1.recv=c1Recv;
        	party2.send=c2Send;
        	party2.recv=c2Recv;
        	
        	party1.connection = connection1;
        	party2.connection = connection2;

            party1.source = connection1.audioChannel.splitter.newOutput();
            party2.sink = connection2.audioChannel.mixer.newInput();

            audioRxPipe.connect(party2.sink);
        	audioRxPipe.connect(party1.source);
        	
        	//logging details
        	Boolean firstLocal=false,secondLocal=false;
        	if(connection1 instanceof LocalConnectionImpl)
        		firstLocal=true;
        	if(connection2 instanceof LocalConnectionImpl)
        		secondLocal=true;
        	
        	if(c2Send && c1Recv)
            	audioRxPipe.start();
            
            party2.source = connection2.audioChannel.splitter.newOutput();
            party1.sink = connection1.audioChannel.mixer.newInput();

            audioTxPipe.connect(party1.sink);
        	audioTxPipe.connect(party2.source);
        	
            if(c1Send && c2Recv)
            	audioTxPipe.start();            
        }  
        
        protected void update() {
        	boolean c1Send=true,c1Recv=true,c2Send=true,c2Recv=true;
        	
        	switch(party1.connection.getMode(MediaType.AUDIO))
        	{
        		case SEND_ONLY:
        			c1Recv=false;
        			break;
        		case RECV_ONLY:
        			c1Send=false;
        			break;
        		case NETWORK_LOOPBACK:
        			c1Send=false;
        			c1Recv=false;
        			break;
        	}
        	
        	switch(party2.connection.getMode(MediaType.AUDIO))
        	{
        		case SEND_ONLY:
        			c2Recv=false;
        			break;
        		case RECV_ONLY:
        			c2Send=false;
        			break;
        		case NETWORK_LOOPBACK:        			
        			c2Send=false;
        			c2Recv=false;
        			break;
        	}                	
        	
        	//logging details
        	Boolean firstLocal=false,secondLocal=false;
        	if(party1.connection instanceof LocalConnectionImpl)
        		firstLocal=true;
        	if(party2.connection instanceof LocalConnectionImpl)
        		secondLocal=true;
        	
        	if(c2Send && c1Recv && !(party2.send && party1.recv))
        		audioRxPipe.start();
            else if(!(c2Send && c1Recv) && party2.send && party1.recv)
            	audioRxPipe.stop();
            
            if(c1Send && c2Recv && !(party1.send && party2.recv))
            	audioTxPipe.start();
            else if(!(c1Send && c2Recv) && party1.send && party2.recv)
            	audioTxPipe.stop();
            
            party1.send=c1Send;
        	party1.recv=c1Recv;
        	party2.send=c2Send;
        	party2.recv=c2Recv;
        }

        public boolean match(BaseConnection connection) {
            return connection == party1.connection || connection == party2.connection;
        }

        public void unjoin() {
            audioTxPipe.stop();
            audioTxPipe.disconnect();
            audioRxPipe.stop();
            audioRxPipe.disconnect();
            
            party1.release();
            party2.release();
        }
        
        public void setDebug(boolean isDebug) {
            audioRxPipe.setDebug(isDebug);
        }

        private class Party {
            private BaseConnection connection;
            private MediaSource source;
            private MediaSink sink;
            private Boolean send;
            private Boolean recv;
            
            private void release() {
                connection.audioChannel.splitter.release(source);
                connection.audioChannel.mixer.release(sink);
            }
        }
    }    
}
