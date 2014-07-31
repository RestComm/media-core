package org.mobicents.media.server.io.network.channel;

import java.io.IOException;
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
	
	private static final Logger LOGGER = Logger.getLogger(Channel.class);
	
	// Data channel where data will be received and transmitted
	protected DatagramChannel channel;

	// Registered protocol handlers. Used for multiplexing.
	protected final PacketHandlerPipeline handlers;

	// The buffer into which we will read data when it's available
	private static final int BUFFER_SIZE = 8192;
	private final ByteBuffer buffer;
	
	// Data that is pending for writing
	private final List<ByteBuffer> pendingData;

	public MultiplexedChannel() {
		this.handlers = new PacketHandlerPipeline();
		this.pendingData = new ArrayList<ByteBuffer>();
		this.buffer = ByteBuffer.allocate(BUFFER_SIZE);
	}
	
	public void setChannel(final DatagramChannel channel) {
		this.channel = channel;
	}
	
	protected void queueData(final byte[] data) {
		ByteBuffer dataBuffer = ByteBuffer.wrap(data);
		this.pendingData.add(dataBuffer);
	}
	
	public boolean hasPendingData() {
		synchronized (this.pendingData) {
			return !this.pendingData.isEmpty();
		}
	}
	
	protected void flush() {
		if(this.channel != null) {
			try {
				// lets clear the receiver
				SocketAddress currAddress;
				do {
					currAddress = this.channel.receive(this.buffer);
					this.buffer.clear();
				} while(currAddress != null);
			} catch (Exception e) {
				LOGGER.error(e.getMessage(), e);
			}
		}
	}
	
	public void receive() throws IOException {
		// Get buffer ready to read new data
		this.buffer.clear();

		// Read data from channel
		int dataLength = 0;
		try {
			SocketAddress remotePeer = channel.receive(this.buffer);
			if (!isConnected() && remotePeer != null) {
				connect(remotePeer);
			}
			dataLength = (remotePeer == null) ? -1 : this.buffer.position();
		} catch (IOException e) {
			dataLength = -1;
		}

		// Stop if socket was shutdown or error occurred
		if (dataLength == -1) {
			close();
			return;
		}

		// Delegate work to the proper handler
		byte[] data = this.buffer.array();
		PacketHandler handler = this.handlers.getHandler(data);

		if (handler != null) {
			try {
				// Copy data from buffer so we don't mess with original
				byte[] dataCopy = new byte[dataLength];
				System.arraycopy(data, 0, dataCopy, 0, dataLength);
				
				// Let the handler process the incoming packet.
				// A response MAY be provided as result.
				byte[] response = handler.handle(dataCopy, dataLength, 0);
				
				/*
				 * If handler intends to send a response to the remote peer,
				 * queue the data to send it on writing cycle. Only allowed if
				 * Selection Key is writable!
				 */
				if (response != null && response.length > 0) {
					queueData(response);
				}
			} catch (PacketHandlerException e) {
				LOGGER.error("Could not handle incoming packet: " + e.getMessage());
			}
		} else {
			LOGGER.warn("No protocol handler was found to process an incoming packet. Packet will be dropped.");
		}
	}

	public void send() throws IOException {
		// Write all pending data on requested channel
		while (!this.pendingData.isEmpty()) {
			ByteBuffer dataBuffer = this.pendingData.get(0);
			this.channel.send(dataBuffer, this.channel.getRemoteAddress());

			if (dataBuffer.remaining() > 0) {
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
				LOGGER.error(e);
			}
			try {
				channel.close();
			} catch (IOException e) {
				LOGGER.error(e);
			}
		}
	}
	
}
