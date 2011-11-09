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

package org.mobicents.media.server.impl.rtp;

import org.mobicents.media.server.impl.rtp.sdp.RTPFormats;
import java.io.IOException;
import java.net.SocketAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.mobicents.media.MediaSink;
import org.mobicents.media.MediaSource;
import org.mobicents.media.server.impl.AbstractSink;
import org.mobicents.media.server.impl.AbstractSource;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormat;
import org.mobicents.media.server.io.network.ProtocolHandler;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.scheduler.Task;
import org.mobicents.media.server.spi.format.AudioFormat;
import org.mobicents.media.server.spi.format.FormatFactory;
import org.mobicents.media.server.spi.format.Formats;
import org.mobicents.media.server.spi.memory.Frame;
/**
 *
 * @author kulikov
 */
public class RTPDataChannel {

    private final static AudioFormat dtmfFormat = FormatFactory.createAudioFormat("telephony-event", 8000);
    
    private final static int PORT_ANY = -1;
    private final long ssrc = System.currentTimeMillis();

    //RTP Manager instance
    private RTPManager rtpManager;

    //UDP channels
    private DatagramChannel dataChannel;
    private DatagramChannel controlChannel;

    //Receiver and transmitter
    private Input input;
    private Output output;

    //RTP clock
    private RtpClock rtpClock;

    //allowed jitter
    private int jitter;

    //Media stream format
    private RTPFormats rtpFormats = new RTPFormats();

    //Remote peer address
    private SocketAddress remotePeer;
    private int sn;

    private int count;

    private RTPHandler rtpHandler;

    private volatile long rxCount;
    private volatile long txCount;

    private JitterBuffer rxBuffer;
    private ConcurrentLinkedQueue<Frame> txBuffer;

    private Formats formats = new Formats();
    
    //Formats for RTP events
    private RTPFormats eventFormats = new RTPFormats();
    
    /**
     * Create RTP channel instance.
     *
     * @param rtpManager RTP manager
     * @throws IOException
     */
    protected RTPDataChannel(RTPManager rtpManager) throws IOException {
        this.rtpManager = rtpManager;
        this.jitter = rtpManager.jitter;

        //open data channel
        rtpHandler = new RTPHandler();

        //create clock with RTP units
        rtpClock = new RtpClock(rtpManager.getClock());

        //receiver
        input = new Input(rtpManager.scheduler);

        //transmittor
        output = new Output(rtpManager.scheduler);

        rxBuffer = new JitterBuffer(rtpClock, jitter);
        rxBuffer.setListener(input);

        txBuffer = new ConcurrentLinkedQueue();
        
        //add RFC2833 dtmf event formats
        if (rtpManager.dtmf > 0) {
            RTPFormat f = new RTPFormat(rtpManager.dtmf, dtmfFormat);
            f.setClockRate(8000);
            eventFormats.add(f);
        }
    }

    /**
     * Gets the formats for RTP events.
     * 
     * @return collection of formats used for RTP events.
     */
    public RTPFormats getEventFormats() {
        return this.eventFormats;
    }
    
    /**
     * Binds channel to the first available port.
     *
     * @throws SocketException
     */
    public void bind() throws SocketException {
        try {
            dataChannel = rtpManager.udpManager.open(rtpHandler);
            
            //if control enabled open rtcp channel as well
            if (rtpManager.isControlEnabled) {
                controlChannel = rtpManager.udpManager.open(new RTCPHandler());
            }
        } catch (IOException e) {
            throw new SocketException(e.getMessage());
        }
        //bind data channel
        rtpManager.udpManager.bind(dataChannel, PORT_ANY);

        //if control enabled open rtcp channel as well
        if (rtpManager.isControlEnabled) {
            rtpManager.udpManager.bind(controlChannel, dataChannel.socket().getLocalPort() + 1);
        }
    }

    /**
     * Gets the port number to which this channel is bound.
     *
     * @return the port number.
     */
    public int getLocalPort() {
        return dataChannel != null? dataChannel.socket().getLocalPort() : 0;
    }

    /**
     * Sets the address of remote peer.
     *
     * @param address the address object.
     */
    public void setPeer(SocketAddress address) {
        this.remotePeer = address;
        if(dataChannel!=null && !dataChannel.isConnected() && rtpManager.udpManager.connectImmediately((InetSocketAddress)address))
        	try {
        		dataChannel.connect(address);
        	}
        	catch (IOException e) {           		
        	}        	
    }

    /**
     * Closes this socket.
     */
    public void close() {
        input.stop();
        output.stop();
        
        if(dataChannel.isConnected())
        	try {        
        		dataChannel.disconnect();        		
        	}
        	catch(IOException e) {        		
        	}        	        	        
        
        try {   
        	dataChannel.socket().close();
        	dataChannel.close();
        } catch(IOException e) {        		
        }  
        	
        if (controlChannel != null) {
            controlChannel.socket().close();
        }
    }

    /**
     * Gets the receiver stream.
     *
     * @return receiver as media source
     */
    public MediaSource getInput() {
        return input;
    }

    /**
     * Gets the transmitter stream.
     *
     * @return transceiver as media sink.
     */
    public MediaSink getOutput() {
        return output;
    }

    public int getPacketsLost() {
        return input.getPacketsLost();
    }

    public long getPacketsReceived() {
        return rxCount;
    }

    public long getPacketsTransmitted() {
        return txCount;
    }

    /**
     * Modifies the map between format and RTP payload number
     * 
     * @param rtpFormats the format map
     */
    public void setFormatMap(RTPFormats rtpFormats) {
        this.rtpFormats = rtpFormats;
        this.rxBuffer.setFormats(rtpFormats);
    }
    
    /**
     * Implements IO operations for RTP protocol.
     *
     * This class is attached to channel and when channel is ready for IO
     * the scheduler will call either receive or send.
     */
    private class RTPHandler implements ProtocolHandler {
        //The schedulable task for read operation
        private RxTask rx = new RxTask(rtpManager.scheduler);
        private TxTask tx = new TxTask(rtpManager.scheduler);

        private volatile boolean isReading = false;
        private volatile boolean isWritting;

        private final Integer rxMonitor = new Integer(1);
        private final Integer txMonitor = new Integer(2);

        private int i;
        /**
         * (Non Java-doc.)
         *
         * @see org.mobicents.media.server.io.network.ProtocolHandler#receive(java.nio.channels.DatagramChannel)
         */
        public void receive(DatagramChannel channel) {
        		count++;
        		rtpManager.scheduler.submit(rx,rtpManager.scheduler.RX_TASK_QUEUE);        	
        }

        public boolean isReadable() {
            return !this.isReading;
        }

        public boolean isWriteable() {
            return true;
        }

        protected void allowReading() {
                this.isReading = false;            
        }

        /**
         * (Non Java-doc.)
         *
         * @see org.mobicents.media.server.io.network.ProtocolHandler#send(java.nio.channels.DatagramChannel)
         */
        public void send(DatagramChannel channel) {
        		//at this point we are not writting directly
        		//instead the task for writting is scheduled to run at next time unit
        		rtpManager.scheduler.submit(tx,rtpManager.scheduler.OUTPUT_QUEUE);        	
        }

        public void setKey(SelectionKey key) {
        }

    }

    /**
     * Implements IO operations for RTCP protocol.
     * 
     */
    private class RTCPHandler implements ProtocolHandler {

        public void receive(DatagramChannel channel) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void send(DatagramChannel channel) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void setKey(SelectionKey key) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean isReadable() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean isWriteable() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    /**
     * Receiver implementation.
     *
     * The Media source of RTP data.
     */
    private class Input extends AbstractSource implements BufferListener {

        /**
         * Creates new receiver.
         */
        protected Input(Scheduler scheduler) {
            super("input", scheduler,scheduler.INPUT_QUEUE);
        }

        protected int getPacketsLost() {
            return 0;
        }

        /**
         * Accept new RTP packet.         *
         * This method is called by RX task.
         *
         * @param rtpPacket the new RTP data packet
         */
        protected void accept(RtpPacket rtpPacket) {
            //juts put it into the rx buffer
            rxBuffer.write(rtpPacket);
        }

        @Override
        public Frame evolve(long timestamp) {
            Frame f = rxBuffer.read(timestamp);
            return f;
        }

        /**
         * (Non Java-doc.)
         *
         * @see org.mobicents.media.MediaSource#getFormats()
         */
        public Formats getNativeFormats() {
            return formats;
        }

        /**
         * RX buffer's call back method.
         * 
         * This method is called when rxBuffer is full and it is time to start
         * transmission to the consumer.
         */
        public void onFill() {
            this.wakeup();
        }
    }

    /**
     * Transmitter implementation.
     *
     */
    private class Output extends AbstractSink {
        private TxTask tx = new TxTask(rtpManager.scheduler);
        protected boolean isWritable = true;

        /**
         * Creates new transmitter
         */
        protected Output(Scheduler scheduler) {
            super("Output", scheduler,scheduler.OUTPUT_QUEUE);
        }

        @Override
        public void onMediaTransfer(Frame frame) throws IOException {
        	if (dataChannel.isConnected()) {
        		txBuffer.offer(frame);
        		tx.perform();
        	}
        }

        /**
         * (Non Java-doc.)
         *
         * @see org.mobicents.media.MediaSink#getFormats()
         */
        public Formats getNativeFormats() {
            return formats;
        }
    }

    /**
     * Implements scheduled rx job.
     *
     */
    private class RxTask extends Task {

        //RTP packet representation
        private RtpPacket rtpPacket = new RtpPacket(8192, true);

        private RxTask(Scheduler scheduler) {
            super(scheduler);
        }

        public int getQueueNumber()
        {
        	return scheduler.RX_TASK_QUEUE;
        }
        
        /**
         * (Non Java-doc.)
         *
         * @see org.mobicents.media.server.scheduler.Task#getPriority()
         */
        public long getPriority() {
            return 0;
        }

        /**
         * (Non Java-doc.)
         *
         * @see org.mobicents.media.server.scheduler.Task#getDuration()
         */
        public long getDuration() {
            return 0;
        }

        /**
         * (Non Java-doc.)
         *
         * @see org.mobicents.media.server.scheduler.Task#perform()
         */
        public long perform() {
            try {
                //clean buffer before read
                rtpPacket.getBuffer().clear();
                
                SocketAddress currAddress=dataChannel.receive(rtpPacket.getBuffer());
                try {                	               
                	if(currAddress!=null && !dataChannel.isConnected())                	
                		dataChannel.connect(currAddress);
                }
                catch (IOException e) {            
                }
                                	
                while (currAddress != null) {                	
                    //put pointer to the begining of the buffer
                    rtpPacket.getBuffer().flip();

                    //queue packet into the receiver's jitter buffer
                    if (rtpPacket.getBuffer().limit() > 0) {
                        input.accept(rtpPacket);
                        rxCount++;
                    } 
                    rtpPacket.getBuffer().clear();
                    currAddress=dataChannel.receive(rtpPacket.getBuffer());
                }
            } catch (Exception e) {
                //TODO: handle error
            }
            rtpHandler.isReading = false;
            return 0;
        }

    }

    /**
     * Writer job.
     */
    private class TxTask extends Task {
        private RtpPacket rtpPacket = new RtpPacket(8192, true);
        private RTPFormat fmt;
        private long timestamp;
        
        private TxTask(Scheduler scheduler) {
            super(scheduler);
        }

        public int getQueueNumber()
        {
        	return scheduler.OUTPUT_QUEUE;
        }
        
        /**
         * (Non Java-doc.)
         *
         * @see org.mobicents.media.server.scheduler.Task#getPriority()
         */
        public long getPriority() {
            return 0;
        }

        /**
         * (Non Java-doc.)
         *
         * @see org.mobicents.media.server.scheduler.Task#getDuration()
         */
        public long getDuration() {
            return 0;
        }

        /**
         * (Non Java-doc.)
         *
         * @see org.mobicents.media.server.scheduler.Task#perform()
         */
        public long perform() {
            //TODO: add key frame flag
            while (!txBuffer.isEmpty()) {
                Frame frame = txBuffer.poll();

                //discard frame if format is unknown
                if (frame.getFormat() == null) {
                    return 0;
                }

                //if current rtp format is unknown determine it
                if (fmt == null) {
                    fmt = rtpFormats.getRTPFormat(frame.getFormat());
                    //format still unknown? discard packet
                    if (fmt == null) return 0;
                    //update clock rate
                    rtpClock.setClockRate(fmt.getClockRate());
                }

                //the format of previous sent packet is known
                //change it if current frame has another format
                if (!fmt.getFormat().matches(frame.getFormat())) {
                    fmt = rtpFormats.getRTPFormat(frame.getFormat());
                    //it is unknown format? drop packet
                    if (fmt == null) return 0;
                }

                //ignore frames with duplicate timestamp
                if (frame.getTimestamp()/1000000L == timestamp) {
                    return 0;
                }
                
                //convert to milliseconds first
                timestamp = frame.getTimestamp() / 1000000L;

                //convert to rtp time units
                timestamp = rtpClock.convertToRtpTime(timestamp);
                rtpPacket.wrap(false, fmt.getID(), sn++, timestamp,
                        ssrc, frame.getData(), frame.getOffset(), frame.getLength());

                frame.recycle();
                try {
                    if (dataChannel.isConnected()) {
                    	dataChannel.send(rtpPacket.getBuffer(),dataChannel.socket().getRemoteSocketAddress());
                        txCount++;
                    }
                } catch (Exception e) {
                    //TODO : handle IO problems
                }
            }

            output.isWritable = true;
            return 0;
        }

    }

}
