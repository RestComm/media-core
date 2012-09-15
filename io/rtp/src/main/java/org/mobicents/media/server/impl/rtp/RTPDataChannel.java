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
import java.net.PortUnreachableException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.text.Format;
import org.mobicents.media.MediaSink;
import org.mobicents.media.MediaSource;
import org.mobicents.media.server.component.audio.CompoundComponent;
import org.mobicents.media.server.impl.rtp.rfc2833.DtmfConverter;
import org.mobicents.media.server.impl.AbstractSink;
import org.mobicents.media.server.impl.AbstractSource;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormat;
import org.mobicents.media.server.io.network.ProtocolHandler;
import org.mobicents.media.server.io.network.UdpManager;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.scheduler.Task;
import org.mobicents.media.server.spi.FormatNotSupportedException;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.format.AudioFormat;
import org.mobicents.media.server.spi.format.FormatFactory;
import org.mobicents.media.server.spi.format.Formats;
import org.mobicents.media.server.spi.memory.Frame;
import org.mobicents.media.server.spi.dsp.Codec;
import org.mobicents.media.server.spi.dsp.Processor;
import org.mobicents.media.server.utils.Text;

import org.apache.log4j.Logger;
/**
 *
 * @author Oifa Yulian
 */
public class RTPDataChannel {
	private AudioFormat format = FormatFactory.createAudioFormat("LINEAR", 8000, 16, 1);
	
    private final static int PORT_ANY = -1;
    private final long ssrc = System.currentTimeMillis();

    private final static AudioFormat dtmf = FormatFactory.createAudioFormat("telephone-event", 8000);
    static {
        dtmf.setOptions(new Text("0-15"));
    }
    
    //RTP Manager instance
    private ChannelsManager channelsManager;

    //UDP channels
    private DatagramChannel dataChannel;
    private DatagramChannel controlChannel;

    //Receiver and transmitter
    private RTPInput input;
    private RTPOutput output;
    //RTP dtmf event converter
    private DtmfConverter dtmfConverter;    
    
    //tx task - sender
    private TxTask tx = new TxTask();
    
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

    private Formats formats = new Formats();
    
    private Boolean shouldReceive=false;
    private Boolean shouldLoop=false;
    
    private HeartBeat heartBeat;
    private long lastPacketReceived;
    
    private RTPChannelListener rtpChannelListener;
    private Scheduler scheduler;
    private UdpManager udpManager;
    
    private Logger logger = Logger.getLogger(RTPDataChannel.class) ;
        
    private CompoundComponent compoundComponent;
    /**
     * Create RTP channel instance.
     *
     * @param channelManager Channel manager
     * 
     */
    protected RTPDataChannel(ChannelsManager channelsManager,int channelId) {    	
        this.channelsManager = channelsManager;
        this.jitter = channelsManager.getJitter();

        //open data channel
        rtpHandler = new RTPHandler();

        //create clock with RTP units
        rtpClock = new RtpClock(channelsManager.getClock());

        rxBuffer = new JitterBuffer(rtpClock, jitter);
        
        scheduler=channelsManager.getScheduler();
        udpManager=channelsManager.getUdpManager();
        //receiver
        input = new RTPInput(scheduler,rxBuffer);
        rxBuffer.setListener(input);
        
        //transmittor
        output = new RTPOutput(scheduler,this);               

        dtmfConverter=new DtmfConverter(scheduler,rtpClock);
        
        heartBeat=new HeartBeat(scheduler);
        
        formats.add(format);
        
        compoundComponent=new CompoundComponent(channelId); 
        compoundComponent.addInput(dtmfConverter.getCompoundInput());
        compoundComponent.addInput(input.getCompoundInput());
        compoundComponent.addOutput(output.getCompoundOutput());
    }
    
    public CompoundComponent getCompoundComponent()
    {
    	return this.compoundComponent;
    }
    
    public void setInputDsp(Processor dsp) {
    	input.setDsp(dsp);
    }
    
    public Processor getInputDsp() {
    	return input.getDsp();
    }
    
    public void setOutputDsp(Processor dsp) {
    	output.setDsp(dsp);
    }
    
    public Processor getOutputDsp() {
    	return output.getDsp();
    }
    
    public void setOutputFormats(Formats fmts) throws FormatNotSupportedException {
    	output.setFormats(fmts);
    }
    
    public void setRtpChannelListener(RTPChannelListener rtpChannelListener) {
    	this.rtpChannelListener=rtpChannelListener;
    }
    
    public void updateMode(ConnectionMode connectionMode)
    {
    	switch (connectionMode) {
        	case SEND_ONLY:
        		shouldReceive=false;
        		shouldLoop=false;
        		compoundComponent.updateMode(false,true);
        		dtmfConverter.deactivate();
        		input.deactivate();
        		output.activate();
        		break;
        	case RECV_ONLY:
        		shouldReceive=true;
        		shouldLoop=false;
        		compoundComponent.updateMode(true,false);
        		dtmfConverter.activate();
        		input.activate();
        		output.deactivate();
        		break;
        	case INACTIVE:
        		shouldReceive=false;
        		shouldLoop=false;
        		compoundComponent.updateMode(false,false);
        		dtmfConverter.deactivate();
        		input.deactivate();
        		output.deactivate();
        		break;
        	case SEND_RECV:
        	case CONFERENCE:
        		shouldReceive=true;
        		shouldLoop=false;
        		compoundComponent.updateMode(true,true);
        		dtmfConverter.activate();
        		input.activate();
        		output.activate();
        		break;
        	case NETWORK_LOOPBACK:
        		shouldReceive=false;
        		shouldLoop=true;
        		compoundComponent.updateMode(false,false);
        		dtmfConverter.deactivate();
        		input.deactivate();
        		output.deactivate();
        		break;
    	}
    	
    	boolean connectImmediately=false;
    	if(this.remotePeer!=null)
    		connectImmediately=udpManager.connectImmediately((InetSocketAddress)this.remotePeer);
    	
    	if(udpManager.getRtpTimeout()>0 && this.remotePeer!=null && !connectImmediately) {
    		if(shouldReceive) {
    			lastPacketReceived=scheduler.getClock().getTime();
    			scheduler.submitHeatbeat(heartBeat);
    		}
    		else {
    			heartBeat.cancel();
    		}
    	}
    }

    /**
     * Binds channel to the first available port.
     *
     * @throws SocketException
     */
    public void bind(boolean isLocal) throws IOException, SocketException {
    	try {
            dataChannel = udpManager.open(rtpHandler);
            
            //if control enabled open rtcp channel as well
            if (channelsManager.getIsControlEnabled()) {
                controlChannel = udpManager.open(new RTCPHandler());
            }
        } catch (IOException e) {
            throw new SocketException(e.getMessage());
        }
        //bind data channel
    	if(!isLocal) {
    		this.rxBuffer.setBufferInUse(true);
    		udpManager.bind(dataChannel, PORT_ANY);
    	} else {
    		this.rxBuffer.setBufferInUse(false);
    		udpManager.bindLocal(dataChannel, PORT_ANY);
    	}
    	
        //if control enabled open rtcp channel as well
        if (channelsManager.getIsControlEnabled()) {
        	if(!isLocal)
        		udpManager.bind(controlChannel, dataChannel.socket().getLocalPort() + 1);
        	else
        		udpManager.bindLocal(controlChannel, dataChannel.socket().getLocalPort() + 1);
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
    	boolean connectImmediately=false;
        if(dataChannel!=null)
        {
        	if(dataChannel.isConnected())
        		try {
        			dataChannel.disconnect();
        		}
    			catch (IOException e) {
    				logger.error(e);
    			}
    		
    		connectImmediately=udpManager.connectImmediately((InetSocketAddress)address);
        	if(connectImmediately)
        		try {
        			dataChannel.connect(address);        		
        		}
        		catch (IOException e) {    
        			logger.error(e);
        		}
        }
        
        if(udpManager.getRtpTimeout()>0 && !connectImmediately) {        	
        	if(shouldReceive) {
        		lastPacketReceived=scheduler.getClock().getTime();
        		scheduler.submitHeatbeat(heartBeat);
        	}
        	else {
        		heartBeat.cancel();
        	}
        }
    }

    /**
     * Closes this socket.
     */
    public void close() {
        if(dataChannel.isConnected())
        	try {        
        		dataChannel.disconnect();        		
        	}
        	catch(IOException e) {
        		logger.error(e);
        	}        	        	        
        
        try {   
        	dataChannel.socket().close();
        	dataChannel.close();
        } catch(IOException e) {
        	logger.error(e);
        }  
        	
        if (controlChannel != null) {
            controlChannel.socket().close();
        }
        
        //System.out.println("RX COUNT:" + rxCount + ",TX COUNT:" + txCount);
        rxCount=0;
        txCount=0;
        input.deactivate();
        dtmfConverter.deactivate();
        output.deactivate();        
        this.tx.clear();    	
        
        heartBeat.cancel();        
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
    	this.rtpHandler.flush();
    	this.rtpFormats = rtpFormats;
        this.rxBuffer.setFormats(rtpFormats);                        
    }        
    
    protected void send(Frame frame)
    {
    	if(dataChannel.isConnected())
    		tx.perform(frame);
    }
    
    /**
     * Implements IO operations for RTP protocol.
     *
     * This class is attached to channel and when channel is ready for IO
     * the scheduler will call either receive or send.
     */
    private class RTPHandler implements ProtocolHandler {
        //The schedulable task for read operation
        private RxTask rx = new RxTask();
        
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
        		rx.perform();        		        	
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

        private void flush()
        {
        	rx.flush();
        }
        
        public void onClosed()
        {
        	if(rtpChannelListener!=null)
        		rtpChannelListener.onRtpFailure();
        }
        
        /**
         * (Non Java-doc.)
         *
         * @see org.mobicents.media.server.io.network.ProtocolHandler#send(java.nio.channels.DatagramChannel)
         */
        public void send(DatagramChannel channel) {        		        
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
        
        public void onClosed() {
        
        }
    }
    
    /**
     * Implements scheduled rx job.
     *
     */
    private class RxTask {

        //RTP packet representation
        private RtpPacket rtpPacket = new RtpPacket(8192, true);
        private RTPFormat format;
        private SocketAddress currAddress;
        
        private RxTask() {            
        }

        private void flush()
        {
        	try {
        		//lets clear the receiver    	
        		currAddress=dataChannel.receive(rtpPacket.getBuffer());
        		rtpPacket.getBuffer().clear();
        	
        		while(currAddress!=null)
        		{
        			currAddress=dataChannel.receive(rtpPacket.getBuffer());
        			rtpPacket.getBuffer().clear();
        		}
        	}
        	catch (Exception e) {
        		logger.error(e);
            }
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
                
                currAddress=null;
                
                try {
                	currAddress=dataChannel.receive(rtpPacket.getBuffer());
                	if(currAddress!=null && !dataChannel.isConnected())
                	{
                		rxBuffer.restart();    	
                        dataChannel.connect(currAddress);                        
                	}
                	else if(currAddress!=null && rxCount==0)
                		rxBuffer.restart();
                }
                catch(PortUnreachableException e) {
                	//icmp unreachable received
                	//disconnect and wait for new packet
                	try
                	{
                		dataChannel.disconnect();
                	}
                	catch(IOException ex) {
                		logger.error(ex);
                	}
                }
                catch (IOException e) {  
                	logger.error(e);
                }
                                	
                while (currAddress != null) {
                	lastPacketReceived=scheduler.getClock().getTime();                	
                    //put pointer to the begining of the buffer
                    rtpPacket.getBuffer().flip();

                    if(rtpPacket.getVersion()!=0 && (shouldReceive || shouldLoop))
                    {
                    	//rpt version 0 packets is used in some application ,
                    	//discarding since we do not handle them
                    	//queue packet into the receiver's jitter buffer
                    	if (rtpPacket.getBuffer().limit() > 0) {
                    		if(shouldLoop && dataChannel.isConnected()) {                    			
                            	dataChannel.send(rtpPacket.getBuffer(),dataChannel.socket().getRemoteSocketAddress());
                            	rxCount++;
                            	txCount++;
                            }
                    		else if(!shouldLoop) {
                    			format = rtpFormats.find(rtpPacket.getPayloadType());
                    			if (format != null && format.getFormat().matches(dtmf))
                    				dtmfConverter.write(rtpPacket);
                    			else
                    				rxBuffer.write(rtpPacket,format);	
                        			
                    			rxCount++;                    			
                    		}                    			
                    	}
                    }
                    
                    rtpPacket.getBuffer().clear();
                    currAddress=dataChannel.receive(rtpPacket.getBuffer());
                }
            }
        	catch(PortUnreachableException e) {
            	//icmp unreachable received
            	//disconnect and wait for new packet
            	try
            	{
            		dataChannel.disconnect();
            	}
            	catch(IOException ex) {
            		logger.error(ex);
            	}
            }
        	catch (Exception e) {
            	logger.error(e);
            }
            
            rtpHandler.isReading = false;
            return 0;
        }
    }

    /**
     * Writer job.
     */
    private class TxTask {
    	private RtpPacket rtpPacket = new RtpPacket(8192, true);
        private RTPFormat fmt;
        private long timestamp=-1;
        
        private TxTask() {        	                       
        }

        /**
         * if connection is reused fmt could point to old codec , which in case will be incorrect
         *
         */
        public void clear() {
        	this.timestamp=-1;
        	this.fmt=null;
        }
        
        public void perform(Frame frame) {
            //discard frame if format is unknown
            if (frame.getFormat() == null) {
            	return;
            }

            //if current rtp format is unknown determine it
            if (fmt == null || !fmt.getFormat().matches(frame.getFormat())) {
                fmt = rtpFormats.getRTPFormat(frame.getFormat());
                //format still unknown? discard packet
                if (fmt == null) {
                	return;
                }
                //update clock rate
                rtpClock.setClockRate(fmt.getClockRate());
            }

            //ignore frames with duplicate timestamp
            if (frame.getTimestamp()/1000000L == timestamp) {
            	return;
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
            }
            catch(PortUnreachableException e) {
            	//icmp unreachable received
            	//disconnect and wait for new packet
            	try
            	{
            		dataChannel.disconnect();
            	}
            	catch(IOException ex) {
            		logger.error(ex);
            	}
            }
            catch (Exception e) {
            	logger.error(e);
            }
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
            if (scheduler.getClock().getTime()-lastPacketReceived>udpManager.getRtpTimeout()*1000000000L) {
            	if(rtpChannelListener!=null)
            		rtpChannelListener.onRtpFailure();
            } else {
                scheduler.submitHeatbeat(this);
            }
            return 0;
        }
    }
}
