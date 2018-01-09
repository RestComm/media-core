/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */

package org.restcomm.media.network.deprecated.channel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Data channel that supports multiplexing.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * @deprecated Use {@link NetworkChannel} instead.
 */
@Deprecated
public class MultiplexedChannel implements Channel {
	
	private static final Logger logger = LogManager.getLogger(MultiplexedChannel.class);
	
	// Data channel where data will be received and transmitted
	protected SelectionKey selectionKey;
	protected DatagramChannel dataChannel;

	// Registered protocol handlers. Used for multiplexing.
	protected final PacketHandlerPipeline handlers;

	// The buffer into which we will read data when it's available
	private static final int BUFFER_SIZE = 8192;
	private final ByteBuffer receiveBuffer;
	
	// Data that is pending for writing
	private final Queue<byte[]> pendingData;

	public MultiplexedChannel() {
		this.handlers = new PacketHandlerPipeline();
		this.pendingData = new ConcurrentLinkedQueue<>();
		this.receiveBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
	}
	
	@Override
	public SocketAddress getLocalAddress() {
		if(isOpen()) {
			try {
				return this.dataChannel.getLocalAddress();
			} catch (IOException e) {
				logger.error("Could not get local addres of the data channel: "+ e.getMessage());
			}
		}
		return null;
	}
	
	@Override
	public String getLocalHost() {
		if(this.dataChannel != null && this.dataChannel.isOpen()) {
			try {
				return ((InetSocketAddress) this.dataChannel.getLocalAddress()).getHostString();
			} catch (IOException e) {
				logger.error("Could not lookup the local channel address: "+ e.getMessage(), e);
			}
		}
		return "";
	}
	
	@Override
	public int getLocalPort() {
		if(this.dataChannel != null && this.dataChannel.isOpen()) {
			return this.dataChannel.socket().getLocalPort();
		}
		return 0;
	}
	
	public String getRemoteHost() {
		if(this.dataChannel != null && this.dataChannel.isConnected()) {
			try {
				return ((InetSocketAddress) this.dataChannel.getRemoteAddress()).getHostString();
			} catch (IOException e) {
				logger.error("Could not lookup the remote peer address", e);
			}
		}
		return "";
	}
	
	public int getRemotePort() {
		if(this.dataChannel != null && this.dataChannel.isConnected()) {
			try {
				return ((InetSocketAddress) this.dataChannel.getRemoteAddress()).getPort();
			} catch (IOException e) {
				logger.error("Could not lookup the remote peer port", e);
			}
		}
		return 0;
	}
	
	/**
	 * 
	 * @param channel
	 * @deprecated use {@link #setSelectionKey(SelectionKey)}
	 */
	@Deprecated
	public void setTransport(final DatagramChannel channel) {
		this.dataChannel = channel;
	}
	
    protected void queueData(final byte[] data) {
        if (data != null && data.length > 0) {
            this.pendingData.offer(data);
        }
    }
	
	@Override
	public boolean hasPendingData() {
	    return !this.pendingData.isEmpty();
	}
	
    protected void flush() {
        try {
            this.receiveBuffer.clear();
            // lets clear the receiver
            SocketAddress currAddress = null;
            do {
                if (this.dataChannel != null && this.dataChannel.isOpen()) {
                    currAddress = this.dataChannel.receive(this.receiveBuffer);
                    this.receiveBuffer.clear();
                } else {
                    currAddress = null;
                }
            } while (currAddress != null);
        } catch (Exception e) {
            logger.warn("Stopped flushing the channel abruptly: " + e.getMessage());
        }
    }
	
	@Override
	public void receive() throws IOException {
		// Get buffer ready to read new data
		this.receiveBuffer.clear();

		// Read data from channel
		int dataLength = 0;
		try {
			SocketAddress remotePeer = dataChannel.receive(this.receiveBuffer);
			if (!isConnected() && remotePeer != null) {
				connect(remotePeer);
			}
			dataLength = this.receiveBuffer.position();
		} catch (IOException e) {
			dataLength = -1;
		}

		// Stop if socket was shutdown or error occurred
		if (dataLength == -1) {
			close();
			return;
		} else if (dataLength > 0) {
			// Copy data from buffer so we don't mess with original
			byte[] dataCopy = new byte[dataLength];
			this.receiveBuffer.rewind();
			this.receiveBuffer.get(dataCopy, 0, dataLength);
			
			// Delegate work to the proper handler
			PacketHandler handler = this.handlers.getHandler(dataCopy);
			if (handler != null) {
				try {
					// Let the handler process the incoming packet.
					// A response MAY be provided as result.
					byte[] response = handler.handle(dataCopy, dataLength, 0, (InetSocketAddress) dataChannel.getLocalAddress(), (InetSocketAddress) dataChannel.getRemoteAddress());
					
					/*
					 * If handler intends to send a response to the remote peer,
					 * queue the data to send it on writing cycle. Only allowed if
					 * Selection Key is writable!
					 */
					if (response != null && response.length > 0) {
						queueData(response);
					}
				} catch (PacketHandlerException e) {
					logger.error("Could not handle incoming packet: " + e.getMessage());
				}
			} else {
                if (logger.isDebugEnabled()) {
                    logger.debug("No protocol handler was found to process an incoming packet. Packet will be dropped.");
                }
			}
		}
	}

    @Override
    public void send() throws IOException {
        byte[] data = this.pendingData.poll();
        if (data != null) {
            ByteBuffer buffer = ByteBuffer.wrap(data);
            this.dataChannel.send(buffer, this.dataChannel.getRemoteAddress());

            // Keep sending queued data, recursive style
            send();
        }
    }
	
	@Override
	public boolean isOpen() {
		if(this.dataChannel != null) {
			return this.dataChannel.isOpen();
		}
		return false;
	}
	
	@Override
	public boolean isConnected() {
		return this.dataChannel != null && this.dataChannel.isConnected();
	}
	
	@Override
	public void bind(SocketAddress address) throws IOException {
		if(!isOpen()) {
			throw new IOException("The channel is closed.");
		}
		this.dataChannel.bind(address);
	}
	
	@Override
	public void connect(SocketAddress address) throws IOException {
		if(this.dataChannel == null) {
			throw new IOException("No channel available to connect.");
		}
		this.dataChannel.connect(address);
	}
	
	@Override
	public void disconnect() throws IOException {
		if(isConnected()) {
			this.dataChannel.disconnect();
		}
	}
	
	@Override
	public void open() throws IOException {
		if(this.dataChannel == null || !this.dataChannel.isOpen()) {
			this.dataChannel = DatagramChannel.open();
		} else {
			throw new IOException("Channel is already open.");
		}
	}
	
	@Override
	public void open(DatagramChannel dataChannel) throws IOException {
		if(dataChannel == null) {
			throw new IOException("The data channel cannot be null.");
		}
		
		if(!dataChannel.isOpen()) {
			throw new IOException("The data channel is closed.");
		}
		
		this.dataChannel= dataChannel;
	}
	
	@Override
	public void close() {
		if (isOpen()) {
			if (isConnected()) {
				try {
					dataChannel.disconnect();
				} catch (IOException e) {
					logger.warn(e.getMessage(), e);
				}
			}
			try {
				/*
				 * All these steps are necessary to effectively close the channel
				 * on Java NIO. Otherwise the channels will only be elected for
				 * closing and effectively closed when the Selector is closed!
				 * 
				 * https://telestax.atlassian.net/browse/MEDIA-53
				 */
				this.selectionKey.cancel();
				this.dataChannel.socket().close();
				this.dataChannel.close();
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}
		}
	}
	
}
