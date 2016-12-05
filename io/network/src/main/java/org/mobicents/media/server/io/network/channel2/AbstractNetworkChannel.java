/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
 * by the @authors tag. 
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

package org.mobicents.media.server.io.network.channel2;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;

import org.apache.log4j.Logger;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public abstract class AbstractNetworkChannel implements NetworkChannel {

    // Data channel where data will be received and transmitted
    protected SelectionKey selectionKey;
    protected DatagramChannel dataChannel;
    
    // The buffer into which we will read data when it's available
    private static final int BUFFER_SIZE = 8192;
    private final ByteBuffer receiveBuffer;

    public AbstractNetworkChannel() {
        super();
        this.receiveBuffer = ByteBuffer.allocate(BUFFER_SIZE);
    }

    @Override
    public void bind(SocketAddress address) throws IOException {
        if (isOpen()) {
            this.dataChannel.bind(address);
        } else {
            throw new IOException("The channel is closed.");
        }
    }

    @Override
    public void open() throws IOException {
        if (!isOpen()) {
            this.dataChannel = DatagramChannel.open();
        } else {
            throw new IOException("Channel is already open.");
        }
    }

    @Override
    public void close() {
        if (isOpen()) {
            if (isConnected()) {
                try {
                    disconnect();
                } catch (IOException e) {
                    log().warn("Was unable to disconnect channel", e);
                }
            }
        }

        try {
            this.selectionKey.cancel();
            this.dataChannel.close();
        } catch (IOException e) {
            log().error("Could not close channel.", e);
        }
    }
    
    @Override
    public void connect(SocketAddress address) throws IOException {
        if(isOpen()) {
            this.dataChannel.connect(address);
        } else {
            throw new IOException("The channel is closed.");
        }
    }
    
    @Override
    public void disconnect() throws IOException {
        if(isConnected()) {
            this.dataChannel.disconnect();
        }
    }

    @Override
    public SocketAddress getLocalAddress() {
        if (isOpen()) {
            try {
                return this.dataChannel.getLocalAddress();
            } catch (IOException e) {
                log().warn("Cannot retrieve local address.", e);
            }
        }
        return null;
    }

    @Override
    public SocketAddress getRemoteAddress() {
        if (isConnected()) {
            try {
                return this.dataChannel.getRemoteAddress();
            } catch (IOException e) {
                log().warn("Cannot retrieve remote address.", e);
            }
        }
        return null;
    }

    @Override
    public boolean isOpen() {
        return this.dataChannel != null && this.dataChannel.isOpen();
    }

    @Override
    public boolean isConnected() {
        return this.dataChannel != null && this.dataChannel.isConnected();
    }
    
    @Override
    public void receive() throws IOException {
        // Get buffer ready for new data
        this.receiveBuffer.clear();
        
        // Read data from channel
        SocketAddress remotePeer = null;
        int dataLength = 0;
        try {
            remotePeer = dataChannel.receive(this.receiveBuffer);
            dataLength = this.receiveBuffer.position();
        } catch (IOException e) {
            dataLength = -1;
        }
        
        if (dataLength == -1) {
            // Stop if socket was shutdown or error occurred
            close();
            return;
        } else if (dataLength > 0) {
            // Copy data from buffer so we don't mess with original
            byte[] dataCopy = new byte[dataLength];
            this.receiveBuffer.rewind();
            this.receiveBuffer.get(dataCopy, 0, dataLength);

            // Handle incoming packet
            onIncomingPacket(dataCopy, remotePeer);
        }
    }
    
    @Override
    public void send(byte[] data) throws IOException {
        if(isConnected()) {
            this.dataChannel.send(ByteBuffer.wrap(data), this.dataChannel.getRemoteAddress());
        } else {
            throw new IOException("Channel is not connected");
        }
    }
    
    @Override
    public void send(byte[] data, SocketAddress remotePeer) throws IOException {
        if(isOpen()) {
            this.dataChannel.send(ByteBuffer.wrap(data), remotePeer);
        } else {
            throw new IOException("Channel is closed.");
        }
    }
    
    protected abstract void onIncomingPacket(byte[] data, SocketAddress remotePeer);

    protected abstract Logger log();
}
