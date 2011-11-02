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
package org.mobicents.media.server.impl.rtp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;

import net.java.stun4j.StunAddress;
import net.java.stun4j.StunException;
import net.java.stun4j.client.NetworkConfigurationDiscoveryProcess;
import net.java.stun4j.client.StunDiscoveryReport;

import org.apache.log4j.Logger;
import org.mobicents.media.server.impl.rtp.clock.AudioClock;
import org.mobicents.media.server.impl.rtp.clock.VideoClock;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.ResourceUnavailableException;
import org.mobicents.media.server.spi.rtp.RtpManager;
import org.mobicents.media.server.spi.dsp.Codec;
import org.mobicents.media.server.spi.dsp.CodecFactory;
import org.mobicents.media.server.spi.rtp.AVProfile;
import org.mobicents.media.server.spi.rtp.RtpListener;
import org.mobicents.media.server.spi.rtp.RtpSocket;

/**
 * 
 * @author Oleg Kulikov
 * @author amit bhayani
 */
public class RtpFactory implements RtpManager {

    /** UDP Receiver */
    private Receiver receiver;
    
    /** Jitter value*/
    private Integer jitter = 60;
    
    /** Bind address */
    private InetAddress bindAddress;
    protected InetSocketAddress publicAddress;
    private String stunHost;
    private int stunPort = 3478;
    
    /** Default audio/video profile */
    private AVProfile avProfile = new AVProfile();
    
    /** List of codecs */
    private Hashtable<MediaType, List<CodecFactory>> codecFactories;
    
    /** Queue for socket registration */
    protected BufferConcurrentLinkedQueue<RtpSocketImpl> registerQueue = new BufferConcurrentLinkedQueue();

    private int evenPortIndex;
    private int oddPortIndex;
    
    /** Available port range */
    private int lowPort = 1024;
    private int highPort = 65535;
    
    /** RTP event listener */
    protected RtpListener listener;
    
    /** Logger instance */
    private transient Logger logger = Logger.getLogger(RtpFactory.class);

    private boolean silenceSuppression = false;
    
    /**
     * Creates RTP Factory instance
     */
    public RtpFactory() {
    }

    public void setListener(RtpListener listener) {
        this.listener = listener;
    }

    public RtpListener getListener() {
        return listener;
    }
    
    protected void notify(Exception e) {
        if (listener != null) {
            listener.notify(e);
        }
    }

    /**
     * Gets the address of stun server if present.
     * 
     * @return the address of stun server or null if not assigned.
     */
    public String getStunAddress() {
        return stunHost == null ? null : stunPort == 3478 ? stunHost : (stunHost + ":" + stunPort);
    }

    /**
     * Assigns address of the STUN server.
     * 
     * @param address
     *            the address of the stun server in format host[:port]. if port is not set then default port is used.
     */
    public void setStunAddress(String address) {
        String tokens[] = address.split(":");
        stunHost = tokens[0];
        if (tokens.length == 2) {
            stunPort = Integer.parseInt(tokens[1]);
        }
    }

    public void start(long now) throws SocketException, IOException {
    	if(this.lowPort % 2 == 0){
    		this.evenPortIndex = this.lowPort;
    	} else {
    		this.evenPortIndex = this.lowPort + 1;
    		
    		//Reset lowPort
    		this.lowPort = this.evenPortIndex;
    	}
    	
    	this.oddPortIndex = this.evenPortIndex + 1;

        receiver = new Receiver(this);
        receiver.start();
    }

    public boolean isActive() {
        return receiver.isActive();
    }

    /**
     * Get the next even port to be used by the RtpSocket to bind RTP Socket
     * to passed port.
     * </br>
     * The portIndex increments cyclic starting from lowPort to highPort
     * and then back to lowPort
     * </br>
     * The Port is incremented by 2 as every alternate port is for RTCP.
     * @return
     */
    protected int getNextEvenPort() {
        this.evenPortIndex += 2;
        if (this.evenPortIndex > this.highPort) {
            this.evenPortIndex = this.lowPort;
        }
        return this.evenPortIndex;
    }
    
    /**
     * Get the next odd port to be used by the RtpSocket to bind RTCP Socket
     * to passed port.
     * </br>
     * The portIndex increments cyclic starting from lowPort to highPort
     * and then back to lowPort
     * </br>
     * The Port is incremented by 2 as every alternate port is for RTP.
     * @return
     */
    protected int getNextOddPort(){
    	this.oddPortIndex +=2;
        if (this.oddPortIndex > this.highPort) {
            this.oddPortIndex = this.lowPort+1;
        }
        return this.oddPortIndex;    	
    }

    public void stop() {
        receiver.stop();
    }

    /**
     * Gets the IP address to which trunk is bound. All endpoints of the trunk use this address for RTP connection.
     * 
     * @return the IP address string to which this trunk is bound.
     */
    public String getBindAddress() {
        return bindAddress != null ? bindAddress.getHostAddress() : null;
    }

    /**
     * Modify the bind address. All endpoints of the trunk use this address for RTP connection.
     * 
     * @param bindAddress
     *            IP address as string or host name.
     */
    public void setBindAddress(String bindAddress) throws UnknownHostException {
        this.bindAddress = InetAddress.getByName(bindAddress);
    }

    /**
     * Gets the minimum available port number.
     * 
     * @return port number
     */
    public int getLowPort() {
        return lowPort;
    }

    /**
     * Modifies minimum available port
     * 
     * @param lowPort the port number.
     */
    public void setLowPort(int lowPort) {
        this.lowPort = lowPort;
    }

    /**
     * Gets the maximum available port number.
     * 
     * @return port number
     */
    public int getHighPort() {
        return highPort;
    }

    /**
     * Modifies maximum available port
     * 
     * @param port the port number.
     */
    public void setHighPort(int port) {
        this.highPort = port;
    }

    /**
     * Gets the size of the jitter buffer in milliseconds.
     * 
     * Jitter buffer is used at the receiving ends of a VoIP connection. A jitter buffer stores received, time-jittered
     * VoIP packets, that arrive within its time window. It then plays stored packets out, in sequence, and at a
     * constant rate for subsequent decoding. A jitter buffer is typically filled half-way before playing out packets to
     * allow early, or late, packet-arrival jitter compensation.
     * 
     * Choosing a large jitter buffer reduces packet dropping from jitter but increases VoIP path delay
     * 
     * @return the size of the buffer in milliseconds.
     */
    public Integer getJitter() {
        return jitter;
    }

    /**
     * Modify size of the jitter buffer.
     * 
     * Jitter buffer is used at the receiving ends of a VoIP connection. A jitter buffer stores received, time-jittered
     * VoIP packets, that arrive within its time window. It then plays stored packets out, in sequence, and at a
     * constant rate for subsequent decoding. A jitter buffer is typically filled half-way before playing out packets to
     * allow early, or late, packet-arrival jitter compensation.
     * 
     * Choosing a large jitter buffer reduces packet dropping from jitter but increases VoIP path delay
     * 
     * @param jitter
     *            the new buffer's size in milliseconds
     */
    public void setJitter(Integer jitter) {
        this.jitter = jitter;
    }

    /**
     * Gets currently used Audio/Video profile.
     * 
     * @return audio/video profile.
     */
    public AVProfile getAVProfile() {
        return avProfile;
    }

    /**
     * Modify audio/video profile.
     * 
     * @param avProfile the new value of the audio/video profile.
     */
    public void setAVProfile(AVProfile avProfile) {
        this.avProfile = avProfile;
    }

    /**
     * Gets RTP clocks for specified media type.
     * 
     * @param media the media type
     * @return the clock instance
     */
    public RtpClock getClock(MediaType media) {
        if (media == MediaType.AUDIO) {
            return new AudioClock();
        } else if (media == MediaType.VIDEO) {
            return new VideoClock();
        }
        return null;
    }

    public void setSilenceSuppression(boolean value) {
        this.silenceSuppression = value;
    }
    
    public boolean isSilenceSuppressed() {
        return this.silenceSuppression;
    }
    
    /**
     * Gets list of assigned codecs.
     * 
     * @return the map between media type and list of codec factories.
     */
    public Hashtable<MediaType, List<CodecFactory>> getCodecs() {
        return codecFactories;
    }

    /**
     * Modify list of codecs.
     * 
     * @param codecFactories the map between media type and list of codec's factories.
     */
    public void setCodecs(Hashtable<MediaType, List<CodecFactory>> codecFactories) {
        this.codecFactories = codecFactories;
    }

    /**
     * Registers sockets in the receiver.
     * 
     * This method is called from Receiver. 
     * New socket is always placed into the registration queue and later receiver 
     * will call this method in the IO cycle. It will allow to prevent usage of expensive locks during IO.
     * 
     */
    protected void register() {
        //registering all sockets in the queue
        while (!registerQueue.isEmpty()) {
            //extract socket from queue
            RtpSocketImpl socket = registerQueue.poll();
            try {
                //registering
                socket.register(receiver.getSelector());
            } catch (ClosedChannelException e) {
                //unable to register, notify socket
                socket.notify(e);
            }
        }
    }

    /**
     * Constructs new RTP socket for the specified media type.
     * 
     * @return the RTPSocketInstance.
     * @throws StunException
     * @throws IOException
     * @throws SocketException
     * @throws StunException
     * @throws IOException
     */
    public RtpSocket getRTPSocket(MediaType media) throws IOException, ResourceUnavailableException {
        //check receiver state first
        if (!this.isActive()) {
            throw new ResourceUnavailableException("Receiver is not running");
        }
        
        RtpSocketImpl rtpSocket = new RtpSocketImpl(this, getCodecs(media), media, silenceSuppression);
        return rtpSocket;
    }

    /**
     * Creates list of codec for the specified media type.
     * 
     * @param media the media type
     * @return list of codecs.
     */
    private ArrayList<Codec> getCodecs(MediaType media) {
        ArrayList<Codec> codecs = new ArrayList();
        if (codecFactories != null) {
            Collection<CodecFactory> factories = codecFactories.get(media);
            if (factories != null) {
                for (CodecFactory factory : factories) {
                    codecs.add(factory.getCodec());
                }
            }
        }
        return codecs;
    }
    
    private InetSocketAddress getPublicAddress(InetSocketAddress localAddress) throws StunException {
        StunAddress local = new StunAddress(localAddress.getAddress(), localAddress.getPort());
        StunAddress stun = new StunAddress(stunHost, stunPort);

        // discovery stun server
        NetworkConfigurationDiscoveryProcess addressDiscovery = new NetworkConfigurationDiscoveryProcess(local, stun);
        try {
            addressDiscovery.start();
            StunDiscoveryReport report = addressDiscovery.determineAddress();
            return report.getPublicAddress().getSocketAddress();
        } finally {
            addressDiscovery.shutDown();
        }
    }
}
