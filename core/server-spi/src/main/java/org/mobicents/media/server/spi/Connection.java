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
package org.mobicents.media.server.spi;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;

import javax.sdp.SdpException;

import org.mobicents.media.Component;

/**
 *
 * @author Oleg Kulikov
 * @author amit bhayani
 */
public interface Connection extends Serializable {
    public final static int CHANNEL_RX = 0;
    public final static int CHANNEL_TX = 1;
    
    /**
     * Gets the identifier of this connection.
     *
     * @return hex view of the integer.
     */
    public String getId();
    
    /**
     * Returns state of this connection
     * @return
     */
    public ConnectionState getState();

    /**
     * Gets the time to live of the connection.
     * 
     * @return the time in seconds.
     */
    public int[] getLifeTime();
    
    /**
     * Modify life time of the connection.
     * 
     * @param lifeTime the time value in seconds.
     */
    public void setLifeTime(int[] lifeTime);
    
    /**
     * Gets the current mode of this connection.
     *
     * @return integer constant indicating mode.
     */
    public ConnectionMode getMode(MediaType mediaType);

    /**
     * Modify mode of this connection.
     *
     * @param mode the new value of the mode.
     */
    public void setMode(ConnectionMode mode, MediaType mediaType);
    
    /**
     * Modify mode of this connection for all known media types.
     * 
     * @param mode the new mode of the connection.
     */
    public void setMode(ConnectionMode mode);
    
    /**
     * Gets the endpoint which executes this connection.
     *
     * @return the endpoint object.
     */
    public Endpoint getEndpoint();

    /**
     * Gets the local descriptor of the connection.
     * The SDP format is used to encode the parameters of the connection.
     *
     * @return SDP descriptor.
     */
    public String getLocalDescriptor();
        
    /**
     * Gets the descriptor of the remote party.
     * The SDP format is used to encode the parameters of the connection.
     *
     * @return SDP descriptor.
     */
    public String getRemoteDescriptor();

    /**
     * Modify the descriptor of the remote party.
     * The SDP format is used to encode the parameters of the connection.
     *
     * @param remoteDescriptor the SDP descriptor of the remote party.
     * @throws ResourceUnavailableException 
     */
    public void setRemoteDescriptor(String descriptor) throws SdpException, IOException, ResourceUnavailableException;

    /**
     * Joins localy this and other connections.
     *
     * @param other the other connectio to join with.
     * @throws InterruptedException 
     */
    public void setOtherParty(Connection other) throws IOException;
    public void setOtherParty(Connection other, MediaType mediaType) throws IOException;
    public void setOtherParty(String media, InetSocketAddress address) throws IOException;

    /**
     * Adds connection state listener.
     * 
     * @param listener to be registered
     */
    public void addListener(ConnectionListener listener);
    public void addNotificationListener(NotificationListener listener);

    /**
     * Removes connection state listener.
     * 
     * @param listener to be unregistered
     */
    public void removeListener(ConnectionListener listener);
    public void removeNotificationListener(NotificationListener listener);

    public Component getComponent(MediaType type, Class _interface);
    
    /**
     * The number of packets of the specified media type received .
     * 
     * @param media the media type.
     * @return the number of packets.
     */
    public long getPacketsReceived(MediaType media);
    
    /**
     * The number of bytes of the specified media type received .
     * 
     * @param media the media type.
     * @return the number of bytes.
     */
    public long getBytesReceived(MediaType media);
    
    /**
     * The total number of bytes  received .
     * 
     * @return the number of bytes.
     */
    public long getBytesReceived();
    
    /**
     * The number of packets of the specified media type transmitted.
     * 
     * @param media the media type 
     * @return the number of packets.
     */
    public long getPacketsTransmitted(MediaType media);

    /**
     * The number of bytes of the specified media type transmitted.
     * 
     * @param media the media type 
     * @return the number of bytes.
     */
    public long getBytesTransmitted(MediaType media);
    
    
    /**
     * The total number of bytes transmitted.
     * 
     * @return the number of bytes.
     */
    public long getBytesTransmitted();
    
    /**
     * The interarrival jitter for the specific media type.
     * 
     * @param media the media type
     * @return jitter value
     */
    public double getJitter(MediaType media);
    
    /**
     * The average jitter value accross all media types.
     * 
     * @return average jitter value. 
     */
    public double getJitter();
    
}
