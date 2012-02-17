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

import org.mobicents.media.CheckPoint;
import org.mobicents.media.MediaSink;
import org.mobicents.media.MediaSource;
import org.mobicents.media.server.component.Mixer;
import org.mobicents.media.server.component.Splitter;
import org.mobicents.media.server.component.audio.AudioMixer;
import org.mobicents.media.server.impl.PipeImpl;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.FormatNotSupportedException;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.ModeNotSupportedException;
import org.mobicents.media.server.impl.rtp.RTPDataChannel;
import org.mobicents.media.server.utils.Text;
import org.apache.log4j.Logger;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents the bi-directional transition path for a particular media type stream.
 *
 *
 * @author kulikov
 */
public class Channel {
    protected BaseConnection connection;
    private MediaType mediaType;
    
    protected Mixer mixer;
    protected Splitter splitter;
    
    protected PipeImpl txPipe = new PipeImpl();
    protected PipeImpl rxPipe = new PipeImpl();
    protected PipeImpl localPipe = new PipeImpl();
    
    protected Channel otherChannel=null;
    protected Connections connections;
    
    private boolean isRTP = false;

    protected RecvOnlyMode recvOnly;
    protected SendOnlyMode sendOnly;
    protected SendRecvMode send_recv;
    protected NetworkLoopMode network_loop;
    protected CnfMode cnfMode;
    protected AtomicReference<Mode> mode=new AtomicReference();
    
    public Channel(BaseConnection connection, Connections connections, MediaType mediaType, Mixer mixer, Splitter splitter) throws Exception {
        this.connection = connection;
        this.mediaType = mediaType;

        this.mixer = mixer;
        this.splitter = splitter;
        
        this.connections=connections;   

        //create transmission switches
        recvOnly = new RecvOnlyMode(connections);
        sendOnly = new SendOnlyMode(connections);
        send_recv = new SendRecvMode(sendOnly, recvOnly);
        network_loop = new NetworkLoopMode(connections);
        cnfMode = new CnfMode(connection, connections, send_recv);
    }

    /**
     * Modify dtmf clamp for splitter
     * 
     * Applicable for audio channel only.
     * 
     * @param dtmfClamp the value of the dtmfClamp.
     */
    public void setDtmfClamp(Boolean dtmfClamp) {
    	splitter.setDtmfClamp(dtmfClamp);
    }
    
    /**
     * Modify gain of the tx stream.
     * 
     * Applicable for audio channel only.
     * 
     * @param gain the value of the gain.
     */
    public void setGain(double gain) {
        ((AudioMixer) mixer).setGain(gain);
    }
    
    /**
     * Gets the media type of this channel.
     *
     * @return the media type identifier.
     */
    public MediaType getMediaType() {
        return mediaType;
    }

    /**
     * Changes transmission mode.
     *
     * @param mode the identifier of transmission mode.
     * @throws ModeNotSupportedException
     */
    public void setMode(ConnectionMode mode) throws ModeNotSupportedException {
    	Mode selectedMode=this.mode.get();    	
    	if(selectedMode!=null && selectedMode.getID()==mode)
		{
			//same mode
			return;
		}
    	
    	//setting mode before updating so other threads will see it.
    	Mode newMode=convert(mode);
        this.mode.set(newMode);
        
        if (selectedMode != null) {
        	if(selectedMode.getID()==ConnectionMode.CONFERENCE)
        		connections.removeFromConference(connection);
        	
        	selectedMode.deactivate();
        }    	
    	
        if (newMode != null) {
            try {
            	if(newMode.getID()==ConnectionMode.CONFERENCE)
            		connections.addToConference(connection);
            	            	
            	newMode.activate();
            } catch (FormatNotSupportedException e) {
                throw new ModeNotSupportedException(e.getMessage());
            }
        }
    }

    /**
     * Gets the current transmission mode.
     *
     * @return the mode identifier.
     */
    public ConnectionMode getMode() {
    	Mode selectedNode=mode.get();
        return  selectedNode== null ? ConnectionMode.INACTIVE : selectedNode.getID();
    }
    
    /**
     * Creates switch executor for specified mode.
     *
     * @param mode the mode value.
     * @return switch executor
     * @throws ModeNotSupportedException
     */
    protected Mode convert(ConnectionMode mode) throws ModeNotSupportedException {
        switch (mode) {
            case RECV_ONLY:
                return recvOnly;
            case SEND_ONLY:
                return sendOnly;
            case SEND_RECV:
                return send_recv;
            case CONFERENCE:
                return cnfMode;
            case NETWORK_LOOPBACK:
                return network_loop;
            case INACTIVE:
                return null;
            default:
                throw new ModeNotSupportedException(mode);
        }
    }

    /**
     * Gets the access to the specified check point.
     *
     * @param i the check point identifier.
     * @return check point.
     */
    public CheckPoint getCheckPoint(int i) {
        switch(i) {
            case 5:
                return sendOnly.getCheckPoint(5);
            case 6:
                return recvOnly.getCheckPoint(6);
            case 7:
                return sendOnly.getCheckPoint(7);
            case 8:
                return recvOnly.getCheckPoint(8);
            case 9:
                return new CheckPointImpl(mixer.getOutput().getPacketsTransmitted(), mixer.getOutput().getBytesTransmitted());
            case 10:
                return new CheckPointImpl(splitter.getInput().getPacketsReceived(), splitter.getInput().getBytesReceived());
            default: throw new IllegalArgumentException();
        }
    }

    public String report() {
        StringBuilder builder = new StringBuilder();
        builder.append(splitter.report());
        builder.append(mixer.report());
        return builder.toString();
    }
    
    public void connect(RTPDataChannel other) {
    	this.isRTP = true;

        txPipe.connect(mixer.getOutput());
        txPipe.connect(other.getOutput());

        rxPipe.connect(splitter.getInput());
        rxPipe.connect(other.getInput());
    }

    /**
     * Joins with other channel
     *
     * @param other the other channel
     */
    public void connect(Channel other) {
    	localPipe.connect(mixer.getOutput());
    	localPipe.connect(other.splitter.getInput());
    	localPipe.start();
    	
    	other.localPipe.connect(other.mixer.getOutput());
        other.localPipe.connect(splitter.getInput());        
        other.localPipe.start();
        
        otherChannel=other;
    }

    /**
     * Disconnects this channel from another if it was connected previously
     */
    public void disconnect() {
    	rxPipe.stop();
    	rxPipe.disconnect();
    	
        txPipe.stop();
        txPipe.disconnect();
        
        localPipe.stop();
        localPipe.disconnect();
        
        if(otherChannel!=null)
        {
        	otherChannel.localPipe.stop();
        	otherChannel.localPipe.disconnect();
        	otherChannel=null;
        }
    }

    /**
     * Transmission mode
     */
    protected abstract class Mode {

        /**
         * Gets the unique identifier of the mode.
         *
         * @return mode identifier.
         */
        public abstract ConnectionMode getID();

        /**
         * Establishes transmission path according to this mode.
         */
        public abstract void activate() throws FormatNotSupportedException;

        /**
         * Clears media path.
         */
        public abstract void deactivate();
    }

    /**
     * Transmission from channel to connections
     */
    private class RecvOnlyMode extends Mode {

        private Connections connections;

        //connection's splitter output
        private MediaSource source;
        //endpoint's mixer input
        private MediaSink sink;
        //pipe for joining
        private PipeImpl pipe = new PipeImpl();

        public RecvOnlyMode(Connections connections) {
            this.connections = connections;
        }

        @Override
        public void activate() throws FormatNotSupportedException {
            //create input/output
            source = splitter.newOutput();
            sink = connections.getMixer(mediaType).newInput();

            //join
            pipe.connect(source);
            pipe.connect(sink);

            //enable transmission
            pipe.start();
            rxPipe.start();                        
        }

        @Override
        public void deactivate() {
            //break transmission
            rxPipe.stop();
            pipe.stop();                       
            
            //drop connection
            pipe.disconnect();            
            
            //release input/output
            splitter.release(source);
            connections.getMixer(mediaType).release(sink);
        }

        @Override
        public ConnectionMode getID() {
            return ConnectionMode.RECV_ONLY;
        }

        /**
         * Get access to check points.
         *
         * @param i the identifier of check point.
         * @return check point.
         */
        public CheckPoint getCheckPoint(int i) {
            switch(i) {
                case 8:
                    return new CheckPointImpl(source.getPacketsTransmitted(), source.getBytesTransmitted());
                case 6:
                    return new CheckPointImpl(sink.getPacketsReceived(), sink.getBytesReceived());
                default:
                    throw new IllegalArgumentException();
            }
        }
    }

    /**
     * Transmission from connections to channel.
     */
    public class SendOnlyMode extends Mode {

        private Connections connections;

        //connection's splitter output
        private MediaSource source;
        //endpoint's mixer input
        private MediaSink sink;
        //pipe for joining
        private PipeImpl pipe = new PipeImpl();

        private long mediaTime;
        
        public SendOnlyMode(Connections connections) {
            this.connections = connections;
        }

        @Override
        public void activate() throws FormatNotSupportedException {
            //create input/output
            source = connections.getSplitter(mediaType).newOutput();
            sink = mixer.newInput();

            //join
            pipe.connect(source);
            pipe.connect(sink);

            //start transmission
            mixer.getOutput().setMediaTime(mediaTime);
            mixer.start();            
            pipe.start();
            txPipe.start(); 
            localPipe.start();
            	            
        }

        @Override
        public void deactivate() {
            //remember media time
            this.mediaTime = mixer.getOutput().getMediaTime();
            
            //break transmission
            txPipe.stop();
            mixer.stop();
            pipe.stop();
            localPipe.stop();            
            	
            
            //unjoin
            pipe.disconnect();

            //release input/output
            connections.getSplitter(mediaType).release(source);
            mixer.release(sink);
        }

        @Override
        public ConnectionMode getID() {
            return ConnectionMode.SEND_ONLY;
        }

        /**
         * Get access to check points.
         *
         * @param i the identifier of check point.
         * @return check point.
         */
        public CheckPoint getCheckPoint(int i) {
            switch(i) {
                case 5:
                    return new CheckPointImpl(source != null ? source.getPacketsTransmitted() :0, source != null ? source.getBytesTransmitted() : 0);
                case 7:
                    return new CheckPointImpl(sink != null ? sink.getPacketsReceived() : 0, sink != null ? sink.getBytesReceived() : 0);
                default:
                    throw new IllegalArgumentException();
            }
        }
    }

    /**
     * Transmission in both directions
     */
    public class SendRecvMode extends Mode {
        //sendrecv mode is a combination of send and recv only

        private SendOnlyMode sendOnly;
        private RecvOnlyMode recvOnly;

        private SendRecvMode(SendOnlyMode sendOnly, RecvOnlyMode recvOnly) {
            this.sendOnly = sendOnly;
            this.recvOnly = recvOnly;
        }

        @Override
        public void activate() throws FormatNotSupportedException {
            recvOnly.activate();
            sendOnly.activate();
        }

        @Override
        public void deactivate() {
            sendOnly.deactivate();
            recvOnly.deactivate();
        }

        @Override
        public ConnectionMode getID() {
            return ConnectionMode.SEND_RECV;
        }
    }

    /**
     * Terminates channel to itself
     */
    public class NetworkLoopMode extends Mode {

        private Connections connections;
        
        //connection's splitter output
        private MediaSource tx;
        //endpoint's mixer input
        private MediaSink rx;
        //pipe for joining
        private PipeImpl pipe = new PipeImpl();

        public NetworkLoopMode(Connections connections) {
            this.connections = connections;
        }

        @Override
        public void activate() throws FormatNotSupportedException {
            //create input/output
            tx = splitter.newOutput();
            rx = mixer.newInput();
            
            //join
            pipe.connect(tx);
            pipe.connect(rx);
            
            //start transmission
            //pipe.setDebug(true);
            pipe.start();
            
            //start RTP
            if (isRTP) {
                rxPipe.start();
                txPipe.start();
            }
        }

        @Override
        public void deactivate() {
            //start RTP
            if (isRTP) {
                rxPipe.stop();
                txPipe.stop();
            }
            
            pipe.stop();
            
            //unjoin
            pipe.disconnect();

            //release input/output
            splitter.release(tx);
            mixer.release(rx);
        }

        @Override
        public ConnectionMode getID() {
            return ConnectionMode.NETWORK_LOOPBACK;
        }

    }

    /**
     * Conference mode
     */
    protected class CnfMode extends Mode {

        private Connections connections;
        private BaseConnection connection;

        private SendRecvMode sendRecv;

        public CnfMode(BaseConnection connection, Connections connections, SendRecvMode sendRecv) {
            this.connection = connection;
            this.connections = connections;
            this.sendRecv = sendRecv;
        }

        @Override
        public ConnectionMode getID() {
            return ConnectionMode.CONFERENCE;
        }

        @Override
        public void activate() throws FormatNotSupportedException {
            sendRecv.activate();
        }

        @Override
        public void deactivate() {        	
        	sendRecv.deactivate();
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

}
