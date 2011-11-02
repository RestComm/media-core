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
package org.mobicents.media.server.spi.rtp;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Collection;
import org.mobicents.media.Format;
import org.mobicents.media.MediaSink;
import org.mobicents.media.MediaSource;
import org.mobicents.media.server.spi.ResourceUnavailableException;
import org.mobicents.media.server.spi.dsp.Codec;

/**
 *
 * @author kulikov
 */
public interface RtpSocket {
    /**
     * Gets address to which this socked is bound.
     * 
     * @return either local address to which this socket is bound or public
     *         address in case of NAT translation.
     */
    public String getLocalAddress() ;

    /**
     * Returns port number to which this socked is bound.
     * 
     * @return port number or -1 if socket not bound.
     */
    public int getLocalPort();
    
    /**
     * Gets currently used audio/video profile
     * @return
     */
    public AVProfile getAVProfile();
    /**
     * Binds Datagram to the address sprecified.
     * 
     * @throws java.io.IOException
     * @throws org.mobicents.media.server.spi.ResourceUnavailableException
     */
    public void bind() throws IOException, ResourceUnavailableException;
    
    /**
     * Assigns remote end.
     * 
     * @param address
     *            the address of the remote party.
     * @param port
     *            the port number of the remote party.
     */
    public void setPeer(InetAddress address, int port) throws IOException;
    
    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.server.impl.rtp.RtpSocket#startSendStream(PushBufferDataSource);
     */
    public MediaSink getSendStream();
    
    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.server.impl.rtp.RtpSocket#startSendStream(PushBufferDataSource);
     */
    public MediaSource getReceiveStream();

    /**
     * Specifies format and payload id which will be used by this socket for transmission 
     * 
     * This methods should be used by other components which are responsible for SDP negotiation.
     * The socket itself can not negotiate SDP. 
     * 
     * 
     * @param payloadId rtp payload number
     * @param format the format object.
     */
    public void setFormat(int payloadId, Format format);

    /**
     * Assigns RFC2833 DTMF playload number.
     * 
     * @param dtmf the DTMF payload number.
     */
    public void setDtmfPayload(int dtmf);
    
    /**
     * Gets the list of used codecs.
     * 
     * @return list of codecs.
     */
    public Collection<Codec> getCodecs();
    
    /**
     * Gets the jitter for time of packet arrival
     * 
     * @return the value of jitter in milliseconds.
     */
    public int getJitter();

    /**
     * Assign new value of packet time arrival jitter.
     * 
     * @param jitter
     *            the value of jitter in milliseconds.
     */
    public void setJitter(int jitter);
    
    /**
     * Gets the number of received packets
     * 
     * @return the number of packets received
     */
    public int getPacketsReceived();
    
    /**
     * Gets the number of sent packets
     * 
     * @return the number of sent packets.
     */
    public int getPacketsSent();
    
    /**
     * Statistical method.
     * 
     * @return the number of bytes received.
     */
    public long getBytesReceived();
    
    /**
     * Statistical method.
     * 
     * @return the number of bytes sent.
     */
    public long getBytesSent();
    
    /**
     * Closes this socket and resets its streams;
     * This method is called by RtpSocket user.
     * 
     */
    public void release();
    
}
