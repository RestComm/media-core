package org.mobicents.media.server.io.network.handler;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * Data channel that supports multiplexing.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class MultiplexedChannel implements Multiplexer {
	
	private static final Logger LOGGER = Logger.getLogger(Multiplexer.class);
	
	// Data channel where data will be received and transmitted
	protected SelectionKey selectionKey;

	// Registered protocol handlers. Used for multiplexing.
	protected final ProtocolHandlerPipeline handlers;

	// The buffer into which we will read data when it's available
	private static final int BUFFER_SIZE = 8192;
	private final ByteBuffer buffer;

	// Data that is pending for writing
	private final List<ByteBuffer> pendingData;

	public MultiplexedChannel() {
		this.handlers = new ProtocolHandlerPipeline();
		this.pendingData = new ArrayList<ByteBuffer>();
		this.buffer = ByteBuffer.allocate(BUFFER_SIZE);
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
	
	public void receive() throws IOException {
		// Get channel from selection key
		DatagramChannel channel = (DatagramChannel) selectionKey.channel();

		// Get buffer ready to read new data
		this.buffer.clear();

		// Read data from channel
		int dataLength = 0;
		try {
			SocketAddress remotePeer = channel.receive(this.buffer);
			if (!channel.isConnected() && remotePeer != null) {
				channel.connect(remotePeer);
			}
			dataLength = (remotePeer == null) ? -1 : this.buffer.position();
		} catch (IOException e) {
			dataLength = -1;
		}

		// Stop if socket was shutdown or error occurred
		if (dataLength == -1) {
			channel.close();
			selectionKey.cancel();
			return;
		}

		// Delegate work to the proper handler
		byte[] data = this.buffer.array();
		ProtocolHandler handler = this.handlers.getHandler(data);

		if (handler != null) {
			try {
				// Let the handler process the incoming packet.
				// A response MAY be provided as result.
				byte[] response = handler.handle(data, dataLength, 0);
				
				/*
				 * If handler intends to send a response to the remote peer,
				 * queue the data to send it on writing cycle. Only allowed if
				 * Selection Key is writable!
				 */
				if (selectionKey.isWritable() && response != null && response.length > 0) {
					queueData(data);
				}
			} catch (ProtocolHandlerException e) {
				LOGGER.error("Could not handle incoming packet: " + e.getMessage());
			}
		} else {
			// No proper handler was found. Drop packet.
			LOGGER.warn("No protocol handler was found to process an incoming packet. Packet will be dropped.");
		}
	}

	public void send() throws IOException {
		// Get all pending data on the requests channel
		DatagramChannel channel = (DatagramChannel) selectionKey.channel();

		// Write all pending data on requested channel
		while (!this.pendingData.isEmpty()) {
			ByteBuffer dataBuffer = this.pendingData.get(0);
			channel.send(dataBuffer, channel.getRemoteAddress());
			// XXX channel.write(dataBuffer);

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
		return ((DatagramChannel) this.selectionKey.channel()).isConnected();
	}
	
	public void disconnect() throws IOException {
		DatagramChannel channel = (DatagramChannel) this.selectionKey.channel();
		if(channel.isConnected()) {
			channel.disconnect();
		}
	}
	
	public void close() {
		// TODO close the channel
	}

}
