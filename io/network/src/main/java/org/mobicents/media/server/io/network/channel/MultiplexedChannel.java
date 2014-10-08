package org.mobicents.media.server.io.network.channel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * Data channel that supports multiplexing.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class MultiplexedChannel implements Channel {
	
	private static final Logger logger = Logger.getLogger(Channel.class);
	
	// Data channel where data will be received and transmitted
	protected DatagramChannel channel;

	// Registered protocol handlers. Used for multiplexing.
	protected final PacketHandlerPipeline handlers;

	// The buffer into which we will read data when it's available
	private static final int BUFFER_SIZE = 8192;
	private final ByteBuffer receiveBuffer;
	
	// Data that is pending for writing
	private final List<byte[]> pendingData;
	private final ByteBuffer pendingDataBuffer;

	public MultiplexedChannel() {
		this.handlers = new PacketHandlerPipeline();
		this.pendingData = new ArrayList<byte[]>();
		this.pendingDataBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
		this.receiveBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
	}
	
	public void setChannel(final DatagramChannel channel) {
		this.channel = channel;
	}
	
	protected void queueData(final byte[] data) {
		if (data != null && data.length > 0) {
			synchronized (this.pendingData) {
				this.pendingData.add(data);	
			}
		}
	}
	
	public boolean hasPendingData() {
		synchronized (this.pendingData) {
			return !this.pendingData.isEmpty();
		}
	}
	
	protected void flush() {
		if(this.channel != null && this.channel.isOpen()) {
			try {
				// lets clear the receiver
				SocketAddress currAddress;
				this.receiveBuffer.clear();
				do {
					currAddress = this.channel.receive(this.receiveBuffer);
					this.receiveBuffer.clear();
				} while(currAddress != null);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
	}
	
	public void receive() throws IOException {
		// Get buffer ready to read new data
		this.receiveBuffer.clear();

		// Read data from channel
		int dataLength = 0;
		try {
			SocketAddress remotePeer = channel.receive(this.receiveBuffer);
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
					byte[] response = handler.handle(dataCopy, dataLength, 0, (InetSocketAddress) channel.getLocalAddress(), (InetSocketAddress) channel.getRemoteAddress());
					
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
				logger.warn("No protocol handler was found to process an incoming packet. Packet will be dropped.");
			}
		}
	}

	public void send() throws IOException {
		while (!this.pendingData.isEmpty()) {
			// Get pending data into the proper buffer
			this.pendingDataBuffer.clear();
			this.pendingDataBuffer.put(this.pendingData.get(0));
			this.pendingDataBuffer.flip();
			
			// Send data over the channel
			this.channel.send(this.pendingDataBuffer, this.channel.getRemoteAddress());

			if (this.pendingDataBuffer.remaining() > 0) {
				// Channel buffer is full
				// Data will be written in the next write operation
				break;
			} else {
				// All data was passed to the channel
				// Remove empty buffer
				this.pendingData.remove(0);
			}
		}
	}
	
	public boolean isConnected() {
		return this.channel != null && this.channel.isConnected();
	}
	
	public void connect(SocketAddress address) throws IOException {
		if(this.channel == null) {
			throw new IOException("No channel available to connect.");
		}
		this.channel.connect(address);
	}
	
	public void disconnect() throws IOException {
		if(isConnected()) {
			this.channel.disconnect();
		}
	}
	
	public void close() {
		if(isConnected()) {
			try {
				channel.disconnect();
			} catch (IOException e) {
				logger.error(e);
			}
			try {
				channel.close();
			} catch (IOException e) {
				logger.error(e);
			}
		}
	}
	
}
