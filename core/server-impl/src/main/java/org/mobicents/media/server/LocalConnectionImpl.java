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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import org.mobicents.media.server.resource.Channel;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.ResourceUnavailableException;

/**
 *
 * @author kulikov
 */
public class LocalConnectionImpl extends ConnectionImpl {

    private LocalConnectionImpl otherConnection;

    public LocalConnectionImpl(Endpoint endpoint, ConnectionFactory factory) throws ResourceUnavailableException {
        super(endpoint, factory);
    }
    
    
    public String getLocalDescriptor() {
        return null;
    }

    public String getRemoteDescriptor() {
        return null;
    }

    public void setRemoteDescriptor(String descriptor) throws IOException, ResourceUnavailableException {
        
    }
    
    //This is just helper method if Controller doesn't care of MediaType.
    protected void bind() throws ResourceUnavailableException {
        Collection<MediaType> mediaTypes = getEndpoint().getMediaTypes();
        for (MediaType mediaType : mediaTypes) {
           this.bind(mediaType);
        }
    }
    
    public void setOtherParty(Connection other, MediaType mediaType) throws IOException {
    	//We want to setOtherPart only if this Connection is already joined to Endpoint source/sink
    	if((this.stream & mediaType.getMask()) != 0x0){
            //hold reference for each other
            this.otherConnection = (LocalConnectionImpl) other;
            otherConnection.otherConnection = this;
            
            int k = mediaType.getCode();
            
            //join channels
            Channel txChannel = txChannels[k];
            Channel rxChannel = otherConnection.rxChannels[k];
            
            if (txChannel != null && rxChannel != null) {
                txChannel.connect(rxChannel);
            }
            
            rxChannel = rxChannels[k];
            txChannel = otherConnection.txChannels[k];
            
            if (txChannel != null && rxChannel != null) {
                txChannel.connect(rxChannel);
            }
            
            this.setMode(mode[k], mediaType);   
    	}
    }
    
    public void setOtherParty(Connection other) throws IOException {
        //hold reference for each other
        this.otherConnection = (LocalConnectionImpl) other;
        otherConnection.otherConnection = this;

        //join channels
        Collection<MediaType> types = getEndpoint().getMediaTypes();
        for (MediaType mediaType : types) {
        	setOtherParty(other, mediaType);
        }
    }

    @Override
    public void close() {
        try {
            super.close();

            if (this.otherConnection == null) {
                return;
            }

            Collection<MediaType> types = getEndpoint().getMediaTypes();
            for (MediaType media : types) {
                Channel txChannel = txChannels[media.getCode()];
                Channel rxChannel = otherConnection.rxChannels[media.getCode()];

                if (txChannel != null && rxChannel != null) {
                    txChannel.disconnect(rxChannel);
                }

                rxChannel = rxChannels[media.getCode()];
                txChannel = otherConnection.txChannels[media.getCode()];

                if (txChannel != null && rxChannel != null) {
                    rxChannel.disconnect(txChannel);
                }
            }

            otherConnection.otherConnection = null;
            this.otherConnection = null;

        } catch (Exception e) {
        }
    }
    
    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.server.spi.Connection#getPacketsReceived(org.mobicents.media.server.spi.MediaType) 
     */
    public long getPacketsReceived(MediaType media) {
        Channel rxChannel = rxChannels[media.getCode()];
        return rxChannel != null ? rxChannel.getPacketsTransmitted() : 0;
    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.server.spi.Connection#getBytesReceived(org.mobicents.media.server.spi.MediaType) 
     */
    public long getBytesReceived(MediaType media) {
        Channel rxChannel = rxChannels[media.getCode()];
        return rxChannel != null ? rxChannel.getBytesTransmitted() : 0;
    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.server.spi.Connection#getBytesReceived() 
     */
    public long getBytesReceived() {
        long res = 0;
        for (int i = 0; i < rxChannels.length; i++) {
            if (rxChannels[i] != null) {
                res +=  rxChannels[i].getBytesTransmitted();
            }
        }
        return res;
    }
    
    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.server.spi.Connection#getPacketsTransmitted(org.mobicents.media.server.spi.MediaType) 
     */
    public long getPacketsTransmitted(MediaType media) {
        Channel txChannel = txChannels[media.getCode()];
        return txChannel != null ? txChannel.getPacketsTransmitted() : 0;
    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.server.spi.Connection#getBytesTransmitted(org.mobicents.media.server.spi.MediaType) 
     */
    public long getBytesTransmitted(MediaType media) {
        Channel txChannel = txChannels[media.getCode()];
        return txChannel != null ? txChannel.getBytesTransmitted() : 0;
    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.server.spi.Connection#getBytesTransmitted() 
     */
    public long getBytesTransmitted() {
        long res = 0;
        for (int i = 0; i < txChannels.length; i++) {
            if (txChannels[i] != null) {
                res += txChannels[i].getBytesTransmitted();
            }
        }
        return res;
    }
    
    @Override
    public String toString() {
        return "Local Connection [" + getEndpoint().getLocalName() + ", idx=" + getIndex() + "]";
    }

    public void setOtherParty(String media, InetSocketAddress address) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.server.spi.Connection#getJitter() 
     */
    public double getJitter(MediaType media) {
        return 0;
    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.server.spi.Connection#getJitter() 
     */
    public double getJitter() {
        return 0;
    }
    
}
